/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formularios;

import Login.login;
import Metodos.metodos;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import ReportesCodigo.jif_PrincipalReportes;
import conexiondb.Backup;
import conexiondb.DB_consultas_R_D;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author Monkeyelgrande
 */
public class frm_main extends javax.swing.JFrame {

    /**
     * Creates new form main
     */
    public static int id_bodega;
    public static int id_user;
    public static String bodega;
    public static int perfil;
    public static boolean cerra = false;
    frm_contactos frm_contacto = null;
    frm_productos frm_producto = null;
    frm_Recortes frm_recortes = null;
    frm_Recortes_pendientes frm_recortes_pendientes = null;
    frm_ingreso_mercancia frm_ingreso_mercancias = null;
    public static frm_Ordenes frm_factura = null;
    public static frm_facturas_ventas frm_factura_ventas = null;
    public static frm_cotizaciones frm_cotizaciones = null;
    public static frm_traslado_productos_entre_bodegas frm_mover = null;
    public static frm_facturas_anuladas frm_anulados = null;
    public static frm_Crear_Orden frm_facturacion = null;
    public static frm_facturacion_ventas frm_facturacion_venta = null;
    public static frm_cotizacion frm_cotizacion = null;
    public static frm_devoluciones jif_devolucion = null;
    public static String impresora_ticket = "";
    public static String nombre_usuario = "";
    // Auto-impresión por usuario
    public static boolean imprime_ordenes = false;
    public static String nombre_impresora_user = null;
    public static javax.print.PrintService print_service_user = null;
    public static boolean imp_ticket_bodega_asignada = false;
    public static boolean barra_notificaciones = false;
    public static Metodos.BarraNotificacionesPanel barraNotif = null;
    public static boolean panel_notificaciones = false;
    public static Metodos.PanelNotificacionesStock panelNotifStock = null;
    // Permiso para analizar el histórico y aprobar/rechazar órdenes de compra.
    public static boolean aprueba_compras = false;
    // Rol del módulo Precios (fusión productos-agroinsumos):
    // 0=sin acceso, 2=captura cantidades, 3=costos, 4=precios.
    // Es el rol ACTIVO: cuando el usuario tiene varios (roles_precios), el
    // selector de rol lo asigna por operación.
    public static int rol_precios = 0;
    // Roles del módulo Precios del usuario (usuario_roles_precios): un usuario
    // puede tener varios (almacenista/contable/precios).
    public static java.util.List<Integer> roles_precios = new java.util.ArrayList<>();
    frm_ordenes_compra frm_orden_compra = null;
    Precios.frm_ingresos_precios frm_ingresos_precios = null;
    Precios.frm_precios_productos frm_precios_productos = null;
    Precios.frm_descuentos_precios frm_descuentos_precios = null;
    private javax.swing.JMenu menuPrecios = null;
    private javax.swing.JMenuItem itemPermisos = null;
    // Entradas del menú Precios, gobernables por el sistema de permisos
    private javax.swing.JMenuItem itemIngresosPrecios = null;
    private javax.swing.JMenuItem itemPreciosProductos = null;
    private javax.swing.JMenuItem itemDescuentosPrecios = null;
    private javax.swing.JMenuItem itemEtiquetasPrecios = null;
    private javax.swing.JMenu menuReportesPrecios = null;
    private javax.swing.JMenuItem itemConfigPrecios = null;

    jif_users jif_user = null;
    jif_PrincipalReportes jif_reportes = null;
    Backup backup = null;
    static String titulo;
    frm_tipo_ingresos frm_tipo_ingresos = null;
    frm_unidades frm_unidad = null;
    frm_bodegas frm_bodega = null;
    frm_ajuste_inventario frm_ajuste_inv = null;
    public static String tipo_factura = "";
  
    DecimalFormat formatea = new DecimalFormat("###,###.##");

