import java.io.Serializable;
import java.net.InetAddress;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

public class TSPSlaveAgent extends AbstractAgent implements IMessageListener {

    private static final int MSG_PORT = 55878;

    private int numCities;
    private int start, end;
    private int[][] distanceMatrix;
    private int workerId;
    private String masterId;

    private volatile boolean received = false;

    private int bestCost = Integer.MAX_VALUE;
    private int[] bestPath;

    private boolean shouldStop = false;

    @Override
    public void run() {
        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        System.out.println("[TSPSlave] START id=" + getAgentID());

        while (!received && !shouldStop) {
            try { Thread.sleep(200); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        if (shouldStop) return;

        System.out.println("[TSPSlave] task received workerId=" + workerId
                + " range=[" + start + "," + end + ") masterId=" + masterId);


        nextDestination = Scheduler.getNextDestination(this);
        migrate(nextDestination);
        solveTSP();
        sendResultToMaster();

        System.out.println("[TSPSlave] END workerId=" + workerId + " bestCost=" + bestCost);
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        try {
            Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
            if (!(raw instanceof TSPMasterAgent.TSPMasterMessage)) {
                System.err.println("[TSPSlave] unexpected content=" + (raw == null ? "null" : raw.getClass()));
                return;
            }
            TSPMasterAgent.TSPMasterMessage msg = (TSPMasterAgent.TSPMasterMessage) raw;

            this.numCities = msg.numCities;
            this.workerId = msg.workerId;
            this.start = msg.start;
            this.end = msg.end;
            this.distanceMatrix = msg.distanceMatrix;
            this.masterId = msg.masterId;

            this.received = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResultToMaster() {
        if (masterId == null) return;

        try {

            InetAddress ip = primula.agent.util.DHTutil.getAgentIP(masterId);
            KeyValuePair<InetAddress, Integer> dst = new KeyValuePair<>(ip, MSG_PORT);

            TSPMasterAgent.TSPResultMessage res = new TSPMasterAgent.TSPResultMessage();
            res.workerId = workerId;
            res.bestCost = bestCost;
            res.bestPath = (bestPath == null) ? null : bestPath.clone();

            StandardEnvelope env = new StandardEnvelope(
                    new AgentAddress(masterId),
                    new StandardContentContainer((Serializable) res)
            );

            MessageAPI.send(dst, env);
            System.out.println("[TSPSlave] result sent to masterId=" + masterId + " bestCost=" + bestCost);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void solveTSP() {
        bestCost = Integer.MAX_VALUE;
        bestPath = null;

        if (distanceMatrix == null || numCities <= 1) return;

        boolean[] used = new boolean[numCities];
        int[] path = new int[numCities];

        path[0] = 0;
        used[0] = true;

        for (int city = start; city < end && city < numCities; city++) {
            if (shouldStop) break;
            if (used[city]) continue;

            path[1] = city;
            used[city] = true;

            int cost01 = distanceMatrix[0][city];
            dfs(path, used, 2, cost01);

            used[city] = false;
        }
    }

    private void dfs(int[] path, boolean[] used, int depth, int currentCost) {
        if (shouldStop) return;
        if (currentCost >= bestCost) return;

        if (depth == numCities) {
            int last = path[numCities - 1];
            int totalCost = currentCost + distanceMatrix[last][0];
            if (totalCost < bestCost) {
                bestCost = totalCost;
                bestPath = path.clone();
            }
            return;
        }

        int last = path[depth - 1];
        for (int next = 0; next < numCities; next++) {
            if (used[next]) continue;

            int nextCost = currentCost + distanceMatrix[last][next];
            if (nextCost >= bestCost) continue;

            path[depth] = next;
            used[next] = true;

            dfs(path, used, depth + 1, nextCost);

            used[next] = false;
        }
    }

    @Override public void requestStop() { shouldStop = true; }
    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
}