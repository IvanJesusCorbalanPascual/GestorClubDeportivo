package modelos;

import java.io.Serializable;

public class Jugador
        implements Serializable
{

    public Jugador()
    {
    }

    public Jugador(String id, String nombre, String apellidos, int goles)
    {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.goles = goles;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getNombre()
    {
        return nombre;
    }

    public void setNombre(String nombre)
    {
        this.nombre = nombre;
    }

    public String getApellidos()
    {
        return apellidos;
    }

    public void setApellidos(String apellidos)
    {
        this.apellidos = apellidos;
    }

    public int getGoles()
    {
        return goles;
    }

    public void setGoles(int goles)
    {
        this.goles = goles;
    }

    public void addGol()
    {
        goles++;
    }

    public void removeGol()
    {
        if(goles > 0)
            goles--;
    }

    private static final long serialVersionUID = 1L;
    private String id;
    private String nombre;
    private String apellidos;
    private int goles;
}
