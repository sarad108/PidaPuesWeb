package com.pidapues.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Conversiones pequeñas para armar el JSON igual que lo hacía la API en Node. */
public final class Datos {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Datos() {
    }

    /** Fecha y hora como "2026-10-27T18:33:00" (null si no hay). Las fechas sin hora quedan "2026-10-27". */
    public static String iso(Object fecha) {
        if (fecha == null) {
            return null;
        }
        if (fecha instanceof LocalDateTime) {
            return ((LocalDateTime) fecha).format(ISO);
        }
        if (fecha instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) fecha).toLocalDateTime().format(ISO);
        }
        if (fecha instanceof java.sql.Date || fecha instanceof java.time.LocalDate) {
            return fecha.toString();
        }
        return String.valueOf(fecha).replace(' ', 'T');
    }

    /** Número JSON: entero si no tiene decimales (18000) y decimal si los tiene (4.7). */
    public static Number numero(Object valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal bd;
        if (valor instanceof BigDecimal) {
            bd = (BigDecimal) valor;
        } else if (valor instanceof Number) {
            bd = new BigDecimal(valor.toString());
        } else {
            try {
                bd = new BigDecimal(String.valueOf(valor).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        BigDecimal limpio = bd.stripTrailingZeros();
        if (limpio.scale() <= 0) {
            return limpio.longValue();
        }
        return limpio.doubleValue();
    }

    /** Equivale a !!valor de JavaScript para columnas 0/1. */
    public static boolean bool(Object valor) {
        if (valor == null) {
            return false;
        }
        if (valor instanceof Boolean) {
            return (Boolean) valor;
        }
        if (valor instanceof Number) {
            return ((Number) valor).doubleValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(valor));
    }

    /** Equivale a Number(x) de JavaScript: null si no es un número válido. */
    public static Double num(Object valor) {
        if (valor == null) {
            return 0.0;
        }
        if (valor instanceof Number) {
            double d = ((Number) valor).doubleValue();
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        }
        if (valor instanceof Boolean) {
            return ((Boolean) valor) ? 1.0 : 0.0;
        }
        String s = String.valueOf(valor).trim();
        if (s.isEmpty()) {
            return 0.0;
        }
        try {
            double d = Double.parseDouble(s);
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** El número como entero, o null si no es un entero (Number.isInteger de JavaScript). */
    public static Integer entero(Object valor) {
        Double d = num(valor);
        if (d == null || d != Math.rint(d) || Math.abs(d) > Integer.MAX_VALUE) {
            return null;
        }
        return d.intValue();
    }

    /** Texto sin espacios sobrantes; "" si es null. */
    public static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    /** Arma un mapa que conserva el orden: mapa("a", 1, "b", 2). */
    public static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            m.put(String.valueOf(pares[i]), pares[i + 1]);
        }
        return m;
    }
}
