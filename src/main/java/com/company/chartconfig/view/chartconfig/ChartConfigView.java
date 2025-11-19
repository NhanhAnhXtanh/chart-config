package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.barconfigfragment.BarConfigFragment;
import com.company.chartconfig.view.main.MainView;
import com.company.chartconfig.view.pieconfigfragment.PieConfigFragment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Route(value = "chart-config-view", layout = MainView.class)
@ViewController(id = "ChartConfigView")
@ViewDescriptor(path = "chart-config-view.xml")
public class ChartConfigView extends StandardView {

    private UUID datasetId;
    private ChartType chartType;

    private Dataset dataset;
    private List<String> fieldNames = new ArrayList<>();

    private ChartConfig editingConfig;

    @Autowired private DataManager dataManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private Notifications notifications;
    @Autowired private ChartConfigService chartConfigService;

    // LEFT PANEL
    @ViewComponent private NativeLabel datasetNameLabel;
    @ViewComponent private NativeLabel chartTypeLabel;

    // FRAGMENTS
    @ViewComponent private BarConfigFragment barConfig;
    @ViewComponent private PieConfigFragment pieConfig;

    // RIGHT PANEL
    @ViewComponent private VerticalLayout chartContainer;
    @ViewComponent private JmixTextArea fieldsArea;
    @ViewComponent private TypedTextField<Object> chartNameField;
    @Autowired
    private ViewNavigators viewNavigators;


    // ===================================================================
    //  CREATE MODE (from NewChartConfig)
    // ===================================================================
    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null;
        this.datasetId = datasetId;
        this.chartType = chartType;

        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();
    }


    // ===================================================================
    //  EDIT MODE (from ListView)
    // ===================================================================
    public void initFromExisting(UUID chartConfigId) {

        ChartConfig cfg = dataManager.load(ChartConfig.class)
                .id(chartConfigId)
                .one();

        this.editingConfig = cfg;
        this.datasetId = cfg.getDataset().getId();
        this.chartType = cfg.getChartType();

        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();

        chartNameField.setValue(cfg.getName());

        // Load existing settings → fill fragment combobox
        try {
            JsonNode node = objectMapper.readTree(cfg.getSettingsJson());

            if (chartType == ChartType.BAR) {
                barConfig.setFields(fieldNames);
                barConfig.setXField(node.path("xField").asText(null));
                barConfig.setYField(node.path("yField").asText(null));
            }

            if (chartType == ChartType.PIE) {
                pieConfig.setFields(fieldNames);
                pieConfig.setLabelField(node.path("labelField").asText(null));
                pieConfig.setValueField(node.path("valueField").asText(null));
            }

        } catch (Exception e) {
            notifications.create("Cannot parse settingsJson").show();
        }
    }



    // ===================================================================
    //  LOAD DATASET + FIELDS
    // ===================================================================
    private void loadDatasetAndFields() {

        dataset = dataManager.load(Dataset.class)
                .id(datasetId)
                .one();

        fieldNames.clear();

        try {
            JsonNode root = objectMapper.readTree(dataset.getSchemaJson());

            if (root.isArray()) {
                StringBuilder sb = new StringBuilder();

                for (JsonNode col : root) {
                    String name = col.path("name").asText();
                    String type = col.path("type").asText();

                    if (!name.isEmpty()) {
                        fieldNames.add(name);

                        if (sb.length() > 0) sb.append("\n");
                        sb.append(name);

                        if (!type.isEmpty()) sb.append(" (").append(type).append(")");
                    }
                }

                fieldsArea.setValue(sb.toString());
            }

        } catch (IOException e) {
            fieldsArea.setValue("Cannot parse schemaJson: " + e.getMessage());
        }
    }


    // ===================================================================
    //  LEFT PANE
    // ===================================================================
    private void setupLeftPane() {
        datasetNameLabel.setText(dataset.getName());
        chartTypeLabel.setText(chartType.name());
    }


    // ===================================================================
    //  MIDDLE PANE → Show fragment + load fields
    // ===================================================================
    private void setupMiddlePane() {

        if (chartType == ChartType.BAR) {
            barConfig.setFields(fieldNames);
            barConfig.setVisible(true);

            pieConfig.setVisible(false);
        }
        else if (chartType == ChartType.PIE) {
            pieConfig.setFields(fieldNames);
            pieConfig.setVisible(true);

            barConfig.setVisible(false);
        }
    }


    // ===================================================================
    //  PREVIEW CHART
    // ===================================================================
    @Subscribe(id = "previewBtn", subject = "clickListener")
    public void onPreviewBtnClick(ClickEvent<JmixButton> event) {

        String settingsJson = buildSettingsJsonFromUi();

        if (settingsJson == null) return;

        chartContainer.removeAll();

        Chart chart = chartConfigService.buildPreviewChart(
                dataset,
                chartType,
                settingsJson
        );

        chart.setWidthFull();
        chart.setHeight("400px");
        chartContainer.add(chart);
    }


    // ===================================================================
    //  Convert UI to settingsJson
    // ===================================================================
    private String buildSettingsJsonFromUi() {

        ObjectNode node = objectMapper.createObjectNode();

        if (chartType == ChartType.BAR) {
            String x = barConfig.getXField();
            String y = barConfig.getYField();
            if (x == null || y == null) {
                notifications.create("Please select X and Y").show();
                return null;
            }
            node.put("xField", x);
            node.put("yField", y);
        }

        if (chartType == ChartType.PIE) {
            String lbl = pieConfig.getLabelField();
            String val = pieConfig.getValueField();
            if (lbl == null || val == null) {
                notifications.create("Please select Label and Value").show();
                return null;
            }
            node.put("labelField", lbl);
            node.put("valueField", val);
        }

        return node.toString();
    }


    // ===================================================================
    //  SAVE
    // ===================================================================
    @Subscribe(id = "save", subject = "clickListener")
    public void onSaveClick(ClickEvent<JmixButton> event) {

        if (chartNameField.getValue() == null || chartNameField.getValue().toString().isBlank()) {
            notifications.create("Please enter chart name").show();
            return;
        }

        String settingsJson = buildSettingsJsonFromUi();
        if (settingsJson == null) return;

        ChartConfig config =
                (editingConfig != null) ? editingConfig : dataManager.create(ChartConfig.class);

        config.setName(chartNameField.getValue().toString());
        config.setDataset(dataset);
        config.setChartType(chartType);
        config.setSettingsJson(settingsJson);

        dataManager.save(config);

        notifications.create("ChartConfig saved!").show();

        viewNavigators
                .view(this, ChartConfigListView.class)
                .navigate();
    }
}
