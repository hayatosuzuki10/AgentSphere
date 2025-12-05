/**
 *
 */
package primula.api;

import org.apache.log4j.Logger;

import primula.api.core.resource.ConfigResource;
import primula.api.core.resource.SystemConfigResource;
import primula.api.core.resource.SystemResource;

/**
 * @author migiside
 *
 */
public class SystemAPI {

	private static SystemResource systemResource = SystemResource.getInstance();
	private static ConfigResource configResource = ConfigResource.getInstance();
	private static SystemConfigResource systemConfigResource = SystemConfigResource.getInstance();

	public synchronized static void shutdown() {
		systemResource.Shutdown();

	}

	public synchronized static Object getConfigData(String configName) {
		return configResource.getConfigData(configName);
	}

	public synchronized static Object getSystemConfigData(String configName) {
		return systemConfigResource.getConfigData(configName);
	}

	public synchronized static Logger getLogger() {
		return systemResource.getLogger();
	}

	public synchronized static String getAgentSphereId() {
		return systemResource.getAgentSphereId();
	}

}
