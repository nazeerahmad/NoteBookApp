package com.example.notebookapp.network;

import android.content.Context;
import android.text.TextUtils;

import com.example.notebookapp.app.Const;
import com.example.notebookapp.utils.PrefUtils;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ClientApi {
    private static Retrofit retrofit=null;
    private static int REQUEST_TIMEOUT=60;
    private static OkHttpClient okHttpClient;

    public static Retrofit getRetrofit(Context context){
        if(okHttpClient==null)
            initOkHttp(context);

        if(retrofit==null){
            retrofit=new Retrofit.Builder()
                    .baseUrl(Const.BASE_URL)
                    .client(okHttpClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static void initOkHttp(final Context context) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REQUEST_TIMEOUT,TimeUnit.SECONDS)
                .writeTimeout(REQUEST_TIMEOUT,TimeUnit.SECONDS);
        HttpLoggingInterceptor loggingInterceptor=new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        httpClient.addInterceptor(loggingInterceptor);

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request requestOriginal=chain.request();
                Request.Builder requestBuilder=requestOriginal.newBuilder()
                        .addHeader("Accept","application/json")
                        .addHeader("Content-type","application/json");

                if(!TextUtils.isEmpty(PrefUtils.getApiKey(context))){
                    requestBuilder.addHeader("Authorization",PrefUtils.getApiKey(context));
                }
                Request request=requestBuilder.build();

                return chain.proceed(request);
            }
        });
        okHttpClient=httpClient.build();
    }
}
