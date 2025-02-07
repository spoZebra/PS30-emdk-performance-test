package com.spozebra.ps30emdkperftest;

import android.content.Context;
import android.content.SharedPreferences;

public class CounterManager {
    private static final String PREFS_NAME = "CounterPrefsMgr";
    private static final String COUNTER_KEY = "CounterVal";
    private SharedPreferences sharedPreferences;

    public CounterManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getCounter() {
        return sharedPreferences.getInt(COUNTER_KEY, 0);
    }

    public void incrementCounter() {
        int currentCount = getCounter();
        sharedPreferences.edit().putInt(COUNTER_KEY, currentCount + 1).apply();
    }
}
