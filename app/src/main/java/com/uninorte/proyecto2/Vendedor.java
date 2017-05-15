package com.uninorte.proyecto2;

/**
 * Created by LauryV on 14/05/2017.
 */

public class Vendedor {
    String key;
    String email;


    public Vendedor(String key, String email) {
        this.key = key;
        this.email = email;
    }

    public Vendedor() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
