/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.utill;

/**
 * ダミーのロガー、このクラスのメソッドは何もしない
 * @author VENDETTA
 */
public class DummyLogger extends Logger {
    private static final long serialVersionUID = 3913508246539266519L;
    
    
    DummyLogger(String _class){
        super(_class);
    }

    @Override
    public boolean isEnabledFor(LogLevel l) {
        return false;
    }

    @Override
    public void debug(Object msg) {
        
    }

    @Override
    public void debug(Object msg, Throwable t) {
        
    }

    @Override
    public void info(Object msg) {
        
    }

    @Override
    public void info(Object msg, Throwable t) {
        
    }

    @Override
    public void warn(Object msg) {
       
    }

    @Override
    public void warn(Object msg, Throwable t) {
       
    }

    @Override
    public void error(Object msg) {
       
    }

    @Override
    public void error(Object msg, Throwable t) {
       
    }

    @Override
    public void fatal(Object msg, Throwable t) {
       
    }
    
    @Override
    public void fatal(Object msg){
        
    }
    
}
