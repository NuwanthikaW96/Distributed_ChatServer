package main.Client;

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
}
