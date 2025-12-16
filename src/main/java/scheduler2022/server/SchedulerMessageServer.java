package scheduler2022.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import scheduler2022.DynamicPCInfo;
import scheduler2022.Scheduler;

public class SchedulerMessageServer extends Thread {

    private final ServerSocket serverSocket;
    private volatile boolean running = true;

    public SchedulerMessageServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        setName("SchedulerMessageServer-" + port);
        setDaemon(true);
    }

    public void stopServer() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("[SchedulerMessageServer] started. port=" + serverSocket.getLocalPort());

        while (running) {
            try (Socket clientSocket = serverSocket.accept();
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                Object obj = in.readObject();

                if (obj instanceof MigrateInstruction) {
                    MigrateInstruction instr = (MigrateInstruction) obj;

                    String id = instr.agentId;
                    AbstractAgent agent = AgentAPI.getAgentByID(id);

                    if (agent == null) {
                        System.err.println("[SchedulerMessageServer] agent is null! id=" + id);
                        continue;
                    }
                    if (instr.destinationIP == null) {
                        System.err.println("[SchedulerMessageServer] destinationIP is null! id=" + id);
                        continue;
                    }

                    System.out.println("[SchedulerMessageServer] migrate request: agentId=" + id
                            + " -> " + instr.destinationIP);

                    agent.moveByExternal(instr.destinationIP);
                    continue;
                }

                if (obj instanceof SchedulerConfig) {
                    SchedulerConfig config = (SchedulerConfig) obj;

                    System.out.println("[SchedulerMessageServer] config update: strategy=" + config.strategy
                            + " interval=" + config.interval);

                    Scheduler.setStrategyAndInterval(
                            config.strategy,
                            config.interval,
                            config.agentObserveTime,
                            config.remigrateProhibitTime,
                            config.agentEMAAlpha
                    );
                    continue;
                }
                if (obj instanceof DynamicPCInfoUpdate) {
                    DynamicPCInfoUpdate update = (DynamicPCInfoUpdate) obj;

                    String ip = update.getIp();
                    DynamicPCInfo dpi = update.getDpi();

                    Scheduler.getDpis().put(ip, dpi);

                    continue;
                }

                if (obj instanceof StaticPCInfoUpdate) {
                    StaticPCInfoUpdate update = (StaticPCInfoUpdate) obj;

                    String ip = update.getIp();
                    Scheduler.getSpis().put(ip, update.getSpi());

                    continue;
                }

                System.err.println("[SchedulerMessageServer] unknown message type: "
                        + (obj == null ? "null" : obj.getClass().getName()));

            } catch (IOException | ClassNotFoundException e) {
                if (running) e.printStackTrace();
            } catch (Throwable t) {
                // ここで死ぬとサーバ止まるので握ってログ
                t.printStackTrace();
            }
        }

        System.out.println("[SchedulerMessageServer] stopped.");
    }
}