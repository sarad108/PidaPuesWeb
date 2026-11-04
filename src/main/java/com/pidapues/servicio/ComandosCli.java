package com.pidapues.servicio;

import com.pidapues.modelo.Empleado;
import com.pidapues.modelo.Rol;
import com.pidapues.modelo.Usuario;
import com.pidapues.repositorio.EmpleadoRepository;
import com.pidapues.repositorio.RolRepository;
import com.pidapues.repositorio.UsuarioRepository;
import com.pidapues.util.Scrypt;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Comandos de consola (reemplazan a scripts/empleados.js y scripts/inicializar-bd.js):
 *   --empleados=crear | pin | activar | desactivar | listar   (con --cedula=... en pin, activar, desactivar)
 *   --bd=revisar | init                                       (init pide confirmar; --si para no preguntar)
 */
@Component
public class ComandosCli implements ApplicationRunner {

    private final EmpleadoRepository empleados;
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final EsquemaService esquema;
    private final TransactionTemplate tx;

    public ComandosCli(EmpleadoRepository empleados, UsuarioRepository usuarios, RolRepository roles,
                       EsquemaService esquema, TransactionTemplate tx) {
        this.empleados = empleados;
        this.usuarios = usuarios;
        this.roles = roles;
        this.esquema = esquema;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean comandoEmpleados = args.containsOption("empleados");
        boolean comandoBd = args.containsOption("bd");
        if (!comandoEmpleados && !comandoBd) {
            return;
        }
        int codigo = 0;
        try {
            if (comandoEmpleados) {
                ejecutarEmpleados(valor(args, "empleados"), valor(args, "cedula"));
            } else {
                ejecutarBd(valor(args, "bd"), args.containsOption("si"));
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            for (String linea : EsquemaService.explicarError(e)) {
                System.err.println("   " + linea);
            }
            codigo = 1;
        }
        System.exit(codigo);
    }

    private static String valor(ApplicationArguments args, String opcion) {
        List<String> v = args.getOptionValues(opcion);
        return v == null || v.isEmpty() ? "" : v.get(0);
    }

    // ------------------------------------------------------------------ base de datos

    private void ejecutarBd(String comando, boolean sinPreguntar) throws Exception {
        String base = esquema.nombreBd();
        EsquemaService.Diagnostico antes = esquema.diagnosticar();
        if ("revisar".equals(comando)) {
            System.out.println("Base de datos: " + base);
            System.out.println(antes.completo ? "Esquema completo."
                    : "Faltan tablas: " + antes.tablasFaltantes + " | vistas: " + antes.vistasFaltantes);
            if (antes.tieneEsquemaAntiguo) {
                System.out.println("Se detectaron tablas del esquema antiguo (productos, pedidos, pedido_items...).");
            }
            return;
        }
        if (!"init".equals(comando)) {
            System.out.println("Comandos: --bd=revisar | --bd=init [--si]");
            return;
        }
        if (!sinPreguntar) {
            System.out.println("Esto BORRA y vuelve a crear las tablas del modelo relacional en la base '" + base
                    + "' (pedidos, pagos, clientes...).");
            String r = preguntar("¿Continuar? escribe 'si' para confirmar: ").toLowerCase(Locale.ROOT);
            if (!r.equals("si") && !r.equals("sí")) {
                System.out.println("Cancelado. No se cambió nada.");
                return;
            }
        }
        esquema.aplicarEsquema(false);
        EsquemaService.Diagnostico despues = esquema.diagnosticar();
        System.out.println(despues.completo
                ? "Base de datos creada: tablas, vistas, productos, mesas y usuarios de ejemplo."
                : "Quedaron faltando: " + despues.tablasFaltantes + " " + despues.vistasFaltantes);
    }

    // ------------------------------------------------------------------ empleados

    private void ejecutarEmpleados(String comando, String cedula) {
        switch (comando) {
            case "crear":
                crear();
                break;
            case "pin":
                cambiarPin(cedula);
                break;
            case "desactivar":
                cambiarActivo(cedula, false);
                break;
            case "activar":
                cambiarActivo(cedula, true);
                break;
            case "listar":
                listar();
                break;
            default:
                System.out.println("Comandos: --empleados=crear | pin --cedula=X | desactivar --cedula=X | activar --cedula=X | listar");
        }
    }

    private static String preguntar(String texto) {
        System.out.print(texto);
        Scanner entrada = new Scanner(System.in);
        return entrada.hasNextLine() ? entrada.nextLine().trim() : "";
    }

    private static String nombreRolDb(String texto) {
        switch (texto) {
            case "cocina":
                return "Cocina";
            case "mesero":
                return "Mesero";
            case "administrador":
            case "admin":
                return "Administrador";
            default:
                return null;
        }
    }

    private void crear() {
        String nombre = preguntar("Primer nombre: ");
        String apellido = preguntar("Primer apellido: ");
        String cedula = preguntar("Cédula: ");
        String correo = preguntar("Correo electrónico: ");
        String rolTxt = preguntar("Rol (cocina / mesero / administrador): ").toLowerCase(Locale.ROOT);
        String pin = preguntar("PIN (4 a 8 dígitos): ");

        if (nombre.isEmpty() || apellido.isEmpty()) {
            throw new IllegalArgumentException("El nombre y el apellido son obligatorios.");
        }
        if (!cedula.matches("[A-Za-z0-9]{5,15}")) {
            throw new IllegalArgumentException("La cédula debe tener entre 5 y 15 letras o números.");
        }
        if (!correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$") || correo.length() > 50) {
            throw new IllegalArgumentException("Correo inválido (máximo 50 caracteres).");
        }
        String nombreRol = nombreRolDb(rolTxt);
        if (nombreRol == null) {
            throw new IllegalArgumentException("Rol inválido. Usa: cocina, mesero o administrador.");
        }
        if (!pin.matches("[0-9]{4,8}")) {
            throw new IllegalArgumentException("El PIN debe tener entre 4 y 8 dígitos.");
        }

        Rol rol = roles.findByNombreRol(nombreRol).orElseThrow(() ->
                new IllegalStateException("No existe el rol " + nombreRol + " en la base de datos."));
        try {
            tx.execute(status -> {
                // nombre_usuario = la cédula (único y fácil de relacionar con la persona)
                Usuario u = new Usuario();
                u.setNombreUsuario(cedula);
                u.setContrasena("!");
                u.setPin(Scrypt.hashPin(pin));
                u.setFechaRegistro(LocalDate.now());
                u.setCorreoElectronico(correo);
                u = usuarios.saveAndFlush(u);

                Empleado e = new Empleado();
                e.setIdUsuario(u.getIdUsuario());
                e.setIdRol(rol.getIdRol());
                e.setPrimerNombre(nombre.length() > 25 ? nombre.substring(0, 25) : nombre);
                e.setPrimerApellido(apellido.length() > 25 ? apellido.substring(0, 25) : apellido);
                e.setCedula(cedula);
                e.setCorreoElectronico(correo);
                e.setActivo(true);
                empleados.saveAndFlush(e);
                return null;
            });
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ya existe un empleado/usuario con esa cédula o ese correo.");
        }
        System.out.println("Empleado creado: " + nombre + " " + apellido + " (" + nombreRol
                + "). Entra con su cédula y su PIN.");
    }

    private void cambiarPin(String cedula) {
        if (cedula.isEmpty()) {
            throw new IllegalArgumentException("Falta la cédula: --empleados=pin --cedula=XXXXX");
        }
        Empleado emp = empleados.findByCedula(cedula).orElseThrow(() ->
                new IllegalArgumentException("No existe un empleado con esa cédula."));
        String pin = preguntar("Nuevo PIN (4 a 8 dígitos): ");
        if (!pin.matches("[0-9]{4,8}")) {
            throw new IllegalArgumentException("El PIN debe tener entre 4 y 8 dígitos.");
        }
        Usuario u = usuarios.findById(emp.getIdUsuario()).orElseThrow(() ->
                new IllegalStateException("El empleado no tiene usuario."));
        u.setPin(Scrypt.hashPin(pin));
        usuarios.save(u);
        System.out.println("PIN actualizado.");
    }

    private void cambiarActivo(String cedula, boolean activo) {
        if (cedula.isEmpty()) {
            throw new IllegalArgumentException("Falta la cédula: --cedula=XXXXX");
        }
        Optional<Empleado> emp = empleados.findByCedula(cedula);
        if (emp.isEmpty()) {
            throw new IllegalArgumentException("No existe un empleado con esa cédula.");
        }
        emp.get().setActivo(activo);
        empleados.save(emp.get());
        System.out.println(activo ? "Empleado activado." : "Empleado desactivado: ya no puede entrar.");
    }

    private void listar() {
        System.out.println(String.format("%-16s %-32s %-16s %s", "cedula", "nombre", "rol", "activo"));
        for (Empleado e : empleados.findAllByOrderByIdEmpleado()) {
            String rol = roles.findById(e.getIdRol()).map(Rol::getNombreRol).orElse("?");
            System.out.println(String.format("%-16s %-32s %-16s %s", e.getCedula(),
                    e.getPrimerNombre() + " " + e.getPrimerApellido(), rol,
                    Boolean.TRUE.equals(e.getActivo()) ? "sí" : "no"));
        }
    }
}
