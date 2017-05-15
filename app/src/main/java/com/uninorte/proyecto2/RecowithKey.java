package com.uninorte.proyecto2;

/**
 * Created by daniel on 15/05/17.
 */

public class RecowithKey {
    private String key;
    private Recorrido recorrido;
    //


    public RecowithKey() {
    }

    public RecowithKey(String key, Recorrido recorrido) {
        this.key = key;
        this.recorrido = recorrido;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Recorrido getRecorrido() {
        return recorrido;
    }

    public void setRecorrido(Recorrido recorrido) {
        this.recorrido = recorrido;
    }
}
