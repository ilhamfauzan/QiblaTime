package com.andro2.qiblatime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ViewGroup;
import android.graphics.Color;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PrayerTimeActivity extends AppCompatActivity {

    private TextView cityName;
    private LinearLayout prayerTimesContainer;

    private double latitude;
    private double longitude;
    private String city;

    // Handler untuk update countdown setiap detik
    private Handler handler = new Handler();
    private Runnable updateCountdownRunnable;

    // Class untuk menyimpan informasi shalat
    public static class PrayerInfo {
        String name;
        String time;
        long timeUntilPrayer;
        TextView timeTextView;
        TextView countdownTextView;

        public PrayerInfo(String name, String time) {
            this.name = name;
            this.time = time;
        }
    }

    // List untuk menyimpan informasi shalat
    private List<PrayerInfo> prayerList = new ArrayList<>();

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
        NotificationHelper.createNotificationChannel(this);
    }

    private void initViews() {
        cityName = findViewById(R.id.cityName);
        prayerTimesContainer = findViewById(R.id.prayerTimesContainer);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnToQibla = findViewById(R.id.btnToQibla);

        btnBack.setOnClickListener(v -> {
            // Kembali ke MainActivity
            Intent intent = new Intent(PrayerTimeActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Menutup activity saat ini
        });

        btnToQibla.setOnClickListener(v -> {
            Intent intent = new Intent(PrayerTimeActivity.this, CompassActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("city", city);
            startActivity(intent);
        });

        // Pastikan prayerTimesContainer tidak null
        if (prayerTimesContainer == null) {
            Toast.makeText(this, "Error: prayerTimesContainer not found in layout", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
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
        Call<PrayerTimesResponse> call = apiService.getPrayerTimes(latitude, longitude, 2);

        call.enqueue(new Callback<PrayerTimesResponse>() {
            @Override
            public void onResponse(Call<PrayerTimesResponse> call, Response<PrayerTimesResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                    response.body().getData() != null && response.body().getData().getTimings() != null) {
                    PrayerTimesResponse times = response.body();
                    PrayerTimesResponse.Timings timings = times.getData().getTimings();

                    // Buat list shalat
                    prayerList.clear();
                    prayerList.add(new PrayerInfo("Fajr", timings.getFajr()));
                    prayerList.add(new PrayerInfo("Dhuhr", timings.getDhuhr()));
                    prayerList.add(new PrayerInfo("Asr", timings.getAsr()));
                    prayerList.add(new PrayerInfo("Maghrib", timings.getMaghrib()));
                    prayerList.add(new PrayerInfo("Isha", timings.getIsha()));

                    // Hitung waktu hingga setiap shalat
                    calculateTimeUntilPrayers();

                    // Urutkan berdasarkan waktu terdekat
                    sortPrayersByClosest();

                    // Buat UI berdasarkan urutan baru
                    createSortedPrayerViews();

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

    private void calculateTimeUntilPrayers() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (int i = 0; i < prayerList.size(); i++) {
            PrayerInfo prayer = prayerList.get(i);
            try {
                Date prayerDate = timeFormat.parse(prayer.time);
                Calendar prayerCalendar = Calendar.getInstance();
                prayerCalendar.setTime(prayerDate);
                prayerCalendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
                prayerCalendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
                prayerCalendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

                // Jika waktu shalat sudah lewat hari ini, set untuk besok
                if (prayerCalendar.before(now)) {
                    prayerCalendar.add(Calendar.DAY_OF_MONTH, 1);
                }

                prayer.timeUntilPrayer = prayerCalendar.getTimeInMillis() - now.getTimeInMillis();

                // Schedule notification
                scheduleNotification(prayerCalendar.getTimeInMillis(), prayer.name, i);

            } catch (ParseException e) {
                e.printStackTrace();
                prayer.timeUntilPrayer = Long.MAX_VALUE;
            }
        }
    }

    private void sortPrayersByClosest() {
        Collections.sort(prayerList, new Comparator<PrayerInfo>() {
            @Override
            public int compare(PrayerInfo p1, PrayerInfo p2) {
                return Long.compare(p1.timeUntilPrayer, p2.timeUntilPrayer);
            }
        });
    }

    private void createSortedPrayerViews() {
        // Pastikan container tidak null
        if (prayerTimesContainer == null) {
            Toast.makeText(this, "Error: Prayer times container is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bersihkan container
        prayerTimesContainer.removeAllViews();

        for (int i = 0; i < prayerList.size(); i++) {
            PrayerInfo prayer = prayerList.get(i);

            // Gunakan CardView untuk shadow yang lebih baik
            CardView cardView = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 24); // Margin antar kartu
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(16f); // Sudut membulat
            cardView.setCardElevation(16f); // Shadow yang lebih kuat

            // Buat container di dalam CardView
            LinearLayout prayerContainer = new LinearLayout(this);
            prayerContainer.setOrientation(LinearLayout.VERTICAL);
            prayerContainer.setPadding(24, 24, 24, 24);

            // Atur warna latar belakang kartu
            if (i == 0) {
                cardView.setCardBackgroundColor(Color.parseColor("#E9EFEC"));
            } else if (i == 1) {
                cardView.setCardBackgroundColor(Color.parseColor("#C4DAD2"));
            } else {
                cardView.setCardBackgroundColor(Color.parseColor("#6A9C89"));
            }

            // Tentukan warna teks berdasarkan posisi kartu
            int textColor;
            if (i == 0 || i == 1) {
                textColor = Color.BLACK; // Teks hitam untuk kartu terang
            } else {
                textColor = Color.WHITE; // Teks putih untuk kartu gelap
            }

            // TextView untuk NAMA shalat (kecil, di atas)
            TextView prayerNameView = new TextView(this);
            prayerNameView.setTextSize(16);
            prayerNameView.setTextColor(textColor);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.setMargins(0, 0, 0, 4);
            prayerNameView.setLayoutParams(nameParams);

            // Tentukan emoji berdasarkan nama sholat
            String emoji = "";
            switch (prayer.name.toLowerCase()) {
                case "fajr":    emoji = "🌅"; break;
                case "dhuhr":   emoji = "☀️"; break;
                case "asr":     emoji = "🌇"; break;
                case "maghrib": emoji = "🌙"; break;
                case "isha":    emoji = "🌕"; break;
            }

            String prayerText = emoji + " " + prayer.name;

            // Beri penanda untuk shalat terdekat
            if (i == 0) {
                prayerNameView.setTextSize(18);
                prayerNameView.setText(prayerText + " (NEXT)");
            } else {
                prayerNameView.setText(prayerText);
            }

            // TextView untuk WAKTU shalat (besar, bold, di bawah nama)
            TextView prayerTimeView = new TextView(this);
            prayerTimeView.setText(prayer.time);
            prayerTimeView.setTextSize(40);
            prayerTimeView.setTypeface(null, android.graphics.Typeface.BOLD);
            prayerTimeView.setTextColor(textColor);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeParams.setMargins(0, 0, 0, 12);
            prayerTimeView.setLayoutParams(timeParams);

            // TextView untuk countdown
            TextView countdownView = new TextView(this);
            countdownView.setTextSize(16);
            countdownView.setTextColor(textColor);
            countdownView.setText("Time remaining: Calculating...");

            // Simpan reference ke TextViews
            prayer.countdownTextView = countdownView;

            // Tambahkan TextViews ke dalam LinearLayout
            prayerContainer.addView(prayerNameView);
            prayerContainer.addView(prayerTimeView);
            prayerContainer.addView(countdownView);

            // Tambahkan LinearLayout ke dalam CardView
            cardView.addView(prayerContainer);

            // Tambahkan CardView ke layout utama
            prayerTimesContainer.addView(cardView);
        }
    }

    private void startCountdown() {
        updateCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllCountdowns();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateCountdownRunnable);
    }

    private void updateAllCountdowns() {
        Calendar now = Calendar.getInstance();

        for (PrayerInfo prayer : prayerList) {
            if (prayer.countdownTextView != null) {
                String countdown = getTimeUntilPrayer(prayer.time);
                prayer.countdownTextView.setText("Time remaining: " + countdown);
            }
        }

        // Periksa apakah perlu mengurutkan ulang (setiap 60 detik)
        if (now.get(Calendar.SECOND) == 0) {
            calculateTimeUntilPrayers();
            sortPrayersByClosest();
            createSortedPrayerViews();
        }
    }

    private String getTimeUntilPrayer(String prayerTime) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date prayerDate = timeFormat.parse(prayerTime);

            Calendar now = Calendar.getInstance();
            Calendar prayer = Calendar.getInstance();

            prayer.setTime(prayerDate);
            prayer.set(Calendar.YEAR, now.get(Calendar.YEAR));
            prayer.set(Calendar.MONTH, now.get(Calendar.MONTH));
            prayer.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            if (prayer.before(now)) {
                prayer.add(Calendar.DAY_OF_MONTH, 1);
            }

            long difference = prayer.getTimeInMillis() - now.getTimeInMillis();

            if (difference <= 0) {
                return "00:00:00";
            }

            long hours = difference / (1000 * 60 * 60);
            long minutes = (difference % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (difference % (1000 * 60)) / 1000;

            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

        } catch (ParseException e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private void showLoadingState() {
        if (prayerTimesContainer == null) return;

        prayerTimesContainer.removeAllViews();

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading prayer times...");
        loadingText.setTextSize(16);
        loadingText.setTextColor(Color.GRAY);
        loadingText.setPadding(16, 16, 16, 16);

        prayerTimesContainer.addView(loadingText);
    }

    private void showErrorState() {
        if (prayerTimesContainer == null) return;

        prayerTimesContainer.removeAllViews();

        TextView errorText = new TextView(this);
        errorText.setText("Error loading prayer times");
        errorText.setTextSize(16);
        errorText.setTextColor(Color.RED);
        errorText.setPadding(16, 16, 16, 16);

        prayerTimesContainer.addView(errorText);

        Toast.makeText(this, "Gagal memuat jadwal shalat", Toast.LENGTH_SHORT).show();
    }

    private void scheduleNotification(long time, String prayerName, int notificationId) {
        Intent intent = new Intent(this, PrayerAlarmReceiver.class);
        intent.putExtra("prayerName", prayerName);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
                } else {
                    // Handle case where permission is not granted
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateCountdownRunnable != null) {
            handler.removeCallbacks(updateCountdownRunnable);
        }
    }
}