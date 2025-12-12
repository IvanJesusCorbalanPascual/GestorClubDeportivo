package servidor;

import modelos.Club;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
                    case "LISTCLUBES":
                        procesarListClubes(numeroEnvio);
                        break;
                    case "GETCLUB":
                        procesarGetClub(numeroEnvio, partes);
                        break;
                    case "EXIT":
                        pw.println("OK " + numeroEnvio + " 200 Bye");
                        socket.close();
                        return;
                    default:
                        if (!loginCorrecto) {
                            pw.println("FAILED " + numeroEnvio + " 403 Necesitas hacer login primero");
                        } else {
                            pw.println("FAILED " + numeroEnvio + " 404 Comando desconocido");
                        }

                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
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
            pw.println("OK " + num + "200 Welcome " + nombreUsuario);
        } else {
            loginCorrecto = false;
            usuarioCorrecto = false;
            pw.println("FAILED " + num + " 403 Contraseña incorrecta");
        }
    }

    private void procesarListClubes(String num) {
        // Si no se ha logueado, no puede ver nada
        if (!loginCorrecto) {
            pw.println("FAILED " + num + " 403 Es necesario hacer login primero");
            return;
        }
        // Le pasa la lista de clubes de server
        enviarObjeto(num, Server.clubes);
    }

    private void procesarGetClub(String num, String[] partes) {
        if (!loginCorrecto) {
            pw.println("FAILED " + num + " 403 Es necesario hacer login primero");
            return;
        }
        if (partes.length < 3) {
            pw.println("FAILED " + num + " 404 Falta el ID del club (Ejemplo: GETCLUB 1)");
            return;
        }

        // El ID que escribio el usuario
        String idBuscado = partes[2];
        Club clubEncontrado = null;

        // Synchronized evita errores
        synchronized (Server.clubes) {
            for (Club c : Server.clubes) {
                if (c.getId().equals(idBuscado)) {
                    clubEncontrado = c;
                    // Si lo encuentra, deja de buscar
                    break;
                }
            }
        }
        // Si existe lo envia, en caso contrario da error
        if (clubEncontrado != null) {
            enviarObjeto(num, clubEncontrado);
        } else {
            pw.println("FAILED" + num + "404 No se ha encontrado el club con ese ID");
        }
    }

    private void enviarObjeto(String num, Object objetoAEnviar) {
        try {
            // Al poner 0, busca automáticamente un puerto libre
            java.net.ServerSocket servidorDatos = new java.net.ServerSocket(0);
            // Guarda el puerto que ha tocado
            int puertoDatos = servidorDatos.getLocalPort();

            // Avisa al cliente por el canal de comandos
            String miIp = socket.getInetAddress().getHostAddress();
            pw.println("PREOK " + num + " 200 " + miIp + " " + puertoDatos);

            // Espera a que el cliente se conecte
            Socket socketDatos = servidorDatos.accept();

            // Convierte el objeto en bytes para su envio
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(socketDatos.getOutputStream());

            // Envia el objeto
            oos.writeObject(objetoAEnviar);
            oos.flush();

            // Cierra lo que hemos abierto
            oos.close();
            socketDatos.close();
            servidorDatos.close();

            pw.println("OK " + num + " 200 Transferencia completada");

        } catch (IOException e) {
            e.printStackTrace();
            pw.println("FAILED " + num + " 500 Error interno al enviar los datos");
        }
    }

}
