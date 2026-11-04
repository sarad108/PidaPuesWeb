package com.pidapues.servicio;

import com.pidapues.excepcion.ApiException;
import com.pidapues.modelo.Empleado;
import com.pidapues.modelo.IntentoAcceso;
import com.pidapues.modelo.Rol;
import com.pidapues.modelo.Usuario;
import com.pidapues.repositorio.EmpleadoRepository;
import com.pidapues.repositorio.IntentoAccesoRepository;
import com.pidapues.repositorio.RolRepository;
import com.pidapues.repositorio.UsuarioRepository;
import com.pidapues.util.Datos;
import com.pidapues.util.Scrypt;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Acceso por cédula y PIN personal para Cocina, Mesero y Admin.
 * Al validar el PIN se entrega un token de sesión que vive en memoria del servidor (se pierde si se
 * reinicia) y viaja en el header x-pidapues-token. Protege tanto las pantallas como los endpoints.
 */
@Service
public class AuthService {

    public static final List<String> ROLES = List.of("cocina", "mesero", "admin");
    public static final int MINUTOS_BLOQUEO = 5;

    private static final int HORAS_EXPIRACION = 12;
    private static final int INTENTOS_MAXIMOS = 3;
    private static final String CABECERA_TOKEN = "x-pidapues-token";

    /** Lo que el servidor recuerda de cada token. */
    public static class Sesion {
        public final String rol;
        public final long expira;
        public final Integer idEmpleado;
        public final Integer idUsuario;

        Sesion(String rol, long expira, Integer idEmpleado, Integer idUsuario) {
            this.rol = rol;
            this.expira = expira;
            this.idEmpleado = idEmpleado;
            this.idUsuario = idUsuario;
        }
    }

    private static class Intentos {
        int fallidos = 0;
        long bloqueadoHasta = 0;
    }

    private final UsuarioRepository usuarios;
    private final EmpleadoRepository empleados;
    private final RolRepository roles;
    private final IntentoAccesoRepository intentosDb;
    private final Environment entorno;

    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Sesion> tokens = new ConcurrentHashMap<>();
    private final Map<String, Intentos> intentos = new HashMap<>();

    public AuthService(UsuarioRepository usuarios, EmpleadoRepository empleados, RolRepository roles,
                       IntentoAccesoRepository intentosDb, Environment entorno) {
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.roles = roles;
        this.intentosDb = intentosDb;
        this.entorno = entorno;
    }

    // ------------------------------------------------------------------ login

    public Map<String, Object> login(Map<String, Object> body) {
        Object rolObj = body == null ? null : body.get("rol");
        if (!(rolObj instanceof String) || !ROLES.contains(rolObj)) {
            throw new ApiException(400, "Rol inválido.");
        }
        String rol = (String) rolObj;
        String cedula = Datos.texto(body.get("cedula"));
        String pin = Datos.texto(body.get("pin"));
        if (!cedula.matches("[A-Za-z0-9]{5,15}") || pin.isEmpty()) {
            throw new ApiException(400, "Ingresa tu cédula y tu PIN.");
        }

        // NF004: bloqueo tras 3 intentos fallidos consecutivos (por cédula).
        String clave = "ced:" + cedula;
        if (estaBloqueado(clave)) {
            throw new ApiException(423, "Acceso bloqueado por intentos fallidos. Intenta de nuevo en "
                    + minutosRestantesBloqueo(clave) + " minuto(s).");
        }

        Optional<Empleado> empleado = empleados.findByCedulaAndActivoTrue(cedula);
        Usuario usuario = null;
        if (empleado.isPresent()) {
            usuario = usuarios.findById(empleado.get().getIdUsuario()).orElse(null);
        }

        boolean exito = usuario != null && Scrypt.verificar(pin, usuario.getPin());
        int restantes = registrarIntento(clave, exito);

        // NF004: se registra fecha y hora de cada intento de un empleado que existe.
        if (usuario != null) {
            IntentoAcceso intento = new IntentoAcceso();
            intento.setIdUsuario(usuario.getIdUsuario());
            intento.setExitoso(exito);
            intentosDb.save(intento);
        }

        if (!exito) {
            if (restantes == 0) {
                throw new ApiException(423, "Datos incorrectos. Se bloqueó el acceso por seguridad; intenta de nuevo en "
                        + MINUTOS_BLOQUEO + " minutos.");
            }
            // Mismo mensaje exista o no la cédula, para no revelar qué cédulas están registradas.
            throw new ApiException(401, "Cédula o PIN incorrectos.").con("intentosRestantes", restantes);
        }

        Empleado emp = empleado.get();
        String nombreRolDb = roles.findById(emp.getIdRol()).map(Rol::getNombreRol).orElse("");
        String rolReal = rolDeNombreDb(nombreRolDb);
        if (rolReal == null || (!rolReal.equals("admin") && !rolReal.equals(rol))) {
            throw new ApiException(403, "Tu cuenta no tiene acceso a este módulo.");
        }

        String token = crearToken(rolReal, emp.getIdEmpleado(), emp.getIdUsuario());
        return Datos.mapa("token", token, "rol", rolReal,
                "nombre", emp.getPrimerNombre() + " " + emp.getPrimerApellido());
    }

