package com.andro2.qiblatime;

import android.content.Intent;
import android.view.View;
import android.widget.Button;

import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.location.LocationListener;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.content.pm.PackageManager; // Tambahkan impor untuk PackageManager




public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Button openCompassButton;

    private TextView fajr, dhuhr, asr, maghrib, isha, cityName;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fajr = findViewById(R.id.fajr);
        dhuhr = findViewById(R.id.dhuhr);
        asr = findViewById(R.id.asr);
        maghrib = findViewById(R.id.maghrib);
        isha = findViewById(R.id.isha);
        cityName = findViewById(R.id.cityName);
        openCompassButton = findViewById(R.id.openCompassButton);

        // Aksi tombol ke CompassActivity
        openCompassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CompassActivity.class);
                startActivity(intent);
            }
        });



        // Cek izin lokasi
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Jika izin tidak diberikan, maka minta izin
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Jika izin sudah diberikan, langsung akses lokasi
            getLocation();
        }

    }

    private void getLocation() {
        // Mendapatkan lokasi menggunakan LocationManager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                // Mendapatkan nama kota menggunakan Geocoder
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && addresses.size() > 0) {
                        String city = addresses.get(0).getLocality(); // Nama kota
                        cityName.setText("City: " + city);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Ambil jadwal salat berdasarkan lokasi
                fetchPrayerTimes(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Request lokasi dari GPS provider
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    private void fetchPrayerTimes(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.aladhan.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PrayerApiService apiService = retrofit.create(PrayerApiService.class);
        Call<PrayerTimesResponse> call = apiService.getPrayerTimes(latitude, longitude, 2); // Method 2 is Umm al-Qura

        call.enqueue(new Callback<PrayerTimesResponse>() {
            @Override
            public void onResponse(Call<PrayerTimesResponse> call, Response<PrayerTimesResponse> response) {
                if (response.isSuccessful()) {
                    PrayerTimesResponse times = response.body();

                    fajr.setText("Fajr: " + times.getData().getTimings().getFajr());
                    dhuhr.setText("Dhuhr: " + times.getData().getTimings().getDhuhr());
                    asr.setText("Asr: " + times.getData().getTimings().getAsr());
                    maghrib.setText("Maghrib: " + times.getData().getTimings().getMaghrib());
                    isha.setText("Isha: " + times.getData().getTimings().getIsha());
                }
            }


            @Override
            public void onFailure(Call<PrayerTimesResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);  // Tambahkan pemanggilan ke super

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Jika izin diberikan, ambil lokasi
                getLocation();
            } else {
                // Jika izin ditolak, beri pesan atau hentikan aplikasi
                cityName.setText("Permission denied. Cannot access location.");
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }
}
