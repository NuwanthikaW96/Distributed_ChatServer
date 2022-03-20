package main.java.server;

public class Room {

    private int roomID;
    private int ownerID;
    private int serverID;

    public Room(int roomID, int ownerID, int serverID) {
        this.roomID = roomID;
        this.ownerID = ownerID;
        this.serverID = serverID;
    }

    public int getRoomID() {return roomID;}

    public void setRoomID(int roomID) {this.roomID = roomID;}

    public int getOwnerID() {return ownerID;}

    public void setOwnerID(int ownerID) {this.ownerID =ownerID;}

    public int getServerID() {return serverID;}

    public void setServerID(int serverID) {this.serverID = serverID;}


}
