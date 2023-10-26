import java.io.*;
import java.net.*;

// The main class that creates the client socket and communicates with the server
public class Client {
    // Declare the running variable as a static volatile field of the Client class
    private static volatile boolean running = true;

    public static void main(String[] args) throws IOException {
        // Create a socket and connect to the server on localhost and port 5000
        Socket socket = new Socket("localhost", 5000);
        System.out.println("Connected to the server");
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        // Create a new thread for reading and printing the message from the server
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Loop until exit message is received from the server
                    while (running) {
                        // Read the message from the server and print it
                        String message = input.readUTF();
                        System.out.print(message);
                        // Check if the message is an exit message
                        if (message.equals("Exit command received. Closing the connection.\n")) {
                            // Set the running variable to false to terminate the thread
                            running = false;
                        }
                    }
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        });
        // Start the thread
        thread.start();
        // Loop until exit command is typed by the user
        while (running) {
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
