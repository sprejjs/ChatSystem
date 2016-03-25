package com.spreys;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vspreys on 25/03/16.
 */
public class GUIChatClient {
    Socket socket = null;
    private static final String COMMAND_SEPARATOR = "||";
    private static final String COMMAND_CLIENTS_LIST = "CLIENTS";
    private static final String COMMAND_INCOMING_MESSAGE = "INCOMING";
    private static final String COMMAND_CLIENTS_SEPARATOR = "|";
    private List<User> users = new ArrayList<>();

    private JPanel rootPanel;
    private JTextField serverAddressTextField;
    private JTextField serverPortTextField;
    private JTextField usernameTextField;
    private JButton loginButton;
    private JLabel serverAddressLabel;
    private JLabel servertPortLabel;
    private JLabel nameLabel;
    private JPanel loginPanel;
    private JPanel messasagingPannel;
    private JList clientsList;
    private JTextField messageTextField;
    private JButton sendButton;
    private JList messagesList;

    public static void main(String[] args) throws Exception {
        GUIChatClient client = new GUIChatClient();

        JFrame frame = new JFrame("Chat System Login");
        frame.setContentPane(client.rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        client.messasagingPannel.setVisible(false);
    }

    private void register(String name) {
        //Listen to messages from the server
        ServerResponseReader serverResponseReader = new ServerResponseReader();
        Thread responseThread = new Thread(serverResponseReader);
        responseThread.start();

        try {
            SendToServer("REGISTER||" + name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private class ServerResponseReader implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String serverSentence;
                    while ((serverSentence = inFromServer.readLine()) != null) {

                        String code = serverSentence.substring(0, serverSentence.indexOf(COMMAND_SEPARATOR));

                        if(code.equals(COMMAND_CLIENTS_LIST)) {
                            String rowListOfClients = serverSentence
                                    .substring(serverSentence.indexOf(COMMAND_SEPARATOR) + COMMAND_SEPARATOR.length());

                            users = parseClients(rowListOfClients);
                            displayClients(users);
                            loginPanel.setVisible(false);
                            messasagingPannel.setVisible(true);
                        }

                        if(code.equals(COMMAND_INCOMING_MESSAGE)) {
                            String rowMessage = serverSentence
                                    .substring(serverSentence.indexOf(COMMAND_SEPARATOR) + COMMAND_SEPARATOR.length());

                            int clientId = Integer.valueOf(
                                    rowMessage.substring(0, rowMessage.indexOf(COMMAND_CLIENTS_SEPARATOR))
                            );
                            String text = rowMessage.substring(
                                    rowMessage.indexOf(COMMAND_CLIENTS_SEPARATOR) + COMMAND_CLIENTS_SEPARATOR.length()
                            );

                            Message incomingMessage = new Message(true, text);
                            users.get(clientId).addMessage(incomingMessage);
                            displayChat();
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } while (true);
        }
    }

    private List<User> parseClients (String rowListOfClients) {
        List<User> users = new ArrayList<>();
        while (rowListOfClients.indexOf(COMMAND_CLIENTS_SEPARATOR) > 0) {

            int firstOccurrenceOfSeparator = rowListOfClients.indexOf(COMMAND_CLIENTS_SEPARATOR);
            int secondOccurrenceOfSeparator = rowListOfClients.indexOf(
                    COMMAND_CLIENTS_SEPARATOR, rowListOfClients.indexOf(COMMAND_CLIENTS_SEPARATOR) + 1
            );

            int id = Integer.valueOf(rowListOfClients.substring(0, firstOccurrenceOfSeparator));
            String name = rowListOfClients.substring(firstOccurrenceOfSeparator + 1,
                    secondOccurrenceOfSeparator);

            users.add(new User(id, name));

            rowListOfClients = rowListOfClients.substring(secondOccurrenceOfSeparator + 1);
        }

        return users;
    }

    private void displayClients(List<User> users) {
        DefaultListModel model = new DefaultListModel();

        for (User user : users) {
            model.addElement(user.getUsername());
        }

        clientsList.setModel(model);
    }

    GUIChatClient(){
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    socket = new Socket(serverAddressTextField.getText(), Integer.valueOf(serverPortTextField.getText()));
                    register(usernameTextField.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int clientNumber = clientsList.getSelectedIndex();
                    String message = messageTextField.getText();
                    users.get(clientNumber).addMessage(new Message(false, message));
                    SendToServer("MESSAGE||" + clientNumber + ":|:" + message);
                    displayChat();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        clientsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                displayChat();
            }
        });
    }

    private void displayChat() {
        User selectedRecipient = users.get(clientsList.getSelectedIndex());

        DefaultListModel model = new DefaultListModel();

        for (Message message : selectedRecipient.getMessages()) {
            String text = (message.getIncoming() ? selectedRecipient.getUsername() + ": " : "You: ") + message.getMessage();
            model.addElement(text);
        }

        messagesList.setModel(model);
    }

    void SendToServer(String msg) throws Exception{
        //create output stream attached to socket
        PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        //send msg to server
        outToServer.print(msg + '\n');
        outToServer.flush();
    }

}
