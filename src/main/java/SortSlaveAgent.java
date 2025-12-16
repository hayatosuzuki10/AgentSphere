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

public class SortSlaveAgent extends AbstractAgent implements IMessageListener {

    private int workerId;
    private int[] segment;
    private String masterId;
    private String masterHost;

    private volatile boolean received = false;

    @Override
    public void run() {

        try {
            MessageAPI.registerMessageListener(this);
        } catch (Exception e) {}

        while (!received) {
            try { Thread.sleep(500); }
            catch (InterruptedException e) {}
        }

        Arrays.sort(segment);
        sendResult();
    }

    private void sendResult() {
        SortMasterAgent.SortResultMessage msg =
                new SortMasterAgent.SortResultMessage();
        msg.workerId = workerId;
        msg.sortedSegment = segment;

        try {
            InetAddress addr = InetAddress.getByName(masterHost);
            KeyValuePair<InetAddress, Integer> ip =
                    new KeyValuePair<>(addr, 55878);

            StandardEnvelope env = new StandardEnvelope(
                    new AgentAddress(masterId),
                    new StandardContentContainer(msg)
            );

            MessageAPI.send(ip, env);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {

        Object raw = ((StandardContentContainer) envelope.getContent()).getContent();
        if (!(raw instanceof SortMasterAgent.SortTaskMessage)) return;

        SortMasterAgent.SortTaskMessage task =
                (SortMasterAgent.SortTaskMessage) raw;

        workerId = task.workerId;
        segment = task.segment;
        masterId = task.masterId;
        masterHost = task.masterHost;

        received = true;
    }

    @Override public void requestStop() {}
    @Override public String getStrictName() { return getAgentID(); }
    @Override public String getSimpleName() { return getAgentName(); }
}