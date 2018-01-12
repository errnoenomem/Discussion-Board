/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package CSE310Source;

/**
 *
 * @author 晓程哥
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

public class ServerThread implements Runnable {

    Socket threadSocket;
    //Server server;

    //This constructor will be passed the socket
    public ServerThread(Socket socket) {
        //Here we set the socket to a local variable so we can use it later
        threadSocket = socket;

    }

    public void run() {
        //All this should look familiar
        try {
            //Create the streams
            PrintWriter output = new PrintWriter(threadSocket.getOutputStream(), true);
            BufferedReader input = new BufferedReader(new InputStreamReader(threadSocket.getInputStream()));

            //Tell the client that he/she has connected
    
            
       output.println("Login successful!");
       //     server.commands();

            while (true) {
                //This will wait until a line of text has been sent
//                server.commands();

                String chatInput = input.readLine();
                System.out.println(chatInput);
            }
        } catch (IOException exception) {
            System.out.println("Error: " + exception);
        }
    }

}
