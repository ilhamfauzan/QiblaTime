package com.andro2.qiblatime;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.cardview.widget.CardView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private CardView btnJadwal;
    private CardView btnKiblat;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private String currentCity = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi views
        initViews();

        // Setup click listeners
        setupClickListeners();

        // Request lokasi saat aplikasi dimulai
        requestLocationPermission();

        // Request notifikasi saat aplikasi dimulai
        requestNotificationPermission();

        // Tampilkan pesan loading
        Toast.makeText(this, "Mendapatkan lokasi...", Toast.LENGTH_SHORT).show();
    }

    private void initViews() {
        btnJadwal = findViewById(R.id.btnJadwal);
        btnKiblat = findViewById(R.id.btnKiblat);
    }

    private void setupClickListeners() {
        // Tombol Jadwal Shalat - mengarah ke PrayerTimeActivity dengan data lokasi
        btnJadwal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                    Intent intent = new Intent(MainActivity.this, PrayerTimeActivity.class);
                    intent.putExtra("latitude", currentLatitude);
                    intent.putExtra("longitude", currentLongitude);
                    intent.putExtra("city", currentCity);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Lokasi belum tersedia, mohon tunggu sebentar...", Toast.LENGTH_LONG).show();
                    // Coba get lokasi lagi jika belum ada
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        getLocation();
                    }
                }
            }
        });

        // Tombol Kiblat - mengarah ke QiblaActivity
        btnKiblat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                    Intent intent = new Intent(MainActivity.this, CompassActivity.class);
                    intent.putExtra("latitude", currentLatitude);
                    intent.putExtra("longitude", currentLongitude);
                    intent.putExtra("city", currentCity);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Lokasi belum tersedia, mohon tunggu sebentar...", Toast.LENGTH_LONG).show();
                    // Coba get lokasi lagi jika belum ada
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        getLocation();
                    }
                }
            }
        });
    }

    private void requestLocationPermission() {
        // Cek izin lokasi
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Jika izin tidak diberikan, minta izin
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Jika izin sudah diberikan, langsung akses lokasi
            getLocation();
        }
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();

                // Mendapatkan nama kota menggunakan Geocoder
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
                    if (addresses != null && addresses.size() > 0) {
                        currentCity = addresses.get(0).getLocality();
                        if (currentCity == null) {
                            currentCity = addresses.get(0).getSubAdminArea();
                        }
                        if (currentCity == null) {
                            currentCity = "Tidak diketahui";
                        }

                        Toast.makeText(MainActivity.this, "Lokasi berhasil ditemukan: " + currentCity, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    currentCity = "Tidak diketahui";
                    Toast.makeText(MainActivity.this, "Tidak dapat menentukan nama kota", Toast.LENGTH_SHORT).show();
                }

                // Hentikan update lokasi setelah mendapat lokasi pertama
                locationManager.removeUpdates(locationListener);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Request lokasi dari GPS provider
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Coba gunakan network provider terlebih dahulu (lebih cepat)
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(this, "GPS atau Network Provider tidak tersedia", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Jika izin diberikan, ambil lokasi
                getLocation();
            } else {
                // Jika izin ditolak, beri pesan
                Toast.makeText(this, "Izin lokasi diperlukan untuk mendapatkan jadwal shalat", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin notifikasi diberikan
            } else {
                // Izin notifikasi ditolak
                Toast.makeText(this, "Izin notifikasi diperlukan untuk menampilkan notifikasi shalat", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}