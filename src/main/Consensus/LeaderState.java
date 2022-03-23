package main.Consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.ChatRoom.ChatRoom;
import main.Server.ServerState;

public class LeaderState {
    private Integer leader_id;

    private final List<String> activeClientsList = new ArrayList<>();
    private final HashMap<String, ChatRoom> activeChatRooms = new HashMap<>();

    private static LeaderState leaderStateInstance;

    private LeaderState() {
    }

    public static LeaderState getLeaderState() {
        if (leaderStateInstance == null) {
            synchronized (LeaderState.class) {
                if (leaderStateInstance == null) {
                    leaderStateInstance = new LeaderState(); //instance will be created at request time
                }
            }
        }
        return leaderStateInstance;
    }

    public boolean isLeader() {
        return ServerState.getServerState().getSelf_id() == LeaderState.getLeaderState().getLeader_id();
    }

    public boolean isLeaderElected() {
        //TODO: return (using BullyAlgorithm)
        return true;
    }

    public Integer getLeader_id() {
        return leader_id;
    }

    public void setLeader_id( int leader_id ) {
        this.leader_id = leader_id;
    }

    //Remove all rooms and clients by server ID
    public void removeRemoteChatRoomsClientsByServerId(Integer serverId) {
        for (String entry : activeChatRooms.keySet()) {
            ChatRoom remoteRoom = activeChatRooms.get(entry);
            if(remoteRoom.getServer_id()==serverId){
                for(String client : remoteRoom.getClientStateMap().keySet()){
                    activeClientsList.remove(client);
                }
                activeChatRooms.remove(entry);
            }
        }

    }

}