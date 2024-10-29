import java.io.*;
import java.net.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatInterface chatInterface;
    private String username;
    private String password;
    private int loginAttempts;

    public ChatClient(String serverAddress, int port, String username, String password, ChatInterface chatInterface) {
        this.username = username;
        this.password = password;
        this.chatInterface = chatInterface;
        this.loginAttempts = 0;
        connectToServer(serverAddress, port);
    }

    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            while (true) {
                try {
                    socket = new Socket(serverAddress, port);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Send login credentials
                    out.println("LOGIN:" + username + ":" + password);

                    // Clear chat area on successful connection
                    if (chatInterface != null) {
                        chatInterface.clearChatArea();
                    }

                    // Start thread to handle incoming messages
                    new Thread(new IncomingMessageHandler()).start();
                    break;
                } catch (IOException e) {
                    if (chatInterface != null) {
                        chatInterface.restartWithError("Error: Unable to connect to server. Retrying...");
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }).start();
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (chatInterface != null) {
                        // Check for invalid login message
                        if (message.equals("ERROR: Invalid username or password. Please try again.")) {
                            loginAttempts++;
                            if (loginAttempts >= 3) {
                                chatInterface.displayMessage("Maximum login attempts reached. Exiting.");
                                System.exit(0); // Exit application after max attempts
                            } else {
                                chatInterface.displayMessage("Invalid login. Please try again. Attempts remaining: " + (3 - loginAttempts));
                                chatInterface.resetLoginFields(); // Clear username and password fields
                            }
                        } else {
                            chatInterface.receiveMessage(message);
                        }
                    }
                }
            } catch (IOException e) {
                if (chatInterface != null) {
                    chatInterface.restartWithError("Disconnected from server. Please try reconnecting.");
                }
            }
        }
    }

    // Main method to run the ChatClient application
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;
        ChatInterface chatInterface = new ChatInterface();

        chatInterface.connect(serverAddress, port);
    }
}
