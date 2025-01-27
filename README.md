# Tic Tac Toe Server

## Overview

The Tic Tac Toe Server manages game sessions for a multiplayer Tic Tac Toe application. It handles player connections, manages active game sessions, and facilitates communication between players during a game.

## Features

- Handles player login and registration.
- Manages active game sessions and player statuses.
- Notifies opponents when a player disconnects.
- Synchronizes the game state between two players.

## Prerequisites

- Java Development Kit (JDK) 8 or higher.
- Apache Derby database.
- An IDE or terminal to compile and run the application.

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/MasameEh/TicTacToe_Server
   ```
2. Navigate to the server directory:
   ```bash
   cd TicTacToeServer
   ```
3. Compile the server:
   ```bash
   javac -d bin src/**/*.java
   ```
4. Run the server:
   ```bash
   java -cp bin TicTacToeServer
   ```

## Database Setup

1. Create a table named `players` in Apache Derby:
   ```sql
   CREATE TABLE players (
       username VARCHAR(50) PRIMARY KEY,
       password VARCHAR(50) NOT NULL,
       email VARCHAR(100) NOT NULL,
       score INT DEFAULT 0
   );
   ```

## Usage

1. Start the server.
2. Monitor server logs in the console for player connections and game activities.



