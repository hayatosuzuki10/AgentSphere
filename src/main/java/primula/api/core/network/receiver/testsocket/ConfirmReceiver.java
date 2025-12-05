/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.receiver.testsocket;

import primula.api.core.ICoreModule;




/**
 *
 * @author kurosaki
 */
/**
 * 
 * 起動時に受信スレッドを立てる
 */
public class ConfirmReceiver implements ICoreModule{
    private ReceiverThread receiverThread = new ReceiverThread();
    
    @Override
    public void initializeCoreModele(){
        receiverThread.start();
    }

    @Override
    public void finalizeCoreModule() {
        receiverThread.requestStop();
        // new UnsupportedOperationException("Not supported yet.");
    }
}
