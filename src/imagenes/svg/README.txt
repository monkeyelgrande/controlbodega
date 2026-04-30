Coloca aqui los iconos .svg que se usaran con FlatSVGIcon.

Uso desde codigo:
    JButton btn = new JButton("Guardar", Metodos.Iconos.svg("save.svg", 20, 20));

Los SVG se incluyen automaticamente en el JAR (no estan excluidos en
build.classes.excludes) y FlatSVGIcon los resuelve por classpath usando
el prefijo "imagenes/svg/" definido en Metodos.Iconos.

Recomendaciones:
- Usa un solo color (currentColor o fill="#333") para que respondan al tema.
- Tamanos comunes: 16x16, 20x20, 24x24.
- Fuente recomendada: https://www.flaticon.com  o  https://fonts.google.com/icons (descargar SVG).
