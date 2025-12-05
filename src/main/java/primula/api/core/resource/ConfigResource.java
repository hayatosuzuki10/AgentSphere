/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.arnx.jsonic.JSON;

/**
 * Configファイルのラッパーみたいなもの
 * @author yamamoto
 */
public class ConfigResource {

    private static ConfigResource configResource;
    private Map<String, Object> data;

    private ConfigResource() {
        try {
//        	data = new HashMap<String, Object>();
//        	data.put("FirstAccessAddress", "192.168.0.3");
//        	data.put("FirstAccessAddressPort", "55878");
//        	data.put("DefaultPort", "55878");
//        	data.put("NodeRefreshRate", "5");
//        	data.put("DefaultAgentGroupName", "Default");
            //TODO:どっちかに統一
        	InputStream resourcePath = getClass().getResourceAsStream("/setting/Config.json");
        	data = JSON.decode(resourcePath);
//            String resourcePath = (getClass().getResource("/setting/Config.json").getPath() == null) ? getClass().getClassLoader().getResource("setting/Config.json").getPath() : getClass().getResource("/setting/Config.json").getPath();
//            System.out.println(resourcePath);
//            data = JSON.decode(new FileInputStream(resourcePath));
        } catch (Exception ex) {
            Logger.getLogger(ConfigResource.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
    }

    public static synchronized ConfigResource getInstance() {
        if (configResource == null) {
            configResource = new ConfigResource();
        }
        return configResource;
    }

    public Object getConfigData(String configName) {
        return data.get(configName);
    }
}
