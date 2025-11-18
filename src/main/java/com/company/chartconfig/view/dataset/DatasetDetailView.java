package com.company.chartconfig.view.dataset;

import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.view.main.MainView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Route(value = "datasets/:id", layout = MainView.class)
@ViewController("Dataset.detail")
@ViewDescriptor("dataset-detail-view.xml")
@EditedEntityContainer("datasetDc")
public class DatasetDetailView extends StandardDetailView<Dataset> {

    @ViewComponent
    private KeyValueCollectionContainer schemaDc;

    @Autowired
    private Notifications notifications;

    @Autowired
    private ObjectMapper objectMapper;

    @Subscribe("parseJsonBtn")
    public void onParseJsonBtnClick(ClickEvent<Button> event) {
        try {
            Dataset dataset = getEditedEntity();
            String rawJson = dataset.getRawJson();

            if (rawJson == null || rawJson.isBlank()) {
                notifications.create("⚠ Raw JSON is empty")
                        .withType(Notifications.Type.WARNING)
                        .show();
                return;
            }

            JsonNode rootNode = objectMapper.readTree(rawJson);
            if (!rootNode.isArray() || !rootNode.elements().hasNext()) {
                notifications.create("❌ JSON must be an array of objects: [ { ... } ]")
                        .withType(Notifications.Type.ERROR)
                        .show();
                return;
            }

            JsonNode firstObject = rootNode.get(0);
            if (!firstObject.isObject()) {
                notifications.create("❌ JSON array elements must be objects")
                        .withType(Notifications.Type.ERROR)
                        .show();
                return;
            }

            List<Map<String, Object>> schemaList = new ArrayList<>();
            List<KeyValueEntity> rows = new ArrayList<>();

            // parse từng field
            firstObject.fields().forEachRemaining(f -> {

                String fieldName = f.getKey();
                String fieldType = detectType(f.getValue());

                schemaList.add(Map.of(
                        "name", fieldName,
                        "type", fieldType
                ));

                KeyValueEntity kv = new KeyValueEntity();
                kv.setValue("name", fieldName);
                kv.setValue("type", fieldType);
                rows.add(kv);
            });

            // ---- LƯU LẠI SCHEMA_JSON ----
            String schemaJson = objectMapper.writeValueAsString(schemaList);
            dataset.setSchemaJson(schemaJson);

            // update grid
            schemaDc.getMutableItems().clear();
            schemaDc.getMutableItems().addAll(rows);

            notifications.create("✅ Parsed " + rows.size() + " fields and stored schemaJson")
                    .withType(Notifications.Type.SUCCESS)
                    .show();

        } catch (Exception e) {
            notifications.create("❌ Invalid JSON: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    private String detectType(JsonNode value) {
        if (value.isNumber()) return "number";
        if (value.isTextual()) return "string";
        if (value.isBoolean()) return "boolean";
        if (value.isArray()) return "array";
        if (value.isObject()) return "object";
        return "unknown";
    }
}
