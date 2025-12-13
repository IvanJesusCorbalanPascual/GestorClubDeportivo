package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import modelos.Club;
import modelos.Jugador;

public class Server {
    // Listas estáticas que guardan los clubes y los jugadores
    public static final ArrayList<Club> clubes = new ArrayList<Club>();
    public static final ArrayList<Jugador> jugadores = new ArrayList<>();
    // Cuenta las sesiones de clientes conectados
    public static int clientesConectados = 0;

    public static void main(String[] args) throws IOException {

        try {
            int puerto = 5000;
            ServerSocket serverSocket = new ServerSocket(puerto);
            System.out.println("Servidor Iniciado en el puerto: " + puerto);

            // Crea y añade clubs de prueba
            Club c1 = new Club("C1", "Real Madrid");
            Club c2 = new Club("C2", "FC Barcelona");
            Club c3 = new Club("C3", "Valencia CF");

            clubes.add(c1);
            clubes.add(c2);
            clubes.add(c3);

            // Crea y añade jugadores de prueba
            Jugador j1 = new Jugador("J1", "Vinicius", "Jr", 15);
            Jugador j2 = new Jugador("J2", "Lamine", "Yamal", 10);
            Jugador j3 = new Jugador("J3", "Hugo", "Duro", 12);
            Jugador j4 = new Jugador("J4", "Antoine", "Griezmann", 8);

            jugadores.add(j1);
            jugadores.add(j2);
            jugadores.add(j3);
            jugadores.add(j4);

            // Añade jugadores dentro de un club para futuras pruebas
            c1.addJugador(j1.getId(), j1); // Vinicius, Madrid
            c3.addJugador(j3.getId(), j3); // Hugo Duro, Valencia

            System.out.println("Datos cargados: " + clubes.size() + " clubes y " + jugadores.size() + " jugadores.");

            while (true) { // El Servidor está constantemente aceptando peticiones de los Clientes
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
