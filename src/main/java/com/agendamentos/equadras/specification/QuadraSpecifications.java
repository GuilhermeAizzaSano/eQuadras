package com.agendamentos.equadras.specification;

import com.agendamentos.equadras.model.entity.Quadra;
import com.agendamentos.equadras.model.enums.TipoEsporte;
import org.springframework.data.jpa.domain.Specification;

public final class QuadraSpecifications {

    private QuadraSpecifications() {}

    public static Specification<Quadra> ativa() {
        return (root, query, cb) -> cb.isTrue(root.get("ativa"));
    }

    public static Specification<Quadra> doAdmin(Long adminId) {
        if (adminId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("admin").get("id_usuario"), adminId);
    }

    public static Specification<Quadra> comTipoEsporte(TipoEsporte tipoEsporte) {
        if (tipoEsporte == null) return null;
        return (root, query, cb) -> cb.equal(root.get("tipoEsporte"), tipoEsporte);
    }

    public static Specification<Quadra> comNome(String nome) {
        if (nome == null || nome.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + nome.trim().toLowerCase() + "%");
    }

    public static Specification<Quadra> comCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("cidade")), "%" + cidade.trim().toLowerCase() + "%");
    }

    public static Specification<Quadra> comBairro(String bairro) {
        if (bairro == null || bairro.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("bairro")), "%" + bairro.trim().toLowerCase() + "%");
    }

    public static Specification<Quadra> comCep(String cep) {
        if (cep == null || cep.isBlank()) return null;
        String limpo = cep.replaceAll("[^0-9]", "");
        return (root, query, cb) -> cb.like(root.get("cep"), "%" + limpo + "%");
    }

    public static Specification<Quadra> comEndereco(String endereco) {
        if (endereco == null || endereco.isBlank()) return null;
        String termo = "%" + endereco.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("logradouro")), termo),
                cb.like(cb.lower(root.get("bairro")), termo)
        );
    }
}
