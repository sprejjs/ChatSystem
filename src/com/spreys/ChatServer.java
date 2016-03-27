package com.spreys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by vspreys on 25/03/16.
 */
public class ChatServer {
    private String COMMAND_SEPARATOR = "||";
    private List<Connection> clients = new ArrayList<>();

    public static void main (String args[]) throws Exception{
        new ChatServer();
    }
    ChatServer() throws Exception{
        //create welcoming socket at port 6789
        ServerSocket welcomeSocket = new ServerSocket(6790);

        UdpConnection udpConnection = new UdpConnection();
        Thread udpConnectionThread = new Thread(udpConnection);
        udpConnectionThread.start();

        while (true) {
            //block on welcoming socket for contact by a client
            Socket connectionSocket = welcomeSocket.accept();
            // create thread for client
            Connection c = new Connection(connectionSocket);
            clients.add(c);
        }
    }

    private class UdpConnection extends Thread {
        public void run() {
            try {
                DatagramSocket serverSocket = new DatagramSocket(6791);
                byte[] receiveData = new byte[1024];
                while(true)
                {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    byte[] data = new byte[receivePacket.getLength()];
                    System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
                    String sentence = new String(data);
                    System.out.println("RECEIVED: " + sentence);

                    String code = sentence.substring(0, sentence.indexOf(COMMAND_SEPARATOR));
                    if(code.equals("UDP_REGISTER")) {
                        //find the client code
                        int clientCode = Integer.valueOf(
                                sentence.substring(sentence.indexOf(COMMAND_SEPARATOR) + COMMAND_SEPARATOR.length())
                        );

                        for (Connection connection : clients) {
                            if(connection.identifier == clientCode) {
                                connection.udpAddress = receivePacket.getAddress();
                                connection.udpPort = receivePacket.getPort();
                                connection.udpSocket = serverSocket;
                            }
                        }

                        sendClientsList();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class Connection extends Thread {
        private String clientName;
        private int identifier;
        Socket tcpSocket;
        DatagramSocket udpSocket;
        InetAddress udpAddress;
        int udpPort;

        private String messageSeparator = ":|:";
        Connection(Socket tcpSocket){
            this.tcpSocket = tcpSocket;
            this.start();
        }
        public void run(){
            try{
                //create input stream attached to socket
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
                //create output stream attached to socket
                PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
                //read in line from the socket
                String clientSentence;
                while ((clientSentence = inFromClient.readLine()) != null) {

                    //Get the command code
                    String code = clientSentence.substring(0, clientSentence.indexOf(COMMAND_SEPARATOR));

                    //Process the command
                    String response = null;
                    if(code.equals("TCP_REGISTER")) {
                        response = registerTcp(clientSentence);
                    }

                    if(code.equals("MESSAGE")){
                        processMessage(clientSentence);
                    }

                    //Return the response back to the client
                    if(response != null) {
                        outToClient.print(response);
                        outToClient.flush();
                    }
                }
            }catch(Exception e){}
        }

        private void processMessage(String messageCommand) {
            String recipientCode = messageCommand.substring(
                    messageCommand.indexOf(COMMAND_SEPARATOR) + COMMAND_SEPARATOR.length(),
                    messageCommand.indexOf(messageSeparator)
                    );
            String message = messageCommand.substring(messageCommand.indexOf(messageSeparator) + messageSeparator.length());
            System.out.println("Incoming message from " + clientName + ". Client Code: " + recipientCode + ". Message: " + message);

            sendMessageToTheClient(message, Integer.valueOf(recipientCode));
        }

        private void sendMessageToTheClient(String message, int clientId) {
            Connection client = clients.get(clientId);

            try {
                PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(client.tcpSocket.getOutputStream()));
                outToClient.println("INCOMING||" + clientId + "|" + message);
                outToClient.flush();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private String registerTcp(String registrationCommand) {
            clientName = registrationCommand.substring(registrationCommand.indexOf(COMMAND_SEPARATOR) + COMMAND_SEPARATOR.length());

            System.out.println("Client connected. Client name is: " + clientName);

            //Generating unique identifier
            Random rand = new Random();
            identifier = rand.nextInt(10000) + 1;

            return "TCP_REG_SUCCESS" + COMMAND_SEPARATOR + String.valueOf(identifier) + "\n";
        }
    }

    private void sendClientsList() {
        String output = "CLIENTS||";
        for (int i = 0; i < clients.size(); i++) {
            output += String.valueOf(i) + "|" + clients.get(i).clientName + "|";
        }

        try {
            for(Connection connection : clients) {
                byte[] sendData = output.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        connection.udpAddress,
                        connection.udpPort);
                connection.udpSocket.send(sendPacket);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
