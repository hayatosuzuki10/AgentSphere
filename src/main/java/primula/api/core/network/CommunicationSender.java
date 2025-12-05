/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import primula.api.SystemAPI;
import primula.api.core.ICoreModule;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.resource.ReadySendAgentPool;
import primula.api.core.resource.ReadySendAgentPoolListener;
import primula.util.KeyValuePair;

/**
 *
 * @author migiside
 */
public class CommunicationSender implements ICoreModule {

    private ReadySendAgentPool readySendAgentPool = ReadySendAgentPool.getInstance();
/*
    public CommunicationSender() {
        readySendAgentPool.addReadySendAgentPoolListener(new ReadySendAgentPoolListener() {

            @Override
            public void readySendAgentAdded(String agentID, String agentName) {
                KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair = readySendAgentPool.poll();  // InetAddress : ホスト名, Integer : ポート番号
                try {
                    //Socket socket = new Socket(pair.getKey().getKey(), pair.getKey().getValue());
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(pair.getKey().getKey(), pair.getKey().getValue()));
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(pair.getValue());
                    oos.flush();
                    oos.close();
                } catch (IOException ex) {
                    SystemAPI.getLogger().error(ex);
                }

                //TODO:Receiverの方が対応したらこっちを解禁

                CommunicationQueue<AgentPack> sender = new CommunicationQueue<AgentPack>();
                KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair = readySendAgentPool.poll();
                if (sender.transfer(pair.getValue(), new InetSocketAddress(pair.getKey().getKey(), pair.getKey().getValue()))) {
                System.out.println("Agent Send Success");
                } else {
                System.out.println("Agent Send Fialed");
                }
                sender.close();

            }
        });

    }



    public CommunicationSender() {
        readySendAgentPool.addReadySendAgentPoolListener(new ReadySendAgentPoolListener() {

            @Override
            public void readySendAgentAdded(String agentID, String agentName) {
                KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair = readySendAgentPool.poll();  // InetAddress : ホスト名, Integer : ポート番号
                try {
                    //Socket socket = new Socket(pair.getKey().getKey(), pair.getKey().getValue());
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(pair.getKey().getKey(), pair.getKey().getValue()));
                    ObjectIO oio = new ObjectIO();
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.write(oio.getBinary(pair.getValue())); // pair.getValue() : AgentPack
                    oos.flush();
                    oos.close();
                } catch (IOException ex) {
                    SystemAPI.getLogger().error(ex);
                }

                //TODO:Receiverの方が対応したらこっちを解禁

//                CommunicationQueue<AgentPack> sender = new CommunicationQueue<AgentPack>();
//                KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair = readySendAgentPool.poll();
//                if (sender.transfer(pair.getValue(), new InetSocketAddress(pair.getKey().getKey(), pair.getKey().getValue()))) {
//                System.out.println("Agent Send Success");
//                } else {
//                System.out.println("Agent Send Fialed");
//                }
//                sender.close();

            }
        });

    }
*/

    public CommunicationSender() {
        readySendAgentPool.addReadySendAgentPoolListener(new ReadySendAgentPoolListener() {

            @Override
            public void readySendAgentAdded(String agentID, String agentName) {
                KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair = readySendAgentPool.poll();  // InetAddress : ホスト名, Integer : ポート番号
                try {
                    Socket socket = new Socket();
                    socket.setReuseAddress(true);
                    socket.connect(new InetSocketAddress(pair.getKey().getKey(), pair.getKey().getValue()));
                    ObjectIO oio = new ObjectIO();
                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                    byte[] binary;
                    binary = oio.getBinary(pair.getValue()); // pair.getValue() : AgentPack
                    oos.writeInt(binary.length);             // 初めにバイト配列の長さを送信する。
                    oos.flush();
                    oos.reset();

                    oos.write(binary);  // バイト配列を送信する。
                    oos.flush();
                    oos.close();
                    socket.close();
                } catch (IOException ex) {
                    SystemAPI.getLogger().error(ex);
                }
            }
        });

    }

    @Override
    public void initializeCoreModele() {
    }

    @Override
    public void finalizeCoreModule() {
    }
}
