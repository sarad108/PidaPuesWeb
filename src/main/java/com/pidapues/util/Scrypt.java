package com.pidapues.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Hash del PIN con scrypt (RFC 7914), el mismo formato que usaba la versión en Node:
 * {@code scrypt$<sal en hex>$<hash en hex>} con N=16384, r=8, p=1 y 32 bytes de salida.
 * Así los PIN que ya están guardados en la tabla usuario siguen funcionando.
 */
public final class Scrypt {

    private static final int N = 16384;
    private static final int R = 8;
    private static final int P = 1;
    private static final int LONGITUD = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Scrypt() {
    }

    /** Devuelve "scrypt$sal$hash". */
    public static String hashPin(String pin) {
        byte[] bytesSal = new byte[16];
        ALEATORIO.nextBytes(bytesSal);
        String sal = aHex(bytesSal);
        byte[] hash = calcular(String.valueOf(pin), sal);
        return "scrypt$" + sal + "$" + aHex(hash);
    }

    /** Compara un PIN con el hash guardado en usuario.pin. */
    public static boolean verificar(String pin, String guardado) {
        String[] partes = String.valueOf(guardado == null ? "" : guardado).split("\\$");
        if (partes.length != 3 || !"scrypt".equals(partes[0])) {
            return false;
        }
        byte[] esperado;
        try {
            esperado = deHex(partes[2]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] calculado = calcular(String.valueOf(pin == null ? "" : pin).trim(), partes[1]);
        return MessageDigest.isEqual(esperado, calculado);
    }

    // La sal se usa como texto (sus caracteres hexadecimales), igual que hacía Node.
    private static byte[] calcular(String pin, String sal) {
        return scrypt(pin.getBytes(StandardCharsets.UTF_8), sal.getBytes(StandardCharsets.UTF_8), N, R, P, LONGITUD);
    }

    public static byte[] scrypt(byte[] clave, byte[] sal, int n, int r, int p, int longitud) {
        byte[] b = pbkdf2Una(clave, sal, p * 128 * r);
        int[] v = new int[32 * r * n];
        for (int i = 0; i < p; i++) {
            smix(b, i * 128 * r, r, n, v);
        }
        return pbkdf2Una(clave, b, longitud);
    }

    // PBKDF2-HMAC-SHA256 con una sola iteración, como pide scrypt.
    private static byte[] pbkdf2Una(byte[] clave, byte[] sal, int longitud) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // Una clave vacía no se puede pasar a SecretKeySpec; una de ceros da el mismo HMAC.
            byte[] k = clave.length == 0 ? new byte[1] : clave;
            mac.init(new SecretKeySpec(k, "HmacSHA256"));
            byte[] salida = new byte[longitud];
            int bloques = (longitud + 31) / 32;
            int pos = 0;
            for (int i = 1; i <= bloques; i++) {
                mac.update(sal);
                mac.update(new byte[] {(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8), (byte) i});
                byte[] t = mac.doFinal();
                int copiar = Math.min(32, longitud - pos);
                System.arraycopy(t, 0, salida, pos, copiar);
                pos += copiar;
            }
            return salida;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void smix(byte[] b, int inicio, int r, int n, int[] v) {
        int palabras = 32 * r;
        int[] x = new int[palabras];
        int[] y = new int[palabras];
        for (int i = 0; i < palabras; i++) {
            int o = inicio + i * 4;
            x[i] = (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8) | ((b[o + 2] & 0xff) << 16) | ((b[o + 3] & 0xff) << 24);
        }
        for (int i = 0; i < n; i++) {
            System.arraycopy(x, 0, v, i * palabras, palabras);
            bloqueMix(x, y, r);
        }
        for (int i = 0; i < n; i++) {
            int j = x[(2 * r - 1) * 16] & (n - 1);
            for (int k = 0; k < palabras; k++) {
                x[k] ^= v[j * palabras + k];
            }
            bloqueMix(x, y, r);
        }
        for (int i = 0; i < palabras; i++) {
            int o = inicio + i * 4;
            b[o] = (byte) x[i];
            b[o + 1] = (byte) (x[i] >>> 8);
            b[o + 2] = (byte) (x[i] >>> 16);
            b[o + 3] = (byte) (x[i] >>> 24);
        }
    }

    private static void bloqueMix(int[] b, int[] y, int r) {
        int[] x = new int[16];
        System.arraycopy(b, (2 * r - 1) * 16, x, 0, 16);
        for (int i = 0; i < 2 * r; i++) {
            for (int k = 0; k < 16; k++) {
                x[k] ^= b[i * 16 + k];
            }
            salsa208(x);
            int destino = (i % 2 == 0) ? (i / 2) * 16 : (r + i / 2) * 16;
            System.arraycopy(x, 0, y, destino, 16);
        }
        System.arraycopy(y, 0, b, 0, 32 * r);
    }

    private static int rot(int a, int b) {
        return (a << b) | (a >>> (32 - b));
    }

    private static void salsa208(int[] b) {
        int[] x = b.clone();
        for (int i = 0; i < 8; i += 2) {
            x[4] ^= rot(x[0] + x[12], 7);
            x[8] ^= rot(x[4] + x[0], 9);
            x[12] ^= rot(x[8] + x[4], 13);
            x[0] ^= rot(x[12] + x[8], 18);
            x[9] ^= rot(x[5] + x[1], 7);
            x[13] ^= rot(x[9] + x[5], 9);
            x[1] ^= rot(x[13] + x[9], 13);
            x[5] ^= rot(x[1] + x[13], 18);
            x[14] ^= rot(x[10] + x[6], 7);
            x[2] ^= rot(x[14] + x[10], 9);
            x[6] ^= rot(x[2] + x[14], 13);
            x[10] ^= rot(x[6] + x[2], 18);
            x[3] ^= rot(x[15] + x[11], 7);
            x[7] ^= rot(x[3] + x[15], 9);
            x[11] ^= rot(x[7] + x[3], 13);
            x[15] ^= rot(x[11] + x[7], 18);
            x[1] ^= rot(x[0] + x[3], 7);
            x[2] ^= rot(x[1] + x[0], 9);
            x[3] ^= rot(x[2] + x[1], 13);
            x[0] ^= rot(x[3] + x[2], 18);
            x[6] ^= rot(x[5] + x[4], 7);
            x[7] ^= rot(x[6] + x[5], 9);
            x[4] ^= rot(x[7] + x[6], 13);
            x[5] ^= rot(x[4] + x[7], 18);
            x[11] ^= rot(x[10] + x[9], 7);
            x[8] ^= rot(x[11] + x[10], 9);
            x[9] ^= rot(x[8] + x[11], 13);
            x[10] ^= rot(x[9] + x[8], 18);
            x[12] ^= rot(x[15] + x[14], 7);
            x[13] ^= rot(x[12] + x[15], 9);
            x[14] ^= rot(x[13] + x[12], 13);
            x[15] ^= rot(x[14] + x[13], 18);
        }
        for (int i = 0; i < 16; i++) {
            b[i] += x[i];
        }
    }

    private static String aHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte x : bytes) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    private static byte[] deHex(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hex inválido");
        }
        byte[] r = new byte[hex.length() / 2];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return r;
    }
}
