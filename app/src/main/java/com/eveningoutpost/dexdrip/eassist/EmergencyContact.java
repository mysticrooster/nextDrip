package com.eveningoutpost.dexdrip.eassist;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// jamorham

// Manage lightweight database of names and numbers for EmergencyAssist message feature

public class EmergencyContact {

    private static final String PREF_NAME = "Emergency-Contact-List";

    @Expose
    public String name;
    @Expose
    public String number;

    public EmergencyContact(String name, String number) {
        this.name = name;
        this.number = number;
    }


    public static List<EmergencyContact> load() {
        final String json = Pref.getString(PREF_NAME, "");
        final Type queueType = new TypeToken<ArrayList<EmergencyContact>>() {
        }.getType();
        List<EmergencyContact> result = JoH.defaultGsonInstance().fromJson(json, queueType);
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    public static void save(List<EmergencyContact> contacts) {
        Pref.setString(PREF_NAME, JoH.defaultGsonInstance().toJson(contacts));
    }
}
