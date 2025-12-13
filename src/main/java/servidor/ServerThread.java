package servidor;

import modelos.Club;
import modelos.Jugador;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
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
                    case "ADDCLUB":
                        if (loginCorrecto) {
                            procesarAddClub(numeroEnvio);
                        } else {
                            pw.println("FAILED " + numeroEnvio + " 403 Necesitas iniciar sesión primero");
                        }
                        break;
                    case "LISTCLUBES":
                        procesarListClubes(numeroEnvio);
                        break;
                    case "GETCLUB":
                        procesarGetClub(numeroEnvio, partes);
                        break;
                    case "UPDATECLUB":
                        procesarUpdateClub(numeroEnvio, partes);
                        break;
                    case "REMOVECLUB":
                        break;
//                    case "ADDJUGADOR":
//                        if (loginCorrecto) procesarAddJugador(numeroEnvio);
//                        else pw.println("FAILED "+numeroEnvio+" 403 Inicio de Sesión requerido");
//                        break;
                    case "ADDJUGADOR2CLUB":
                        if (loginCorrecto) procesarAddJugadorToClub(numeroEnvio, partes);
                        else pw.println("FAILED " + numeroEnvio + " 403 Login requerido");
                        break;
                    case "LISTJUGADORES":
                        procesarListJugadores(numeroEnvio);
                        break;
                    case "GETJUGADOR":
                        procesarGetJugador(numeroEnvio, partes);
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
            e.printStackTrace();
        }
    }

    // --- METODOS DE COMANDOS ---

    // Metodo que válida si el usuario puede acceder al sistema o no en base a su nombre
    public void procesarUser(String numeroEnvio, String[] partes) {
        if (partes.length < 3) {
            pw.println("Error, formato incorrecto");
        }
        String user = partes[2];
        if (user.equalsIgnoreCase("admin")) {
            usuarioCorrecto = true;
            nombreUsuario = "admin"; // Si el nombre es "admin" entonces el usuario es válido
            pw.println("OK " + numeroEnvio + " 201 Envie contraseña");
        } else {
            usuarioCorrecto = false;
            pw.println("FAILED " + numeroEnvio + " 401 Usuario incorrecto");
        }
    }

    // Metodo que válida si la contraseña "PASS" es correcta
    private void procesarPass(String numeroEnvio, String[] partes) {
        if (!usuarioCorrecto) {
            pw.println("FAILED " + numeroEnvio + " 402 Primero envíe <USER>");
            return;
        }
        if (partes.length < 3) {
            pw.println("FAILED " + numeroEnvio + " 401 Usuario incorrecto");
            return;
        }
        String pass = partes[2]; // Estamos cogiendo como valor de contraseña la tercera palabra del comando
        if (pass.equalsIgnoreCase("admin")) { // Si la contraseña es "admin" entonces tod0 guay
            loginCorrecto = true;
            pw.println("OK " + numeroEnvio + " 200 Bienvenido " + nombreUsuario);
        } else {
            loginCorrecto = false;
            usuarioCorrecto = false;
            pw.println("FAILED " + numeroEnvio + " 403 Contraseña incorrecta");
        }
    }

    private void procesarAddClub(String numeroEnvio) throws IOException {
        // Abriendo un puerto para DATOS en 0 para que el sistema nos dé uno libre
        try (ServerSocket dataSocket = new ServerSocket(0)) {
            int puertoDatos = dataSocket.getLocalPort();
            pw.println("PREOK " + numeroEnvio + " 200 localhost " + puertoDatos);

            // Espera a que el cliente se conecte para enviar el objeto Club
            try (Socket clienteDatos = dataSocket.accept();) {
                ObjectInputStream ois = new ObjectInputStream(clienteDatos.getInputStream());
                Club nuevoClub = (Club) ois.readObject(); // Leyendo el objeto que el cliente ha creado y pasado por el Socket

                synchronized (Server.clubes) {
                    Server.clubes.add(nuevoClub);
                }
                pw.println("OK " + numeroEnvio + " 201 Club creado con exito " + nuevoClub.getNombre());
            } catch (Exception e) {
                e.printStackTrace();
                pw.println("FAILED " + numeroEnvio + " 500 Usuario incorrecto");
            }
        }
    }

    private void procesarListClubes(String numeroEnvio) {
        // Si no se ha logueado, no puede ver nada
        if (!loginCorrecto) {
            pw.println("FAILED " + numeroEnvio + " 403 Es necesario hacer login primero");
            return;
        }
        // Le pasa la lista de clubes de server
        enviarObjeto(numeroEnvio, Server.clubes);
    }

    private void procesarGetClub(String numeroEnvio, String[] partes) {
        if (!loginCorrecto) {
            pw.println("FAILED " + numeroEnvio + " 403 Es necesario hacer login primero");
            return;
        }
        if (partes.length < 3) {
            pw.println("FAILED " + numeroEnvio + " 404 Falta el ID del club (Ejemplo: GETCLUB 1)");
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
            enviarObjeto(numeroEnvio, clubEncontrado);
        } else {
            pw.println("FAILED" + numeroEnvio + "No se ha encontrado el club con ese ID");
        }
    }

    private void procesarUpdateClub(String numeroEnvio, String[] partes) {
        if (partes.length < 3) {
            pw.println("FAILED " + numeroEnvio + " 404 Falta el ID del club (UPDATECLUB <id>)");
            return;
        }
        String idBuscado = partes[2];
        Club clubAActualizar = null;

        // Bucle que busca dentro del ArrayList "clubes" uno con el id pasado, si lo encuentra lo actualiza
        synchronized (Server.clubes) {
            for (Club c : Server.clubes) {
                if (c.getId().equals(idBuscado)) {
                    clubAActualizar = c;
                    break;
                }
            }
        }

        // Si el club no se ha encontrado, esta variable seguirá siendo null y por lo tanto se cancela la operacion
        if (clubAActualizar == null) {
            pw.println("FAILED " + numeroEnvio + " 404 No existe un club con ese ID");
            return;
        }

        try (ServerSocket dataSocket = new ServerSocket(0)) {
            int puertoDatos = dataSocket.getLocalPort();
            pw.println("PREOK " + numeroEnvio + " 200 localhost " + puertoDatos);

            try (Socket clienteDatos = dataSocket.accept();) {
                ObjectInputStream ois = new ObjectInputStream(clienteDatos.getInputStream());
                // Leemos los datos del club una vez ya ha sido actualizado
                Club clubActualizado = (Club) ois.readObject();

                synchronized (Server.clubes) {
                    clubAActualizar.setNombre(clubActualizado.getNombre());
                }
                pw.println("OK " + numeroEnvio + " 200 Club Actualizado con exito " + clubAActualizar.getNombre());
            }

        } catch (Exception e) {
            e.printStackTrace();
            pw.println("FAILED " + numeroEnvio + " 500 Error al actualizar");
        }
    }

