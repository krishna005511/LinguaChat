package com.nakama.linguachat.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.nakama.linguachat.models.User;

public class SessionManager {

    private static final String PREF_NAME = "LinguaChatSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER = "user_json";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PREFERRED_LANG = "preferred_language";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    private static SessionManager instance;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // ── Token ──────────────────────────────────────────

    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Returns "Bearer <token>" for use in Authorization headers */
    public String getBearerToken() {
        String token = getToken();
        return token != null ? "Bearer " + token : null;
    }

    // ── User ──────────────────────────────────────────

    public void saveUser(User user) {
        editor.putString(KEY_USER, gson.toJson(user)).apply();
        if (user.getId() != null) {
            editor.putString(KEY_USER_ID, user.getId()).apply();
        }
        if (user.getPreferredLanguage() != null) {
            editor.putString(KEY_PREFERRED_LANG, user.getPreferredLanguage()).apply();
        }
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, User.class);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getPreferredLanguage() {
        return prefs.getString(KEY_PREFERRED_LANG, "en");
    }

    // ── Session state ──────────────────────────────────

    public boolean isLoggedIn() {
        return getToken() != null && getUserId() != null;
    }

    public void saveSession(String token, User user) {
        saveToken(token);
        saveUser(user);
    }

    public void clearSession() {
        editor.clear().apply();
    }
}
