package com.utopios.taskforge.service;

// A08 - simple POJO lie par Jackson vers une classe fixe : aucune desserialisation
// polymorphe, aucun gadget possible (contrairement a ObjectInputStream).
public class Preferences {
    private String theme = "clair";

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
