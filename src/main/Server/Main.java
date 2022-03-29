package main.Server;

import main.Client.ClientThreadHandler;
import main.Heartbeat.Gossiping;
import main.Heartbeat.Consensus;
import main.Heartbeat.Heartbeat;
import main.Model.Constant;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

public class Main {

    private static Integer alive_interval = 3;
    private static Integer alive_error_factor = 5;
    private static Boolean isGossip = true;
    private static Integer consensus_interval=10;
    private static Integer consensus_vote_duration=5;

    public static void main(String[] args) {

        ServerState.getServerState().serverInitializeWithConfig("s1", "D:\\Sem 8\\Distributed system\\ChatServer\\src\\main\\config\\server_config.txt");
        System.out.println("LOG  : ------server started------");

        try {

            if (ServerState.getServerState().getServer_address() == null) {
                throw new IllegalArgumentException();
            }

//          server socket for coordination
            ServerSocket coordinatorServerSocket = new ServerSocket();

//          bind SocketAddress with inetAddress and port
            SocketAddress endPointCoordination = new InetSocketAddress(
                    "0.0.0.0",//ServerState.getInstance().getServerAddress()
                    ServerState.getServerState().getCoordination_port()
            );

            coordinatorServerSocket.bind(endPointCoordination);
            System.out.println(coordinatorServerSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for coordination on port " +
                    coordinatorServerSocket.getLocalPort()); // port open for coordination


            // port open for coordination server socket for client
            ServerSocket ClientServerSocket = new ServerSocket();

            // bind SocketAddress with inetAddress and port
            SocketAddress endPointClient = new InetSocketAddress(
                    ServerState.getServerState().getServer_address(),
                    ServerState.getServerState().getClient_port()
            );
            ClientServerSocket.bind(endPointClient);
            System.out.println(ClientServerSocket.getLocalSocketAddress());
            System.out.println("LOG  : TCP Server waiting for clients on port " +
                    ClientServerSocket.getLocalPort()); // port open for clients

            ServerHandler serverHandler = new ServerHandler(coordinatorServerSocket);

            serverHandler.start();

            Runnable heartbeat = new Heartbeat("Heartbeat");
            new Thread(heartbeat).start();

            if (isGossip) {
                System.out.println("Failure Detection is running GOSSIP mode");
                startGossip();
                startConsensus();
            }

            while (true) {
                Socket clientSocket = ClientServerSocket.accept();
                ClientThreadHandler clientHandlerThread = new ClientThreadHandler(clientSocket);
                // starting the thread
                ServerState.getServerState().addClientHandlerThreadToMap(clientHandlerThread);
                clientHandlerThread.start();
//            }

            }
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR : invalid server ID");
        } catch (IndexOutOfBoundsException  e) {
            System.out.println("ERROR : server arguments not provided");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("ERROR : occurred in main " + Arrays.toString(e.getStackTrace()));
        }
    }

        private static void startGossip() {
        try {

            JobDetail gossipJob = JobBuilder.newJob(Gossiping.class)
                    .withIdentity(Constant.GOSSIP_JOB, "group1").build();

            gossipJob.getJobDataMap().put("aliveErrorFactor", alive_error_factor);

            Trigger gossipTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity(Constant.GOSSIP_JOB_TRIGGER, "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(alive_interval).repeatForever())
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(gossipJob, gossipTrigger);

        } catch (SchedulerException e) {
            System.out.println("ERROR : Error in starting gossiping");
        }
    }

    private static void startConsensus() {
        try {

            JobDetail consensusJob = JobBuilder.newJob(Consensus.class)
                    .withIdentity(Constant.CONSENSUS_JOB, "group1").build();

            consensusJob.getJobDataMap().put("consensusVoteDuration", consensus_vote_duration);

            Trigger consensusTrigger = TriggerBuilder
                    .newTrigger()
                    .withIdentity(Constant.CONSENSUS_JOB_TRIGGER, "group1")
                    .withSchedule(
                            SimpleScheduleBuilder.simpleSchedule()
                                    .withIntervalInSeconds(consensus_interval).repeatForever())
                    .build();

            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(consensusJob, consensusTrigger);

        } catch (SchedulerException e) {
            System.out.println("Error in starting consensus");
        }
    }

}