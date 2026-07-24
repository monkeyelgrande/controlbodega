package Estilos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Barra lateral de navegación colapsable con animación de desplazamiento.
 * Traída del rediseño de electro-industrial.
 *
 * Reemplaza visualmente al JMenuBar: cada opción se enlaza a un
 * {@link AbstractButton} existente (JMenuItem o JButton) y al hacer clic
 * ejecuta {@code doClick()} sobre él, de modo que toda la lógica (abrir
 * formularios internos, permisos, atajos de teclado) sigue viviendo en
 * los componentes originales.
 *
 * Además de la visibilidad del componente enlazado, cada ítem o grupo
 * puede declarar un "gobernante": otro componente (típicamente el JMenu
 * padre) cuya visibilidad también debe cumplirse. Así se respeta el
 * esquema de permisos que gobierna menús completos.
 *
 * Uso:
 * <pre>
 *   BarraLateral barra = new BarraLateral("Control Bodega", "ContaMonkey");
 *   barra.agregarItem("Contactos", FontAwesome.CONTACTOS, jmenu_contactos, jmenu_con);
 *   BarraLateral.Grupo g = barra.agregarGrupo("Productos", FontAwesome.CAJAS)
 *           .gobernadoPor(jMenu_productos_principal);
 *   g.agregarItem("Productos", jmenu_productos);
 *   ...
 *   barra.sincronizar(); // refleja la visibilidad de los componentes
 * </pre>
 *
 * @author Monkeyelgrande
 */
public class BarraLateral extends JPanel {

    public static final int ANCHO_EXPANDIDA = 252;
    public static final int ANCHO_COLAPSADA = 64;
    /** Debajo de este ancho se ocultan los textos durante la animación. */
    private static final int UMBRAL_TEXTO = 150;
    private static final int ALTO_ITEM = 36;

    private boolean colapsada = false;
    private float anchoActual = ANCHO_EXPANDIDA;
    private final Timer animador;

    private final String nombreSistema;
    private final String subtitulo;

    private final JPanel nav;
    private final JPanel pie;
    private final List<BotonBarra> botones = new ArrayList<BotonBarra>();
    private final List<Grupo> grupos = new ArrayList<Grupo>();
    private final List<JLabel> secciones = new ArrayList<JLabel>();
    private BotonBarra itemActivo;

    public BarraLateral(String nombreSistema, String subtitulo) {
        this.nombreSistema = nombreSistema;
        this.subtitulo = subtitulo;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(ANCHO_EXPANDIDA, 0));
        setOpaque(true);

        add(new Marca(), BorderLayout.NORTH);

        nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(nav);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        pie = new JPanel();
        pie.setOpaque(false);
        pie.setLayout(new BoxLayout(pie, BoxLayout.Y_AXIS));
        pie.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(pie, BorderLayout.SOUTH);

