package com.spreys;

import java.io.*;
import java.net.Socket;

/**
 * Created by vspreys on 25/03/16.
 */
public class ChatClient {

    String host = "localhost";
    int port = 6790;
    Socket socket = null;
    public static void main(String args[]) throws Exception{
        ChatClient client = new ChatClient();
        client.register();
        client.startChat();
        client.close();
    }

    private void register() {
        //Listen to messages from the server
        ServerResponseReader serverResponseReader = new ServerResponseReader();
        Thread responseThread = new Thread(serverResponseReader);
        responseThread.start();

        try {
            System.out.print("Connecting to the chat server. Enter your name: ");
            SendToServer("REGISTER||" + ReadUserInput());
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

    ChatClient(String _host, int _port) throws Exception{
        host = _host;
        port = _port;
        socket = new Socket(host, port);
    }
    ChatClient() throws Exception{
        socket = new Socket(host, port);
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
