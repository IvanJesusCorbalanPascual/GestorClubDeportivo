package cliente;

import modelos.Club;
import modelos.Jugador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static boolean sesionIniciada = false;

    public static void main(String[] args) {

        String host = "localhost";
        int puerto = 5000;

        System.out.println("Iniciando cliente en " + host + ":" + puerto);

        try (Socket socket = new Socket(host, puerto);
             // Creando las herramientas para escribir y leer dentro del socket para hablar con el Servidor
             PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner sc = new Scanner(System.in)) {

            // Variable para llevar la cuenta de los mensajes (número de envío)
            int idMensaje = 1;
            String comandoAEnviar = "";

            do {
                // Si el usuario aun no ha iniciado sesion, solo tendrá acceso a este menú
                if (sesionIniciada == false) {
                    System.out.println("\n--- INICIO DE SESION ---");
                    System.out.println(" > USER <nombre>      (Ej: USER admin)");
                    System.out.println(" > PASS <contraseña>  (Ej: PASS admin)");
                    System.out.println(" > EXIT               (Salir del programa)");

                    // Cuando el usuario haya iniciado sesion, desbloqueará el acceso a todas las opciones del CRUD
                } else if (sesionIniciada == true) {
                    System.out.println("\n-------------- SISTEMA GESTIÓN DEPORTIVA ------[🍏GreenTonic🍏]------");
                    System.out.println(" > ADDCLUB    <id> <nombre>                     (Crea un nuevo club)");
                    System.out.println(" > LISTCLUBES                                   (Lista todos los clubes)");
                    System.out.println(" > GETCLUB    <id>                              (Ej: GETCLUB 1)");
                    System.out.println(" > UPDATECLUB <id>                              (Ej: UPDATECLUB 1)");
                    System.out.println(" > REMOVECLUB <id>                              (Ej: REMOVECLUB 1)");
                    System.out.println(" > COUNTCLUBES                                  (Cuenta total de clubes)");
                    System.out.println(" > ADDJUGADOR <id> <nombre> <apellidos> <goles> (Crea un nuevo jugador)");
                    System.out.println(" > GETJUGADOR    <id>                           (Crea un nuevo jugador)");
                    System.out.println(" > REMOVEJUGADOR <id>                           (Crea un nuevo jugador)");
                    System.out.println(" > LISTJUGADORES                                (Crea un nuevo jugador)");
                    System.out.println(" > ADDJUGADOR2CLUB   <idJugador> <idClub>       (Añade jugador a un club)");
                    System.out.println(" > REMOVEJUGFROMCLUB <idJugador> <idClub>       (Crea un nuevo jugador)");
                    System.out.println(" > LISTJUGFROMCLUB                              (Crea un nuevo jugador)");
                    System.out.println(" > EXIT                                         (Cierra sesión y sale)");
                }

                System.out.print("root@club-deportivo:~$ "); // Un prompt estilo terminal que queda chulo
                String inputUsuario = sc.nextLine().trim();

                if (inputUsuario.isEmpty()) continue; // Manejo de errores de mensajes en blanco
                String[] partes = inputUsuario.split(" ");
                comandoAEnviar = inputUsuario;

                // Concatenamos el ID del mensaje antes del comando
                String mensajeProtocolo = idMensaje + " " + comandoAEnviar;

                System.out.println("[Cliente envía]: " + mensajeProtocolo);
                pw.println(mensajeProtocolo);

                // Leemos la respuesta del servidor
                String respuesta = br.readLine();
                System.out.println("[Servidor responde]: " + respuesta);

                if (comandoAEnviar.toUpperCase().startsWith("PASS") && respuesta.contains(" 200 ")) {
                    sesionIniciada = true;
                    System.out.println("¡Login correcto! Accediendo al sistema...");
                }
                // En caso de que el servidor responda con PREOK
                if (respuesta != null && respuesta.toUpperCase().startsWith("PREOK")) {

                    if (comandoAEnviar.toUpperCase().startsWith("ADDCLUB")) {
                        Club nuevoClub = new modelos.Club(partes[1], partes[2]);
                        enviarObjetoAServidor(respuesta, nuevoClub);

                    } else if (comandoAEnviar.toUpperCase().startsWith("UPDATECLUB")) {
                        Club clubActualizado = new Club(partes[1], partes[2]);
                        enviarObjetoAServidor(respuesta, clubActualizado);
                    } else if (comandoAEnviar.toUpperCase().startsWith("ADDJUGADOR")) {
                        Jugador nuevoJugador = new Jugador(partes[1],partes[2],partes[3],Integer.parseInt(partes[4]));
                        enviarObjetoAServidor(respuesta, nuevoJugador);
                    } else { // Llama al mét0d0 que se encarga de la conexión
                        recibirDatosDeServidor(respuesta);
                    }

                    // Envia una confirmación final
                    String respuestaFinal = br.readLine();
                    System.out.println("[Servidor final]: " + respuestaFinal);
                }

                // Incrementamos el ID para el siguiente mensaje
                idMensaje++;

            } while (!comandoAEnviar.equalsIgnoreCase("EXIT"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recibirDatosDeServidor(String respuestaPreok) {
        try {
            // Parte el mensaje con espacios para sacar los datos facilmente
            String[] partes = respuestaPreok.split(" ");
            String ip = partes[3];
            int puerto = Integer.parseInt(partes[4]);

            System.out.println("Conectando a " + ip + ":" + puerto + " para descargar...");

            Socket socketDatos = new Socket(ip, puerto);

            // Prepara el lector de objetos
            java.io.ObjectInputStream ois = new java.io.ObjectInputStream(socketDatos.getInputStream());

            Object recibido = ois.readObject();

            // Comprueba que es y lo muestra
            if (recibido instanceof java.util.List) {
                System.out.println("Lista de clubes:");
                java.util.List lista = (java.util.List) recibido;

                if(!lista.isEmpty() && lista.get(0) instanceof modelos.Jugador) {
                    System.out.println("He recibido la lista de jugadores:");
                } else {
                    System.out.println("He recibido la lista de clubes:");
                }
                for (Object o : lista) {
                    System.out.println("       - " + o);
                }
            } else if (recibido instanceof modelos.Club) {
                System.out.println("He recibido un Club: " + recibido);
            } else if (recibido instanceof modelos.Jugador) {
                System.out.println("He recibido un Jugador: " + recibido);
            }

            ois.close();
            socketDatos.close();
            System.out.println("Datos recibidos con éxito");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

    }

    private static void enviarObjetoAServidor(String respuestaPreok, Object objetoAEnviar) {
        try {
            String[] partes = respuestaPreok.split(" ");
            String ip = partes[3];
            int puerto = Integer.parseInt(partes[4]);
            System.out.println("Conectando a " + ip + ":" + puerto + " para subir datos...");

            Socket socket = new Socket(ip, puerto);
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(objetoAEnviar);
            oos.flush();
            oos.close();
            socket.close();

            System.out.println("Datos enviados correctamente");

        } catch (IOException e) {
            System.out.println("Error al enviar: " + e.getMessage());

        }
    }
}