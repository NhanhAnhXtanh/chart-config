package com.company.chartconfig.service;

import com.company.chartconfig.entity.Dataset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;

@Service
public class DatasetService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataManager dataManager;

    /**
     * Parse rawJson thành schemaJson và trả về Dataset đã cập nhật
     */
    public Dataset parseSchema(Dataset dataset) {
        String rawJson = dataset.getRawJson();
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("JSON input is empty");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);
            if (!rootNode.isArray() || !rootNode.elements().hasNext()) {
                throw new IllegalArgumentException("JSON must be an array of objects, e.g. [ { ... } ]");
            }

            ArrayNode arrayNode = (ArrayNode) rootNode;
            JsonNode firstItem = arrayNode.get(0);
            if (!firstItem.isObject()) {
                throw new IllegalArgumentException("Array elements must be JSON objects");
            }

            ArrayNode schemaArray = objectMapper.createArrayNode();
            Iterator<Map.Entry<String, JsonNode>> fields = firstItem.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                ObjectNode schemaItem = objectMapper.createObjectNode();
                schemaItem.put("name", entry.getKey());
                schemaItem.put("type", detectType(entry.getValue()));
                schemaArray.add(schemaItem);
            }

            dataset.setSchemaJson(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(schemaArray));
            return dataset;

        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Save Dataset
     */
    public Dataset save(Dataset dataset) {
        return dataManager.save(dataset);
    }

    private String detectType(JsonNode value) {
        if (value.isNumber()) return "number";
        if (value.isTextual()) return "string";
        if (value.isBoolean()) return "boolean";
        if (value.isArray()) return "array";
        if (value.isObject()) return "object";
        if (value.isNull()) return "null";
        return "string";
    }
}