package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import modelos.Club;

public class ServerThread extends Thread {
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;

    // Estado de la sesion
    private boolean usuarioCorrecto = false;
    private boolean loginCorrecto = false;
    private String nombreUsuario = "";

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream(), true);

            String line = null;
            while ((line = br.readLine()) != null) {
                String[] partes = line.trim().split(" ");
                if (partes.length < 2) {
                    pw.println("Error, formato incorrecto");
                    continue;
                }

                String numeroEnvio = partes[0]; // Número del mensaje
                String comando = partes[1].toUpperCase(); // El comando

                switch (comando) {
                    case "USER":
                        procesarUser(numeroEnvio, partes);
                        break;
                    case "PASS":
                        procesarPass(numeroEnvio, partes);
                        break;
                    case "EXIT":
                        pw.println("OK " + numeroEnvio + " 200 Bye");
                        socket.close();
                        return;
                    case "ADDCLUB":
                        if (loginCorrecto) {
                            procesarAddClub(numeroEnvio);
                        } else {
                            pw.println("FAILED " + numeroEnvio + " 403 Necesitas iniciar sesión primero");
                        }
                    default:
                        if (!loginCorrecto) {
                            pw.println("FAILED " + numeroEnvio + " 403 Necesitas hacer login primero");
                        } else {
                            pw.println("FAILED " + numeroEnvio + " 404 Comando desconocido");
                        }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- METODOS DE COMANDOS ---

    // Metodo que válida si el usuario puede acceder al sistema o no en base a su nombre
    public void procesarUser(String num, String[] partes) {
        if (partes.length < 3) {
            pw.println("Error, formato incorrecto");
        }
        String user = partes[2];
        if (user.equalsIgnoreCase("admin")) {
            usuarioCorrecto = true;
            nombreUsuario = "admin"; // Si el nombre es "admin" entonces el usuario es válido
            pw.println("OK " + num + " 201 Envie contraseña");
        } else {
            usuarioCorrecto = false;
            pw.println("FAILED " + num + " 401 Usuario incorrecto");
        }
    }

    // Metodo que válida si la contraseña "PASS" es correcta
    private void procesarPass(String num, String[] partes) {
        if (!usuarioCorrecto) {
            pw.println("FAILED " + num + " 402 Primero envíe <USER>");
            return;
        }
        if (partes.length < 3) {
            pw.println("FAILED " + num + " 401 Usuario incorrecto");
            return;
        }
        String pass = partes[2]; // Estamos cogiendo como valor de contraseña la tercera palabra del comando
        if (pass.equalsIgnoreCase("admin")) { // Si la contraseña es "admin" entonces tod0 guay
            loginCorrecto = true;
            pw.println("OK " + num + " 200 Bienvenido " + nombreUsuario);
        } else {
            loginCorrecto = false;
            usuarioCorrecto = false;
            pw.println("FAILED " + num + " 403 Contraseña incorrecta");
        }
    }

    private void procesarAddClub(String num) throws IOException {
        // Abriendo un puerto para DATOS en 0 para que el sistema nos dé uno libre
        try (ServerSocket dataSocket = new ServerSocket(0)) {
            int puertoDatos = dataSocket.getLocalPort();
            pw.println("PREOK " + num + " 200 localhost " + puertoDatos);

            // Espera a que el cliente se conecte para enviar el objeto Club
            try (Socket clienteDatos = dataSocket.accept();) {
                ObjectInputStream ois = new ObjectInputStream(clienteDatos.getInputStream());
                Club nuevoClub = (Club) ois.readObject(); // Leyendo el objeto que el cliente ha creado y pasado por el Socket

                synchronized (Server.clubes) {
                    Server.clubes.add(nuevoClub);
                }
                pw.println("OK " + num + " 201 Club creado con exito " + nuevoClub.getNombre());
            } catch (Exception e) {
                e.printStackTrace();
                pw.println("FAILED " + num + " 500 Usuario incorrecto");
            }
        }
    }
}
