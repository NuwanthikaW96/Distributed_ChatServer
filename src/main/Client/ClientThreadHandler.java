package main.Client;

import main.ChatRoom.ChatRoom;
import main.Message.ServerMessage;
import main.Server.ServerState;
import org.json.simple.JSONObject;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClientThreadHandler extends Thread{
    private final Socket clientSocket;
    private ClientState clientState;

    private DataOutputStream dataOutputStream;

    public ClientThreadHandler(Socket clientSocket) {
        String serverId = ServerState.getServerState().getServer_id();
        ServerState.getServerState().getChatRoomDictionary().put("MainHall" + serverId , ServerState.getServerState().getMainHall());

        this.clientSocket = clientSocket;
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
            sendToClient = ServerMessage.getCreateRoomChange(array[1], array[2].replace("_",""), array[3]);
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
            sendBroadCast(sendToClient, socketArrayList);;
        }
    }

    private void createRoom(String newRoomId, Socket connected, String jsonStringFromClient) throws IOException {
        if(newRoomId != null && ServerState.getServerState().getChatRoomDictionary().containsKey(newRoomId)){
            System.out.println("INFO : Received correct room ID ::" + jsonStringFromClient);

            String formerRoomId = clientState.getRoom_id();

            HashMap<String, ClientState> clientList = ServerState.getServerState().getChatRoomDictionary().get(formerRoomId).getClientStateMap();

            ArrayList<Socket> formerSocketList = new ArrayList<>();

            for (String each: clientList.keySet()){
                if(clientList.get(each).getRoom_id().equals(formerRoomId)){
                    formerSocketList.add(clientList.get(each).getSocket());
                }
            }
            ServerState.getServerState().getRoomMap().get(formerRoomId).removeParticipants(clientState);

            ChatRoom newRoom = new ChatRoom(clientState.getClient_id(), newRoomId);
            ServerState.getServerState().getRoomMap().put(newRoomId, newRoom);

            clientState.setRoom_id(newRoomId);
            newRoom.addParticipants(clientState);

            synchronized (connected) { //TODO : check sync | lock on out buffer?
                sendMessage(null, "createroom " + newRoomId + " true", null);
                sendMessage(formerSocketList, "createroomchange " + clientState.getClient_id() + " " + formerRoomId + " " + newRoomId, null);
            }
        } else {
            System.out.println("WARN : Recieved wrong room ID type or room ID already in use");
            sendMessage(null, "createroom " + newRoomId + " false", null);
        }

    }

    private void newID(String clientID, Socket connected, String jsonStringFromClient) throws IOException {
        if (checkID(clientID) && !ServerState.getServerState().isClientIDAlreadyTaken(clientID)){
            System.out.println("INFO : Received correct ID ::" + jsonStringFromClient);

            this.clientState = new ClientState(clientID, ServerState.getServerState().getMainHall().getRoom_id(),connected);
            ServerState.getServerState().getMainHall().addParticipants(clientState);

            synchronized (connected){
                sendMessage(null, "newid true", null);
                sendMessage(null, "roomchange" + clientID + "_" + "MainHall-"+ ServerState.getServerState().getServer_id(), null);
            }
        }
        else{
            System.out.println("WARN : Recieved wrong ID type or ID already in use");
            sendMessage(null, "newid false", null);
        }
    }

    private void list(Socket connected, String jsonStringFromClient) throws IOException {
        List<String> roomsList = new ArrayList<>(ServerState.getServerState().getRoomMap().keySet());
        System.out.println("INFO : rooms in the system :");
        sendMessage(null,"roomlist", roomsList);
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
}