package org.acme.resource.seguros.sinistro.infrastructure.config;

import org.acme.resource.seguros.sinistro.adapter.in.rest.dto.SinistroPayload;
import org.acme.resource.seguros.sinistro.adapter.out.persistence.Sinistro;
import org.acme.resource.seguros.sinistro.api.request.SinistroRequest;
import org.acme.resource.seguros.sinistro.api.request.SinistroUpdateRequest;
import org.acme.resource.seguros.sinistro.api.response.SinistroResponse;

import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface SinistroMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)    // gerado no @PrePersist
    @Mapping(target = "status", ignore = true) // service define ABERTO
    Sinistro toEntity(SinistroRequest request);

    SinistroResponse toResponse(Sinistro entity);

    List<SinistroResponse> toResponseList(List<Sinistro> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    void updateEntity(SinistroUpdateRequest request, @MappingTarget Sinistro entity);

    // Eventos e payload do Camel também ficam aqui
    @Mapping(target = "tipoEvento", constant = "SINISTRO_CRIADO")
    @Mapping(target = "dataHora", expression = "java(java.time.LocalDateTime.now())")
    SinistroEvent toEvent(Sinistro sinistro);

    SinistroPayload toPayload(Sinistro sinistro);


}