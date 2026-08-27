package Precios;

import conexiondb.DB_consultas_R_D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Unico lugar donde viven las formulas de precios de venta del modulo Precios
 * (antes el calculo estaba duplicado en el doble clic de fila y en el boton
 * "Calcular P. Venta" de jif_crear_ingreso_precios).
 *
 * Ambos modos parten del mismo costo:
 *   costo_iva_descuento       = (costo + IVA) - descuento del proveedor
 *   costo_iva_descuento_gasto = costo_iva_descuento + % de operacion
 * y ambos fijan precios con margen sobre el precio de venta:
 *   precio = costo / ((100 - %) / 100)
 *
 * @author Monkeyelgrande
 */
public final class CalculadoraPrecios {

    /** Precios calculados de una linea. En modo TECNI valorDesc1/2 SON Precio 2/3. */
    public static class Resultado {

        public double venta;
        public double valorDesc1;
        public double valorDesc2;
        public double valorSyT;
        public double valorCredito;
    }

    private CalculadoraPrecios() {
    }

    /** Margen sobre el precio de venta: costo / ((100 - pct) / 100). */
    public static double margenSobreVenta(double costo, double pct) {
        return costo / ((100 - pct) / 100);
    }

    /** Utilidad inversa: % de margen que resulta de un precio ya fijado. */
    public static double utilidadInversa(double costo, double venta) {
        if (costo <= 0) {
            return 0;
        }
        return ((venta - costo) / costo) * 100;
    }

    /** Modo AGRO: 1 margen + descuentos escalonados + S&T + credito. */
    public static Resultado calcularAgro(double costoIvaDescuento, double costoIvaDescuentoGasto,
            double pctUtilidad, double pctSyT, double pctCredito) {
        Resultado r = new Resultado();
        r.venta = margenSobreVenta(costoIvaDescuentoGasto, pctUtilidad);
        r.valorDesc1 = calcularDescuentoEscala(r.venta, costoIvaDescuentoGasto, 1);
        r.valorDesc2 = calcularDescuentoEscala(r.venta, costoIvaDescuentoGasto, 2);
        r.valorSyT = valorSyT(costoIvaDescuento, pctSyT);
        r.valorCredito = r.venta + (r.venta * (pctCredito / 100));
        return r;
    }

    /** Modo TECNI: 3 margenes independientes -> Precio 1/2/3; sin credito. */
    public static Resultado calcularTecni(double costoIvaDescuento, double costoIvaDescuentoGasto,
            double pct1, double pct2, double pct3, double pctSyT) {
        Resultado r = new Resultado();
        r.venta = margenSobreVenta(costoIvaDescuentoGasto, pct1);       // Precio 1
        r.valorDesc1 = margenSobreVenta(costoIvaDescuentoGasto, pct2);  // Precio 2
        r.valorDesc2 = margenSobreVenta(costoIvaDescuentoGasto, pct3);  // Precio 3
        r.valorSyT = valorSyT(costoIvaDescuento, pctSyT);
        r.valorCredito = 0;
        return r;
    }

    /** Valor S&T: el parametro es un DIVISOR (no un porcentaje). */
    private static double valorSyT(double costoIvaDescuento, double pctSyT) {
        if (pctSyT == 0) {
            return 0;
        }
        return costoIvaDescuento / pctSyT;
    }

    /**
     * Precio con descuento escalonado (tabla descuentos, tipo 1 o 2): segun la
     * utilidad real del producto busca el primer tramo aplicable y descuenta
     * ese % de la utilidad monetaria. Solo lo usa el modo AGRO.
     */
    public static double calcularDescuentoEscala(double venta, double costo, int tipo) {
        double valor_descuento = 0.0;

        double porcentaje_utilidad = 0.0;
        if (costo > 0) {
            porcentaje_utilidad = ((venta - costo) / costo) * 100;
        } else {
            System.out.println("El costo es 0, no se puede calcular el porcentaje de utilidad.");
            return venta;
        }

        String consulta = "SELECT utilidad, descuento FROM descuentos WHERE tipo = ? ORDER BY utilidad ASC";
        try (Connection con = DB_consultas_R_D.getConexion();
                PreparedStatement ps = con.prepareStatement(consulta)) {
            ps.setInt(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double utilidad = rs.getDouble("utilidad");
                    double descuento = rs.getDouble("descuento");

                    if (porcentaje_utilidad <= utilidad) {
                        double utilidad_monetaria = venta - costo;
                        valor_descuento = utilidad_monetaria * (descuento / 100);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return venta - valor_descuento;
    }
}
