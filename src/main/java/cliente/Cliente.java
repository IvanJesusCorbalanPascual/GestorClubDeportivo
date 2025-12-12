package cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {

        String host = "localhost";
        int puerto = 5000;

        System.out.println("Iniciando cliente en " + host + ":" + puerto);

        try (Socket socket = new Socket(host, puerto);
             PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner sc = new Scanner(System.in)) {

            // Variable para llevar la cuenta de los mensajes (Protocolo: <number> <comando>)
            int idMensaje = 1;
            String opcionUsuario;

            do {
                System.out.println("\n--- CLIENTE CLUB DEPORTIVO ---");
                System.out.println("Escribe el comando completo (Ej: USER admin)");
                System.out.println("O elige una opción rápida:");
                System.out.println("1. Enviar USER admin");
                System.out.println("2. Enviar PASS admin");
                System.out.println("3. Salir (EXIT)");
                System.out.print("➡ ");

                opcionUsuario = sc.nextLine();

                String comandoAEnviar = "";

                // Lógica para facilitar las pruebas
                switch (opcionUsuario) {
                    case "1":
                        comandoAEnviar = "USER admin";
                        break;
                    case "2":
                        comandoAEnviar = "PASS admin";
                        break;
                    case "3":
                        comandoAEnviar = "EXIT";
                        break;

                    default:
                        // Permite escribir comandos manuales
                        comandoAEnviar = opcionUsuario;
                        break;
                }

                // Concatenamos el ID del mensaje antes del comando
                String mensajeProtocolo = idMensaje + " " + comandoAEnviar;

                System.out.println("[Cliente envía]: " + mensajeProtocolo);
                pw.println(mensajeProtocolo);

                // Leemos la respuesta del servidor
                String respuesta = br.readLine();
                System.out.println("[Servidor responde]: " + respuesta);

                // Incrementamos el ID para el siguiente mensaje
                idMensaje++;

            } while (!opcionUsuario.equals("3") && !opcionUsuario.equalsIgnoreCase("EXIT"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}