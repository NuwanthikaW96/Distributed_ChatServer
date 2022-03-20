package main.java.server;

public class Server {

    private int server_id;
    private String server_address;
    private int clients_port;
    private int coordination_port;

    public Server(int server_id, String server_address, int clients_port, int coordination_port) {
        this.server_id = server_id;
        this.server_address = server_address;
        this.clients_port = clients_port;
        this.coordination_port = coordination_port;
    }

    public int getServerID() {return server_id;}

    public void setServerID(int new_server_id) {
        this.server_id = new_server_id;
    }

    public String getServerAddress() {
        return server_address;
    }

    public void setServerAddress(String new_server_address) {
        this.server_address = new_server_address;
    }

    public int getClientPort() {
        return clients_port;
    }

    public void setClientPort(int new_client_port) {
        this.clients_port = new_client_port;
    }

    public int getCoordinationPort() {
        return coordination_port;
    }

    public void setCoordinationPort(int new_coordination_port) {
        this.coordination_port = new_coordination_port;
    }
}
