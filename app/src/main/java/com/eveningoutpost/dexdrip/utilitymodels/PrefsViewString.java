package com.eveningoutpost.dexdrip.utilitymodels;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableMap;

import com.eveningoutpost.dexdrip.adapters.ObservableArrayMapNoNotify;
import com.google.common.collect.ForwardingMap;

import java.util.Map;

/**
 * Created by jamorham on 04/07/2018.
 *
 * Observable map with transparent persistence
 */

public class PrefsViewString extends ForwardingMap<String, String> implements ObservableMap<String, String> {

    private final ObservableArrayMapNoNotify<String, String> delegate = new ObservableArrayMapNoNotify<>();

    @Override
    protected Map<String, String> delegate() {
        return delegate;
    }

    @Override
    public void addOnMapChangedCallback(OnMapChangedCallback<? extends ObservableMap<String, String>, String, String> listener) {
        delegate.addOnMapChangedCallback(listener);
    }

    @Override
    public void removeOnMapChangedCallback(OnMapChangedCallback<? extends ObservableMap<String, String>, String, String> listener) {
        delegate.removeOnMapChangedCallback(listener);
    }

    public String getString(String name) {
        return Pref.getString(name, "");
    }

    public void setString(String name, String value) {
        Pref.setString(name, value);
        delegate.put(name, value);
    }

    @NonNull
    @Override
    public String get(Object key) {
        String value = delegate.get(key);
        if (value == null) {
            value = getString((String) key);
            delegate.putNoNotify((String) key, value);
        }
        return value;
    }

    @Override
    public String put(String key, String value) {
        String current = delegate.get(key);
        if (current == null || !current.equals(value)) {
            setString(key, value);
        }
        return value;
    }

}
