package org.acme.resource.seguros.sinistro.domain.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;

@ApplicationScoped
public class SinistroEventMapper {

    @Inject
    ObjectMapper objectMapper;



    // Converter JSON → SinistroEvent
    public SinistroEvent fromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, SinistroEvent.class);
    }
    public String toJson(SinistroEvent evento) throws JsonProcessingException {
        return objectMapper.writeValueAsString(evento);
    }

    public String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }
}