// =========================================
// PidaPues — auth.js
// Acceso por PIN para Cocina, Mesero y Admin. Guarda el token en
// sessionStorage (se pierde al cerrar la pestaña, así que cada turno
// de trabajo vuelve a pedir el PIN) y lo agrega automáticamente a
// todas las peticiones que hace datos.js.
// =========================================

const PidaPuesAuth = (() => {

  const CLAVE_TOKEN = "pidapues_token";
  const CLAVE_ROL = "pidapues_rol";
  const CLAVE_NOMBRE = "pidapues_nombre";

  function obtenerToken() {
    return sessionStorage.getItem(CLAVE_TOKEN);
  }

  function obtenerRol() {
    return sessionStorage.getItem(CLAVE_ROL);
  }

  function guardarSesion(token, rol, nombre) {
    sessionStorage.setItem(CLAVE_TOKEN, token);
    sessionStorage.setItem(CLAVE_ROL, rol);
    if (nombre) sessionStorage.setItem(CLAVE_NOMBRE, nombre);
  }

  function cerrarSesion() {
    sessionStorage.removeItem(CLAVE_TOKEN);
    sessionStorage.removeItem(CLAVE_ROL);
    sessionStorage.removeItem(CLAVE_NOMBRE);
  }

  async function iniciarSesion(rol, cedula, pin) {
    const respuesta = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ rol, cedula, pin }),
    });
    const cuerpo = await respuesta.json().catch(() => null);
    if (!respuesta.ok) {
      throw new Error((cuerpo && cuerpo.error) || "No se pudo iniciar sesión.");
    }
    guardarSesion(cuerpo.token, cuerpo.rol, cuerpo.nombre);
    return cuerpo;
  }

  // Encabezado que datos.js agrega a cada petición a la API.
  function encabezadosAuth() {
    const token = obtenerToken();
    return token ? { "x-pidapues-token": token } : {};
  }

  // Bloquea la pantalla con un formulario de PIN hasta que se ingrese
  // el correcto para `rolRequerido`. Si ya hay una sesión válida para
  // ese rol (o para admin), resuelve de inmediato.
  function exigirAcceso(rolRequerido, nombreModulo) {
    return new Promise((resolve) => {
      const rolActual = obtenerRol();
      if (obtenerToken() && (rolActual === rolRequerido || rolActual === "admin")) {
        return resolve();
      }

      const overlay = document.createElement("div");
      overlay.className = "auth-overlay";
      overlay.innerHTML = `
        <form class="auth-caja" autocomplete="off">
          <h2>🔒 Acceso — ${nombreModulo}</h2>
          <p>Ingresa tu cédula y tu PIN personal para continuar.</p>
          <input type="text" inputmode="numeric" class="auth-input auth-cedula" placeholder="Cédula" maxlength="15" autofocus />
          <input type="password" inputmode="numeric" class="auth-input auth-pin" placeholder="PIN" />
          <button type="submit" class="auth-boton">Entrar</button>
          <p class="auth-error"></p>
        </form>
      `;
      document.body.appendChild(overlay);

      const form = overlay.querySelector("form");
      const inputCedula = overlay.querySelector(".auth-cedula");
      const input = overlay.querySelector(".auth-pin");
      const error = overlay.querySelector(".auth-error");

      form.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        error.classList.remove("visible");
        try {
          await iniciarSesion(rolRequerido, inputCedula.value.trim(), input.value);
          overlay.remove();
          resolve();
        } catch (e) {
          error.textContent = e.message;
          error.classList.add("visible");
          input.value = "";
          (inputCedula.value ? input : inputCedula).focus();
        }
      });
    });
  }

  return { iniciarSesion, exigirAcceso, obtenerToken, obtenerRol, cerrarSesion, encabezadosAuth };
})();

// `const` en un script clásico NO crea una propiedad de window, y datos.js
// busca window.PidaPuesAuth para mandar el token en cada petición. Sin esta
// línea el token nunca se enviaba y cocina/mesero/admin siempre daban 401.
window.PidaPuesAuth = PidaPuesAuth;
