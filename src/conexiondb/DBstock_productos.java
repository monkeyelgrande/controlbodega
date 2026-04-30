package conexiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;

/**
 * Gestión de stock por bodega y movimientos de inventario. Trabaja con las
 * tablas: stock_productos, movimientos_inventario
 *
 * MODELO HÍBRIDO: - Un solo movimiento por operación (creación, edición o
 * eliminación) - Guarda valor_anterior y valor_nuevo para ediciones - Histórico
 * completo con snapshots de stock antes/después
 *
 * TIPOS DE MOVIMIENTO:
 * ┌─────────────────────────────────────────────────────────────────────────┐ │
 * OPERACIÓN │ CREAR │ EDITAR │ ELIMINAR │
 * ├────────────────────┼────────────────────┼───────────────────┼──────────┤ │
 * Ingreso │ INGRESO │ EDICION_INGRESO │ ELIM_INGRESO │ Venta directa │ VENTA │
 * EDICION_VENTA │ ANULACION_VENTA │ Orden normal │ ORDEN │ EDICION_ORDEN │
 * ANULACION_ORDEN │ Orden referenciada │ ORDEN_REFERENCIADA │ EDICION_ORDEN_REF
 * │ ANULACION_ORDEN_REF │ Entrega │ ENTREGA │ EDICION_ENTREGA │ ELIM_ENTREGA │
 * Devolución │ DEVOLUCION │ EDICION_DEVOLUCION│ ELIM_DEVOLUCION │ Traslado │
 * TRASLADO_* │ EDICION_TRASLADO_*│ ELIM_TRASLADO_* │ Ajuste │ AJUSTE_* │ - │ -
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * @author Sistema Bodega
 */
public class DBstock_productos {

    // ========================================================================
    // CONSTANTES DE TIPOS DE MOVIMIENTO
    // ========================================================================
    // Creación
    public static final String TIPO_INGRESO = "INGRESO";
    public static final String TIPO_VENTA = "VENTA";
    public static final String TIPO_ORDEN = "ORDEN";
    public static final String TIPO_ORDEN_REFERENCIADA = "ORDEN_REFERENCIADA";
    public static final String TIPO_ENTREGA = "ENTREGA";
    public static final String TIPO_DEVOLUCION = "DEVOLUCION";
    public static final String TIPO_TRASLADO_SALIDA = "TRASLADO_SALIDA";
    public static final String TIPO_TRASLADO_ENTRADA = "TRASLADO_ENTRADA";
    public static final String TIPO_AJUSTE_POSITIVO = "AJUSTE_POSITIVO";
    public static final String TIPO_AJUSTE_NEGATIVO = "AJUSTE_NEGATIVO";

    // Edición
    public static final String TIPO_EDICION_INGRESO = "EDICION_INGRESO";
    public static final String TIPO_EDICION_VENTA = "EDICION_VENTA";
    public static final String TIPO_EDICION_ORDEN = "EDICION_ORDEN";
    public static final String TIPO_EDICION_ORDEN_REF = "EDICION_ORDEN_REF";
    public static final String TIPO_EDICION_ENTREGA = "EDICION_ENTREGA";
    public static final String TIPO_EDICION_DEVOLUCION = "EDICION_DEVOLUCION";
    public static final String TIPO_EDICION_TRASLADO_SALIDA = "EDICION_TRASLADO_SAL";
    public static final String TIPO_EDICION_TRASLADO_ENTRADA = "EDICION_TRASLADO_ENT";

    // Eliminación / Anulación
    public static final String TIPO_ELIM_INGRESO = "ELIM_INGRESO";
    public static final String TIPO_ANULACION_VENTA = "ANULACION_VENTA";
    public static final String TIPO_ANULACION_ORDEN = "ANULACION_ORDEN";
    public static final String TIPO_ANULACION_ORDEN_REF = "ANULACION_ORDEN_REF";
    public static final String TIPO_ELIM_ENTREGA = "ELIM_ENTREGA";
    public static final String TIPO_ELIM_DEVOLUCION = "ELIM_DEVOLUCION";
    public static final String TIPO_ELIM_TRASLADO_SALIDA = "ELIM_TRASLADO_SAL";
    public static final String TIPO_ELIM_TRASLADO_ENTRADA = "ELIM_TRASLADO_ENT";

