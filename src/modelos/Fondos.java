/*
 * Modulo Caja: fondos de dinero (cajas fisicas o cuentas digitales).
 * Portado desde cajadiaria.
 */
package modelos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Monkeyelgrande
 */
public class Fondos {

    String nombre;
    int id, predeterminado, fisico_digital;

    public Fondos() {
    }

    public int getFisico_digital() {
        return fisico_digital;
    }

    public void setFisico_digital(int fisico_digital) {
        this.fisico_digital = fisico_digital;
    }

    public Fondos(int id, String nombre) {
        this.nombre = nombre;
        this.id = id;
    }

    public int getPredeterminado() {
        return predeterminado;
    }

    public void setPredeterminado(int predeterminado) {
        this.predeterminado = predeterminado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static void mostrarFondos(JComboBox<Fondos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from fondos order by predeterminado desc, nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Fondos(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    public static int TraerPredeterminado() {
        int id_fondo = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id from fondos where predeterminado =1");
            while (rs.next()) {

                id_fondo = rs.getInt("id");
            }
        } catch (Exception e) {
        }

        return id_fondo;
    }

    public static String TraerPredeterminadoNombre() {
        String name = "";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select nombre from fondos where predeterminado =1");
            while (rs.next()) {

                name = rs.getString("nombre");
            }
        } catch (Exception e) {
        }

        return name;
    }

    @Override
    public String toString() {
        return nombre;
    }

    public static int Traer_fondos_modelo_lista(JList jlist) {
        DefaultListModel modelo = new DefaultListModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from fondos order by nombre");
            while (rs.next()) {
                modelo.addElement(new Fondos(rs.getInt("id"), rs.getString("nombre")));
                jlist.setModel(modelo);
            }
        } catch (Exception e) {
        }
        return modelo.size();
    }
}
