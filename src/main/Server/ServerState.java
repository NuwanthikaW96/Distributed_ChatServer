package main.Server;

import main.ChatRoom.ChatRoom;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class ServerState {
    private static final Logger logger = Logger.getLogger(ServerState.class.getName());

    private String server_id;
    private String server_address;
    private int client_port, coordination_port;
    private int selfID;

    public Server leaderServer;

    private ConcurrentLinkedQueue<String> chatRoomRequsts  = new ConcurrentLinkedQueue<String>();
    private ConcurrentLinkedQueue<String> identityRequsts  = new ConcurrentLinkedQueue<String>();

    private ConcurrentHashMap<String, Server> ServerDictionary = new ConcurrentHashMap<String, Server>();  // list of server names and their attributes
    private ConcurrentHashMap<String, ChatRoom> chatRoomDictionary = new ConcurrentHashMap<String, ChatRoom>(); // list of chat rooms

    private ConcurrentHashMap<String, String> otherServerChatRooms = new ConcurrentHashMap<String, String>();  // list of other avalibale servers and their chat rooms
    private ConcurrentHashMap<String, String> otherServerUsers = new ConcurrentHashMap<String, String>();  // list of all the users and their servers

    private static ServerState serverStateInstance;

    public ServerState(){}

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
                    this.client_port = Integer.parseInt(server_config_list[2]);
                    this.coordination_port = Integer.parseInt(server_config_list[3]);
                    this.selfID = Integer.parseInt(server_config_list[0].substring(1, 2));
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
}
