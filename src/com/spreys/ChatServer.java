package com.spreys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vspreys on 25/03/16.
 */
public class ChatServer {
    private List<Connection> clients = new ArrayList<>();
    public static void main (String args[]) throws Exception{
        new ChatServer();
    }
    ChatServer() throws Exception{
        //create welcoming socket at port 6789
        ServerSocket welcomeSocket = new ServerSocket(6790);

        while (true) {
            //block on welcoming socket for contact by a client
            Socket connectionSocket = welcomeSocket.accept();
            // create thread for client
            Connection c = new Connection(connectionSocket);
            clients.add(c);
        }
    }
    class Connection extends Thread{
        private String clientName;
        private String commandSeparator = "||";
        private String messageSeparator = ":|:";

        Socket connectionSocket;
        Connection(Socket _connectionSocket){
            connectionSocket = _connectionSocket;
            this.start();
        }
        public void run(){
            try{
                //create input stream attached to socket
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                //create output stream attached to socket
                PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(connectionSocket.getOutputStream()));
                //read in line from the socket
                String clientSentence;
                while ((clientSentence = inFromClient.readLine()) != null) {

                    //Get the command code
                    String code = clientSentence.substring(0, clientSentence.indexOf(commandSeparator));

                    //Process the command
                    String response = "";
                    if(code.equals("REGISTER")) {
                        response = register(clientSentence);
                    }

                    if(code.equals("MESSAGE")){
                        response = processMessage(clientSentence);
                    }

                    //Return the response back to the client
                    outToClient.print(response);
                    outToClient.flush();
                }
            }catch(Exception e){}
        }

        private String processMessage(String messageCommand) {
            String recipientCode = messageCommand.substring(
                    messageCommand.indexOf(commandSeparator) + commandSeparator.length(),
                    messageCommand.indexOf(messageSeparator)
                    );
            String message = messageCommand.substring(messageCommand.indexOf(messageSeparator) + messageSeparator.length());
            System.out.println("Incoming message from " + clientName + ". Client Code: " + recipientCode + ". Message: " + message);

            sendMessageToTheClient(message, Integer.valueOf(recipientCode));

            return "Message delivered \n";
        }

        private void sendMessageToTheClient(String message, int clientId) {
            Connection client = clients.get(clientId);

            try {
                PrintWriter outToClient = new PrintWriter(new OutputStreamWriter(client.connectionSocket.getOutputStream()));
                outToClient.println("INCOMING||" + clientId + "|" + message);
                outToClient.flush();

            } catch (Exception ex) {
                ex.printStackTrace();
            }


        }

        private String register(String registrationCommand) {
            clientName = registrationCommand.substring(registrationCommand.indexOf(commandSeparator) + commandSeparator.length());

            System.out.println("Client connected. Client name is: " + clientName);

            String output = "CLIENTS||";
            for (int i = 0; i < clients.size(); i++) {
                output += String.valueOf(i) + "|" + clients.get(i).clientName + "|";
            }

            return output + "\n";
        }
    }
}
