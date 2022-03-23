package main.Leader;

import main.Message.MessageTransfer;
import main.Message.ServerMessage;
import main.Server.Server;
import main.Server.ServerState;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

public class FastBully {
    private static ArrayList<Server> serverList = new ArrayList<>();
    private static ArrayList<Server> workingServers = new ArrayList<>();
    private static Server leader;
    private static Server self;
    private static ServerState selfState;

    private FastBully() {
    }

    public static Server getLeader(){
        System.out.println("Start a new leader election process");
        selfState = ServerState.getServerState();

        serverList.clear();
        serverList.addAll(selfState.getServerDictionary().values());

        String serverId = selfState.getServer_id();
        self = selfState.getServerDictionary().get(serverId);

        electNewLeader();
        return leader;
    }

    private static void electNewLeader(){
        workingServers.clear();
        JSONObject electionStartMessage = ServerMessage.electionMessage("start_election", self.getServer_id());
        for (Server server: serverList){
            if ((server.getServer_id()).compareTo(self.getServer_id())>0){
                try {
                    sendElectionMessage(server, electionStartMessage);
                } catch (IOException e) {
                    System.out.println("Cannot send election start message");
                }
            }
        }

        try{
            Thread.sleep(500);
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }

        Server highestPriorityServer = getHighestPriorityServer();
        try {
            if (!(highestPriorityServer.getServer_id().equals(self.getServer_id()))) {
                JSONObject leaderNominationMessage = ServerMessage.electionMessage("nomination", self.getServer_id());
                sendElectionMessage(highestPriorityServer, leaderNominationMessage);
            }else {
                sendIamCoordinatorMsg();
            }
        } catch (IOException e) {
           System.out.println("Cannot send leader nomination message");
        }

        workingServers.clear();
        leader = highestPriorityServer;
    }

    public static void replyToElectionStartMessage(JSONObject response){
        String proposerId = (String) response.get("senderServerId");
        Server proposingServer = ServerState.getServerState().getServerDictionary().get(proposerId);
        JSONObject electionAnswerMessage = ServerMessage.electionMessage("answer_election", self.getServer_id());
        System.out.println(self.getServer_id() + "answering to election message");
        try {
            sendElectionMessage(proposingServer, electionAnswerMessage);
        } catch (IOException e) {
            System.out.println(self.getServer_id() + "answering to election message failed");
        }
    }

    public static void receiveElectionAnswerMessage(JSONObject response){
        String senderId = (String) response.get("senderServerId");
        Server senderServer = ServerState.getServerState().getServerDictionary().get(senderId);
        workingServers.add(senderServer);
        System.out.println(senderId + "answer to election received");
    }

    public static void receiveNominationMessage(JSONObject response){
        leader = self;
        sendIamCoordinatorMsg();
    }

    public static void receiveCoordinatorConfirmationMessage(JSONObject response){
        String senderId = (String) response.get("senderServerId");
        Server senderServer = ServerState.getServerState().getServerDictionary().get(senderId);
        leader = senderServer;
        ServerState.getServerState().setLeader(senderServer);
    }

    public static void serverRecoveredMessage(){
        JSONObject IamUpMessage = ServerMessage.electionMessage("IamUp", self.getServer_id());
        workingServers.clear();
        for (Server server: serverList){
            try {
                sendElectionMessage(server, IamUpMessage);
            } catch (IOException e) {
                System.out.println("IamUp message sending failed");
            }
        }

        try{
            Thread.sleep(500);
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }

        Server highestPriorityServer = getHighestPriorityServer();
        leader = highestPriorityServer;
        if(highestPriorityServer == self){
            sendIamCoordinatorMsg();
        }
    }

    public static void receiveIamUpMessage(JSONObject response){
        String senderId = (String) response.get("senderServerId");
        Server senderServer = ServerState.getServerState().getServerDictionary().get(senderId);
        JSONObject viewMessage = ServerMessage.electionMessage("view", self.getServer_id());
        try {
            sendElectionMessage(senderServer, viewMessage);
        } catch (IOException e) {
            System.out.println("View message sending failed");
        }
    }

    public static void receiveViewMessage(JSONObject response){
        String senderId = (String) response.get("senderServerId");
        Server senderServer = ServerState.getServerState().getServerDictionary().get(senderId);
        workingServers.add(senderServer);
    }

    private static void sendElectionMessage(Server server, JSONObject message) throws IOException{
        MessageTransfer.sendServer(message, server);
    }

    private static Server getHighestPriorityServer(){
        Server highestPriorityServer = self;
        String highestPriorityServerId = self.getServer_id();
        if(workingServers.size() > 0){
            for (Server server: workingServers){
                if((server.getServer_id()).compareTo(highestPriorityServerId) > 0){
                    highestPriorityServerId = server.getServer_id();
                    highestPriorityServer = server;
                }
            }
        }
        return highestPriorityServer;
    }

    private static void sendIamCoordinatorMsg(){
        JSONObject iAmCoordinatorMessage = ServerMessage.electionMessage("inform_coordinator", self.getServer_id());
        for (Server server: FastBully.workingServers){
            try {
                sendElectionMessage(server, iAmCoordinatorMessage);
            } catch (IOException e) {
                System.out.println("I am coordinator message sending failed");
            }
        }
    }

    public static void setServerList(ArrayList<Server> serverList){
        FastBully.serverList = serverList;
    }

    public static void setLeader(Server leader){
        FastBully.leader = leader;
    }

    public static void setSelf(Server self){
        FastBully.self = self;
    }

    public static void setSelfState(ServerState selfState){
        FastBully.selfState = selfState;
    }

}