package main.Message;

import main.Consensus.LeaderState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import main.Server.Server;
import main.Server.ServerState;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MessageTransfer {
    public static JSONObject convertToJson(String jsonString) {
        JSONObject j_object = null;
        try {
            JSONParser jsonParser = new JSONParser();
            Object object = jsonParser.parse(jsonString);
            j_object = (JSONObject) object;

        } catch(ParseException  e) {
            e.printStackTrace();
        }
        return j_object;
    }

    //Sending message to the server
    public static void sendServer( JSONObject obj, Server destServer) throws IOException
    {
        Socket socket = new Socket(destServer.getServer_address(),
                destServer.getCoordination_port());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }
    
    //Sending broadcast message
    public static void sendServerBroadcast(JSONObject obj, ArrayList<Server> serverList) throws IOException {
        for (Server each : serverList) {
            Socket socket = new Socket(each.getServer_address(), each.getCoordination_port());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
            dataOutputStream.flush();
        }
    }

    //Sending message to leader server
    public static void sendToLeader(JSONObject obj) throws IOException
    {
        Server destServer = ServerState.getServerState().getServers()
                .get( LeaderState.getLeaderState().getLeader_id() );
        Socket socket = new Socket(destServer.getServer_address(),
                destServer.getCoordination_port());
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write((obj.toJSONString() + "\n").getBytes( StandardCharsets.UTF_8));
        dataOutputStream.flush();
    }
}
