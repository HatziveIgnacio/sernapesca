package cl.sernapesca.laboratorio.validators;

import cl.sernapesca.periodo.ValorPermitido;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Listas de valores permitidos de un período, cargadas desde `valor_permitido`.
 *
 * Encapsula el lookup que antes estaba hardcodeado en los validadores. Si un
 * campo no tiene lista cargada (período sin plantilla extraída), `permite()`
 * devuelve true para no bloquear la validación — la obligatoriedad y las reglas
 * de fecha siguen aplicando, solo se omite la verificación contra catálogo.
 */
public class ReglasDinamicas {

    /** campo → conjunto de valores permitidos, normalizados a minúscula/trim */
    private final Map<String, Set<String>> porCampo;

    private ReglasDinamicas(Map<String, Set<String>> porCampo) {
        this.porCampo = porCampo;
    }

    /** Construye desde las filas de BD de un período. */
    public static ReglasDinamicas desde(List<ValorPermitido> valores) {
        Map<String, Set<String>> mapa = valores.stream().collect(Collectors.groupingBy(
                ValorPermitido::getCampo,
                Collectors.mapping(v -> normaliza(v.getValor()), Collectors.toCollection(HashSet::new))
        ));
        return new ReglasDinamicas(mapa);
    }

    /** Vacío: ningún campo tiene catálogo. Usado cuando no hay plantilla extraída. */
    public static ReglasDinamicas vacio() {
        return new ReglasDinamicas(Map.of());
    }

    /** ¿Este campo tiene catálogo cargado? */
    public boolean tieneCatalogo(String campo) {
        Set<String> s = porCampo.get(campo);
        return s != null && !s.isEmpty();
    }

    /**
     * ¿El valor está permitido para el campo?
     * Si el campo no tiene catálogo cargado, devuelve true (no bloquea).
     */
    public boolean permite(String campo, Object valor) {
        Set<String> permitidos = porCampo.get(campo);
        if (permitidos == null || permitidos.isEmpty()) return true; // sin catálogo → no valida
        if (valor == null) return false;
        return permitidos.contains(normaliza(valor.toString()));
    }

    public boolean estaVacio() {
        return porCampo.isEmpty();
    }

    private static String normaliza(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
