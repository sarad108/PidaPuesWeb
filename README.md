# PidaPues

PidaPues es un sistema de pedidos para restaurante. Tiene cuatro pantallas: la del cliente con el menú, la de cocina, la del mesero y la del administrador.

Lo desarrollé con Java y Spring Boot para el servidor, MySQL para la base de datos y HTML, CSS y JavaScript para las pantallas. Esta guía explica, paso a paso, cómo inicializar el proyecto en un computador con Windows. En Mac o Linux los pasos son los mismos.

## Lo que se necesita tener instalado

| Programa | Para qué sirve | Cómo comprobar que está instalado |
|---|---|---|
| Java 17 o superior | Ejecutar el servidor | Escribir `java -version` en la consola |
| Maven | Descargar las librerías y arrancar el proyecto | Escribir `mvn -version` en la consola |
| MySQL 8 con MySQL Workbench | La base de datos | Poder abrir una conexión en Workbench |

## Paso 1. Descomprimir el proyecto

1. Descomprimir el archivo `PidaPues_Java.zip`.
2. Entrar a la carpeta `PidaPuesJava`. Es la que contiene el archivo `pom.xml`.
3. Abrir una consola dentro de esa carpeta. En Windows se puede escribir `cmd` en la barra de direcciones del explorador de archivos y presionar Enter.

## Paso 2. Comprobar que MySQL está encendido

Si en MySQL Workbench se puede abrir la conexión, MySQL ya está encendido. No hay que crear la base de datos a mano: el servidor la crea sola la primera vez que arranca, con todas las tablas, el menú de ejemplo, las 12 mesas y los usuarios del personal.

## Paso 3. archivo `.env`

En este archivo van los datos para conectarse a MySQL y los PIN del personal.

Se abre el archivo `.env` con cualquier editor, por ejemplo VS Code o el Bloc de notas, y se dejan estos datos:

```
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USER=root
DB_PASSWORD=1023629880sa@
DB_NAME=pidapues
PORT=3000

PIN_COCINA=1234
PIN_MESERO=5678
PIN_ADMIN=9999

RESENAS_EJEMPLO=1
AUTO_CREAR_BD=1
```

Qué significa cada dato:

| Dato | Qué poner |
|---|---|
| `DB_HOST` | `127.0.0.1`, que es este mismo computador. |
| `DB_PORT` | `3306`, o el puerto que aparezca en la conexión de Workbench. |
| `DB_USER` | `root`, o el usuario con el que se entra a Workbench. |
| `DB_PASSWORD` | La contraseña de MySQL de este computador. Es la misma que Workbench pide al abrir la conexión. Se escribe sin comillas ni espacios. |
| `DB_NAME` | `pidapues`. No hay que crearla antes, el servidor lo hace. |
| `PORT` | `3000`. Es el puerto en el que se abre la página. Si ya está ocupado, se puede poner otro, por ejemplo `3001`. |
| `PIN_COCINA`, `PIN_MESERO`, `PIN_ADMIN` | El PIN de cada módulo del personal. Se pueden dejar como están. |
| `RESENAS_EJEMPLO` | `1` para que el inicio muestre tres reseñas de ejemplo. Con `0` no se crean. |
| `AUTO_CREAR_BD` | `1` para que el servidor cree las tablas si faltan. |

El único dato que cambia de un computador a otro es `DB_PASSWORD`. Si el archivo se llama `.env.txt` o no está en la misma carpeta que el `pom.xml`, el servidor no lo lee y falla la conexión con MySQL.

## Paso 4. Arrancar el servidor

En la consola, dentro de la carpeta del proyecto:

```
mvn spring-boot:run
```

La primera vez tarda unos minutos porque Maven descarga las librerías. Cuando termine, la consola debe mostrar estos mensajes:

```
PidaPues escuchando en http://localhost:3000
Conexión a MySQL correcta
Vistas y usuarios de acceso listos
```

Esa consola hay que dejarla abierta mientras se use la página.

## Paso 5. Comprobar que todo quedó bien

1. Abrir http://localhost:3000/api/salud. Debe decir que `esquemaCompleto` es `true`.
2. Abrir http://localhost:3000. Debe aparecer el inicio de PidaPues con los platos favoritos y las reseñas.

## Paso 6. Probar la página

**Como cliente.**

