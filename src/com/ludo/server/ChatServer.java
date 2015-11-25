/**
 * 
 */
package com.ludo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * @author Petter
 *
 */
public class ChatServer {
    
    /**
     * Chat server port
     */
    private static final int port = 4040;
    
    /**
     * List of connected user names
     */
    private static HashSet<String> users = new HashSet<String>();
    
    /**
     * List of writers for broadcasting messages to every user
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    
    /**
     * The chat server contrusctor listens for new connections on
     * a specified port and creates new Handler objects
     * to handle server communications.
     * @throws Exception
     */
    public ChatServer() throws Exception {
        System.out.println("Chat server listening on port " + port);
        ServerSocket listener = new ServerSocket(port);
        try {
            while(true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }
    
    /**
     * Handles communication between a client and the server.
     * @author Petter
     *
     */
    private static class Handler extends Thread {
        private String username;
        private String password;
        private String request;
        private String[] args;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private UserHandler userHandler = new UserHandler();
        
        /**
         * Sets the socket for communication between the new
         * client and the server.
         * @param socket
         */
        public Handler(Socket socket) {
            System.out.println("Handling a new connection");
            this.socket = socket;
        }
        
        /**
         * Running the chat communication for input and outputs
         */
        public void run() {
            try {
                
                // Input reader (from client)
                this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                
                // Output printer (to client)
                this.out = new PrintWriter(this.socket.getOutputStream(), true);
                
                /**
                 * First, send a login request command REQUESTLOGIN
                 * continuously to client until login data has been received.
                 */
                while(true) {
                    out.println("LOGINREQUEST");
                    this.request = in.readLine();
                    
                    // If no login data is received, keep reading inputs
                    if (request == null) {
                        return;
                    }
                    
                    System.out.println(request);
                    
                    // Split incoming message
                    this.args = this.request.split(" ");
                    
                    // If client is attempting to log in with LOGIN <username> <password>
                    if(this.request.startsWith("LOGIN") && args.length == 3) {
                        
                        System.out.println("Login reuqest:" + request);
                        
                        this.username = args[1];
                        this.password = args[2];
                        
                        // Attempt to authenticate user
                        synchronized(users) {
                            if(!users.contains(username)) {
                                if(userHandler.authenticateUser(username, password)) {
                                    users.add(username);
                                    break;
                                } else {
                                    out.println("LOGINDENIED");
                                }
                            } else {
                                out.println("LOGINDENIED");
                            }
                        }
                        
                    }
                }
                
                // Report back to client that login authentication succeeded.
                this.out.println("LOGINACCEPTED");
                writers.add(out);
                
                /**
                 * Handle incoming chat messages from client and
                 * broadcast them to every connected client.
                 */
                
                while(true) {
                    request = in.readLine();
                    args = request.split(" ");
                    
                    if(request.startsWith("MESSAGE")) {
                        for(PrintWriter writer : writers) {
                            writer.println("MESSAGE " + username + " " + request.substring("MESSAGE ".length()));
                        }
                    }
                    
                }
                
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                
                if(username != null) {
                    users.remove(this.username);
                }
                
                if(out != null) {
                    writers.remove(this.out);
                }
                
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket for " + username + ": " + e);
                }
            }
        }
    }
    
}
