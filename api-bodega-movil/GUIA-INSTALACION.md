# Guía de instalación — API Bodega Móvil + App Android

Guía paso a paso para instalar el sistema en el servidor de **una empresa nueva**.
Incluye: backend como servicio Windows 24/7, configuración por empresa y la app
en los celulares.

> Arquitectura: la **app Android** (celulares por WiFi) habla con la **API**
> (Spring Boot, puerto 8080) que corre en el **servidor**, y la API habla con
> **PostgreSQL** en ese mismo servidor (`localhost`). Los celulares nunca tocan
> la base directamente.

---

## 0. Datos que necesitas tener a mano (por empresa)

Antes de empezar, anota:

- **Nombre de la base** de bodega en ese servidor (ej. `bodega_nuevo`).
- **Usuario y clave** de PostgreSQL (normalmente `postgres` / la clave de esa empresa).
- **IP del servidor** en la red local (en el servidor: `ipconfig` → IPv4, ej. `192.168.5.117`).
- Que en el servidor estén instalados **PostgreSQL** (con la base restaurada) y **Java 8**.

---

## 1. Verificar Java en el servidor

En el servidor, abre **PowerShell** y ejecuta:

```powershell
(Get-Command java).Source
```

Anota la ruta que devuelva, p. ej.:
`C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe`

> Si no devuelve nada, hay que instalar Java 8 (Temurin/Adoptium JDK 8) en el servidor.

---

## 2. Aplicar las migraciones de base de datos (¡importante!)

El módulo de QR necesita una tabla nueva. **Una sola vez por base**, en el servidor:

```powershell
$env:PGPASSWORD = "LA_CLAVE_DE_POSTGRES"
& "C:\Program Files\PostgreSQL\9.4\bin\psql.exe" -h localhost -p 5432 -U postgres -d NOMBRE_DE_LA_BASE -v ON_ERROR_STOP=1 -f "ruta\a\migracion_escaneos_qr_ordenes.sql"
```

(Ajusta la ruta de `psql.exe` a la versión instalada y el nombre de la base.)
Debe terminar sin errores (varias líneas `DO` y `COMMENT`). Es idempotente: si
se corre dos veces no pasa nada.

> El script `migracion_escaneos_qr_ordenes.sql` está en la carpeta del proyecto.
> (El otro, `migracion_ajuste_pendientes.sql`, no es necesario para la app móvil.)

---

## 3. Generar el JAR (en el equipo de desarrollo)

1. Abrir el proyecto `api-bodega-movil` en NetBeans.
2. Clic derecho en el proyecto → **Clean and Build**.
3. El jar queda en:
   `api-bodega-movil\target\api-bodega-movil-0.0.1.jar`

Ese mismo jar sirve para **todas** las empresas; lo único que cambia por empresa
es el archivo `application.properties`.

---

## 4. Crear la carpeta en el servidor y copiar archivos

En el servidor, crea la carpeta **`D:\apimovil`** y copia adentro estos 4 archivos:

