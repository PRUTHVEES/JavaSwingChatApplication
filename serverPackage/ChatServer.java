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
                            if(checkCredentials(usernameAttempt, passwordAttempt)) {
                                username = usernameAttempt; // Set username on successful login
                                int userId = getUserId(usernameAttempt);
                                String displayName = getDisplayName(usernameAttempt); // Get display name from the database

                                out.println("Welcome " + username + "!"); // Send welcome message
                                out.println("DISPLAY_NAME:" + displayName); // Send display name to the client

                                sendRetrievedMessagesToClient(out, getUserId(username)); // Send messages to the client

                                broadcast(displayName + " has joined the chat."); // Notify others
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
                            handleMessage(message,out);
                        } else {
                            out.println("ERROR: Message format is incorrect. Use: MESSAGE:userId:chatRoomId:messageContent");
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

    
        private void handleMessage(String message, PrintWriter out) {
            // Example message format: MESSAGE:<displayName>:<chatRoomId>:<messageContent>
            String[] parts = message.split(":");
            if (parts.length == 4 && parts[0].equals("MESSAGE")) {
                String displayName = parts[1];
                String chatRoomId = parts[2]; // If you're using chat rooms
                String messageContent = parts[3];

                // Store the message in the database
                saveMessageToDatabase(displayName, messageContent, chatRoomId);

                // Broadcast the message to all clients
                broadcast(displayName + ": " + messageContent);
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
        
        private String getDisplayName(String username) {
            String url = "jdbc:mysql://localhost:3306/chat_db"; // Replace with your DB URL
            String dbUser = "root"; // Replace with your DB username
            String dbPassword = ""; // Replace with your DB password

            String query = "SELECT displayname FROM users WHERE username = ?";
            String displayName = null;

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                    stmt.setString(1, username);
                    try(ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            displayName = rs.getString("displayname");
                        }
                }
            } catch (SQLException e) {
                e.printStackTrace();
        }
    return displayName; // Return the display name or null if not found
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

        private List<String> retrieveMessages(int userId) {
            String url = "jdbc:mysql://localhost:3306/chat_db";
            String dbUser = "root";
            String dbPassword = "";

            String query = "SELECT u.displayname, c.message_content FROM chats c JOIN users u ON c.user_id = u.user_id WHERE c.user_id = ?";
            List<String> messages = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String displayName = rs.getString("displayname");
                        String messageContent = rs.getString("message_content");

                        String formattedMessage = displayName + ": " + messageContent;
                        messages.add(formattedMessage);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return messages;
        }


    private void sendRetrievedMessagesToClient(PrintWriter out, int userId) {
        List<String> messages = retrieveMessages(userId); // Retrieve messages for the user
        StringBuilder messageBuilder = new StringBuilder();

        for (String message : messages) {
            // Assuming your message format in the database is: "displayname: messageContent"
            String[] messageParts = message.split(": ", 2); // Split into display name and content
            if (messageParts.length == 2) {
                String displayName = messageParts[0]; // Get the display name
                String messageContent = messageParts[1]; // Get the message content
                messageBuilder.append(displayName).append(": ").append(messageContent).append("\n"); // Format it
            }
        }

        // Send the messages to the client, prefixing with "MESSAGES:"
        out.println("MESSAGES:\n" + messageBuilder.toString().trim());
    }


        
    
    private void saveMessageToDatabase(String displayName, String messageContent, String chatRoomId) {
        String url = "jdbc:mysql://localhost:3306/chat_db"; // Your DB URL and name
        String dbUser = "root"; // Your DB username
        String dbPassword = ""; // Your DB password

        // First, find the user_id associated with the displayName
        int userId = getUserIdFromDisplayName(displayName);
    
        // Query to insert the new message into the chats table
        String query = "INSERT INTO chats (user_id, message_content, timestamp, chat_room_id) VALUES (?, ?, NOW(), ?)";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(query)) {
        
            stmt.setInt(1, userId);
            stmt.setString(2, messageContent);
            stmt.setString(3, chatRoomId); // Or null if not using chat rooms
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getUserIdFromDisplayName(String displayName) {
        String url = "jdbc:mysql://localhost:3306/chat_db"; // Your DB URL and name
        String dbUser = "root"; // Your DB username
        String dbPassword = ""; // Your DB password

        String query = "SELECT user_id FROM users WHERE display_name = ?";
        int userId = -1;

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, displayName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userId;
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
