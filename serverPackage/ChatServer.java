package serverPackage;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345; // Define the port
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static volatile boolean isDatabaseAvailable = false; // Track database status
    private static boolean alreadyNotified = false; 
    private static final int USER_ACTIVE = 1;
    private static final int USER_INACTIVE = 0;
    
    private static boolean isDatabaseOnline() {
        String url = "jdbc:mysql://localhost:3306/chat_db"; // Replace with your database URL
        String dbUser = "root"; // Replace with your database username
        String dbPassword = ""; // Replace with your database password

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            System.out.println("Database is online");
            return true; // Database is online and reachable
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            return false; // Database is offline or unreachable
        }
    }

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

    private static void startDatabaseStatusChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            boolean currentlyAvailable = isDatabaseOnline();
            if (currentlyAvailable == false && alreadyNotified == false) {
                System.out.println("Database is offline. Notifying connected clients.");
                broadcastDatabaseIssue(); // Notify all clients of the database issue
            } else if(alreadyNotified == true) {
                return;
            }else {
                System.out.println("Database connection restored.");
                //broadcast("INFO: Database connection restored.");
            }
        }, 0, 5, TimeUnit.SECONDS); // Check every 5 seconds
    }

    private static void broadcastDatabaseIssue() {
        broadcast("ERROR: Database is currently offline. Please try again later.");
        alreadyNotified = true;
    }

    private static void broadcast(String message) {
        synchronized (clientWriters) {
            for (PrintWriter writer : clientWriters) {
                writer.println(message); // Send message to all connected clients
            }
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username; // Store the username after login
        private final String url = "jdbc:mysql://localhost:3306/chat_db"; // Your DB URL and name
        private final String dbUser = "root"; // Your DB username
        private final String dbPassword = ""; // Your DB password

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                startDatabaseStatusChecker();
                
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
                                turnUserToActiveState(userId);

                                String displayName = getDisplayName(usernameAttempt); // Get display name from the database

                                out.println("Welcome " + username + "!"); // Send welcome message
                                out.println("USER_ID:" + userId);
                                out.println("DISPLAY_NAME:" + displayName); // Send display name to the client

                                sendRetrievedMessagesToClient(out); // Send messages to the client

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
                        handleMessage(message, out);
                    } else {
                        out.println("ERROR: Message format is incorrect. Use: MESSAGE:userId:chatRoomId:messageContent");
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
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
                    updateActiveStatus(getUserId(username), false); // Set is_active to 0 and update last_login timestamp on disconnect
                    broadcast(username + " has left the chat."); // Notify others
                }
            }
        }
    
        private void updateActiveStatus(int userId, boolean isActive) {
            String query = "UPDATE users SET is_active = ?, last_login = ? WHERE user_id = ?";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setBoolean(1, isActive);
                stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis())); // Set current time
                stmt.setInt(3, userId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        private void handleMessage(String message, PrintWriter out) {
            // Example message format: MESSAGE:<displayName>:<chatRoomId>:<messageContent>
            if(message.contains("ADD_USER_INVITE")) {
                String[] parts = message.split(":");
                String receiverId = parts[1];
                String senderId = parts[2];
                handleInvitationToUser(receiverId, senderId);
            } else if (message.contains("INVITE_ACCEPTED")) {
                String parts[] = message.split(":");
                String senderId = parts[1];
                String receiverID = parts[2];
                saveContactToDatabase(senderId,receiverID);
                sendMessageToClient(message);
            } else if (message.contains("INVITE_REJECTED")) {
                String[] parts = message.split(":");
                String senderId = parts[1];
                String receiverId = parts[2];
                sendMessageToClient(message);
                // Optionally update the database to reflect the invitation was rejected
                } else {
                String[] parts = message.split(":");
                if (parts.length == 4 && parts[0].equals("MESSAGE")) {
                    String displayName = parts[1];
                    int chatRoomId;
                    if(!(parts[2].equals("null"))) { 
                        chatRoomId = Integer.parseInt(parts[2]);
                    } else {
                        chatRoomId = -1;
                    }  // If you're using chat rooms

                    String messageContent = parts[3];

                    // Store the message in the database
                    saveMessageToDatabase(displayName, messageContent, chatRoomId);

                    // Broadcast the message to all clients
                    broadcast(displayName + ": " + messageContent);
                }
            }
        }


        private int getUserId(String username) {
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
        
        public void saveContactToDatabase(String senderId, String receiverId) {
            // Query to insert the new message into the chats table
            String query = "INSERT INTO chats (user_id, contact_user_id, added_at) VALUES (?, ?, NOW())";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, senderId);
                stmt.setString(2, receiverId);

                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        private boolean checkCredentials(String username, String password) {
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
        
        private void sendMessageToClient(String message) {
            if (out != null) {
                out.println(message);
            }
        }


        private void handleInvitationToUser(String toSendInviteTo,String userIdOfReceiver) {
            String query = "SELECT is_active FROM users WHERE user_id = ?";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, toSendInviteTo);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int status = rs.getInt("is_active"); //Checks if the user is active or not to receive invitations
                        if(status == USER_ACTIVE) {
                            sendMessageToClient(toSendInviteTo + ":ADD_USER_INVITE:" + userIdOfReceiver);
			} else if(status == USER_INACTIVE) {
                            sendMessageToClient(userIdOfReceiver + ":RESP_USER_INVITE:Server cannot send request to this user right now");
			}
	            } else {
			sendMessageToClient(userIdOfReceiver + ":USER_NOT_FOUND");
		    } 
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        private List<String> retrieveMessages() {
        // Modify the query to select all messages without filtering by user_id
            String query = "SELECT u.displayname, c.message_content FROM chats c JOIN users u ON c.user_id = u.user_id ORDER BY c.timestamp"; // Order by timestamp if you want messages in chronological order
            List<String> messages = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String displayName = rs.getString("displayname");
                    String messageContent = rs.getString("message_content");

                    // Format the message
                    String formattedMessage = displayName + ": " + messageContent;
                    messages.add(formattedMessage);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return messages;
        }
        


    private void sendRetrievedMessagesToClient(PrintWriter out) {
        List<String> messages = retrieveMessages(); // Retrieve messages for the user
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
        System.out.println(messageBuilder.toString());
    }


        
    
    private void saveMessageToDatabase(String displayName, String messageContent, int chatRoomId) {
        // First, find the user_id associated with the displayName
        int userId = getUserIdFromDisplayName(displayName);
    
        // Query to insert the new message into the chats table
        String query = "INSERT INTO chats (user_id, message_content, timestamp, chat_room_id) VALUES (?, ?, NOW(), ?)";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(query)) {
        
            stmt.setInt(1, userId);
            stmt.setString(2, messageContent);
            
            if(chatRoomId == -1) {
                stmt.setNull(3, Types.INTEGER);    // Set chat_room_id as NULL
            } else {
                stmt.setInt(3, chatRoomId); // Or null if not using chat rooms
            }
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void turnUserToActiveState(int userId) {
        // Query to insert the new message into the chats table
        String query = "UPDATE users SET is_active = 1 WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
            PreparedStatement stmt = conn.prepareStatement(query)) {
        
            stmt.setInt(1, userId);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private int getUserIdFromDisplayName(String displayName) {
        String query = "SELECT user_id FROM users WHERE displayname = ?";
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

