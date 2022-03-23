package main.Message;

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
}

