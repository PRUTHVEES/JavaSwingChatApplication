import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatInterface {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private ChatClient chatClient;

    public ChatInterface(ChatClient chatClient) {
        this.chatClient = chatClient; // Keep a reference to ChatClient
        initializeUI();
    }

    private void initializeUI() {
        frame = new JFrame("Chat Client");
        chatArea = new JTextArea(20, 50);
        messageField = new JTextField(40);
        sendButton = new JButton("Send");

        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);
        JPanel inputPanel = new JPanel();
        inputPanel.add(messageField);
        inputPanel.add(sendButton);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(); // Call send message when button is clicked
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(); // Call send message when Enter key is pressed
            }
        });
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            chatClient.sendMessage(message); // Send message through ChatClient
            messageField.setText(""); // Clear the input field
        }
    }

    public void clearChatArea() {
        chatArea.setText(""); // Clear chat area
    }

    public void displayMessage(String message) {
        chatArea.append(message + "\n"); // Append new message to chat area
    }

    public void receiveMessage(String message) {
        displayMessage(message); // Display received message in chat area
    }

    public static void main(String[] args) {
        String serverAddress = "localhost"; // Change as needed
        int port = 12345; // Change as needed

        // Prompt for username and password
        String username = JOptionPane.showInputDialog("Enter your username:");
        String password = JOptionPane.showInputDialog("Enter your password:");

        // Initialize ChatClient after getting the username and password
        ChatClient chatClient = new ChatClient(serverAddress, port, username, password);
    }
}
