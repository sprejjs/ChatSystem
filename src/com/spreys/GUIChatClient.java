package com.spreys;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vspreys on 25/03/16.
 */
public class GUIChatClient {
    Socket tcpSocket = null;
    DatagramSocket udpSocket;
    private int identifier;
    private static final String COMMAND_SEPARATOR = "||";
    private static final String COMMAND_CLIENTS_SEPARATOR = "|";
    private static final String COMMAND_CLIENTS_LIST = "CLIENTS";
    private static final String COMMAND_INCOMING_MESSAGE = "INCOMING";
    private static final String COMMAND_TCP_REGISTRATION_SUCCESS = "TCP_REG_SUCCESS";
    private List<User> users = new ArrayList<>();

    private JPanel rootPanel;
    private JTextField serverAddressTextField;
    private JTextField serverTcpPortTextField;
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
    private JTextField serverUdpPortTextField;

    public static void main(String[] args) throws Exception {
        GUIChatClient client = new GUIChatClient();

        JFrame frame = new JFrame("Chat System Login");
        frame.setContentPane(client.rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        client.messasagingPannel.setVisible(false);
    }

    private void registerTcp(String name) throws Exception {
        tcpSocket = new Socket(serverAddressTextField.getText(), Integer.valueOf(serverTcpPortTextField.getText()));
        //Listen to TCP messages from the server
        ServerTCPResponseReader serverResponseReader = new ServerTCPResponseReader();
        Thread responseThread = new Thread(serverResponseReader);
        responseThread.start();

        try {
            SendToServer("TCP_REGISTER||" + name);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void registerUdp(int connectionId) throws Exception {
        udpSocket = new DatagramSocket();

        int serverPort = Integer.valueOf(serverUdpPortTextField.getText());
        InetAddress inetAddress = InetAddress.getByName(serverAddressTextField.getText());

        byte[] message = ("UDP_REGISTER" + COMMAND_SEPARATOR + String.valueOf(connectionId)).getBytes();

        DatagramPacket sendPacket = new DatagramPacket(message, message.length, inetAddress, serverPort);
        udpSocket.send(sendPacket);

        //Listen to UDP messages from the server
        ServerUDPResponseReader serverUDPResponseReader = new ServerUDPResponseReader();
        Thread udpResponseThread = new Thread(serverUDPResponseReader);
        udpResponseThread.start();
    }

    private class ServerUDPResponseReader implements Runnable {

        @Override
        public void run() {
            try {
                byte[] receivedData = new byte[1024];

                while (true) {
                    DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
                    udpSocket.receive(receivedPacket);

                    String message = new String(receivedPacket.getData());

                    parseServerResponse(message);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class ServerTCPResponseReader implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                    String serverSentence;
                    while ((serverSentence = inFromServer.readLine()) != null) {
                        parseServerResponse(serverSentence);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } while (true);
        }
    }

    private void parseServerResponse(String response) {
        String code = response.substring(0, response.indexOf(COMMAND_SEPARATOR));

        String rowMessage = response.substring(response.indexOf(COMMAND_SEPARATOR) + COMMAND_SEPARATOR.length());

        System.out.println(response);

        if(code.equals(COMMAND_CLIENTS_LIST)) {
            users = parseClients(rowMessage);
            displayClients(users);
            loginPanel.setVisible(false);
            messasagingPannel.setVisible(true);
        }

        if(code.equals(COMMAND_INCOMING_MESSAGE)) {
            int clientId = Integer.valueOf(
                    rowMessage.substring(0, rowMessage.indexOf(COMMAND_CLIENTS_SEPARATOR))
            );
            String text = rowMessage.substring(
                    rowMessage.indexOf(COMMAND_CLIENTS_SEPARATOR) + COMMAND_CLIENTS_SEPARATOR.length()
            );

            Message incomingMessage = new Message(true, text);
            for (User user : users) {
                if (user.getId() == clientId) {
                    user.addMessage(incomingMessage);
                }
            }
            displayChat();
        }

        if(code.equals(COMMAND_TCP_REGISTRATION_SUCCESS)) {
            //Make a UDP connection to the server
            identifier = Integer.valueOf(rowMessage);
            try {
                registerUdp(identifier);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
            model.addElement(user.getUsername() + "[" + user.getId() + "]");
        }

        clientsList.setModel(model);
    }

    GUIChatClient(){
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    registerTcp(usernameTextField.getText());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int index = clientsList.getSelectedIndex();
                    String message = messageTextField.getText();
                    users.get(index).addMessage(new Message(false, message));
                    SendToServer("MESSAGE||" + users.get(index).getId() + ":|:" + message);
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
        PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
        //send msg to server
        outToServer.print(msg + '\n');
        outToServer.flush();
    }

}
