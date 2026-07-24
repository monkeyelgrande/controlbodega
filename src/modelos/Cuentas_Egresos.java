/*
 * Modulo Caja: cuentas (categorias) de egresos.
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
public class Cuentas_Egresos {

    String nombre;
    int id, predeterminado;

    public Cuentas_Egresos() {
    }

    public Cuentas_Egresos(int id, String nombre) {
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

    public static void mostrarCuentas(JComboBox<Cuentas_Egresos> jbox) {
        DefaultComboBoxModel modeloCombo = new DefaultComboBoxModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from cuentas_egresos order by nombre");
            while (rs.next()) {
                modeloCombo.addElement(new Cuentas_Egresos(rs.getInt("id"), rs.getString("nombre")));
                jbox.setModel(modeloCombo);
            }
        } catch (Exception e) {
        }
        AutoCompleteDecorator.decorate(jbox);
    }

    public static int Traer_cuentas_modelo_lista(JList jlist) {
        DefaultListModel modelo = new DefaultListModel();
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id,nombre from cuentas_egresos order by nombre");
            while (rs.next()) {
                modelo.addElement(new Cuentas_Egresos(rs.getInt("id"), rs.getString("nombre")));
                jlist.setModel(modelo);
            }
        } catch (Exception e) {
        }
        return modelo.size();
    }

    public static String TraerPredeterminadoNombre() {
        String name = "";
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select nombre from cuentas_egresos where predeterminado =1");
            while (rs.next()) {

                name = rs.getString("nombre");
            }
        } catch (Exception e) {
        }

        return name;
    }

    public static int TraerPredeterminadoID() {
        int name = 0;
        try {
            ResultSet rs = DB_consultas_R_D.getTabla("select id from cuentas_egresos where predeterminado =1");
            while (rs.next()) {

                name = rs.getInt("id");
            }
        } catch (Exception e) {
        }

        return name;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
