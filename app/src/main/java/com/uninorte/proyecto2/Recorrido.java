package com.uninorte.proyecto2;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by daniel on 7/05/17.
 */
@IgnoreExtraProperties
public class Recorrido {

    private String User_id;
    private String TimeI;

    public Recorrido() {

    }

    public Recorrido( String user_id, String timeI) {
        User_id = user_id;
        TimeI = timeI;
    }

    public String getTimeI() {
        return TimeI;
    }

    public void setTimeI(String timeI) {
        TimeI = timeI;
    }


    public String getUser_id() {
        return User_id;
    }

    public void setUser_id(String user_id) {
        User_id = user_id;
    }
}
