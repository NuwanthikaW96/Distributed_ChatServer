package main.Client;

import main.ChatRoom.ChatRoom;
import main.Consensus.LeaderState;
import main.Message.MessageTransfer;
import main.Message.ServerMessage;
import main.Server.Server;
import main.Server.ServerState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClientThreadHandler extends Thread{
    private final Socket clientSocket;
    private ClientState clientState;
    private String approvedRoomCreation= "no";
    private String approvedClient_id= "no";
    private int approvedJoinRoom = -1;

    private List<String> roomsListTemp;

    private DataOutputStream dataOutputStream;
    private String serverHostAddressOfApprovedJoinRoom;
    private String serverPortOfApprovedJoinRoom;

    Object lock;

    private boolean quitFlag = false;

    public ClientThreadHandler(Socket clientSocket) {
        String serverId = ServerState.getServerState().getServer_id();
        ServerState.getServerState().getChatRoomDictionary().put("MainHall" + serverId , ServerState.getServerState().getMainHall());

        this.clientSocket = clientSocket;
    }

    public String getClient_id(){return clientState.getClient_id();}

    public void setApprovedRoomCreation(String approvedRoomCreation){
        this.approvedRoomCreation=approvedRoomCreation;
    }

    public void setApprovedClient_id(String approvedClient_id){
        this.approvedClient_id=approvedClient_id;
    }

    public void setApprovedJoinRoom(int approvedJoinRoom){
        this.approvedJoinRoom=approvedJoinRoom;
    }

    private boolean hasKey(JSONObject jsonObject, String key){
        if(jsonObject != null && jsonObject.get(key) != null){
            return true;
        }
        else{
            return false;
        }
    }

    private boolean checkID(String id){
        if (Character.toString(id.charAt(0)).matches("[a-zA-Z]+") && id.matches("[a-zA-Z0-9]+") && id.length() >= 3 && id.length() <= 16){
            return true;
        }
        else{
            return false;
        }
    }

    private void sendBroadCast(JSONObject jsonObject, ArrayList<Socket> socketArrayList) throws IOException {
        for (Socket each : socketArrayList){
            Socket temp_socket = (Socket) each;
            PrintWriter TEMP_OUT = new PrintWriter(temp_socket.getOutputStream());
            TEMP_OUT.println(jsonObject);
            TEMP_OUT.flush();
            System.out.println("Sent to: " + temp_socket.getLocalAddress().getHostName() + temp_socket.getPort());
        }
    }

    private void send(JSONObject obj) throws IOException {
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }

    private void sendMessage(ArrayList<Socket> socketArrayList, String msg, List<String> msgList) throws IOException {
        JSONObject sendToClient = new JSONObject();
        String[] array = msg.split(" ");


        if(array[0].equals("newid")){
            sendToClient = ServerMessage.getApprovalNewID(array[1]);
            send(sendToClient);
        }
        else if (array[0].equals("roomchange")){
            sendToClient = ServerMessage.getCreateRoomChange(array[1], "", array[2]);
            send(sendToClient);
        }
        else if (array[0].equals("createroom")){
            sendToClient = ServerMessage.getCreateRoom(array[1], array[2]);
            send(sendToClient);
        }
        else if (array[0].equals("createroomchange")){
            sendToClient = ServerMessage.getCreateRoomChange(array[1], array[2], array[3]);
            sendBroadCast(sendToClient, socketArrayList);
        }
        else if (array[0].equals("roomcontents")){
            sendToClient = ServerMessage.getWho(array[1], msgList, array[2]);
            send(sendToClient);
        }
        else if (array[0].equals("roomlist")){
            sendToClient = ServerMessage.getList(msgList);
            send(sendToClient);
        }
        else if (array[0].equals("deleteroom")){
            sendToClient = ServerMessage.getDeleteRoom(array[1], array[2]);
            send(sendToClient);
        }
        else if (array[0].equals("message")){
            sendToClient = ServerMessage.getMessage(array[1], String.join(" ", Arrays.copyOfRange(array, 2, array.length)));
            sendBroadCast(sendToClient, socketArrayList);
        }
    }

    private void createRoom(String newRoomId, Socket connected, String jsonStringFromClient) throws IOException {

        // TODO:Leader election

        if(approvedRoomCreation=="yes"){
            System.out.println("INFO : Received correct room ID ::" + jsonStringFromClient);

            String formerRoomId = clientState.getRoom_id();

            HashMap<String, ClientState> clientList = ServerState.getServerState().getChatRoomDictionary().get(formerRoomId).getClientStateMap();

            ArrayList<Socket> formerSocketList = new ArrayList<>();

            for (String each: clientList.keySet()){
                if(clientList.get(each).getRoom_id().equals(formerRoomId)){
                    formerSocketList.add(clientList.get(each).getSocket());
                }
            }
            ServerState.getServerState().getRoomMap().get(formerRoomId).removeParticipants(clientState.getClient_id());

            ChatRoom newRoom = new ChatRoom(clientState.getClient_id(), newRoomId, ServerState.getServerState().getServer_id());
            ServerState.getServerState().getRoomMap().put(newRoomId, newRoom);

            clientState.setRoom_id(newRoomId);
            clientState.setOwner(true);
            newRoom.addParticipants(clientState);

            //TODO:update Leader state if self is leader

            synchronized (connected) { //TODO : check sync | lock on out buffer?
                sendMessage(null, "createroom " + newRoomId + " true", null);
                sendMessage(formerSocketList, "createroomchange " + clientState.getClient_id() + " " + formerRoomId + " " + newRoomId, null);
            }
        }else if (approvedRoomCreation=="neutral"){
            System.out.println("WARN : Room id [" + newRoomId + "] already in use");
            sendMessage(null, "createroom " + newRoomId + " false", null);
        }
        else {
            System.out.println("WARN : Recieved wrong room ID type or room ID already in use");
            sendMessage(null, "createroom " + newRoomId + " false", null);
        }

    }

    private void newID(String clientID, Socket connected, String jsonStringFromClient) throws IOException {
        if(checkID(clientID)){
            //wait for leader election

            //get approved by leader
            approvedClient_id = "yes";

            if (approvedClient_id == "yes"){
                System.out.println( "INFO : Received correct ID :" + clientID );
                this.clientState = new ClientState(clientID, ServerState.getServerState().getMainHall().getRoom_id(), clientSocket);
                ServerState.getServerState().getMainHall().addParticipants(clientState);

                //check leader

                String mainHallRoomId = ServerState.getServerState().getMainHall().getRoom_id();
                HashMap<String, ClientState> mainHallClientList = ServerState.getServerState().getChatRoomDictionary().get(mainHallRoomId).getClientStateMap();
                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each : mainHallClientList.keySet()){
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

                synchronized (connected){
                    sendMessage(null, "newid true", null);
                    sendMessage(null, "roomchange " + clientID + " " + "MainHall-"+ ServerState.getServerState().getServer_id(), null);
                }

            }else if (approvedClient_id == "neutral"){
                System.out.println("WARN : ID already in use");
                sendMessage(null, "newid false", null);
            }
            approvedClient_id = "no";
        }
        else{
            System.out.println("WARN : Recieved wrong ID type or ID already in use");
            sendMessage(null, "newid false", null);
        }
    }

    private void list(Socket connected, String jsonStringFromClient) throws IOException {
        roomsListTemp = null;

        //leader election

        //leader make the list

        if (roomsListTemp != null){
            System.out.println("INFO : rooms in the system :");
            sendMessage(null,"roomlist", roomsListTemp);
        }
    }

    private void who(Socket connected, String jsomStringFromClient) throws IOException {
        String roomId = clientState.getRoom_id();
        ChatRoom room = ServerState.getServerState().getRoomMap().get(roomId);
        HashMap<String, ClientState> clientStateMap = room.getClientStateMap();

        List<String> participants = new ArrayList<>(clientStateMap.keySet());
        String owner = room.getOwner();

        System.out.println("LOG  : participants in room [" + roomId + "] : " + participants);
        sendMessage(null, "roomcontents " + roomId + " " + owner, participants);
    }

    // Delete Room
    private void deleteRoom(String roomID) throws IOException {
        String previousRoomID = clientState.getRoom_id();
        String mainHallRoomID = ServerState.getServerState().getMainHall().getRoom_id();

        if (ServerState.getServerState().getRoomMap().containsKey(roomID)) {

            ChatRoom room = ServerState.getServerState().getRoomMap().get(roomID);
            if (room.getOwner().equals(clientState.getClient_id())) {

// client in delete room
                HashMap<String,ClientState> previousClientList = ServerState.getServerState().getRoomMap().get(roomID).getClientStateMap();
                //client in mainHAll
                HashMap<String,ClientState> mainHallClientList = ServerState.getServerState().getRoomMap().get(mainHallRoomID).getClientStateMap();
                mainHallClientList.putAll(previousClientList);

                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each:mainHallClientList.keySet()){
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

//                clientState.setRoom_id(mainHallRoomID);
                ServerState.getServerState().getRoomMap().remove(roomID);
//                ServerState.getServerState().getRoomMap().get(mainHallRoomID).addParticipants(clientState);
                clientState.setOwner(false);


                //broadcast roomchange message to all client
                for(String client:previousClientList.keySet()){
                    String id = previousClientList.get(client).getClient_id();
                    previousClientList.get(client).setRoom_id(mainHallRoomID);
                    ServerState.getServerState().getRoomMap().get(mainHallRoomID).addParticipants(previousClientList.get(client));

                    //+++++ ClientMessageContext
                    sendMessage(socketList, "roomchangeall " + id + " " + roomID + " " + mainHallRoomID, null);
                }


                sendMessage(null, "deleteroom " + roomID + " true", null);

                System.out.println("INFO : room [" + roomID + "] was deleted by : " + clientState.getClient_id());

            } else {
                sendMessage(null, "deleteroom " + roomID + " false", null);
                System.out.println("WARN : Requesting client [" + clientState.getClient_id() + "] does not own the room ID [" + roomID + "]");
            }
            //TODO : check global, room change all members
            // } else if(inAnotherServer){
        }
        else{
            System.out.println("WARN : Received room ID [" + roomID + "] does not exist");
            sendMessage(null, "deleteroom " + roomID + " false", null);
        }
    }

    //Join Room
    private void joinRoom(String roomID) throws IOException, InterruptedException {
        String previousRoomID = clientState.getRoom_id();

        if(clientState.isOwner()){

            System.out.println("WARN : Join room denied, Client" + clientState.getClient_id() + " Owns a room");
//             sendMessage(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.JOIN_ROOM));
        }
        else if(ServerState.getServerState().getRoomMap().containsKey(roomID)){
            clientState.setRoom_id(roomID);
            ServerState.getServerState().getRoomMap().get(previousRoomID).removeParticipants(clientState.getClient_id());
            ServerState.getServerState().getRoomMap().get(roomID).addParticipants(clientState);

            System.out.println("INFO : client [" + clientState.getClient_id() + "] joined room :" + roomID);

            //create broadcast list
            HashMap<String, ClientState> clientListNew = ServerState.getServerState().getRoomMap().get(roomID).getClientStateMap();
            HashMap<String, ClientState> clientListOld = ServerState.getServerState().getRoomMap().get(previousRoomID).getClientStateMap();
            HashMap<String, ClientState> clientList = new HashMap<>();
            clientList.putAll(clientListOld);
            clientList.putAll(clientListNew);

            ArrayList<Socket> SocketList = new ArrayList<>();
            for (String each:clientList.keySet()){
                SocketList.add(clientList.get(each).getSocket());
            }

            sendMessage(SocketList, "roomchangeall " + clientState.getClient_id() + " " + previousRoomID + " " + roomID, null);
            //TODO : show for ones already in room

            //TODO : check global, route and server change
            // } else if(inAnotherServer){
            while(!LeaderState.getLeaderState().isLeaderElected()){
                Thread.sleep(2000);
            }

            if (LeaderState.getLeaderState().isLeader()) {
                LeaderState.getLeaderState().localJoinRoomClient(clientState, previousRoomID);
            } else {
                //update leader server
                MessageTransfer.sendServer(
                        ServerMessage.getJoinRoom(
                                clientState.getClient_id(),
                                roomID,
                                previousRoomID
                        ), ServerState.getServerState().getLeader()
                );
            }
        }else {
            while (!LeaderState.getLeaderState().isLeaderElected()) {
                Thread.sleep(1000);
            }

            approvedJoinRoom = -1;

            if (LeaderState.getLeaderState().isLeader()) {
                int targetRoomServerID = LeaderState.getLeaderState().getServerIdIfRoomExist(roomID);
                if (targetRoomServerID != -1) {
                    approvedJoinRoom = 1;
                } else {
                    approvedJoinRoom = 0;
                }

                if (approvedJoinRoom == 1) {
                    Server targetRoomServer = ServerState.getServerState().getServers().get(targetRoomServerID);
                    serverHostAddressOfApprovedJoinRoom = targetRoomServer.getServer_address();
                    serverPortOfApprovedJoinRoom = String.valueOf(targetRoomServer.getClient_port());
                }

                System.out.println("INFO : Received response for route request for join room (Self is Leader)");

            } else {
                MessageTransfer.sendServer(
                        ServerMessage.getJoinRoom(
                                clientState.getClient_id(),
                                roomID,
                                previousRoomID
                        ), ServerState.getServerState().getLeader()
                );

                synchronized (lock) {
                    while (approvedJoinRoom == -1) {
                        System.out.println("INFO : Wait until server approve route on Join room request");
                        lock.wait(7000);
                        //wait for response
                    }
                }

                System.out.println("INFO : Received response for route request for join room");
            }
            if (approvedJoinRoom == 1) {
                ServerState.getServerState().removeClient(clientState.getClient_id(), previousRoomID, getId());
                System.out.println("INFO : client [" + clientState.getClient_id() + "] left room :" + previousRoomID);

                HashMap<String, ClientState> clientListOld = ServerState.getServerState().getRoomMap().get(previousRoomID).getClientStateMap();
                System.out.println("INFO : Send broadcast to former room in local server");

                ArrayList<Socket> SocketList = new ArrayList<>();
                for (String each : clientListOld.keySet()) {
                    SocketList.add(clientListOld.get(each).getSocket());
                }

                //sendMessage(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));

                //server change : route
                //sendMessage(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.ROUTE));
                System.out.println("INFO : Route Message Sent to Client");
                quitFlag = true;
            } else if (approvedJoinRoom == 0) {
                System.out.println("WARN : Received room ID does not exist");
                sendMessage(null, "roomchange " + clientState.getClient_id() + " " + previousRoomID + " " + previousRoomID, null);
            }
            approvedJoinRoom = -1;
        }
    }

    private void moveJoin(String roomID, String formerRoomID, String clientID) throws IOException, InterruptedException {
        roomID = (ServerState.getServerState().getRoomMap().containsKey(roomID))? roomID:ServerState.getServerState().getMainHallID();
        this.clientState = new ClientState(clientID, roomID, clientSocket);
        ServerState.getServerState().getRoomMap().get(roomID).addParticipants(clientState);

        //create broadcast list
        HashMap<String, ClientState> clientListNew = ServerState.getServerState().getRoomMap().get(roomID).getClientStateMap();

        ArrayList<Socket> SocketList = new ArrayList<>();
        for (String each : clientListNew.keySet()) {
            SocketList.add(clientListNew.get(each).getSocket());
        }



//        messageSend(null, msgCtx.setMessageType(CLIENT_MSG_TYPE.SERVER_CHANGE));
//        messageSend(SocketList, msgCtx.setMessageType(CLIENT_MSG_TYPE.BROADCAST_JOIN_ROOM));


        while (!LeaderState.getLeaderState().isLeaderElected()) {
            Thread.sleep(1000);
        }

        //if self is leader update leader state directly
        if (LeaderState.getLeaderState().isLeader()) {
            ClientState client = new ClientState(clientID, roomID, null);
            LeaderState.getLeaderState().clientAdd(client);
        } else {
            //update leader server
            MessageTransfer.sendToLeader(
                    ServerMessage.getMoveJoinRequest(
                            clientState.getClient_id(),
                            roomID,
                            formerRoomID,
                            String.valueOf(ServerState.getServerState().getSelf_id()),
                            String.valueOf(this.getId())
                    )
            );
        }

    }
    private void message(String content, Socket connected, String fromClient) throws IOException {
        String id = clientState.getClient_id();
        String roomId = clientState.getRoom_id();

        HashMap<String, ClientState> clientList = ServerState.getServerState().getChatRoomDictionary().get(roomId).getClientStateMap();

        ArrayList<Socket> roomList = new ArrayList<>();

        for (String each: clientList.keySet()){
            if (clientList.get(each).getRoom_id().equals(roomId) && !clientList.get(each).getClient_id().equals(id)){
                roomList.add(clientList.get(each).getSocket());
            }
        }
        sendMessage(roomList, "message "+ id + " " + content, null);
    }

    private void quit() throws IOException {
        if (clientState.isOwner()){
            deleteRoom(clientState.getRoom_id());
            System.out.println("INFO : Deleted room before " + clientState.getClient_id() + " quit");
        }
        HashMap<String,ClientState> formerClientList = ServerState.getServerState().getChatRoomDictionary().get(clientState.getRoom_id()).getClientStateMap();
        ArrayList<Socket> socketList = new ArrayList<>();
        for (String each: formerClientList.keySet()){
            socketList.add(formerClientList.get(each).getSocket());
        }
        sendMessage(socketList, "quit", null);

        ServerState.getServerState();


    }

    @Override
    public void run(){
        try {
            System.out.println("INFO : THE CLIENT" + " " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + " IS CONNECTED ");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            this.dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

            while (true){
                String jsonStringFromClient = bufferedReader.readLine();

                if (jsonStringFromClient.equalsIgnoreCase("Exit")){
                    break;
                }

                try{
                    Object object = null;
                    JSONParser jsonParser = new JSONParser();
                    object = jsonParser.parse(jsonStringFromClient);
                    JSONObject jsonObject = (JSONObject) object;

                    if(hasKey(jsonObject, "type")){
                        if (jsonObject.get("type").equals("newidentity") && jsonObject.get("identity") != null){
                            String newClientId = jsonObject.get("identity").toString();
                            newID(newClientId,clientSocket,jsonStringFromClient);
                        }
                        if (jsonObject.get("type").equals("createroom") && jsonObject.get("roomid") != null){
                            String newRoomId = jsonObject.get("roomid").toString();
                            createRoom(newRoomId,clientSocket,jsonStringFromClient);
                        }
                        if (jsonObject.get("type").equals("who")) {
                            who(clientSocket, jsonStringFromClient);
                        } //check list
                        if (jsonObject.get("type").equals("list")) {
                            list(clientSocket, jsonStringFromClient);
                        } //check join room
                        if (jsonObject.get("type").equals("joinroom")) {
                            String roomID = jsonObject.get("roomid").toString();
                            joinRoom(roomID);
                        }
                        if (jsonObject.get("type").equals("deleteroom")) {
                            String roomID = jsonObject.get("roomid").toString();
                            deleteRoom(roomID);
                        }
                        if (jsonObject.get("type").equals("message")) {
                            String content = jsonObject.get("content").toString();
                            message(content, clientSocket, jsonStringFromClient);
                        }
                    }
                    else {
                        System.out.println("WARN : Command error, Corrupted JSON");
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setApprovedClientID(int approved) {
    }

    public Object getLock() {
        return lock;
    }

    public void setApprovedJoinRoomServerHostAddress(String host) {
    }

    public void setApprovedJoinRoomServerPort(String port) {
    }

    public void setRoomsListTemp(ArrayList<String> roomIDList) {
    }
}
