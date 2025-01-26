/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tictactoeserver;

import Game.GameManager;
import db.DAO;
import dto.DTOPlayer;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javafx.application.Platform;

/**
 *
 * @author Mohamed Sameh
 */


public class ClientHandler extends Thread{

    // ear
    public DataInputStream dis;
    // mouth
    public PrintStream ps;
    public Socket soc;
    public String data;
    private JSONObject jsonMsg;
    public static Vector<ClientHandler> clients = new Vector<ClientHandler>();
    public DTOPlayer playerData;
    public  static List<String> onlinePlayers = new ArrayList<>();

    
    static Map<String, ClientHandler> onlinePlayerSocs = new HashMap<>();
    
    private boolean running = true;
    ServerUIController controlerUI;
     
    public ClientHandler(Socket soc, ServerUIController cont) {
        controlerUI = cont;
        try {
            this.soc = soc;
            dis =  new DataInputStream(soc.getInputStream());
            ps = new PrintStream(soc.getOutputStream());
            playerData = new DTOPlayer();
            ClientHandler.clients.add(this);
            start();
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

@Override
    public void run() {
        try {
            while (true) {
                data = dis.readLine(); // Read data from client
                if (data == null || data.isEmpty()) {
                    System.out.println("Break ");
                    break; // Client disconnected 
                }
                handleJSON(data);
            }
        } catch (IOException | ParseException ex) {
            System.out.println("Client disconnected: " + soc.getInetAddress());
        } finally {
            cleanup(); // Cleanup resources
        }
    }
  
    public void stopHandler() {
        //running = false; 
        try {
            if (soc != null && !soc.isClosed()) 
            {
                soc.close(); // Close the client socket to end the connection
            }
        } catch (IOException e) {
             e.printStackTrace();
        }
       }
    
    
    private void handleJSON(String data) throws ParseException{        
        JSONParser parser = new JSONParser();
        jsonMsg = (JSONObject) parser.parse(data);
        
        
        switch(jsonMsg.get("type").toString()){
            case "register":
                try {
                 System.out.println("Regsitration-----------");
                   int res = DAO.createPlayer(jsonMsg);
                   Map<String, String> result = new HashMap<>();

                   if(res == 1)
                   {
                       playerData.setUsername(jsonMsg.get("username").toString());
                       playerData.setEmail(jsonMsg.get("email").toString());
//                       int upRes = DAO.updateStatus(jsonMsg);
//                       if (upRes == 1) {
//                           System.out.println("Status updateded");
//                       }

                       onlinePlayerSocs.put(playerData.getUsername(), this);
                       System.out.println("Hello----------- " + playerData.getUsername() + " Resgistered successfully");

                       // Set default score to 0
                       playerData.setScore(0);

                       result.put("type", "register");
                       result.put("status", "" + res);

                       result.put("score", ""+playerData.getScore()); // Send default score to the client

                       onlinePlayers.add(playerData.getUsername());
                       controlerUI.addOnlinePlayer(playerData.getUsername());
                   }else {
                    System.out.println("Hello-----------" + jsonMsg.get("username").toString() + " Resgistered failed");
                    result.put("type", "register");
                    result.put("status", "" + res);
                    }
                     sendJSONResponse(result);
                     broadcastOnlineList();
                  } catch (SQLException ex) {
                 Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                  break;
            case "login":
            try {
                System.out.println("Login-----------");
                int res = DAO.validatePlayer(jsonMsg);
                Map<String, String> result = new HashMap<>();

               if(res == 1)
               {
                   playerData.setUsername(jsonMsg.get("username").toString());
                   
                   onlinePlayerSocs.put(playerData.getUsername(), this);
                   System.out.println("Hello-----------" +  playerData.getUsername() + " Login successfully");


                   // Get the player's current score
                   int score = DAO.getScore(playerData.getUsername());
                   playerData.setScore(score);


                   result.put("type", "login");
                   result.put("status", "" + res);
                   result.put("score", "" + score); // Send the score to the client

                   onlinePlayers.add(playerData.getUsername());
                   controlerUI.addOnlinePlayer(playerData.getUsername());
                   }else{
                       System.out.println("Hello-----------" + jsonMsg.get("username").toString() + " Login failed");
                       result.put("type", "login");
                       result.put("status", "" + res);
                       }

                   sendJSONResponse(result);
                   broadcastOnlineList();
                   }catch(SQLException ex) {
                   Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                   }
                break;

            case "sendGameReq":
                String challenged = jsonMsg.get("challenged").toString();
                String challenger = jsonMsg.get("challenger").toString();
                System.out.println("player "+  challenged + " is challanged");
                Map<String, String> req = new HashMap<>();

                req.put("type", "receiveGameReq");
                req.put("challenger", challenger);
                
                JSONObject reqJSON = new JSONObject();
                reqJSON.putAll(req);
                onlinePlayerSocs.get(challenged).ps.println(reqJSON.toJSONString());
                
                break;

            case "gameReqResponse":
                String challenger1 = jsonMsg.get("challenger").toString();
                String challenged1 = jsonMsg.get("challenged").toString();
                String status = jsonMsg.get("status").toString();
                
                // Find both players' handlers
                ClientHandler challengerHandler = onlinePlayerSocs.get(challenger1);
                ClientHandler challengedHandler = onlinePlayerSocs.get(challenged1);
                
                if(challengerHandler != null){
                    Map<String, String> notification = new HashMap<>();
                    notification.put("type", "gameReqResult");
                    notification.put("status", status);
                    notification.put("challenged", challenged1);
                    
               
                    if (status.equals("accepted")) {
                       
                            GameManager.startNewGame(challengerHandler, challengedHandler,controlerUI);
                            broadcastOnlineList();
                    }

                    challengerHandler.sendJSONResponse(notification);
                }
                break;
            case "forfeitGameReq":                
                GameManager.handleGameForfeit(this);
                break;
            case "gameMove":
                System.out.println(jsonMsg.get("symbol"));
                GameManager.handleMove(this, jsonMsg);
                break;
            case "logout":
                System.out.println(jsonMsg.get("username")+"logout");
                onlinePlayers.remove(jsonMsg.get("username").toString());
                controlerUI.removeOnlinePlayer(jsonMsg.get("username").toString());
                broadcastOnlineList();
                
                break; 
            case "serverShutdown": 
                System.out.println("Server is shutting down. Disconnecting...");
                // Notify the client that the server is shutting down
                Map<String, String> shutdownMsg = new HashMap<>();
                shutdownMsg.put("type", "serverShutdown");
                shutdownMsg.put("message", "Server is shutting down. Please reconnect later.");
                sendJSONResponse(shutdownMsg);
                cleanup(); // Cleanup resources and disconnect
                break;
            default:
                System.out.println("Unhandled message type: " + jsonMsg.get("type").toString());

        }
    }
        

            
       
    
    public void sendJSONResponse(Map<String, String> fields) {
        JSONObject data = new JSONObject();
        data.putAll(fields);
        this.ps.println(data.toJSONString());
    }
    
    public void sendMessage(String data) {
        this.ps.println(data);
    }
    
  public static void broadcastOnlineList() {
    synchronized (clients) {
        synchronized (onlinePlayers) {
            // Remove players who are also in gamers
            onlinePlayers.removeIf(GameManager.gamePlayers::contains);

            // Construct the message
            JSONObject message = new JSONObject();
            message.put("type", "onlinePlayers");
            message.put("players", new ArrayList<>(onlinePlayers));

            System.out.println("broadcastOnlineList " + onlinePlayers);

            // Create a copy of clients to avoid concurrent modification
            List<ClientHandler> clientsCopy = new ArrayList<>(clients);
            for (ClientHandler client : clientsCopy) {
                try {
                    client.ps.println(message.toJSONString());
                } catch (Exception e) {
                    System.out.println("Error broadcasting to client: " + e.getMessage());
                }
            }
        }
    }
}

    
    private void cleanup() {
        try {
            // Remove the player from the onlinePlayers list
            if (playerData != null && playerData.getUsername() != null) {
                String username = playerData.getUsername();
                System.out.println("Starting cleanup for player: " + username);
                
                synchronized(clients) {
                    onlinePlayers.remove(username);
                    onlinePlayerSocs.remove(username);
                    clients.remove(this);
                }
                
                Platform.runLater(() -> {
                    controlerUI.removeOnlinePlayer(username);
                });
                
                if(controlerUI.isInGame(username)){
                    
                    GameManager.handlePlayerDisconnection(this);
                    Platform.runLater(() -> {
                        controlerUI.removeInGamePlayer(username);
                    });
                }

                // Broadcast after removal
                broadcastOnlineList();
            }
            
            // Close resources
            if (ps != null) ps.close();
            if (dis != null) dis.close();
            if (soc != null) {
                System.out.println("Closing socket for: " + soc.getInetAddress());
                soc.close();
            }
            
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
