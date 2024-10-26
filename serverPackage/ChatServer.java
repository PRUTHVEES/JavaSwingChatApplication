package serverPackage;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static List<String> messageHistory = new ArrayList<>(); // To store the history of messages
    private static AtomicInteger userIdCounter = new AtomicInteger(1); // Counter to assign unique user IDs

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(12345); // Server listens on port 12345
        System.out.println("Chat server started...");

        while (true) {
            Socket socket = serverSocket.accept(); // Accept client connection
            int userId = userIdCounter.getAndIncrement(); // Assign a unique user ID
            System.out.println("New user (User ID: " + userId + ") connected!");
            ClientHandler clientHandler = new ClientHandler(socket, userId);
            clientHandlers.add(clientHandler);
            new Thread(clientHandler).start(); // Handle client in a separate thread
        }
    }

    // Broadcast message to all connected clients
    public static void broadcastMessage(String message, ClientHandler sender) {
        // Add message to history
        messageHistory.add(message);

        for (ClientHandler client : clientHandlers) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    // Remove client if they disconnect
    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        String disconnectedMessage = clientHandler.getName() + " (User ID: " + clientHandler.getUserId() + ") disconnected.";
        System.out.println(disconnectedMessage);

        broadcastMessage(disconnectedMessage, clientHandler);
        broadcastUserList();  // Update user list when someone disconnects
    }

    // Send the message history to a new client
    public static void sendHistory(ClientHandler clientHandler) {
        for (String message : messageHistory) { 
            clientHandler.sendMessage(message);
        }
    }

    // Broadcast the list of currently connected users
    public static void broadcastUserList() {
        List<String> userList = new ArrayList<>();
        for (ClientHandler client : clientHandlers) {
            userList.add(client.getName());
        }

        // Convert the user list to a string
        String userListMessage = "USER_LIST:" + String.join(",", userList);
        for (ClientHandler client : clientHandlers) {
            client.sendMessage(userListMessage);
        }
    }
}

// Handles communication with individual clients
class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private int userId;
    private String name; // Add a field for the user's name

    public ClientHandler(Socket socket, int userId) {
        this.socket = socket;
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name; // Method to get the user's name
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Ask the user to enter their name
            out.println("Please enter your name: ");
            name = in.readLine(); // Read the user's name

            // Notify the newly connected user with their ID and name
            out.println("You are connected as " + name + " (User ID: " + userId + ")");

            // Notify all other clients that a new user has connected
            String connectionMessage = name + " has joined the chat!";
            ChatServer.broadcastMessage(connectionMessage, this);

            // Broadcast the updated user list to all clients
            ChatServer.broadcastUserList();

            // Send the message history to the new client
            ChatServer.sendHistory(this);

            // Read messages from client
            String message;
            while ((message = in.readLine()) != null) {
                String formattedMessage = name + " (User ID: " + userId + "): " + message;
                System.out.println("Received: " + formattedMessage);
                ChatServer.broadcastMessage(formattedMessage, this); // Broadcast message to all clients
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ChatServer.removeClient(this);
        }
    }

    public void sendMessage(String message) {
        out.println(message); // Send message to the client
    }
}
