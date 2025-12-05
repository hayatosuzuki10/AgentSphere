/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver.testsocket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import primula.util.IPAddress;
import primula.util.KeyValuePair;



/**
 *
 * @author selab
 */
public class SenderThread extends Thread{
    public static BlockingQueue<KeyValuePair<String, String>> task;
    private Socket socket = null;
    public boolean requestStop = false;
    
    static{
        task = new LinkedBlockingQueue<KeyValuePair<String, String>>();
    }
    
    
    
    /**
     * taskに仕事が入ると中身を相手に送信する
     */
    @Override
    public void run(){ 
        KeyValuePair<String, String> box = new KeyValuePair<String, String>();
        while(requestStop == false){

            socket = new Socket();
            try {
                box = task.take(); // タスクが来るまで待機
            } catch (InterruptedException ex) {
                Logger.getLogger(SenderThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                socket.connect(new InetSocketAddress(IPAddress.IPAddress, 8085)); // DefaultDestIPアドレスに接続を試みる
                //送信
                System.out.println(box.getValue()+"の移動");
                System.out.println("通信要求を送信します");
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(box); // エージェントを書き込む
                oos.flush(); // データを流す
                oos.close();  
            } catch (IOException ex) {
                //Logger.getLogger(SenderThread.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("エージェント通信失敗");
                try {
                    box.setKey("again"); // 再送信を試みる
                    ReceiverThread.result.put(box);
                } catch (InterruptedException ex1) {
                    Logger.getLogger(SenderThread.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } finally{
                try {
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(SenderThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public void requestStop(){
        this.requestStop = true;
        try {
            task.put(new KeyValuePair("dummy", null));
        } catch (InterruptedException ex) {
            Logger.getLogger(SenderThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
