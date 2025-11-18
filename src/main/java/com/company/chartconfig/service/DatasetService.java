package com.company.chartconfig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DatasetService {
    private final ObjectMapper objectMapper;

    public DatasetService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse raw JSON and return:
     * - schemaJson string
     * - List<KeyValueEntity> rows for UI Grid
     */
    public ParsedSchemaResult parseSchema(String rawJson) throws Exception {

        JsonNode rootNode = objectMapper.readTree(rawJson);

        if (!rootNode.isArray() || !rootNode.elements().hasNext()) {
            throw new IllegalArgumentException("JSON must be an array of objects");
        }

        JsonNode firstObject = rootNode.get(0);
        if (!firstObject.isObject()) {
            throw new IllegalArgumentException("JSON array elements must be objects");
        }

        List<Map<String, Object>> schemaList = new ArrayList<>();
        List<KeyValueEntity> rows = new ArrayList<>();

        firstObject.fields().forEachRemaining(f -> {
            String name = f.getKey();
            String type = detectType(f.getValue());

            schemaList.add(Map.of("name", name, "type", type));

            KeyValueEntity kv = new KeyValueEntity();
            kv.setValue("name", name);
            kv.setValue("type", type);
            rows.add(kv);
        });

        String schemaJson = objectMapper.writeValueAsString(schemaList);

        return new ParsedSchemaResult(schemaJson, rows);
    }

    private String detectType(JsonNode v) {
        if (v.isNumber()) return "number";
        if (v.isTextual()) return "string";
        if (v.isBoolean()) return "boolean";
        if (v.isArray()) return "array";
        if (v.isObject()) return "object";
        return "unknown";
    }

    public record ParsedSchemaResult(String schemaJson, List<KeyValueEntity> rows) {}

}