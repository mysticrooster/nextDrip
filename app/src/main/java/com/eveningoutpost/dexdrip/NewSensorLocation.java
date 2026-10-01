package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.ui.secondary.NewSensorLocationScreen;

/**
 * Sensor placement selection (Track V pass 5, now Compose).
 */
public class NewSensorLocation extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NewSensorLocationScreen.installNewSensorLocation(this);
    }

    public void saveLocation(String location) {
        Toast.makeText(getApplicationContext(), "Sensor locaton is " + location, Toast.LENGTH_LONG).show();
        Log.d("NEW SENSOR", "Sensor location is " + location);
        Sensor.updateSensorLocation(location);
        goHome();
    }

    public void cancelLocation() {
        goHome();
    }

    private void goHome() {
        startActivity(new Intent(getApplicationContext(), Home.class));
        finish();
    }
}
