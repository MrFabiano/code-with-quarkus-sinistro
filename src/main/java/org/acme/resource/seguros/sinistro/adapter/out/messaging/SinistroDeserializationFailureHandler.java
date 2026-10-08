package org.acme.resource.seguros.sinistro.adapter.out.messaging;

import io.smallrye.reactive.messaging.kafka.DeserializationFailureHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;
import org.apache.kafka.common.header.Headers;

@ApplicationScoped
@Named("sinistroDeserializationFailureHandler")
public class SinistroDeserializationFailureHandler implements DeserializationFailureHandler<SinistroEvent> {

    @Override
    public SinistroEvent handleDeserializationFailure(
            String topic,
            boolean isKey,
            String deserializer,
            byte[] data,
            Exception exception,
            Headers headers) {

        System.err.println("|| DLQ HANDLER ATIVO || Mensagem inválida no tópico: " + topic);
        System.err.println("|| DLQ HANDLER ATIVO || Erro: " + exception.getMessage());

        // Lança a exceção para que a estratégia de DLQ configurada no properties seja acionada
        //throw new RuntimeException("Forçando envio para DLQ devido a erro de parse: " + exception.getMessage(), exception);
        return null;
    }
}