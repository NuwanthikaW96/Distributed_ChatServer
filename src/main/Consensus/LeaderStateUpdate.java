package main.Consensus;


import main.Message.MessageTransfer;
import main.Message.ServerMessage;
import main.Server.Server;
import main.Server.ServerState;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import java.util.List;

public class LeaderStateUpdate extends Thread {
    int numberOfServersWithLowerIds = ServerState.getServerState().getSelf_id() - 1;
    int numberOfUpdatesReceived = 0;
    volatile boolean leaderUpdateInProgress = true;

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long end = start + 5000;
        try {

            while ( leaderUpdateInProgress ) {
                if( System.currentTimeMillis() > end || numberOfUpdatesReceived == numberOfServersWithLowerIds ) {
                    leaderUpdateInProgress = false;
                    System.out.println("INFO : Leader update completed");

                    // add self clients and chat rooms to leader state
                    List<String> selfClients = ServerState.getServerState().getClientIdList();
                    List<List<String>> selfRooms = ServerState.getServerState().getChatRoomList();

                    for( String clientID : selfClients ) {
                        LeaderState.getLeaderState().addClientLeaderUpdate( clientID );
                    }

                    for( List<String> chatRoom : selfRooms ) {
                        LeaderState.getLeaderState().addApprovedRoom( chatRoom.get( 0 ),
                                chatRoom.get( 1 ), chatRoom.get( 2 )) ;
                    }
                    System.out.println("INFO : Finalized clients: " + LeaderState.getLeaderState().getClientIDList() +
                            ", rooms: " + LeaderState.getLeaderState().getRoomIDList());

                    // send update complete message to other servers
                    for ( int key : ServerState.getServerState().getServers().keySet() ) {
                        if ( key != ServerState.getServerState().getSelf_id() ){
                            Server destServer = ServerState.getServerState().getServers().get(key);

                            try {
                                MessageTransfer.sendServer(
                                        ServerMessage.getLeaderStateUpdateComplete(ServerState.getServerState().getServer_id()),
                                        destServer
                                );
                                System.out.println("INFO : Sent leader update complete message to s"+destServer.getServer_id());
                            }
                            catch(Exception e) {
                                System.out.println("WARN : Server s"+destServer.getServer_id()+
                                        " has failed, it will not receive the leader update complete message");
                            }
                        }
                    }
                }
                Thread.sleep(10);
            }

        } catch( Exception e ) {
            System.out.println( "WARN : Exception in leader update thread" );
        }

    }

    // update client list and chat room list of leader
    public void receiveUpdate( JSONObject j_object ) {
        numberOfUpdatesReceived += 1;
        JSONArray clientIdList = ( JSONArray ) j_object.get( "clients" );
        JSONArray chatRoomsList = ( JSONArray ) j_object.get( "chatrooms" );
        //System.out.println(chatRoomsList);

        for( Object clientID : clientIdList ) {
            LeaderState.getLeaderState().addClientLeaderUpdate( clientID.toString() );
        }

        for( Object chatRoom : chatRoomsList ) {
            JSONObject j_room = (JSONObject)chatRoom;
            LeaderState.getLeaderState().addApprovedRoom( j_room.get("clientid").toString(),
                    j_room.get("roomid").toString(), j_room.get("serverid").toString());
        }
    }
}