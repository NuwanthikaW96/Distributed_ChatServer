package main.Server;

import main.ChatRoom.ChatRoom;
import main.Client.ClientThreadHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static java.lang.Integer.parseInt;

public class ServerState {
    private static final Logger logger = Logger.getLogger(ServerState.class.getName());

    private String server_id;
    private String server_address;
    private int client_port, coordination_port;
    private int self_id;

    private AtomicBoolean ongoingConsensus = new AtomicBoolean(false);

    private ConcurrentHashMap<Integer, String> suspectList = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> heartbeatCountList = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> voteSet = new ConcurrentHashMap<>();
    
    private ConcurrentHashMap<Integer, Server> servers = new ConcurrentHashMap<>(); // list of other servers

    public Server leaderServer;
    private ChatRoom mainHall;

    private ConcurrentLinkedQueue<String> chatRoomRequsts  = new ConcurrentLinkedQueue<String>();
    private ConcurrentLinkedQueue<String> identityRequsts  = new ConcurrentLinkedQueue<String>();

    private ConcurrentHashMap<String, Server> ServerDictionary = new ConcurrentHashMap<String, Server>();  // list of server names and their attributes
    private ConcurrentHashMap<String, ChatRoom> chatRoomDictionary = new ConcurrentHashMap<String, ChatRoom>(); // list of chat rooms

    private ConcurrentHashMap<String, String> otherServerChatRooms = new ConcurrentHashMap<String, String>();  // list of other avalibale servers and their chat rooms
    private ConcurrentHashMap<String, String> otherServerUsers = new ConcurrentHashMap<String, String>();  // list of all the users and their servers

    private final ConcurrentHashMap<Long, ClientThreadHandler> clientHandlerThreadMap = new ConcurrentHashMap<>();

    private static ServerState serverStateInstance;

    public ServerState(){}

    public static String getMainHallIDbyServerInt(int sender) {
    }

    public ChatRoom getMainHall() {
        return mainHall;
    }

    public static ServerState getServerState(){
        if (serverStateInstance != null){
            return serverStateInstance;
        }
        else{
            synchronized(ServerState.class){     // avoiding multiple threads accessing this block of code
                serverStateInstance = new ServerState();
            }
            return serverStateInstance;
        }
    }

