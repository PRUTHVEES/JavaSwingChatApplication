import java.io.*;
import java.net.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatInterface chatInterface;
    private String username;
    private String password;

    public ChatClient(String serverAddress, int port, String username, String password) {
        this.username = username;
        this.password = password;
        connectToServer(serverAddress, port);
    }

    public void setChatInterface(ChatInterface chatInterface) {
        this.chatInterface = chatInterface;
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
                    // Restart ChatInterface with an error message
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
                        chatInterface.receiveMessage(message);
                    }
                }
            } catch (IOException e) {
                if (chatInterface != null) {
                    chatInterface.restartWithError("Disconnected from server. Please try reconnecting.");
                }
            }
        }
    }

    // Main method to start the chat client application
    public static void main(String[] args) {
        ChatInterface chatInterface = new ChatInterface();
    }
}
