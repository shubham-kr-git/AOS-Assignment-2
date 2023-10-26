import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

// A class to store a key-value pair
class Pair {
    String key;
    String value;

    public Pair(String key, String value) {
        this.key = key;
        this.value = value;
    }
}

// A class to handle the client requests
class ClientHandler implements Runnable {
    private Socket socket; // The socket for communication
    private DataInputStream input; // The input stream
    private DataOutputStream output; // The output stream
    private String username; // The username of the client
    private static List<ClientHandler> clients = new ArrayList<>(); // The list of all connected clients
    private static Map<String, String> store = new ConcurrentHashMap<>(); // The key-value store
    private static Semaphore semaphore = new Semaphore(1); // The semaphore to synchronize the writes

    public ClientHandler(Socket socket, DataInputStream input, DataOutputStream output) {
        this.socket = socket;
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            // Ask the client for the username
            output.writeUTF("Enter your username: ");
            username = input.readUTF();
            // Add the client to the list and broadcast the connection message
            clients.add(this);
            broadcast(username + " has connected\n");
            // Loop until the client sends exit command
            while (true) {
                // Read the command from the client
                String command = input.readUTF();
                // Parse the command and execute it
                switch (command.split(" ")[0]) {
                    case "put":
                        put(command);
                        break;
                    case "get":
                        get(command);
                        break;
                    case "del":
                        del(command);
                        break;
                    case "store":
                        store();
                        break;
                    case "exit":
                        exit();
                        return;
                    default:
                        output.writeUTF("Invalid command\n");
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                // Close the socket and streams
                socket.close();
                input.close();
                output.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    // A method to perform a put operation
    private void put(String command) throws IOException {
        try {
            // Acquire the semaphore to ensure exclusive access to the store
            semaphore.acquire();
            // Split the command by space and check if it has two arguments
            String[] args = command.split(" ");
            if (args.length == 3) {
                // Store the key-value pair in the store
                store.put(args[1], args[2]);
                output.writeUTF("Put operation successful\n");
            } else {
                output.writeUTF("Put operation requires two arguments\n");
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        } finally {
            // Release the semaphore after finishing the operation
            semaphore.release();
        }
    }

    // A method to perform a get operation
    private void get(String command) throws IOException {
        // Split the command by space and check if it has one argument
        String[] args = command.split(" ");
        if (args.length == 2) {
            // Check if the store contains the key and return the value or null
            if (store.containsKey(args[1])) {
                output.writeUTF("The value of " + args[1] + " is " + store.get(args[1]) + "\n");
            } else {
                output.writeUTF("The key " + args[1] + " does not exist\n");
            }
        } else {
            output.writeUTF("Get operation requires one argument\n");
        }
    }

    // A method to perform a del operation
    private void del(String command) throws IOException {
        try {
            // Acquire the semaphore to ensure exclusive access to the store
            semaphore.acquire();
            // Split the command by space and check if it has one argument
            String[] args = command.split(" ");
            if (args.length == 2) {
                // Remove the key-value pair from the store if it exists
                if (store.containsKey(args[1])) {
                    store.remove(args[1]);
                    output.writeUTF("Del operation successful\n");
                } else {
                    output.writeUTF("The key " + args[1] + " does not exist\n");
                }
            } else {
                output.writeUTF("Del operation requires one argument\n");
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        } finally {
            // Release the semaphore after finishing the operation
            semaphore.release();
        }
    }

    // A method to display the store contents
    private void store() throws IOException {
        // Create a list of pairs from the store entries
        List<Pair> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : store.entrySet()) {
            pairs.add(new Pair(entry.getKey(), entry.getValue()));
        }
        // Sort the list by the key in ascending order
        Collections.sort(pairs, new Comparator<Pair>() {
            @Override
            public int compare(Pair p1, Pair p2) {
                return p1.key.compareTo(p2.key);
            }
        });
        // Append the pairs to a string builder and send it to the client
        StringBuilder sb = new StringBuilder();
        sb.append("The store contains:\n");
        for (Pair pair : pairs) {
            sb.append(pair.key + " : " + pair.value + "\n");
        }
        output.writeUTF(sb.toString());
    }

    // A method to exit the client and server
    private void exit() throws IOException {
        // Remove the client from the list and broadcast the exit message
        clients.remove(this);
        broadcast(username + " has exited\n");
        output.writeUTF("Exit command received. Closing the connection.\n");
        // Check if there are no more clients and shutdown the server
        if (clients.isEmpty()) {
            System.out.println("No more clients. Shutting down the server.");
            System.exit(0);
        }
    }

    // A method to broadcast a message to all clients
    private void broadcast(String message) throws IOException {
        // Loop through all clients and send the message to their output streams
        for (ClientHandler client : clients) {
            client.output.writeUTF(message);
        }
    }
}

// The main class that creates the server socket and accepts the client connections
public class Server {
    public static void main(String[] args) throws IOException {
        // Create a server socket on port 5000
        ServerSocket server = new ServerSocket(5000);
        System.out.println("Server started on port 5000");
        // Loop until exit command is received from a client
        while (true) {
            // Accept a client connection and create a new thread to handle it
            Socket socket = server.accept();
            System.out.println("A new client is connected: " + socket);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            ClientHandler handler = new ClientHandler(socket, input, output);
            Thread thread = new Thread(handler);
            thread.start();
            System.out.println("A new thread is created for the client");
        }
    }
}
