// bienvenida.js
// Lo que hace el inicio: botones al menú, reseñas, favoritos y el formulario de contacto.

document.addEventListener("DOMContentLoaded", () => {

  /* botones que llevan al menú */
  document.querySelectorAll("[data-ir-menu]").forEach((boton) => {
    boton.addEventListener("click", () => {
      document.body.classList.add("saliendo");
      setTimeout(() => { window.location.href = "menu.html"; }, 400);
    });
  });

  /* pintar las estrellas */
  function pintarEstrellas(contenedor, nota) {
    const llenas = Math.round(nota);
    contenedor.textContent = "";
    for (let i = 1; i <= 5; i++) {
      const estrella = document.createElement("span");
      estrella.className = "estrella" + (i <= llenas ? " on" : "");
      estrella.textContent = "★";
      contenedor.appendChild(estrella);
    }
    contenedor.setAttribute("title", `${nota} de 5`);
  }

  const TITULAR = { 5: "¡Excelente!", 4: "Muy bueno", 3: "Estuvo bien", 2: "Podría mejorar", 1: "No me gustó" };

  function tarjetaResena(r) {
    const card = document.createElement("article");
    card.className = "resena-card";

    const titular = document.createElement("h3");
    titular.className = "resena-card-titular";
    titular.textContent = TITULAR[r.calificacion] || "Reseña";

    const cita = document.createElement("blockquote");
    cita.className = "resena-card-texto";
    cita.textContent = `“${r.comentario}”`;

    const estrellas = document.createElement("div");
    estrellas.className = "estrellas";
    pintarEstrellas(estrellas, r.calificacion);
    estrellas.setAttribute("role", "img");
    estrellas.setAttribute("aria-label", `${r.calificacion} de 5 estrellas`);

    const autor = document.createElement("p");
    autor.className = "resena-card-autor";
    autor.textContent = `— ${r.nombre}`;

    card.append(titular, cita, estrellas, autor);
    return card;
  }

  /* traer las reseñas de la base de datos */
  async function cargarResenas() {
    const grid = document.getElementById("resenasGrid");
    try {
      const datos = await PidaPuesDatos.obtenerResenas();

      if (datos.total > 0 && datos.promedio != null) {
        const nota = datos.promedio.toFixed(1).replace(".", ",");
        const textoTotal = `${datos.total} ${datos.total === 1 ? "reseña" : "reseñas"}`;

        document.getElementById("resumenNota").textContent = nota;
        pintarEstrellas(document.getElementById("resumenEstrellas"), datos.promedio);
        document.getElementById("resumenTotal").textContent = textoTotal;
        document.getElementById("resenasResumen").hidden = false;

        document.getElementById("heroNota").textContent = nota;
        document.getElementById("heroTotal").textContent = textoTotal;
        pintarEstrellas(document.getElementById("heroEstrellas"), datos.promedio);
        document.getElementById("heroPrueba").hidden = false;
      }

      grid.textContent = "";
      if (datos.resenas.length === 0) {
        const vacio = document.createElement("p");
        vacio.className = "resenas-vacio";
        vacio.textContent = "Aún no hay reseñas. ¡Haz tu pedido y sé el primero en dejar la tuya!";
        grid.appendChild(vacio);
        return;
      }
      datos.resenas.forEach((r) => grid.appendChild(tarjetaResena(r)));
    } catch {
      grid.textContent = "";
      const aviso = document.createElement("p");
      aviso.className = "resenas-vacio";
      aviso.textContent = "No pudimos cargar las reseñas en este momento.";
      grid.appendChild(aviso);
    }
  }

  /* datos de contacto y whatsapp (salen de config-sitio.js) */
  const SITIO = window.PIDAPUES_SITIO || {};

  function enlaceWhatsapp(mensaje) {
    return `https://wa.me/${SITIO.whatsapp || ""}?text=${encodeURIComponent(mensaje || SITIO.mensajeWhatsApp || "")}`;
  }

  function texto(id, valor) {
    const el = document.getElementById(id);
    if (el && valor) el.textContent = valor;
  }

  function enlace(id, href, etiqueta) {
    const el = document.getElementById(id);
    if (!el) return;
    el.href = href;
    if (etiqueta) el.textContent = etiqueta;
  }

  function aplicarContacto(direccion) {
    texto("cHorario", SITIO.horario);
    texto("pHorario", SITIO.horario);
    if (direccion) { texto("cDireccion", direccion); texto("pDireccion", direccion); }
    const tel = (SITIO.telefono || "").replace(/\s+/g, "");
    ["cTelefono", "pTelefono"].forEach((id) => enlace(id, `tel:${tel}`, SITIO.telefono));
    ["cCorreo", "pCorreo"].forEach((id) => enlace(id, `mailto:${SITIO.correo}`, SITIO.correo));
    ["btnWhatsapp", "waFlotante"].forEach((id) => enlace(id, enlaceWhatsapp()));
    const redes = SITIO.redes || {};
    enlace("redInstagram", redes.instagram || "#");
    enlace("redFacebook", redes.facebook || "#");
    enlace("redTiktok", redes.tiktok || "#");
    texto("anioActual", String(new Date().getFullYear()));
  }

  aplicarContacto(null);

  /* platos favoritos: saco 4 del menú */
  const dinero = (n) => "$" + Number(n).toLocaleString("es-CO");

  function tarjetaFavorito(producto) {
    const card = document.createElement("article");
    card.className = "favorito";

    const circulo = document.createElement("div");
    circulo.className = "favorito-img";
    const emoji = document.createElement("span");
    emoji.className = "favorito-emoji";
    emoji.setAttribute("aria-hidden", "true");
    emoji.textContent = producto.emoji || "🍽️";
    circulo.appendChild(emoji);
    if (producto.imagenUrl) {
      const img = document.createElement("img");
      img.alt = producto.nombre;
      img.loading = "lazy";
      img.addEventListener("load", () => circulo.classList.add("con-foto"));
      img.addEventListener("error", () => img.remove());
      img.src = producto.imagenUrl;
      circulo.appendChild(img);
    }

    const nombre = document.createElement("h3");
    nombre.textContent = producto.nombre;
    const descripcion = document.createElement("p");
    descripcion.textContent = producto.descripcion;

    const pie = document.createElement("div");
    pie.className = "favorito-pie";
    const precio = document.createElement("span");
    precio.className = "favorito-precio";
    precio.textContent = dinero(producto.precio);
    const pedir = document.createElement("button");
    pedir.type = "button";
    pedir.className = "btn-pedir";
    pedir.textContent = "Pedir";
    pedir.setAttribute("aria-label", `Pedir ${producto.nombre}: ir al menú`);
    pedir.addEventListener("click", () => {
      document.body.classList.add("saliendo");
      setTimeout(() => { window.location.href = "menu.html"; }, 400);
    });
    pie.append(precio, pedir);

    card.append(circulo, nombre, descripcion, pie);
    return card;
  }

  async function cargarFavoritos() {
    const grid = document.getElementById("favoritosGrid");
    try {
      await PidaPuesDatos.listo;
      aplicarContacto(PidaPuesDatos.datosNegocio && PidaPuesDatos.datosNegocio.direccion);

      const disponibles = PidaPuesDatos.productos.filter((p) => !p.agotado);
      // agarro uno de cada categoría para que no sean todos de comida
      const porCategoria = {};
      disponibles.forEach((p) => { (porCategoria[p.categoria] = porCategoria[p.categoria] || []).push(p); });
      const colas = Object.values(porCategoria);
      const elegidos = [];
      while (elegidos.length < 4 && colas.some((c) => c.length)) {
        colas.forEach((c) => { if (c.length && elegidos.length < 4) elegidos.push(c.shift()); });
      }

      grid.textContent = "";
      if (elegidos.length === 0) {
        document.getElementById("favoritos").hidden = true;
        return;
      }
      elegidos.forEach((p) => grid.appendChild(tarjetaFavorito(p)));
    } catch {
      // si no hay conexión con el servidor escondo la sección
      document.getElementById("favoritos").hidden = true;
    }
  }

  /* formulario de contacto (es una simulación, no manda nada) */
  const form = document.getElementById("formContacto");
  const exito = document.getElementById("contactoExito");

  function hoyISO() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }

  if (form) {
    const campoMotivo = form.elements.motivo;
    const bloqueReserva = document.getElementById("camposReserva");
    form.elements.fecha.min = hoyISO();

    function actualizarReserva() {
      bloqueReserva.hidden = campoMotivo.value !== "Reserva";
    }
    campoMotivo.addEventListener("change", actualizarReserva);
    actualizarReserva();

    function marcarError(nombre, mensaje) {
      const aviso = form.querySelector(`[data-error="${nombre}"]`);
      if (aviso) aviso.textContent = mensaje || "";
      const campo = form.elements[nombre] && form.elements[nombre].closest(".campo");
      if (campo) campo.classList.toggle("invalido", Boolean(mensaje));
    }

    function validar() {
      let valido = true;
      const nombre = form.elements.nombre.value.trim();
      const celular = form.elements.celular.value.replace(/\s+/g, "");
      marcarError("nombre", nombre.length >= 2 ? "" : "Escribe tu nombre.");
      marcarError("celular", /^\d{7,15}$/.test(celular) ? "" : "Escribe un celular válido (solo números).");
      if (nombre.length < 2 || !/^\d{7,15}$/.test(celular)) valido = false;

      if (campoMotivo.value === "Reserva") {
        const fecha = form.elements.fecha.value;
        const personas = Number(form.elements.personas.value);
        const fechaOk = fecha && fecha >= hoyISO();
        const personasOk = Number.isInteger(personas) && personas >= 1 && personas <= 20;
        marcarError("fecha", fechaOk ? "" : "Elige una fecha desde hoy.");
        marcarError("personas", personasOk ? "" : "Entre 1 y 20 personas.");
        if (!fechaOk || !personasOk) valido = false;
      } else {
        marcarError("fecha", "");
        marcarError("personas", "");
      }
      return valido;
    }

    form.addEventListener("submit", (evento) => {
      evento.preventDefault();
      if (!validar()) return;

      const nombre = form.elements.nombre.value.trim();
      const celular = form.elements.celular.value.trim();
      const motivo = campoMotivo.value;
      const personas = Number(form.elements.personas.value);
      const fechaLarga = form.elements.fecha.value
        ? new Date(form.elements.fecha.value + "T00:00:00").toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long" })
        : "";
      const mensajeExtra = form.elements.mensaje.value.trim();

      const boton = form.querySelector(".btn-enviar");
      boton.disabled = true;
      boton.textContent = "Enviando…";

      setTimeout(() => {
        // aquí no se envía nada, solo muestro el mensaje de gracias
        const resumen = motivo === "Reserva"
          ? `Recibimos tu solicitud de reserva para ${personas} ${personas === 1 ? "persona" : "personas"} el ${fechaLarga}. Te contactaremos al ${celular} para confirmarla.`
          : `Recibimos tu mensaje (${motivo.toLowerCase()}). Te responderemos al ${celular}.`;
        document.getElementById("exitoNombre").textContent = nombre;
        document.getElementById("exitoResumen").textContent = resumen + " (Simulación: no se envió ningún dato).";
        const textoWa = motivo === "Reserva"
          ? `Hola PidaPues, soy ${nombre}. Quisiera reservar una mesa para ${personas} ${personas === 1 ? "persona" : "personas"} el ${fechaLarga}.${mensajeExtra ? " " + mensajeExtra : ""}`
          : `Hola PidaPues, soy ${nombre}. ${motivo}: ${mensajeExtra || "quisiera más información."}`;
        document.getElementById("btnWhatsappExito").href = enlaceWhatsapp(textoWa);
        form.hidden = true;
        exito.hidden = false;
        exito.scrollIntoView({ behavior: "smooth", block: "center" });
        boton.disabled = false;
        boton.textContent = "Enviar mensaje";
      }, 900);
    });

    document.getElementById("btnOtroMensaje").addEventListener("click", () => {
      form.reset();
      form.elements.personas.value = 2;
      actualizarReserva();
      form.hidden = false;
      exito.hidden = true;
      form.elements.nombre.focus();
    });
  }

  cargarFavoritos();
  cargarResenas();
});

