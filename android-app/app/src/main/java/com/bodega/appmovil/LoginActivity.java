package com.bodega.appmovil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bodega.appmovil.net.ApiClient;
import com.bodega.appmovil.net.ApiService;
import com.bodega.appmovil.net.modelo.LoginRequest;
import com.bodega.appmovil.net.modelo.LoginResponse;
import com.bodega.appmovil.net.modelo.SesionInfo;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText inServidor, inUsuario, inClave;
    private TextView txtEstado;
    private Button btnLogin;
    private Sesion sesion;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_login);

        sesion = new Sesion(this);
        inServidor = findViewById(R.id.inServidor);
        inUsuario = findViewById(R.id.inUsuario);
        inClave = findViewById(R.id.inClave);
        txtEstado = findViewById(R.id.txtEstado);
        btnLogin = findViewById(R.id.btnLogin);

        // Recuerda el ultimo servidor usado
        inServidor.setText(sesion.getServidor());

        btnLogin.setOnClickListener(v -> intentarLogin());

        // Si hay sesion guardada, intentar reanudarla automaticamente
        if (sesion.tieneSesion()) {
            reanudarSesion();
        }
    }

    /**
     * Valida el token guardado contra el servidor. Si sigue valido, entra
     * directo. Si expiro, pide login. Si el servidor no responde, avisa y
     * deja la pantalla de login para revisar la IP.
     */
    private void reanudarSesion() {
        btnLogin.setEnabled(false);
        txtEstado.setTextColor(0xFF40493D);
        txtEstado.setText("Reanudando sesion...");

        ApiService api = ApiClient.crear(sesion);
        api.me().enqueue(new Callback<SesionInfo>() {
            @Override
            public void onResponse(Call<SesionInfo> call, Response<SesionInfo> resp) {
                if (resp.code() == 401) {
                    // Token vencido o invalido: si pide login de nuevo.
                    sesion.cerrar();
                    btnLogin.setEnabled(true);
                    txtEstado.setTextColor(0xFFB00020);
                    txtEstado.setText("Tu sesion expiro. Inicia de nuevo.");
                } else {
                    // 200 o cualquier otro codigo (ej. 404 si el backend esta
                    // desactualizado): el servidor SI respondio, la IP esta
                    // bien. Entramos; si el token estuviera mal, las pantallas
                    // internas detectan el 401 y regresan al login.
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<SesionInfo> call, Throwable t) {
                // Aqui SI es problema real de conexion (servidor apagado o IP
                // mala). No se borra el token: cuando se corrija, reintenta.
                btnLogin.setEnabled(true);
                txtEstado.setTextColor(0xFFB00020);
                txtEstado.setText("No se pudo conectar a " + sesion.getServidor()
                        + ".\nVerifica la IP y que estes en el WiFi de la empresa.");
            }
        });
    }

    private void intentarLogin() {
        String servidor = txt(inServidor);
        String usuario = txt(inUsuario);
        String clave = txt(inClave);

        if (servidor.isEmpty() || usuario.isEmpty() || clave.isEmpty()) {
            txtEstado.setText("Completa servidor, usuario y contrasena.");
            return;
        }

        sesion.guardarServidor(servidor);
        btnLogin.setEnabled(false);
        txtEstado.setText("Conectando...");

        ApiService api = ApiClient.crear(sesion);
        api.login(new LoginRequest(usuario, clave)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> resp) {
                btnLogin.setEnabled(true);
                if (resp.isSuccessful() && resp.body() != null) {
                    LoginResponse r = resp.body();
                    sesion.guardarLogin(r.token, r.idUser, r.idBodega, r.nombre, r.bodega);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else if (resp.code() == 401) {
                    txtEstado.setText("Usuario o contrasena incorrectos.");
                } else {
                    txtEstado.setText("Error del servidor (" + resp.code() + ").");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                txtEstado.setText("No se pudo conectar al servidor.\n"
                        + "Revisa la IP y que estes en el WiFi de la empresa.");
            }
        });
    }

    private String txt(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