    // Generado automáticamente por wo-printer (integración WorldOffice)
    public static final String TIPO_AUTOMATICO = "AUTOMATICO";

    // ========================================================================
    // MÉTODO PRINCIPAL: Registrar Movimiento (interno)
    // ========================================================================
    /**
     * Registra un movimiento de inventario y actualiza stock_productos. Método
     * interno usado por todos los demás métodos.
     *
     * @param idProducto ID del producto
     * @param idBodega ID de la bodega
     * @param idUser ID del usuario que realiza el movimiento
     * @param tipo Tipo de movimiento
     * @param valor Delta a aplicar (siempre positivo)
     * @param afectaCantidad +1 suma, -1 resta, 0 no afecta
     * @param afectaPendientes +1 suma, -1 resta, 0 no afecta
     * @param valorAnterior Para ediciones: valor original del documento (puede
     * ser null)
     * @param valorNuevo Para ediciones: valor nuevo del documento (puede ser
     * null)
     * @param costoUnitario Costo unitario (puede ser null si no aplica)
     * @param idReferencia ID de la tabla de origen (puede ser null)
     * @param tablaReferencia Nombre de la tabla origen (puede ser null)
     * @param observacion Nota opcional (puede ser null)
     * @return ID del movimiento creado, o -1 si falla
     */
    private int registrarMovimiento(
            int idProducto,
            int idBodega,
            int idUser,
            String tipo,
            double valor,
            int afectaCantidad,
            int afectaPendientes,
            Double valorAnterior,
            Double valorNuevo,
            Double costoUnitario,
            Integer idReferencia,
            String tablaReferencia,
            String observacion
    ) {
        Connection con = null;
        int idMovimiento = -1;

        try {
            con = DB_consultas_R_D.getConexion();
            con.setAutoCommit(false);

            // ----------------------------------------------------------------
            // PASO 1: Obtener o crear registro en stock_productos
            // ----------------------------------------------------------------
            double cantidadAnterior = 0;
            double pendientesAnterior = 0;
            double costoPromedioAnterior = 0;
            boolean existeStock = false;

            String sqlSelect = "SELECT cantidad, pendientes, costo_promedio "
                    + "FROM stock_productos "
                    + "WHERE id_producto = ? AND id_bodega = ? "
                    + "FOR UPDATE";

            try (PreparedStatement psSelect = con.prepareStatement(sqlSelect)) {
                psSelect.setInt(1, idProducto);
                psSelect.setInt(2, idBodega);

                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        existeStock = true;
                        cantidadAnterior = rs.getDouble("cantidad");
                        pendientesAnterior = rs.getDouble("pendientes");
                        costoPromedioAnterior = rs.getDouble("costo_promedio");
                    }
                }
            }

            if (!existeStock) {
                String sqlInsertStock = "INSERT INTO stock_productos "
                        + "(id_producto, id_bodega, cantidad, pendientes, costo_promedio) "
                        + "VALUES (?, ?, 0, 0, 0)";

                try (PreparedStatement psInsert = con.prepareStatement(sqlInsertStock)) {
                    psInsert.setInt(1, idProducto);
                    psInsert.setInt(2, idBodega);
                    psInsert.executeUpdate();
                }
            }

            // ----------------------------------------------------------------
            // PASO 2: Calcular nuevos valores de stock
            // ----------------------------------------------------------------
            double deltaCantidad = valor * afectaCantidad;
            double deltaPendientes = valor * afectaPendientes;

            double cantidadNueva = cantidadAnterior + deltaCantidad;
            double pendientesNuevo = pendientesAnterior + deltaPendientes;

            // ----------------------------------------------------------------
            // PASO 3: Calcular costo promedio (solo si es entrada con costo)
            // ----------------------------------------------------------------
            double costoPromedioNuevo = costoPromedioAnterior;