| Archivo | De dónde sale |
|---|---|
| `api-bodega-movil-0.0.1.jar` | del paso 3 (`target\`) |
| `bodega-api.exe` | WinSW (ver paso 5) |
| `bodega-api.xml` | crearlo con el contenido del paso 6 |
| `application.properties` | crearlo con el contenido del paso 7 |

La carpeta debe quedar exactamente así:

```
D:\apimovil\
   api-bodega-movil-0.0.1.jar
   bodega-api.exe
   bodega-api.xml
   application.properties
```

---

## 5. Descargar WinSW (el motor del servicio)

1. Ir a: https://github.com/winsw/winsw/releases
2. Descargar **`WinSW-x64.exe`** (la versión más reciente).
3. **Renombrarlo** a `bodega-api.exe`.
4. Copiarlo a `D:\apimovil\`.

> El `.exe` y el `.xml` deben tener el **mismo nombre base** (`bodega-api`) y estar
> **en la misma carpeta**. WinSW busca su `.xml` al lado del `.exe`.

---

## 6. Crear `D:\apimovil\bodega-api.xml`

Con el Bloc de notas, crear el archivo (al guardar: "Tipo: Todos los archivos",
para que NO quede `bodega-api.xml.txt`). Contenido:

```xml
<service>
  <id>bodegaapi</id>
  <name>Bodega Movil API</name>
  <description>API REST para la app movil de bodega (ordenes, entregas, inventario).</description>

  <executable>C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe</executable>
  <arguments>-jar api-bodega-movil-0.0.1.jar</arguments>
  <workingdirectory>D:\apimovil</workingdirectory>

  <startmode>Automatic</startmode>

  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <resetfailure>1 hour</resetfailure>

  <log mode="roll-by-size">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>8</keepFiles>
  </log>
</service>
```

⚠️ En `<executable>` pon la **ruta REAL de java** que obtuviste en el **paso 1**
(con sus espacios y `\` tal cual, sin comillas). Si la ruta es distinta en ese
servidor, cámbiala aquí.

---

## 7. Crear `D:\apimovil\application.properties`

Con el Bloc de notas. Aquí va lo específico de **esta empresa**:

```properties
server.port=8080

spring.datasource.url=jdbc:postgresql://localhost:5432/NOMBRE_DE_LA_BASE?tcpKeepAlive=true
spring.datasource.username=postgres
spring.datasource.password=LA_CLAVE_DE_POSTGRES
spring.datasource.driver-class-name=org.postgresql.Driver

spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=10000

app.token.secret=UNA-CADENA-LARGA-Y-UNICA-PARA-ESTA-EMPRESA
app.token.ttl-minutes=43200

logging.level.org.springframework=WARN
logging.level.com.bodega=INFO
```

Reemplaza:
- `NOMBRE_DE_LA_BASE` → la base de bodega de esa empresa (ej. `bodega_nuevo`).
- `LA_CLAVE_DE_POSTGRES` → la clave real de Postgres del servidor.
- `app.token.secret` → una frase larga e inventada, **distinta por empresa**.

> `localhost` es correcto: la API y Postgres están en el mismo servidor.
> `app.token.ttl-minutes=43200` = 30 días (la sesión de la app se mantiene).

---

## 8. Probar a mano antes de instalar el servicio

En PowerShell:

```powershell
cd D:\apimovil
java -jar api-bodega-movil-0.0.1.jar
```

- Si aparece **`Started ApiBodegaMovilApplication in X seconds`** → la config está
  bien. Presiona **Ctrl + C** para detenerlo y sigue al paso 9.
- Si sale un error (de PostgreSQL u otro), revisa la base/clave en
  `application.properties` y vuelve a intentar.

---

## 9. Instalar e iniciar el servicio

PowerShell **como Administrador**, en `D:\apimovil`:

```powershell
cd D:\apimovil
.\bodega-api.exe install
.\bodega-api.exe start
```

---

## 10. Verificar

```powershell
Get-Service bodegaapi | Select-Object Status
netstat -ano | findstr :8080
Invoke-RestMethod http://localhost:8080/api/health/db
try { (Invoke-WebRequest http://localhost:8080/api/auth/me -UseBasicParsing).StatusCode } catch { $_.Exception.Response.StatusCode.value__ }
```

Esperado:
- `Status` → **Running**
- algo **LISTENING** en `:8080`
- `health/db` → `status: UP` y `totalUsuarios` con un número
- el último → **401** (backend al día)

---

## 11. Abrir el firewall y probar autoarranque

1. En el **Firewall de Windows** del servidor: abrir el **puerto 8080 de entrada**.
2. **Reiniciar el servidor** y, sin tocar nada, ejecutar:
   ```powershell
   Invoke-RestMethod http://localhost:8080/api/health
   ```
   Debe responder `UP` solo (el servicio arrancó con Windows).

---

## 12. Instalar la app en los celulares

1. Pasar el **APK firmado** (`app-release.apk`) al celular (USB, WhatsApp, correo…).
2. En el celular: permitir **instalar apps de origen desconocido** para esa app.
3. Abrir la app. En la pantalla de login:
   - **Servidor**: la **IP del servidor** de esa empresa (ej. `192.168.5.117`).
   - **Usuario / Contraseña**: los del sistema de bodega.
4. El celular debe estar en el **WiFi de esa empresa**.

La sesión queda guardada (30 días); botón **Salir** para cerrar sesión.

---

## 13. Mantenimiento

**Actualizar el backend a una versión nueva:**
```powershell
cd D:\apimovil
.\bodega-api.exe stop
# reemplazar api-bodega-movil-0.0.1.jar por el nuevo
.\bodega-api.exe start
```

**Comandos del servicio** (PowerShell como Administrador, en `D:\apimovil`):

| Acción | Comando |
|---|---|
| Detener | `.\bodega-api.exe stop` |
| Iniciar | `.\bodega-api.exe start` |
| Reiniciar | `.\bodega-api.exe restart` |
| Quitar servicio | `.\bodega-api.exe stop` y `.\bodega-api.exe uninstall` |

**Logs:** en `D:\apimovil\bodega-api.wrapper.log` (y `.out.log` / `.err.log` si los hay).

**APK / actualizaciones de la app:** generar el APK firmado siempre con el
**MISMO keystore** (guárdalo con sus contraseñas en lugar seguro; sin él no se
pueden actualizar las apps ya instaladas). Subir `versionCode` en
`android-app\app\build.gradle` en cada versión nueva.

---

## 14. Problemas comunes (ya vividos)

| Síntoma | Causa | Solución |
|---|---|---|
| `Port 8080 was already in use` | Hay otra instancia (jar a mano o servicio) | Detener la otra; solo una cosa usa el 8080 |
| Servicio "started" pero 8080 no responde; en `wrapper.log`: *"no puede encontrar el archivo especificado"* | Ruta de `java.exe` mal en `bodega-api.xml` | Poner la ruta real (`(Get-Command java).Source`) |
| App: "error 404 / verifica la IP" al reabrir | El jar del servidor es viejo (sin `/api/auth/me`) | Regenerar el jar (Clean and Build) y reemplazarlo |
| API no arranca, error de PostgreSQL | `application.properties` con base/clave/host equivocados | Host = `localhost`; revisar nombre de base y clave |
| Doble clic al jar y queda "fantasma" | Se ejecuta sin ventana (`javaw`) | Usar el servicio; no doble clic |

**Verificación rápida de que el backend está al día:**
```powershell
try { (Invoke-WebRequest http://localhost:8080/api/auth/me -UseBasicParsing).StatusCode } catch { $_.Exception.Response.StatusCode.value__ }
```
`401` = al día ✅ · `404` = jar viejo, regenerarlo.
