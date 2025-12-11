package servidor;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import modelos.Club;

public class Server {
    public static final ArrayList<Club> clubes = new ArrayList<Club>();

    public static void main(String[] args) throws IOException {

        try {
            int puerto = 5000;
            ServerSocket serverSocket = new ServerSocket(puerto);
            System.out.println("Servidor Iniciado en el puerto: " + puerto);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado desde: " + socket.getInetAddress());
                ServerThread serverThread = new ServerThread(socket);
                serverThread.start();

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