            if (afectaCantidad == 1 && costoUnitario != null && costoUnitario > 0) {
                double valorInventarioAnterior = cantidadAnterior * costoPromedioAnterior;
                double valorIngreso = valor * costoUnitario;
                double stockTotal = cantidadAnterior + valor;

                if (stockTotal > 0) {
                    costoPromedioNuevo = (valorInventarioAnterior + valorIngreso) / stockTotal;
                } else {
                    costoPromedioNuevo = costoUnitario;
                }
            }

            // ----------------------------------------------------------------
            // PASO 4: Actualizar stock_productos
            // ----------------------------------------------------------------
            String sqlUpdate = "UPDATE stock_productos SET "
                    + "cantidad = ?, "
                    + "pendientes = ?, "
                    + "costo_promedio = ?, "
                    + "updated_at = now() "
                    + "WHERE id_producto = ? AND id_bodega = ?";

            try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                psUpdate.setDouble(1, cantidadNueva);
                psUpdate.setDouble(2, pendientesNuevo);
                psUpdate.setDouble(3, costoPromedioNuevo);
                psUpdate.setInt(4, idProducto);
                psUpdate.setInt(5, idBodega);
                psUpdate.executeUpdate();
            }

            // ----------------------------------------------------------------
            // PASO 5: Insertar movimiento en histórico
            // ----------------------------------------------------------------
            String horaActual = new SimpleDateFormat("HH:mm:ss").format(new Date());

            String sqlMovimiento = "INSERT INTO movimientos_inventario ("
                    + "id_producto, id_bodega, id_user, "
                    + "tipo, afecta_cantidad, afecta_pendientes, valor, "
                    + "valor_anterior, valor_nuevo, "
                    + "costo_unitario, costo_promedio_anterior, costo_promedio_nuevo, "
                    + "cantidad_anterior, cantidad_nueva, pendientes_anterior, pendientes_nuevo, "
                    + "id_referencia, tabla_referencia, fecha, hora, observacion"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                    + "('now'::text)::date, ?, ?)";

            try (PreparedStatement psMov = con.prepareStatement(sqlMovimiento,
                    PreparedStatement.RETURN_GENERATED_KEYS)) {

                int idx = 1;
                psMov.setInt(idx++, idProducto);
                psMov.setInt(idx++, idBodega);
                psMov.setInt(idx++, idUser);
                psMov.setString(idx++, tipo);
                psMov.setInt(idx++, afectaCantidad);
                psMov.setInt(idx++, afectaPendientes);
                psMov.setDouble(idx++, valor);

                // valor_anterior (nullable)
                if (valorAnterior != null) {
                    psMov.setDouble(idx++, valorAnterior);
                } else {
                    psMov.setNull(idx++, java.sql.Types.DOUBLE);
                }

                // valor_nuevo (nullable)
                if (valorNuevo != null) {
                    psMov.setDouble(idx++, valorNuevo);
                } else {
                    psMov.setNull(idx++, java.sql.Types.DOUBLE);
                }

                // costo_unitario (nullable)
                if (costoUnitario != null) {
                    psMov.setDouble(idx++, costoUnitario);
                } else {
                    psMov.setNull(idx++, java.sql.Types.DOUBLE);
                }

                psMov.setDouble(idx++, costoPromedioAnterior);
                psMov.setDouble(idx++, costoPromedioNuevo);
                psMov.setDouble(idx++, cantidadAnterior);
                psMov.setDouble(idx++, cantidadNueva);
                psMov.setDouble(idx++, pendientesAnterior);
                psMov.setDouble(idx++, pendientesNuevo);

                // id_referencia (nullable)
                if (idReferencia != null) {
                    psMov.setInt(idx++, idReferencia);
                } else {
                    psMov.setNull(idx++, java.sql.Types.INTEGER);
                }

                // tabla_referencia (nullable)
                if (tablaReferencia != null) {
                    psMov.setString(idx++, tablaReferencia);
                } else {
                    psMov.setNull(idx++, java.sql.Types.VARCHAR);
                }

                psMov.setString(idx++, horaActual);

                // observacion (nullable)
                if (observacion != null) {
                    psMov.setString(idx++, observacion);
                } else {
                    psMov.setNull(idx++, java.sql.Types.VARCHAR);
                }

                psMov.executeUpdate();

                try (ResultSet rs = psMov.getGeneratedKeys()) {
                    if (rs.next()) {
                        idMovimiento = rs.getInt(1);
                    }
                }
            }

            con.commit();

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    "Error al registrar movimiento de inventario:\n" + e.getMessage(),
                    "Error en la operación",
                    JOptionPane.ERROR_MESSAGE);

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error al cerrar conexión: " + ex.getMessage());
                }
            }
        }

        return idMovimiento;
    }

    // ========================================================================
    // INGRESO DE MERCANCÍA
    // ========================================================================
    /**
     * Registra un INGRESO de mercancía. cantidad: +valor | pendientes: sin
     * cambio
     */
    public int ingreso(int idProducto, int idBodega, int idUser, double cantidad,
            Double costoUnitario, Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_INGRESO, cantidad, +1, 0,
                null, null, // Sin valores anterior/nuevo (es creación)
                costoUnitario, idReferencia, "ingresos_mercancias_cabecera", observacion);
    }

    /**
     * Edita un INGRESO de mercancía. Aplica el delta (cantidadNueva -
     * cantidadAnterior).
     */
    public int editarIngreso(int idProducto, int idBodega, int idUser,
            double cantidadAnterior, double cantidadNueva, Double costoUnitarioNuevo,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return 0; // Sin cambios
        }

        int afecta = (delta > 0) ? +1 : -1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_EDICION_INGRESO, Math.abs(delta), afecta, 0,
                cantidadAnterior, cantidadNueva, // Guardar valores para histórico
                costoUnitarioNuevo, idReferencia, "ingresos_mercancias_cabecera", observacion);
    }

    /**
     * Elimina/Reversa un INGRESO de mercancía.
     */
    public int eliminarIngreso(int idProducto, int idBodega, int idUser,
            double cantidadOriginal, Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ELIM_INGRESO, cantidadOriginal, -1, 0,
                cantidadOriginal, 0.0, // Anterior: lo que había, Nuevo: 0
                null, idReferencia, "ingresos_mercancias_cabecera", observacion);
    }

    // ========================================================================
    // VENTA DIRECTA
    // ========================================================================
    /**
     * Registra una VENTA directa (sin orden). cantidad: -valor | pendientes:
     * sin cambio
     */
    public int venta(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_VENTA, cantidad, -1, 0,
                null, null,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    /**
     * Edita una VENTA directa. Si se vendió menos, devuelve al stock. Si se
     * vendió más, descuenta.
     */
    public int editarVenta(int idProducto, int idBodega, int idUser,
            double cantidadAnterior, double cantidadNueva,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return 0;
        }

        // Si delta > 0: se vendió más → restar
        // Si delta < 0: se vendió menos → sumar (devolver)
        int afecta = (delta > 0) ? -1 : +1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_EDICION_VENTA, Math.abs(delta), afecta, 0,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    /**
     * Anula una VENTA directa. Devuelve la cantidad al stock.
     */
    public int anularVenta(int idProducto, int idBodega, int idUser,
            double cantidadOriginal, Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ANULACION_VENTA, cantidadOriginal, +1, 0,
                cantidadOriginal, 0.0,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    // ========================================================================
    // ORDEN NORMAL (sin venta previa)
    // ========================================================================
    /**
     * Registra una ORDEN normal (compromete stock). cantidad: sin cambio |
     * pendientes: +valor
     */
    public int orden(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ORDEN, cantidad, 0, +1,
                null, null,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    /**
     * Edita una ORDEN normal.
     */
    public int editarOrden(int idProducto, int idBodega, int idUser,
            double cantidadAnterior, double cantidadNueva,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return 0;
        }

        int afecta = (delta > 0) ? +1 : -1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_EDICION_ORDEN, Math.abs(delta), 0, afecta,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    /**
     * Anula una ORDEN normal (libera pendientes).
     */
    public int anulacionOrden(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ANULACION_ORDEN, cantidad, 0, -1,
                cantidad, 0.0,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    // ========================================================================
    // ORDEN REFERENCIADA (a una venta existente)
    // ========================================================================
    /**
     * Registra una ORDEN REFERENCIADA a una venta existente. cantidad: +valor
     * (revierte descuento de venta) | pendientes: +valor
     */
    public int ordenReferenciada(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ORDEN_REFERENCIADA, cantidad, +1, +1,
                null, null,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    /**
     * Edita una ORDEN REFERENCIADA.
     */
    public int editarOrdenReferenciada(int idProducto, int idBodega, int idUser,
            double cantidadAnterior, double cantidadNueva,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return 0;
        }

        int afecta = (delta > 0) ? +1 : -1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_EDICION_ORDEN_REF, Math.abs(delta), afecta, afecta,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    /**
     * Anula una ORDEN REFERENCIADA.
     */
    public int anulacionOrdenReferenciada(int idProducto, int idBodega, int idUser,
            double cantidad, Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ANULACION_ORDEN_REF, cantidad, -1, -1,
                cantidad, 0.0,
                null, idReferencia, "facturas_cabeceras", observacion);
    }

    // ========================================================================
    // ENTREGA
    // ========================================================================
    /**
     * Registra una ENTREGA de orden. cantidad: -valor | pendientes: -valor
     */
    public int entrega(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ENTREGA, cantidad, -1, -1,
                null, null,
                null, idReferencia, "entregas_productos_cabecera", observacion);
    }

    /**
     * Edita una ENTREGA.
     */
    public int editarEntrega(int idProducto, int idBodega, int idUser,
            double cantidadAnterior, double cantidadNueva,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return 0;
        }

        // Si delta > 0: se entregó más → -cantidad, -pendientes
        // Si delta < 0: se entregó menos → +cantidad, +pendientes
        int afecta = (delta > 0) ? -1 : +1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_EDICION_ENTREGA, Math.abs(delta), afecta, afecta,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "entregas_productos_cabecera", observacion);
    }

    /**
     * Elimina una ENTREGA. Devuelve cantidad y pendientes.
     */
    public int eliminarEntrega(int idProducto, int idBodega, int idUser,
            double cantidadOriginal, Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ELIM_ENTREGA, cantidadOriginal, +1, +1,
                cantidadOriginal, 0.0,
                null, idReferencia, "entregas_productos_cabecera", observacion);
    }

    /**
     * Edita una DEVOLUCIÓN.
     */
    public int editarDevolucion(int idProducto, int idBodega, int idUser,
            double cantidadAnterior, double cantidadNueva,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return 0;
        }

        int afecta = (delta > 0) ? +1 : -1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_EDICION_DEVOLUCION, Math.abs(delta), afecta, 0,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "devoluciones", observacion);
    }

    /**
     * Elimina una DEVOLUCIÓN.
     */
    public int eliminarDevolucion(int idProducto, int idBodega, int idUser,
            double cantidadOriginal, Integer idReferencia, String observacion) {

        return registrarMovimiento(idProducto, idBodega, idUser,
                TIPO_ELIM_DEVOLUCION, cantidadOriginal, -1, 0,
                cantidadOriginal, 0.0,
                null, idReferencia, "devoluciones", observacion);
    }

    // ========================================================================
    // TRASLADO ENTRE BODEGAS
    // ========================================================================
    /**
     * Registra un TRASLADO entre bodegas. Origen: cantidad -valor | Destino:
     * cantidad +valor
     *
     * @return Array [idMovimientoSalida, idMovimientoEntrada] o null si falla
     */
    public int[] traslado(int idProducto, int idBodegaOrigen, int idBodegaDestino,
            int idUser, double cantidad, Integer idReferencia, String observacion) {

        int idSalida = registrarMovimiento(idProducto, idBodegaOrigen, idUser,
                TIPO_TRASLADO_SALIDA, cantidad, -1, 0,
                null, null,
                null, idReferencia, "traslados_productos", observacion);

        if (idSalida == -1) {
            return null;
        }

        int idEntrada = registrarMovimiento(idProducto, idBodegaDestino, idUser,
                TIPO_TRASLADO_ENTRADA, cantidad, +1, 0,
                null, null,
                null, idReferencia, "traslados_productos", observacion);

        if (idEntrada == -1) {
            return null;
        }

        return new int[]{idSalida, idEntrada};
    }

    /**
     * Edita un TRASLADO entre bodegas.
     *
     * @return Array [idMovSalida, idMovEntrada] o null si falla
     */
    public int[] editarTraslado(int idProducto, int idBodegaOrigen, int idBodegaDestino,
            int idUser, double cantidadAnterior, double cantidadNueva,
            Integer idReferencia, String observacion) {

        double delta = cantidadNueva - cantidadAnterior;

        if (delta == 0) {
            return new int[]{0, 0};
        }

        // Origen: si delta > 0 salió más (-), si delta < 0 salió menos (+)
        int afectaOrigen = (delta > 0) ? -1 : +1;
        // Destino: si delta > 0 entró más (+), si delta < 0 entró menos (-)
        int afectaDestino = (delta > 0) ? +1 : -1;

        int idSalida = registrarMovimiento(idProducto, idBodegaOrigen, idUser,
                TIPO_EDICION_TRASLADO_SALIDA, Math.abs(delta), afectaOrigen, 0,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "traslados_productos", observacion);

        if (idSalida == -1) {
            return null;
        }

        int idEntrada = registrarMovimiento(idProducto, idBodegaDestino, idUser,
                TIPO_EDICION_TRASLADO_ENTRADA, Math.abs(delta), afectaDestino, 0,
                cantidadAnterior, cantidadNueva,
                null, idReferencia, "traslados_productos", observacion);

        if (idEntrada == -1) {
            return null;
        }

        return new int[]{idSalida, idEntrada};
    }

    /**
     * Elimina un TRASLADO entre bodegas.
     *
     * @return Array [idMovSalida, idMovEntrada] o null si falla
     */
    public int[] eliminarTraslado(int idProducto, int idBodegaOrigen, int idBodegaDestino,
            int idUser, double cantidadOriginal, Integer idReferencia, String observacion) {

        // Origen: recupera lo que había salido (+)
        int idSalida = registrarMovimiento(idProducto, idBodegaOrigen, idUser,
                TIPO_ELIM_TRASLADO_SALIDA, cantidadOriginal, +1, 0,
                cantidadOriginal, 0.0,
                null, idReferencia, "traslados_productos", observacion);

        if (idSalida == -1) {
            return null;
        }

        // Destino: pierde lo que había entrado (-)
        int idEntrada = registrarMovimiento(idProducto, idBodegaDestino, idUser,
                TIPO_ELIM_TRASLADO_ENTRADA, cantidadOriginal, -1, 0,
                cantidadOriginal, 0.0,
                null, idReferencia, "traslados_productos", observacion);

        if (idEntrada == -1) {
            return null;
        }

        return new int[]{idSalida, idEntrada};
    }

    // ========================================================================
    // AJUSTE DE INVENTARIO
    // ========================================================================
    /**
     * Registra un AJUSTE de inventario (positivo o negativo).
     */
    public int ajuste(int idProducto, int idBodega, int idUser, double cantidad,
            boolean esPositivo, String observacion) {

        String tipo = esPositivo ? TIPO_AJUSTE_POSITIVO : TIPO_AJUSTE_NEGATIVO;
        int afecta = esPositivo ? +1 : -1;

        return registrarMovimiento(idProducto, idBodega, idUser,
                tipo, Math.abs(cantidad), afecta, 0,
                null, null,
                null, null, null, observacion);
    }

    // ========================================================================
    // SELECCION INTELIGENTE DE BODEGA
    // ========================================================================
    /**
     * Determina de que bodega se debe descargar un producto en una venta/orden.
     *
     * Logica:
     *   1. Bodega con MAYOR stock disponible (cantidad > 0)
     *   2. Si ninguna tiene stock positivo, ultima bodega con movimiento donde hubo stock
     *   3. Fallback: bodega ID 1
     *
     * @param idProducto ID del producto
     * @return ID de la bodega seleccionada
     */
    public static int seleccionarBodegaDescarga(int idProducto) {
        // 1. Bodega con mayor stock positivo
        String sql1 = "SELECT id_bodega FROM stock_productos "
                + "WHERE id_producto = " + idProducto + " AND cantidad > 0 "
                + "ORDER BY cantidad DESC LIMIT 1";
        try {
            java.sql.ResultSet rs = DB_consultas_R_D.getTabla(sql1);
            if (rs.next()) {
                int id = rs.getInt("id_bodega");
                rs.close();
                return id;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando bodega con mayor stock: " + e.getMessage());
        }

        // 2. Ultima bodega donde el producto tuvo stock positivo (segun movimientos)
        String sql2 = "SELECT id_bodega FROM movimientos_inventario "
                + "WHERE id_producto = " + idProducto + " AND cantidad_nueva > 0 "
                + "ORDER BY fecha DESC, id DESC LIMIT 1";
        try {
            java.sql.ResultSet rs = DB_consultas_R_D.getTabla(sql2);
            if (rs.next()) {
                int id = rs.getInt("id_bodega");
                rs.close();
                return id;
            }
            rs.close();
        } catch (Exception e) {
            System.err.println("Error consultando ultima bodega con stock: " + e.getMessage());
        }

        // 3. Fallback
        return 1;
    }

    // ========================================================================
    // MÉTODOS DE CONSULTA
    // ========================================================================
    /**
     * Obtiene el stock de un producto en una bodega específica.
     *
     * @param idProducto ID del producto
     * @param idBodega ID de la bodega
     * @return Array [cantidad, pendientes, disponible, costoPromedio] o null si
     * no existe
     */
    public double[] getStock(int idProducto, int idBodega) {
        Connection con = null;
        double[] resultado = null;

        String sql = "SELECT cantidad, pendientes, costo_promedio, "
                + "(cantidad - pendientes) AS disponible "
                + "FROM stock_productos "
                + "WHERE id_producto = ? AND id_bodega = ?";

        try {
            con = DB_consultas_R_D.getConexion();

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idProducto);
                ps.setInt(2, idBodega);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resultado = new double[4];
                        resultado[0] = rs.getDouble("cantidad");
                        resultado[1] = rs.getDouble("pendientes");
                        resultado[2] = rs.getDouble("disponible");
                        resultado[3] = rs.getDouble("costo_promedio");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar stock: " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error al cerrar conexión: " + ex.getMessage());
                }
            }
        }

        return resultado;
    }

    /**
     * Obtiene el stock TOTAL de un producto sumando todas las bodegas.
     *
     * @param idProducto ID del producto
     * @return Array [cantidad, pendientes, disponible] o null si no existe
     */
    public double[] getStockTotal(int idProducto) {
        Connection con = null;
        double[] resultado = null;

        String sql = "SELECT "
                + "COALESCE(SUM(cantidad), 0) AS cantidad, "
                + "COALESCE(SUM(pendientes), 0) AS pendientes, "
                + "COALESCE(SUM(cantidad - pendientes), 0) AS disponible "
                + "FROM stock_productos "
                + "WHERE id_producto = ?";

        try {
            con = DB_consultas_R_D.getConexion();

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idProducto);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resultado = new double[3];
                        resultado[0] = rs.getDouble("cantidad");
                        resultado[1] = rs.getDouble("pendientes");
                        resultado[2] = rs.getDouble("disponible");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar stock total: " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    System.err.println("Error al cerrar conexión: " + ex.getMessage());
                }
            }
        }

        return resultado;
    }

    /**
     * Obtiene el disponible de un producto en una bodega.
     *
     * @return Cantidad disponible (cantidad - pendientes), o 0 si no existe
     */
    public double getDisponible(int idProducto, int idBodega) {
        double[] stock = getStock(idProducto, idBodega);
        return (stock != null) ? stock[2] : 0;
    }

    /**
     * Verifica si hay suficiente disponible para una operación.
     *
     * @return true si disponible >= cantidad
     */
    public boolean hayDisponible(int idProducto, int idBodega, double cantidad) {
        return getDisponible(idProducto, idBodega) >= cantidad;
    }

    /**
     * Obtiene el costo promedio de un producto en una bodega.
     *
     * @return Costo promedio, o 0 si no existe
     */
    public double getCostoPromedio(int idProducto, int idBodega) {
        double[] stock = getStock(idProducto, idBodega);
        return (stock != null) ? stock[3] : 0;
    }

    /**
     * Obtiene la cantidad física de un producto en una bodega.
     *
     * @return Cantidad física, o 0 si no existe
     */
    public double getCantidad(int idProducto, int idBodega) {
        double[] stock = getStock(idProducto, idBodega);
        return (stock != null) ? stock[0] : 0;
    }

    /**
     * Obtiene los pendientes de un producto en una bodega.
     *
     * @return Cantidad pendiente, o 0 si no existe
     */
    public double getPendientes(int idProducto, int idBodega) {
        double[] stock = getStock(idProducto, idBodega);
        return (stock != null) ? stock[1] : 0;
    }

    /**
     * Anula una entrega: devuelve cantidad a bodega y restaura pendientes
     * cantidad: +valor, pendientes: +valor
     */
    public int anularEntrega(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto, // 1. idProducto
                idBodega, // 2. idBodega
                idUser, // 3. idUser
                "ANULACION_ENTREGA", // 4. tipo
                cantidad, // 5. valor (double)
                +1, // 6. afectaCantidad (int)
                +1, // 7. afectaPendientes (int)
                null, // 8. valorAnterior (Double)
                null, // 9. valorNuevo (Double)
                null, // 10. costoUnitario (Double)
                idReferencia, // 11. idReferencia (Integer)
                "entregas_productos_cabecera", // 12. tablaReferencia
                observacion // 13. observacion
        );

    }

    /**
     * Traslado salida: resta cantidad de bodega origen cantidad: -valor,
     * pendientes: 0
     */
    public int trasladoSalida(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto,
                idBodega,
                idUser,
                "TRASLADO_SALIDA",
                cantidad,
                -1, // afecta_cantidad: -
                0, // afecta_pendientes: sin cambio
                null,
                null,
                null,
                idReferencia,
                "traslados_productos",
                observacion
        );
    }

    /**
     * Traslado entrada: suma cantidad a bodega destino cantidad: +valor,
     * pendientes: 0
     */
    public int trasladoEntrada(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto,
                idBodega,
                idUser,
                "TRASLADO_ENTRADA",
                cantidad,
                +1, // afecta_cantidad: +
                0, // afecta_pendientes: sin cambio
                null,
                null,
                null,
                idReferencia,
                "traslados_productos",
                observacion
        );
    }

    /**
     * Anular traslado salida: devuelve cantidad a bodega origen cantidad:
     * +valor, pendientes: 0
     */
    public int anularTrasladoSalida(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto,
                idBodega,
                idUser,
                "ANULACION_TRASLADO_SALIDA",
                cantidad,
                +1, // afecta_cantidad: + (devuelve)
                0,
                null,
                null,
                null,
                idReferencia,
                "traslados_productos",
                observacion
        );
    }

    /**
     * Anular traslado entrada: resta cantidad de bodega destino cantidad:
     * -valor, pendientes: 0
     */
    public int anularTrasladoEntrada(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto,
                idBodega,
                idUser,
                "ANULACION_TRASLADO_ENTRADA",
                cantidad,
                -1, // afecta_cantidad: - (quita)
                0,
                null,
                null,
                null,
                idReferencia,
                "traslados_productos",
                observacion
        );
    }

    /**
     * Devolución: suma cantidad al stock (producto regresa a bodega) cantidad:
     * +valor, pendientes: 0
     */
    public int devolucion(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto,
                idBodega,
                idUser,
                "DEVOLUCION",
                cantidad,
                +1, // afecta_cantidad: +
                0, // afecta_pendientes: sin cambio
                null,
                null,
                null,
                idReferencia,
                "devoluciones",
                observacion
        );
    }

    /**
     * Anular devolución: resta cantidad del stock (revierte la devolución)
     * cantidad: -valor, pendientes: 0
     */
    public int anularDevolucion(int idProducto, int idBodega, int idUser, double cantidad,
            Integer idReferencia, String observacion) {
        return registrarMovimiento(
                idProducto,
                idBodega,
                idUser,
                "ANULACION_DEVOLUCION",
                cantidad,
                -1, // afecta_cantidad: -
                0, // afecta_pendientes: sin cambio
                null,
                null,
                null,
                idReferencia,
                "devoluciones",
                observacion
        );
    }
}
