package com.company.chartconfig.view.dataset;

import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.service.DatasetService;
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
    private DatasetService datasetService;

    @Subscribe("parseJsonBtn")
    public void onParseJsonBtnClick(ClickEvent<Button> event) {
        Dataset dataset = getEditedEntity();
        String rawJson = dataset.getRawJson();

        if (rawJson == null || rawJson.isBlank()) {
            notifications.create("⚠ Raw JSON is empty")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        try {
            DatasetService.ParsedSchemaResult result =
                    datasetService.parseSchema(rawJson);

            dataset.setSchemaJson(result.schemaJson());

            // update grid
            schemaDc.getMutableItems().clear();
            schemaDc.getMutableItems().addAll(result.rows());

            notifications.create("✅ Parsed fields and stored schemaJson")
                    .withType(Notifications.Type.SUCCESS)
                    .show();

        } catch (Exception e) {
            notifications.create("❌ Invalid JSON: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }
}
