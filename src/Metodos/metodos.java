/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Metodos;

import static Formularios.frm_main.cerra;
import static Formularios.frm_main.escritorio;
import com.ezware.oxbow.swingbits.table.filter.TableRowFilterSupport;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import org.jdesktop.swingx.JXFindBar;
import org.jdesktop.swingx.JXTable;

/**
 *
 * @author Monkeyelgrande
 */
public class metodos {

    /**
     * Redondea al multiplo de 10^digitos mas cercano (modulo Precios).
     */
    public static double redondearNumero(double numero, int digitos) {
        double factor = Math.pow(10, digitos);
        return Math.round(numero / factor) * factor;
    }

    /**
     * Instala en un JTextField el auto-formato de miles EN VIVO (a medida que
     * se escribe): separador de miles con punto y SIN centavos. Solo admite
     * digitos; cualquier otro caracter se ignora. El caret queda al final.
     *
     * Para leer el valor numerico usar:
     *   Double.parseDouble(metodos.EliminaCaracteres(campo.getText(), "."))
     * (igual que el resto de la app).
     *
     * @autor monkeyelgrande
     */
    public static void instalarFormatoMiles(final JTextField campo) {
        final DecimalFormatSymbols sim = new DecimalFormatSymbols();
        sim.setGroupingSeparator('.');
        final DecimalFormat fmt = new DecimalFormat("#,###", sim);

        ((javax.swing.text.AbstractDocument) campo.getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String str,
                    javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                reformatear(fb, off, 0, str, a);
            }

            @Override
            public void remove(FilterBypass fb, int off, int len)
                    throws javax.swing.text.BadLocationException {
                reformatear(fb, off, len, "", null);
            }

            @Override
            public void replace(FilterBypass fb, int off, int len, String str,
                    javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                reformatear(fb, off, len, str, a);
            }

            private void reformatear(FilterBypass fb, int off, int len, String str,
                    javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
                javax.swing.text.Document doc = fb.getDocument();
                String actual = doc.getText(0, doc.getLength());
                String nuevo = actual.substring(0, off)
                        + (str == null ? "" : str)
                        + actual.substring(off + len);
                String digitos = nuevo.replaceAll("[^0-9]", "");
                if (digitos.isEmpty()) {
                    fb.replace(0, doc.getLength(), "", a);
                    return;
                }
                if (digitos.length() > 15) {
                    digitos = digitos.substring(0, 15);
                }
                final String formateado = fmt.format(Long.parseLong(digitos));
                fb.replace(0, doc.getLength(), formateado, a);
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        campo.setCaretPosition(campo.getDocument().getLength());
                    }
                });
            }
        });
    }

    /** Copia un valor de dinero al portapapeles sin '$' ni separadores de miles. */
    public static void dinero_a_porta_papeles(String texto) {
        texto = EliminaCaracteres(texto, "$");
        texto = EliminaCaracteres(texto, ".");
        java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(texto);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }

    public static DecimalFormat formateador_dos_decimales() {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.');
        simbolos.setGroupingSeparator(',');
        return new DecimalFormat("#,##0.00", simbolos);
    }

    /** Al ganar foco un campo de dinero, quita los puntos de miles y selecciona todo. */
    public static void eliminar_puntos_focus_gained(JTextField jtext) {
        if (!jtext.getText().equals("")) {
            String texto = EliminaCaracteres(jtext.getText(), ".");
            jtext.setText(texto);
        }
        jtext.selectAll();
    }

    /** Al perder foco un campo de dinero, lo reformatea con separador de miles. */
    public static void formateo_dinero_en_jtextfield_fucus_lost(JTextField jtext) {
        if (!jtext.getText().equals("")) {
            double to = Double.parseDouble(jtext.getText());
            String nuevo = formateador_dinero().format(to);
            jtext.setText(nuevo);
        }
    }

    /**
     * Eliminar caracteres
     *
     * Este metodo recibe un primer campo tipo String con la cadena que desea eliminar los caracteres y un segundo parametro con el caracter que desea eliminar de toda la cadena
     *
     */
    public static void configurarCampoDinero(JFormattedTextField campo) {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setDecimalSeparator('.');
        s.setGroupingSeparator(',');

        DecimalFormat df = new DecimalFormat("#,##0.00", s);

        NumberFormatter nf = new NumberFormatter(df);
        nf.setValueClass(Double.class);
        nf.setAllowsInvalid(false);        // evita texto inválido
        nf.setMinimum(0.0);                // opcional: solo positivos
        nf.setCommitsOnValidEdit(true);    // actualiza el value al editar

        campo.setFormatterFactory(new DefaultFormatterFactory(nf));
        campo.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
    }

    public static DecimalFormat formateador_x_decimal(int cand_decim) {
        DecimalFormat formatea = null;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.'); // este es el de decimales
        simbolos.setGroupingSeparator(',');

        switch (cand_decim) {
            case 0:
                formatea = new DecimalFormat("######", simbolos);
                break;
            case 1:
                formatea = new DecimalFormat("######.#", simbolos);
                break;
            case 2:
                formatea = new DecimalFormat("######.##", simbolos);
                break;
            case 3:
                formatea = new DecimalFormat("######.###", simbolos);
                break;
            case 4:
                formatea = new DecimalFormat("######.####", simbolos);
                break;
            case 5:
                formatea = new DecimalFormat("######.#####", simbolos);
                break;
            case 6:
                formatea = new DecimalFormat("######.######", simbolos);
                break;
            case 7:
                formatea = new DecimalFormat("######.#######", simbolos);
                break;
            case 8:
                formatea = new DecimalFormat("######.########");
                break;
        }
        return formatea;
    }

    public static void EstiloTablaMaterialGlobal(JTable jtabla) {

        jtabla.getTableHeader().setReorderingAllowed(false);
        jtabla.getTableHeader().setDefaultRenderer(new EstiloTablasHeader());
        jtabla.setDefaultRenderer(Object.class, new EstiloTablasBody());
        jtabla.setRowHeight(30);

    }

    public static void BuscarEnTabla(JTextField txt_Filtro, JTable jtabla) {
        txt_Filtro.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }

            private void filterTable() {
                String searchText = txt_Filtro.getText().toLowerCase();

                DefaultTableModel model = (DefaultTableModel) jtabla.getModel();
                TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(model);
                jtabla.setRowSorter(rowSorter);

                RowFilter<DefaultTableModel, Object> rowFilter = RowFilter.regexFilter("(?i)" + searchText); // Ignora mayúsculas y minúsculas
                rowSorter.setRowFilter(rowFilter);
            }
        });
    }

    public static String EliminaCaracteres(String s_cadena, String s_caracteres) {
        String nueva_cadena = "";
        Character caracter = null;
        boolean valido = true;

        /* Va recorriendo la cadena s_cadena y copia a la cadena que va a regresar,
         sólo los caracteres que no estén en la cadena s_caracteres */
        for (int i = 0; i < s_cadena.length(); i++) {
            valido = true;
            for (int j = 0; j < s_caracteres.length(); j++) {
                caracter = s_caracteres.charAt(j);

                if (s_cadena.charAt(i) == caracter) {
                    valido = false;
                    break;
                }
            }
            if (valido) {
                nueva_cadena += s_cadena.charAt(i);
            }
        }

        return nueva_cadena;
    }

    public static boolean estacerrado(Object obj) {
        JInternalFrame[] activos = escritorio.getAllFrames();
        boolean cerrado = true;
        int i = 0;
        while (i < activos.length && cerrado) {
            if (activos[i] == obj) {
                cerrado = false;
                cerra = false;
            }
            i++;
        }
        return cerrado;
    }

    public static void addEscapeListenerWindowDialog(final JDialog windowDialog) {
        ActionListener escAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                windowDialog.dispose();
            }
        };
        windowDialog.getRootPane().registerKeyboardAction(escAction,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Evitar tabulado en JTextArea
     *
     * Este metodo evita que se tabule en los jtext area y en reemplazo cambia al siguente foco recibe como parametro el un elemento tipo JTextArea
     */
    public static void EvitarTabEnJTextArea(JTextArea area) {
        area.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (e.getModifiers() > 0) {
                        area.transferFocusBackward();
                    } else {
                        area.transferFocus();
                    }
                    e.consume();
                }
            }
        });
    }

    public static String ReemplazarCaracteres(String s_cadena, String s_caracter_a_reemplazar, String nuevo_caracter) {
        String nueva_cadena = "";

        nueva_cadena = s_cadena.replace(s_caracter_a_reemplazar, nuevo_caracter);

        return nueva_cadena;
    }

    /**
     *
     * @param carta
     * @param oficio
     * @return
     */
    public static String TamanoHoja(String carta, String oficio) {
        String cad = "";
        String[] botones = {"Carta", "Oficio"};
        ImageIcon icono = new ImageIcon("src/imagenes/page.png");
        int variable = JOptionPane.showOptionDialog(null, "Seleccione tamaño de impresión", "Tamaño hoja",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, icono, botones, null);
        if (variable >= 0) {

            if (variable == 0) {
                cad = new File("").getAbsolutePath() + carta; // opcion carta
            }
            if (variable == 1) {
                cad = new File("").getAbsolutePath() + oficio; // opcion oficio
            }
        }

        return cad;
    }

    public static DecimalFormat formateador_decimal() {
        DecimalFormat formatea = new DecimalFormat("###,###.##");
        return formatea;
    }

    public static DecimalFormat formateador_decimal_punto_para_decimal() {
        DecimalFormat formatea = null;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.'); // este es el de decimales esta bien
        simbolos.setGroupingSeparator(',');
        formatea = new DecimalFormat("###.###", simbolos); // se le retiro el .## para que salieran sin decimales
        return formatea;
    }

    public static DecimalFormat formateador_dinero() {
        DecimalFormat formatea = new DecimalFormat("###,###");
        return formatea;
    }

    public static DecimalFormat formateador_un_decimal() {
        DecimalFormat formatea = null;
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setDecimalSeparator('.'); // este es el de decimales esta bien
        simbolos.setGroupingSeparator(',');
        formatea = new DecimalFormat("###,###.0", simbolos); // se le retiro el .## para que salieran sin decimales
        return formatea;
    }

    /**
     * Este metodo permite que las tablas de tipo JXtable tengan la opcion del ctrl+f par busquedas y filtros de clic derecho sobre el nombre de las columnas Libreria swing-bits-0.5.0.jar
     *
     * @param tabla
     * @autor monkeyelgrande
     */
    public static void TablaAptaParaBusquedaAndSSM(JXTable tabla) {
        TableRowFilterSupport.forTable(tabla).searchable(true).apply();
        JXFindBar findBar = new JXFindBar(tabla.getSearchable());
        tabla.setSelectionModel(new ForcedListSelectionModel());
    }
}
