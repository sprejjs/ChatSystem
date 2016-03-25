package com.spreys;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

/**
 * Created by vspreys on 25/03/16.
 */
public class GUIChatClient {
    Socket socket = null;

    private JPanel loginPanel;
    private JTextField serverAddressTextField;
    private JTextField serverPortTextField;
    private JTextField usernameTextField;
    private JButton loginButton;
    private JLabel serverAddressLabel;
    private JLabel servertPortLabel;
    private JLabel nameLabel;

    public static void main(String[] args) throws Exception {
        GUIChatClient client = new GUIChatClient();

        JFrame frame = new JFrame("Chat System Login");
        frame.setContentPane(client.loginPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
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

    private void startChat(){
        while (true) {
            try {
                Thread.sleep(1000);
                BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
                System.out.print("Enter the number of the client you would like to chat with: ");
                String clientNumber = inFromUser.readLine();
                System.out.print("Enter your message: ");
                String message = inFromUser.readLine();

                SendToServer("MESSAGE||" + clientNumber + ":|:" + message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }

    private class ServerResponseReader implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String lineOfCode;
                    while ((lineOfCode = inFromServer.readLine()) != null) {
                        System.out.println(lineOfCode);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            } while (true);
        }
    }

    String ReadUserInput() {
        try {
            BufferedReader inFromUser = new BufferedReader( new InputStreamReader(System.in));
            return inFromUser.readLine();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
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
    }
    void SendToServer(String msg) throws Exception{
        //create output stream attached to socket
        PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        //send msg to server
        outToServer.print(msg + '\n');
        outToServer.flush();
    }
    String RecieveFromServer() throws Exception{
        //create input stream attached to socket
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        //read line from server
        String res = inFromServer.readLine(); // if connection closes on server end, this throws java.net.SocketException
        return res;
    }
    void close() throws IOException {
        socket.close();
    }

}
