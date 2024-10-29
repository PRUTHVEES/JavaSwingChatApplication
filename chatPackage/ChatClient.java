import java.io.*;
import java.net.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatInterface chatInterface;
    private String username; // Field to store the username
    private String password; // Field to store the password

    public ChatClient(String serverAddress, int port, String username, String password) {
        this.username = username; // Set the username
        this.password = password; // Set the password

        connectToServer(serverAddress, port); // Try to connect on initialization
    }

    // Method to set chat interface after initialization
    public void setChatInterface(ChatInterface chatInterface) {
        this.chatInterface = chatInterface;
    }

    // Method to connect to the server
    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            while (true) {
                try {
                    socket = new Socket(serverAddress, port);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Send username and password to the server
                    out.println("LOGIN:" + username + ":" + password); // Format: LOGIN:username:password

                    // Clear chat area once connected
                    if (chatInterface != null) {
                        chatInterface.clearChatArea();
                    }

                    // Start a thread to listen for incoming messages from the server
                    new Thread(new IncomingMessageHandler()).start();
                    break; // Exit loop on successful connection
                } catch (IOException e) {
                    if (chatInterface != null) {
                        chatInterface.displayMessage("Error: Unable to connect to the server. Retrying in 5 seconds...");
                    }
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

    // Handle incoming messages from the server
    public class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (chatInterface != null) {
                        chatInterface.receiveMessage(message); // Update the chat area in the UI
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
