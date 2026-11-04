package com.pidapues.servicio;

import com.pidapues.excepcion.ApiException;
import com.pidapues.modelo.Carrito;
import com.pidapues.modelo.CarritoProducto;
import com.pidapues.modelo.Cliente;
import com.pidapues.modelo.DetallePedido;
import com.pidapues.modelo.Factura;
import com.pidapues.modelo.Inventario;
import com.pidapues.modelo.Mesa;
import com.pidapues.modelo.Negocio;
import com.pidapues.modelo.Pago;
import com.pidapues.modelo.Pedido;
import com.pidapues.modelo.Producto;
import com.pidapues.repositorio.CarritoProductoRepository;
import com.pidapues.repositorio.CarritoRepository;
import com.pidapues.repositorio.ClienteRepository;
import com.pidapues.repositorio.DetallePedidoRepository;
import com.pidapues.repositorio.FacturaRepository;
import com.pidapues.repositorio.InventarioRepository;
import com.pidapues.repositorio.MesaRepository;
import com.pidapues.repositorio.NegocioRepository;
import com.pidapues.repositorio.PagoRepository;
import com.pidapues.repositorio.PedidoRepository;
import com.pidapues.repositorio.ProductoRepository;
import com.pidapues.repositorio.VistaRepository;
import com.pidapues.util.Datos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reglas del pedido: crearlo (pendiente de pago), verificar el pago con código, flujo de cocina y
 * entrega del mesero. Lee con las vistas y escribe con los repositorios JPA.
 */
@Service
public class PedidoService {

    private static final Logger LOG = LoggerFactory.getLogger(PedidoService.class);

    private static final List<String> METODOS_PAGO = Arrays.asList("efectivo", "tarjeta", "qr");
    private static final int ID_CLIENTE_GENERICO = 1;   // "Consumidor final" (ver schema.sql)
    private static final String DOCUMENTO_GENERICO = "222222222222";
    private static final Pattern CEDULA = Pattern.compile("^[0-9]{5,15}$");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    private final VistaRepository vistas;
    private final ProductoRepository productos;
    private final InventarioRepository inventarios;
    private final ClienteRepository clientes;
    private final MesaRepository mesas;
    private final CarritoRepository carritos;
    private final CarritoProductoRepository carritoProductos;
    private final PedidoRepository pedidos;
    private final DetallePedidoRepository detalles;
    private final PagoRepository pagos;
    private final FacturaRepository facturas;
    private final NegocioRepository negocios;
    private final MesaService mesaService;
    private final EstadoPagos estado;
    private final CorreoService correo;
    private final TransactionTemplate tx;

    public PedidoService(VistaRepository vistas, ProductoRepository productos, InventarioRepository inventarios,
                         ClienteRepository clientes, MesaRepository mesas, CarritoRepository carritos,
                         CarritoProductoRepository carritoProductos, PedidoRepository pedidos,
                         DetallePedidoRepository detalles, PagoRepository pagos, FacturaRepository facturas,
                         NegocioRepository negocios, MesaService mesaService, EstadoPagos estado,
                         CorreoService correo, TransactionTemplate tx) {
        this.vistas = vistas;
        this.productos = productos;
        this.inventarios = inventarios;
        this.clientes = clientes;
        this.mesas = mesas;
        this.carritos = carritos;
        this.carritoProductos = carritoProductos;
        this.pedidos = pedidos;
        this.detalles = detalles;
        this.pagos = pagos;
        this.facturas = facturas;
        this.negocios = negocios;
        this.mesaService = mesaService;
        this.estado = estado;
        this.correo = correo;
        this.tx = tx;
    }

    // =====================================================================
    // Lectura: arma el JSON que consume el frontend
    // =====================================================================

    private static int entero(Object valor) {
        return ((Number) valor).intValue();
    }

