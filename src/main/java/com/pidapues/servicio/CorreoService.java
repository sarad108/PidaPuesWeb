package com.pidapues.servicio;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.pidapues.util.Datos;

/**
 * Envío de la factura por correo (Gmail / SMTP). Configuración en .env:
 * GMAIL_USER y GMAIL_APP_PASSWORD (contraseña de aplicación, NO tu clave normal).
 * Si falta la configuración el sistema sigue funcionando: el pago se guarda y la pantalla avisa
 * que el correo no salió.
 */
@Service
public class CorreoService {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final Environment entorno;
    private JavaMailSenderImpl remitente;

    public CorreoService(Environment entorno) {
        this.entorno = entorno;
    }

    private String usuario() {
        return entorno.getProperty("GMAIL_USER", "").trim();
    }

    private String clave() {
        return entorno.getProperty("GMAIL_APP_PASSWORD", "").replaceAll("\\s+", "");
    }

    public boolean estaConfigurado() {
        return !usuario().isEmpty() && !clave().isEmpty();
    }

    private synchronized JavaMailSenderImpl obtenerRemitente() {
        if (remitente == null) {
            JavaMailSenderImpl s = new JavaMailSenderImpl();
            s.setHost("smtp.gmail.com");
            s.setPort(465);
            s.setUsername(usuario());
            s.setPassword(clave());
            s.setDefaultEncoding("UTF-8");
            Properties p = s.getJavaMailProperties();
            p.put("mail.transport.protocol", "smtp");
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.ssl.enable", "true");
            p.put("mail.smtp.connectiontimeout", "10000");
            p.put("mail.smtp.timeout", "15000");
            p.put("mail.smtp.writetimeout", "15000");
            remitente = s;
        }
        return remitente;
    }

