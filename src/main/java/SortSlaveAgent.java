import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.assh.command.demo;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;
import scheduler2022.Scheduler;

public class SortSlaveAgent extends AbstractAgent implements IMessageListener {

    private String masterId;
    private String masterIp;
    private int masterPort;
    private String homeIP = IPAddress.myIPAddress;
    private volatile boolean finish = false;
    private volatile int totalRounds = -1;
    private volatile boolean initialized = false;
    private volatile boolean canMigrate = true;

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

    

        while (!finish) {
            if (!initialized) {

                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                continue;
            }
            String dest = Scheduler.getNextDestination(this);
            migrate(dest);
            try {
                MessageAPI.registerMessageListener(this);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            

            try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
        	
        }
        System.out.println("[SortSlave] finish=true, leaving loop");
        System.out.println("[SortSlave] homeIP=" + homeIP + " cur=" + IPAddress.myIPAddress);
        migrate(homeIP);
        demo.reportAgentHistory(getAgentID(), getAgentName(), buildHistoryText());
    }

    @Override
    public void receivedMessage(AbstractEnvelope envelope) {
        Object raw = ((StandardContentContainer) envelope.getContent()).getContent();

        // STOP
        if (raw instanceof SortMasterAgent.StopMessage) {
            System.out.println("[SortSlave] STOP received id=" + getAgentID());
            finish = true;
            return;
        }

        // Init
        if (raw instanceof SortMasterAgent.SortInitMessage) {
            SortMasterAgent.SortInitMessage init = (SortMasterAgent.SortInitMessage) raw;
            this.totalRounds = init.totalRounds;
            this.initialized = true;
            System.out.println(
                "[SortSlave] INIT received id=" + getAgentID() +
                " totalRounds=" + totalRounds
            );
            return;
        }

        // Task
        if (!(raw instanceof SortMasterAgent.SortTaskMessage)) return;
        SortMasterAgent.SortTaskMessage msg = (SortMasterAgent.SortTaskMessage) raw;

        System.out.println(
            "[SortSlave] TASK received id=" + getAgentID() +
            " round=" + msg.round +
            " segmentSize=" + msg.segment.length
        );

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

        System.out.println(
            "[SortSlave] RESULT sent id=" + getAgentID() +
            " round=" + msg.round +
            " sortMs=" + sortMs
        );

        if (initialized && msg.round >= totalRounds - 1) {
            System.out.println("[SortSlave] FINISH flag set id=" + getAgentID());
            finish = true;
        }
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