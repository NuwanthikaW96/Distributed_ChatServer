package main.ChatRoom;

import main.Client.ClientState;
import java.util.ArrayList;

public class ChatRoom {
    private String room_id;
    private String owner;
    private String server_id;

    ArrayList<ClientState> client_list = new ArrayList<ClientState>();

    public ChatRoom(String room_id, String owner, String server_id) {
        this.room_id = room_id;
        this.owner = owner;
        this.server_id = server_id;
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getServer_id() {
        return server_id;
    }

    public void setServer_id(String server_id) {
        this.server_id = server_id;
    }

    public void add_client(ClientState clientState){
        client_list.add(clientState);
    }

    //method to get client list
}
