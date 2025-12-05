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
 * 起動時に送信スレッドを立てる
 * 
 */
public class ConfirmSender implements ICoreModule{
    private SenderThread senderThread = new SenderThread();
    
    @Override
    public void initializeCoreModele() {
        senderThread.start();
    }

    @Override
    public void finalizeCoreModule() {
        senderThread.requestStop();
        //throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