    public frm_main() {
        initComponents();
        this.setLocationRelativeTo(null);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        try {
            consulta_database_name(new File("").getAbsolutePath() + "/src/database_name.txt");
        } catch (IOException ex) {
            Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setTitle("Bodega - " + titulo);

        montarMenuOrdenesCompra();
        montarMenuPrecios();
        montarMenuPermisos();

        // Detener el listener de auto-impresión al cerrar la app
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Metodos.AutoImpresionOrdenesService.getInstance().detener();
            } catch (Exception ignored) {
            }
            try {
                Metodos.NotificacionesService.getInstance().detener();
            } catch (Exception ignored) {
            }
        }));
    }

    /**
     * Inserta la barra lateral de notificaciones a la derecha del jDesktopPane
     * cuando el usuario logueado tiene barra_notificaciones=true.
     * Llamar despues del login (cuando ya se conoce el flag).
     */
    public void montarBarraNotificaciones() {
        if (!barra_notificaciones) {
            return;
        }
        java.awt.Container cp = getContentPane();
        // Quitar layout actual y reorganizar: escritorio CENTER, barra EAST
        cp.removeAll();
        cp.setLayout(new java.awt.BorderLayout());
        cp.add(escritorio, java.awt.BorderLayout.CENTER);

        int anchoPantalla = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        int anchoBarra = Math.max(280, Math.min(380, anchoPantalla / 5));

        barraNotif = new Metodos.BarraNotificacionesPanel(anchoBarra);
        cp.add(barraNotif, java.awt.BorderLayout.EAST);
        cp.revalidate();
        cp.repaint();

        Metodos.NotificacionesService.getInstance().iniciar();
    }

    /**
     * Muestra dentro del escritorio (JDesktopPane) un panel con tarjetas de
     * productos agotados o en negativo, cuando el usuario logueado tiene
     * panel_notificaciones=true. Llamar despues del login.
     */
    public void montarPanelNotificaciones() {
        if (!panel_notificaciones) {
            return;
        }
        if (panelNotifStock != null && !panelNotifStock.isClosed()) {
            panelNotifStock.toFront();
            return;
        }
        panelNotifStock = new Metodos.PanelNotificacionesStock();
        escritorio.add(panelNotifStock, javax.swing.JLayeredPane.PALETTE_LAYER);
        panelNotifStock.setVisible(true);

        // El posicionamiento se difiere: al montarse desde el login el escritorio
        // aun no tiene ancho real (frm_main no se ha mostrado). invokeLater corre
        // despues de show(), cuando ya hay bounds; si aun no, cae al tamano de pantalla.
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                int ancho = panelNotifStock.getWidth();
                int dispo = escritorio.getWidth() > 0
                        ? escritorio.getWidth()
                        : java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
                int x = Math.max(0, dispo - ancho - 24);
                panelNotifStock.setLocation(x, 24);
                panelNotifStock.toFront();
            }
        });

        panelNotifStock.iniciar();
    }

    /**
     * Agrega al menú principal la entrada de Órdenes de compra. Se construye
     * programáticamente (fuera de initComponents) para no tocar el código
     * generado por el editor de formularios. El menú es visible para todos los
     * perfiles; el permiso de análisis/aprobación se controla dentro del módulo.
     */
    private void montarMenuOrdenesCompra() {
        javax.swing.JMenu menu = new javax.swing.JMenu("Compras");
        javax.swing.JMenuItem item = new javax.swing.JMenuItem("Órdenes de compra");
        item.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirOrdenesCompra();
            }
        });
        menu.add(item);
        jMenuBar1.add(menu);
        jMenuBar1.revalidate();
        jMenuBar1.repaint();
    }

    /**
     * Agrega el menú "Precios" (módulo fusionado de productos-agroinsumos).
     * Se construye oculto; la visibilidad se decide tras el login con
     * {@link #actualizarMenuPrecios()} según users.rol_precios (admin = acceso
     * total).
     */
    private void montarMenuPrecios() {
        menuPrecios = new javax.swing.JMenu("Precios");

        itemIngresosPrecios = new javax.swing.JMenuItem("Ingresos de productos");
        itemIngresosPrecios.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirIngresosPrecios();
            }
        });
        menuPrecios.add(itemIngresosPrecios);

        itemPreciosProductos = new javax.swing.JMenuItem("Precios de productos");
        itemPreciosProductos.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirPreciosProductos();
            }
        });
        menuPrecios.add(itemPreciosProductos);

        itemDescuentosPrecios = new javax.swing.JMenuItem("Descuentos escalonados");
        itemDescuentosPrecios.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                abrirDescuentosPrecios();
            }
        });
        menuPrecios.add(itemDescuentosPrecios);

        itemEtiquetasPrecios = new javax.swing.JMenuItem("Imprimir etiquetas");
        itemEtiquetasPrecios.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Precios.jd_productos_a_imprimir etiquetas = new Precios.jd_productos_a_imprimir(null, false);
                etiquetas.setVisible(true);
            }
        });
        menuPrecios.add(itemEtiquetasPrecios);

        javax.swing.JMenu menuReportes = new javax.swing.JMenu("Reportes");
        menuReportesPrecios = menuReportes;

        javax.swing.JMenuItem itemRepDiario = new javax.swing.JMenuItem("Ingresos del día");
        itemRepDiario.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Precios.jd_reporte_ingresos_diario rep = new Precios.jd_reporte_ingresos_diario(null, false);
                rep.setVisible(true);
            }
        });
        menuReportes.add(itemRepDiario);

        javax.swing.JMenuItem itemRepXFactura = new javax.swing.JMenuItem("Entre fechas (por factura)");
        itemRepXFactura.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Precios.jd_reporte_ingresos_x_factura rep = new Precios.jd_reporte_ingresos_x_factura(null, false);
                rep.setVisible(true);
            }
        });
        menuReportes.add(itemRepXFactura);

        javax.swing.JMenuItem itemRepXUsuario = new javax.swing.JMenuItem("Entre fechas (por usuario)");
        itemRepXUsuario.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Precios.jd_reporte_ingresos_x_producto rep = new Precios.jd_reporte_ingresos_x_producto(null, false);
                rep.setVisible(true);
            }
        });
        menuReportes.add(itemRepXUsuario);

        menuPrecios.add(menuReportes);

        itemConfigPrecios = new javax.swing.JMenuItem("Configuración de precios");
        itemConfigPrecios.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Precios.jd_config_precios config = new Precios.jd_config_precios(null, true);
                config.setVisible(true);
            }
        });
        menuPrecios.add(itemConfigPrecios);

        menuPrecios.setVisible(false);
        jMenuBar1.add(menuPrecios);
        jMenuBar1.revalidate();
        jMenuBar1.repaint();
    }

    /** Llamar tras el login, cuando ya se conocen perfil y rol_precios. */
    public void actualizarMenuPrecios() {
        if (menuPrecios != null) {
            menuPrecios.setVisible(rol_precios > 0 || perfil == 1);
            jMenuBar1.revalidate();
            jMenuBar1.repaint();
        }
    }

    /**
     * Agrega al menú de administración la entrada de la pantalla de permisos.
     * Se construye oculta; la visibilidad la decide permisos() con la opción
     * "jmenu_permisos" (por defecto solo Admin).
     */
    private void montarMenuPermisos() {
        itemPermisos = new javax.swing.JMenuItem("Permisos de la aplicación");
        itemPermisos.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Formularios_internos.jif_permisos dlg = new Formularios_internos.jif_permisos();
                dlg.setVisible(true);
            }
        });
        itemPermisos.setVisible(false);
        jmenu_admin.add(itemPermisos);
    }

    private void abrirIngresosPrecios() {
        if (metodos.estacerrado(frm_ingresos_precios)) {
            frm_ingresos_precios = new Precios.frm_ingresos_precios();
            escritorio.add(frm_ingresos_precios);
            try {
                frm_ingresos_precios.setMaximum(true);
            } catch (java.beans.PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_ingresos_precios.show();
        } else {
            frm_ingresos_precios.toFront();
        }
    }

    private void abrirPreciosProductos() {
        if (metodos.estacerrado(frm_precios_productos)) {
            frm_precios_productos = new Precios.frm_precios_productos();
            escritorio.add(frm_precios_productos);
            try {
                frm_precios_productos.setMaximum(true);
            } catch (java.beans.PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_precios_productos.show();
        } else {
            frm_precios_productos.toFront();
        }
    }

    private void abrirDescuentosPrecios() {
        if (metodos.estacerrado(frm_descuentos_precios)) {
            frm_descuentos_precios = new Precios.frm_descuentos_precios();
            escritorio.add(frm_descuentos_precios);
            frm_descuentos_precios.show();
        } else {
            frm_descuentos_precios.toFront();
        }
    }

    private void abrirOrdenesCompra() {
        if (metodos.estacerrado(frm_orden_compra)) {
            frm_orden_compra = new frm_ordenes_compra();
            escritorio.add(frm_orden_compra);
            try {
                frm_orden_compra.setMaximum(true);
            } catch (java.beans.PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_orden_compra.show();
        } else {
            frm_orden_compra.toFront();
        }
    }

    private void abrirAjusteInventario() {
        if (metodos.estacerrado(frm_ajuste_inv)) {
            frm_ajuste_inv = new frm_ajuste_inventario();
            escritorio.add(frm_ajuste_inv);
            try {
                frm_ajuste_inv.setMaximum(true);
            } catch (java.beans.PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_ajuste_inv.show();
        } else {
            frm_ajuste_inv.toFront();
        }
    }

    public static void consulta_database_name(String archivo) throws FileNotFoundException, IOException {
        String cadena;
        FileReader f = new FileReader(archivo);
        BufferedReader b = new BufferedReader(f);
        while ((cadena = b.readLine()) != null) {
//            System.out.println(cadena);
            titulo = cadena;
        }
        b.close();
    }

    /**
     * Aplica la visibilidad de menús y botones según los permisos efectivos
     * del usuario (tablas opciones/perfil_opciones/usuario_opciones, cargadas
     * en el login con Permisos.cargar). Si la carga falló — BD sin la
     * migración de permisos o sin conexión — cae al switch por perfil
     * anterior para no dejar a nadie sin sus accesos.
     */
    public void permisos() {
        if (!Metodos.Permisos.estaCargado()) {
            permisosLegacy();
            return;
        }
        for (java.util.Map.Entry<String, javax.swing.JComponent> e : registroComponentes().entrySet()) {
            e.getValue().setVisible(Metodos.Permisos.puede(e.getKey()));
        }
        if (itemPermisos != null) {
            itemPermisos.setVisible(Metodos.Permisos.puede("jmenu_permisos"));
        }
    }

    /**
     * Registro clave de opción → componente gobernado. Las claves coinciden
     * con opciones.clave (sembradas en sql/migracion_permisos.sql).
     */
    private java.util.Map<String, javax.swing.JComponent> registroComponentes() {
        java.util.LinkedHashMap<String, javax.swing.JComponent> m = new java.util.LinkedHashMap<>();
        m.put("jmenu_configuraciones", jmenu_configuraciones);
        m.put("jmenu_user", jmenu_user);
        m.put("jmenu_backup", jmenu_backup);
        m.put("jMenu_tipo_ingreso", jMenu_tipo_ingreso);
        m.put("jmenu_bodegas", jmenu_bodegas);
        m.put("jMenu_unidades", jMenu_unidades);
        m.put("jmenu_con", jmenu_con);
        m.put("jmenu_contactos", jmenu_contactos);
        m.put("btn_contactos", btn_contactos);
        m.put("jMenu_productos_principal", jMenu_productos_principal);
        m.put("btn_productos", btn_productos);
        m.put("btn_ingreso_productos", btn_ingreso_productos);
        m.put("jMenu_ordenes", jMenu_ordenes);
        m.put("jmenu_facturacion", jmenu_facturacion);
        m.put("btn_generar_orden", btn_generar_orden);
        m.put("btn_ver_ordenes", btn_ver_ordenes);
        m.put("btn_facturar", btn_facturar);
        m.put("btn_ver_facturas", btn_ver_facturas);
        m.put("btn_decolucion", btn_decolucion);
        m.put("jMenu_recortes", jMenu_recortes);
        m.put("btn_generar_recorte", btn_generar_recorte);
        m.put("btn_ver_recortes", btn_ver_recortes);
        // Entradas del menú Precios: la visibilidad del menú completo la dan
        // los roles (actualizarMenuPrecios); estas opciones afinan qué
        // entradas ve cada perfil/usuario dentro del menú.
        m.put("menu_precios_ingresos", itemIngresosPrecios);
        m.put("menu_precios_productos", itemPreciosProductos);
        m.put("menu_precios_descuentos", itemDescuentosPrecios);
        m.put("menu_precios_etiquetas", itemEtiquetasPrecios);
        m.put("menu_precios_reportes", menuReportesPrecios);
        m.put("menu_precios_config", itemConfigPrecios);
        return m;
    }

    /** Switch por perfil anterior. Solo se usa si la BD no tiene la migración de permisos. */
    private void permisosLegacy() {
        if (itemPermisos != null) {
            itemPermisos.setVisible(perfil == 1);
        }
        switch (perfil) {
            case 1:

                break;
            case 2:
                jmenu_configuraciones.setVisible(false);
                jmenu_user.setVisible(false);
                jmenu_contactos.setVisible(false);
                jmenu_con.setVisible(false);
                jMenu_productos_principal.setVisible(false);
                jmenu_backup.setVisible(false);
                jmenu_facturacion.setVisible(false);
                jMenu_tipo_ingreso.setVisible(false);
                jmenu_bodegas.setVisible(false);
                jMenu_unidades.setVisible(false);

                btn_contactos.setVisible(false);
                btn_productos.setVisible(false);
                btn_ingreso_productos.setVisible(false);
//                btn_generar_orden.setVisible(false);
                btn_generar_recorte.setVisible(false);

                btn_facturar.setVisible(false);
                btn_ver_facturas.setVisible(false);
                btn_decolucion.setVisible(false);

                break;
            case 3:
                jmenu_configuraciones.setVisible(false);
                jmenu_user.setVisible(false);
                jmenu_contactos.setVisible(false);
                jmenu_con.setVisible(false);
                jMenu_productos_principal.setVisible(false);
                jmenu_backup.setVisible(false);
//                jmenu_facturacion.setVisible(false);
                jMenu_tipo_ingreso.setVisible(false);
                jmenu_bodegas.setVisible(false);
                jMenu_unidades.setVisible(false);

                btn_contactos.setVisible(false);
                btn_productos.setVisible(false);
                btn_ingreso_productos.setVisible(false);
//                btn_generar_orden.setVisible(false);
//                btn_generar_recorte.setVisible(false);

                btn_facturar.setVisible(false);
                btn_ver_facturas.setVisible(false);
                btn_decolucion.setVisible(false);

                break;
            case 4:
                jmenu_configuraciones.setVisible(false);
                jmenu_user.setVisible(false);
                jmenu_contactos.setVisible(false);
                jmenu_con.setVisible(false);
                jMenu_productos_principal.setVisible(false);
                jmenu_backup.setVisible(false);
                jmenu_facturacion.setVisible(false);
                jMenu_tipo_ingreso.setVisible(false);
                jmenu_bodegas.setVisible(false);
                jMenu_unidades.setVisible(false);
                jMenu_recortes.setVisible(false);
                jMenu_ordenes.setVisible(false);

                btn_contactos.setVisible(false);
                btn_productos.setVisible(false);
                btn_ingreso_productos.setVisible(false);
                btn_generar_orden.setVisible(false);
                btn_generar_recorte.setVisible(false);
                btn_ver_ordenes.setVisible(false);
                btn_ver_recortes.setVisible(false);
//                
//                btn_facturar.setVisible(false);
//                btn_ver_facturas.setVisible(false);
//                btn_decolucion.setVisible(false);

                break;
            case 5:
                jmenu_configuraciones.setVisible(false);
                jmenu_user.setVisible(false);
                jmenu_contactos.setVisible(false);
                jmenu_con.setVisible(false);
//                jMenu_productos_principal.setVisible(false);
                jmenu_backup.setVisible(false);
//                jmenu_facturacion.setVisible(false);
                jMenu_tipo_ingreso.setVisible(false);
                jmenu_bodegas.setVisible(false);
                jMenu_unidades.setVisible(false);

                btn_contactos.setVisible(false);
//                btn_productos.setVisible(false);
                btn_ingreso_productos.setVisible(false);
//                btn_generar_orden.setVisible(false);
//                btn_generar_recorte.setVisible(false);

                btn_facturar.setVisible(false);
                btn_ver_facturas.setVisible(false);
                btn_decolucion.setVisible(false);

                break;
        }

    }
  

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        escritorio = new javax.swing.JDesktopPane();
        lbl_user = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        btn_facturar = new javax.swing.JButton();
        btn_ver_facturas = new javax.swing.JButton();
        btn_decolucion = new javax.swing.JButton();
        lbl_perfil = new javax.swing.JLabel();
        btn_contactos = new javax.swing.JButton();
        btn_productos = new javax.swing.JButton();
        btn_ingreso_productos = new javax.swing.JButton();
        btn_generar_orden = new javax.swing.JButton();
        btn_ver_ordenes = new javax.swing.JButton();
        btn_generar_recorte = new javax.swing.JButton();
        btn_ver_recortes = new javax.swing.JButton();
        btn_ingreso_productos1 = new javax.swing.JButton();
        btn_cotizacion = new javax.swing.JButton();
        btn_ver_cotizaciones = new javax.swing.JButton();
        lbl_bodega_user = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jmenu_con = new javax.swing.JMenu();
        jmenu_contactos = new javax.swing.JMenuItem();
        jMenu_productos_principal = new javax.swing.JMenu();
        jmenu_productos = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jmenu_mover_productos = new javax.swing.JMenuItem();
        jMenu_verificar_inventario = new javax.swing.JMenuItem();
        jMenu_ordenes = new javax.swing.JMenu();
        jmenu_facturacion = new javax.swing.JMenuItem();
        jmenu_ver_factura = new javax.swing.JMenuItem();
        jmenu_ver_anulados = new javax.swing.JMenuItem();
        jMenu_recortes = new javax.swing.JMenu();
        jmenu_reportes1 = new javax.swing.JMenuItem();
        jmenu_reportes2 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jmenu_reportes = new javax.swing.JMenuItem();
        jmenu_admin = new javax.swing.JMenu();
        jmenu_user = new javax.swing.JMenuItem();
        jmenu_configuraciones = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jmenu_backup = new javax.swing.JMenuItem();
        jMenu_tipo_ingreso = new javax.swing.JMenuItem();
        jMenu_unidades = new javax.swing.JMenuItem();
        jmenu_bodegas = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jmenu_calculadora_retenciones = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ContaMonkey");
        setIconImage(getIconImage());
        setMinimumSize(new java.awt.Dimension(1340, 800));
        setSize(new java.awt.Dimension(0, 0));

        escritorio.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        lbl_user.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_user.setForeground(new java.awt.Color(255, 255, 255));
        lbl_user.setText("usuario");

        btn_facturar.setBackground(new java.awt.Color(244, 67, 54));
        btn_facturar.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_facturar.setForeground(new java.awt.Color(255, 255, 255));
        btn_facturar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Purchase Order_2_1.png"))); // NOI18N
        btn_facturar.setText("Ordenes");
        btn_facturar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn_facturarMouseEntered(evt);
            }
        });
        btn_facturar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_facturarActionPerformed(evt);
            }
        });

        btn_ver_facturas.setBackground(new java.awt.Color(33, 33, 33));
        btn_ver_facturas.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_ver_facturas.setForeground(new java.awt.Color(255, 255, 255));
        btn_ver_facturas.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/View.png"))); // NOI18N
        btn_ver_facturas.setText("Ver Ordenes");
        btn_ver_facturas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn_ver_facturasMouseEntered(evt);
            }
        });
        btn_ver_facturas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_facturasActionPerformed(evt);
            }
        });

        btn_decolucion.setBackground(new java.awt.Color(255, 193, 7));
        btn_decolucion.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_decolucion.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Back.png"))); // NOI18N
        btn_decolucion.setText("Devolución");
        btn_decolucion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn_decolucionMouseEntered(evt);
            }
        });
        btn_decolucion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_decolucionActionPerformed(evt);
            }
        });

        lbl_perfil.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_perfil.setForeground(new java.awt.Color(255, 255, 255));
        lbl_perfil.setText("perfil");

        btn_contactos.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_contactos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Contact_2.png"))); // NOI18N
        btn_contactos.setText("Contactos");
        btn_contactos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_contactosActionPerformed(evt);
            }
        });

        btn_productos.setBackground(new java.awt.Color(0, 153, 153));
        btn_productos.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_productos.setForeground(new java.awt.Color(255, 255, 255));
        btn_productos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Product.png"))); // NOI18N
        btn_productos.setText("Productos");
        btn_productos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_productosActionPerformed(evt);
            }
        });

        btn_ingreso_productos.setBackground(new java.awt.Color(0, 153, 153));
        btn_ingreso_productos.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_ingreso_productos.setForeground(new java.awt.Color(255, 255, 255));
        btn_ingreso_productos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Buy.png"))); // NOI18N
        btn_ingreso_productos.setText("Ingreso Productos");
        btn_ingreso_productos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ingreso_productosActionPerformed(evt);
            }
        });

        btn_generar_orden.setBackground(new java.awt.Color(153, 0, 0));
        btn_generar_orden.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_generar_orden.setForeground(new java.awt.Color(255, 255, 255));
        btn_generar_orden.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Purchase Order_1.png"))); // NOI18N
        btn_generar_orden.setText("Generar Orden");
        btn_generar_orden.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_generar_ordenActionPerformed(evt);
            }
        });

        btn_ver_ordenes.setBackground(new java.awt.Color(153, 0, 0));
        btn_ver_ordenes.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_ver_ordenes.setForeground(new java.awt.Color(255, 255, 255));
        btn_ver_ordenes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/View Delivery.png"))); // NOI18N
        btn_ver_ordenes.setText("Ver Ordenes");
        btn_ver_ordenes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_ordenesActionPerformed(evt);
            }
        });

        btn_generar_recorte.setBackground(new java.awt.Color(153, 0, 0));
        btn_generar_recorte.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_generar_recorte.setForeground(new java.awt.Color(255, 255, 255));
        btn_generar_recorte.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Cut Paper.png"))); // NOI18N
        btn_generar_recorte.setText("Generar Recorte");
        btn_generar_recorte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_generar_recorteActionPerformed(evt);
            }
        });

        btn_ver_recortes.setBackground(new java.awt.Color(153, 0, 0));
        btn_ver_recortes.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_ver_recortes.setForeground(new java.awt.Color(255, 255, 255));
        btn_ver_recortes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Look For.png"))); // NOI18N
        btn_ver_recortes.setText("Ver Recortes");
        btn_ver_recortes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_recortesActionPerformed(evt);
            }
        });

        btn_ingreso_productos1.setBackground(new java.awt.Color(0, 153, 153));
        btn_ingreso_productos1.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_ingreso_productos1.setForeground(new java.awt.Color(255, 255, 255));
        btn_ingreso_productos1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Search.png"))); // NOI18N
        btn_ingreso_productos1.setText("Consultar");
        btn_ingreso_productos1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ingreso_productos1ActionPerformed(evt);
            }
        });

        btn_cotizacion.setBackground(new java.awt.Color(244, 67, 54));
        btn_cotizacion.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_cotizacion.setForeground(new java.awt.Color(255, 255, 255));
        btn_cotizacion.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Bill.png"))); // NOI18N
        btn_cotizacion.setText("Cotización");
        btn_cotizacion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn_cotizacionMouseEntered(evt);
            }
        });
        btn_cotizacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_cotizacionActionPerformed(evt);
            }
        });

        btn_ver_cotizaciones.setBackground(new java.awt.Color(33, 33, 33));
        btn_ver_cotizaciones.setFont(new java.awt.Font("Yu Gothic Medium", 1, 18)); // NOI18N
        btn_ver_cotizaciones.setForeground(new java.awt.Color(255, 255, 255));
        btn_ver_cotizaciones.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/View.png"))); // NOI18N
        btn_ver_cotizaciones.setText("Ver Cotizaciones");
        btn_ver_cotizaciones.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn_ver_cotizacionesMouseEntered(evt);
            }
        });
        btn_ver_cotizaciones.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_ver_cotizacionesActionPerformed(evt);
            }
        });

        lbl_bodega_user.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        lbl_bodega_user.setForeground(new java.awt.Color(255, 255, 255));
        lbl_bodega_user.setText("bodega");

        jButton1.setText("rev");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        escritorio.setLayer(lbl_user, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(jSeparator1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_facturar, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_ver_facturas, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_decolucion, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(lbl_perfil, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_contactos, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_productos, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_ingreso_productos, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_generar_orden, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_ver_ordenes, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_generar_recorte, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_ver_recortes, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_ingreso_productos1, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_cotizacion, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(btn_ver_cotizaciones, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(lbl_bodega_user, javax.swing.JLayeredPane.DEFAULT_LAYER);
        escritorio.setLayer(jButton1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout escritorioLayout = new javax.swing.GroupLayout(escritorio);
        escritorio.setLayout(escritorioLayout);
        escritorioLayout.setHorizontalGroup(
            escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(escritorioLayout.createSequentialGroup()
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(escritorioLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btn_cotizacion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_facturar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_decolucion, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btn_ver_cotizaciones, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btn_ver_facturas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(escritorioLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jSeparator1)))
                .addGap(559, 559, 559))
            .addGroup(escritorioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_user)
                    .addComponent(btn_contactos, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btn_ingreso_productos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_productos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_ingreso_productos1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lbl_perfil)
                    .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_bodega_user)
                    .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btn_generar_orden, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_ver_ordenes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_generar_recorte, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btn_ver_recortes, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(559, Short.MAX_VALUE))
        );
        escritorioLayout.setVerticalGroup(
            escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(escritorioLayout.createSequentialGroup()
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbl_user)
                    .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lbl_perfil)
                        .addComponent(lbl_bodega_user)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btn_contactos)
                    .addGroup(escritorioLayout.createSequentialGroup()
                        .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_productos)
                            .addComponent(btn_generar_orden))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_ingreso_productos)
                            .addComponent(btn_ver_ordenes))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btn_generar_recorte)
                            .addComponent(btn_ingreso_productos1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btn_ver_recortes)
                            .addComponent(jButton1))))
                .addGap(19, 19, 19)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_facturar)
                    .addComponent(btn_ver_facturas))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn_decolucion)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(escritorioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_cotizacion)
                    .addComponent(btn_ver_cotizaciones))
                .addGap(0, 131, Short.MAX_VALUE))
        );

        jMenuBar1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N

        jmenu_con.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/contacto_peq.png"))); // NOI18N
        jmenu_con.setText("Contactos");
        jmenu_con.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_contactos.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_contactos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/contacto_peq.png"))); // NOI18N
        jmenu_contactos.setText("Contactos");
        jmenu_contactos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_contactosActionPerformed(evt);
            }
        });
        jmenu_con.add(jmenu_contactos);

        jMenuBar1.add(jmenu_con);

        jMenu_productos_principal.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/productos_pequeno.png"))); // NOI18N
        jMenu_productos_principal.setText("Productos");
        jMenu_productos_principal.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_productos.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        jmenu_productos.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_productos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/productos_pequeno.png"))); // NOI18N
        jmenu_productos.setText("Productos");
        jmenu_productos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_productosActionPerformed(evt);
            }
        });
        jMenu_productos_principal.add(jmenu_productos);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
        jMenuItem2.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jMenuItem2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/ingreso_productos.png"))); // NOI18N
        jMenuItem2.setText("Ingreso productos");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu_productos_principal.add(jMenuItem2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0));
        jMenuItem3.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jMenuItem3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/database_peq.png"))); // NOI18N
        jMenuItem3.setText("Consulta");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu_productos_principal.add(jMenuItem3);

        jmenu_mover_productos.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_mover_productos.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Sorting Arrows Horizontal.png"))); // NOI18N
        jmenu_mover_productos.setText("Traslado productos entre bodegas");
        jmenu_mover_productos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_mover_productosActionPerformed(evt);
            }
        });
        jMenu_productos_principal.add(jmenu_mover_productos);

        jMenu_verificar_inventario.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jMenu_verificar_inventario.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Adjust.png"))); // NOI18N
        jMenu_verificar_inventario.setText("Ajustar inventario");
        jMenu_verificar_inventario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_verificar_inventarioActionPerformed(evt);
            }
        });
        jMenu_productos_principal.add(jMenu_verificar_inventario);

        jMenuBar1.add(jMenu_productos_principal);

        jMenu_ordenes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Purchase Order_2.png"))); // NOI18N
        jMenu_ordenes.setText("Ordenes");
        jMenu_ordenes.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_facturacion.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        jmenu_facturacion.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_facturacion.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/ventas.png"))); // NOI18N
        jmenu_facturacion.setText("Generar Orden");
        jmenu_facturacion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_facturacionActionPerformed(evt);
            }
        });
        jMenu_ordenes.add(jmenu_facturacion);

        jmenu_ver_factura.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        jmenu_ver_factura.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_ver_factura.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/familias.png"))); // NOI18N
        jmenu_ver_factura.setText("Ordenes");
        jmenu_ver_factura.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_ver_facturaActionPerformed(evt);
            }
        });
        jMenu_ordenes.add(jmenu_ver_factura);

        jmenu_ver_anulados.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_ver_anulados.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/shutdown.png"))); // NOI18N
        jmenu_ver_anulados.setText("Ordenes anuladas");
        jmenu_ver_anulados.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_ver_anuladosActionPerformed(evt);
            }
        });
        jMenu_ordenes.add(jmenu_ver_anulados);

        jMenuBar1.add(jMenu_ordenes);

        jMenu_recortes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/WaterPipe24.png"))); // NOI18N
        jMenu_recortes.setText("Recortes");
        jMenu_recortes.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_reportes1.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_reportes1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/WaterPipe24.png"))); // NOI18N
        jmenu_reportes1.setText("Recortes");
        jmenu_reportes1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_reportes1ActionPerformed(evt);
            }
        });
        jMenu_recortes.add(jmenu_reportes1);

        jmenu_reportes2.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_reportes2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/database_peq.png"))); // NOI18N
        jmenu_reportes2.setText("Recortes pendientes");
        jmenu_reportes2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_reportes2ActionPerformed(evt);
            }
        });
        jMenu_recortes.add(jmenu_reportes2);

        jMenuBar1.add(jMenu_recortes);

        jMenu6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/estadisticas.png"))); // NOI18N
        jMenu6.setText("Reportes");
        jMenu6.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_reportes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F9, 0));
        jmenu_reportes.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_reportes.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/estadisticas.png"))); // NOI18N
        jmenu_reportes.setText("Reportes");
        jmenu_reportes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_reportesActionPerformed(evt);
            }
        });
        jMenu6.add(jmenu_reportes);

        jMenuBar1.add(jMenu6);

        jmenu_admin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/configuraciones.png"))); // NOI18N
        jmenu_admin.setText("Admin");
        jmenu_admin.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_user.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jmenu_user.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_user.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/users_pequeno.png"))); // NOI18N
        jmenu_user.setText("Users");
        jmenu_user.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_userActionPerformed(evt);
            }
        });
        jmenu_admin.add(jmenu_user);

        jmenu_configuraciones.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_configuraciones.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/configuraciones.png"))); // NOI18N
        jmenu_configuraciones.setText("Configuraciones");
        jmenu_configuraciones.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_configuracionesActionPerformed(evt);
            }
        });
        jmenu_admin.add(jmenu_configuraciones);

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jMenuItem1.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/shutdown.png"))); // NOI18N
        jMenuItem1.setText("Cerrar Sesión");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jmenu_admin.add(jMenuItem1);

        jmenu_backup.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_backup.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/database_peq.png"))); // NOI18N
        jmenu_backup.setText("BackUp");
        jmenu_backup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_backupActionPerformed(evt);
            }
        });
        jmenu_admin.add(jmenu_backup);

        jMenu_tipo_ingreso.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jMenu_tipo_ingreso.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/estadisticas.png"))); // NOI18N
        jMenu_tipo_ingreso.setText("Tipo de ingreso");
        jMenu_tipo_ingreso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_tipo_ingresoActionPerformed(evt);
            }
        });
        jmenu_admin.add(jMenu_tipo_ingreso);

        jMenu_unidades.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jMenu_unidades.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/estadisticas.png"))); // NOI18N
        jMenu_unidades.setText("Unidades de medida");
        jMenu_unidades.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_unidadesActionPerformed(evt);
            }
        });
        jmenu_admin.add(jMenu_unidades);

        jmenu_bodegas.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_bodegas.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/bodega.png"))); // NOI18N
        jmenu_bodegas.setText("Bodegas");
        jmenu_bodegas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_bodegasActionPerformed(evt);
            }
        });
        jmenu_admin.add(jmenu_bodegas);

        jMenuBar1.add(jmenu_admin);

        jMenu7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Calculator.png"))); // NOI18N
        jMenu7.setText("Calculadora retenciones");
        jMenu7.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        jmenu_calculadora_retenciones.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0));
        jmenu_calculadora_retenciones.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jmenu_calculadora_retenciones.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagenes/Calculator.png"))); // NOI18N
        jmenu_calculadora_retenciones.setText("Calculadora retenciones");
        jmenu_calculadora_retenciones.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmenu_calculadora_retencionesActionPerformed(evt);
            }
        });
        jMenu7.add(jmenu_calculadora_retenciones);

        jMenuBar1.add(jMenu7);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(escritorio)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(escritorio)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
