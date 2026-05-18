package com.bodega.appmovil.net;

import com.bodega.appmovil.net.modelo.EntregaRequest;
import com.bodega.appmovil.net.modelo.EntregaResponse;
import com.bodega.appmovil.net.modelo.LoginRequest;
import com.bodega.appmovil.net.modelo.LoginResponse;
import com.bodega.appmovil.net.modelo.ProductoInventario;
import com.bodega.appmovil.net.modelo.ResultadoEscaneo;
import com.bodega.appmovil.net.modelo.SesionInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest req);

    @GET("api/auth/me")
    Call<SesionInfo> me();

    @GET("api/ordenes")
    Call<ResultadoEscaneo> escanear(@Query("qr") String qr);

    @POST("api/entregas")
    Call<EntregaResponse> entregar(@Body EntregaRequest req);

    @GET("api/inventario")
    Call<List<ProductoInventario>> inventario(@Query("q") String q);
}
