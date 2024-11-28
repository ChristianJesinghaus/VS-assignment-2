package de.luh.vss.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import de.luh.vss.chat.common.Message.ChatMessage;
import de.luh.vss.chat.common.Message.RegisterRequest;
import java.net.InetAddress;
import de.luh.vss.chat.common.Message.ErrorResponse;
import de.luh.vss.chat.common.MessageType;
import de.luh.vss.chat.common.Message;
import de.luh.vss.chat.common.User.UserId;
import de.luh.vss.chat.common.User.*;


import java.util.Scanner;

/**
 * ChatClient class to handle communication with the server.
 */
public class ChatClient {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    /**
     * Main method to start the ChatClient.
     * @param args Command line arguments
     */
    public static void main(String... args) {
        try {
            new ChatClient().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the ChatClient and handles communication with the server.
     * @throws IOException If an I/O error occurs
     */
    public void start() throws IOException {
        System.out.println("Congratulation for successfully setting up your environment for Assignment 1!");

        // Connect to the server using the IP address and port
        socket = new Socket("130.75.202.197", 4444);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        // Enable OOB data
        socket.setOOBInline(true);

        // Create a Scanner object for user input
        Scanner myObj = new Scanner(System.in);

        // Enter UserId
        System.out.println("Enter UserId");
        int userId = Integer.parseInt(myObj.nextLine()); // Read user input and convert to int

        // Create UserId object
        UserId user = new UserId(userId);
        InetAddress address = InetAddress.getByName("130.75.202.197");

        // Send the trigger message to initiate the test
        System.out.println("Enter Message");
        String triggerMessage = myObj.nextLine(); // Read user input
        //ChatMessage triggerMsg = new ChatMessage(user, triggerMessage);
        RegisterRequest triggerMsg = new RegisterRequest(user,address, 4444);
        triggerMsg.toStream(output);

        // Setting maximum Byte length for messages
        int MAX_MESSAGE_LENGTH = 4000;

        // Thread to receive messages from the server
        new Thread(() -> {
            try {
                while (true) {
                    int inputLength = input.available();
                    if (inputLength > 0) {
                        // Read the first byte to check for message type
                        int messageType = input.readInt();

                        // Check if it is one of the defined message types
                        if (messageType == 4 || messageType == 2 || messageType == 1 || messageType == 0) {
                            // Convert Input to Message
                            Message receivedMsg = MessageType.fromInt(messageType, input);

                            // Check if it's a ChatMessage to handle it correspondingly
                            if (receivedMsg instanceof ChatMessage) {
                                ChatMessage chatMsg = (ChatMessage) receivedMsg;

                                // Calculate the length of the message in Bytes using just *2
                                String messageString = chatMsg.getMessage();
                                int messageBytes = messageString.length();
                                int byteLength = messageBytes * 2;

                                // Handle it if the message exceeds the limit
                                if (byteLength > MAX_MESSAGE_LENGTH) {
                                    // Calculate the amount of bytes above the limit and send that back to the server
                                    int diff = byteLength - MAX_MESSAGE_LENGTH;
                                    String diffStr = String.valueOf(diff);
                                    ChatMessage diffMsg = new ChatMessage(user, diffStr);
                                    diffMsg.toStream(output);
                                } else {
                                    // If it's a ChatMessage and it doesn't exceed the limit, echo it back to the server
                                    chatMsg.toStream(output);
                                }
                            }
                            // Handle errorResponses of the server and print it to the console
                            if (receivedMsg instanceof ErrorResponse) {
                                ErrorResponse chatMsg = (ErrorResponse) receivedMsg;
                                System.out.println(chatMsg.toString());
                            }
                        } else { // If it is none of the known types handle it as if it's OOB data
                            System.out.println("Received OOB data"); // Let user know that OOB Data arrived
                            byte[] oobData = new byte[input.available()];
                            int length = input.read(oobData);
                            String oobMessage = new String(oobData, 0, length, "UTF-8");
                            System.out.println("OOB Message: " + oobMessage); // Print the OOB data
                        }
                    }
                }
            } catch (IOException | ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }).start();

        // Loop to send multiple messages
        while (true) {
            System.out.println("Enter Message");
            String msg = myObj.nextLine(); // Read user input

            ChatMessage newMsg = new ChatMessage(user, msg);

            // Send message to the server
            newMsg.toStream(output);
        }
    }
}