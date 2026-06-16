package Metodos;

import conexiondb.DB_consultas_R_D;
import java.sql.ResultSet;

/**
 * Consultas/métricas de apoyo para el módulo de compras: existencia actual,
 * rotación (movimiento mensual), cantidad de la última compra, pedidos en
 * tránsito y el sugerido automático de cantidad a pedir.
 *
 * @author Monkeyelgrande
 */
public final class ComprasConsultas {

    private ComprasConsultas() {
    }

    private static double primerValor(String sql) {
        ResultSet rs = DB_consultas_R_D.getTabla(sql);
        try {
            if (rs != null && rs.next()) {
                return rs.getDouble(1);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return 0;
    }

    /** Existencia actual total (suma de todas las bodegas). */
    public static double existencia(int idProducto) {
        return primerValor("SELECT COALESCE(SUM(cantidad),0) FROM stock_productos "
                + "WHERE id_producto = " + idProducto);
    }

    /** Existencia en una bodega específica. */
    public static double existenciaBodega(int idProducto, int idBodega) {
        return primerValor("SELECT COALESCE(SUM(cantidad),0) FROM stock_productos "
                + "WHERE id_producto = " + idProducto + " AND id_bodega = " + idBodega);
    }

    /**
     * Rotación mensual estimada: salidas de inventario (afecta_cantidad &lt; 0)
     * en los últimos {@code dias} días, normalizadas a 30 días.
     */
    public static double rotacionMensual(int idProducto, int dias) {
        double salidas = primerValor("SELECT COALESCE(SUM(valor),0) FROM movimientos_inventario "
                + "WHERE id_producto = " + idProducto + " AND afecta_cantidad < 0 "
                + "AND fecha >= (current_date - " + dias + ")");
        return dias <= 0 ? 0 : salidas * 30.0 / dias;
    }

    /** Rotación mensual con ventana por defecto de 90 días. */
    public static double rotacionMensual(int idProducto) {
        return rotacionMensual(idProducto, 90);
    }

    /** Cantidad de la última compra registrada (ingreso de mercancía). */
    public static double ultimaCompra(int idProducto) {
        return primerValor("SELECT imd.cantidad FROM ingresos_mercancias_detalle imd "
                + "JOIN ingresos_mercancias_cabecera imc ON imc.id = imd.id_ingreso_cabecera "
                + "WHERE imd.id_producto = " + idProducto
                + " ORDER BY imc.fecha DESC, imc.id DESC LIMIT 1");
    }

    /** Pedidos en tránsito: cantidades en órdenes de compra ya aprobadas (estado=2). */
    public static double transito(int idProducto) {
        return primerValor("SELECT COALESCE(SUM(d.cantidad),0) FROM ordenes_compra_detalle d "
                + "JOIN ordenes_compra_cabecera c ON c.id = d.id_orden_cabecera "
                + "WHERE d.id_producto = " + idProducto + " AND c.estado = 2");
    }

    /**
     * Sugerido automático = (rotación mensual × meses de cobertura) − existencia
     * − tránsito, sin valores negativos.
     */
    public static double sugeridoAutomatico(double rotacionMensual, double existencia,
            double transito, double mesesCobertura) {
        double s = rotacionMensual * mesesCobertura - existencia - transito;
        return s < 0 ? 0 : Math.round(s * 100.0) / 100.0;
    }
}