    private static String esc(Object texto) {
        return String.valueOf(texto == null ? "" : texto)
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String pesos(Object valor) {
        double v = valor instanceof Number ? ((Number) valor).doubleValue() : 0;
        return "$" + NumberFormat.getIntegerInstance(COLOMBIA).format(Math.round(v));
    }

    private static String fechaLegible(Object iso) {
        if (iso == null) {
            return "";
        }
        try {
            return LocalDateTime.parse(String.valueOf(iso)).format(DateTimeFormatter.ofPattern("d/M/yy, h:mm a", COLOMBIA));
        } catch (DateTimeParseException e) {
            return String.valueOf(iso);
        }
    }

    private static String etiquetaMetodo(Object metodo) {
        if ("efectivo".equals(metodo)) {
            return "Efectivo";
        }
        if ("tarjeta".equals(metodo)) {
            return "Tarjeta";
        }
        if ("qr".equals(metodo)) {
            return "Código QR";
        }
        return String.valueOf(metodo);
    }

    private static String razonSocial(Map<String, Object> f) {
        Object r = f.get("razonSocial");
        return r == null || String.valueOf(r).isEmpty() ? "PidaPues" : String.valueOf(r);
    }

    @SuppressWarnings("unchecked")
    private static String construirHtml(Map<String, Object> f) {
        StringBuilder filas = new StringBuilder();
        for (Map<String, Object> i : (List<Map<String, Object>>) f.get("items")) {
            Object obs = i.get("observacion");
            filas.append("\n    <tr>\n      <td style=\"padding:6px 8px;border-bottom:1px solid #eee\">")
                    .append(esc(i.get("nombre")));
            if (obs != null && !String.valueOf(obs).isEmpty()) {
                filas.append("<br><small style=\"color:#888\">Obs: ").append(esc(obs)).append("</small>");
            }
            filas.append("</td>\n      <td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:center\">")
                    .append(esc(i.get("cantidad")))
                    .append("</td>\n      <td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:right\">")
                    .append(pesos(i.get("precio")))
                    .append("</td>\n      <td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:right\">")
                    .append(pesos(i.get("subtotal")))
                    .append("</td>\n    </tr>");
        }
        return "\n  <div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;color:#2b2118\">\n"
                + "    <h2 style=\"margin-bottom:0\">" + esc(razonSocial(f)) + "</h2>\n"
                + "    <p style=\"margin:2px 0;color:#666\">NIT: " + esc(f.get("nit")) + " · " + esc(f.get("direccion"))
                + "<br>" + esc(f.get("regimen")) + "</p>\n    <hr>\n"
                + "    <p><b>Factura de venta " + esc(f.get("numero")) + "</b><br>" + esc(fechaLegible(f.get("fecha"))) + "</p>\n"
                + "    <p><b>Cliente:</b> " + esc(f.get("cliente")) + "<br>\n"
                + "       <b>Documento:</b> " + esc(f.get("documento")) + "<br>\n"
                + "       <b>Mesa:</b> " + esc(f.get("mesa")) + " · <b>Pedido:</b> #" + esc(f.get("pedidoId")) + "<br>\n"
                + "       <b>Método de pago:</b> " + esc(etiquetaMetodo(f.get("metodoPago"))) + "</p>\n"
                + "    <table style=\"width:100%;border-collapse:collapse;font-size:14px\">\n"
                + "      <thead><tr style=\"background:#fff0e6\">\n"
                + "        <th style=\"padding:6px 8px;text-align:left\">Producto</th>\n"
                + "        <th style=\"padding:6px 8px\">Cant.</th>\n"
                + "        <th style=\"padding:6px 8px;text-align:right\">Precio</th>\n"
                + "        <th style=\"padding:6px 8px;text-align:right\">Subtotal</th>\n"
                + "      </tr></thead>\n      <tbody>" + filas + "</tbody>\n    </table>\n"
                + "    <p style=\"text-align:right;font-size:18px\"><b>Total pagado: " + pesos(f.get("total")) + "</b></p>\n"
                + "    <p style=\"font-size:12px;color:#888\">Documento generado electrónicamente por PidaPues como soporte de la transacción. Prototipo con fines demostrativos.</p>\n"
                + "  </div>";
    }

    @SuppressWarnings("unchecked")
    private static String construirTexto(Map<String, Object> f) {
        StringBuilder lineas = new StringBuilder();
        for (Map<String, Object> i : (List<Map<String, Object>>) f.get("items")) {
            if (lineas.length() > 0) {
                lineas.append("\n");
            }
            lineas.append("- ").append(i.get("cantidad")).append("x ").append(i.get("nombre")).append("  ")
                    .append(pesos(i.get("subtotal")));
        }
        return "Factura " + f.get("numero") + "\nCliente: " + f.get("cliente") + " (" + f.get("documento") + ")\n"
                + "Mesa " + f.get("mesa") + " · Pedido #" + f.get("pedidoId") + "\n\n" + lineas
                + "\n\nTotal pagado: " + pesos(f.get("total"));
    }

    /** Devuelve {enviado, motivo}. Lanza una excepción solo si el SMTP falla. */
    public Map<String, Object> enviarFactura(String para, Map<String, Object> factura) throws Exception {
        if (!estaConfigurado()) {
            return Datos.mapa("enviado", false, "motivo", "El envío de correos no está configurado en el servidor.");
        }
        JavaMailSenderImpl s = obtenerRemitente();
        MimeMessage mensaje = s.createMimeMessage();
        MimeMessageHelper ayuda = new MimeMessageHelper(mensaje, true, "UTF-8");
        ayuda.setFrom(new InternetAddress(usuario(), razonSocial(factura), "UTF-8"));
        ayuda.setTo(para);
        ayuda.setSubject("Factura " + factura.get("numero") + " — " + razonSocial(factura));
        ayuda.setText(construirTexto(factura), construirHtml(factura));
        s.send(mensaje);
        return Datos.mapa("enviado", true, "motivo", null);
    }
}
