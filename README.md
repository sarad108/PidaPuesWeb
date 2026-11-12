# PidaPues

Sistema web de pedidos para restaurantes. PidaPues permite consultar el menú, crear pedidos, registrar y verificar pagos, gestionar la preparación en cocina, controlar las entregas y consultar información administrativa.

## 1. Tecnologías utilizadas

| Tecnología | Uso dentro del proyecto |
|---|---|
| Java 17 | Lenguaje principal del backend |
| Spring Boot 3.3.5 | Framework para ejecutar el servidor y construir la API REST |
| Maven | Administración de dependencias y ejecución del proyecto |
| Spring Data JPA | Acceso a los datos mediante repositorios |
| MySQL | Base de datos relacional |
| HTML | Estructura de las interfaces |
| CSS | Estilos y diseño visual |
| JavaScript | Interactividad y comunicación con la API |
| Chart.js y jsPDF | Gráficos y generación de reportes PDF |

## 2. Módulos del sistema

- **Cliente:** consulta el menú, agrega productos al pedido, registra sus datos, realiza la verificación del pago y consulta la factura.
- **Cocina:** visualiza los pedidos y actualiza su estado durante la preparación.
- **Mesero:** consulta los pedidos listos y registra su entrega.
- **Administrador:** accede a funciones administrativas, reportes e inventario.

## 3. Estructura del repositorio

```text
PidaPuesJava/
├── .env.example
├── .gitignore
├── pom.xml
├── README.md
├── db-migraciones/
│   ├── migracion_inventario.sql
│   └── migracion_resenas.sql
└── src/
    └── main/
        ├── java/com/pidapues/
        │   ├── PidaPuesApplication.java
        │   ├── config/
        │   ├── controlador/
        │   ├── excepcion/
        │   ├── modelo/
        │   ├── repositorio/
        │   ├── servicio/
        │   └── util/
        └── resources/
            ├── application.properties
            ├── db/schema.sql
            └── static/
                ├── index.html
                ├── menu.html
                ├── pago.html
                ├── cocina.html
                ├── mesero.html
                ├── admin.html
                ├── css/
                ├── js/
                └── img/
```

### 3.1 Backend: `src/main/java/com/pidapues`

Esta carpeta contiene la lógica del servidor y está dividida en paquetes según la responsabilidad de cada componente.

| Paquete o archivo | Responsabilidad |
|---|---|
| `PidaPuesApplication.java` | Punto de entrada. Inicia la aplicación Spring Boot y habilita las tareas programadas. |
| `controlador/` | Recibe las solicitudes HTTP de las interfaces y expone los endpoints de la API. |
| `servicio/` | Contiene las reglas del negocio, validaciones y procesos del sistema. |
| `repositorio/` | Gestiona las consultas y operaciones sobre la base de datos mediante JPA. |
| `modelo/` | Define las entidades que representan los datos del sistema y sus relaciones. |
| `config/` | Contiene configuraciones generales, como la configuración de CORS. |
| `excepcion/` | Maneja excepciones y respuestas de error de la API. |
| `util/` | Agrupa funciones auxiliares reutilizables. |

### 3.2 Frontend: `src/main/resources/static`

Aquí se encuentran los archivos que el navegador utiliza para mostrar e interactuar con el sistema.

- **Archivos HTML:** definen las páginas de inicio, menú, pago, cocina, mesero y administración.
- **Carpeta `css/`:** contiene las hojas de estilo que controlan la apariencia de las interfaces.
- **Carpeta `js/`:** contiene los scripts que gestionan las interacciones, validaciones y comunicación con el backend.
- **Carpeta `img/`:** almacena los logos, banners, fondos e imágenes de los productos.

### 3.3 Recursos y base de datos

- `application.properties`: configura el puerto, la conexión a MySQL y el comportamiento de Spring Boot.
- `db/schema.sql`: contiene el esquema SQL de la base de datos.
- `db-migraciones/`: contiene scripts SQL para actualizar aspectos específicos de la estructura de la base de datos.

### 3.4 Archivos de configuración del proyecto

- `pom.xml`: identifica el proyecto y define sus dependencias, versiones y configuración de Maven.
- `.env.example`: sirve como plantilla para las variables de entorno necesarias para ejecutar el proyecto.
- `.gitignore`: indica qué archivos no deben incluirse en el seguimiento de Git, como `.env` y `target/`.

