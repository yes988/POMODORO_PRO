package com.example.POMODORO_PRO.ui.event;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;

import java.util.Collections;

public class GoogleCalendarServiceHelper {
    public static Calendar getCalendarService(GoogleSignInAccount account, Context context) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton("https://www.googleapis.com/auth/calendar")
        );
        credential.setSelectedAccount(account.getAccount());

        return new Calendar.Builder(transport, jsonFactory, credential)
                .setApplicationName("Pomodoro Pro")
                .build();
    }
}