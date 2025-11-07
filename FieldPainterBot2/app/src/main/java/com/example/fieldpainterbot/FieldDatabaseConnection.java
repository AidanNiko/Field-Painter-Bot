package com.example.fieldpainterbot;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;

public class FieldDatabaseConnection {

    private final OkHttpClient client = new OkHttpClient();

    // Method to fetch data from backend
    public interface OnDataFetchedListener {
        void onDataFetched(String data);
        void onError(String error);
    }

    public void fetchData(String FieldName, DataCallback callback) {
        String url = "https://your-backend.onrender.com/FieldData?collection_name=" + FieldName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer 221865dd6af38df8f23845fa33f98caa")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("API Error", "Request failed", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();  // Keep JSON intact
                    callback.onDataReceived(jsonData);
                } else {
                    Log.e("API Error", "Response not successful: " + response.code());
                }
            }
        });
    }

    // Simple callback interface
    public interface DataCallback {
        void onDataReceived(String data);
    }

}

