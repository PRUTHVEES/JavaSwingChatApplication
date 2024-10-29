import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChatInterface {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private ChatClient chatClient;

    public ChatInterface() {
        initializeLoginUI();
    }

    // Initialize the login UI
    private void initializeLoginUI() {
        frame = new JFrame("Chat Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        connectButton = new JButton("Connect");

        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(connectButton);

        frame.add(loginPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        // Handle connect button press
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                // Close login UI and start the chat UI if credentials are provided
                if (!username.isEmpty() && !password.isEmpty()) {
                    frame.dispose();
                    startChat(username, password);
                } else {
                    JOptionPane.showMessageDialog(frame, "Please enter both username and password.");
                }
            }
        });
    }

    // Start chat UI after successful login
    private void startChat(String username, String password) {
        chatClient = new ChatClient("localhost", 12345, username, password);
        chatClient.setChatInterface(this);

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
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            chatClient.sendMessage(message);
            messageField.setText("");
        }
    }

    public void clearChatArea() {
        chatArea.setText("");
    }

    public void displayMessage(String message) {
        chatArea.append(message + "\n");
    }

    public void receiveMessage(String message) {
        displayMessage(message);
    }

    // Restart interface with an error message
    public void restartWithError(String errorMessage) {
        frame.dispose();
        new ChatInterface().displayMessage(errorMessage);
    }
}
