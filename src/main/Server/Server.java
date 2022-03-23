package main.Server;

import main.ChatRoom.ChatRoom;
import java.util.ArrayList;

public class Server {
    private String server_id;
    private int coordination_port;
    private String server_address;
    private int client_port;

    ArrayList<ChatRoom> chat_room_list = new ArrayList<ChatRoom>();

    public Server(String server_id, String server_address, int client_port, int cordination_port) {
        this.server_id = server_id;
        this.server_address = server_address;
        this.client_port = client_port;
        this.coordination_port = cordination_port;
        System.out.println(server_id + " server created");
    }

    public String getServer_id() {
        return server_id;
    }

    public void setServer_id(String server_id) {
        this.server_id = server_id;
    }

    public String getServer_address() {
        return server_address;
    }

    public void setServer_address(String server_address) {
        this.server_address = server_address;
    }
 
    public int getClient_port() {
        return client_port;
    }

    public void setClient_port(int client_port) {
        this.client_port = client_port;
    }

    public int getCoordination_port() {
        return coordination_port;
    }

    public void setCordination_port(int coordination_port) {
        this.coordination_port = coordination_port;
    }
}
