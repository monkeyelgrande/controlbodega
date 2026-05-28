package com.bodega.appmovil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Pantalla principal con dos pestanas: Entregas y Consulta de inventario.
 */
public class MainActivity extends AppCompatActivity {

    private View headerBar;
    private TabLayout tabs;
    private ViewPager2 pager;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        Sesion sesion = new Sesion(this);
        TextView txtUsuario = findViewById(R.id.txtUsuario);
        txtUsuario.setText("Bodega: " + sesion.getBodega() + "   |   " + sesion.getNombre());

        MaterialButton btnSalir = findViewById(R.id.btnSalir);
        btnSalir.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("Vas a cerrar la sesion de " + sesion.getNombre() + ".")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesion", (d, w) -> {
                    sesion.cerrar();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .show());

        headerBar = findViewById(R.id.headerBar);
        tabs = findViewById(R.id.tabs);
        pager = findViewById(R.id.pager);
        pager.setAdapter(new MainPagerAdapter(this));

        new TabLayoutMediator(tabs, pager, (tab, pos) ->
                tab.setText(pos == 0 ? "Entregas" : "Inventario")
        ).attach();
    }

    /**
     * Colapsa/expande la cabecera y las pestanas. Se usa desde la pestana
     * de Entregas para dar mas espacio a la lista cuando hay una orden
     * escaneada (y restaurarlas al volver a esperar escaneo).
     */
    public void setChromeVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        if (headerBar != null) headerBar.setVisibility(vis);
        if (tabs != null) tabs.setVisibility(vis);
        // Mientras se procesa una orden, bloquea el swipe entre pestanas.
        if (pager != null) pager.setUserInputEnabled(visible);
    }
}
