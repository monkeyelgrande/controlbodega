package com.bodega.api.auth;

import com.bodega.api.util.Sha256;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Valida credenciales contra la tabla {@code users}, igual que la app de
 * escritorio: usuario por {@code user_name} y contrasena comparada como
 * SHA-256 hexadecimal.
 *
 * Diferencia con el escritorio: aqui la consulta es PARAMETRIZADA (no se
 * arma concatenando strings), lo que elimina el riesgo de inyeccion SQL en
 * el login.
 */
@Service
public class AuthService {

    private final JdbcTemplate jdbc;

    public AuthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Resultado de un login correcto. */
    public static final class UsuarioAutenticado {
        public int idUser;
        public String nombre;
        public String userName;
        public int idPerfil;
        public String perfil;
        public int idBodega;
        public String bodega;
        public String estado;
    }

    /**
     * Devuelve el usuario si {@code userName} + {@code passwordPlano} coinciden;
     * null si no hay coincidencia.
     */
    public UsuarioAutenticado autenticar(String userName, String passwordPlano) {
        if (userName == null || passwordPlano == null) {
            return null;
        }
        String passwordHash = Sha256.hex(passwordPlano);

        String sql =
                "SELECT u.id, u.nombre, u.user_name, u.id_perfil, u.id_bodega, "
                + "       u.estado, p.perfil AS perfil_nombre, b.nombre AS bodega_nombre "
                + "FROM users u "
                + "LEFT JOIN perfiles p ON p.id = u.id_perfil "
                + "LEFT JOIN bodegas b ON b.id = u.id_bodega "
                + "WHERE u.user_name = ? AND u.password = ?";

        List<UsuarioAutenticado> filas = jdbc.query(
                sql,
                new Object[]{userName, passwordHash},
                (rs, i) -> {
                    UsuarioAutenticado u = new UsuarioAutenticado();
                    u.idUser = rs.getInt("id");
                    u.nombre = rs.getString("nombre");
                    u.userName = rs.getString("user_name");
                    u.idPerfil = rs.getInt("id_perfil");
                    u.perfil = rs.getString("perfil_nombre");
                    u.idBodega = rs.getInt("id_bodega");
                    u.bodega = rs.getString("bodega_nombre");
                    u.estado = rs.getString("estado");
                    return u;
                });

        return filas.isEmpty() ? null : filas.get(0);
    }
}