        animador = new Timer(12, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animarPaso();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, Tema.barraFondo(),
                0, getHeight(), Tema.barraFondo2()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // ------------------------------------------------------------------
    // API de construcción
    // ------------------------------------------------------------------

    /** Agrega un rótulo de sección (p. ej. "OPERACIÓN"). */
    public void agregarSeccion(String titulo) {
        JLabel lbl = new JLabel(titulo.toUpperCase());
        lbl.setFont(Tema.fuenteBase(Font.BOLD, 10));
        lbl.setForeground(Tema.barraTextoSuave());
        lbl.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 4));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        secciones.add(lbl);
        nav.add(lbl);
    }

    /** Agrega una opción de primer nivel enlazada a un botón o item de menú. */
    public void agregarItem(String texto, char glifo, AbstractButton enlace) {
        agregarItem(texto, glifo, enlace, null);
    }

    /**
     * Agrega una opción de primer nivel cuya visibilidad también depende
     * del componente gobernante (p. ej. el JMenu que la contiene).
     */
    public void agregarItem(String texto, char glifo, AbstractButton enlace, JComponent gobernante) {
        BotonBarra btn = new BotonBarra(texto, glifo, enlace, gobernante, null, false);
        botones.add(btn);
        nav.add(btn);
    }

    /** Agrega un grupo desplegable (acordeón / flyout al estar colapsada). */
    public Grupo agregarGrupo(String titulo, char glifo) {
        Grupo grupo = new Grupo(titulo, glifo);
        grupos.add(grupo);
        nav.add(grupo.cabecera);
        nav.add(grupo.caja);
        return grupo;
    }

    /** Agrega una opción fija al pie de la barra (p. ej. cerrar sesión). */
    public void agregarItemPie(String texto, char glifo, AbstractButton enlace) {
        BotonBarra btn = new BotonBarra(texto, glifo, enlace, null, null, false);
        btn.esPie = true;
        botones.add(btn);
        pie.add(btn);
    }

    // ------------------------------------------------------------------
    // Comportamiento
    // ------------------------------------------------------------------

    /** Colapsa o expande la barra con animación de desplazamiento. */
    public void alternar() {
        colapsada = !colapsada;
        if (colapsada) {
            // al encoger se ocultan los acordeones y los rótulos de sección
            for (Grupo g : grupos) {
                g.caja.setVisible(false);
            }
            for (JLabel s : secciones) {
                s.setVisible(false);
            }
        }
        for (BotonBarra b : botones) {
            b.setToolTipText(colapsada ? b.texto : null);
        }
        animador.restart();
    }

    public boolean estaColapsada() {
        return colapsada;
    }

    private void animarPaso() {
        float destino = colapsada ? ANCHO_COLAPSADA : ANCHO_EXPANDIDA;
        anchoActual += (destino - anchoActual) * 0.22f;
        if (Math.abs(destino - anchoActual) < 0.6f) {
            anchoActual = destino;
            animador.stop();
            if (!colapsada) {
                // al terminar de expandir se restauran acordeones y secciones
                for (Grupo g : grupos) {
                    g.caja.setVisible(g.abierto && g.cabecera.isVisible());
                }
                for (JLabel s : secciones) {
                    s.setVisible(true);
                }
            }
        }
        setPreferredSize(new Dimension(Math.round(anchoActual), 0));
        revalidate();
        repaint();
    }

    private boolean mostrarTexto() {
        return anchoActual > UMBRAL_TEXTO;
    }

    /**
     * Refleja en la barra la visibilidad actual de los componentes
     * enlazados y sus gobernantes. Llamar después de {@code permisos()}.
     */
    public void sincronizar() {
        for (BotonBarra b : botones) {
            if (b.grupo != null) {
                continue; // las cabeceras se calculan con su grupo
            }
            boolean visible = b.enlace == null || b.enlace.isVisible();
            if (visible && b.gobernante != null) {
                visible = b.gobernante.isVisible();
            }
            b.setVisible(visible);
        }
        for (Grupo g : grupos) {
            boolean alguno = false;
            for (BotonBarra item : g.items) {
                if (item.isVisible()) {
                    alguno = true;
                    break;
                }
            }
            boolean visible = alguno
                    && (g.gobernante == null || g.gobernante.isVisible());
            g.cabecera.setVisible(visible);
            g.caja.setVisible(visible && g.abierto && !colapsada);
        }
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------
    // Grupo desplegable
    // ------------------------------------------------------------------

    /** Grupo de opciones: acordeón expandida, flyout colapsada. */
    public class Grupo {

        final String titulo;
        final BotonBarra cabecera;
        final JPanel caja;
        final List<BotonBarra> items = new ArrayList<BotonBarra>();
        boolean abierto = false;
        JComponent gobernante;

        private Grupo(String titulo, char glifo) {
            this.titulo = titulo;
            cabecera = new BotonBarra(titulo, glifo, null, null, this, false);
            botones.add(cabecera);

            caja = new JPanel();
            caja.setOpaque(false);
            caja.setLayout(new BoxLayout(caja, BoxLayout.Y_AXIS));
            caja.setAlignmentX(Component.LEFT_ALIGNMENT);
            caja.setVisible(false);
        }

        /**
         * El grupo entero se oculta si este componente (típicamente el
         * JMenu que agrupa las opciones) no es visible.
         */
        public Grupo gobernadoPor(JComponent componente) {
            this.gobernante = componente;
            return this;
        }

        /** Agrega una opción dentro del grupo. */
        public void agregarItem(String texto, AbstractButton enlace) {
            agregarItem(texto, enlace, null);
        }

        /** Agrega una opción con un gobernante de visibilidad adicional. */
        public void agregarItem(String texto, AbstractButton enlace, JComponent gobernante) {
            BotonBarra btn = new BotonBarra(texto, (char) 0, enlace, gobernante, null, true);
            items.add(btn);
            botones.add(btn);
            caja.add(btn);
        }

        void alternarAcordeon() {
            abierto = !abierto;
            caja.setVisible(abierto);
            nav.revalidate();
            nav.repaint();
        }

        void mostrarFlyout() {
            JPopupMenu popup = new JPopupMenu();
            JLabel encabezado = new JLabel(titulo.toUpperCase());
            encabezado.setFont(Tema.fuenteBase(Font.BOLD, 10));
            encabezado.setForeground(Tema.TEXTO_SUAVE);
            encabezado.setBorder(BorderFactory.createEmptyBorder(6, 12, 4, 12));
            popup.add(encabezado);
            for (final BotonBarra item : items) {
                if (!item.isVisible()) {
                    continue;
                }
                JMenuItem mi = new JMenuItem(item.texto);
                mi.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        item.ejecutar();
                    }
                });
                popup.add(mi);
            }
            popup.show(cabecera, cabecera.getWidth() - 4, 0);
        }
    }

    // ------------------------------------------------------------------
    // Botón de la barra (pintado a mano)
    // ------------------------------------------------------------------

    private class BotonBarra extends JButton {

        final String texto;
        final char glifo;
        final AbstractButton enlace;
        final JComponent gobernante;
        final Grupo grupo;
        final boolean esSubitem;
        boolean esPie = false;

        BotonBarra(String texto, char glifo, AbstractButton enlace,
                JComponent gobernante, Grupo grupo, boolean esSubitem) {
            this.texto = texto;
            this.glifo = glifo;
            this.enlace = enlace;
            this.gobernante = gobernante;
            this.grupo = grupo;
            this.esSubitem = esSubitem;

            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setHorizontalAlignment(SwingConstants.LEFT);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            int alto = esSubitem ? 30 : ALTO_ITEM;
            setPreferredSize(new Dimension(10, alto));
            setMinimumSize(new Dimension(10, alto));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));

            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    alClic();
                }
            });
        }

        private void alClic() {
            if (grupo != null) {
                if (colapsada) {
                    grupo.mostrarFlyout();
                } else {
                    grupo.alternarAcordeon();
                }
            } else {
                ejecutar();
            }
        }

        void ejecutar() {
            if (enlace != null) {
                enlace.doClick();
            }
            if (!esPie) {
                itemActivo = this;
            }
            BarraLateral.this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover() || getModel().isPressed();
            boolean activo = (itemActivo == this);

            if (activo) {
                g2.setColor(Tema.barraSeleccion());
                g2.fillRoundRect(0, 1, w, h - 2, 10, 10);
                g2.setColor(Tema.PRIMARIO_CLARO);
                g2.fillRoundRect(0, h / 2 - 8, 3, 16, 3, 3);
            } else if (hover) {
                g2.setColor(Tema.barraHover());
                g2.fillRoundRect(0, 1, w, h - 2, 10, 10);
            }

            Color fg;
            if (activo || hover) {
                fg = Color.WHITE;
            } else if (esSubitem) {
                fg = Tema.barraTextoSuave();
            } else {
                fg = Tema.barraTexto();
            }

            if (esSubitem) {
                // línea guía vertical bajo el carril de iconos
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillRect(21, 0, 1, h);
            }

            if (glifo != 0) {
                g2.setFont(FontAwesome.solid(14f));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                String s = String.valueOf(glifo);
                int cx = 22 - fm.stringWidth(s) / 2;
                int cy = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, cx, cy);
            }

            if (mostrarTexto()) {
                g2.setFont(Tema.fuenteBase(Font.PLAIN, 13));
                g2.setColor(fg);
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(texto, esSubitem ? 44 : 44, ty);

                if (grupo != null) {
                    g2.setFont(FontAwesome.solid(9f));
                    String flecha = String.valueOf(grupo.abierto
                            ? FontAwesome.CHEVRON_ABAJO : FontAwesome.CHEVRON_DERECHA);
                    FontMetrics fmF = g2.getFontMetrics();
                    int fy = (h - fmF.getHeight()) / 2 + fmF.getAscent();
                    g2.drawString(flecha, w - 20, fy);
                }
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Encabezado con la marca del sistema
    // ------------------------------------------------------------------

    private class Marca extends JComponent {

        Marca() {
            setPreferredSize(new Dimension(10, 66));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int h = getHeight();

            // logo: cuadrado redondeado con degradado y las iniciales
            int lado = 38;
            int x = 13;
            int y = (h - lado) / 2 - 2;
            g2.setPaint(new GradientPaint(x, y, Tema.PRIMARIO,
                    x + lado, y + lado, Tema.PRIMARIO_CLARO));
            g2.fillRoundRect(x, y, lado, lado, 12, 12);

            g2.setColor(Color.WHITE);
            g2.setFont(Tema.fuenteBase(Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            String iniciales = iniciales(nombreSistema);
            g2.drawString(iniciales,
                    x + (lado - fm.stringWidth(iniciales)) / 2,
                    y + (lado - fm.getHeight()) / 2 + fm.getAscent());

            if (mostrarTexto()) {
                g2.setColor(Color.WHITE);
                g2.setFont(Tema.fuenteBase(Font.BOLD, 14));
                g2.drawString(nombreSistema, x + lado + 12, y + 16);
                g2.setColor(Tema.barraTextoSuave());
                g2.setFont(Tema.fuenteBase(Font.PLAIN, 11));
                g2.drawString(subtitulo, x + lado + 12, y + 32);
            }

            // separador inferior
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillRect(0, h - 1, getWidth(), 1);
            g2.dispose();
        }

        private String iniciales(String nombre) {
            StringBuilder sb = new StringBuilder();
            for (String parte : nombre.split("[\\s-]+")) {
                if (!parte.isEmpty() && sb.length() < 2) {
                    sb.append(Character.toUpperCase(parte.charAt(0)));
                }
            }
            return sb.length() == 0 ? "?" : sb.toString();
        }
    }
}
