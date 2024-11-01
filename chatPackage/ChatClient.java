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
    private int loginAttempts = 5;
    private ChatInterface chatInterface;
    private int userId = -1;
    
    public ChatClient(String serverAddress, int port, ChatInterface chatInterface) {
        this.chatInterface = chatInterface;
        connectToServer(serverAddress, port);
    }

    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            while (true) {
                chatInterface.disableLogin();
                try {
                    socket = new Socket(serverAddress, port);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Start a thread to listen for incoming messages
                    new Thread(new IncomingMessageHandler()).start();

                    
                    chatInterface.displayMessage("Connected to the server.");
                    chatInterface.enableLogin();
                    break; // Exit loop once connected

                } catch (IOException e) {
                    chatInterface.displayMessage("Error: Unable to connect to the server. Retrying...");
                    
                    // Wait for a few seconds before retrying
                    try {
                        Thread.sleep(3000); // 3-second delay
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void sendLoginCredentials(String username, String password) {
        this.username = username;
        this.password = password;

        out.println("LOGIN:" + username + ":" + password);
        
    }

    public void sendMessage(String message) {
        if (userId != -1) { // Ensure userId is set
            String formattedMessage = "MESSAGE:" + userId + ":null:" + message;
            out.println(formattedMessage); // Send message with userId and null for chatRoomId
        } else {
            System.out.println("Error: User ID not set. Login is required.");
        }
    }


private class IncomingMessageHandler implements Runnable {
    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                
                // Handle login success message
                    if (message.startsWith("Welcome")) {
                        chatInterface.displayMessage("Login successful! You can start chatting.");
                        chatInterface.enableChat();  // Enable chat area on successful login
                    } else if (message.startsWith("USER_ID:")) {
                        String userIdStr = message.split(":")[1]; // Extract and store user ID
                        
                        // Store user ID in a variable, e.g., this.userId = userId;
                        userId = Integer.parseInt(userIdStr);
                    } else if (message.equals("ERROR: Invalid username or password. Please try again.")) {
                    loginAttempts--;
                    chatInterface.displayMessage("Invalid login. Attempts remaining: " + loginAttempts);
                    if (loginAttempts > 0) {
                        chatInterface.clearLoginFields();  // Clear login fields on failed login
                    } else {
                        chatInterface.disableLogin();
                        chatInterface.displayMessage("Maximum login attempts reached. Please restart the application.");
                    }
                } else {
                    if (message.startsWith(username + ":")) { // username is client's own username
                        String selfMessage = "You: " + message.substring(username.length() + 2); // Add "You:" prefix
                        chatInterface.displayMessage(selfMessage);
                    } else {
                        // Display regular incoming message
                        chatInterface.displayMessage(message);
                    }
                }
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
    private JPanel userPanel;  // New user panel for displaying user info
    private ChatClient chatClient;

    public ChatInterface() {
        initializeUI();
    }

    // Method to clear the chat area
    public void clearChatArea() {
        chatArea.setText(""); // Clears all text in the chat area
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
        userPanel = new JPanel(); // Initialize user panel

        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        userPanel.setLayout(new BorderLayout());
        userPanel.setPreferredSize(new Dimension(150, 0)); // Set preferred width

        // Add components to user panel
        userPanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        JTextArea usersArea = new JTextArea();
        usersArea.setEditable(false);
        userPanel.add(new JScrollPane(usersArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, userPanel);
        splitPane.setDividerLocation(500); // Initial divider position
        splitPane.setResizeWeight(0.8); // Adjust resize behavior

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);

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

    public void clearLoginFields() {
        usernameField.setText("");
        passwordField.setText("");
    }

    public void disableLogin() {
        loginButton.setEnabled(false);
        usernameField.setEditable(false);
        passwordField.setEditable(false);
    }

    public void enableLogin() {
        loginButton.setEnabled(true);
        usernameField.setEditable(true);
        passwordField.setEditable(true);
    }
    
    public void enableChat() {
        sendButton.setEnabled(true); // Enable send button after successful login
        loginButton.setEnabled(false); // Disable login button after successful login
        usernameField.setEditable(false);
        passwordField.setEditable(false);
    }

    public void displayMessage(String message) {
        chatArea.append(message + "\n");
    }

    // Method to update the user panel with online users
    public void updateUserList(String[] users) {
        JTextArea usersArea = (JTextArea) ((JScrollPane) userPanel.getComponent(1)).getViewport().getView();
        usersArea.setText(String.join("\n", users));
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