    public void serverInitializeWithConfig(String server_id, String config_file_path){
        try{
            File configFile = new File(config_file_path);
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] server_config_list = data.split("\\s+");
                if (server_config_list[0].equals(server_id)){
                    this.server_address = server_config_list[1];
                    this.client_port = parseInt(server_config_list[2]);
                    this.coordination_port = parseInt(server_config_list[3]);
                    this.self_id = parseInt(server_config_list[0].substring(1, 2));
                }
                Server server = new Server(server_id, server_address, client_port, coordination_port);
                ServerDictionary.put(server.getServer_id(), server);
                System.out.println(ServerDictionary);

            }
            scanner.close();

        } catch (FileNotFoundException e) {
            System.out.println("Config file not found");
            e.printStackTrace();
        }
        this.mainHall = new ChatRoom("MainHall-" + server_id , "default-" + server_id);
        this.chatRoomDictionary.put("MainHall-" + server_id, mainHall);
    }

    public boolean isClientIDAlreadyTaken(String clientID){
        for (Map.Entry<String, ChatRoom> entry : this.getChatRoomDictionary().entrySet()) {
            ChatRoom room = entry.getValue();
            if (room.getClientStateMap().containsKey(clientID)) return true;
        }
        return false;
    }

    public void addClientHandlerThreadToMap(ClientThreadHandler clientHandlerThread) {
        clientHandlerThreadMap.put(clientHandlerThread.getId(), clientHandlerThread);
    }

    public ClientThreadHandler getClientHandlerThread(Long threadID) {
        return clientHandlerThreadMap.get(threadID);
    }

    // used for updating leader client list when newly elected
    public List<String> getClientIdList() {
        List<String> clientIdList = new ArrayList<>();
        chatRoomDictionary.forEach((roomID, room) -> {
            clientIdList.addAll(room.getClientStateMap().keySet());
        });
        return clientIdList;
    }

    // used for updating leader chat room list when newly elected
    public List<List<String>> getChatRoomList() {
        // [ [clientID, roomID, serverID] ]
        List<List<String>> chatRoomList = new ArrayList<>();
        for (ChatRoom room: chatRoomDictionary.values()) {
            List<String> roomInfo = new ArrayList<>();
            roomInfo.add( room.getOwner());
            roomInfo.add( room.getRoom_id() );
            roomInfo.add( String.valueOf(room.getServer_id()) );

            chatRoomList.add( roomInfo );
        }
        return chatRoomList;
    }

    public void removeClient (String clientID, String formerRoom, Long threadID){
        this.chatRoomDictionary.get(formerRoom).removeParticipants(clientID);
        this.clientHandlerThreadMap.remove(threadID);
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

    public void setCoordination_port(int coordination_port) {
        this.coordination_port = coordination_port;
    }

    public int getSelf_id() {
        return self_id;
    }

    public ConcurrentHashMap<Integer, Server> getServers() {
        return servers;
    }

    public ConcurrentLinkedQueue<String> getChatRoomRequsts() {
        return chatRoomRequsts;
    }

    public void setChatRoomRequsts(ConcurrentLinkedQueue<String> chatRoomRequsts) {
        this.chatRoomRequsts = chatRoomRequsts;
    }

    public ConcurrentLinkedQueue<String> getIdentityRequsts() {
        return identityRequsts;
    }

    public void setIdentityRequsts(ConcurrentLinkedQueue<String> identityRequsts) {
        this.identityRequsts = identityRequsts;
    }

    public ConcurrentHashMap<String, Server> getServerDictionary() {
        return ServerDictionary;
    }

    public void setServerDictionary(ConcurrentHashMap<String, Server> serverDictionary) {
        ServerDictionary = serverDictionary;
    }

    public ConcurrentHashMap<String, ChatRoom> getChatRoomDictionary() {
        return chatRoomDictionary;
    }

    public void setChatRoomDictionary(ConcurrentHashMap<String, ChatRoom> chatRoomDictionary) {
        this.chatRoomDictionary = chatRoomDictionary;
    }

    public ConcurrentHashMap<String, String> getOtherServerChatRooms() {
        return otherServerChatRooms;
    }

    public void setOtherServerChatRooms(ConcurrentHashMap<String, String> otherServerChatRooms) {
        this.otherServerChatRooms = otherServerChatRooms;
    }

    public ConcurrentHashMap<String, String> getOtherServerUsers() {
        return otherServerUsers;
    }

    public void setOtherServerUsers(ConcurrentHashMap<String, String> otherServerUsers) {
        this.otherServerUsers = otherServerUsers;
    }

    public void setLeader(Server leader) {
        this.leaderServer = leader;
    }

    public Server getLeader() {
        return leaderServer;
    }

    public boolean isLeaderElected() {
        if(leaderServer != null) {
            return true;
        }
        return false;
    }

    public ConcurrentHashMap<String, ChatRoom> getRoomMap() {
        return chatRoomDictionary;
    }

    public synchronized void removeServerInSuspect_list(Integer serverId) {
        suspectList.remove(serverId);
    }

    public ConcurrentHashMap<Integer, String> getSuspect_list() {
        return suspectList;
    }

    public synchronized void removeServerInCount_list(Integer serverId) {
        heartbeatCountList.remove(serverId);
    }

    public ConcurrentHashMap<Integer, Integer> getHeartbeatCount_list() {
        return heartbeatCountList;
    }
    
    public AtomicBoolean onGoingConsensus() {
        return ongoingConsensus;
    }

    public ConcurrentHashMap<String, Integer> getVote_set() {
        return voteSet;
    }

    public String getMainHallID() {
        return getMainHallIDbyServerInt(this.self_id);
    }
    public static String getMainHallIDbyServerInt(int server) {
        return "MainHall-s" + server;}
        
    public static void removeSuspectServer(String suspectServerId) {
        if (ServerState.getServerState().getServerDictionary().containsKey(suspectServerId)) {
            ServerState.getServerState().removeServer(suspectServerId);
        }

        if(ServerState.getServerState().getHeartbeatCount_list().containsKey(parseInt(suspectServerId))) {
            ServerState.getServerState().removeServerInCountList(suspectServerId);
        }

        if(ServerState.getServerState().getSuspect_list().containsKey(parseInt(suspectServerId))) {
            ServerState.getServerState().removeServerInSuspectList(suspectServerId);
        }

        ServerState.getServerState().otherServerChatRooms.entrySet().removeIf(stringStringEntry -> stringStringEntry.getValue().equals(suspectServerId));
        ServerState.getServerState().otherServerUsers.entrySet().removeIf(stringStringEntry -> stringStringEntry.getValue().equals(suspectServerId));
    }

    public synchronized void removeServer(String serverId) {
        ServerDictionary.remove(serverId);
    }

    public synchronized void removeServerInCountList(String serverId) {
        heartbeatCountList.remove(parseInt(serverId));
    }

    public synchronized void removeServerInSuspectList(String serverName) {
        suspectList.remove(parseInt(serverName));
    }
}
