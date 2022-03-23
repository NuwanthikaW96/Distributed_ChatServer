package main.Heartbeat;

import main.Consensus.LeaderState;
import main.Message.MessageTransfer;
import main.Message.ServerMessage;
import org.json.simple.JSONObject;
import org.quartz.*;
import main.Server.Server;
import main.Server.ServerState;

import java.util.ArrayList;

public class Consensus implements Job {

    private ServerState serverState = ServerState.getServerState();
    private LeaderState leaderState = LeaderState.getLeaderState();
    private ServerMessage serverMessage = ServerMessage.getServerMessage();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!serverState.onGoingConsensus().get()) {
            // If no leader elected at the moment then no consensus task to perform.
            if (leaderState.isLeaderElected()) {
                serverState.onGoingConsensus().set(true);
                performConsensus(context); // critical region
                serverState.onGoingConsensus().set(false);
            }
        } else {
            System.out.println("There seems to be on going consensus at the moment, SKIP!");
        }
    }

    private void performConsensus(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String consensusVoteDuration = dataMap.get("consensusVoteDuration").toString();

        Integer suspectServerId = null;

        // Initialize the vote set
        serverState.getVote_set().put("YES", 0);
        serverState.getVote_set().put("NO", 0);

        Integer leaderServerId = leaderState.getLeader_id();
        Integer myServerId = serverState.getSelf_id();

        // If I am leader, and suspect someone, I want to start voting to KICK him!
        if (myServerId.equals(leaderServerId)) {

            // Find the next suspect to vote and break the loop
            for (Integer serverId : serverState.getSuspect_list().keySet()) {
                if (serverState.getSuspect_list().get(serverId).equals("SUSPECTED")) {
                    suspectServerId = serverId;
                    break;
                }
            }

            ArrayList<Server> serverList = new ArrayList<>();
            for (Integer serverid : serverState.getServers().keySet()) {
                if (!serverid.equals(serverState.getSelf_id()) && serverState.getSuspect_list().get(serverid).equals("NOT_SUSPECTED")) {
                    serverList.add(serverState.getServers().get(serverid));
                }
            }

            //Got a suspect
            if (suspectServerId != null) {

                serverState.getVote_set().put("YES", 1); // Suspect it already and vote yes.
                JSONObject startVoteMessage = new JSONObject();
                startVoteMessage = serverMessage.startVoteMessage(serverState.getSelf_id(), suspectServerId);
                try {
                    MessageTransfer.sendServerBroadcast(startVoteMessage, serverList);
                    System.out.println("Leader calling for vote to kick suspect-server: " + startVoteMessage);
                } catch (Exception e) {
                    System.out.println("WARNNING! Leader calling for vote to kick suspect-server is failed.");
                }

                //Waiting for consensus vote duration period
                try {
                    Thread.sleep(Integer.parseInt(consensusVoteDuration) * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println((String.format("Consensus votes to kick server [%s]: %s", suspectServerId, serverState.getVote_set())));

                if (serverState.getVote_set().get("YES") > serverState.getVote_set().get("NO")) {

                    JSONObject notifyServerDownMessage = new JSONObject();
                    notifyServerDownMessage = serverMessage.notifyServerDownMessage(suspectServerId);
                    try {

                        MessageTransfer.sendServerBroadcast(notifyServerDownMessage, serverList);
                        System.out.println("Notify server " + suspectServerId + " down. Removing...");                 
                        leaderState.removeRemoteChatRoomsClientsByServerId(suspectServerId);
                        serverState.removeServerInCount_list(suspectServerId);
                        serverState.removeServerInSuspect_list(suspectServerId);

                    } catch (Exception e) {
                        System.out.println("ERROR! " + suspectServerId + "Failed Removing.");
                    }

                    System.out.println("Number of servers in group: " + serverState.getServers().size());
                }
            }
        }
    }

    public static void startVoteMessageHandler(JSONObject j_object){

        ServerState serverState = ServerState.getServerState();
        ServerMessage serverMessage = ServerMessage.getServerMessage();

        Integer suspectServerId = (int) (long)j_object.get("suspectServerId");
        Integer serverId = (int) (long)j_object.get("serverId");
        Integer mySeverId = serverState.getSelf_id();

        if (serverState.getSuspect_list().containsKey(suspectServerId)) {
            if (serverState.getSuspect_list().get(suspectServerId).equals("SUSPECTED")) {

                JSONObject answerVoteMessage = new JSONObject();
                answerVoteMessage = serverMessage.answerVoteMessage(suspectServerId, "YES", mySeverId);
                try {
                    MessageTransfer.sendServer(answerVoteMessage, serverState.getServers().get(LeaderState.getLeaderState().getLeader_id()));
                    System.out.println(String.format("Voting on suspected server: [%s] vote: YES", suspectServerId));
                } catch (Exception e) {
                    System.out.println("ERROR! Voting on suspected server is failed");
                }

            } else {

                JSONObject answerVoteMessage = new JSONObject();
                answerVoteMessage = serverMessage.answerVoteMessage(suspectServerId, "NO", mySeverId);
                try {
                    MessageTransfer.sendServer(answerVoteMessage, serverState.getServers().get(LeaderState.getLeaderState().getLeader_id()));
                    System.out.println(String.format("Voting on suspected server: [%s] vote: NO", suspectServerId));
                } catch (Exception e) {
                    System.out.println("ERROR! Voting on suspected server is failed");
                }
            }
        }

    }

    public static void answerVoteHandler(JSONObject j_object){

        ServerState serverState = ServerState.getServerState();

        Integer suspectServerId = (int) (long)j_object.get("suspectServerId");
        String vote = (String) j_object.get("vote");
        Integer votedBy = (int) (long)j_object.get("votedBy");

        Integer voteCount = serverState.getVote_set().get(vote);

        System.out.println(String.format("Receiving voting to kick [%s]: [%s] voted by server: [%s]", suspectServerId, vote, votedBy));

        if (voteCount == null) {
            serverState.getVote_set().put(vote, 1);
        } else {
            serverState.getVote_set().put(vote, voteCount + 1);
        }

    }

    public static void notifyServerDownMessageHandler(JSONObject j_object){

        ServerState serverState = ServerState.getServerState();
        LeaderState leaderState = LeaderState.getLeaderState();

        Integer serverId = (int) (long)j_object.get("serverId");

        System.out.println("Server down notification received. Removing server: " + serverId);

        leaderState.removeRemoteChatRoomsClientsByServerId(serverId);
        serverState.removeServerInCount_list(serverId);
        serverState.removeServerInSuspect_list(serverId);
    }

}
