package chatPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatInterface extends JFrame {
    private JTextArea chatArea;
    private JTextField messageInput;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private ChatClient chatClient; // Reference to the ChatClient

    public ChatInterface(String username) {
        setTitle("Chat Interface");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set a chat-like font
        Font chatFont = new Font("Arial", Font.PLAIN, 14);
        Font userListFont = new Font("Arial", Font.PLAIN, 12);
        Font inputFont = new Font("Arial", Font.PLAIN, 16);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setFont(chatFont);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        add(chatScrollPane, BorderLayout.CENTER);

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(userListFont);
        userList.setBorder(BorderFactory.createTitledBorder("Users"));
        userList.setPreferredSize(new Dimension(100, 0));
        add(new JScrollPane(userList), BorderLayout.EAST);

        // Make the user list resizable horizontally
        userList.setPreferredSize(new Dimension(150, 0)); // Set initial width
        add(new JScrollPane(userList), BorderLayout.EAST);

        // Message input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        messageInput = new JTextField();
        messageInput.setFont(inputFont);
        sendButton = new JButton("Send");
        sendButton.setFont(inputFont);

        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(inputPanel, BorderLayout.SOUTH);

        // Send button action
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Send message on Enter key press
        messageInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Initialize the ChatClient and connect to the server
        startChatClient(username);
    }

    // Method to initialize ChatClient
    private void startChatClient(String username) {
        chatClient = new ChatClient("localhost", 12345, this, username);
    }

    // Method to send messages from the UI to the server via ChatClient
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (!message.isEmpty()) {
            if (chatClient != null) {
                chatArea.append(chatClient.getUsername() + ": " + message + "\n");
                messageInput.setText("");
                chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Scroll to the bottom
                chatClient.sendMessage(message); // Send the message to the server
            } else {
                chatArea.append("Error: Not connected to the server.\n");
            }
        }
    }

    // Method to receive messages from the server and display them in the chat area
    public void receiveMessage(String message) {
        if (message.startsWith("USER_LIST:")) {
            updateUserList(message.substring(10)); // Update the user list when the message starts with USER_LIST:
        } else {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Scroll to the bottom
        }
    }

    // Method to clear the chat area
    public void clearChatArea() {
        chatArea.setText(""); // Clear the chat area upon successful connection
    }

    // Method to display a message in the chat area
    public void displayMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // Scroll to the bottom
    }

    // Method to update the user list when a new list is received from the server
    private void updateUserList(String userList) {
        userListModel.clear(); // Clear the existing list
        String[] users = userList.split(",");
        for (String user : users) {
            userListModel.addElement(user.equals(chatClient.getUsername()) ? "(You) " + user : user); // Prefix "(You)" for the user's own name
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Enter your username:");
        SwingUtilities.invokeLater(() -> {
            ChatInterface chatInterface = new ChatInterface(username);
            chatInterface.setVisible(true);
        });
    }
}
