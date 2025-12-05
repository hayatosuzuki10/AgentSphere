/**
 *
 */
package primula.api.core.resource;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import primula.api.core.AgentSphereCoreLoader;

/**
 * @author yamamoto
 *
 */
public class SystemResource { // アプリ全体のシステム資源・ログ・メモリ情報を一元管理するユーティリティクラス（シングルトンクラス）

    private static SystemResource systemResource; // シングルトンクラス
    private static AgentSphereCoreLoader agentSphereCoreLoader;
    private Logger logger; // ログ用
    private String agentSphereId = UUID.randomUUID().toString(); // Universally Unique Identifier：被らない識別子
    private MemoryMXBean mbean = ManagementFactory.getMemoryMXBean(); // ヒープメモリを取得
    private MemoryUsage heap = mbean.getHeapMemoryUsage(); // ヒープメモ使用量を取得

    protected SystemResource() {
    }

    public synchronized static void initialize(AgentSphereCoreLoader coreLoader) {
        if (agentSphereCoreLoader == null) {
            agentSphereCoreLoader = coreLoader;
        }
    }

    public synchronized static SystemResource getInstance() {
        if (systemResource == null) { // シングルトンクラス
            systemResource = new SystemResource();
        }
        return systemResource;
    }

    public synchronized void Shutdown() {
        agentSphereCoreLoader.shutdown();
    }

    public synchronized Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("Primula"); // Primulaというロガーを取得
//            String resourcePath = (getClass().getResource("/setting/log4j.xml").getPath() == null) ? getClass().getClassLoader().getResource("/setting/log4j.xml").getPath() : getClass().getResource("/setting/log4j.xml").getPath();
//            DOMConfigurator.configure(resourcePath);
            DOMConfigurator.configure(getClass().getResource("/setting/log4j.xml")); // log4j.xmlにログの設定が書いてある
        }
        return logger;
    }

    public String getAgentSphereId() {
        return agentSphereId;
    }

    public long getUsedMemory(){
        return heap.getUsed();
    }

    public long getCommittedMemory(){
        return heap.getCommitted();
    }

}
