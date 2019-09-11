package com.example.notebookapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtils {
    private static final String PREF="APP_PREF";
    private static final String APIKEY="API_KEY";
    public static SharedPreferences getSharedPrefrences(Context context){
        return context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }
    public static void setApiKey(Context context,String apiKey){
        SharedPreferences.Editor editor=getSharedPrefrences(context).edit();
        editor.putString(APIKEY,apiKey);
        editor.apply();
    }
    public static String getApiKey(Context context){
        return getSharedPrefrences(context).getString(APIKEY,null);
    }
}
