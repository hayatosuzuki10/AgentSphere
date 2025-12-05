/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing;

import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author VENDETTA
 */
public final class PropertiesLoader {
    
    private static boolean loaded=false;
    
    private PropertiesLoader(){
    //インスタンス化させない
    }
    
    public final static String PROPERTY_WHERE_TO_FIND_PROPERTY_FILE="mercury.properties.file";
    
    public final static String STANDARD_PROPERTY_FILE="mercury.properties";
    
    public static void loadPropertyFile(){
        if(loaded){
            throw new IllegalStateException("Properties have already been loaded!");
        }
        loaded=true;
        
        String file = STANDARD_PROPERTY_FILE;
        if(System.getProperty(PROPERTY_WHERE_TO_FIND_PROPERTY_FILE)!=null&&
                System.getProperty(PROPERTY_WHERE_TO_FIND_PROPERTY_FILE).length()!=0){
            file = System.getProperty(PROPERTY_WHERE_TO_FIND_PROPERTY_FILE);
        }
        
        try{
            Properties pros = System.getProperties();
            pros.load(ClassLoader.getSystemResourceAsStream(file));
            System.setProperties(pros);

        }catch(IOException e){
            throw new RuntimeException("Property file was not found;"+file+"! it must be located in the CLASSPATH"
                    + " and either  be named 'mercury.properties' ");
        }
        catch(NullPointerException e){
            throw new RuntimeException("Property file was not found:"+file+"! it must be located in the CLASSPATH"
                    + " and either be named 'mercury.proerties'");
        }
    }
}
