import java.io.*;
import java.net.*;

// The main class that creates the client socket and communicates with the server
public class Client {
    public static void main(String[] args) throws IOException {
        // Create a socket and connect to the server on localhost and port 5000
        Socket socket = new Socket("localhost", 5000);
        System.out.println("Connected to the server");
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        // Loop until exit command is sent by the server or typed by the user
        while (true) {
            // Read the message from the server and print it
            String message = input.readUTF();
            System.out.print(message);
            // Check if the message is an exit message
            if (message.equals("Exit command received. Closing the connection.\n")) {
                break;
            }
            // Read the command from the user and send it to the server
            String command = reader.readLine();
            output.writeUTF(command);
        }
        // Close the socket and streams
        socket.close();
        input.close();
        output.close();
    }
}
