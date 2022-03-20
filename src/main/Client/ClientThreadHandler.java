package main.Client;

import main.Message.ServerMessage;
import main.Server.ServerState;
import main.ChatRoom.ChatRoom;
import org.json.simple.JSONObject;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

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

// Delete Room
    private void deleteRoom(int roomID) throws IOException {
        String formerRoomID = clientState.getRoom_id();

        if (ServerState.getServerState().getRoomMap().containsKey(roomID)) {

            ChatRoom room = ServerState.getServerState().getRoomMap().get(roomID);
            if (room.getOwner().equals(clientState.getClient_id())) {

                String mainHallRoomID = ServerState.getServerState().getMainHall().getRoom_id();

                HashMap<String,ClientState> formerClientList = ServerState.getServerState().getRoomMap().get(roomID).getClientStateMap();
                HashMap<String,ClientState> mainHallClientList = ServerState.getServerState().getRoomMap().get(mainHallRoomID).getClientStateMap();
                mainHallClientList.putAll(formerClientList);

                ArrayList<Socket> socketList = new ArrayList<>();
                for (String each:mainHallClientList.keySet()){
                    socketList.add(mainHallClientList.get(each).getSocket());
                }

                clientState.setRoom_id(mainHallRoomID);
                ServerState.getServerState().getRoomMap().remove(roomID);
                ServerState.getServerState().getRoomMap().get(mainHallRoomID).addParticipants(clientState);

                for(String client:formerClientList.keySet()){
                    String id = formerClientList.get(client).getClient_id();
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
}
