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

    private QuadraSpecifications() {}

    public static Specification<Quadra> ativa() {
        return (root, query, cb) -> cb.isTrue(root.get("ativa"));
    }

    public static Specification<Quadra> doAdmin(Long adminId) {
        if (adminId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("admin").get("id_usuario"), adminId);
    }

    public static Specification<Quadra> comIds(java.util.Collection<Long> ids) {
        return (root, query, cb) -> root.get("id_quadra").in(ids);
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
