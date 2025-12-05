/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.utill;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author VENDETTA
 */
public abstract class  Logger implements java.io.Serializable{
  
    private static final long serialVersionUID = 2277601966641682531L;
    
    public static enum LogLevel{DEBUG, INFO , WARN , ERROR, FATAL};
    
    public final static String LOGGER_CLASS_NAME_PROPERTY_NAME ="Tequila.src.utill.logger.class";

    public final static String LOGGING_OFF_PROPERTY_NAME= "Tequila.src.utill.off";

    public static String STANDARD_LOGGER_CLASS =Log4jLogger.class.getName();
    
    protected String name ="";
    
    private static final Map<String,Logger> loggerInstants = new HashMap<String, Logger>();
    
    protected Logger(String name){
        this.name=name;
    }
    
    public static Logger getLogger(Class _class){
        return getLogger(_class.getName());
    }
    
    public synchronized static Logger getLogger(String name){
        String loggingOff= System.getProperty(LOGGING_OFF_PROPERTY_NAME);
        boolean logOff = false;
        
        if((loggingOff != null)&&(loggingOff.equalsIgnoreCase("true"))){
            name = Logger.class.getName();
            logOff=true;
        }
        
        Logger logger = Logger.loggerInstants.get(name);
        if(logger!=null){
            return logger;
        }
        else{
            if(!logOff){
                String loggerClassName = System.getProperty(LOGGER_CLASS_NAME_PROPERTY_NAME);
                if((loggerClassName==null)||(loggerClassName.equals(""))){
                    loggerClassName =STANDARD_LOGGER_CLASS;
                }
                
                try{
                    Class loggerClass = Class.forName(loggerClassName);
                    Constructor cons = loggerClass.getConstructor(new Class[]{java.lang.String.class});
                    logger = (Logger)cons.newInstance(new Object[]{name});
                    
                }catch(Throwable t){
                    System.setProperty(LOGGING_OFF_PROPERTY_NAME, "true");
                    logger = getLogger(name);
                }                
            }else{
                logger = new DummyLogger(name);
            }
            Logger.loggerInstants.put(name, logger);
            return logger;
        }
    }
    
    
    public abstract boolean isEnabledFor(LogLevel l);
    
    public abstract void debug(Object msg);
    
    public abstract void debug(Object msg,Throwable t);
    
    public abstract void info(Object msg);
    
    public abstract void info(Object msg,Throwable t);
    
    public abstract void warn(Object msg);
    
    public abstract void warn(Object msg,Throwable t);
    
    public abstract void error(Object msg);
    
    public abstract void error(Object msg,Throwable t);
    
    public abstract void fatal(Object msg);
    
    public abstract void fatal(Object msg,Throwable t);
    
    
    
}
