package main.Server;

import java.io.*;

import main.Client.ClientThreadHandler;
import main.Client.ClientState;
import main.Consensus.LeaderState;
import main.Heartbeat.Consensus;
import main.Heartbeat.Gossiping;
import main.Leader.FastBully;
import main.Message.MessageTransfer;
import main.Message.ServerMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ServerHandler extends Thread {

    private final ServerSocket serverCoordinationSocket;


    public ServerHandler(ServerSocket serverCoordinationSocket) {
        this.serverCoordinationSocket = serverCoordinationSocket;
    }

    @Override
    public void run() {
        System.out.println("ServerHeanderler");
        try {
            while (true) {
                Socket serverSocket = serverCoordinationSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(serverSocket.getInputStream(), StandardCharsets.UTF_8)
                );
                String jsonStringFromServer = bufferedReader.readLine();

                // convert received message to json object
                JSONObject j_object = MessageTransfer.convertToJson(jsonStringFromServer);

                System.out.println(j_object);
                if (MessageTransfer.hasKey(j_object, "election")) {
                    System.out.println("buly");
                    String electionMessageType = (String) j_object.get("electionMessageType");
                    switch(electionMessageType){
                        case "start_election":
                            FastBully.replyToElectionStartMessage(j_object);
                            break;

                        case "answer_election":
                            FastBully.receiveElectionAnswerMessage(j_object);
                            break;

                        case "nomination":
                            FastBully.receiveNominationMessage(j_object);
                            break;

                        case "inform_coordinator":
                            FastBully.receiveCoordinatorConfirmationMessage(j_object);
                            break;

                        case "IamUp":
                            FastBully.receiveIamUpMessage(j_object);
                            break;

                        case "view":
                            FastBully.receiveViewMessage(j_object);
                            break;
                    }
                } else if (MessageTransfer.hasKey(j_object, "type")) {

                    if (j_object.get("type").equals("clientidapprovalrequest")
                            && j_object.get("clientid") != null && j_object.get("sender") != null
                            && j_object.get("threadid") != null) {

                        // leader processes client ID approval request received
                        String clientID = j_object.get("clientid").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();

                        boolean approved = !LeaderState.getLeaderState().isClientIDAlreadyTaken(clientID);
                        if (approved) {
                            ClientState clientState = new ClientState(clientID,
                                    ServerState.getMainHallIDbyServerInt(sender),null);
                            LeaderState.getLeaderState().addClient(clientState);
                        }
                        Server destServer = ServerState.getServerState().getServers()
                                .get(sender);
                        try {
                            // send client id approval reply to sender
                            MessageTransfer.sendServer(
                                    ServerMessage.getClientIdApprovalReply(String.valueOf(approved), threadID),
                                    destServer
                            );
                            System.out.println("INFO : Client ID '" + clientID +
                                    "' from s" + sender + " is" + (approved ? " " : " not ") + "approved");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else if (j_object.get("type").equals("clientidapprovalreply")
                            && j_object.get("approved") != null && j_object.get("threadid") != null) {

                        // non leader processes client ID approval request reply received
                        String approved = Boolean.parseBoolean(j_object.get("approved").toString()) ? "yes" : "neutral";
                        Long threadID = Long.parseLong(j_object.get("threadid").toString());

                        ClientThreadHandler clientHandlerThread = ServerState.getServerState()
                                .getClientHandlerThread(threadID);
                        clientHandlerThread.setApprovedClient_id(approved);
                        Object lock = clientHandlerThread.getLock();
                        synchronized (lock) {
                            lock.notifyAll();
                        }

                    } else if (j_object.get("type").equals("roomcreateapprovalrequest")) {

                        // leader processes room create approval request received
                        String clientID = j_object.get("clientid").toString();
                        String roomID = j_object.get("roomid").toString();
                        String sender = (j_object.get("sender").toString());
                        //String sender1 = j_object.get("sender").toString();
                        String threadID = j_object.get("threadid").toString();

                        boolean approved = LeaderState.getLeaderState().isRoomCreationApproved(roomID);

                        if (approved) {
                            LeaderState.getLeaderState().addApprovedRoom(clientID, roomID, sender);
                        }
                        Server destServer = ServerState.getServerState().getServers()
                                .get(sender);
                        try {
                            // send room create approval reply to sender
                            MessageTransfer.sendServer(
                                    ServerMessage.getRoomCreateApprovalReply(String.valueOf(approved), threadID),
                                    destServer
                            );
                            System.out.println("INFO : Room '" + roomID +
                                    "' creation request from client " + clientID +
                                    " is" + (approved ? " " : " not ") + "approved");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (j_object.get("type").equals("roomcreateapprovalreply")) {

                        // non leader processes room create approval request reply received
                        String approved = Boolean.parseBoolean(j_object.get("approved").toString()) ? "yes" : "neutral";
                        Long threadID = Long.parseLong(j_object.get("threadid").toString());

                        ClientThreadHandler clientHandlerThread = ServerState.getServerState()
                                .getClientHandlerThread(threadID);
                        clientHandlerThread.setApprovedRoomCreation(approved);
                        Object lock = clientHandlerThread.getLock();
                        synchronized (lock) {
                            lock.notifyAll();
                        }

                    } else if (j_object.get("type").equals("joinroomapprovalrequest")) {

                        // leader processes join room approval request received

                        //get params
                        String clientID = j_object.get("clientid").toString();
                        String roomID = j_object.get("roomid").toString();
                        String formerRoomID = j_object.get("former").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();
                        boolean isLocalRoomChange = Boolean.parseBoolean(j_object.get("isLocalRoomChange").toString());

                        if (isLocalRoomChange) {
                            //local change update leader
                            ClientState clientState = new ClientState(clientID, roomID, null);
                            LeaderState.getLeaderState().localJoinRoomClient(clientState, formerRoomID);
                        } else {
                            int serverIDofTargetRoom = LeaderState.getLeaderState().getServerIdIfRoomExist(roomID);

                            Server destServer = ServerState.getServerState().getServers().get(sender);
                            try {

                                boolean approved = serverIDofTargetRoom != -1;
                                if (approved) {
                                    LeaderState.getLeaderState().removeClient(clientID, formerRoomID);//remove before route, later add on move join
                                }
                                Server serverOfTargetRoom = ServerState.getServerState().getServers().get(serverIDofTargetRoom);

                                String host = (approved) ? serverOfTargetRoom.getServer_address() : "";
                                String port = (approved) ? String.valueOf(serverOfTargetRoom.getClient_port()) : "";

                                MessageTransfer.sendServer(
                                        ServerMessage.getJoinRoomApprovalReply(
                                                String.valueOf(approved),
                                                threadID, host, port),
                                        destServer
                                );
                                System.out.println("INFO : Join Room from [" + formerRoomID +
                                        "] to [" + roomID + "] for client " + clientID +
                                        " is" + (serverIDofTargetRoom != -1 ? " " : " not ") + "approved");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (j_object.get("type").equals("joinroomapprovalreply")) {

                        // non leader processes room create approval request reply received
                        int approved = Boolean.parseBoolean(j_object.get("approved").toString()) ? 1 : 0;
                        Long threadID = Long.parseLong(j_object.get("threadid").toString());
                        String host = j_object.get("host").toString();
                        String port = j_object.get("port").toString();

                        ClientThreadHandler clientHandlerThread = ServerState.getServerState()
                                .getClientHandlerThread(threadID);

                        Object lock = clientHandlerThread.getLock();

                        synchronized (lock) {
                            clientHandlerThread.setApprovedJoinRoom(approved);
                            clientHandlerThread.setApprovedJoinRoomServerHostAddress(host);
                            clientHandlerThread.setApprovedJoinRoomServerPort(port);
                            lock.notifyAll();
                        }

                    } else if (j_object.get("type").equals("movejoinack")) {
                        //leader process move join acknowledgement from the target room server after change

                        //parse params
                        String clientID = j_object.get("clientid").toString();
                        String roomID = j_object.get("roomid").toString();
                        String formerRoomID = j_object.get("former").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());
                        String threadID = j_object.get("threadid").toString();

                        ClientState client = new ClientState(clientID, roomID, null);
                        LeaderState.getLeaderState().addClient(client);

                        System.out.println("INFO : Moved Client [" + clientID + "] to server s" + sender
                                + " and room [" + roomID + "] is updated as current room");
                    } else if (j_object.get("type").equals("listrequest")) {
                        //leader process list request

                        //parse params
                        String clientID = j_object.get("clientid").toString();
                        String threadID = j_object.get("threadid").toString();
                        int sender = Integer.parseInt(j_object.get("sender").toString());

                        Server destServer = ServerState.getServerState().getServers().get(sender);

                        MessageTransfer.sendServer(
                                ServerMessage.getListResponse(LeaderState.getLeaderState().getRoomIDList(), threadID),
                                destServer
                        );
                    } else if (j_object.get("type").equals("listresponse")) {

                        Long threadID = Long.parseLong(j_object.get("threadid").toString());
                        JSONArray roomsJSONArray = (JSONArray) j_object.get("rooms");
                        ArrayList<String> roomIDList = new ArrayList(roomsJSONArray);

                        ClientThreadHandler clientHandlerThread = ServerState.getServerState()
                                .getClientHandlerThread(threadID);

                        Object lock = clientHandlerThread.getLock();

                        synchronized (lock) {
                            clientHandlerThread.setRoomsListTemp(roomIDList);
                            lock.notifyAll();
                        }
                    } else if (j_object.get("type").equals("deleterequest")) {
                        String ownerID = j_object.get("owner").toString();
                        String roomID = j_object.get("roomid").toString();
                        String mainHallID = j_object.get("mainhall").toString();

                        LeaderState.getLeaderState().removeRoom(roomID, mainHallID, ownerID);

                    } else if (j_object.get("type").equals("quit")) {
                        String clientID = j_object.get("clientid").toString();
                        String formerRoomID = j_object.get("former").toString();
                        // leader removes client from global room list
                        LeaderState.getLeaderState().removeClient(clientID, formerRoomID);
                        System.out.println("INFO : Client '" + clientID + "' deleted by leader");

                    } else if (j_object.get("type").equals("gossip")) {
                        Gossiping.receiveMessages(j_object);

                    } else if (j_object.get("type").equals("startVote")) {
                        Consensus.startVoteMessageHandler(j_object);

                    } else if (j_object.get("type").equals("answervote")) {
                        Consensus.answerVoteHandler(j_object);

                    } else if (j_object.get("type").equals("notifyserverdown")) {
                        Consensus.notifyServerDownMessageHandler(j_object);

                    } else {
                        System.out.println("WARN : Command error, Corrupted JSON from Server");
                    }
                } else {
                    System.out.println("WARN : Command error, Corrupted JSON from Server");
                }
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}