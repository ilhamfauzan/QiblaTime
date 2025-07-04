package com.andro2.qiblatime;

public class PrayerTimesResponse {
    private Data data;

    public Data getData() {
        return data;
    }

    public class Data {
        private Timings timings;

        public Timings getTimings() {
            return timings;
        }
    }

    public class Timings {
        private String Fajr;
        private String Dhuhr;
        private String Asr;
        private String Maghrib;
        private String Isha;

        public String getFajr() {
            return Fajr;
        }

        public String getDhuhr() {
            return Dhuhr;
        }

        public String getAsr() {
            return Asr;
        }

        public String getMaghrib() {
            return Maghrib;
        }

        public String getIsha() {
            return Isha;
        }
    }
}