//    private void procesarAddJugador(String numeroEnvio) throws IOException {
//        try (ServerSocket dataSocket = new ServerSocket(0)){
//            int puertoDatos = dataSocket.getLocalPort();
//            pw.println("PREOK " + numeroEnvio + " 200 localhost " + puertoDatos);
//
//            try {
//                Socket clienteDatos = dataSocket.accept();
//                 ObjectInputStream ois = new ObjectInputStream(clienteDatos.getInputStream())) {
//                 modelos.Jugador nuevoJugador = (modelos.Jugador) ois.readObject();
//
//                 synchronized (Server.jugadores) {
//                Server.jugadores.add(nuevoJugador);
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            pw.println("FAILED " + numeroEnvio + " 500 Error al actualizar");
//        }
//    }

    private void procesarAddJugadorToClub(String numeroEnvio, String[] partes) {
        if (partes.length < 4) {
            pw.println("FAILED " + numeroEnvio + " 400 Faltan argumentos (ID_JUGADOR ID_CLUB)");
            return;
        }
        String idJugador = partes[2];
        String idClub = partes[3];

        Jugador jugadorEncontrado = null;
        Club clubEncontrado = null;

        // Buscamos el Jugador por su id
        synchronized (Server.jugadores) {
            for (Jugador j : Server.jugadores) {
                if (j.getId().equals(idJugador)) {
                    jugadorEncontrado = j;
                    break;
                }
            }
        }
        // Buscamos el Club por su id
        synchronized (Server.clubes) {
            for (Club c : Server.clubes) {
                if (c.getId().equals(idClub)) {
                    clubEncontrado = c;
                    break;
                }
            }
        }

        // Si se encontró tanto al jugador como al club, se llama al metodo de la clase Club que inserta dentro de este Club
        // un jugador por su id, y el objeto del jugador encontrado
        if (jugadorEncontrado != null && clubEncontrado != null) {
            clubEncontrado.addJugador(jugadorEncontrado.getId(), jugadorEncontrado);
            pw.println("OK " + numeroEnvio + " 200 Jugador " + idJugador + " añadido al club " + idClub);

        } else {
            pw.println("FAILED " + numeroEnvio + " 404 Jugador o Club no encontrado");
        }
    }

    private void procesarListJugadores(String numeroEnvio) {
        if (!loginCorrecto) {
            pw.println("FAILED " + numeroEnvio + " 403 Es necesario iniciar sesión antes");
            return;
        }
        // Envía la lista de jugadores global
        enviarObjeto(numeroEnvio, Server.jugadores);
    }

    private void procesarGetJugador(String numeroEnvio, String[] partes) {
        if (!loginCorrecto) {
            pw.println("FAILED " + numeroEnvio + " 403 Es necesario iniciar sesión antes");
            return;
        }
        if (partes.length < 3) {
            pw.println("FAILED " + numeroEnvio + " 404 Falta el ID del jugador (GETJUGADOR <id>)");
            return;
        }

        String idJugador = partes[2];
        Jugador jugadorEncontrado = null;

        synchronized (Server.jugadores) {
            for (Jugador j : Server.jugadores) {
                if (j.getId().equals(idJugador)) {
                    jugadorEncontrado = j;
                    break;
                }
            }
        }

        if (jugadorEncontrado != null) {
            enviarObjeto(numeroEnvio, jugadorEncontrado);
        } else {
            pw.println("FAILED " + numeroEnvio + " 404 Jugador no encontrado");
        }
    }



    private void enviarObjeto(String numeroEnvio, Object objetoAEnviar) {
        try {
            // Al poner 0, busca automáticamente un puerto libre
            java.net.ServerSocket servidorDatos = new java.net.ServerSocket(0);
            // Guarda el puerto que ha tocado
            int puertoDatos = servidorDatos.getLocalPort();

            // Avisa al cliente por el canal de comandos
            String miIp = socket.getInetAddress().getHostAddress();
            pw.println("PREOK " + numeroEnvio + " 200 " + miIp + " " + puertoDatos);

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

            pw.println("OK " + numeroEnvio + " 200 Transferencia completada");

        } catch (IOException e) {
            e.printStackTrace();
            pw.println("FAILED " + numeroEnvio + " 500 Error interno al enviar los datos");
        }
    }

}
