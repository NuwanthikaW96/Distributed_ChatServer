package main;

import main.Server.ServerState;

public class Main {
    public static void main (String [] args){
        ServerState s = new ServerState();
        s.serverInitializeWithConfig("s1", "E:\\Semester 8\\Distributed Systems\\chatServerMaster\\src\\main\\config\\server_config.txt");

    }
}
