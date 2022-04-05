package main.Message;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class ServerMessage {

    private static ServerMessage instance = null;

    private ServerMessage() {
    }

    public static synchronized ServerMessage getServerMessage() {
        if (instance == null) instance = new ServerMessage();
        return instance;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getApprovalNewID(String approve) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "newidentity");
        jsonObject.put("approved", approve);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomOnCreate(String clientID, String MainHall) {
        return getJoinRoom(clientID, "", MainHall);
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoom(String clientID, String formerRoomID, String roomID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("roomid", roomID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCreateRoom(String roomID, String approve) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "createroom");
        jsonObject.put("roomid", roomID);
        jsonObject.put("approved", approve);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCreateRoomChange(String clientID, String former, String roomID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomchange");
        jsonObject.put("identity", clientID);
        jsonObject.put("former", former);
        jsonObject.put("roomid", roomID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getWho(String roomID, List<String> participants, String id) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcontents");
        jsonObject.put("roomid", roomID);
        jsonObject.put("identities", participants);
        jsonObject.put("owner", id);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getList(List<String> rooms) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomlist");
        jsonObject.put("rooms", rooms);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getDeleteRoom(String roomID, String isApproved) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "deleteroom");
        jsonObject.put("roomid", roomID);
        jsonObject.put("approved", isApproved);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getMessage(String id, String content) {
        JSONObject join = new JSONObject();
        join.put("type", "message");
        join.put("identity",id);
        join.put("content",content);
        return join;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject gossipMessage(Integer serverId, HashMap<Integer, Integer> heartbeatCountList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "gossip");
        jsonObject.put("serverId", serverId);
        jsonObject.put("heartbeatCountList", heartbeatCountList);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject startVoteMessage(Integer serverId, Integer suspectServerId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "startVote");
        jsonObject.put("serverId", serverId);
        jsonObject.put("suspectServerId", suspectServerId);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject notifyServerDownMessage(Integer serverId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "notifyserverdown");
        jsonObject.put("serverId", serverId);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject answerVoteMessage(Integer suspectServerId, String vote, Integer votedBy){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "answervote");
        jsonObject.put("suspectServerId", suspectServerId);
        jsonObject.put("votedBy", votedBy);
        jsonObject.put("vote", vote);
        return jsonObject;
    }

    public static JSONObject electionMessage(String messageType, String selfId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "election");
        jsonObject.put("electionMessageType",messageType);
        jsonObject.put("senderServerId", selfId);
        return jsonObject;
    }
    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomRequest(String clientID, String roomID, String formerRoomID, String sender, String threadID, String isLocalRoomChange) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "joinroomapprovalrequest");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        jsonObject.put("isLocalRoomChange", isLocalRoomChange);
        return jsonObject;
    }
    @SuppressWarnings("unchecked")
    public static JSONObject getMoveJoinRequest(String clientID, String roomID, String formerRoomID, String sender, String threadID) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "movejoinack");
        jsonObject.put("sender", sender);
        jsonObject.put("roomid", roomID);
        jsonObject.put("former", formerRoomID);
        jsonObject.put("clientid", clientID);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    public static JSONObject getClientIdApprovalReply(String approved, String threadID) {
        // {"type" : "clientidapprovalreply", "approved" : "1", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    public static JSONObject getRoomCreateApprovalReply(String approved, String threadID) {
        // {"type" : "roomcreateapprovalreply", "approved" : "1", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcreateapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("threadid", threadID);
        return jsonObject;

    }

    public static JSONObject getJoinRoomApprovalReply(String approved, String threadID, String host, String port) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "joinroomapprovalreply");
        jsonObject.put("approved", approved);
        jsonObject.put("host", host);
        jsonObject.put("port", port);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    public static JSONObject getListResponse(Object roomIDList, String threadID) {
        // {"type" : "listresponse", "rooms" : ["room-1","MainHall-s1","MainHall-s2"], "threadid" : 12 }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "listresponse");
        jsonObject.put("threadid", threadID);
        jsonObject.put("rooms", roomIDList);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getLeaderStateUpdate( List<String> clientIdList, List<List<String>> chatRoomList) {
        JSONArray clients = new JSONArray();
        clients.addAll( clientIdList );

        JSONArray chatRooms = new JSONArray();
        for( List<String> chatRoomObj : chatRoomList ) {
            JSONObject chatRoom = new JSONObject();
            chatRoom.put( "clientid", chatRoomObj.get( 0 ) );
            chatRoom.put( "roomid", chatRoomObj.get( 1 ) );
            chatRoom.put( "serverid", chatRoomObj.get( 2 ) );
            chatRooms.add( chatRoom );
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leaderstateupdate");
        jsonObject.put("clients", clients);
        jsonObject.put("chatrooms", chatRooms);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getLeaderStateUpdateComplete(String serverID) {
        // {"type" : "leaderstateupdatecomplete", "serverid" : "s3"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "leaderstateupdatecomplete");
        jsonObject.put("serverid", serverID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static  JSONObject heartbeatMessage(String serverID){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "heartbeat");
        jsonObject.put("serverid", serverID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoomCreateApprovalRequest(String clientID, String roomID, String sender, String threadID) {
        // {"type" : "roomcreateapprovalrequest", "clientid" : "Adel", "roomid" : "jokes", "sender" : "s2", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "roomcreateapprovalrequest");
        jsonObject.put("clientid", clientID);
        jsonObject.put("roomid", roomID);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getClientIdApprovalRequest(String clientID, String sender, String threadID) {
        // {"type" : "clientidapprovalrequest", "clientid" : "Adel", "sender" : "s2", "threadid" : "10"}
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "clientidapprovalrequest");
        jsonObject.put("clientid", clientID);
        jsonObject.put("sender", sender);
        jsonObject.put("threadid", threadID);
        return jsonObject;
    }

}