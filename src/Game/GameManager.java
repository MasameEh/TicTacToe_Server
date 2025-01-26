/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import tictactoeserver.ClientHandler;
import db.DAO;
import java.util.logging.Level;
import java.util.logging.Logger;
import tictactoeserver.ServerUIController;


/**
 *
 * @author Mohamed Sameh
 */


public class GameManager {
   private static Map<String, GameSession> activeSessions = new HashMap<>();
   public static List<String> gamePlayers = new ArrayList<>();
   static ServerUIController controlerUI;
    
    
    private static class GameSession {
        ClientHandler player1;  // X player
        ClientHandler player2;  // O player
        
        String[][] board = new String[3][3];
        boolean isGameOver = false;
        
        public GameSession(ClientHandler p1, ClientHandler p2) {
            this.player1 = p1;
            this.player2 = p2;
            initializeBoard();
        }
        
        private void initializeBoard() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    board[i][j] = "";
                }
            }
        }
    }
    
    
    // start the game then waiting for moves from the first player
    public static void startNewGame(ClientHandler player1, ClientHandler player2, ServerUIController controlerUI) {
        GameManager.controlerUI = controlerUI;
        GameSession session = new GameSession(player1, player2);
        String sessionId = player1.playerData.getUsername() + "_vs_" + player2.playerData.getUsername();
        activeSessions.put(sessionId, session);
        
        
        if (controlerUI == null) {
            System.err.println("Error: ControlerUI is not initialized.");
            return;
        }
        System.out.println(activeSessions.toString());
        
        synchronized (gamePlayers) {
            gamePlayers.add(player1.playerData.getUsername());
            gamePlayers.add(player2.playerData.getUsername());
        }

        // Update UI with in-game players
        controlerUI.addInGamePlayer(player1.playerData.getUsername().toString());
        controlerUI.addInGamePlayer(player2.playerData.getUsername().toString());
        
        // Notify player1 (X)
        JSONObject p1Start = new JSONObject();
        p1Start.put("type", "gameStart");
        p1Start.put("symbol", "X");
        p1Start.put("opponent", player2.playerData.getUsername());
        p1Start.put("score", player2.playerData.getScore());
        player1.sendMessage(p1Start.toJSONString());
        
        // Notify player2 (O)
        JSONObject p2Start = new JSONObject();
        p2Start.put("type", "gameStart");
        p2Start.put("symbol", "O");
        p2Start.put("opponent", player1.playerData.getUsername());
        p1Start.put("score", player1.playerData.getScore());
        player2.sendMessage(p2Start.toJSONString());
    }
    
    private static String findSessionId(ClientHandler player) {
        for (Map.Entry<String, GameSession> entry : activeSessions.entrySet()) {
            GameSession session = entry.getValue();
            if (session.player1 == player || session.player2 == player) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    // handle any move from players in client side
    public static void handleMove(ClientHandler player, JSONObject moveData) {
        String sessionId = findSessionId(player);
        if (sessionId == null) return;
        
        System.out.println("in handle move" + moveData.toJSONString());
        
        GameSession session = activeSessions.get(sessionId);
        
        int row = Integer.parseInt(moveData.get("row").toString());
        int col = Integer.parseInt(moveData.get("col").toString());
        String symbol = moveData.get("symbol").toString();
        
        System.out.println("in handle move" + moveData.toJSONString());
        
        if(!isValidMove(session, row, col, player)) {
            JSONObject errorMsg = new JSONObject();
            errorMsg.put("type", "gameError");
            errorMsg.put("message", "Invalid move");
            player.sendMessage(errorMsg.toJSONString());
            System.out.println("isValidMove");
            return;
        }
        
         // Update board
        session.board[row][col] = symbol;
        
        
        // Broadcast move to both players
        ClientHandler opponent = (player == session.player1) ? session.player2 : session.player1;
        System.out.println("in handle move opponent: " + opponent.playerData.getUsername());
        opponent.sendMessage(moveData.toJSONString());
        
        System.out.println("in handle move" + moveData.toJSONString());
        // Check game end conditions
        checkGameEnd(session);
    }
    
    private static boolean isValidMove(GameSession session, int row, int col, ClientHandler player) {
        // Check if it's player's turn
        boolean isPlayer1Turn = getFilledCellCount(session.board) % 2 == 0;  // Changed function
        boolean isPlayer1 = player == session.player1;

        if (isPlayer1Turn != isPlayer1) {
            System.out.println("Turn validation failed: isPlayer1Turn=" + isPlayer1Turn + ", isPlayer1=" + isPlayer1);
            return false;
        }

        // Check if position is empty
        return row >= 0 && row < 3 && col >= 0 && col < 3 
            && session.board[row][col].isEmpty();
    }


    private static int getFilledCellCount(String[][] board) {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (!board[i][j].isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    
    
    private static boolean isBoardFull(String[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    
    private static String checkWinner(String[][] board) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (!board[i][0].isEmpty() && 
                board[i][0].equals(board[i][1]) && 
                board[i][0].equals(board[i][2])) {
                System.out.println("Row win: " + board[i][0]); // Debug log
                return board[i][0];
            }
        }

        // Check columns
        for (int j = 0; j < 3; j++) {
            if (!board[0][j].isEmpty() && 
                board[0][j].equals(board[1][j]) && 
                board[0][j].equals(board[2][j])) {
                System.out.println("Column win: " + board[0][j]); // Debug log
                return board[0][j];
            }
        }

        // Check diagonals
        if (!board[0][0].isEmpty() && 
            board[0][0].equals(board[1][1]) && 
            board[0][0].equals(board[2][2])) {
            System.out.println("Diagonal win: " + board[0][0]); // Debug log
            return board[0][0];
        }

        if (!board[0][2].isEmpty() && 
            board[0][2].equals(board[1][1]) && 
            board[0][2].equals(board[2][0])) {
            System.out.println("Diagonal win: " + board[0][2]); // Debug log
            return board[0][2];
        }

        return null;
    }

    
    public static void handlePlayerDisconnection(ClientHandler disconnectedPlayer) {
        String sessionId = findSessionId(disconnectedPlayer);
        
        if(sessionId != null){
            GameSession session = activeSessions.get(sessionId);
            ClientHandler opponent = session.player1.equals(disconnectedPlayer) ? session.player2: session.player1;
            
            JSONObject disconnectNotification  = new JSONObject();
            disconnectNotification.put("type", "opponentDisconnected");
            disconnectNotification.put("message", "Your opponent has disconnected. Returning to the home screen.");
            opponent.sendMessage(disconnectNotification.toJSONString());
            
            activeSessions.remove(sessionId);
            gamePlayers.remove(disconnectedPlayer.playerData.getUsername());
            gamePlayers.remove(opponent.playerData.getUsername());
            controlerUI.removeInGamePlayer(disconnectedPlayer.playerData.getUsername());
            controlerUI.removeInGamePlayer(opponent.playerData.getUsername());
            ClientHandler.onlinePlayers.add(opponent.playerData.getUsername());
        }
    
    }
    
    public static void handleGameForfeit(ClientHandler disconnectedPlayer) {
        
        String sessionId = findSessionId(disconnectedPlayer);
        
        if(sessionId != null){
            GameSession session = activeSessions.get(sessionId);
            ClientHandler opponent = session.player1.equals(disconnectedPlayer) ? session.player2: session.player1;
            
            JSONObject disconnectNotification  = new JSONObject();
            disconnectNotification.put("type", "gameForfeit");
            disconnectNotification.put("message", "Your opponent has forfeit the game");
            opponent.sendMessage(disconnectNotification.toJSONString());
            disconnectedPlayer.sendMessage(disconnectNotification.toJSONString());
            System.out.println("we are at handleGameForfeit");
            activeSessions.remove(sessionId);
            gamePlayers.remove(disconnectedPlayer.playerData.getUsername());
            gamePlayers.remove(opponent.playerData.getUsername());
            controlerUI.removeInGamePlayer(disconnectedPlayer.playerData.getUsername());
            controlerUI.removeInGamePlayer(opponent.playerData.getUsername());
            ClientHandler.onlinePlayers.add(session.player1.playerData.getUsername());
            ClientHandler.onlinePlayers.add(session.player2.playerData.getUsername());
            
            new Thread(() -> {
                synchronized (ClientHandler.clients) {
                    ClientHandler.broadcastOnlineList();
                }
            }).start();
        }
    
    }
    
    private static void checkGameEnd(GameSession session) {
    String winner = checkWinner(session.board);
    System.out.println("Checking game end. Winner: " + winner);
    int lScore=0;

    if (winner != null || isBoardFull(session.board)) {
        try {
            try {
                if (winner != null) {
                    String winnerName;
                    ClientHandler winnerHandler;
                    
                    if (winner.equals("X")) {
                        winnerName = session.player1.playerData.getUsername();
                        winnerHandler = session.player1;
                        lScore=session.player2.playerData.getScore();
                    } else {
                        winnerName = session.player2.playerData.getUsername();
                        winnerHandler = session.player2;
                       lScore=session.player1.playerData.getScore();

                    }
                    
                    // Update the winner's score
                    int currentScore = DAO.getScore(winnerName);
                    int newScore = currentScore + 10; // Increment score by 1
                    DAO.updateScore(winnerName, newScore);
                    
                    JSONObject p1Msg = new JSONObject();
                    p1Msg.put("type", "gameEnd");
                    
                    JSONObject p2Msg = new JSONObject();
                    p2Msg.put("type", "gameEnd");
                    
                    if (winner.equals("X")) {
                        // Player 1 wins
                        p1Msg.put("result", "win");
                        p1Msg.put("winner", winnerName);
                        p1Msg.put("score", newScore); // Send updated score
                        
                        // Player 2 loses
                        p2Msg.put("result", "lose");
                        p2Msg.put("winner", winnerName);
                        p2Msg.put("score", lScore);
                    } else {
                        // Player 2 wins
                        p1Msg.put("result", "lose");
                        p1Msg.put("winner", winnerName);
                        p1Msg.put("score", lScore);
                        
                        // Player 1 loses
                        p2Msg.put("result", "win");
                        p2Msg.put("winner", winnerName);
                        p2Msg.put("score", newScore); // Send updated score
                    }
                    
                    
                    // Send messages to both players
                    session.player1.sendMessage(p1Msg.toJSONString());
                    session.player2.sendMessage(p2Msg.toJSONString());
                } else {
                    // It's a draw
                    System.out.println("Game is a draw");
                    JSONObject drawMsg = new JSONObject();
                    drawMsg.put("type", "gameEnd");
                    drawMsg.put("result", "draw");
                  
                    
                    // Send draw message to both players
                    session.player1.sendMessage(drawMsg.toJSONString());
                    session.player2.sendMessage(drawMsg.toJSONString());
                 
                    
                    // Broadcast updated online list
                    new Thread(() -> {
                        synchronized(ClientHandler.clients) {
                            ClientHandler.broadcastOnlineList();
                        }
                    }).start();
                }
            } catch (Exception e) {
                System.out.println("Error during game end: " + e.getMessage());
                e.printStackTrace();

            }

            // Add a small delay to ensure messages are sent
            Thread.sleep(1000);

            // Clean up session
            String sessionId = findSessionId(session.player1);
            if (sessionId != null) {
                activeSessions.remove(sessionId);
                System.out.println("Game session removed: " + sessionId);
            }
            gamePlayers.remove(session.player1.playerData.getUsername());
            gamePlayers.remove(session.player2.playerData.getUsername());
            controlerUI.removeInGamePlayer(session.player1.playerData.getUsername());
            controlerUI.removeInGamePlayer(session.player2.playerData.getUsername());
            ClientHandler.onlinePlayers.add(session.player1.playerData.getUsername());
            ClientHandler.onlinePlayers.add(session.player2.playerData.getUsername());
            // Broadcast updated online list
            new Thread(() -> {
                synchronized (ClientHandler.clients) {
                    ClientHandler.broadcastOnlineList();
                }
            }).start();
            
        } catch (InterruptedException ex) {
                Logger.getLogger(GameManager.class.getName()).log(Level.SEVERE, null, ex);

            }

        
    }
}
}

