package cliente;

import modelos.Club;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
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

            // Variable para llevar la cuenta de los mensajes (Protocolo: <number> <comando>)
            int idMensaje = 1;
            String opcionUsuario = "";


            do {
                String comandoAEnviar = "";
                if (sesionIniciada == false) {
                    System.out.println("\n--- CLIENTE CLUB DEPORTIVO ---");
                    System.out.println("Escribe el comando completo (Ej: USER admin)");
                    System.out.println("1. Introducir Usuario USER <usuario>");
                    System.out.println("2. Introducir Clave -> PASS <clave>");
                    System.out.println("3. Salir -> (EXIT)");
                    System.out.print("➡ ");
                    opcionUsuario = sc.nextLine();

                    switch (opcionUsuario) {
                        case "1":
                            System.out.println("Introduce el nombre del usuario");
                            String username = sc.nextLine();
                            comandoAEnviar = "USER " + username;
                            break;
                        case "2":
                            System.out.println("Introduce la Contraseña");
                            String password = sc.nextLine();
                            comandoAEnviar = "PASS " + password;
                            break;
                        case "3":
                            comandoAEnviar = "EXIT";
                            break;
                        default:
                            // Permite escribir comandos manuales
                            System.out.println("Comando desconocido o no permitido, porfavor inicie sesión");
                            continue;
                    }
                } else if (sesionIniciada == true) {
                    System.out.println("\n--- CLIENTE CLUB DEPORTIVO ---");
                    System.out.println("1. Crear Club -> ADDCLUB <Id> <nombreClub>");
                    System.out.println("2. Listar Clubes -> LISTCLUBES");
                    System.out.println("3. Buscar Club -> GETCLUB <IdClub>");
                    System.out.println("4. Actualizar Club -> UPDATECLUB <IdClub> <nombreClub>");
                    System.out.println("5. Eliminar Club -> REMOVECLUB <IdClub>");
                    System.out.println("6. Contar Clubes -> COUNTCLUBES");
                    System.out.println("7. Salir -> EXIT");

                    opcionUsuario = sc.nextLine();

                    switch (opcionUsuario) {
                        case "1":
                            comandoAEnviar = "ADDCLUB";
                            break;
                        case "2":
                            comandoAEnviar = "LISTCLUBES";
                            break;
                        case "3":
                            System.out.println("Escribe el ID del club a obtener: ");
                            String idClub = sc.nextLine();
                            comandoAEnviar = "GETCLUB " + idClub;
                            break;
                        case "4":
                            System.out.println("Escribe el ID del club a actualizar: ");
                            idClub = sc.nextLine();
                            comandoAEnviar = "UPDATECLUB " + idClub;
                            break;
                        case "5":
                            System.out.println("Escribe el ID del club a eliminar: ");
                            idClub = sc.nextLine();
                            comandoAEnviar = "REMOVECLUB " + idClub;
                            break;
                        case "6":
                            comandoAEnviar = "COUNTCLUBES";
                            break;
                        case "7":
                            comandoAEnviar = "EXIT";
                            break;
                        default:
                            // Comandos desconocidos o mal escritos
                            comandoAEnviar = opcionUsuario;
                            continue;
                    }
                }

                // Concatenamos el ID del mensaje antes del comando
                String mensajeProtocolo = idMensaje + " " + comandoAEnviar;

                System.out.println("[Cliente envía]: " + mensajeProtocolo);
                pw.println(mensajeProtocolo);

                // Leemos la respuesta del servidor
                // Leemos la respuesta del servidor
                String respuesta = br.readLine();
                System.out.println("[Servidor responde]: " + respuesta);

                if (comandoAEnviar.startsWith("PASS") && respuesta.contains(" 200 ")) {
                    sesionIniciada = true;
                    System.out.println("¡Login correcto! Accediendo al sistema...");
                }
                // En caso de que el servidor responda con PREOK
                if (respuesta != null && respuesta.startsWith("PREOK")) {

                    if (comandoAEnviar.startsWith("ADDCLUB")) {
                        System.out.println("Escribe el id:");
                        String idClub = sc.nextLine();
                        System.out.println("Escribe el nombre:");
                        String nombreClub = sc.nextLine();
                        modelos.Club nuevoClub = new modelos.Club(idClub, nombreClub);
                        enviarClubAServidor(respuesta, nuevoClub);

                    } else if (comandoAEnviar.startsWith("UPDATECLUB")) {
                        String[] partes = comandoAEnviar.split(" ");
                        String idClub = partes[1];
                        System.out.println("Escribe el nuevo nombre:");
                        String nuevoNombre = sc.nextLine();
                        Club clubActualizado = new Club(idClub, nuevoNombre);
                        enviarClubAServidor(respuesta, clubActualizado);
                    } else { // Llama al mét0d0 que se encarga de la conexión
                        recibirDatosDeServidor(respuesta);
                    }

                    // Envia una confirmación final
                    String respuestaFinal = br.readLine();
                    System.out.println("[Servidor final]: " + respuestaFinal);
                }

                // Incrementamos el ID para el siguiente mensaje
                idMensaje++;

            } while (!opcionUsuario.equals("7") && !opcionUsuario.equalsIgnoreCase("EXIT"));

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
                System.out.println("He recibido la lista de clubes:");
                java.util.List lista = (java.util.List) recibido;
                for (Object o : lista) {
                    System.out.println("       - " + o);
                }
            } else if (recibido instanceof modelos.Club) {
                System.out.println("He recibido un Club: " + recibido);
            }

            ois.close();
            socketDatos.close();
            System.out.println("Datos recibidos con éxito");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

    }

    private static void enviarClubAServidor(String respuestaPreok, Club clubAEnviar) {
        try {
            String[] partes = respuestaPreok.split(" ");
            String ip = partes[3];
            int puerto = Integer.parseInt(partes[4]);
            System.out.println("Conectando a " + ip + ":" + puerto + " para subir datos...");

            Socket socket = new Socket(ip, puerto);
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(clubAEnviar);
            oos.flush();
            oos.close();
            socket.close();
            System.out.println("Club enviado correctamente");
        } catch (IOException e) {
            System.out.println("Error al enviar: " + e.getMessage());

        }
    }
}