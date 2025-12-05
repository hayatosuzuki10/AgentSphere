/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver.testsocket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.api.AgentAPI;
import primula.api.core.network.receiver.AgentRegistrator;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;


/**
 *
 * @author kurosaki
 */
public class ReceiverThread extends Thread{
    /**
     * flagの説明
     * flag = 1：判定処理の結果収容可能だった
     * flag = 2：判定処理の結果収容不可能だった
     * flag = 3：判定処理を行わない
     */
    private int flag = 3;
    private static AgentRegistrator agentServer;
    public boolean requestStop = false;
    private Socket socket = null;
    private KeyValuePair<String, String> reply;
    public static BlockingQueue<KeyValuePair<String, String>> result; // インターフェースで宣言

    static{
        result = new LinkedBlockingQueue<KeyValuePair<String, String>>(); // 結果をスレッドが参照できるように保存するキュー
        agentServer = AgentRegistrator.getInstance(); // エージェントID情報を使用
        
    }

    /**
     * アクセプト成功されるまで待ち状態。
     * 受信したオブジェクトの中身を見てreplyなら送信スレッドのtaskにsuccessをputする。
     */
    @Override
    public void run(){
        try {
            ServerSocket serverSocket = new ServerSocket(8085); // サーバーソケットを開く

            while(requestStop == false){
                socket = serverSocket.accept(); // 受け取り要求が来るまで待機

                //受信
                System.out.println("通信要求を受信しました");
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                try {
                    reply = (KeyValuePair<String, String>) ois.readObject();
                    ois.close();
                    System.out.println("x:"+reply.getKey());//デバッグ用出力
                } catch (ClassNotFoundException ex) { // PCにあるAgentSphereにクラスの型がない場合
                    Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
                    reply.setKey("again"); // 再送信メッセージ
                    try {
                        SenderThread.task.put(reply);  // 再送信要求
                    } catch (InterruptedException ex1) { // スレッドが割り込み要求された場合
                        Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                }

                if(reply.getKey().equals("reply")){
                    //判定処理（簡易）
                    System.out.println("収容判定中");
                    double usedMemoryRate = ((double)AgentAPI.getUsedMemory()/(double)AgentAPI.getCommittedMemory())*100; // メモリ使用率
                    if(60 < usedMemoryRate){ // 受け入れ不可
                        flag =2; 
                    }
                    else{
                        flag = 1;  // 受け入れ可能
                    }
                }

                //返信する場合送信スレッドにタスクをputする
                if(flag == 1){ // 受け入れるAgentを登録し、そのことを返信する
                    flag = 3;
                    //if(reply.getKey().equals("reply")){
                        //System.out.println("申請を受理しました");
                    Scheduler.storeDPI();
                        reply.setKey("access");
                        try {
                            SenderThread.task.put(reply);
                            agentServer.registerAgentID(reply.getValue()); // 送られてくるエージェントを登録する
                            System.out.println("送られてくるエージェントを登録しました");
                            //AgentReceiveThread.queue.put(reply.getValue());
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex){
                            Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    //}
                }
                else if(flag == 2){ // 受け入れ不可を返信する
                    flag = 3;
                    reply.setKey("over");
                    try{
                        SenderThread.task.put(reply);
                    } catch(InterruptedException ex){
                        Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE,null,ex);
                    }
                }
                else if(flag == 3) // replyのkeyごとの対応
                    if(reply.getKey().equals("finish")){
                        System.out.println("エージェントは正常に移動を完了しました");
                    }
                    else if(reply.getKey().equals("access")){
                        System.out.println("要求が受理されました");
                        try {
                            result.put(reply);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else if(reply.getKey().equals("again")){
                        reply.setKey("reply");
                        try {
                            SenderThread.task.put(reply);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    else if(reply.getKey().equals("over")){
                        try {
                            result.put(reply);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
                        }
                   }
               }
            
            System.out.println("receiver thread finish");
        } catch (IOException ex) {
            Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
        } finally{
            try {
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(ReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void requestStop(){
        this.requestStop = true;
    }

}