    public static String rolDeNombreDb(String nombreRolDb) {
        if ("Cocina".equals(nombreRolDb)) {
            return "cocina";
        }
        if ("Mesero".equals(nombreRolDb)) {
            return "mesero";
        }
        if ("Administrador".equals(nombreRolDb)) {
            return "admin";
        }
        return null;
    }

    // ------------------------------------------------------------------ sesiones

    private String crearToken(String rol, Integer idEmpleado, Integer idUsuario) {
        long ahora = System.currentTimeMillis();
        tokens.values().removeIf(s -> s.expira < ahora);
        byte[] bytes = new byte[24];
        aleatorio.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String token = sb.toString();
        tokens.put(token, new Sesion(rol, ahora + HORAS_EXPIRACION * 60L * 60 * 1000, idEmpleado, idUsuario));
        return token;
    }

    /**
     * Exige un token válido. Si se pasan roles, el token debe ser de uno de ellos (el admin siempre
     * tiene acceso: en un restaurante real puede resolver cualquier pantalla).
     */
    public Sesion exigir(HttpServletRequest peticion, String... rolesPermitidos) {
        String token = peticion.getHeader(CABECERA_TOKEN);
        Sesion sesion = token == null ? null : tokens.get(token);
        if (sesion == null || sesion.expira < System.currentTimeMillis()) {
            throw new ApiException(401, "Sesión inválida o expirada. Ingresa el PIN nuevamente.");
        }
        boolean permitido = "admin".equals(sesion.rol) || rolesPermitidos.length == 0
                || Arrays.asList(rolesPermitidos).contains(sesion.rol);
        if (!permitido) {
            throw new ApiException(403, "No tienes permiso para acceder a este módulo.");
        }
        return sesion;
    }

    // ------------------------------------------------------------------ bloqueo por intentos (NF004)

    private synchronized boolean estaBloqueado(String clave) {
        Intentos d = intentos.get(clave);
        if (d == null || d.bloqueadoHasta == 0) {
            return false;
        }
        if (d.bloqueadoHasta < System.currentTimeMillis()) {
            // El bloqueo ya expiró: se libera y se reinicia el contador.
            intentos.remove(clave);
            return false;
        }
        return true;
    }

    private synchronized int minutosRestantesBloqueo(String clave) {
        Intentos d = intentos.get(clave);
        if (d == null || d.bloqueadoHasta == 0) {
            return 0;
        }
        return (int) Math.max(1, Math.ceil((d.bloqueadoHasta - System.currentTimeMillis()) / 60000.0));
    }

    /** Registra el resultado de un intento y devuelve cuántos intentos quedan (0 si se bloqueó). */
    private synchronized int registrarIntento(String clave, boolean exito) {
        if (exito) {
            intentos.remove(clave);
            return INTENTOS_MAXIMOS;
        }
        Intentos d = intentos.computeIfAbsent(clave, k -> new Intentos());
        d.fallidos += 1;
        if (d.fallidos >= INTENTOS_MAXIMOS) {
            d.bloqueadoHasta = System.currentTimeMillis() + MINUTOS_BLOQUEO * 60L * 1000;
            return 0;
        }
        return INTENTOS_MAXIMOS - d.fallidos;
    }

    // ------------------------------------------------------------------ PIN de las cuentas de módulo

    /**
     * Guarda en usuario.pin el hash de PIN_COCINA, PIN_MESERO y PIN_ADMIN (.env) para las cuentas de
     * módulo sembradas en schema.sql. Solo escribe cuando el PIN cambió.
     */
    public void sincronizarPines() {
        for (String rol : ROLES) {
            String pin = entorno.getProperty("PIN_" + rol.toUpperCase(java.util.Locale.ROOT));
            if (pin == null || pin.isBlank()) {
                continue;
            }
            Optional<Usuario> usuario = usuarios.findByNombreUsuario(rol);
            if (usuario.isPresent() && !Scrypt.verificar(pin, usuario.get().getPin())) {
                usuario.get().setPin(Scrypt.hashPin(pin));
                usuarios.save(usuario.get());
            }
        }
    }
}
