/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.interim.application;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author kurosaki
 */
public class TestApplication {
    public void runApplication(DesktopAgent agent){
        ApplicationFrame frame = new ApplicationFrame("TestApplication");
        

        while(ApplicationFrame.close){
            /**
             * 無限ループさせるだけだと負荷がものすごいので少しスリープさせてる。
             * 一応負荷は減ったけど何らかの問題が発生したら直す。
             */
            try {
                Thread.sleep(1000*2);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }//閉じたらAgentが終わる

    }
   
}
