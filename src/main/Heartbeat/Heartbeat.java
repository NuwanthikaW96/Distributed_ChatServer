package main.Heartbeat;

import java.io.IOException;

import main.Leader.FastBully;
import main.Message.MessageTransfer;
import org.json.simple.JSONObject;

import main.Message.ServerMessage;
import main.Server.Server;
import main.Server.ServerState;

public class Heartbeat implements Runnable{

    private final String operation;
    boolean active = true;

    public Heartbeat(String operation){
        this.operation = operation.toLowerCase();
    }

    @Override
    public void run() {
        switch(operation) {
            case "heartbeat":
                while(this.active) {
                    System.out.println("1"+ServerState.getServerState().getServer_id());
                    System.out.println("2"+ServerState.getServerState().getLeader().getServer_id());
                    if(!ServerState.getServerState().getServer_id().equals(ServerState.getServerState().getLeader().getServer_id())) {
                        try {
                            Thread.sleep(1500);
                            JSONObject heartbeatMessage = ServerMessage.heartbeatMessage(ServerState.getServerState().getServer_id());

                            MessageTransfer.sendServer(heartbeatMessage, ServerState.getServerState().getServerDictionary().get(ServerState.getServerState().getLeader().getServer_id()));

                        } catch (IOException e) {
                            this.active = false;
                            Runnable LeaderNotFound = new Heartbeat("LeaderNotFound");
                            new Thread(LeaderNotFound).start();

                        } catch (Exception e) {
                            this.active = false;
                        }
                    }
                }
                break;
            case "leadernotfound":
                String previousLeaderId = ServerState.getServerState().getLeader().getServer_id();
                ServerState.removeSuspectServer(previousLeaderId);
                ServerState.getServerState().setLeader(null);

                Server newLeader = FastBully.getLeader();
                ServerState.getServerState().setLeader(newLeader);

                if (!(ServerState.getServerState().getServer_id().equals(newLeader.getServer_id()))) {
                    Thread heartbeatThread = new Thread(new Heartbeat("heartbeat"));
                    heartbeatThread.start();
                }
                Thread.currentThread().interrupt();
                break;
        }
    }

    public static void updateHeartbeat(JSONObject response) {
        if(ServerState.getServerState().getServer_id().equals(ServerState.getServerState().getLeader().getServer_id())) {
            String senderID = response.get( "sender" ).toString();
        }
    }
}