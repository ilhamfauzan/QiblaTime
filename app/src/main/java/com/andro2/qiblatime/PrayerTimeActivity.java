package com.andro2.qiblatime;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrayerTimeActivity extends AppCompatActivity {

    private TextView fajr, dhuhr, asr, maghrib, isha, cityName;

    private double latitude;
    private double longitude;
    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer);

        // Inisialisasi views
        initViews();

        // Dapatkan data lokasi dari Intent
        getLocationDataFromIntent();

//        // Setup click listeners
//        setupClickListeners();

        // Ambil jadwal shalat berdasarkan lokasi
        if (latitude != 0.0 && longitude != 0.0) {
            fetchPrayerTimes(latitude, longitude);
        } else {
            Toast.makeText(this, "Data lokasi tidak valid", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        fajr = findViewById(R.id.fajr);
        dhuhr = findViewById(R.id.dhuhr);
        asr = findViewById(R.id.asr);
        maghrib = findViewById(R.id.maghrib);
        isha = findViewById(R.id.isha);
        cityName = findViewById(R.id.cityName);
    }

    private void getLocationDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            latitude = intent.getDoubleExtra("latitude", 0.0);
            longitude = intent.getDoubleExtra("longitude", 0.0);
            city = intent.getStringExtra("city");

            // Tampilkan nama kota
            if (city != null && !city.isEmpty()) {
                cityName.setText("City: " + city);
            } else {
                cityName.setText("City: Unknown");
            }
        }
    }



    private void fetchPrayerTimes(double latitude, double longitude) {
        // Tampilkan loading
        showLoadingState();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.aladhan.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PrayerApiService apiService = retrofit.create(PrayerApiService.class);
        Call<PrayerTimesResponse> call = apiService.getPrayerTimes(latitude, longitude, 2); // Method 2 is Umm al-Qura

        call.enqueue(new Callback<PrayerTimesResponse>() {
            @Override
            public void onResponse(Call<PrayerTimesResponse> call, Response<PrayerTimesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PrayerTimesResponse times = response.body();

                    // Update UI dengan jadwal shalat
                    fajr.setText("Fajr: " + times.getData().getTimings().getFajr());
                    dhuhr.setText("Dhuhr: " + times.getData().getTimings().getDhuhr());
                    asr.setText("Asr: " + times.getData().getTimings().getAsr());
                    maghrib.setText("Maghrib: " + times.getData().getTimings().getMaghrib());
                    isha.setText("Isha: " + times.getData().getTimings().getIsha());

                    Toast.makeText(PrayerTimeActivity.this, "Jadwal shalat berhasil dimuat", Toast.LENGTH_SHORT).show();
                } else {
                    showErrorState();
                }
            }

            @Override
            public void onFailure(Call<PrayerTimesResponse> call, Throwable t) {
                t.printStackTrace();
                showErrorState();
            }
        });
    }

    private void showLoadingState() {
        fajr.setText("Fajr: Loading...");
        dhuhr.setText("Dhuhr: Loading...");
        asr.setText("Asr: Loading...");
        maghrib.setText("Maghrib: Loading...");
        isha.setText("Isha: Loading...");
    }

    private void showErrorState() {
        fajr.setText("Fajr: Error");
        dhuhr.setText("Dhuhr: Error");
        asr.setText("Asr: Error");
        maghrib.setText("Maghrib: Error");
        isha.setText("Isha: Error");

        Toast.makeText(this, "Gagal memuat jadwal shalat", Toast.LENGTH_SHORT).show();
    }
}