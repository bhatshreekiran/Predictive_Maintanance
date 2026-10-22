package com.cogxar.pdm.data.remote;

import com.cogxar.pdm.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton client configuring Retrofit and OkHttpClient.
 */
public class RetrofitClient {

    private static volatile ApiService apiService;

    public static ApiService getApiService() {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(BuildConfig.DEBUG
                            ? HttpLoggingInterceptor.Level.BODY
                            : HttpLoggingInterceptor.Level.NONE);

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(10, TimeUnit.SECONDS)
                            .addInterceptor(logging)
                            .build();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    apiService = retrofit.create(ApiService.class);
                }
            }
        }
        return apiService;
    }
}
