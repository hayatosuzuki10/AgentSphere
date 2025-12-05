package primula.api.core.resource;

import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.arnx.jsonic.JSON;

/**
 * ConfigResourceの使用用途がいまいちわからずなので、それをはっきりさせるべく作成した悲しみのクラス<br>
 * このクラスはマシンごとに違う設定となるであろう情報(デフォルトの送信先IPとか)を設定したjsonファイルのラッパーとなります。<br>
 * githubへのプッシュ対象外となるjsonファイルになるのでエントリはここで設定して内容は各々書かせる感じにする
 * @author Norito
 *
 */
public class SystemConfigResource {
	private static SystemConfigResource configResource;
	private static final String configDir = "./setting";
	private static final String configPath = configDir + "/SystemConfig.json";
	private Map<String, Object> data;

	public static final String DEFAULT_DEST="DefaultDest";
	public static final String DEFAULT_IP_ADDR="DefaultIPaddr";
	public static final String DEFAULT_NETWORK_PREFIX_LENGTH="DefaultNetworkPrefixLength";
	public static final String DEFAULT_MESSAGE_TTL = "DefaultMessageTTL";
	public static final String AGENTSPHERE_VERSION_NAME="AgentSphereVersionName";

	private SystemConfigResource() {
		try {
			if (Files.notExists(Paths.get(configPath))) {
				JSON myJOSN = new JSON();
				myJOSN.setPrettyPrint(true);
				Map<String, Object> myMap = new HashMap<String, Object>();
				myMap.put(DEFAULT_DEST, "");//AbstractAgent.migrete()のデフォルト転送先
				myMap.put(DEFAULT_IP_ADDR, "");//エージェントスフィアがデフォルトで使用するIPアドレス
				myMap.put(DEFAULT_NETWORK_PREFIX_LENGTH, "");//デフォルトIPアドレスのネットワークプレフィックス
				//MessageAPIで送信したメッセージのデフォルト転送数
				//この回数だけ再検索してそのエージェントがいるAgentSphereに送ろうとする
				myMap.put(DEFAULT_MESSAGE_TTL, "5");
				myMap.put(AGENTSPHERE_VERSION_NAME,"AgentSphere");
				if (Files.notExists(Paths.get(configDir))) {
					Files.createDirectories(Paths.get(configDir));
				}
				Files.createFile(Paths.get(configPath));
				FileWriter fout = new FileWriter(configPath);
				fout.write(myJOSN.format(myMap));
				fout.close();
			}
			InputStream resourcePath = Files.newInputStream(Paths.get(configPath));
			data = JSON.decode(resourcePath);
		} catch (Exception ex) {
			Logger.getLogger(ConfigResource.class.getName()).log(Level.SEVERE, null, ex);
			ex.printStackTrace();
			System.err.println("sinnda");
			//System.exit(1);
		}
	}

	public static synchronized SystemConfigResource getInstance() {
		if (configResource == null) {
			configResource = new SystemConfigResource();
		}
		return configResource;
	}

	public Object getConfigData(String configName) {
		try {
			return data.get(configName);
		} catch (NullPointerException e) {
			throw new NullPointerException("ｶﾞｯ");
		}
	}
}
