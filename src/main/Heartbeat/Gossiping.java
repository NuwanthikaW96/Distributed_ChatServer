package main.Heartbeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.json.simple.JSONObject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import main.Consensus.LeaderState;
import main.Message.MessageTransfer;
import main.Message.ServerMessage;
import main.Server.Server;
import main.Server.ServerState;

public class Gossiping implements Job {
    private ServerState serverState = ServerState.getServerState();
    private ServerMessage serverMessage = ServerMessage.getServerMessage();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException{

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String aliveErrorFactor = dataMap.get("aliveErrorFactor").toString();

        // First work on heartbeat vector and suspect failure server list

        for (Server serverInfo : serverState.getServers().values()){
            Integer serverId = serverInfo.getServer_id();
            Integer myServerId = serverState.getSelf_id();

            // Get current heartbeat count of a server
            Integer count = serverState.getHeartbeatCount_list().get(serverId);

            // Update heartbeat count vector
            if (serverId.equals(myServerId)) {
                serverState.getHeartbeatCount_list().put(serverId, 0); // Reset own vector always
            } else {
                // Up count all others
                if (count == null) {
                    serverState.getHeartbeatCount_list().put(serverId, 1);
                } else {
                    serverState.getHeartbeatCount_list().put(serverId, count + 1);
                }
            }

            // FIX get the fresh updated current count again
            count = serverState.getHeartbeatCount_list().get(serverId);

            if (count != null) {
                // If heartbeat count is more than error factor
                if (count > Integer.parseInt(aliveErrorFactor)) {
                    serverState.getSuspect_list().put(serverId, "SUSPECTED");
                } else {
                    serverState.getSuspect_list().put(serverId, "NOT_SUSPECTED");
                }
            }

        }

        // Finally gossip about heartbeat vector to a next peer

        int numOfServers = serverState.getServers().size();

        if (numOfServers > 1) { // Gossip required at least 2 servers to be up

            // After updating the heartbeatCountList, randomly select a server and send
            Integer serverIndex = ThreadLocalRandom.current().nextInt(numOfServers - 1);
            ArrayList<Server> remoteServer = new ArrayList<>();
            for (Server server : serverState.getServers().values()) {
                Integer serverId = server.getServer_id();
                Integer myServerId = serverState.getSelf_id();
                if (!serverId.equals(myServerId)) {
                    remoteServer.add(server);
                }
            }

            // Change concurrent hashmap to hashmap before sending
            HashMap<Integer, Integer> heartbeatCountList = new HashMap<>(serverState.getHeartbeatCount_list());
            JSONObject gossipMessage = new JSONObject();
            gossipMessage = serverMessage.gossipMessage(serverState.getSelf_id(), heartbeatCountList);
            try {
                MessageTransfer.sendServer(gossipMessage,remoteServer.get(serverIndex));
                System.out.println("Gossip heartbeat info to next peer s"+remoteServer.get(serverIndex).getServer_id());
            } catch (Exception e){
                System.out.println("WARNNING! Server s"+remoteServer.get(serverIndex).getServer_id() +
                        " has failed");
            }

        }

    }

    public static void receiveMessages(JSONObject j_object) {

        ServerState serverState = ServerState.getServerState();

        HashMap<String, Long> gossipFromOthers = (HashMap<String, Long>) j_object.get("heartbeatCountList");
        Integer fromServer = (int) (long)j_object.get("serverId");

        System.out.println(("Receiving gossip from server: [" + fromServer.toString() + "] gossipping: " + gossipFromOthers));

        //Update the heartbeatcountlist by taking minimum
        for (String serverId : gossipFromOthers.keySet()) {
            Integer localHeartbeatCount = serverState.getHeartbeatCount_list().get(Integer.parseInt(serverId));
            Integer remoteHeartbeatCount = (int) (long)gossipFromOthers.get(serverId);
            if (localHeartbeatCount != null && remoteHeartbeatCount < localHeartbeatCount) {
                serverState.getHeartbeatCount_list().put(Integer.parseInt(serverId), remoteHeartbeatCount);
            }
        }

        System.out.println(("Current cluster heartbeat state is: " + serverState.getHeartbeatCount_list()));

        if (LeaderState.getLeaderState().isLeaderElected() && LeaderState.getLeaderState().getLeader_id().equals(serverState.getSelf_id())) {
            if (serverState.getHeartbeatCount_list().size() < gossipFromOthers.size()) {
                for (String serverId : gossipFromOthers.keySet()) {
                    if (!serverState.getHeartbeatCount_list().containsKey(serverId)) {
                        serverState.getSuspect_list().put(Integer.parseInt(serverId), "SUSPECTED");
                    }  
                }
            }
        }

    }
}

