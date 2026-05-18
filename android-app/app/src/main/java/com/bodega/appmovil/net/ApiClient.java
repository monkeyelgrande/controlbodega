package com.bodega.appmovil.net;

import com.bodega.appmovil.Sesion;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Construye el cliente Retrofit apuntando al servidor que escribio el
 * usuario. Inyecta el token (si existe) en cada peticion.
 */
public final class ApiClient {

    private ApiClient() {
    }

    public static ApiService crear(final Sesion sesion) {
        String host = sesion.getServidor().trim();
        String base = host.contains(":")
                ? "http://" + host + "/"
                : "http://" + host + ":8080/";

        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    String token = sesion.getToken();
                    if (token == null || token.isEmpty()) {
                        return chain.proceed(chain.request());
                    }
                    return chain.proceed(chain.request().newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .header("X-Dispositivo", android.os.Build.MODEL)
                            .build());
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(base)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }
}
