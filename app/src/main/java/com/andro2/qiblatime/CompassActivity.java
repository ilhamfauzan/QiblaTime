package com.andro2.qiblatime;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor magneticSensor, accelerometerSensor;
    private float[] gravity;
    private float[] geomagnetic;

    private TextView compassDirection, locationInfo;

    // Lokasi pengguna (akan didapat dari Intent)
    private double userLatitude = -6.200000;  // default Jakarta
    private double userLongitude = 106.816666; // default Jakarta

    // Lokasi Ka'bah
    private final double qiblaLatitude = 21.422487;
    private final double qiblaLongitude = 39.826206;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        compassDirection = findViewById(R.id.compassDirection);
        locationInfo = findViewById(R.id.locationInfo); // Tambahkan TextView untuk info lokasi

        // Dapatkan lokasi dari Intent
        getLocationFromIntent();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void getLocationFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            double intentLatitude = intent.getDoubleExtra("latitude", 0.0);
            double intentLongitude = intent.getDoubleExtra("longitude", 0.0);
            String city = intent.getStringExtra("city");

            if (intentLatitude != 0.0 && intentLongitude != 0.0) {
                userLatitude = intentLatitude;
                userLongitude = intentLongitude;

                if (city != null && !city.isEmpty()) {
                    locationInfo.setText("Lokasi: " + city);
                } else {
                    locationInfo.setText("Lokasi: " + String.format("%.6f, %.6f", userLatitude, userLongitude));
                }

                Toast.makeText(this, "Menggunakan lokasi: " + city, Toast.LENGTH_SHORT).show();
            } else {
                locationInfo.setText("Lokasi: Jakarta (Default)");
                Toast.makeText(this, "Menggunakan lokasi default Jakarta", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]); // arah utara
                if (azimuth < 0) azimuth += 360;

                double qiblaAzimuth = calculateQiblaAzimuth(userLatitude, userLongitude, qiblaLatitude, qiblaLongitude);
                float kiblatDirection = (float) (qiblaAzimuth - azimuth);
                if (kiblatDirection < 0) kiblatDirection += 360;

                compassDirection.setText("Arah Kiblat: " + Math.round(kiblatDirection) + "°");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak digunakan
    }

    private double calculateQiblaAzimuth(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double azimuth = Math.atan2(y, x);
        return (Math.toDegrees(azimuth) + 360) % 360;
    }
}