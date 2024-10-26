package chatPackage;

import java.io.*;
import java.net.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatInterface chatInterface;
    private String username; // Field to store the username

    public ChatClient(String serverAddress, int port, ChatInterface chatInterface, String username) {
        this.chatInterface = chatInterface;
        this.username = username; // Set the username

        connectToServer(serverAddress, port); // Try to connect on initialization
    }

    // Method to connect to the server
    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            while (true) {
                try {
                    socket = new Socket(serverAddress, port);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Send username to the server (if needed)
                    out.println("USERNAME:" + username);

                    // Clear chat area once connected
                    chatInterface.clearChatArea();

                    // Start a thread to listen for incoming messages from the server
                    new Thread(new IncomingMessageHandler()).start();
                    break; // Exit loop on successful connection
                } catch (IOException e) {
                    chatInterface.displayMessage("Error: Unable to connect to the server. Retrying in 5 seconds...");
                    try {
                        Thread.sleep(5000); // Wait before retrying
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    // Method to send messages to the server
    public void sendMessage(String message) {
        out.println(message); // Send message to the server
    }

    // Method to send a private message to a specific user
    public void sendPrivateMessage(String recipient, String message) {
        out.println("PRIVATE:" + recipient + ":" + message); // Format: PRIVATE:recipient:message
    }

    // Method to get the username
    public String getUsername() {
        return username; // Return the stored username
    }

    // Handle incoming messages from the server
    public class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    chatInterface.receiveMessage(message); // Update the chat area in the UI
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
