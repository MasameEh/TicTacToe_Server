/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dto;

import java.net.Socket;

/**
 *
 * @author Mohamed Sameh
 */
public class DTOPlayer {
    private int playerId;
    private String username;
    private String password;
    private String email;
    private int score;
    private Socket socket;
    
    public DTOPlayer(String username, int score, Socket socket) {
        this.username = username;
        this.score = score;
        this.socket = socket;
    }

    public DTOPlayer(int playerId, String username) {
        this.playerId = playerId;
        this.username = username;
    }
     public DTOPlayer() {
        this.username = "";
        this.password="";
        this.email="";
        this.score = 0;
        this.socket = null;
    }
      public DTOPlayer(String username,String password,String email, String status, int score, Socket socket) {
        this.username = username;
        this.password=password;
        this.email=email;
        this.score = score;
        this.socket = socket;
    }

    public String getEmail() {
        return email;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
   

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
    
    
}
