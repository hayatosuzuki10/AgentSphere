package scheduler2022.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SchedulerMessenger {

    public static void sendMigrateRequest(String targetIP, int port, MigrateInstruction instruction) {
        try (Socket socket = new Socket(targetIP, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(instruction);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void sendChangeSchedulerStrategyRequest(String targetIP, int port, SchedulerConfig config) {
        try (Socket socket = new Socket(targetIP, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

               out.writeObject(config);
               out.flush();

           } catch (IOException e) {
               e.printStackTrace();
           }
       }
}