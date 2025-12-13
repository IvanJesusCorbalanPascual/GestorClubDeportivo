package modelos;

import java.io.Serializable;
import java.util.*;

// Referenced classes of package edu.ucam.domain:
//            Jugador

public class Club
        implements Serializable {

    public Club() {
        jugadores = new Hashtable();
    }

    public Club(String id, String nombre) {
        jugadores = new Hashtable();
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void addJugador(String id, Jugador jugador) {
        jugadores.put(id, jugador);
    }

    public void updateJugador(String id, Jugador jugador) {
        if (jugadores.get(id) != null)
            jugadores.replace(id, jugador);
    }

    public boolean removeJugador(String id) {
        if (jugadores.get(id) != null) {
            jugadores.remove(id);
            return true;
        } else {
            return false;
        }
    }

    public int totalJugadores() {
        return jugadores.size();
    }

    public ArrayList getJugadores() {
        ArrayList jugadores = new ArrayList();
        Jugador jugador;
        for (Iterator iterator = this.jugadores.values().iterator(); iterator.hasNext(); jugadores.add(jugador))
            jugador = (Jugador) iterator.next();

        return jugadores;
    }

    private static final long serialVersionUID = 1L;
    private String id;
    private String nombre;
    private Hashtable jugadores;

    @Override
    public String toString() { // toString mejorado visualmente
        return "[ID: " + id + "] " + nombre + " (" + (jugadores != null ? jugadores.size() : 0) + " jugadores)";
    }
}


