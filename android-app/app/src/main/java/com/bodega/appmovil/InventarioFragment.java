package com.bodega.appmovil;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bodega.appmovil.net.ApiClient;
import com.bodega.appmovil.net.ApiService;
import com.bodega.appmovil.net.modelo.BodegaStock;
import com.bodega.appmovil.net.modelo.ProductoInventario;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Pestana de consulta de inventario: busca y muestra stock por bodega. */
public class InventarioFragment extends Fragment {

    private static final String TAG = "BodegaMovil";

    private TextInputEditText inBusqueda;
    private TextView txtEstado;
    private LinearLayout contResultados;

    private ApiService api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventario, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        api = ApiClient.crear(new Sesion(requireContext()));

        inBusqueda = v.findViewById(R.id.inBusqueda);
        txtEstado = v.findViewById(R.id.txtEstadoInv);
        contResultados = v.findViewById(R.id.contResultados);
        MaterialButton btnBuscar = v.findViewById(R.id.btnBuscar);

        btnBuscar.setOnClickListener(x -> buscar());
        inBusqueda.setOnEditorActionListener((tv, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                buscar();
                return true;
            }
            return false;
        });
    }

    private void buscar() {
        String q = inBusqueda.getText() == null ? "" : inBusqueda.getText().toString().trim();
        if (q.length() < 2) {
            estado("Escribe al menos 2 caracteres.");
            return;
        }
        estado("Buscando...");
        contResultados.removeAllViews();

        api.inventario(q).enqueue(new Callback<List<ProductoInventario>>() {
            @Override
            public void onResponse(Call<List<ProductoInventario>> c,
                                   Response<List<ProductoInventario>> r) {
                try {
                    if (r.code() == 401) {
                        estado("Sesion vencida, vuelve a entrar.");
                        return;
                    }
                    if (!r.isSuccessful() || r.body() == null) {
                        estado("Error del servidor (" + r.code() + ").");
                        return;
                    }
                    List<ProductoInventario> lista = r.body();
                    if (lista.isEmpty()) {
                        estado("Sin resultados para \"" + q + "\".");
                        return;
                    }
                    estado(lista.size() + " producto(s) encontrado(s).");
                    for (ProductoInventario p : lista) {
                        contResultados.addView(tarjetaProducto(p));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error mostrando inventario", e);
                    estado("Error mostrando resultados: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<List<ProductoInventario>> c, Throwable t) {
                Log.e(TAG, "Fallo de red en inventario", t);
                estado("No se pudo conectar al servidor.");
            }
        });
    }

    private View tarjetaProducto(ProductoInventario p) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);

        TextView titulo = new TextView(requireContext());
        titulo.setText(safe(p.descripcion));
        titulo.setTextColor(Color.parseColor("#181D17"));
        titulo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(titulo);

        TextView sub = new TextView(requireContext());
        sub.setText("Codigo: " + safe(p.codigo)
                + "    Total disp.: " + num(p.totalDisponible)
                + "  (cant " + num(p.totalCantidad)
                + " / pend " + num(p.totalPendientes) + ")");
        sub.setTextColor(Color.parseColor("#40493D"));
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setPadding(0, dp(2), 0, dp(8));
        card.addView(sub);

        for (BodegaStock b : p.bodegas) {
            TextView fila = new TextView(requireContext());
            fila.setText("• " + safe(b.bodega)
                    + "   →   disp: " + num(b.disponible)
                    + "   (cant " + num(b.cantidad)
                    + " / pend " + num(b.pendientes) + ")");
            fila.setTextColor(Color.parseColor("#181D17"));
            fila.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            fila.setPadding(0, dp(4), 0, dp(4));
            card.addView(fila);
        }
        return card;
    }

    private void estado(String s) {
        txtEstado.setText(s == null ? "" : s);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String num(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }
}
