import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

public class SortSlaveAgent extends AbstractAgent implements IMessageListener {

    private String masterId;
    private String masterIp;
    private int masterPort;

    public void setMasterInfo(String masterId, String masterIp, int masterPort) {
        this.masterId = masterId;
        this.masterIp = masterIp;
        this.masterPort = masterPort;
    }

    @Override
    public void run() {
        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[SortSlave] START id=" + getAgentID());
        while (true) {

            nextDestination = Scheduler.getNextDestination(this);
            migrate(nextDestination);
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
    	
        Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
        if (!(raw instanceof SortMasterAgent.SortTaskMessage)) return;

        SortMasterAgent.SortTaskMessage msg =
                (SortMasterAgent.SortTaskMessage) raw;

        long t0 = System.currentTimeMillis();
        int[] segment = msg.segment;
        Arrays.sort(segment);
        long sortMs = System.currentTimeMillis() - t0;

        SortMasterAgent.SortResultMessage result =
                new SortMasterAgent.SortResultMessage();
        result.workerId = msg.workerId;
        result.round = msg.round;
        result.sortedSegment = segment;
        result.sortMs = sortMs;

        sendResult(result);
    }

    private void sendResult(Serializable msg) {
        try {
            InetAddress ip = InetAddress.getByName(masterIp);
            KeyValuePair<InetAddress, Integer> dest =
                    new KeyValuePair<>(ip, masterPort);

            StandardEnvelope env = new StandardEnvelope(
                    new AgentAddress(masterId),
                    new StandardContentContainer(msg)
            );
            MessageAPI.send(dest, env);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void requestStop() {}
    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
}