## 4. Arquitectura y funcionamiento por capas

PidaPues utiliza una arquitectura por capas. El recorrido general de una solicitud es:

```text
Usuario / Navegador
        |
        v
Frontend (HTML, CSS y JavaScript)
        |
        v
Controlador (API REST)
        |
        v
Servicio (reglas del negocio)
        |
        v
Repositorio (Spring Data JPA)
        |
        v
Base de datos MySQL
```

**¿Qué sucede en cada etapa?**

1. El usuario realiza una acción desde una de las interfaces.
2. JavaScript envía una solicitud HTTP a un endpoint de la API.
3. El controlador recibe la solicitud y dirige la operación al servicio correspondiente.
4. El servicio ejecuta las validaciones y reglas del negocio.
5. El repositorio consulta, registra o actualiza la información en MySQL.
6. El resultado regresa al controlador y se envía al frontend para actualizar la interfaz.

Por ejemplo, al crear un pedido, el `PedidoController` recibe la solicitud y delega el proceso a `PedidoService`. Este servicio valida y organiza la información y utiliza los repositorios para trabajar con los datos.

## 5. Base de datos

La aplicación utiliza MySQL para almacenar la información del sistema. Entre las entidades del modelo se encuentran:

- Usuarios, roles y empleados.
- Clientes.
- Productos e inventario.
- Mesas.
- Carritos y productos del carrito.
- Pedidos y detalles del pedido.
- Pagos y facturas.
- Reseñas.
- Intentos de acceso y datos del negocio.

Las clases ubicadas en `modelo/` representan las entidades, mientras que las clases de `repositorio/` permiten acceder a sus datos.

## 6. Requisitos para ejecutar el proyecto

- Java 17 o superior.
- Maven.
- MySQL 8 o una versión compatible.
- Un editor de código, como IntelliJ IDEA, Eclipse o Visual Studio Code.

## 7. Configuración y ejecución

### Paso 1. Preparar la base de datos

Verificar que el servicio de MySQL esté iniciado y que se tengan disponibles las credenciales de conexión.

### Paso 2. Configurar las variables de entorno

Crear un archivo `.env` en la raíz del proyecto, tomando como referencia `.env.example`.

Completar los datos de conexión a MySQL y las variables requeridas por la aplicación. Utilizar credenciales propias y no publicar este archivo.

### Paso 3. Ejecutar el proyecto

Abrir una terminal en la carpeta que contiene `pom.xml` y ejecutar:

```bash
mvn spring-boot:run
```

### Paso 4. Abrir la aplicación

Cuando el servidor haya iniciado correctamente, ingresar desde el navegador a:

```text
http://localhost:3000
```

El puerto puede modificarse mediante la variable `PORT`.

### Paso 5. Comprobar el estado del servidor

Se puede consultar el endpoint:

```text
http://localhost:3000/api/salud
```

## 8. Ejemplo del flujo de un pedido

1. El cliente ingresa al menú y selecciona los productos.
2. El sistema registra la información del pedido.
3. El cliente realiza el proceso de pago y verifica el código correspondiente.
4. El pedido queda disponible para el módulo de cocina.
5. Cocina actualiza el estado mientras prepara el pedido.
6. El mesero registra la entrega.
7. El administrador puede consultar información y reportes del sistema.

## 9. Endpoints principales

Los controladores exponen diferentes rutas de la API. Algunos ejemplos encontrados en el proyecto son:

| Controlador | Ruta base o endpoint | Función |
|---|---|---|
| `PedidoController` | `/api/pedidos` | Crear y consultar pedidos, verificar pagos y actualizar estados. |
| `AuthController` | Según sus rutas definidas | Gestionar el acceso del personal. |
| `CatalogoController` | Según sus rutas definidas | Consultar el catálogo de productos. |
| `ReporteController` | Según sus rutas definidas | Gestionar consultas de reportes. |
| `ResenaController` | Según sus rutas definidas | Gestionar reseñas. |
| `SaludController` | `/api/salud` | Consultar el estado del sistema. |

**PidaPues**  
Sistema web para la gestión digital de pedidos en restaurantes.
