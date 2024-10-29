import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String password;
    private int loginAttempts = 3;
    private ChatInterface chatInterface;

    public ChatClient(String serverAddress, int port, ChatInterface chatInterface) {
        this.chatInterface = chatInterface;
        connectToServer(serverAddress, port);
    }

    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            try {
                socket = new Socket(serverAddress, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Start a thread to listen for incoming messages
                new Thread(new IncomingMessageHandler()).start();
            } catch (IOException e) {
                System.err.println("Error: Unable to connect to the server.");
            }
        }).start();
    }

    public void sendLoginCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        out.println("LOGIN:" + username + ":" + password);
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    chatInterface.displayMessage("Received: " + message); // Display in chat area
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ChatInterface {
        private JFrame frame;
        private JTextArea chatArea;
        private JTextField messageField;
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JButton sendButton;
        private JButton loginButton;
        private ChatClient chatClient;

        public ChatInterface() {
            initializeUI();
        }

        public void setChatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
        }

        private void initializeUI() {
            frame = new JFrame("Chat Client");
            chatArea = new JTextArea(20, 50);
            messageField = new JTextField(40);
            usernameField = new JTextField(15);
            passwordField = new JPasswordField(15);
            sendButton = new JButton("Send");
            loginButton = new JButton("Login");

            chatArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(chatArea);
            frame.setLayout(new BorderLayout());
            frame.add(scrollPane, BorderLayout.CENTER);

            JPanel loginPanel = new JPanel();
            loginPanel.add(new JLabel("Username:"));
            loginPanel.add(usernameField);
            loginPanel.add(new JLabel("Password:"));
            loginPanel.add(passwordField);
            loginPanel.add(loginButton);
            frame.add(loginPanel, BorderLayout.NORTH);

            JPanel inputPanel = new JPanel();
            inputPanel.add(messageField);
            inputPanel.add(sendButton);
            frame.add(inputPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);

            sendButton.setEnabled(false);

            loginButton.addActionListener(e -> {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                chatClient.sendLoginCredentials(username, password);
            });

            sendButton.addActionListener(e -> sendMessage());
            messageField.addActionListener(e -> sendMessage());
        }

        private void sendMessage() {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                chatClient.sendMessage(message);
                messageField.setText("");
            }
        }

        public void restartLogin() {
            usernameField.setText("");
            passwordField.setText("");
            chatArea.setText("");
        }

        public void displayMessage(String message) {
            chatArea.append(message + "\n");
        }

        public void receiveMessage(String message) {
            displayMessage(message);
        }
    }

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;

        ChatInterface chatInterface = new ChatInterface();
        ChatClient chatClient = new ChatClient(serverAddress, port, chatInterface);

        // Set the ChatClient in ChatInterface
        chatInterface.setChatClient(chatClient);
    }
}