1. En el inicio, hacer clic en "Ver menú".
2. Agregar un plato con el botón "Agregar".
3. Hacer clic en "Ir a pagar".
4. Elegir el método de pago, llenar el nombre, la cédula y el correo, y aceptar el tratamiento de datos.
5. Hacer clic en "Continuar a verificación segura".
6. Escribir el código de seis dígitos que aparece en pantalla y hacer clic en "Verificar y confirmar pago". Aparece la factura.

**Como personal del restaurante.** Cada pantalla pide la cédula y el PIN:

| Pantalla | Dirección | Cédula | PIN |
|---|---|---|---|
| Cocina | http://localhost:3000/cocina.html | 1000000001 | 1234 |
| Mesero | http://localhost:3000/mesero.html | 1000000002 | 5678 |
| Administrador | http://localhost:3000/admin.html | 1000000003 | 9999 |

El pedido que se hizo como cliente aparece en cocina. Cuando cocina lo deja listo, el mesero lo marca como entregado. En la pantalla del administrador están los reportes de ventas e inventario.

El administrador también puede entrar a cocina y al mesero con su propio PIN.

## Paso 7. Detener el servidor

En la consola donde está corriendo, presionar `Ctrl + C`. Para volver a arrancarlo basta con repetir el paso 4.

## Si algo falla

| Lo que se ve | Qué pasa y cómo arreglarlo |
|---|---|
| `Access denied` | El usuario o la contraseña del `.env` no coinciden con los de MySQL. Revisar `DB_USER` y `DB_PASSWORD`. |
| `Causa real: Communications link failure` | El servidor no encuentra MySQL. Comprobar que esté encendido y revisar `DB_HOST` y `DB_PORT`. |
| El servidor tarda 30 segundos y dice que no pudo preparar la base | Casi siempre es que falta el archivo `.env` o que la contraseña es incorrecta. |
| `Port 3000 was already in use` | Otro programa usa ese puerto. Cambiar `PORT` en el `.env`, por ejemplo a `3001`, y abrir http://localhost:3001. |
| `mvn` no se reconoce como comando | Maven no está instalado o no está en el PATH del computador. |
| `Unknown database` | Revisar que `DB_NAME` en el `.env` sea `pidapues`. |

## Opcional. Crear la base de datos a mano

No es necesario, pero se puede. En MySQL Workbench se abre el archivo `src/main/resources/db/schema.sql` y se ejecuta completo con el botón del rayo. Ese archivo borra y vuelve a crear las tablas de `pidapues`, así que no debe usarse si la base ya tiene datos que se quieran conservar.

## Comandos extra

Para manejar el personal y la base desde la consola:

```
mvn spring-boot:run -Dspring-boot.run.arguments="--empleados=listar"
mvn spring-boot:run -Dspring-boot.run.arguments="--empleados=crear"
mvn spring-boot:run -Dspring-boot.run.arguments="--empleados=pin --cedula=1000000001"
mvn spring-boot:run -Dspring-boot.run.arguments="--bd=revisar"
mvn spring-boot:run -Dspring-boot.run.arguments="--bd=init"
```

- `--empleados=listar` muestra el personal registrado.
- `--empleados=crear` pregunta los datos y crea un empleado nuevo.
- `--empleados=pin` cambia el PIN de la cédula indicada.
- `--bd=revisar` dice si a la base de datos le falta algo.
- `--bd=init` borra las tablas y las crea de nuevo, después de pedir confirmación.

## Cómo está organizado el proyecto

Cuando alguien hace un pedido, esto es lo que pasa por dentro:

1. El navegador envía la petición a la API.
2. El controlador la recibe y se la pasa al servicio.
3. El servicio revisa las reglas: que el pedido tenga productos, que haya existencias y que el precio sea el real.
4. El repositorio guarda o busca los datos en MySQL con JPA.
5. La respuesta vuelve por el mismo camino hasta el navegador.

Cada capa está en su propia carpeta, dentro de `src/main/java/com/pidapues`:

| Carpeta | Para qué sirve |
|---|---|
| `controlador` | Recibe las peticiones de la web y responde. No toma decisiones. |
| `servicio` | Las reglas del negocio: totales, existencias, código de pago, bloqueo por intentos fallidos y estados del pedido. |
| `repositorio` | Habla con la base de datos. |
| `modelo` | Una clase por cada tabla. |
| `excepcion`, `util`, `config` | Apoyo: mensajes de error, cifrado del PIN y permisos del navegador. |

Las pantallas están en `src/main/resources/static`.