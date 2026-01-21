/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.resource;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import primula.api.core.network.AgentPack;
import primula.util.KeyValuePair;

/**
 *
 * @author yamamoto
 */
public class ReadySendAgentPool {

    // ===== 変更点1: 通知を非同期に流すための専用Executor（単一スレッドで順序維持） =====
    private final ExecutorService notifyExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ReadySendAgentPool-Notifier");
                t.setDaemon(true);
                return t;
            });

    private final ConcurrentLinkedQueue<KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack>> queue =
            new ConcurrentLinkedQueue<>();

    private final ArrayList<ReadySendAgentPoolListener> agentListeners =
            new ArrayList<>();

    private static ReadySendAgentPool agentPool;

    private ReadySendAgentPool() {
    }

    public static synchronized ReadySendAgentPool getInstance() {
        if (agentPool == null) {
            agentPool = new ReadySendAgentPool();
        }
        return agentPool;
    }

    public synchronized void addSendAgent(KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> pair) {
        queue.add(pair);

        // ★ここが重くならないように notify を非同期へ
        notifyAgentAdded(pair.getValue().getAgent().getAgentID(),
                         pair.getValue().getAgent().getAgentName());
    }

    public synchronized int getSize() {
        return queue.size();
    }

    public synchronized KeyValuePair<KeyValuePair<InetAddress, Integer>, AgentPack> poll() {
        return queue.poll();
    }

    public synchronized void addReadySendAgentPoolListener(ReadySendAgentPoolListener listener) {
        agentListeners.add(listener);
    }

    public synchronized void removeReadySendAgentPoolListener(ReadySendAgentPoolListener listener) {
        agentListeners.remove(listener);
    }

    // ===== 変更点2: notifyAgentAddedを「別スレッドで実行」する =====
    private void notifyAgentAdded(String agentID, String agentName) {

        // ===== 変更点3: listener一覧のスナップショットを取ってから非同期で回す =====
        final List<ReadySendAgentPoolListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(agentListeners);
        }

        notifyExecutor.execute(() -> {
            for (ReadySendAgentPoolListener listener : snapshot) {
                try {
                    listener.readySendAgentAdded(agentID, agentName);
                } catch (Throwable t) {
                    // listener 1個が死んでも全体が止まらないようにする
                    System.out.println("[ReadySendAgentPool][notifyAgentAdded][ERR] " + t);
                    t.printStackTrace();
                }
            }
        });
    }

    /**
     * 終了処理が必要なら呼ぶ（任意）
     * アプリ終了時やテストでスレッドが残るのを防ぐ用。
     */
    public void shutdownNotifier() {
        notifyExecutor.shutdown();
    }
}