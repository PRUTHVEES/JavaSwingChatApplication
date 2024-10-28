import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345; // Define the port
    private static Set<PrintWriter> clientWriters = new HashSet<>();
    private static Map<String, String> userCredentials = new HashMap<>(); // To store usernames and passwords

    public static void main(String[] args) {
        loadUserCredentials(); // Load predefined credentials

        System.out.println("Chat Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start(); // Handle new client connections
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadUserCredentials() {
        // Predefined usernames and passwords (this could also come from a database)
        userCredentials.put("user1", "pass1");
        userCredentials.put("user2", "pass2");
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

                            // Check credentials
                            if (checkCredentials(usernameAttempt, passwordAttempt)) {
                                username = usernameAttempt; // Set username on successful login
                                out.println("Welcome " + username + "!"); // Send welcome message
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
                        broadcast(username + ": " + message); // Broadcast message with username
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

        private boolean checkCredentials(String username, String password) {
            return password.equals(userCredentials.get(username)); // Check the stored credentials
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
