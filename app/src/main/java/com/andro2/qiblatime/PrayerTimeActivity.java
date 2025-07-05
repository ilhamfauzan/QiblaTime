package com.andro2.qiblatime;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrayerTimeActivity extends AppCompatActivity {

    private TextView fajr, dhuhr, asr, maghrib, isha, cityName;
    private TextView fajrNextTo, dhuhrNextTo, asrNextTo, maghribNextTo, ishaNextTo;

    private double latitude;
    private double longitude;
    private String city;

    // Handler untuk update countdown setiap detik
    private Handler handler = new Handler();
    private Runnable updateCountdownRunnable;

    // Array untuk menyimpan waktu shalat
    private String[] prayerTimes = new String[5];
    private String[] prayerNames = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer);

        // Inisialisasi views
        initViews();

        // Dapatkan data lokasi dari Intent
        getLocationDataFromIntent();

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

        // Initialize countdown TextViews
        fajrNextTo = findViewById(R.id.fajrNextTo);
        dhuhrNextTo = findViewById(R.id.dhuhrNextTo);
        asrNextTo = findViewById(R.id.asrNextTo);
        maghribNextTo = findViewById(R.id.maghribNextTo);
        ishaNextTo = findViewById(R.id.ishaNextTo);
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

                    // Simpan waktu shalat ke array
                    prayerTimes[0] = times.getData().getTimings().getFajr();
                    prayerTimes[1] = times.getData().getTimings().getDhuhr();
                    prayerTimes[2] = times.getData().getTimings().getAsr();
                    prayerTimes[3] = times.getData().getTimings().getMaghrib();
                    prayerTimes[4] = times.getData().getTimings().getIsha();

                    // Update UI dengan jadwal shalat
                    fajr.setText("Fajr: " + prayerTimes[0]);
                    dhuhr.setText("Dhuhr: " + prayerTimes[1]);
                    asr.setText("Asr: " + prayerTimes[2]);
                    maghrib.setText("Maghrib: " + prayerTimes[3]);
                    isha.setText("Isha: " + prayerTimes[4]);

                    // Mulai countdown
                    startCountdown();

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

    private void startCountdown() {
        updateCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllCountdowns();
                handler.postDelayed(this, 1000); // Update setiap detik
            }
        };
        handler.post(updateCountdownRunnable);
    }

    private void updateAllCountdowns() {
        TextView[] countdownViews = {fajrNextTo, dhuhrNextTo, asrNextTo, maghribNextTo, ishaNextTo};

        for (int i = 0; i < prayerTimes.length; i++) {
            if (prayerTimes[i] != null) {
                String countdown = getTimeUntilPrayer(prayerTimes[i]);
                countdownViews[i].setText("Time before " + prayerNames[i] + ": " + countdown);
            }
        }
    }

    private String getTimeUntilPrayer(String prayerTime) {
        try {
            // Parse waktu shalat (format HH:mm)
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date prayerDate = timeFormat.parse(prayerTime);

            // Dapatkan waktu sekarang
            Calendar now = Calendar.getInstance();
            Calendar prayer = Calendar.getInstance();

            // Set waktu shalat untuk hari ini
            prayer.setTime(prayerDate);
            prayer.set(Calendar.YEAR, now.get(Calendar.YEAR));
            prayer.set(Calendar.MONTH, now.get(Calendar.MONTH));
            prayer.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // Jika waktu shalat sudah lewat hari ini, set untuk besok
            if (prayer.before(now)) {
                prayer.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Hitung selisih waktu
            long difference = prayer.getTimeInMillis() - now.getTimeInMillis();

            if (difference <= 0) {
                return "00:00:00";
            }

            // Konversi ke jam, menit, detik
            long hours = difference / (1000 * 60 * 60);
            long minutes = (difference % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (difference % (1000 * 60)) / 1000;

            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

        } catch (ParseException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private String getNextPrayerInfo() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String nextPrayer = null;
        long shortestTime = Long.MAX_VALUE;

        for (int i = 0; i < prayerTimes.length; i++) {
            if (prayerTimes[i] != null) {
                try {
                    Date prayerDate = timeFormat.parse(prayerTimes[i]);
                    Calendar prayer = Calendar.getInstance();
                    prayer.setTime(prayerDate);
                    prayer.set(Calendar.YEAR, now.get(Calendar.YEAR));
                    prayer.set(Calendar.MONTH, now.get(Calendar.MONTH));
                    prayer.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

                    if (prayer.before(now)) {
                        prayer.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    long difference = prayer.getTimeInMillis() - now.getTimeInMillis();

                    if (difference < shortestTime) {
                        shortestTime = difference;
                        nextPrayer = prayerNames[i];
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        if (nextPrayer != null) {
            long hours = shortestTime / (1000 * 60 * 60);
            long minutes = (shortestTime % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (shortestTime % (1000 * 60)) / 1000;

            return String.format(Locale.getDefault(),
                    "Next: %s in %02d:%02d:%02d", nextPrayer, hours, minutes, seconds);
        }

        return "No upcoming prayer";
    }

    private void showLoadingState() {
        fajr.setText("Fajr: Loading...");
        dhuhr.setText("Dhuhr: Loading...");
        asr.setText("Asr: Loading...");
        maghrib.setText("Maghrib: Loading...");
        isha.setText("Isha: Loading...");

        fajrNextTo.setText("Time before Fajr: Loading...");
        dhuhrNextTo.setText("Time before Dhuhr: Loading...");
        asrNextTo.setText("Time before Asr: Loading...");
        maghribNextTo.setText("Time before Maghrib: Loading...");
        ishaNextTo.setText("Time before Isha: Loading...");
    }

    private void showErrorState() {
        fajr.setText("Fajr: Error");
        dhuhr.setText("Dhuhr: Error");
        asr.setText("Asr: Error");
        maghrib.setText("Maghrib: Error");
        isha.setText("Isha: Error");

        fajrNextTo.setText("Time before Fajr: Error");
        dhuhrNextTo.setText("Time before Dhuhr: Error");
        asrNextTo.setText("Time before Asr: Error");
        maghribNextTo.setText("Time before Maghrib: Error");
        ishaNextTo.setText("Time before Isha: Error");

        Toast.makeText(this, "Gagal memuat jadwal shalat", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan countdown saat activity dihancurkan
        if (handler != null && updateCountdownRunnable != null) {
            handler.removeCallbacks(updateCountdownRunnable);
        }
    }
}