@Override
    public Image getIconImage() {
        Image retValue = Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("imagenes/icono.png"));

        return retValue;
    }

    private void jmenu_productosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_productosActionPerformed
        if (metodos.estacerrado(frm_producto)) {
            frm_producto = new frm_productos();
            escritorio.add(frm_producto);
            try {
                frm_producto.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_producto.show();
        } else {
            frm_producto.toFront();
        }

    }//GEN-LAST:event_jmenu_productosActionPerformed

    private void jmenu_userActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_userActionPerformed
        if (metodos.estacerrado(jif_user)) {
            jif_user = new jif_users();
            escritorio.add(jif_user);
            try {
                jif_user.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            jif_user.show();
        } else {
            jif_user.toFront();
        }

    }//GEN-LAST:event_jmenu_userActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        if (metodos.estacerrado(frm_ingreso_mercancias)) {
            frm_ingreso_mercancias = new frm_ingreso_mercancia();
            escritorio.add(frm_ingreso_mercancias);
            try {
                frm_ingreso_mercancias.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_ingreso_mercancias.show();

        } else {
            frm_ingreso_mercancias.toFront();
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jmenu_facturacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_facturacionActionPerformed
        try {
            if (metodos.estacerrado(frm_facturacion)) {
                frm_facturacion = new frm_Crear_Orden();
                escritorio.add(frm_facturacion);
                frm_facturacion.show();
                try {
                    frm_facturacion.setMaximum(true);
                } catch (PropertyVetoException ex) {
                    Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                frm_facturacion.toFront();
            }
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al abrir Crear Orden:\n" + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jmenu_facturacionActionPerformed

    private void jmenu_ver_facturaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_ver_facturaActionPerformed
        if (metodos.estacerrado(frm_factura)) {
            frm_factura = new frm_Ordenes();
            escritorio.add(frm_factura);
            frm_factura.show();
            try {
                frm_factura.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_factura.toFront();
        }
    }//GEN-LAST:event_jmenu_ver_facturaActionPerformed

    private void jmenu_contactosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_contactosActionPerformed
        if (metodos.estacerrado(frm_contacto)) {
            frm_contacto = new frm_contactos();
            escritorio.add(frm_contacto);
            frm_contacto.show();
            try {
                frm_contacto.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_contacto.toFront();
        }
    }//GEN-LAST:event_jmenu_contactosActionPerformed

    private void jmenu_configuracionesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_configuracionesActionPerformed
        jd_configuraciones confg = new jd_configuraciones(this, rootPaneCheckingEnabled);
        confg.show();
    }//GEN-LAST:event_jmenu_configuracionesActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        this.dispose();
        login log = new login();
        log.show();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jmenu_reportesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_reportesActionPerformed
        if (metodos.estacerrado(jif_reportes)) {
            jif_reportes = new jif_PrincipalReportes();
            escritorio.add(jif_reportes);
            jif_reportes.show();
        } else {
            jif_reportes.toFront();
        }
    }//GEN-LAST:event_jmenu_reportesActionPerformed

    private void jmenu_backupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_backupActionPerformed
        if (metodos.estacerrado(backup)) {
            backup = new Backup(this, true);
            backup.show();
        } else {
        }
    }//GEN-LAST:event_jmenu_backupActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        frm_consulta j = new frm_consulta(this, true);
        j.show();

    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenu_tipo_ingresoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_tipo_ingresoActionPerformed
        if (metodos.estacerrado(frm_tipo_ingresos)) {
            frm_tipo_ingresos = new frm_tipo_ingresos();
            escritorio.add(frm_tipo_ingresos);
            try {
                frm_tipo_ingresos.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_tipo_ingresos.show();
        } else {
            frm_tipo_ingresos.toFront();
        }
    }//GEN-LAST:event_jMenu_tipo_ingresoActionPerformed

    private void btn_contactosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_contactosActionPerformed
        if (metodos.estacerrado(frm_contacto)) {
            frm_contacto = new frm_contactos();
            escritorio.add(frm_contacto);
            frm_contacto.show();
            try {
                frm_contacto.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_contacto.toFront();
        }
    }//GEN-LAST:event_btn_contactosActionPerformed

    private void btn_productosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_productosActionPerformed
        if (metodos.estacerrado(frm_producto)) {
            frm_producto = new frm_productos();
            escritorio.add(frm_producto);
            try {
                frm_producto.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_producto.show();
        } else {
            frm_producto.toFront();
        }
    }//GEN-LAST:event_btn_productosActionPerformed

    private void btn_ingreso_productosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ingreso_productosActionPerformed
        if (metodos.estacerrado(frm_ingreso_mercancias)) {
            frm_ingreso_mercancias = new frm_ingreso_mercancia();
            escritorio.add(frm_ingreso_mercancias);
            try {
                frm_ingreso_mercancias.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_ingreso_mercancias.show();

        } else {
            frm_ingreso_mercancias.toFront();
        }
    }//GEN-LAST:event_btn_ingreso_productosActionPerformed

    private void btn_consultarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_consultarActionPerformed
        frm_consulta j = new frm_consulta(this, true);
        j.show();

    }//GEN-LAST:event_btn_consultarActionPerformed

    private void btn_generar_ordenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_generar_ordenActionPerformed
        try {
            if (metodos.estacerrado(frm_facturacion)) {
                frm_facturacion = new frm_Crear_Orden();
                escritorio.add(frm_facturacion);
                frm_facturacion.show();
                try {
                    frm_facturacion.setMaximum(true);
                } catch (PropertyVetoException ex) {
                    Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                frm_facturacion.toFront();
            }
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al abrir Crear Orden:\n" + e.getClass().getSimpleName() + ": " + e.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btn_generar_ordenActionPerformed

    private void btn_ver_ordenesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_ordenesActionPerformed
        if (metodos.estacerrado(frm_factura)) {
            frm_factura = new frm_Ordenes();
            escritorio.add(frm_factura);
            frm_factura.show();
            try {
                frm_factura.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_factura.toFront();
        }
    }//GEN-LAST:event_btn_ver_ordenesActionPerformed

    private void btn_reportesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_reportesActionPerformed
        if (metodos.estacerrado(jif_reportes)) {
            jif_reportes = new jif_PrincipalReportes();
            escritorio.add(jif_reportes);
            jif_reportes.show();
        } else {
            jif_reportes.toFront();
        }
    }//GEN-LAST:event_btn_reportesActionPerformed

    private void btn_actualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_actualizarActionPerformed

    }//GEN-LAST:event_btn_actualizarActionPerformed

    private void jmenu_ver_anuladosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_ver_anuladosActionPerformed
        if (metodos.estacerrado(frm_anulados)) {
            frm_anulados = new frm_facturas_anuladas();
            escritorio.add(frm_anulados);
            frm_anulados.show();
            try {
                frm_anulados.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_anulados.toFront();
        }
    }//GEN-LAST:event_jmenu_ver_anuladosActionPerformed

    private void jMenu_unidadesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_unidadesActionPerformed
        if (metodos.estacerrado(frm_unidad)) {
            frm_unidad = new frm_unidades();
            escritorio.add(frm_unidad);
            try {
                frm_unidad.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_unidad.show();
        } else {
            frm_unidad.toFront();
        }
    }//GEN-LAST:event_jMenu_unidadesActionPerformed

    private void jmenu_bodegasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_bodegasActionPerformed
        if (metodos.estacerrado(frm_bodega)) {
            frm_bodega = new frm_bodegas();
            escritorio.add(frm_bodega);
            try {
                frm_bodega.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_bodega.show();
        } else {
            frm_bodega.toFront();
        }
    }//GEN-LAST:event_jmenu_bodegasActionPerformed

    private void jmenu_reportes1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_reportes1ActionPerformed
        if (metodos.estacerrado(frm_recortes)) {
            frm_recortes = new frm_Recortes();
            escritorio.add(frm_recortes);
            try {
                frm_recortes.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_recortes.show();
        } else {
            frm_recortes.toFront();
        }
    }//GEN-LAST:event_jmenu_reportes1ActionPerformed

    private void btn_generar_recorteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_generar_recorteActionPerformed
        if (metodos.estacerrado(frm_recortes)) {
            frm_recortes = new frm_Recortes();
            escritorio.add(frm_recortes);
            try {
                frm_recortes.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_recortes.show();
        } else {
            frm_recortes.toFront();
        }
    }//GEN-LAST:event_btn_generar_recorteActionPerformed

    private void btn_ver_recortesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_recortesActionPerformed

        if (metodos.estacerrado(frm_recortes_pendientes)) {
            frm_recortes_pendientes = new frm_Recortes_pendientes();
            escritorio.add(frm_recortes_pendientes);
            try {
                frm_recortes_pendientes.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_recortes_pendientes.show();
        } else {
            frm_recortes_pendientes.toFront();
        }
    }//GEN-LAST:event_btn_ver_recortesActionPerformed

    private void jmenu_reportes2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_reportes2ActionPerformed
        if (metodos.estacerrado(frm_recortes_pendientes)) {
            frm_recortes_pendientes = new frm_Recortes_pendientes();
            escritorio.add(frm_recortes_pendientes);
            try {
                frm_recortes_pendientes.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            frm_recortes_pendientes.show();
        } else {
            frm_recortes_pendientes.toFront();
        }
    }//GEN-LAST:event_jmenu_reportes2ActionPerformed

    private void btn_facturarMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_facturarMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_btn_facturarMouseEntered

    private void btn_facturarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_facturarActionPerformed
        try {
            if (metodos.estacerrado(frm_facturacion_venta)) {
                frm_facturacion_venta = new frm_facturacion_ventas();
                escritorio.add(frm_facturacion_venta);
                frm_facturacion_venta.show();
                try {
                    frm_facturacion_venta.setMaximum(true);
                } catch (PropertyVetoException ex) {
                    Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                frm_facturacion_venta.toFront();
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_btn_facturarActionPerformed

    private void btn_ver_facturasMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_ver_facturasMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_btn_ver_facturasMouseEntered

    private void btn_ver_facturasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_facturasActionPerformed
        if (metodos.estacerrado(frm_factura_ventas)) {
            frm_factura_ventas = new frm_facturas_ventas();
            escritorio.add(frm_factura_ventas);
            frm_factura_ventas.show();
            try {
                frm_factura_ventas.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_factura_ventas.toFront();
        }
    }//GEN-LAST:event_btn_ver_facturasActionPerformed

    private void btn_decolucionMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_decolucionMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_btn_decolucionMouseEntered

    private void btn_decolucionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_decolucionActionPerformed
        if (metodos.estacerrado(jif_devolucion)) {
            jif_devolucion = new frm_devoluciones();
            escritorio.add(jif_devolucion);
            try {
                jif_devolucion.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
            jif_devolucion.show();
        } else {
            jif_devolucion.toFront();
        }
    }//GEN-LAST:event_btn_decolucionActionPerformed

    private void btn_ingreso_productos1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ingreso_productos1ActionPerformed
        frm_consulta j = new frm_consulta(this, true);
        j.show();

    }//GEN-LAST:event_btn_ingreso_productos1ActionPerformed

    private void btn_cotizacionMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_cotizacionMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_btn_cotizacionMouseEntered

    private void btn_cotizacionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_cotizacionActionPerformed
        try {
            if (metodos.estacerrado(frm_cotizacion)) {
                frm_cotizacion = new frm_cotizacion();
                escritorio.add(frm_cotizacion);
                frm_cotizacion.show();
                try {
                    frm_cotizacion.setMaximum(true);
                } catch (PropertyVetoException ex) {
                    Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                frm_cotizacion.toFront();
            }
        } catch (Exception e) {
        }
    }//GEN-LAST:event_btn_cotizacionActionPerformed

    private void btn_ver_cotizacionesMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btn_ver_cotizacionesMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_btn_ver_cotizacionesMouseEntered

    private void btn_ver_cotizacionesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_ver_cotizacionesActionPerformed
        if (metodos.estacerrado(frm_cotizaciones)) {
            frm_cotizaciones = new frm_cotizaciones();
            escritorio.add(frm_cotizaciones);
            frm_cotizaciones.show();
            try {
                frm_cotizaciones.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_cotizaciones.toFront();
        }
    }//GEN-LAST:event_btn_ver_cotizacionesActionPerformed

    private void jmenu_mover_productosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_mover_productosActionPerformed
        if (metodos.estacerrado(frm_mover)) {
            frm_mover = new frm_traslado_productos_entre_bodegas();
            escritorio.add(frm_mover);
            frm_mover.show();
            try {
                frm_mover.setMaximum(true);
            } catch (PropertyVetoException ex) {
                Logger.getLogger(frm_main.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            frm_mover.toFront();
        }
       
    }//GEN-LAST:event_jmenu_mover_productosActionPerformed

    private void jmenu_calculadora_retencionesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmenu_calculadora_retencionesActionPerformed

        frm_calcular_retenciones j = new frm_calcular_retenciones(this, true);
        j.show();
    }//GEN-LAST:event_jmenu_calculadora_retencionesActionPerformed

    private void jMenu_verificar_inventarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_verificar_inventarioActionPerformed
        abrirAjusteInventario();
    }//GEN-LAST:event_jMenu_verificar_inventarioActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        frm_consulta_old frm = new frm_consulta_old(this, cerra);
        frm.show();
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(frm_main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(frm_main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(frm_main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(frm_main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new frm_main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_contactos;
    private javax.swing.JButton btn_cotizacion;
    private javax.swing.JButton btn_decolucion;
    private javax.swing.JButton btn_facturar;
    private javax.swing.JButton btn_generar_orden;
    private javax.swing.JButton btn_generar_recorte;
    private javax.swing.JButton btn_ingreso_productos;
    private javax.swing.JButton btn_ingreso_productos1;
    private javax.swing.JButton btn_productos;
    private javax.swing.JButton btn_ver_cotizaciones;
    private javax.swing.JButton btn_ver_facturas;
    private javax.swing.JButton btn_ver_ordenes;
    private javax.swing.JButton btn_ver_recortes;
    public static javax.swing.JDesktopPane escritorio;
    private javax.swing.JButton jButton1;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenu jMenu_ordenes;
    private javax.swing.JMenu jMenu_productos_principal;
    private javax.swing.JMenu jMenu_recortes;
    private javax.swing.JMenuItem jMenu_tipo_ingreso;
    private javax.swing.JMenuItem jMenu_unidades;
    private javax.swing.JMenuItem jMenu_verificar_inventario;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JMenu jmenu_admin;
    private javax.swing.JMenuItem jmenu_backup;
    private javax.swing.JMenuItem jmenu_bodegas;
    private javax.swing.JMenuItem jmenu_calculadora_retenciones;
    private javax.swing.JMenu jmenu_con;
    private javax.swing.JMenuItem jmenu_configuraciones;
    private javax.swing.JMenuItem jmenu_contactos;
    private javax.swing.JMenuItem jmenu_facturacion;
    private javax.swing.JMenuItem jmenu_mover_productos;
    private javax.swing.JMenuItem jmenu_productos;
    private javax.swing.JMenuItem jmenu_reportes;
    private javax.swing.JMenuItem jmenu_reportes1;
    private javax.swing.JMenuItem jmenu_reportes2;
    private javax.swing.JMenuItem jmenu_user;
    private javax.swing.JMenuItem jmenu_ver_anulados;
    private javax.swing.JMenuItem jmenu_ver_factura;
    public static javax.swing.JLabel lbl_bodega_user;
    public static javax.swing.JLabel lbl_perfil;
    public static javax.swing.JLabel lbl_user;
    // End of variables declaration//GEN-END:variables
}
