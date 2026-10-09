package org.acme.resource.seguros.sinistro.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;

import java.util.UUID;

@Entity
public class Sinistro extends PanacheEntity {

    // O campo 'public Long id;' já é herdado automaticamente do PanacheEntity!

    @Column(nullable = false, unique = true)
    public String uuid; // Alterado para public (sem necessidade de getter/setter)

    @NotNull(message = "A apólice é obrigatória")
    @NotBlank(message = "A apólice não pode estar vazia")
    @Size(min = 4, message = "O ID da apólice deve ter no mínimo 4 caracteres")
    @Column(name = "apoliceid", nullable = false)
    public String apoliceId;

    @NotBlank(message = "A descrição é obrigatória")
    @Column(nullable = false)
    @Size(min = 6, message = "A descrição deve ter no mínimo 6 caracteres")
    @Pattern(
            regexp = ".*[a-zA-Zà-úÀ-Ú].*",
            message = "A descrição deve conter pelo menos uma letra (texto válido)"
    )
    public String descricao;

    @Column(name = "valorestimado", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "O valor estimado é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor estimado deve ser maior que zero")
    public BigDecimal valorEstimado;

    public String status;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null || this.uuid.isBlank()) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
