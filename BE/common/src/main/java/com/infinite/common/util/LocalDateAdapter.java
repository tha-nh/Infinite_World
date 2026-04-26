package com.infinite.common.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateAdapter extends TypeAdapter<LocalDate> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public void write(JsonWriter out, LocalDate value) throws IOException {
        if (value == null) {
            out.nullValue(); // If LocalDate is null, write null to JSON
        } else {
            out.value(value.format(FORMATTER)); // Convert LocalDate to string (e.g., "2025-02-10")
        }
    }

    @Override
    public LocalDate read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull(); // If the JSON value is null, return null
            return null;
        }
        // If the value is not null, parse it as a LocalDate
        return LocalDate.parse(in.nextString(), FORMATTER); // Parse the string into LocalDate
    }
}
