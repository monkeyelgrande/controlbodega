package migracionagro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache de la tabla migracion_mapeo (bodega_nuevo). Cada entidad migrada queda
 * registrada como (tabla_origen, id_viejo) -> id_nuevo, lo que hace la
 * migracion idempotente: lo ya mapeado se omite en re-ejecuciones.
 */
public class Mapeo {

    private final Connection destino;
    private final Map<String, Map<Integer, Integer>> cache = new HashMap<>();

    public Mapeo(Connection destino) throws SQLException {
        this.destino = destino;
        try (Statement st = destino.createStatement();
                ResultSet rs = st.executeQuery(
                        "select tabla_origen, id_viejo, id_nuevo from migracion_mapeo")) {
            while (rs.next()) {
                tabla(rs.getString(1)).put(rs.getInt(2), rs.getInt(3));
            }
        }
    }

    private Map<Integer, Integer> tabla(String t) {
        Map<Integer, Integer> m = cache.get(t);
        if (m == null) {
            m = new HashMap<>();
            cache.put(t, m);
        }
        return m;
    }

    public boolean existe(String tabla, int idViejo) {
        return tabla(tabla).containsKey(idViejo);
    }

    /** id nuevo para un id viejo, o null si no esta mapeado. */
    public Integer get(String tabla, int idViejo) {
        return tabla(tabla).get(idViejo);
    }

    /**
     * Remapea una FK nullable: null -> null; no mapeada -> SQLException
     * (la migracion corre en orden de dependencias, no deberia ocurrir).
     */
    public Integer remap(String tabla, Integer idViejo) throws SQLException {
        if (idViejo == null) {
            return null;
        }
        Integer nuevo = get(tabla, idViejo);
        if (nuevo == null) {
            throw new SQLException("FK no resoluble: " + tabla + " id_viejo=" + idViejo);
        }
        return nuevo;
    }

    public void put(String tabla, int idViejo, int idNuevo, String accion) throws SQLException {
        try (PreparedStatement ps = destino.prepareStatement(
                "insert into migracion_mapeo (tabla_origen, id_viejo, id_nuevo, accion) values (?,?,?,?)")) {
            ps.setString(1, tabla);
            ps.setInt(2, idViejo);
            ps.setInt(3, idNuevo);
            ps.setString(4, accion);
            ps.executeUpdate();
        }
        tabla(tabla).put(idViejo, idNuevo);
    }

    /**
     * Registra solo en el cache en memoria (cuando el INSERT en
     * migracion_mapeo ya se hizo por batch).
     */
    public void putSoloCache(String tabla, int idViejo, int idNuevo) {
        tabla(tabla).put(idViejo, idNuevo);
    }

    public int contar(String tabla) {
        return tabla(tabla).size();
    }
}
