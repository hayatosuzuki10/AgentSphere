/*
 * To change this textlate, choose Tools | textlates
 * and open the textlate in the editor.
 */
package primula.api.core.interim.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javassist.NotFoundException;
import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.NetworkAPI;
import primula.api.core.agent.AgentInstanceInfo;
import primula.api.core.agent.RunningAgentPoolListener;
import primula.api.core.interim.shell.ShellEnvironment;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.api.core.resource.ReadyRunAgentPoolListener;
import primula.util.KeyValuePair;

/**
 * できあがったらstartup.diconにてスタートアップする
 * モニタのための常駐エージェント。エージェントの動きのログをとるよ
 * ログはPrimulaのディレクトリ内にStationaryAgentLog.txtってファイルを作って
 * その中に入れるよ
 * @author onda
 */
public class StationaryAgent extends AbstractAgent implements IMessageListener,
        ReadyRunAgentPoolListener, RunningAgentPoolListener {

    int n = 5000; //5秒のログを取ります.変更可
    int count = 0; //リストためる配列用
    private ShellEnvironment shellEnv = new ShellEnvironment();
    AbstractEnvelope envelope = null;
    StringBuffer text = new StringBuffer();
    StringBuffer AInfo = new StringBuffer();
    String PathLogFile;
    //1秒間のログをためとくリスト
    ArrayList<AgentLog> logSec = new ArrayList<AgentLog>();
    ArrayList[] logList = new ArrayList[n / 1000];

    @Override
    public String getSimpleName() {
        return getAgentName();
    }

    @Override
    public synchronized void runAgent() {
        //ReadyRunAgentPoolとReadySendAgentPoolに動きがあった時にそれを受け取る
        AgentAPI.registReadyRunAgentPoolListener(this);
        AgentAPI.registRunningAgentPoolListener(this);
        long start, stop, time;
        int hour, minute, second;

        //ここから↓
        /*
         * AgentLog.txtがあればそれを消して新しく作ります。
         * 無ければそのまま作ります。場所はカレントディレクトリ=Primula
         * PathLogFileはAgentLog.txtの絶対パス
         */
        File LogFile = new File("./log/AgentLog.txt");
        if (LogFile.exists()) {
            LogFile.delete();
        }
        try {
            LogFile.createNewFile();
        } catch (IOException e) {
            System.out.println(e);
        }
        PathLogFile = LogFile.getAbsolutePath();
        //↑ここまで　ファイル出力用。確認がいらなくなったら消す

        start = System.currentTimeMillis();
        try {
            FileWriter LogWriter = new FileWriter(PathLogFile);
            try {
                MessageAPI.registerMessageListener(this);//Listenerに登録
                /*ここから下は、ASが終わるまでずっと動く*/
                while (!shellEnv.isStop()) {
                    try {//メッセージ受け取り待ち notifyはrecevedMessageでしてる。
                        wait(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(StationaryAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {//メッセージを受け取った時の処理
                        if (envelope != null) {
                            //System.out.println("へい！！");
                            if (envelope.toString().startsWith("primula.api.core.network.message.StandardEnvelope@")) {
                                envelopeCheck((StandardEnvelope) envelope);
                                envelope = null;
                            }//他特殊なEnvelope作ってそれを常駐エージェントが受け取るようなことがあればここに追加
                        }
                    } catch (NotFoundException ex) {
                        Logger.getLogger(StationaryAgent.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //メッセージ処理終了orメッセージを受け取らずに1秒経過

                    stop = System.currentTimeMillis();
                    time = stop - start;

                    //メッセージ処理で一秒以上経ってたらログをファイルに出力
                    //if (time >= 1000 && !logSec.isEmpty()) {
                    if (time >= 1000) { //毎秒
                        //System.out.println("へい！");
                        Calendar cal1 = Calendar.getInstance();
                        hour = cal1.get(Calendar.HOUR_OF_DAY);
                        minute = cal1.get(Calendar.MINUTE);
                        second = cal1.get(Calendar.SECOND);
                        LogWriter.write(hour + "時" + minute + "分" + second + "秒\r\n");
                        //もう配列でよくね？ｗ
                        if (count < n / 1000) {
                            logList[count] = logSec;
                            count++;
                        } else if (count >= n / 1000) {//満杯だったらぐるぐる回して入れなおす
                            for (int i = 0; i >= n / 1000; i++) {
                                logList[i] = logList[i + 1];
                            }
                            logList[(n / 1000) - 1] = logSec;
                        }

                        //↓確認のため一応ファイルに出しときます。できたら消す
                        if (!logSec.isEmpty()) {
                            for (AgentLog log : logSec) {
                                LogWriter.write(log.getAgentName() + "/");
                                LogWriter.write(log.getAgentID() + "/");
                                LogWriter.write(log.getAgentMove() + "\r\n");
                            }
                            LogWriter.flush();
                        }
                        //↑ここまで

                        logSec.clear();
                    }
                    start = System.currentTimeMillis();

                }//while終わり
            } catch (UnknownHostException ex) {
                Logger.getLogger(StationaryAgent.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
            }
        } catch (IOException e) {//FileWriterの例外処理
            System.out.println(e + "例外が発生しました");
        }

        AgentAPI.removeReadyRunAgetPoolListener(this);
        AgentAPI.removeRunnningAgentPoolListener(this);
    }

    private void envelopeCheck(StandardEnvelope envelope) throws NotFoundException {
        String string = envelope.getContent().toString();

        if (string.startsWith("primula.api.core.network.message.StandardContentContainer@")) {
            contentCheck((StandardContentContainer) envelope.getContent());
        } else if (string.startsWith("primula.api.core.interim.monitor.ReturnReqContentContainer@")) {
            contentCheck((ReplyContentContainer) envelope.getContent());
        }
    }

//　　　　contentがStandardContentContainerだったら
    private void contentCheck(StandardContentContainer content) {
    }

//　　　　contentがReturnReqContainer(返信要求)だったら
    private void contentCheck(ReplyContentContainer content) throws NotFoundException {
        text.setLength(0);   //初期化
        AInfo.setLength(0);  //初期化
        int countAgent = 0;
        KeyValuePair<InetAddress, Integer> address = NetworkAPI.getAddressByAgentSphereId(content.getAgentSphereID());
        StandardEnvelope send_envelope = null;
//　　　　RemoteMachine用処理
        if (content.getOperation().equals("MachineInfo")) {
            StringBuffer AInfo = new StringBuffer();
            try {
                InetAddress inet = InetAddress.getLocalHost();
                text.append("Host Name  ：").append(inet.getHostName()).append("\r\n");
                text.append("IP Address ：").append(inet.getHostAddress()).append("\r\n");
            } catch (UnknownHostException uhe) {
                System.err.println(uhe.getMessage());
                System.exit(-1);
            }
            HashMap<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
            AInfo.append("Agent Name / Agent ID：\r\n");
            for (String string : agentInfos.keySet()) {
                AInfo.append(string).append(":\r\n"); //AgentGroup
                for (AgentInstanceInfo info : agentInfos.get(string)) {
                    countAgent++;
                    AInfo.append("\t").append(info.getAgentName()).append(" / ").append(info.getAgentId()).append("\r\n");
                }
            }
            text.append("エージェント数：").append(countAgent).append("\r\n");
            text.append(AInfo);
            //送り返すcontent
            StandardContentContainer send_content = new StandardContentContainer(text);
            send_envelope = new StandardEnvelope(new AgentAddress(content.getAgentID()), send_content);

//　　　　　AgentMovement用処理
        } else if (content.getOperation().equals("LogRequest")) {
            System.out.println("LogRequest");
            //送り返すcontent
            StandardContentContainer send_content = new StandardContentContainer((Serializable) logList);
            send_envelope = new StandardEnvelope(new AgentAddress(content.getAgentID()), send_content);

//　　　　　ImageMachineInfo用処理
        } else if (content.getOperation().equals("ImageMachineInfo")) {
            //自分のエージェントの情報を格納して送り返します。
            System.out.println("ImageMachineInfo");
            HashMap<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
            for (String infos : agentInfos.keySet()) {
                AInfo.append(infos).append(":\r\n"); //AgentGroup
                for (AgentInstanceInfo info : agentInfos.get(infos)) {
                    countAgent++;
                    AInfo.append("　").append(info.getAgentName()).append("\r\n");
                }
            }
            text.append("エージェント数：").append(countAgent).append("\r\n");
            text.append(AInfo);
            //textをcontentにしてsend_envelopeに入れます
            StandardContentContainer send_content = new StandardContentContainer((Serializable) new ImageMachineInfo(text, countAgent));
            send_envelope = new StandardEnvelope(new AgentAddress(content.getAgentID()), send_content);

//      AgentGroup用処理
        } else if (content.getOperation().equals("AgentGroup")) {
            System.out.println("AgentGroup");
            try {
                InetAddress inet = InetAddress.getLocalHost();
                text.append(inet.getHostAddress()).append("\r\n");
            } catch (UnknownHostException uhe) {
                System.err.println(uhe.getMessage());
                System.exit(-1);
            }
            HashMap<String, List<AgentInstanceInfo>> agentInfos = AgentAPI.getAgentInfos();
            for (String string : agentInfos.keySet()) {
                if (string.equals(content.getGroupName())) {
                    for (AgentInstanceInfo info : agentInfos.get(string)) {
                        text.append("\t").append(info.getAgentName()).append(" / ").append(info.getAgentId()).append("\r\n");
                    }
                }
            }
            StandardContentContainer send_content = new StandardContentContainer((Serializable) text);
            send_envelope = new StandardEnvelope(new AgentAddress(content.getAgentID()), send_content);

        } else {//他は以下追加
            System.out.println("要求命令　：" + content.getOperation() + "はStationaryAgentにて未実装です。");
        }
        System.out.println("返信します");
        MessageAPI.send(address, send_envelope);
    }

    @Override
    public void requestStop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public synchronized void receivedMessage(AbstractEnvelope envelope) {
        //System.out.println("env:"+envelope);
        this.envelope = envelope;
        notify();
    }

    @Override //ReadyRunAgentPool
    public void readyRunAgentAdded(String agentID, String agentName) {
        logSec.add(new AgentLog(agentName, agentID, "in"));
    }

    @Override //RunningAgentPool
    public void agentFinished(String agentID, String agentName) {
        logSec.add(new AgentLog(agentName, agentID, "out"));
    }

    @Override
    public String getStrictName() {
        return getAgentID();
    }
}
