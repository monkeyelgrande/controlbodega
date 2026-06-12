package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import modelos.Opcion;

/**
 * Acceso a datos del sistema de permisos administrables (tablas opciones,
 * perfil_opciones, usuario_opciones y usuario_roles_precios).
 *
 * Permiso efectivo de un usuario = opciones de su perfil
 *                                  + opciones concedidas por usuario
 *                                  - opciones revocadas por usuario.
 * El perfil 1 (Admin) no se consulta: la app le concede todo en codigo.
 *
 * @author Monkeyelgrande
 */
public class DBpermisos {

    /**
     * Claves de las opciones efectivas del usuario. Devuelve null si la
     * consulta falla (p. ej. BD sin la migracion de permisos): el llamador
     * debe caer al comportamiento anterior.
     */
    public static Set<String> opcionesEfectivas(int idUser, int idPerfil) {
        String sql = "select o.clave from opciones o "
                + "where ((exists (select 1 from perfil_opciones po "
                + "                where po.id_perfil=? and po.id_opcion=o.id)) "
                + "   or (exists (select 1 from usuario_opciones uo "
                + "               where uo.id_user=? and uo.id_opcion=o.id and uo.concedido))) "
                + "  and not exists (select 1 from usuario_opciones ur "
                + "                  where ur.id_user=? and ur.id_opcion=o.id and not ur.concedido)";
        Set<String> claves = new HashSet<>();
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPerfil);
            ps.setInt(2, idUser);
            ps.setInt(3, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claves.add(rs.getString("clave"));
                }
            }
            return claves;
        } catch (SQLException e) {
            System.out.println("DBpermisos.opcionesEfectivas: " + e);
            return null;
        }
    }

    /** Roles del modulo Precios del usuario (2/3/4). Vacio si no tiene. */
    public static List<Integer> rolesPrecios(int idUser) {
        List<Integer> roles = new ArrayList<>();
        String sql = "select rol from usuario_roles_precios where id_user=? order by rol";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getInt("rol"));
                }
            }
        } catch (SQLException e) {
            System.out.println("DBpermisos.rolesPrecios: " + e);
        }
        return roles;
    }

    /**
     * Reemplaza los roles de Precios del usuario y sincroniza
     * users.rol_precios (columna de transicion) con el mayor rol asignado.
     */
    public static boolean guardarRolesPrecios(int idUser, Collection<Integer> roles) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement(
                    "delete from usuario_roles_precios where id_user=?")) {
                del.setInt(1, idUser);
                del.executeUpdate();
            }
            int mayor = 0;
            try (PreparedStatement ins = con.prepareStatement(
                    "insert into usuario_roles_precios (id_user, rol) values (?, ?)")) {
                for (int rol : roles) {
                    ins.setInt(1, idUser);
                    ins.setInt(2, rol);
                    ins.executeUpdate();
                    if (rol > mayor) {
                        mayor = rol;
                    }
                }
            }
            try (PreparedStatement upd = con.prepareStatement(
                    "update users set rol_precios=? where id=?")) {
                upd.setInt(1, mayor);
                upd.setInt(2, idUser);
                upd.executeUpdate();
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }
            JOptionPane.showMessageDialog(null, "Error al guardar los roles de Precios:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /** Catalogo completo de opciones, ordenado por modulo y orden. */
    public static List<Opcion> listarOpciones() {
        List<Opcion> lista = new ArrayList<>();
        String sql = "select id, clave, nombre, modulo, componente, orden "
                + "from opciones order by modulo, orden, nombre";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new Opcion(rs.getInt("id"), rs.getString("clave"),
                        rs.getString("nombre"), rs.getString("modulo"),
                        rs.getString("componente"), rs.getInt("orden")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al consultar las opciones:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        }
        return lista;
    }

    /** Ids de las opciones concedidas a un perfil. */
    public static Set<Integer> opcionesPerfil(int idPerfil) {
        Set<Integer> ids = new HashSet<>();
        String sql = "select id_opcion from perfil_opciones where id_perfil=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPerfil);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_opcion"));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al consultar permisos del perfil:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        }
        return ids;
    }

    /** Reemplaza las opciones concedidas a un perfil. */
    public static boolean guardarOpcionesPerfil(int idPerfil, Collection<Integer> idsOpciones) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement(
                    "delete from perfil_opciones where id_perfil=?")) {
                del.setInt(1, idPerfil);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "insert into perfil_opciones (id_perfil, id_opcion) values (?, ?)")) {
                for (int idOpcion : idsOpciones) {
                    ins.setInt(1, idPerfil);
                    ins.setInt(2, idOpcion);
                    ins.executeUpdate();
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }
            JOptionPane.showMessageDialog(null, "Error al guardar permisos del perfil:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Excepciones por usuario: id_opcion -> concedido (true=agregada sobre su
     * perfil, false=revocada de su perfil).
     */
    public static Map<Integer, Boolean> excepcionesUsuario(int idUser) {
        Map<Integer, Boolean> excepciones = new HashMap<>();
        String sql = "select id_opcion, concedido from usuario_opciones where id_user=?";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    excepciones.put(rs.getInt("id_opcion"), rs.getBoolean("concedido"));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al consultar permisos del usuario:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
        }
        return excepciones;
    }

    /** Reemplaza las excepciones de permisos de un usuario. */
    public static boolean guardarExcepcionesUsuario(int idUser, Map<Integer, Boolean> excepciones) {
        Connection con = null;
        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);
            try (PreparedStatement del = con.prepareStatement(
                    "delete from usuario_opciones where id_user=?")) {
                del.setInt(1, idUser);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(
                    "insert into usuario_opciones (id_user, id_opcion, concedido) values (?, ?, ?)")) {
                for (Map.Entry<Integer, Boolean> e : excepciones.entrySet()) {
                    ins.setInt(1, idUser);
                    ins.setInt(2, e.getKey());
                    ins.setBoolean(3, e.getValue());
                    ins.executeUpdate();
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }
            JOptionPane.showMessageDialog(null, "Error al guardar permisos del usuario:\n"
                    + e, "Error en la operación", JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
