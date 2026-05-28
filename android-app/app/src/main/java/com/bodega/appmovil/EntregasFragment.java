package com.bodega.appmovil;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private static final String SIN_ORDEN = "Aun no has escaneado ninguna orden.";

    private TextView txtOrden, txtEstado;
    private LinearLayout contProductos, zonaOrden;
    private MaterialButton btnEscanear, btnCompleta, btnParcial, btnCancelar;

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
        zonaOrden = v.findViewById(R.id.zonaOrden);
        btnEscanear = v.findViewById(R.id.btnEscanear);
        btnCompleta = v.findViewById(R.id.btnEntregaCompleta);
        btnParcial = v.findViewById(R.id.btnEntregaParcial);
        btnCancelar = v.findViewById(R.id.btnCancelar);

        btnEscanear.setOnClickListener(x -> abrirEscaner());
        btnCompleta.setOnClickListener(x -> entregar("ENTREGA_COMPLETA"));
        btnParcial.setOnClickListener(x -> entregar("ENTREGA_PARCIAL"));
        btnCancelar.setOnClickListener(x -> cancelar());

        estadoSinOrden();
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
                        estadoSinOrden();
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
                        estadoSinOrden();
                        estadoError(msg);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando la respuesta de la orden", e);
                    estadoSinOrden();
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
                + "   |   Pendiente total: " + num(o.totalPendiente));

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
            cardLp.setMargins(0, dp(5), 0, dp(5));
            card.setLayoutParams(cardLp);

            LinearLayout textos = new LinearLayout(requireContext());
            textos.setOrientation(LinearLayout.VERTICAL);
            textos.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView desc = new TextView(requireContext());
            desc.setText(safe(p.descripcion));
            desc.setTextColor(Color.parseColor("#181D17"));
            desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            desc.setTypeface(Typeface.DEFAULT_BOLD);

            TextView info = new TextView(requireContext());
            info.setText(safe(p.codigo) + "   |   pend: " + num(p.pendiente)
                    + "   |   stock: " + num(p.stockBodega));
            info.setTextColor(Color.parseColor("#40493D"));
            info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            info.setPadding(0, dp(3), 0, 0);

            textos.addView(desc);
            textos.addView(info);

            EditText e = new EditText(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(96), dp(58));
            lp.setMargins(dp(12), 0, 0, 0);
            e.setLayoutParams(lp);
            e.setBackgroundResource(R.drawable.bg_input_cantidad);
            e.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            e.setGravity(Gravity.CENTER);
            e.setTextColor(Color.parseColor("#181D17"));
            e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            e.setTypeface(Typeface.DEFAULT_BOLD);
            e.setText(num(p.pendiente));
            e.setTag(p.idProducto);

            camposCantidad.add(e);
            card.addView(textos);
            card.addView(e);
            contProductos.addView(card);
        }

        estadoConOrden();
        estado(actual != null && actual.mensaje != null
                ? actual.mensaje : "Orden lista para entregar.");
    }

    private void entregar(String accion) {
        if (actual == null || actual.orden == null || ocupado) {
            return;
        }
        EntregaRequest req = new EntregaRequest();
        req.idFactura = actual.orden.idFactura;
        req.idEscaneo = actual.idEscaneo;
        req.accion = accion;

        List<String> lineas = new ArrayList<>();
        double totalUnidades = 0;

        if ("ENTREGA_PARCIAL".equals(accion)) {
            for (EditText e : camposCantidad) {
                double cant = parse(e.getText().toString());
                if (cant > 0 && e.getTag() instanceof Integer) {
                    int idP = (Integer) e.getTag();
                    req.items.add(new ItemEntrega(idP, cant));
                    lineas.add("• " + descripcionDe(idP) + "   x " + num(cant));
                    totalUnidades += cant;
                }
            }
            if (req.items.isEmpty()) {
                Toast.makeText(requireContext(), "Escribe al menos una cantidad.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        } else { // ENTREGA_COMPLETA: se entrega todo lo pendiente
            for (ProductoPendiente p : actual.orden.productos) {
                if (p.pendiente > 0) {
                    lineas.add("• " + safe(p.descripcion) + "   x " + num(p.pendiente));
                    totalUnidades += p.pendiente;
                }
            }
            if (lineas.isEmpty()) {
                Toast.makeText(requireContext(), "No hay pendientes por entregar.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String titulo = "ENTREGA_COMPLETA".equals(accion)
                ? "Confirmar entrega completa" : "Confirmar entrega";
        String msg = "Vas a entregar:\n\n"
                + TextUtils.join("\n", lineas)
                + "\n\nTotal: " + lineas.size() + " producto(s), "
                + num(totalUnidades) + " unidad(es).\n\n¿Confirmar la entrega?";

        new AlertDialog.Builder(requireContext())
                .setTitle(titulo)
                .setMessage(msg)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", (d, w) -> ejecutarEntrega(req))
                .show();
    }

    /** Devuelve la descripcion de un producto de la orden actual. */
    private String descripcionDe(int idProducto) {
        if (actual != null && actual.orden != null && actual.orden.productos != null) {
            for (ProductoPendiente p : actual.orden.productos) {
                if (p.idProducto == idProducto) {
                    return safe(p.descripcion);
                }
            }
        }
        return "Producto " + idProducto;
    }

    /** Ejecuta realmente la entrega (despues de confirmar). */
    private void ejecutarEntrega(EntregaRequest req) {
        ocupado = true;
        habilitarBotones(false);
        ocultarTeclado();
        estado("Registrando entrega...");

        api.entregar(req).enqueue(new Callback<EntregaResponse>() {
            @Override
            public void onResponse(Call<EntregaResponse> c, Response<EntregaResponse> r) {
                ocupado = false;
                try {
                    if (r.isSuccessful() && r.body() != null && r.body().ok) {
                        EntregaResponse er = r.body();
                        estadoSinOrden();
                        txtOrden.setText("Entrega OK (cabecera #" + er.idCabecera + ").\n"
                                + "Escanea la siguiente orden.");
                        estado(er.mensaje);
                    } else {
                        habilitarBotones(true);
                        estadoError("No se pudo entregar (" + r.code() + "). "
                                + leerError(r));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando la entrega", e);
                    habilitarBotones(true);
                    estadoError("Error en la entrega: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<EntregaResponse> c, Throwable t) {
                ocupado = false;
                habilitarBotones(true);
                Log.e(TAG, "Fallo de red al entregar", t);
                estadoError("No se pudo conectar al servidor.");
            }
        });
    }

    /** Boton Cancelar: vuelve al estado inicial (esperando escaneo). */
    private void cancelar() {
        if (ocupado) {
            return;
        }
        ocultarTeclado();
        estadoSinOrden();
        txtEstado.setText("");
    }

    /** Estado "sin orden": muestra escanear, oculta acciones de entrega. */
    private void estadoSinOrden() {
        contProductos.removeAllViews();
        camposCantidad.clear();
        actual = null;
        txtOrden.setText(SIN_ORDEN);
        zonaOrden.setVisibility(View.GONE);
        btnEscanear.setVisibility(View.VISIBLE);
        chrome(true);   // restaura cabecera y pestanas
    }

    /** Estado "con orden": oculta escanear, muestra acciones de entrega. */
    private void estadoConOrden() {
        btnEscanear.setVisibility(View.GONE);
        zonaOrden.setVisibility(View.VISIBLE);
        habilitarBotones(true);
        chrome(false);  // colapsa cabecera y pestanas -> mas espacio
    }

    /** Muestra/oculta la cabecera y pestanas de la pantalla principal. */
    private void chrome(boolean visible) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setChromeVisible(visible);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Por si el fragmento se destruye con una orden cargada, no dejar
        // la cabecera escondida.
        chrome(true);
    }

    private void habilitarBotones(boolean on) {
        btnCompleta.setEnabled(on);
        btnParcial.setEnabled(on);
        btnCancelar.setEnabled(on);
    }

    private void ocultarTeclado() {
        try {
            View f = requireActivity().getCurrentFocus();
            if (f != null) {
                InputMethodManager imm = (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
                }
            }
        } catch (Exception ignore) {
        }
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
