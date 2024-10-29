import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatInterface {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton sendButton;
    private JButton loginButton;
    private ChatClient chatClient;

    public ChatInterface(ChatClient chatClient) {
        this.chatClient = chatClient;
        initializeUI();
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

        // Panel for login fields
        JPanel loginPanel = new JPanel();
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(loginButton);
        frame.add(loginPanel, BorderLayout.NORTH);

        // Panel for chat input
        JPanel inputPanel = new JPanel();
        inputPanel.add(messageField);
        inputPanel.add(sendButton);
        frame.add(inputPanel, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        sendButton.setEnabled(false); // Disable send button until logged in

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            chatClient.sendLoginCredentials(username, password); // Send credentials to ChatClient
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
        chatArea.setText(""); // Clear chat area in case of login reset
    }

    public void displayMessage(String message) {
        chatArea.append(message + "\n");
    }

    public void receiveMessage(String message) {
        displayMessage(message);
    }
}
