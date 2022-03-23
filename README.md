# Distributed Chat System - CS4262 Distributed Systems

## Introduction

This group project is about creating a distributed chat application which consists of two main distributed components: chat servers and chat clients, which can run on different hosts. <br/>

**This repository contains the server-side codebase**

### Chat Client

Chat clients are programs that can connect to at most one chat server; which can be any of the available servers. Chat clients can be used to send requests for creating, deleting, joining and quitting a chat room. They can also be used to send requests to see the list of available chat rooms in the system and the list of client identities currently connected to a given chat room. Finally, they can be used to send chat messages to other chat clients connected to the same chat room. <br/>

### Chat Server

Chat servers are programs accepting multiple incoming TCP connections from chat clients. There are multiple servers working together to serve chat clients. The number of servers is fixed and does not change once the system is active. Each server is responsible for managing a subset of the system's chat rooms. In particular, a server manages only those chat rooms that were created locally after receiving a request from a client. In order to join a particular chat room, clients must be connected to the server managing that chat room. As a result, clients are redirected between servers when a client wants to join a chat room managed by a different server. Chat servers are also responsible for broadcasting messages received from clients to all other clients connected to the same chat room.   <br/>

This documentation consists of well-described details on the implementation of a distributed chat system. It contains two main components, the chat server, and the chat client. What we had to do is to develop the server-side to interact with the provided chat client.</br>
By using the server address and the corresponding port, the client can connect to a server via a TCP connection. Then he can request to create, delete, join, and quit a chat room, request the list of participants in that room, and the list of available chat rooms in the system.</br>
The system consists of several servers that can accept multiple TCP connections from different clients. There are several chat rooms inside a particular server that were created according to the requests of the client. The servers are responsible for managing those chat rooms. If there is a client in a server who wants to join a chat room that is in another server, he is redirected to the respective server while ensuring migration transparency is achieved. It is the responsibility of the chat servers to broadcast all the client messages into a particular chat room, to all the participants in that room.
</br>
At the time of the server initialization, there is a leader election among the servers to select a leader. The responsibility of the leader is to maintain the global consistency of the system. Other servers request permission from the leader for some actions to provide transparency. If a non-leader server crashes, the leader must be notified and the server state of that crashed server must be deleted. For this, a heartbeat is implemented using gossiping and consensus where the server failure is detected and handled accordingly.
</br>

## Executable Jar files

The executable jar files of the client and server are provided in the folder `executables`. <br/>

A chat client can be executed as: <br/>
```java -jar client.jar -h server_address [-p server_port] -i identity [-d]``` <br/>
eg: java -jar client.jar -h localhost -p 4444 -i Adel <br/>

A chat server can be executed as: <br/>
```java -jar server.jar [server_name] "[location of server configuration file]"``` <br/>
eg: java -jar server.jar s1 "C:code\src\main\java\config\server_conf.txt"

## Instructions to Build the executable Jar

Development Environment - `IntelliJ IDEA`

install java (version `1.6`)
\
install Maven (version `3.8.1`)

run the following commands to install dependencies and build

`mvn clean install `
\
`mvn clean compile assembly:single`

The output jar will be created inside the `'target'` folder named `Distributed-Chat-System-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Instructions to Run the Jar

run the following command in a terminal

`java -jar Distributed-Chat-System-1.0-SNAPSHOT-jar-with-dependencies.jar s1 "C:code\src\main\java\config\server_conf.txt"`

note `s1` should be changed according to the server instance
\
note the path to the `server_conf.txt` should be given according to the configuration file location