    private Map<String, Object> mapearPedido(Map<String, Object> fila, List<Map<String, Object>> lineas, Negocio negocio) {
        boolean pagado = Datos.bool(fila.get("pagado"));
        int idPedido = entero(fila.get("id_pedido"));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> l : lineas) {
            Object obs = l.get("observaciones");
            items.add(Datos.mapa(
                    "productoId", l.get("id_producto"),
                    "cantidad", l.get("cantidad"),
                    "observacion", obs == null || String.valueOf(obs).isEmpty() ? "" : obs));
        }

        Map<String, Object> pedido = Datos.mapa(
                "id", idPedido,
                "mesa", fila.get("num_mesa"),
                "items", items,
                "total", Datos.numero(fila.get("total")),
                "estado", fila.get("estado"),
                "metodoPago", fila.get("metodo_pago"),
                "pagado", pagado,
                "notificadoCocina", estado.fueNotificado(idPedido),
                "fechaHoraRegistro", Datos.iso(fila.get("fecha_pedido")),
                "fechaHoraPago", pagado ? Datos.iso(fila.get("fecha_pago")) : null,
                "fechaHoraInicioPreparacion", Datos.iso(fila.get("hora_inicio_prep")),
                "fechaHoraListo", Datos.iso(fila.get("hora_fin")),
                "fechaHoraEntregado", Datos.iso(fila.get("fecha_entrega")),
                "factura", null);

