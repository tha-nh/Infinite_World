package com.infinite.common.config.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;

public class PageImplSerializer extends JsonSerializer<PageImpl<?>> {

    @Override
    public void serialize(PageImpl<?> page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("content");
        gen.writeObject(page.getContent());

        Pageable pageable = page.getPageable();
        if (pageable.isPaged()) {
            gen.writeNumberField("page", pageable.getPageNumber());
            gen.writeNumberField("size", pageable.getPageSize());
            gen.writeNumberField("totalElements", page.getTotalElements());
            gen.writeNumberField("totalPages", page.getTotalPages());
        } else if (pageable.isUnpaged()) {
            // Custom handling for Unpaged
            gen.writeStringField("page", "unpaged");
            gen.writeNullField("size");
            gen.writeNullField("totalElements");
            gen.writeNullField("totalPages");
        }
        gen.writeEndObject();
    }
}
