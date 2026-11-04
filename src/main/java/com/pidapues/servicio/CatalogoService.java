package com.pidapues.servicio;

import com.pidapues.excepcion.ApiException;
import com.pidapues.modelo.Negocio;
import com.pidapues.repositorio.NegocioRepository;
import com.pidapues.repositorio.VistaRepository;
import com.pidapues.util.Datos;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Menú de productos y datos fiscales del negocio. */
@Service
public class CatalogoService {

    private static final String[][] EMOJI_POR_PALABRA = {
        {"hamburguesa", "🍔"}, {"pizza", "🍕"}, {"salchipapa", "🍟"}, {"perro", "🌭"},
        {"alitas", "🍗"}, {"gaseosa", "🥤"}, {"limonada", "🍋"}, {"brownie", "🍫"},
    };

    private final VistaRepository vistas;
    private final NegocioRepository negocios;

    public CatalogoService(VistaRepository vistas, NegocioRepository negocios) {
        this.vistas = vistas;
        this.negocios = negocios;
    }

    // La tabla producto no guarda emoji: se deduce para tener un respaldo visual si la foto no carga.
    static String emojiDe(String nombre, String categoria) {
        String n = String.valueOf(nombre == null ? "" : nombre).toLowerCase(Locale.ROOT);
        for (String[] par : EMOJI_POR_PALABRA) {
            if (n.contains(par[0])) {
                return par[1];
            }
        }
        if ("bebida".equals(categoria)) {
            return "🥤";
        }
        if ("postre".equals(categoria)) {
            return "🍰";
        }
        return "🍽️";
    }

    public List<Map<String, Object>> productos() {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (Map<String, Object> p : vistas.productosMenu()) {
            String categoria = String.valueOf(p.get("nombre_categoria")).toLowerCase(Locale.ROOT);
            Number stock = Datos.numero(p.get("stock_actual"));
            salida.add(Datos.mapa(
                    "id", p.get("id_producto"),
                    "categoria", categoria,
                    "nombre", p.get("nombre_producto"),
                    "descripcion", p.get("descripcion"),
                    "precio", Datos.numero(p.get("precio_unidad")),
                    "emoji", emojiDe(String.valueOf(p.get("nombre_producto")), categoria),
                    "imagen_url", p.get("url_imagen"),
                    "imagenUrl", p.get("url_imagen"),
                    "stock", stock,
                    "agotado", stock != null && stock.doubleValue() <= 0));
        }
        return salida;
    }

    public Negocio negocioONulo() {
        return negocios.findById(1).orElse(null);
    }

    public Map<String, Object> negocio() {
        Negocio n = negocioONulo();
        if (n == null) {
            throw new ApiException(404, "No hay datos del negocio configurados.");
        }
        return Datos.mapa(
                "razonSocial", n.getRazonSocial(),
                "nit", n.getNit(),
                "direccion", n.getDireccion(),
                "regimen", n.getRegimen(),
                "banco", n.getBanco(),
                "tipoCuenta", n.getTipoCuenta(),
                "numeroCuenta", n.getNumeroCuenta(),
                "titularCuenta", n.getTitularCuenta());
    }
}
