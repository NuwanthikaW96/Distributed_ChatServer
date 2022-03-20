package main.Server;

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

public class Main {

    /*private static Integer alive_interval = 3;
    private static Integer alive_error_factor = 5;
    private static Boolean isGossip = true;
    private static Integer consensus_interval=10;
    private static Integer consensus_vote_duration=5;*/

    public static void main(String[] args) {

        System.out.println("LOG  : ------server started------");
        ServerState.getServerState().serverInitializeWithConfig("s1", "D:\\Sem 8\\Distributed system\\ChatServer\\src\\main\\config\\server_config.txt");
        try {

            if( ServerState.getServerState().getServer_address() == null ) {
                throw new IllegalArgumentException();
            }

//          server socket for coordination
            ServerSocket CoordinatorServerSocket = new ServerSocket();

//          bind SocketAddress with inetAddress and port
            SocketAddress endPointCoordination = new InetSocketAddress(
                    ServerState.getServerState().getServer_address(),
                    ServerState.getServerState().getCoordination_port()
            );

            CoordinatorServerSocket.bind(endPointCoordination);
            System.out.println(CoordinatorServerSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for coordination on port "+
                    CoordinatorServerSocket.getLocalPort());

            // port open for coordination server socket for client
            ServerSocket ClientServerSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointClient = new InetSocketAddress(
                    ServerState.getServerState().getServer_address(),
                    ServerState.getServerState().getClient_port()
            );
            ClientServerSocket.bind(endPointClient);
            System.out.println(ClientServerSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for clients on port "+
                    ClientServerSocket.getLocalPort()); // port open for clients

        }
        catch( IllegalArgumentException e ) {
            System.out.println("ERROR : invalid server ID");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

}