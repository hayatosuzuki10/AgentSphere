/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.api.NetworkAPI;
import primula.api.SystemAPI;
import primula.api.core.ICoreModule;
import primula.api.core.agent.loader.UnknownObjectStream.ObjectIO;
import primula.api.core.resource.NetworkResouce;
import primula.api.core.resource.ReadyRunAgentPool;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

/**
 *
 * @author yamamoto
 */
public class CommunicationReceiver implements ICoreModule {

    private ReceiveThread receiveThread = new ReceiveThread();
    private String Inet4Address;

    @Override
    public void initializeCoreModele() {

        //初期の接続先からほかのAgentSphereの情報を取得してくる
    	/*
        if (!SystemAPI.getConfigData("FirstAccessAddress").toString().equals("")) {
            try {
                GetNodeAgent agent = new GetNodeAgent(SystemAPI.getAgentSphereId());
                AgentAPI.migration(new KeyValuePair<InetAddress, Integer>(InetAddress.getByName(SystemAPI.getConfigData("FirstAccessAddress").toString()), Integer.parseInt(SystemAPI.getConfigData("FirstAccessAddressPort").toString())), agent);
            } catch (UnknownHostException ex) {
                Logger.getLogger(CommunicationReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }*/
        receiveThread.start();
    }

    @Override
    public void finalizeCoreModule() {
        receiveThread.requestStop();
    }
}

class SocketHandler extends Thread {

    private Socket socket;
    private ReadyRunAgentPool readyRunAgentPool = ReadyRunAgentPool.getInstance();

    public SocketHandler(Socket socket) {
        this.socket = socket;
    }

    /*
    @Override
    public void run() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectIO oio = new ObjectIO();
            byte[] binary = new byte[15000]; // 受信するバイト配列を格納するための変数。2048では容量が少ないかも。

            byte[] buf = new byte[1024];
            int seed = 0;
            for(int size=ois.read(buf); size!=-1; size=ois.read(buf)) { // バイナリデータが大きくて、一度に読み取れない場合があるので、小分けにして読み取る。
            	System.out.println("size:"+size);
            	System.arraycopy(buf, 0, binary, seed, size);
            	seed += size;
            }
//            for(int i=0; i<binary.length; i++) System.out.println(binary[i] + " 11111 ObjectIO#getBinary()$binary[" + i + "]"); // 受信するバイト配列の中身を確認する。debag用。
            Object o = oio.getObject(binary);
            ois.close();
            AgentPack pack = (AgentPack) o;
            NetworkAPI.setAddress(pack.getAgentSphereId(), new KeyValuePair<InetAddress, Integer>(((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress(), pack.getPort()));
            readyRunAgentPool.addAgent(pack);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    */

    @Override
    public void run() {
        try {
        	ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            int binarySize;
            Scheduler.storeDPI();
            binarySize = ois.readInt(); // バイト配列の容量を決定するために、送信側から送られるバイト配列の長さを先に送ってもらう。

            ObjectIO oio = new ObjectIO();
            byte[] binary = new byte[binarySize]; // 受信するバイト配列を格納するための変数。
//            System.err.println(binarySize);

            byte[] buf = new byte[1024];
            int seed = 0;
            for(int size=ois.read(buf); size!=-1; size=ois.read(buf)) { // バイナリデータが大きくて、一度に読み取れない場合があるので、小分けにして読み取る。
//            	System.out.println("size:"+size);
            	System.arraycopy(buf, 0, binary, seed, size);
            	seed += size;
            }
//            for(int i=0; i<binary.length; i++) System.out.println(binary[i] + " 11111 ObjectIO#getBinary()$binary[" + i + "]"); // 受信するバイト配列の中身を確認する。debag用。
            Object o = oio.getObject(binary);
            ois.close();
            AgentPack pack = (AgentPack) o;
            NetworkAPI.setAddress(pack.getAgentSphereId(), new KeyValuePair<InetAddress, Integer>(((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress(), pack.getPort()));
            readyRunAgentPool.addAgent(pack);
            socket.close();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}


class ReceiveThread extends Thread {

    private volatile boolean requestStop = false;
    private NetworkResouce networkResouce = NetworkResouce.getInstance();

    public ReceiveThread() {
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            //ServerSocket serverSocket2 = new ServerSocket(Integer.parseInt(SystemAPI.getConfigData("DefaultPort").toString()));
            ServerSocket serverSocket=new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(Integer.parseInt(SystemAPI.getConfigData("DefaultPort").toString())));
            while (!requestStop) {
                try {
                    Socket socket = serverSocket.accept();
                    SocketHandler handler = new SocketHandler(socket);
                    handler.start();
                } catch (IOException ex) {
                    Logger.getLogger(ReceiveThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(ReceiveThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void requestStop() {
        //TODO:ちゃんと終了処理ができるように
        requestStop = true;
    }
}
