package scheduler2022.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import primula.api.core.agent.AgentInstanceInfo;
import primula.api.core.assh.MainPanel;
import primula.util.IPAddress;
import scheduler2022.DynamicPCInfo;
import scheduler2022.InformationCenter;
import scheduler2022.Scheduler;
import scheduler2022.StaticPCInfo;

public class EmbeddedHttpServer {
	

	public void start() throws IOException {
        int port = 8083;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // / にアクセスしたとき index.html を返す
        server.createContext("/", new FileHandler("public/index.html", "text/html"));
        server.createContext("/cpu_benchmarks.json", new JsonHandler("public/cpu_benchmarks.json"));
        server.createContext("/gpu_benchmarks.json", new JsonHandler("public/gpu_benchmarks.json"));
        
        server.createContext("/dynamic", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {

            	Map<String, DynamicPCInfo> dynamicPCInfos = InformationCenter.getAllDPIs();
            	Map<String, AgentInstanceInfo> agentInfos = new HashMap<String, AgentInstanceInfo>();
            	//Set<String> allAgentIDs = DHTutil.getAllAgentIDs();
//            	for(String agentID: allAgentIDs) {
//            		AgentInstanceInfo agentInfo = DHTutil.getAgentInfo(agentID);
//            		agentInfos.put(agentID,agentInfo);
//            	}
            	Map<String, Object> dynamicPack = new HashMap<>();
        		dynamicPack.put("dynamicPCInfos", dynamicPCInfos);
        		
        		dynamicPack.put("timeStaticPCInfoChanged", Scheduler.getTimeStaticPCInfoChanged());
        		
        		dynamicPack.put("agentInfos", agentInfos);
        		
                // ObjectMapperでJSONに変換
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(dynamicPack);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(json.getBytes());
                os.close();
            }
        });
        
        server.createContext("/static", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
            	Set<String> allIPAddresses = InformationCenter.getAllIPs();
            	allIPAddresses.add(IPAddress.myIPAddress);
            	Map<String, StaticPCInfo> staticPCInfos = InformationCenter.getAllSPIs();
        		Map<String, Object> staticPack = new HashMap<>();
        		staticPack.put("serverIPAddress", IPAddress.myIPAddress);
        		staticPack.put("allIPAddresses", allIPAddresses);
        		staticPack.put("staticPCInfos", staticPCInfos);

                // ObjectMapperでJSONに変換
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(staticPack);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(json.getBytes());
                os.close();
            }
        });
        server.createContext("/api/scheduler/config", new HttpHandler() {
            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                try (InputStream is = exchange.getRequestBody()) {
                    SchedulerConfig cfg = mapper.readValue(is, SchedulerConfig.class);

                    // バリデーションなど...

                    Scheduler.setStrategyAndInterval(
                    		cfg.strategy, 
                    		cfg.interval,
                    		cfg.agentObserveTime,
                    		cfg.remigrateProhibitTime,
                    		cfg.agentEMAAlpha
                    		);
                    
                    sendJson(exchange, 200, "{\"status\":\"ok\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJson(exchange, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
                }
            }

            // static を外す ✅
            private void sendJson(HttpExchange ex, int code, String json) throws IOException {
                byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                ex.sendResponseHeaders(code, bytes.length);
                try (OutputStream os = ex.getResponseBody()) {
                    os.write(bytes);
                }
            }

            private String escape(String s) {
                return (s == null) ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
            }
        });
        
        server.createContext("/api/console/command", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }

                // リクエストボディ(JSON)読み取り
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();

                // JSON解析
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> req = mapper.readValue(body, Map.class);
                String cmd = (String) req.get("cmd");

                System.out.println("[WEB COMMAND] " + cmd);

                // TODO: ここでコマンド実行やキュー投入などを行う
                // 例えば AgentSphere の ShellEnvironment / Parser を呼び出すなど
                // CommandExecutor.getInstance().submit(cmd);

    			MainPanel.mainPanel.excuteCommand(cmd);

                // 成功レスポンス
                String response = "{\"status\":\"ok\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
            }
        });
        
        // 静的ファイル (JS, CSS) を提供
        server.createContext("/main.js", new FileHandler("public/main.js", "application/javascript"));
        server.createContext("/style.css", new FileHandler("public/style.css", "text/css"));

        server.setExecutor(null); // デフォルトの executor を使う
        server.start();
        System.out.println("Server started on http://localhost:" + port);
    }

    // 静的ファイルを返す汎用ハンドラー
    static class FileHandler implements HttpHandler {
        private final String filePath;
        private final String contentType;

        public FileHandler(String filePath, String contentType) {
            this.filePath = filePath;
            this.contentType = contentType;
            
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File(filePath);
            if (!file.exists()) {
                String notFound = "404 Not Found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.close();
                return;
            }

            byte[] content = Files.readAllBytes(Paths.get(filePath));
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            OutputStream os = exchange.getResponseBody();
            os.write(content);
            os.close();
        }
    }
    
    static class JsonHandler implements HttpHandler {
        private final String filePath;

        // コンストラクタでファイルパスを受け取る
        public JsonHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // JSONファイル読み込み
                String json = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.close();
            } catch (IOException e) {
                // エラーハンドリング
                String error = "{\"error\": \"Cannot read file: " + filePath + "\"}";
                exchange.sendResponseHeaders(500, error.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(error.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        }
    }
}