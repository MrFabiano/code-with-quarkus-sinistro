package org.acme.resource.seguros.sinistro.adapter.out.messaging;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import org.acme.resource.seguros.sinistro.domain.model.SinistroEvent;

public class SinistroEventDeserializer extends ObjectMapperDeserializer<SinistroEvent> {
    public SinistroEventDeserializer() {
        super(SinistroEvent.class);
    }
}
