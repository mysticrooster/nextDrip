package com.eveningoutpost.dexdrip;

import android.app.Application;

import org.robolectric.RuntimeEnvironment;

/**
 * Created by jamorham on 01/10/2017.
 * <p>
 * Skeleton Application class used by Robolectric tests.
 */
public class TestingApplication extends Application {
    @Override
    public void onCreate() {
        xdrip.checkAppContext(RuntimeEnvironment.application);
        super.onCreate();
    }
}