        if (pagado) {
            Object cliente = fila.get("nombre_cliente");
            pedido.put("factura", Datos.mapa(
                    "numero", fila.get("factura_numero"),
                    "fecha", Datos.iso(fila.get("fecha_pago")),
                    "cliente", cliente == null || String.valueOf(cliente).isEmpty() ? "Consumidor final" : cliente,
                    "documento", vacioANulo(fila.get("documento_cliente")),
                    "correo", vacioANulo(fila.get("correo_cliente")),
                    "razonSocial", negocio == null ? null : negocio.getRazonSocial(),
                    "nit", negocio == null ? null : negocio.getNit(),
                    "direccion", negocio == null ? null : negocio.getDireccion(),
                    "regimen", negocio == null ? null : negocio.getRegimen(),
                    "banco", negocio == null ? null : negocio.getBanco(),
                    "tipoCuenta", negocio == null ? null : negocio.getTipoCuenta(),
                    "numeroCuenta", negocio == null ? null : negocio.getNumeroCuenta(),
                    "titularCuenta", negocio == null ? null : negocio.getTitularCuenta()));
        }
        return pedido;
    }

    private static Object vacioANulo(Object v) {
        return v == null || String.valueOf(v).isEmpty() ? null : v;
    }

    /** Trae las líneas de varios pedidos con UNA sola consulta y arma cada pedido. */
    private List<Map<String, Object>> mapearPedidos(List<Map<String, Object>> filas) {
        List<Map<String, Object>> salida = new ArrayList<>();
        if (filas.isEmpty()) {
            return salida;
        }
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> f : filas) {
            ids.add(entero(f.get("id_pedido")));
        }
        Map<Integer, List<Map<String, Object>>> porPedido = new LinkedHashMap<>();
        for (Map<String, Object> l : vistas.lineasDe(ids)) {
            porPedido.computeIfAbsent(entero(l.get("id_pedido")), k -> new ArrayList<>()).add(l);
        }
        Negocio negocio = negocios.findById(1).orElse(null);
        for (Map<String, Object> f : filas) {
            List<Map<String, Object>> lineas = porPedido.get(entero(f.get("id_pedido")));
            salida.add(mapearPedido(f, lineas == null ? new ArrayList<>() : lineas, negocio));
        }
        return salida;
    }

    /** El pedido listo para el frontend, o null si no existe. */
    public Map<String, Object> pedidoCompleto(int id) {
        Map<String, Object> fila = vistas.pedidoCompleto(id);
        if (fila == null) {
            return null;
        }
        return mapearPedidos(List.of(fila)).get(0);
    }

    public Map<String, Object> pedidoOError(int id) {
        Map<String, Object> pedido = pedidoCompleto(id);
        if (pedido == null) {
            throw new ApiException(404, "Pedido no encontrado.");
        }
        return pedido;
    }

    public List<Map<String, Object>> listarPorRol(String rol) {
        return mapearPedidos(vistas.pedidosPorRol(rol));
    }

    // =====================================================================
    // Creación del pedido (queda "pendiente_pago", NO llega a cocina)
    // =====================================================================

    private static class Linea {
        final int idProducto;
        final int cantidad;
        final BigDecimal precio;
        final BigDecimal subtotal;
        final String observacion;

        Linea(int idProducto, int cantidad, BigDecimal precio, String observacion) {
            this.idProducto = idProducto;
            this.cantidad = cantidad;
            this.precio = precio;
            this.subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
            this.observacion = observacion;
        }
    }

    /** Partes del nombre completo escrito en el formulario de pago: nombre, primer y segundo apellido. */
    private static String[] partirNombre(String nombreCompleto) {
        String[] partes = nombreCompleto.trim().split("\\s+");
        String nombre = partes.length > 0 ? recortar(partes[0], 25) : "";
        String primer = partes.length > 1 ? recortar(partes[1], 25) : null;
        String segundo = null;
        if (partes.length > 2) {
            segundo = recortar(String.join(" ", Arrays.copyOfRange(partes, 2, partes.length)), 25);
        }
        return new String[] {nombre, primer, segundo};
    }

    private static String recortar(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    /**
     * Registra, en una sola transacción: cliente, carrito y sus productos, pedido y detalle, pago
     * pendiente, y pasa la mesa a "pago". Devuelve {pedido, codigoVerificacionDemo}.
     */
    public Map<String, Object> crear(Map<String, Object> body) {
        Object itemsObj = body == null ? null : body.get("items");
        if (!(itemsObj instanceof List) || ((List<?>) itemsObj).isEmpty()) {
            throw new ApiException(400, "El pedido no tiene productos.");
        }
        List<?> items = (List<?>) itemsObj;
        Object metodoObj = body.get("metodoPago");
        if (!(metodoObj instanceof String) || !METODOS_PAGO.contains(metodoObj)) {
            throw new ApiException(400, "Método de pago inválido.");
        }
        final String metodoPago = (String) metodoObj;

        mesaService.liberarInactivas();

        // El total SIEMPRE se calcula aquí con el precio real del producto: nunca se confía en el cliente.
        Map<Integer, Producto> catalogo = new LinkedHashMap<>();
        for (Producto p : productos.findByDisponibleTrue()) {
            catalogo.put(p.getIdProducto(), p);
        }

        List<Linea> lineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int cantidadTotal = 0;
        for (Object o : items) {
            if (!(o instanceof Map)) {
                throw new ApiException(400, "Uno de los productos del pedido no es válido.");
            }
            Map<?, ?> item = (Map<?, ?>) o;
            Integer idProducto = Datos.entero(item.get("productoId"));
            Integer cantidad = Datos.entero(item.get("cantidad"));
            Producto producto = idProducto == null ? null : catalogo.get(idProducto);
            if (producto == null || cantidad == null || cantidad < 1 || cantidad > 99) {
                throw new ApiException(400, "Uno de los productos del pedido no es válido.");
            }
            Object obsObj = item.get("observacion");
            String obs = obsObj == null || String.valueOf(obsObj).isEmpty() ? null : recortar(String.valueOf(obsObj), 100);
            Linea linea = new Linea(idProducto, cantidad, producto.getPrecioUnidad(), obs);
            total = total.add(linea.subtotal);
            cantidadTotal += cantidad;
            lineas.add(linea);
        }

        // Existencias: no se vende más de lo que hay en inventario.
        Map<Integer, Integer> pedidoPorProducto = new LinkedHashMap<>();
        for (Linea l : lineas) {
            pedidoPorProducto.merge(l.idProducto, l.cantidad, Integer::sum);
        }
        for (Inventario inv : inventarios.findAllById(pedidoPorProducto.keySet())) {
            if (inv.getStockActual() < pedidoPorProducto.get(inv.getIdProducto())) {
                throw new ApiException(409, "No hay suficientes existencias de "
                        + catalogo.get(inv.getIdProducto()).getNombreProducto()
                        + " (quedan " + inv.getStockActual() + ").");
            }
        }

        // Datos del cliente para la factura (obligatorios): nombre, cédula y correo.
        Object datosObj = body.get("datosFactura");
        Map<?, ?> d = datosObj instanceof Map ? (Map<?, ?>) datosObj : new LinkedHashMap<>();
        final String nombreCliente = Datos.texto(d.get("nombreCliente")).replaceAll("\\s+", " ");
        final String cedulaCliente = Datos.texto(d.get("cedula"));
        final String correoCliente = Datos.texto(d.get("correo")).toLowerCase(java.util.Locale.ROOT);
        if (nombreCliente.length() < 2 || nombreCliente.length() > 60) {
            throw new ApiException(400, "Escribe tu nombre completo (máximo 60 caracteres).");
        }
        if (!CEDULA.matcher(cedulaCliente).matches()) {
            throw new ApiException(400, "La cédula debe tener solo números (5 a 15 dígitos).");
        }
        if (correoCliente.length() > 50 || !CORREO.matcher(correoCliente).matches()) {
            throw new ApiException(400, "Escribe un correo válido (máximo 50 caracteres).");
        }
        if (!Boolean.TRUE.equals(d.get("consentimiento"))) {
            throw new ApiException(400, "Debes aceptar el tratamiento de tus datos para recibir la factura.");
        }

        final BigDecimal totalFinal = total;
        final int cantidadFinal = cantidadTotal;
        final List<Linea> lineasFinal = lineas;

        Integer pedidoId = tx.execute(status -> {
            // RF011: la mesa NO la elige el cliente. Se asigna la mesa libre de menor número.
            Mesa mesa = mesas.findFirstByEstadoOrderByNumMesaAsc("libre").orElseThrow(() ->
                    new ApiException(409, "No hay mesas disponibles en este momento. Intenta de nuevo en unos minutos."));

            // CLIENTE: se reutiliza el de esa cédula (actualizando sus datos) o se crea uno nuevo.
            String[] n = partirNombre(nombreCliente);
            Cliente cliente = clientes.findFirstByCedulaOrderByIdClienteDesc(cedulaCliente).orElseGet(Cliente::new);
            cliente.setNombre(n[0]);
            cliente.setPrimerApellido(n[1]);
            cliente.setSegundoApellido(n[2]);
            cliente.setCorreoElectronico(correoCliente);
            cliente.setCedula(cedulaCliente);
            cliente = clientes.save(cliente);

            // CARRITO (uno por mesa): se reemplaza cualquier carrito viejo de esa mesa.
            carritos.borrarPorMesa(mesa.getIdMesa());
            Carrito carrito = new Carrito();
            carrito.setIdMesa(mesa.getIdMesa());
            carrito.setIdCliente(cliente.getIdCliente());
            carrito.setResumenPrecio(totalFinal);
            carrito.setCantidad(cantidadFinal);
            carrito = carritos.save(carrito);

            Map<Integer, Integer> cantidadPorProducto = new LinkedHashMap<>();
            for (Linea l : lineasFinal) {
                cantidadPorProducto.merge(l.idProducto, l.cantidad, Integer::sum);
            }
            List<CarritoProducto> filasCarrito = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : cantidadPorProducto.entrySet()) {
                filasCarrito.add(new CarritoProducto(carrito.getIdCarrito(), e.getKey(), e.getValue()));
            }
            carritoProductos.saveAll(filasCarrito);

            // PEDIDO + DETALLE_PEDIDO (la fecha la pone la base de datos)
            Pedido pedido = new Pedido();
            pedido.setIdCliente(cliente.getIdCliente());
            pedido.setIdMesa(mesa.getIdMesa());
            pedido.setTotal(totalFinal);
            pedido.setEstado("pendiente_pago");
            pedido = pedidos.save(pedido);

            List<DetallePedido> filasDetalle = new ArrayList<>();
            for (Linea l : lineasFinal) {
                DetallePedido det = new DetallePedido();
                det.setIdPedido(pedido.getIdPedido());
                det.setIdProducto(l.idProducto);
                det.setObservaciones(l.observacion);
                det.setCantidad(l.cantidad);
                det.setPrecioUnitario(l.precio);
                det.setSubtotal(l.subtotal);
                filasDetalle.add(det);
            }
            detalles.saveAll(filasDetalle);

            // PAGO (pendiente de verificar)
            Pago pago = new Pago();
            pago.setIdPedido(pedido.getIdPedido());
            pago.setMetodoPago(metodoPago);
            pago.setValorTotal(totalFinal);
            pago.setEstadoPago("pendiente");
            pagos.save(pago);

            // RF011: la mesa pasa a "pago" mientras se verifica el pago.
            mesas.cambiarEstado(mesa.getIdMesa(), "pago");
            return pedido.getIdPedido();
        });

        String codigo = estado.guardarCodigo(pedidoId);
        // En una pasarela real este código llegaría por SMS o por la app del banco. Aquí se devuelve
        // para simular esa pantalla, pero nunca reemplaza la verificación: solo el servidor confirma.
        return Datos.mapa("pedido", pedidoOError(pedidoId), "codigoVerificacionDemo", codigo);
    }

    // =====================================================================
    // Verificación segura del pago
    // =====================================================================

    /**
     * Aprueba el pago, genera la factura, pasa el pedido a "pendiente" (visible para cocina), descuenta
     * el inventario, ocupa la mesa y cierra el carrito. Después envía la factura por correo.
     */
    public Map<String, Object> verificarPago(int id, String codigoRecibido) {
        String codigo = codigoRecibido == null ? "" : codigoRecibido.trim();

        Map<String, Object> fila = vistas.pedidoCompleto(id);
        if (fila == null) {
            throw new ApiException(404, "Pedido no encontrado.");
        }
        if (Datos.bool(fila.get("pagado"))) {
            throw new ApiException(409, "Este pedido ya fue verificado.");
        }

        EstadoPagos.CodigoPago esperado = estado.obtener(id);
        if (esperado == null) {
            // El código vive en memoria: si el servidor se reinició hay que pedir uno nuevo.
            throw new ApiException(410, "El código de verificación expiró. Solicita uno nuevo.")
                    .con("intentosRestantes", 0).con("bloqueado", true);
        }
        if (esperado.bloqueado) {
            throw new ApiException(423, "Demasiados intentos fallidos. Solicita un nuevo código.")
                    .con("bloqueado", true);
        }
        if (!codigo.equals(esperado.codigo)) {
            esperado.intentos += 1;
            boolean bloquear = esperado.intentos >= 3;
            esperado.bloqueado = bloquear;
            throw new ApiException(400, bloquear
                    ? "Código incorrecto. Se bloqueó por seguridad; solicita un nuevo código."
                    : "Código incorrecto.")
                    .con("intentosRestantes", Math.max(0, 3 - esperado.intentos))
                    .con("bloqueado", bloquear);
        }

        final Cliente cliente = clientes.findById(entero(fila.get("id_cliente"))).orElseThrow(() ->
                new ApiException(404, "Pedido no encontrado."));
        StringBuilder nombreFactura = new StringBuilder();
        for (String parte : new String[] {cliente.getNombre(), cliente.getPrimerApellido(), cliente.getSegundoApellido()}) {
            if (parte != null && !parte.isEmpty()) {
                if (nombreFactura.length() > 0) {
                    nombreFactura.append(' ');
                }
                nombreFactura.append(parte);
            }
        }
        final String nombreParaFactura = nombreFactura.length() == 0
                ? "Consumidor final" : recortar(nombreFactura.toString(), 60);
        final int idPago = entero(fila.get("id_pago"));
        final int idMesa = entero(fila.get("id_mesa"));
        final BigDecimal total = (BigDecimal) fila.get("total");

        tx.execute(status -> {
            // PAGO -> aprobado (solo si sigue pendiente: evita aprobar dos veces)
            String referencia = recortar("SIM-" + id + "-" + System.currentTimeMillis(), 50);
            if (pagos.aprobar(id, referencia) == 0) {
                throw new ApiException(409, "Este pedido ya fue verificado.");
            }

            // FACTURA
            Factura factura = new Factura();
            factura.setIdPago(idPago);
            factura.setFecha(LocalDate.now());
            factura.setNombreCliente(nombreParaFactura);
            String cedula = cliente.getCedula();
            factura.setDocumentoCliente(cedula == null || cedula.isEmpty() ? DOCUMENTO_GENERICO : cedula);
            String mail = cliente.getCorreoElectronico();
            factura.setCorreoCliente(mail == null || mail.isEmpty() ? null : mail);
            factura.setSubtotal(total);
            factura.setImpuestos(BigDecimal.ZERO);
            factura.setDescuento(BigDecimal.ZERO);
            factura.setTotal(total);
            facturas.save(factura);

            // PEDIDO -> pendiente (ya visible en la vista de cocina)
            pedidos.pasarAPendiente(id);

            // INVENTARIO: se descuentan las unidades vendidas (nunca por debajo de 0).
            for (Object[] consumo : detalles.consumoPorProducto(id)) {
                inventarios.descontar(((Number) consumo[0]).intValue(), ((Number) consumo[1]).intValue());
            }

            // MESA -> ocupada y CARRITO cerrado, solo si la mesa sigue siendo de este pedido.
            Integer ultimo = pedidos.ultimoDeMesa(idMesa);
            if (ultimo != null && ultimo.intValue() == id) {
                mesas.cambiarEstado(idMesa, "ocupada");
                carritos.borrarPorMesa(idMesa);
            }
            return null;
        });

        estado.quitarCodigo(id);
        Map<String, Object> pedidoPagado = pedidoOError(id);

        // Factura por correo. Si el envío falla, el pago y la factura quedan guardados igual.
        String correoCliente = cliente.getCorreoElectronico();
        Map<String, Object> correoFactura;
        if (correoCliente == null || correoCliente.isEmpty()) {
            correoFactura = Datos.mapa("enviado", false, "correo", null, "motivo", "El cliente no tiene correo.");
        } else {
            try {
                Map<String, Object> resultado = correo.enviarFactura(correoCliente,
                        armarFacturaParaCorreo(id, pedidoPagado));
                correoFactura = Datos.mapa("enviado", resultado.get("enviado"), "correo", correoCliente,
                        "motivo", resultado.get("motivo"));
            } catch (Exception e) {
                LOG.error("No se pudo enviar la factura por correo: {}", e.getMessage());
                correoFactura = Datos.mapa("enviado", false, "correo", correoCliente,
                        "motivo", "No se pudo enviar el correo.");
            }
        }

        Map<String, Object> respuesta = new LinkedHashMap<>(pedidoPagado);
        respuesta.put("correoFactura", correoFactura);
        return respuesta;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> armarFacturaParaCorreo(int id, Map<String, Object> pedidoPagado) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> l : vistas.lineasDe(List.of(id))) {
            Object obs = l.get("observaciones");
            items.add(Datos.mapa(
                    "nombre", l.get("nombre_producto"),
                    "cantidad", l.get("cantidad"),
                    "precio", Datos.numero(l.get("precio_unitario")),
                    "subtotal", Datos.numero(l.get("subtotal")),
                    "observacion", obs == null ? "" : obs));
        }
        Map<String, Object> factura = (Map<String, Object>) pedidoPagado.get("factura");
        Negocio negocio = negocios.findById(1).orElse(null);
        return Datos.mapa(
                "numero", factura.get("numero"),
                "fecha", factura.get("fecha"),
                "cliente", factura.get("cliente"),
                "documento", factura.get("documento"),
                "mesa", pedidoPagado.get("mesa"),
                "pedidoId", id,
                "metodoPago", pedidoPagado.get("metodoPago"),
                "total", pedidoPagado.get("total"),
                "razonSocial", negocio == null ? null : negocio.getRazonSocial(),
                "nit", negocio == null ? null : negocio.getNit(),
                "direccion", negocio == null ? null : negocio.getDireccion(),
                "regimen", negocio == null ? null : negocio.getRegimen(),
                "items", items);
    }

    public String reenviarCodigo(int id) {
        Map<String, Object> fila = vistas.pedidoCompleto(id);
        if (fila == null) {
            throw new ApiException(404, "Pedido no encontrado.");
        }
        if (Datos.bool(fila.get("pagado"))) {
            throw new ApiException(409, "Este pedido ya fue verificado.");
        }
        String nuevo = estado.guardarCodigo(id);
        // Extiende el tiempo antes de liberar la mesa por inactividad.
        pagos.tocar(id);
        return nuevo;
    }

    // =====================================================================
    // Cocina, aviso a cocina y entrega
    // =====================================================================

    public Map<String, Object> cambiarEstadoCocina(int id, String nuevoEstado, Integer idEmpleado) {
        if (!"en_preparacion".equals(nuevoEstado) && !"listo".equals(nuevoEstado)) {
            throw new ApiException(400, "Estado no permitido desde cocina.");
        }
        int filas = "en_preparacion".equals(nuevoEstado)
                ? pedidos.iniciarPreparacion(id, idEmpleado)
                : pedidos.marcarListo(id, idEmpleado);

        if (filas == 0) {
            Map<String, Object> fila = vistas.pedidoCompleto(id);
            if (fila == null) {
                throw new ApiException(404, "Pedido no encontrado.");
            }
            if (!Datos.bool(fila.get("pagado"))) {
                throw new ApiException(409, "El pedido aún no tiene el pago verificado.");
            }
            throw new ApiException(409, "No se puede pasar de \"" + fila.get("estado") + "\" a \"" + nuevoEstado + "\".");
        }
        return pedidoOError(id);
    }

    public void marcarNotificado(int id) {
        estado.marcarNotificado(id);
    }

    public Map<String, Object> entregar(int id, Integer idEmpleado) {
        // UPDATE atómico: si dos meseros lo marcan a la vez, solo el primero tiene éxito.
        if (pedidos.entregar(id, idEmpleado) == 0) {
            if (!pedidos.existsById(id)) {
                throw new ApiException(404, "Pedido no encontrado.");
            }
            throw new ApiException(409,
                    "Solo se pueden entregar pedidos en estado \"listo\" (es posible que otro mesero ya lo haya entregado).");
        }
        // La entrega libera la mesa (si todavía es de este pedido).
        Pedido pedido = pedidos.findById(id).orElse(null);
        if (pedido != null) {
            mesaService.liberarMesaDelPedido(pedido.getIdMesa(), id);
        }
        return pedidoOError(id);
    }

    // =====================================================================
    // Demo
    // =====================================================================

    public void limpiarDemo() {
        carritos.borrarTodos();          // limpia carrito_productos en cascada
        pedidos.borrarTodos();           // limpia detalle_pedido, pago y factura en cascada
        vistas.borrarClientesExceptoGenerico(ID_CLIENTE_GENERICO);
        mesas.liberarTodas();
        estado.limpiarTodo();
    }
}
