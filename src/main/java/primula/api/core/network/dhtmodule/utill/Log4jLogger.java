/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.utill;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.log4j.Level;

/**
 *
 * @author VENDETTA
 */
public class Log4jLogger extends Logger{

    private static final long serialVersionUID = 7760816665195470868L;

    private String myFQN = this.getClass().getName();

    private transient org.apache.log4j.Logger logger = null;

    public final static String PROPERTIES_FILE_PROPERTY ="log4j.properties.file";

    private static boolean configured = false;

    public Log4jLogger(String _class){
        super(_class);
        this.logger = org.apache.log4j.Logger.getLogger(_class);
        if(!configured){
            configure();
        }
    }

    private static void configure(){
        if(!configured){
            configured=true;
            java.net.URL configURL = null;
            boolean usefile =false;

           configURL = ClassLoader.getSystemClassLoader().getResource(LOGGER_CLASS_NAME_PROPERTY_NAME);
           if(configURL==null){
               try{
                    java.io.File f = new java.io.File(System.getProperty(PROPERTIES_FILE_PROPERTY));
                    usefile = f.exists();
                    configURL=f.toURI().toURL();
               }catch(Exception e){
                   usefile = false;
               }

               if(usefile){
                   System.out.println("["+Thread.currentThread().getName()+"] INFO"+Log4jLogger.class.getName()+"Configuring log4j with "
                           +System.getProperty(PROPERTIES_FILE_PROPERTY)+".");
                   try{
                       if(System.getProperty(PROPERTIES_FILE_PROPERTY).toLowerCase().endsWith(".xml")){
                          org.apache.log4j.xml.DOMConfigurator.configure(configURL);
                       }
                       else{
                           org.apache.log4j.PropertyConfigurator.configure(configURL);
                       }

                       Logger.getLogger(Logger.class).debug("Logger initialized.");

                   }catch(Throwable t){
                   System.out.println("[" + Thread.currentThread().getName()+
                           " - log4j could not be configured with "+System.getProperty(PROPERTIES_FILE_PROPERTY+"."));
               }
               }else{
                   System.out.println("["+Thread.currentThread().getName()
                           +"] INFO"+Log4jLogger.class.getName()
                           +"Could not find log4j properties files with filename "
                           + System.getProperty(PROPERTIES_FILE_PROPERTY)+".");
                   System.out.println("["+Thread.currentThread().getName()
                           +"] INFO"+Log4jLogger.class.getName()+"- Logging is on.");
                   org.apache.log4j.BasicConfigurator.configure();
                   org.apache.log4j.Level level = org.apache.log4j.Level.ERROR;/*2022にいくら　ログレベル下げてます　元はAll*/
                   org.apache.log4j.Logger.getRootLogger().setLevel(level);
               }
           }
        }

    }


    @Override
    public void debug(Object msg) {
        this.logger.log(this.myFQN, Level.DEBUG, msg, null);
    }

    @Override
    public void debug(Object msg, Throwable t) {
        this.logger.log(myFQN, Level.DEBUG, msg, t);
    }

    @Override
    public void info(Object msg) {
        this.logger.log(myFQN, Level.INFO, msg, null);
    }

    @Override
    public void info(Object msg, Throwable t) {
        this.logger.log(myFQN, Level.INFO, msg, t);
    }

    @Override
    public void warn(Object msg) {
        this.logger.log(myFQN, Level.WARN, msg, null);
    }

    @Override
    public void warn(Object msg, Throwable t) {
        this.logger.log(myFQN, Level.WARN, msg, t);
    }

    @Override
    public void error(Object msg) {
        this.logger.log(myFQN, Level.ERROR, msg, null);
    }

    @Override
    public void error(Object msg, Throwable t) {
        this.logger.log(myFQN, Level.ERROR, msg, t);
    }

    @Override
    public void fatal(Object msg, Throwable t) {
        this.logger.log(myFQN, Level.FATAL, msg, t);
    }

    @Override
    public void fatal(Object msg) {
       this.logger.log(myFQN, Level.FATAL, msg, null);
    }

    @Override
    public boolean isEnabledFor(LogLevel l) {
        switch(l){
            case DEBUG:
                return this.logger.isEnabledFor(Level.DEBUG);
            case INFO:
                return this.logger.isEnabledFor(Level.INFO);
            case WARN:
                return this.logger.isEnabledFor(Level.WARN);
            case ERROR:
                return this.logger.isEnabledFor(Level.ERROR);
            case FATAL:
                return this.logger.isEnabledFor(Level.FATAL);
            default :
                return false;
        }
    }

    private void readObject(ObjectInputStream is) throws ClassNotFoundException,IOException{
        is.defaultReadObject();
        this.logger= org.apache.log4j.Logger.getLogger(super.name);
        if(!configured){
            configure();
        }
    }

    private void writeObject(ObjectOutputStream os)throws IOException{
        os.defaultWriteObject();
    }

}


