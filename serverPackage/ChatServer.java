import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345; // Define the port
    private static Set<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Chat Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start(); // Handle new client connections
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username; // Store the username after login

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            synchronized (clientWriters) {
                clientWriters.add(out); // Add client writer to the set
            }

            // Handle login
            String loginMessage;
            while ((loginMessage = in.readLine()) != null) {
                if (loginMessage.startsWith("LOGIN:")) {
                    String[] parts = loginMessage.split(":");
                    if (parts.length == 3) {
                        String usernameAttempt = parts[1];
                        String passwordAttempt = parts[2];

                        // Check credentials from the database
                        if (checkCredentials(usernameAttempt, passwordAttempt)) {
                            username = usernameAttempt; // Set username on successful login
                            int userId = getUserId(usernameAttempt);
                            out.println("Welcome " + username + "!"); // Send welcome message
                            out.println("USER_ID:" + userId);
                            
                            broadcast(username + " has joined the chat."); // Notify others
                            break; // Exit loop to start handling messages
                        } else {
                            out.println("ERROR: Invalid username or password. Please try again.");
                        }
                    }
                }
            }

            // Handle incoming messages
            String message;
            while ((message = in.readLine()) != null) {
                if (username != null) {
                    // Example message format: "MESSAGE:userId:chatRoomId:messageContent"
                    String[] parts = message.split(":");
                    if (parts.length == 4) {
                        int senderUserId = Integer.parseInt(parts[1]); // Extract userId
                        int chatRoomId = (parts[2] != null)? Integer.parseInt(parts[2]) : null; // Extract chatRoomId (can be null)
                        String messageContent = parts[3]; // Extract message content

                        // Validate the senderUserId (should match the logged-in user)
                        if (senderUserId != getUserId(username)) {
                            out.println("ERROR: Invalid sender user ID.");
                            continue; // Skip further processing for invalid user ID
                        }

                        // Save the message to the database (you might want to pass chatRoomId here)
                        saveMessageToDatabase(senderUserId, chatRoomId, messageContent);
                        broadcast(username + ": " + messageContent); // Broadcast message with username
                    } else {
                        out.println("ERROR: Message format is incorrect. Use: MESSAGE:userId:chatRoomId:messageContent");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (clientWriters) {
                clientWriters.remove(out); // Remove client writer on disconnect
            }
            if (username != null) {
            broadcast(username + " has left the chat."); // Notify others
            }
        }
    }


        private int getUserId(String username) {
                // Database connection settings
                String url = "jdbc:mysql://localhost:3306/chat_db"; // Replace with your DB URL and name
                String dbUser = "root"; // Replace with your DB username
            String dbPassword = ""; // Replace with your DB password
            
                String query = "SELECT user_id FROM users WHERE username = ?";
                try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                            return rs.getInt("user_id"); // Retrieve and return user_id
                        }
                    }
                } catch (SQLException e) {
                e.printStackTrace();
        }
            return -1; // Return -1 or handle if user_id not found
        }
        
        private boolean checkCredentials(String username, String password) {
            // Database connection settings
            String url = "jdbc:mysql://localhost:3306/chat_db"; // Replace with your DB URL and name
            String dbUser = "root"; // Replace with your DB username
            String dbPassword = ""; // Replace with your DB password

            String query = "SELECT password FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        return password.equals(storedPassword); // Check if the password matches
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false; // Return false if credentials are invalid
        }

        // Save the message to the database with user_id and chat_room_id
        private void saveMessageToDatabase(int userId, int chatRoomId, String message) {
        String url = "jdbc:mysql://localhost:3306/chat_db"; // Replace with your DB URL and name
        String dbUser = "root"; // Replace with your DB username
        String dbPassword = ""; // Replace with your DB password

        String query = "INSERT INTO chats (user_id, message_content, timestamp, chat_room_id) VALUES (?, ?, NOW(), ?)";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setString(2, message);
            stmt.setInt(4, chatRoomId);
            stmt.executeUpdate(); // Execute the insert query

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


        private void broadcast(String message) {
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(message); // Send message to all connected clients
                }
            }
        }
    }
}
