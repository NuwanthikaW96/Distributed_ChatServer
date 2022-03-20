package main.Client;

import java.net.Socket;

public class ClientState {
    private String client_id;
    private String room_id;
    private Socket socket;
    private boolean isOwner = false;

    public ClientState(String client_id, String room_id, Socket socket) {
        this.client_id = client_id;
        this.room_id = room_id;
        this.socket = socket;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }
}
