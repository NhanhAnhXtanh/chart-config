package com.company.chartconfig.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DatasetService {
    private final ObjectMapper objectMapper;

    // Regex cho YYYY-MM-DD (ISO)
    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}.*");
    // Regex cho DD/MM/YYYY hoặc MM/DD/YYYY (Có dấu gạch chéo)
    private static final Pattern SLASH_DATE = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}.*");

    public DatasetService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedSchemaResult parseSchema(String rawJson) throws Exception {
        JsonNode rootNode = objectMapper.readTree(rawJson);

        if (!rootNode.isArray() || !rootNode.elements().hasNext()) {
            throw new IllegalArgumentException("JSON must be an array of objects");
        }

        JsonNode firstObject = rootNode.get(0);
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
        if (v.isBoolean()) return "boolean";
        if (v.isArray()) return "array";
        if (v.isObject()) return "object";

        if (v.isTextual()) {
            String text = v.textValue();
            // Kiểm tra cả format ISO (-) và format thường (/)
            if (ISO_DATE.matcher(text).matches() || SLASH_DATE.matcher(text).matches()) {
                return "date";
            }
            return "string";
        }
        return "unknown";
    }

    public record ParsedSchemaResult(String schemaJson, List<KeyValueEntity> rows) {}
}