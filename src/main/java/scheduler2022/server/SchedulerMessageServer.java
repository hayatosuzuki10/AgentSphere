package scheduler2022.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import scheduler2022.Scheduler;

public class SchedulerMessageServer extends Thread {

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public SchedulerMessageServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void stopServer() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("SchedulerMessageServer started.");
        while (running) {
            try (Socket clientSocket = serverSocket.accept();
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                Object obj = in.readObject();
                if (obj instanceof MigrateInstruction) {
                    MigrateInstruction instr = (MigrateInstruction) obj;
                    //InetAddress inet = InetAddress.getByName(instr.destinationIP);
                    String id = instr.agentId;
                    AbstractAgent agent = AgentAPI.getAgentByID(id);
                    if (agent == null) {
                        System.err.println("agent is null!");
                        return;
                    }
                    if (instr.destinationIP == null) {
                        System.err.println("destinationIP is null!");
                        return;
                    }
                    agent.moveByExternal(instr.destinationIP);
                   
                } else if(obj instanceof SchedulerConfig) {
                	SchedulerConfig config = (SchedulerConfig) obj;
                	Scheduler.setStrategyAndInterval(config.strategy, config.interval, config.agentObserveTime, config.remigrateProhibitTime, config.agentEMAAlpha);
                    
                }

            } catch (IOException | ClassNotFoundException e) {
                if (running) e.printStackTrace();
            }
        }
        System.out.println("SchedulerMessageServer stopped.");
    }
}