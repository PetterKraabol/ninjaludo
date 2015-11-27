package com.ludo.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.ludo.config.Config;

/**
 * The game server runs in its own thread and handles all game
 * sessions and their players.
 * @author Petter
 *
 */
public class GameServer extends Thread {
    
    /**
     * Game server listener
     */
    private ServerSocket listener;
    
    /**
     * Config settings for loading and settings config keys and values
     */
    private Config config = new Config();
    
    /**
     * Game Server constructor
     */
    public GameServer() {
        System.out.println("Game server running on port " + config.getConfig("gamePort"));
    }
    
    /**
     * Run game server
     */
    public void run() {
        
        // Server socket
        try {
            this.listener = new ServerSocket(Integer.parseInt(this.config.getConfig("gamePort")));
        } catch (IOException e) {
            e.printStackTrace();
        }
       
        // Listen for new connections
        try{
            while(true) {
                Game game = new Game();
                
                System.out.println("Waiting for players...");
                
                // Wait for red user
                game.addPlayer(game.new Player(listener.accept(), "red"));
                
                // Wait for blue user
                game.addPlayer(game.new Player(listener.accept(), "blue"));
                
                // Wait for yellow user
                game.addPlayer(game.new Player(listener.accept(), "yellow"));
                
                // Wait for green user
                game.addPlayer(game.new Player(listener.accept(), "green"));
                
                game.broadcast("STARTGAME");
                
                // Start game
                game.start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                listener.close();
            } catch (IOException e) {
                System.out.println("Error closing game server socket");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Every game session runs in their own thread and has at least 4 users with 4 ludo pieces each.
     * @author Petter
     *
     */
    class Game extends Thread  {
        
        /**
         * List of player objects for this gaming session
         */
        private List<Player> players = new ArrayList<Player>();
        
        public Game() {
            System.out.println("New Game");
            
        }
        
        /**
         * Add a new player to the game.
         * @param player
         */
        public void addPlayer(Player player) {
            System.out.println(player.getColor() + " has joined.");
            this.players.add(player);
            
            broadcast("NEWUSERINQUEUE");
        }
        
        /**
         * Broadcast a message to all players in a game session
         * @param message Broadcast message
         * @throws IOException Connection exceptions
         */
        private void broadcast(String message) {
            
            System.out.println("Broadcasting to users: " + message);
            
            // For every player in session; send the message
            for(Player player : players) {
                player.getOut().println(message + " " + player.getColor());
            }
        }
        
        /**
         * Start the game
         */
        public void run() {
            
            String line = null;
            String[] args;
            int dice;
            boolean noWinner = true;
            
            while(noWinner) {
                
                // Cycle players for their turn.
                for (Player player : players) {
                    
                    // Roll dice
                    dice = 1 + (int)(Math.random() * Integer.parseInt(config.getConfig("dice")));
                    
                    // Broadcast that it's player's turn and 
                    broadcast("TURN " + player.getColor() + " " + dice);
                    
                    // Check if player has any possible moves
                    if(!player.canMoveAny(dice)) {
                        continue;
                    }
                    
                    // Listen for a move request from client
                    while(true) {
                        try {
                            // Read input from client
                            line = player.getIn().readLine();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        // Continue if no request
                        if(line == null) {
                            continue;
                        }
                        
                        // Move request: MOVE <piece_id (0-3)>
                        if(line.startsWith("MOVE")) {
                            args = line.split(" ");
                            
                            if(player.movePieceIfAllowed(Integer.parseInt(args[1]), dice)) {
                                broadcast("MOVE " + args[1]);
                                break;
                            } else {
                                // Cannot move piece
                                player.getOut().println("MOVEDENIED");
                            }
                        }
                    }
                    
                    // Check if user has won
                    if(player.hasWon()) {
                        broadcast("WIN");
                        noWinner = false;
                        break;
                    }
                    
                }
            }
            
        }
        
        /**
         * Player class for a game session
         * @author Petter
         *
         */
        class Player extends Thread {
            private Piece[] pieces = new Piece[4];
            private boolean inQueue = true;
            private String color;
            private Socket socket;
            private PrintWriter out;
            private BufferedReader in;
            
            /**
             * Create a player with a connection socket and color
             * @param socket
             * @param color
             */
            public Player(Socket socket, String color) {
                
                System.out.println("New player: " + color);
                
                // Color
                this.color = color;
                
                // Socket
                this.socket = socket;
                
                // Output
                try {
                    this.out = new PrintWriter(this.socket.getOutputStream(), true);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                // Input
                try {
                    this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                // Start player thread
                this.start();
                
            }
            
            /**
             * Check if a player has won, based on their pieces
             * @return
             */
            public boolean hasWon() {
                
                // Check if any piece is done.
                for(Piece piece : pieces) {
                    if(!piece.isDone()) {
                        return false;
                    }
                }
                
                // All pieces are done, player has won!
                return true;
            }

            public boolean canMoveAny(int dice) {
                for(Piece piece : pieces) {
                    if(!piece.validMovie(dice)) {
                        return false;
                    }
                }
                
                return true;
            }

            /**
             * Attempt to move piece if allowed, according to the game rules.
             * @param pieceId Piece ID (0-3)
             * @param steps How many steps to move the piece
             * @return boolean if the piece has moved
             */
            public boolean movePieceIfAllowed(int pieceId, int steps) {
                return this.pieces[pieceId].move(steps);
            }

            /**
             * Return player socket
             * @return socket
             */
            public Socket getSocket() {
                return this.socket;
            }
            
            /**
             * Return player output writer
             * @return
             */
            public PrintWriter getOut() {
                return this.out;
            }
            
            /**
             * Return player input reader
             * @return BufferedReader input from client
             */
            public BufferedReader getIn() {
                return this.in;
            }
            
            /**
             * Get player color
             */
            public String getColor() {
                return this.color;
            }
            
            /**
             * Make user leave or join queue
             * This is usually used for leaving the queue
             */
            public void setQueue(boolean inQueue) {
                this.inQueue = inQueue;
            }
            
            /**
             * Check if user is in queue
             */
            public boolean isInQueue() {
                return this.inQueue;
            }
            
            /**
             * Thread running
             */
            public void run() {
                
                // Waiting for other players
                while(isInQueue()) {
                    out.println("WAITING");
                }
                
            }
        }
        
        /**
         * A Ludo Piece
         * @author Petter
         *
         */
        class Piece {
            private int position;
            
            /**
             * Default ludo peice position is 0 (home)
             */
            public Piece() {
                System.out.println("Piece created");
                this.position = 0;
            }
            
            /**
             * Check if piece can move to according to dice,
             * if so, move the piece back. The result of this
             * function does not affect the piece's position
             * @param dice
             * @return
             */
            public boolean validMovie(int dice) {
                
                // Try moving
                if(this.move(dice)) {
                    
                    // Return valid, but first revert move
                    setPosition(position - dice);
                    return true;
                }
                
                // Can't move
                return false;
                
            }

            /**
             * Move piece a specified amount of steps from its current location
             * @param steps
             * @return boolean if the piece can move or not
             */
            public boolean move(int steps) {
                int targetPos = this.position += steps;
                
                // If the user is trying to move the piece past the end.
                if(targetPos > Integer.parseInt(config.getConfig("mapLength"))) {
                    return false;
                }
                
                // If piece is in home, a 6 is required to move out
                else if(isHome() && steps != 6) {
                    return false;
                }
                
                // Valid move
                else {
                    setPosition(targetPos);
                    return true;
                }
            }
            
            /**
             * Set a position for this piece
             * @param position
             */
            private void setPosition(int position) {
                this.position = position;
            }
            
            /**
             * Get position for this piece
             * @return
             */
            public int getPosition() {
                return this.position;
            }
            
            /**
             * Check if ludo piece is still in home
             * @return
             */
            public boolean isHome() {
                return this.position == 0;
            }
            
            /**
             * Check if ludo piece has received the goal.
             * @return
             */
            public boolean isDone() {
                return this.position == Integer.parseInt(config.getConfig("mapLength"));
            }
        }
    }
}