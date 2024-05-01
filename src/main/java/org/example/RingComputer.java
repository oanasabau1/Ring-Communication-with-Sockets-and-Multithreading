package org.example;

import java.io.*;
import java.net.*;

public class RingComputer {
    private static volatile int init = 1;
    private static volatile int value = 1;
    private static final int NUM_COMPUTERS = 3;
    private static final int[] PORTS = { 3000, 3001, 3002 };
    private static final String[] ADDRESSES = { "127.0.0.1", "127.0.0.2", "127.0.0.3" };

    public static void main(String[] args) {
        startComputers();
    }

    private static void startComputers() {
        for (int i = 0; i < NUM_COMPUTERS; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    runComputer(index);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void runComputer(int index) throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(PORTS[index]);
        Socket clientSocket = new Socket(ADDRESSES[(index + 1) % NUM_COMPUTERS], PORTS[(index + 1) % NUM_COMPUTERS]);
        Socket connectionSocket = serverSocket.accept();
        BufferedReader inFromPrevious = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        PrintWriter outToNext = new PrintWriter(clientSocket.getOutputStream(), true);

        while (true) {
            if (init == 1) {
                synchronized (RingComputer.class) {
                    init++;
                    System.out.println("Computer " + index + " sends initial message: " + value);
                    outToNext.println(value);
                    value++;
                }
            } else {
                String message = inFromPrevious.readLine();
                System.out.println("Computer " + index + " received message: " + message);
                Integer valueRead = Integer.parseInt(message);
                if (valueRead >= 100 || value > 100) {
                    break;
                }

                synchronized (RingComputer.class) {
                    System.out.println("Computer " + index + " sends message: " + value);
                    outToNext.println(value);
                    value++;
                }
            }
            Thread.sleep(1000);
        }

        closeConnections(serverSocket, clientSocket, connectionSocket);
    }

    private static void closeConnections(ServerSocket serverSocket, Socket clientSocket, Socket connectionSocket) throws IOException {
        connectionSocket.close();
        clientSocket.close();
        serverSocket.close();
    }
}
