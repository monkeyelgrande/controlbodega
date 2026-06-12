package Precios;

import java.util.List;
import javax.swing.JOptionPane;

/**
 * Seleccion del rol activo del modulo Precios cuando el usuario tiene varios
 * (usuario_roles_precios). Con un solo candidato no pregunta; con varios
 * muestra un dialogo "¿Con cual rol desea trabajar?".
 *
 * Roles: 2=Almacenista (cantidades), 3=Contable (costos), 4=Precios (venta).
 *
 * @author Monkeyelgrande
 */
public final class SelectorRolPrecios {

    public static final int ALMACENISTA = 2;
    public static final int CONTABLE = 3;
    public static final int PRECIOS = 4;

    private SelectorRolPrecios() {
    }

    public static String nombre(int rol) {
        switch (rol) {
            case ALMACENISTA:
                return "Almacenista";
            case CONTABLE:
                return "Contable";
            case PRECIOS:
                return "Precios";
            default:
                return "Sin rol";
        }
    }

    /**
     * Devuelve el rol elegido, o -1 si no hay candidatos o el usuario cancela.
     * Con un unico candidato lo devuelve sin preguntar (comportamiento igual
     * al de un usuario con un solo rol).
     */
    public static int elegir(List<Integer> candidatos, String mensaje) {
        if (candidatos == null || candidatos.isEmpty()) {
            return -1;
        }
        if (candidatos.size() == 1) {
            return candidatos.get(0);
        }
        String[] nombres = new String[candidatos.size()];
        for (int i = 0; i < candidatos.size(); i++) {
            nombres[i] = nombre(candidatos.get(i));
        }
        int sel = JOptionPane.showOptionDialog(null, mensaje, "Seleccione el rol",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, nombres, nombres[0]);
        return sel < 0 ? -1 : candidatos.get(sel);
    }
}
