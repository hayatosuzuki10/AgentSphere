package scheduler2022.server;

import java.io.Serializable;

public class MigrateInstruction implements Serializable {
    public String agentId;
    public String destinationIP;
    public boolean force;

    public MigrateInstruction(String agentId, String destinationIP, boolean force) {
        this.agentId = agentId;
        this.destinationIP = destinationIP;
        this.force = force;
    }

    @Override
    public String toString() {
        return "[MigrateInstruction] agentId=" + agentId + ", destIP=" + destinationIP + ", force=" + force;
    }
}