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
    private String displayName = null;

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
        if (displayName != null) {
            String formattedMessage = "MESSAGE:" + displayName + ":null:" + message;
            out.println(formattedMessage);
            chatInterface.displayMessage("You: " + message);
        } else {
            System.out.println("Error: Login is required.");
        }
    }

    private void handleIncomingMessage(String message) {
        if (message.startsWith(displayName + " has joined")) {
            return;
        }

        String[] messageParts = message.split(": ", 2);
        
        if (messageParts.length == 2) {
            String senderDisplayName = messageParts[0];
            String messageContent = messageParts[1];

            if (senderDisplayName.equals(displayName)) {
                chatInterface.displayMessage("You: " + messageContent);
            } else {
                chatInterface.displayMessage(senderDisplayName + ": " + messageContent);
            }
        } else {
            chatInterface.displayMessage(message);
        }
    }

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Welcome")) {
                        chatInterface.displayMessage("Login successful! You can start chatting.");
                        chatInterface.clearLoginFields();
                        chatInterface.enableChat();
                    } else if (message.startsWith("DISPLAY_NAME:")) {
                        displayName = message.split(":")[1];
                        chatInterface.displayMessage("Your display name is: " + displayName);
                    } else if (message.startsWith("MESSAGES:")) {
                        String messages = message.substring("MESSAGES:".length()).trim();
                        String[] messageArray = messages.split("\n");

                        for (String msg : messageArray) {
                            String[] messageParts = msg.split(": ", 2);

                            if (messageParts.length == 2) {
                                String senderDisplayName = messageParts[0];
                                String messageContent = messageParts[1];

                                
                                chatInterface.displayMessage(senderDisplayName + ": " + messageContent);
                                
                            }
                        }
                    } else if (message.equals("ERROR: Invalid username or password. Please try again.")) {
                        loginAttempts--;
                        chatInterface.displayMessage("Invalid login. Attempts remaining: " + loginAttempts);
                        if (loginAttempts > 0) {
                            chatInterface.clearLoginFields();
                        } else {
                            chatInterface.disableLogin();
                            chatInterface.displayMessage("Maximum login attempts reached. Please restart the application.");
                        }
                    } else {
                        handleIncomingMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        if (out != null) {
            out.println("DISCONNECT:" + username);
            out.close();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        private JButton addUserButton;
        private JPanel userPanel;
        private ChatClient chatClient;

        public ChatInterface() {
            initializeUI();
        }

        public void clearChatArea() {
            chatArea.setText("");
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
            addUserButton = new JButton("Add User");
            userPanel = new JPanel();

            messageField.setEditable(false);
            chatArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(chatArea);
            userPanel.setLayout(new BorderLayout());
            userPanel.setPreferredSize(new Dimension(300, 0));

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.add(new JLabel("Online Users"), BorderLayout.WEST);
            topPanel.add(addUserButton, BorderLayout.EAST);
            userPanel.add(topPanel, BorderLayout.NORTH);

            JTextArea usersArea = new JTextArea();
            usersArea.setEditable(false);
            userPanel.add(new JScrollPane(usersArea), BorderLayout.CENTER);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, userPanel);
            splitPane.setDividerLocation(500);
            splitPane.setResizeWeight(0.8);

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

            // Add WindowListener to handle window closing event
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    chatClient.disconnect();  // Notify server on window close
                    System.exit(0);
                }
            });
            
            sendButton.setEnabled(false);
            addUserButton.setEnabled(false);

            usernameField.addActionListener(e -> attemptLogin());
            passwordField.addActionListener(e -> attemptLogin());
            loginButton.addActionListener(e -> attemptLogin());
            sendButton.addActionListener(e -> sendMessage());
            messageField.addActionListener(e -> sendMessage());
            addUserButton.addActionListener(e -> addUser());
        }

        private void attemptLogin() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            chatClient.sendLoginCredentials(username, password);
        }

        private void sendMessage() {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                chatClient.sendMessage(message);
                messageField.setText("");
            }
        }

        private void addUser() {
            displayMessage("Add User functionality will be added soon.");
        }

        public void clearLoginFields() {
            usernameField.setText("");
            passwordField.setText("");
        }

        public void disableLogin() {
            loginButton.setEnabled(false);
            usernameField.setEditable(false);
            passwordField.setEditable(false);
            sendButton.setToolTipText(null);
            messageField.setToolTipText(null);
        }

        public void enableLogin() {
            loginButton.setEnabled(true);
            usernameField.setEditable(true);
            passwordField.setEditable(true);
            sendButton.setToolTipText("Please login first to send messages");
            messageField.setToolTipText("Please login first to send messages");
        }

        public void enableChat() {
            sendButton.setToolTipText(null);
            messageField.setToolTipText(null);
            sendButton.setEnabled(true);
            messageField.setEditable(true);
            loginButton.setEnabled(false);
            usernameField.setEditable(false);
            passwordField.setEditable(false);
            addUserButton.setEnabled(true);
        }

        public void displayMessage(String message) {
            chatArea.append(message + "\n");
        }

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

        chatInterface.setChatClient(chatClient);
    }
}
