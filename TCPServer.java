package site.withoutcaps.tcpclientserversample.TCP;


import android.util.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;

/*
    Hello, would you like to hear a TCP joke?

    Yes, I'd like to hear a TCP joke.

    OK, I'll tell you a TCP joke.

    OK, I'll hear a TCP joke.

    Are you ready to hear a TCP joke?

    Yes, I am ready to hear a TCP joke.

    OK, I'm about to send the TCP joke. It will last 10 seconds, It has two
    characters, it does not have a setting, it ends with a punchline.

    OK, I'm ready to hear the TCP joke. That will last 10 seconds, has two
    characters, does not have a setting and will end with a punchline.

    #> I'm sorry, your connection has timed out...

    Hello, would you like to hear a TCP joke?
 */


public class TCPServer {
    private OnMessageReceived mMessageListener;
    private OnConnect mConnectListener;
    private OnDisconnect mDisconnectListener;
    private OnServerClose mServerClosedListener;
    private OnServerStart mServerStartListener;

    private ServerSocket serverSocket;
    private short lastClientIndex = 0;
    private Map<Integer, Client> clients = new HashMap<>();
    private Boolean mRun = false;
    private static final String TAG = "TCPServer";

    public void startServer(String port) {
        startServer(Integer.valueOf(port));
    }

    public void startServer(int port) {
        mRun = true;
        Socket socket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            mRun = false;
        }
        Log.d(TAG, "startServer: " + (mServerStartListener != null));
        if (mServerStartListener != null)
            mServerStartListener.serverStarted(port);
        while (mRun) {
            System.out.println("Accepting client");
            try {
                socket = serverSocket.accept();
                Client client = new Client(socket);
                lastClientIndex++;
                clients.put((int) lastClientIndex, client);
                new Thread(client).start();
                client.setIndex(lastClientIndex);
                if (mConnectListener != null)
                    mConnectListener.connected(socket, socket.getLocalAddress(), +socket.getLocalPort(), socket.getLocalSocketAddress(), lastClientIndex);
            } catch (IOException e) {
                mRun = false;
                break;
            }
        }
        if (mServerClosedListener != null)
            mServerClosedListener.serverClosed(port);
    }

    public void closeServer() {
        try {
            Log.d(TAG, "closeServer: ");
            mRun = false;
            serverSocket.close();
            kickAll();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void kickAll() {
        for (Client client : clients.values())
            client.kill();
    }

    public void kick(int clientIndex) {
        clients.get(clientIndex).kill();
    }

    public void sendln(int clientIndex, String message) {
        clients.get(clientIndex).getOutput().println(message);
        clients.get(clientIndex).getOutput().flush();
    }

    public void send(int clientIndex, String message) {
        clients.get(clientIndex).getOutput().print(message);
        clients.get(clientIndex).getOutput().flush();
    }

    public void broadcast(String message) {
        for (Client client : clients.values()) {
            client.getOutput().print(message);
            client.getOutput().flush();
        }
    }

    public void broadcastln(String message) {
        for (Client client : clients.values()) {
            client.getOutput().println(message);
            client.getOutput().flush();
        }
    }

    public Boolean isServerRunning() {
        return mRun;
    }

    public Map<Integer, Client> getClients() {
        return clients;
    }

    public int getClientsCount() {
        return clients.size();
    }

//---------------------------------------------[Listeners]----------------------------------------------//

    public void setOnMessageReceivedListener(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    public void setOnConnectListener(OnConnect listener) {
        mConnectListener = listener;
    }

    public void setOnDisconnectListener(OnDisconnect listener) {
        mDisconnectListener = listener;
    }

    public void setOnServerClosedListener(OnServerClose listener) {
        mServerClosedListener = listener;
    }

    public void setOnServerStartListener(OnServerStart listener) {
        mServerStartListener = listener;
    }


//---------------------------------------------[Interfaces]---------------------------------------------//

    public interface OnMessageReceived {
        public void messageReceived(String message, int clientIndex);
    }

    public interface OnConnect {
        public void connected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex);
    }

    public interface OnDisconnect {
        public void disconnected(Socket socket, InetAddress localAddress, int port, SocketAddress localSocketAddress, int clientIndex);
    }

    public interface OnServerClose {
        public void serverClosed(int port);
    }

    public interface OnServerStart {
        public void serverStarted(int port);
    }


//--------------------------------------------[Client class]--------------------------------------------//

    public class Client implements Runnable {

        private PrintWriter output;
        private Socket socket;
        private BufferedReader input;
        private int clientIndex;


        public Client(Socket clientSocket) {
            this.socket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void run() {
            while (mRun) {
                System.out.println("Read line (Client: " + clientIndex + ")");
                try {

                    String line = input.readLine();
                    System.out.println(line);
                    if (mMessageListener != null)
                        if (line == null) {
                            socket.close();
                            clients.remove(clientIndex);
                            if (mDisconnectListener != null)
                                mDisconnectListener.disconnected(socket, socket.getLocalAddress(), +socket.getLocalPort(), socket.getLocalSocketAddress(), clientIndex);
                            break;
                        } else
                            mMessageListener.messageReceived(line, clientIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void kill() {
            try {
                socket.shutdownInput();
            } catch (Exception e) {
            }
            try {
                socket.shutdownOutput();
            } catch (Exception e) {
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setIndex(int index) {
            this.clientIndex = index;
        }

        private PrintWriter getOutput() {
            return output;
        }

        public Socket getSocket() {
            return socket;
        }
    }
}



