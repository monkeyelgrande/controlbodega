package com.bodega.appmovil;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bodega.appmovil.net.ApiClient;
import com.bodega.appmovil.net.ApiService;
import com.bodega.appmovil.net.modelo.EntregaRequest;
import com.bodega.appmovil.net.modelo.EntregaResponse;
import com.bodega.appmovil.net.modelo.ItemEntrega;
import com.bodega.appmovil.net.modelo.OrdenInfo;
import com.bodega.appmovil.net.modelo.ProductoPendiente;
import com.bodega.appmovil.net.modelo.ResultadoEscaneo;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Pestana de entregas: escanear QR -> ver pendientes -> entregar. */
public class EntregasFragment extends Fragment {

    private static final String TAG = "BodegaMovil";

    private TextView txtOrden, txtEstado;
    private LinearLayout contProductos;
    private MaterialButton btnEscanear, btnCompleta, btnParcial;

    private Sesion sesion;
    private ApiService api;

    private ResultadoEscaneo actual;
    private boolean ocupado = false;
    private final List<EditText> camposCantidad = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> escaner =
            registerForActivityResult(new ScanContract(), res -> {
                try {
                    if (res != null && res.getContents() != null) {
                        procesarQr(res.getContents());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error al recibir el QR", e);
                    estadoError("Error leyendo el QR: " + e.getMessage());
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entregas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        sesion = new Sesion(requireContext());
        api = ApiClient.crear(sesion);

        txtOrden = v.findViewById(R.id.txtOrden);
        txtEstado = v.findViewById(R.id.txtEstado);
        contProductos = v.findViewById(R.id.contProductos);
        btnEscanear = v.findViewById(R.id.btnEscanear);
        btnCompleta = v.findViewById(R.id.btnEntregaCompleta);
        btnParcial = v.findViewById(R.id.btnEntregaParcial);

        btnEscanear.setOnClickListener(x -> abrirEscaner());
        btnCompleta.setOnClickListener(x -> entregar("ENTREGA_COMPLETA"));
        btnParcial.setOnClickListener(x -> entregar("ENTREGA_PARCIAL"));
    }

    private void abrirEscaner() {
        if (ocupado) {
            return;
        }
        try {
            ScanOptions o = new ScanOptions();
            o.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            o.setPrompt("Apunta al QR de la orden");
            o.setBeepEnabled(true);
            o.setOrientationLocked(false);
            escaner.launch(o);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo abrir el escaner", e);
            estadoError("No se pudo abrir la camara: " + e.getMessage());
        }
    }

    private void procesarQr(String qr) {
        ocupado = true;
        estado("Consultando orden...");
        api.escanear(qr).enqueue(new Callback<ResultadoEscaneo>() {
            @Override
            public void onResponse(Call<ResultadoEscaneo> c, Response<ResultadoEscaneo> r) {
                ocupado = false;
                try {
                    if (r.code() == 401) {
                        cerrarSesion();
                        return;
                    }
                    if (!r.isSuccessful() || r.body() == null) {
                        limpiar();
                        estadoError("Error del servidor (" + r.code() + ").");
                        return;
                    }
                    actual = r.body();
                    if ("OK".equals(actual.resultado) && actual.orden != null) {
                        mostrarOrden(actual.orden);
                    } else {
                        String msg = actual.mensaje != null
                                ? actual.mensaje
                                : ("Resultado: " + actual.resultado);
                        limpiar();
                        estadoError(msg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando la respuesta de la orden", e);
                    limpiar();
                    estadoError("Error mostrando la orden: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResultadoEscaneo> c, Throwable t) {
                ocupado = false;
                Log.e(TAG, "Fallo de red al escanear", t);
                estadoError("No se pudo conectar al servidor.");
            }
        });
    }

    private void mostrarOrden(OrdenInfo o) {
        txtOrden.setText("Orden " + o.codigoFactura + "  (#" + o.idFactura + ")"
                + "\nCliente: " + safe(o.nombreCliente)
                + "\nBodega: " + safe(o.nombreBodegaOrden)
                + "\nPendiente total: " + num(o.totalPendiente));

        contProductos.removeAllViews();
        camposCantidad.clear();

        List<ProductoPendiente> prods = o.productos != null
                ? o.productos : new ArrayList<>();

        for (ProductoPendiente p : prods) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            int pad = dp(14);
            card.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, dp(6), 0, dp(6));
            card.setLayoutParams(cardLp);

            TextView t = new TextView(requireContext());
            t.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            t.setText(safe(p.descripcion) + "\n"
                    + safe(p.codigo) + "   |   pend: " + num(p.pendiente)
                    + "   |   stock: " + num(p.stockBodega));
            t.setTextColor(Color.parseColor("#181D17"));
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            t.setLineSpacing(dp(2), 1f);

            EditText e = new EditText(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(80), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(12), 0, 0, 0);
            e.setLayoutParams(lp);
            e.setBackgroundResource(R.drawable.bg_input_cantidad);
            e.setPadding(dp(8), dp(12), dp(8), dp(12));
            e.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            e.setGravity(Gravity.CENTER);
            e.setTextColor(Color.parseColor("#181D17"));
            e.setText(num(p.pendiente));
            e.setTag(p.idProducto);

            camposCantidad.add(e);
            card.addView(t);
            card.addView(e);
            contProductos.addView(card);
        }

        btnCompleta.setEnabled(true);
        btnParcial.setEnabled(true);
        estado(actual != null ? actual.mensaje : "Orden lista.");
    }

    private void entregar(String accion) {
        if (actual == null || actual.orden == null || ocupado) {
            return;
        }
        EntregaRequest req = new EntregaRequest();
        req.idFactura = actual.orden.idFactura;
        req.idEscaneo = actual.idEscaneo;
        req.accion = accion;

        if ("ENTREGA_PARCIAL".equals(accion)) {
            for (EditText e : camposCantidad) {
                double cant = parse(e.getText().toString());
                if (cant > 0 && e.getTag() instanceof Integer) {
                    req.items.add(new ItemEntrega((Integer) e.getTag(), cant));
                }
            }
            if (req.items.isEmpty()) {
                Toast.makeText(requireContext(), "Escribe al menos una cantidad.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ocupado = true;
        btnCompleta.setEnabled(false);
        btnParcial.setEnabled(false);
        estado("Registrando entrega...");

        api.entregar(req).enqueue(new Callback<EntregaResponse>() {
            @Override
            public void onResponse(Call<EntregaResponse> c, Response<EntregaResponse> r) {
                ocupado = false;
                try {
                    if (r.isSuccessful() && r.body() != null && r.body().ok) {
                        EntregaResponse er = r.body();
                        limpiar();
                        txtOrden.setText("Entrega OK (cabecera #" + er.idCabecera + ").\n"
                                + "Escanea la siguiente orden.");
                        estado(er.mensaje);
                    } else {
                        btnCompleta.setEnabled(true);
                        btnParcial.setEnabled(true);
                        estadoError("No se pudo entregar (" + r.code() + "). "
                                + leerError(r));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando la entrega", e);
                    estadoError("Error en la entrega: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<EntregaResponse> c, Throwable t) {
                ocupado = false;
                btnCompleta.setEnabled(true);
                btnParcial.setEnabled(true);
                Log.e(TAG, "Fallo de red al entregar", t);
                estadoError("No se pudo conectar al servidor.");
            }
        });
    }

    private void limpiar() {
        contProductos.removeAllViews();
        camposCantidad.clear();
        btnCompleta.setEnabled(false);
        btnParcial.setEnabled(false);
        actual = null;
    }

    private String leerError(Response<?> r) {
        try {
            return r.errorBody() != null ? r.errorBody().string() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void cerrarSesion() {
        sesion.cerrar();
        Toast.makeText(requireContext(),
                "Sesion vencida, inicia de nuevo.", Toast.LENGTH_LONG).show();
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
    }

    private void estado(String s) {
        txtEstado.setTextColor(Color.parseColor("#0D631B"));
        txtEstado.setText(s == null ? "" : s);
    }

    private void estadoError(String s) {
        txtEstado.setTextColor(Color.parseColor("#BA1A1A"));
        txtEstado.setText(s == null ? "" : s);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double parse(String s) {
        try {
            return Double.parseDouble(s.trim().replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    private static String num(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
