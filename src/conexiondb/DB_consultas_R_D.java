/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conexiondb;

import Formularios.frm_main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Monkeyelgrande
 */
public class DB_consultas_R_D {

    public static String database_name;
    public static String ip;

    public static String url = "jdbc:postgresql://localhost:5432/" + database_name;
    public static String usuario = "postgres";
    public static String contrasenia = "monkey";

    public DB_consultas_R_D() {
        System.out.println("asdfasdfasd");
    }

    public static int TieneCreditosOCotizaciones(String id) {
        String cadena = "SELECT \n"
                + "    (\n"
                + "        (SELECT COUNT(f.id) FROM cotizaciones_cabeceras f WHERE f.id_contacto = " + id + ") +\n"
                + "        (SELECT COUNT(f.id) FROM facturas_cabeceras f    WHERE f.id_contacto = " + id + ")\n"
                + "    ) AS id;";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("id"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static boolean validar_admin() {
        int id_perfil = 0;
        String nombre = "";
        if (frm_main.perfil == 1) {
            return true;
        } else {

            String contrasena = "";
            // option2
            JPasswordField pf = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(null, pf, "Inicie como administrador:\n", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                contrasena = new String(pf.getPassword());
            }

            try {
                contrasena = DigestUtils.sha256Hex(contrasena);
            } catch (Exception e) {
                contrasena = "nulo";
            }
            String consulta = "select id_perfil, nombre from users where password ='" + contrasena + "' and id_perfil=1";
//            System.out.println(consulta);
            ResultSet rs = DB_consultas_R_D.getTabla(consulta);
            try {
                while (rs.next()) {
                    id_perfil = rs.getInt("id_perfil");
                    nombre = rs.getString("nombre");

                }
                rs.close();

            } catch (SQLException ex) {
                System.out.println("" + ex);
            }
            if (id_perfil == 1) {
                JOptionPane.showMessageDialog(null, "Ha obtenido permisos de administrador para ejecutar esta accion.\n"
                        + "Autorización otorgada por el administrador: " + nombre, "Acceso permitido", 2);
                return true;
            } else {
                JOptionPane.showMessageDialog(null, "AUTORIZACION DENEGADA", "Alerta de seguridad", 0);

            }
        }

        return false;
    }

    public static double consutla_entregas(String id) {

        ResultSet rs = getTabla("select count(id) from entregas_productos_cabecera where id_factura=" + id);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return (rs.getDouble("count"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static double SumarCampoDoubleConSQL(String consulta) {
        String cadena = consulta;
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return (rs.getDouble("sum"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static double ConsultarDouble(String tabla, String campo, String id) {
        String cadena = "select " + campo + " from " + tabla + " where id=" + id + ";";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return (rs.getDouble(campo));
            }
            rs.close();

        } catch (SQLException e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static String consulta_archivo_texto(String archivo) throws FileNotFoundException, IOException {
        String cadena;
        FileReader f = new FileReader(archivo);
        BufferedReader b = new BufferedReader(f);
        while ((cadena = b.readLine()) != null) {
            return cadena;
        }
        b.close();
        return null;
    }

    public static Connection getConexion() {

        try {
            database_name = consulta_archivo_texto(new File("").getAbsolutePath() + "/src/database_name.txt");
            ip = consulta_archivo_texto(new File("").getAbsolutePath() + "/src/ip.txt");
            // Timeouts JDBC para evitar que la UI se congele en equipos remotos:
            //   connectTimeout=10 : corta el intento de conexion TCP si la ruta al servidor esta
            //                       fria tras inactividad (NAT/firewall remoto). Evita el congelamiento.
            //   socketTimeout=30  : corta una consulta/lectura colgada.
            //   loginTimeout=10   : cubre el handshake/autenticacion de Postgres.
            //   tcpKeepAlive=true : mantiene viva una sesion existente.
            url = "jdbc:postgresql://" + ip + ":5432/" + database_name
                    + "?tcpKeepAlive=true&connectTimeout=10&socketTimeout=30&loginTimeout=10";

        } catch (Exception e) {
        }

        Connection cn = null;
        try {
            Class.forName("org.postgresql.Driver");
            cn = DriverManager.getConnection(url, usuario, contrasenia);
        } catch (Exception e) {
            System.out.println(String.valueOf(e));
            JOptionPane.showMessageDialog(null, "Error de conexxion a la base de datos:\n " + e);
        }
        return cn;
    }

    public static ResultSet getTabla(String Consulta) {
        Connection cn = getConexion();
        Statement st;
        ResultSet datos = null;
        try {
            st = cn.createStatement();
            datos = st.executeQuery(Consulta);
            cn.close();
        } catch (Exception e) {
            System.out.print(e.toString());
        }

        return datos;
    }

    public static boolean eliminar(String tabla, String id) {

        Connection con = null;
        String SSQL = "delete from " + tabla + " where id =  '" + id + "' ";
        try {
            con = getConexion();
            PreparedStatement psql = con.prepareStatement(SSQL);
            psql.executeUpdate();
            psql.close();
            con.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "No se puede elimiar: \n" + e);
            return false;
        }
        return true;
    }

    public static int consultarId(String id, String tabla) {
        String cadena = "select count(id) as id from " + tabla + " where id = " + id;
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("id"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static int TraerIdUser(String user_name) {
        String cadena = "select id  from users where user_name= '" + user_name + "'";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("id"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static String TraerIdCliente(String cliente) {
        String cadena = "select id  from contactos where nombre= '" + cliente + "'";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return rs.getString("id");
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return "";
        }
        return "";
    }

    public static int TraerIdPerfil(int id_user) {
        String cadena = "select id_perfil from users where id= " + id_user;
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("id_perfil"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static String TraerNombrePerfil(int id) {
        String cadena = "select perfil from perfiles where id= " + id;
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return (rs.getString("perfil"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return "";
        }
        return "";
    }

    public static String TraerIdMaximoNuevoContacto() {
        String cadena = "select max(id)+1 as id from contactos";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return rs.getString("id");
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return "";
        }
        return "";
    }

    public static int consultar_existencia_campo_String(String campo, String valor, String tabla) {
        String cadena = "select count(" + campo + ") as codigo from " + tabla + " where " + campo + " = '" + valor + "'";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("codigo"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static double consultar_stock(String codigo_barras) {

        String cadena = "SELECT COALESCE(SUM(sp.cantidad), 0) AS stock "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id "
                + "WHERE p.codigo_barras = '" + codigo_barras + "'";
        ResultSet rs = getTabla(cadena);
        try {
            if (rs.next()) {
                return rs.getDouble("stock");
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static double consultar_stock_x_bodega(String codigo_barras, int id_bodega) {
        String cadena = "SELECT COALESCE(sp.cantidad, 0) AS stock "
                + "FROM productos p "
                + "LEFT JOIN stock_productos sp ON sp.id_producto = p.id AND sp.id_bodega = " + id_bodega + " "
                + "WHERE p.codigo_barras = '" + codigo_barras + "' AND COALESCE(p.estado, true) = true";

        ResultSet rs = getTabla(cadena);
        try {
            if (rs.next()) {
                double stock = rs.getDouble("stock");
                rs.close();
                return stock;
            }
            rs.close();
        } catch (Exception e) {
            System.out.println("Error consultando stock: " + e);
        }
        return 0;
    }

    public static int Login(String user_name, String pass) {
        String cadena = "select * from users where user_name='" + user_name + "' and password='" + pass + "'";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return 1;
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static String cargarId(String tabla) {
        String resultado = "";
        ResultSet rs = DB_consultas_R_D.getTabla("select max(id)+1 as id from " + tabla);
        try {
            while (rs.next()) {
                resultado = (rs.getString("id"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        if (resultado == null) {
            resultado = "1";
        }
        return resultado;
    }

    public static void validar_numeros(java.awt.event.KeyEvent evt, char car) {
        if ((car < '0' || car > '9')) {
            if (car == '.') {
            } else {
                evt.consume();
            }
        }
    }

    public static double consultar_porcentajes(String campo) {
        String cadena = "select " + campo + " as campo from configuraciones";
        ResultSet rs = getTabla(cadena);
        try {
            while (rs.next()) {
                // añade los resultado a al modelo de tabla
                return Integer.parseInt(rs.getString("campo"));
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e);
            return 0;
        }
        return 0;
    }

    public static String obtener_fecha() {
        String fecha = "";
        Calendar calendario = new GregorianCalendar();
        int dia, mes, ano;

        dia = calendario.get(Calendar.DAY_OF_MONTH);
        mes = calendario.get(Calendar.MARCH) + 1;
        ano = calendario.get(Calendar.YEAR);

        fecha = ano + "-" + mes + "-" + dia;
        return fecha;
    }

    public static String obtener_fecha_dia1() {
        String fecha = "";
        Calendar calendario = new GregorianCalendar();
        int dia, mes, ano;

        dia = 1;
        mes = calendario.get(Calendar.MARCH) + 1;
        ano = calendario.get(Calendar.YEAR);

        fecha = ano + "-" + mes + "-" + dia;
        return fecha;
    }

    public static String obtener_fecha_dia_ultimo() {
        String fecha = "";
        Calendar calendario = new GregorianCalendar();
        int dia, mes, ano;

        dia = calendario.getActualMaximum(Calendar.DAY_OF_MONTH);
        mes = calendario.get(Calendar.MARCH) + 1;
        ano = calendario.get(Calendar.YEAR);

        fecha = ano + "-" + mes + "-" + dia;
        return fecha;
    }

    public static String obtener_hora() {
        String fecha = "";
        Calendar calendario = new GregorianCalendar();
        int hora, minutos, segundos;
        hora = calendario.get(Calendar.HOUR_OF_DAY);
        minutos = calendario.get(Calendar.MINUTE);
        segundos = calendario.get(Calendar.SECOND);

        fecha = hora + ":" + minutos + ":" + segundos;
        return fecha;
    }

    public static int traer_id_con_cod_barras(String codigo_bar) {
        int resultado = 0;
        ResultSet rs = DB_consultas_R_D.getTabla("select id from productos where codigo_barras='" + codigo_bar + "' AND COALESCE(estado, true) = true");
        try {
            while (rs.next()) {
                resultado = (rs.getInt("id"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return resultado;

    }

    public static String ImpresoraPredeterminada() {
        String resultado = "";
        ResultSet rs = DB_consultas_R_D.getTabla("select nombre_impresora from configuraciones");
        try {
            while (rs.next()) {
                resultado = (rs.getString("nombre_impresora"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return resultado;

    }

    public static int Imprimir_si_no() {
        int resultado = 0;
        ResultSet rs = DB_consultas_R_D.getTabla("select imprimir_factura from configuraciones");
        try {
            while (rs.next()) {
                resultado = (rs.getInt("imprimir_factura"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return resultado;

    }

    public static int Imprimir_Bodega_si_no(String bodega) {
        int resultado = 0;
        ResultSet rs = DB_consultas_R_D.getTabla("select imprime from bodegas where nombre='" + bodega + "'");
        try {
            while (rs.next()) {
                resultado = (rs.getInt("imprime"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return resultado;

    }

    public static int productos_repetidos() {
        int resultado = 0;
        ResultSet rs = DB_consultas_R_D.getTabla("select productos_repetidos from configuraciones");
        try {
            while (rs.next()) {
                resultado = (rs.getInt("productos_repetidos"));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return resultado;

    }

}
