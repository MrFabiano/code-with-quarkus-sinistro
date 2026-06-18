package org.acme.resource.seguros.sinistro.event;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;

public class SinistroEventDeserializer extends ObjectMapperDeserializer<SinistroEvent> {
    public SinistroEventDeserializer() {
        super(SinistroEvent.class);
    }
}
