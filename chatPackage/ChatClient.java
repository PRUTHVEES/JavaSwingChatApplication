package chatPackage;

import javax.swing.*;
import java.awt.*;
import javax.swing.text.*;
import java.io.*;
import java.net.*;

public class ChatClient{

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String password;
    private int loginAttempts = 5;
    private ChatInterface chatInterface;
    private int userId = -1;
    private boolean cancelConnection = false;  // Flag to control connection retry
    private String displayName = null;

    public ChatClient(String serverAddress, int port, ChatInterface chatInterface) {
        this.chatInterface = chatInterface;
        connectToServer(serverAddress, port);
    }

    private void connectToServer(String serverAddress, int port) {
        new Thread(() -> {
            while (!cancelConnection) {
                chatInterface.disableLogin();
                try {
                    socket = new Socket(serverAddress, port);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Start a thread to listen for incoming messages
                    new Thread(new IncomingMessageHandler()).start();

                    chatInterface.displayMessage("Connected to the server.", Color.BLACK); // Add Color
                    chatInterface.enableLogin();
                    break; // Exit loop once connected

                } catch (IOException e) {
                    chatInterface.displayMessage("Error: Unable to connect to the server. Retrying...", Color.RED); // Add Color

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

    public void cancelConnectionAttempt() {
        cancelConnection = true;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendLoginCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        out.println("LOGIN:" + username + ":" + password);
    }

    public void sendMessage(String message) {
        if((displayName != null) && message.startsWith("AddUser")) {
            out.println(message);
        }  else if (displayName != null) {
            String formattedMessage = "MESSAGE:" + displayName + ":null:" + message;
            out.println(formattedMessage);
//            chatInterface.displayMessage("You: " + message, Color.BLACK); // Change to dark green if desired
        } else {
            System.out.println("Error: Login is required.");
        }
    }

    private void handleIncomingMessage(String message) {
        if (message.startsWith(displayName + " has joined")) {
            return;
        }

        String[] messageParts = message.split(": ", 2);
        Color DarkGreen = new Color(0, 100, 10);

        if (messageParts.length == 2) {
            String senderDisplayName = messageParts[0];
            String messageContent = messageParts[1];

            if (senderDisplayName.equals(displayName)) {
                chatInterface.displayMessage("You: " + messageContent, DarkGreen); // Change to dark green if desired
            } else {
                chatInterface.displayMessage(senderDisplayName + ": " + messageContent, Color.BLUE);
            }
        } else if(message.equals("ADD_USER_INVITE:" + userId)) {
            handleInvitations(out);
        } else {
            chatInterface.displayMessage(message, Color.BLACK);
        }
    }

    private void handleRetrievedMessages(String message) {
        String messages = message.substring("MESSAGES:".length()).trim();
        String[] messageArray = messages.split("\n");

        for (String msg : messageArray) {
            String[] messageParts = msg.split(": ", 2);

            if (messageParts.length == 2) {
                String senderDisplayName = messageParts[0];
                String messageContent = messageParts[1];
                chatInterface.displayMessage(senderDisplayName + ": " + messageContent, Color.BLACK);
            }
        }
    }

    private void handleInvitations(PrintWriter out) {
        int response = JOptionPane.showConfirmDialog(null, "Do you want to send an invitation?", "Send Invitation", JOptionPane.YES_NO_OPTION);
        
        if(response == JOptionPane.YES_OPTION) {
            updateUserList();
        } else if(response == JOptionPane.NO_OPTION) {
            return;
        }
    }

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Welcome")) {
                        chatInterface.displayMessage("Login successful! You can start chatting.", Color.RED); // Add Color
                        chatInterface.clearLoginFields();
                        chatInterface.enableChat();
                    } else if (message.startsWith("USER_ID:")) {
                        userId = Integer.parseInt(message.split(":")[1]);
                    } else if (message.startsWith("DISPLAY_NAME:")) {
                        displayName = message.split(":")[1];
                        chatInterface.displayMessage("Your display name is: " + displayName, Color.BLUE); // Add Color
                    } else if (message.startsWith("MESSAGES:")) {
                        handleRetrievedMessages(message);
                    } else if (message.equals("ERROR: Invalid username or password. Please try again.")) {
                        loginAttempts--;
                        chatInterface.displayMessage("Invalid login. Attempts remaining: " + loginAttempts, Color.RED); // Add Color
                        if (loginAttempts > 0) {
                            chatInterface.clearLoginFields();
                        } else {
                            chatInterface.disableLogin();
                            chatInterface.displayMessage("Maximum login attempts reached. Please restart the application.", Color.RED); // Add Color
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

    public void sendInvitation(String invitationString) {
        sendMessage(invitationString + ":" + userId);
    }
    
    public boolean isLoggedIn() {
        return displayName != null;
    }

    public static class ChatInterface  extends JFrame{

        private JFrame frame;
        private JTextPane chatArea;
        private JTextField messageField;
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JButton sendButton;
        private JButton loginButton;
        private JButton addUserButton;
        private JPanel userPanel;
        private JButton cancelButton;
        private ChatClient chatClient;
        private StyledDocument doc;

        public ChatInterface() {
            initializeUI();
        }

        public void setChatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
        }

        private void initializeUI() {
            frame = new JFrame("Chat Client");
            chatArea = new JTextPane();
            chatArea.setEditable(false);
            doc = chatArea.getStyledDocument();
            messageField = new JTextField(40);
            usernameField = new JTextField(15);
            passwordField = new JPasswordField(15);
            sendButton = new JButton("Send");
            loginButton = new JButton("Login");
            addUserButton = new JButton("Add User");
            cancelButton = new JButton("Logout and Close");
            userPanel = new JPanel();

            // Set background colors
            frame.getContentPane().setBackground(new Color(255, 255, 255));
            chatArea.setBackground(new Color(236, 229, 221));
            messageField.setBackground(new Color(255, 255, 255));
            usernameField.setBackground(new Color(255, 255, 255));
            passwordField.setBackground(new Color(255, 255, 255));

            chatArea.setForeground(new Color(0, 0, 0));
            messageField.setForeground(new Color(0, 0, 0));
            usernameField.setForeground(new Color(0, 0, 0));
            passwordField.setForeground(new Color(0, 0, 0));

            // Set button colors
            sendButton.setBackground(new Color(37, 211, 102));
            sendButton.setForeground(new Color(255, 255, 255));
            loginButton.setBackground(new Color(37, 211, 102));
            loginButton.setForeground(Color.WHITE);
            addUserButton.setBackground(new Color(0, 123, 255));
            addUserButton.setForeground(Color.WHITE);
            cancelButton.setBackground(new Color(255, 0, 0));
            cancelButton.setForeground(Color.WHITE);

            // Set fonts
            Font font = new Font("Poppins", Font.PLAIN, 20);
            chatArea.setFont(font);
            messageField.setFont(font);
            usernameField.setFont(font);
            passwordField.setFont(font);
            sendButton.setFont(font);
            loginButton.setFont(font);
            addUserButton.setFont(font);
            cancelButton.setFont(font);

            JScrollPane scrollPane = new JScrollPane(chatArea);
            userPanel.setLayout(new BorderLayout());
            userPanel.setPreferredSize(new Dimension(300, 500));

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

            // Create the login panel
            JPanel loginPanel = new JPanel();
            loginPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Align components to the left

            // Create and set the icon
            // Create and set the icon
            ImageIcon icon = new ImageIcon("D:\\Documents\\NetBeansProjects\\Chat Application\\src\\chatPackage\\whatsapp.jpg"); // Specify the path to your image
            JLabel iconLabel = new JLabel(icon);
            loginPanel.add(iconLabel); // Add the icon label to the panel

// Add the "WhatsApp (Clone)" label in green
            JLabel cloneLabel = new JLabel("WhatsApp (Clone)                     ");
            cloneLabel.setFont(font); // Set the font (optional)
            cloneLabel.setForeground(new Color(0, 100, 0)); // Set the text color to green
            loginPanel.add(cloneLabel); // Add the clone label to the panel

// Username label and field
            JLabel usernameLabel = new JLabel("Username:");
            usernameLabel.setFont(font); // Set the font
            loginPanel.add(usernameLabel);
            loginPanel.add(usernameField);
            loginPanel.setBackground(new Color(255, 255, 255));

// Password label and field
            JLabel passwordLabel = new JLabel("Password:");
            passwordLabel.setFont(font); // Set the font
            loginPanel.add(passwordLabel);
            loginPanel.add(passwordField);

// Add buttons
            loginPanel.add(cancelButton);
            loginPanel.add(loginButton);

            // Add the login panel to the frame
            frame.add(loginPanel, BorderLayout.NORTH);

            JPanel inputPanel = new JPanel();
            inputPanel.add(messageField);
            inputPanel.add(sendButton);
            frame.add(inputPanel, BorderLayout.SOUTH);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);

            sendButton.setEnabled(false);
            addUserButton.setEnabled(false);

            usernameField.addActionListener(e -> attemptLogin());
            passwordField.addActionListener(e -> attemptLogin());
            loginButton.addActionListener(e -> attemptLogin());
            sendButton.addActionListener(e -> sendMessage());
            messageField.addActionListener(e -> sendMessage());
            addUserButton.addActionListener(e -> addUser());
            cancelButton.addActionListener(e -> cancelConnectionAttempt());
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
            try {
                int userToInvite = Integer.parseInt(JOptionPane.showInputDialog(null, "Send an invitation to Add User with his/her User ID: ", "User ID Input", JOptionPane.QUESTION_MESSAGE));
                chatClient.sendInvitation("AddUser:" + userToInvite);
            } catch(NumberFormatException e) {
                System.out.println(e.getMessage());
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

        private void cancelConnectionAttempt() {
            chatClient.cancelConnectionAttempt();
            displayMessage("Connection canceled by user.", Color.BLACK);
            if (chatClient.isLoggedIn()) {
                messageField.setEnabled(true);
                sendButton.setEnabled(true);
                addUserButton.setEnabled(true);
            } else {
                usernameField.setEnabled(true);
                passwordField.setEnabled(true);
                loginButton.setEnabled(true);
            }
            messageField.setEditable(false);
            sendButton.setEnabled(false);
            System.exit(1);
        }

        public void displayMessage(String message, Color color) {
            try {
                Style style = chatArea.addStyle("Style", null);
                StyleConstants.setForeground(style, color);
                doc.insertString(doc.getLength(), message + "\n", style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        public void updateUserList(String[] users) {
            JTextArea usersArea = (JTextArea) ((JScrollPane) userPanel.getComponent(1)).getViewport().getView();
            usersArea.setText(String.join("\n", users));
        }
    }

public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        ChatInterface chatInterface = new ChatInterface();
        chatInterface.setSize(500, 500); // Set the size of the chat interface
        chatInterface.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Ensure the application closes on exit
        ChatClient client = new ChatClient("localhost", 12345, chatInterface);
        chatInterface.setChatClient(client);
//        chatInterface.setVisible(true); // Make the chat interface visible
    });
}

}