package com.agendamentos.equadras.specification;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import com.agendamentos.equadras.util.TextoUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class QuadraSpecifications {

    private static final String COM_ACENTO = "áàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String SEM_ACENTO = "aaaaaeeeeiiiiooooouuuucn";
    private static final char ESCAPE_LIKE = '\\';
    private static final double RAIO_TERRA_KM = 6371.0;
    private static final double KM_POR_GRAU = 111.0;

    private QuadraSpecifications() {}

    public static Specification<Quadra> ativa() {
        return (root, query, cb) -> cb.isTrue(root.get("ativa"));
    }

    public static Specification<Quadra> doAdmin(Long adminId) {
        if (adminId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("admin").get("id_usuario"), adminId);
    }

    /**
     * Quadras dentro do raio, ordenadas da mais próxima para a mais distante.
     * A bounding box usa o índice idx_quadras_lat_lng; o Haversine refina no próprio SQL.
     * A ordem só é aplicada na consulta de dados, nunca na de contagem da paginação.
     */
    public static Specification<Quadra> dentroDoRaio(double latitude, double longitude, double raioKm) {
        double deltaLat = raioKm / KM_POR_GRAU;
        double cosLat = Math.cos(Math.toRadians(latitude));
        double deltaLng = (Math.abs(cosLat) > 0.0001) ? raioKm / (KM_POR_GRAU * Math.abs(cosLat)) : deltaLat;

        return (root, query, cb) -> {
            Expression<Double> lat = root.get("latitude");
            Expression<Double> lng = root.get("longitude");
            Expression<Double> distancia = distanciaKm(cb, lat, lng, latitude, longitude);

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(cb.asc(distancia), cb.asc(root.get("id_quadra")));
            }

            return cb.and(
                    cb.isNotNull(lat),
                    cb.isNotNull(lng),
                    cb.between(lat, latitude - deltaLat, latitude + deltaLat),
                    cb.between(lng, longitude - deltaLng, longitude + deltaLng),
                    cb.le(distancia, raioKm)
            );
        };
    }

    // Haversine (lei dos cossenos esférica), limitada a [-1, 1] para evitar NaN no acos.
    private static Expression<Double> distanciaKm(CriteriaBuilder cb, Expression<Double> lat, Expression<Double> lng,
                                                  double latitude, double longitude) {
        Expression<Double> latRad = cb.function("radians", Double.class, lat);
        Expression<Double> lngRad = cb.function("radians", Double.class, lng);
        Expression<Double> termoCos = cb.prod(
                cb.prod(cb.literal(Math.cos(Math.toRadians(latitude))), cb.function("cos", Double.class, latRad)),
                // soma com o valor negado: diff com literal negativo vira "--" (comentário SQL) quando o Hibernate inlina o literal
                cb.function("cos", Double.class, cb.sum(lngRad, cb.literal(-Math.toRadians(longitude)))));
        Expression<Double> termoSin = cb.prod(cb.literal(Math.sin(Math.toRadians(latitude))), cb.function("sin", Double.class, latRad));
        Expression<Double> cosseno = cb.function("least", Double.class, cb.literal(1.0),
                cb.function("greatest", Double.class, cb.literal(-1.0), cb.sum(termoCos, termoSin)));
        return cb.prod(cb.literal(RAIO_TERRA_KM), cb.function("acos", Double.class, cosseno));
    }

    public static Specification<Quadra> comTipoEsporte(TipoEsporte tipoEsporte) {
        if (tipoEsporte == null) return null;
        return (root, query, cb) -> cb.equal(root.get("tipoEsporte"), tipoEsporte);
    }

    public static Specification<Quadra> comNome(String nome) {
        return contemNormalizado("nome", nome);
    }

    public static Specification<Quadra> comCidade(String cidade) {
        return contemNormalizado("cidade", cidade);
    }

    public static Specification<Quadra> comBairro(String bairro) {
        return contemNormalizado("bairro", bairro);
    }

    /** CEP: igualdade apenas por dígitos, como no filtro em memória legado. */
    public static Specification<Quadra> comCep(String cep) {
        if (cep == null || cep.isBlank()) return null;
        String limpo = cep.replaceAll("[^0-9]", "");
        return (root, query, cb) -> cb.equal(somenteDigitos(cb, root.get("cep")), limpo);
    }

    public static Specification<Quadra> comEndereco(String endereco) {
        if (endereco == null || endereco.isBlank()) return null;
        String padrao = padraoContem(endereco);
        return (root, query, cb) -> cb.or(
                cb.like(normalizado(cb, root.get("logradouro")), padrao, ESCAPE_LIKE),
                cb.like(normalizado(cb, root.get("bairro")), padrao, ESCAPE_LIKE)
        );
    }

    private static Specification<Quadra> contemNormalizado(String atributo, String termo) {
        if (termo == null || termo.isBlank()) return null;
        String padrao = padraoContem(termo);
        return (root, query, cb) -> {
            Predicate predicado = cb.like(normalizado(cb, root.get(atributo)), padrao, ESCAPE_LIKE);
            return predicado;
        };
    }

    private static String padraoContem(String termo) {
        return "%" + TextoUtil.escaparLike(TextoUtil.normalizar(termo)) + "%";
    }

    private static Expression<String> normalizado(CriteriaBuilder cb, Expression<String> campo) {
        return cb.function("translate", String.class, cb.lower(campo), cb.literal(COM_ACENTO), cb.literal(SEM_ACENTO));
    }

    private static Expression<String> somenteDigitos(CriteriaBuilder cb, Expression<String> campo) {
        Expression<String> semHifen = cb.function("replace", String.class, campo, cb.literal("-"), cb.literal(""));
        Expression<String> semPonto = cb.function("replace", String.class, semHifen, cb.literal("."), cb.literal(""));
        return cb.function("replace", String.class, semPonto, cb.literal(" "), cb.literal(""));
    }
}
