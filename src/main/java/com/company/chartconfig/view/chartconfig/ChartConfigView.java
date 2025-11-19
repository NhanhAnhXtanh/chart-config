package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.main.MainView;
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
import io.jmix.flowui.component.combobox.JmixComboBox;
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

    // nếu != null -> đang edit ChartConfig
    private ChartConfig editingConfig;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Notifications notifications;
    @Autowired
    private ChartConfigService chartConfigService;

    // SPLIT 1
    @ViewComponent
    private NativeLabel datasetNameLabel;
    @ViewComponent
    private NativeLabel chartTypeLabel;

    // SPLIT 2
    @ViewComponent
    private VerticalLayout barConfigBox;
    @ViewComponent
    private VerticalLayout pieConfigBox;
    @ViewComponent
    private JmixComboBox<String> barXFieldCombo;
    @ViewComponent
    private JmixComboBox<String> barYFieldCombo;
    @ViewComponent
    private JmixComboBox<String> pieLabelFieldCombo;
    @ViewComponent
    private JmixComboBox<String> pieValueFieldCombo;

    // SPLIT 3
    @ViewComponent
    private VerticalLayout chartContainer;
    @ViewComponent
    private JmixTextArea fieldsArea;
    @ViewComponent
    private TypedTextField<Object> chartNameField;

    // ---- MODE CREATE (gọi từ NewChartConfig) ----
    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null; // chắc chắn là create
        this.datasetId = datasetId;
        this.chartType = chartType;

        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();
    }

    // ---- MODE EDIT (gọi từ ListView) ----
    public void initFromExisting(UUID chartConfigId) {
        ChartConfig cfg = dataManager.load(ChartConfig.class)
                .id(chartConfigId)
                .one();

        this.editingConfig = cfg;
        this.datasetId = cfg.getDataset().getId();
        this.chartType = cfg.getChartType();

        // load dataset + field list
        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();

        // set tên chart
        chartNameField.setValue(cfg.getName());

        // parse settingsJson để set value cho combo
        String settings = cfg.getSettingsJson();
        if (settings != null && !settings.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(settings);
                if (chartType == ChartType.BAR) {
                    String xField = node.path("xField").asText(null);
                    String yField = node.path("yField").asText(null);
                    barXFieldCombo.setValue(xField);
                    barYFieldCombo.setValue(yField);
                } else if (chartType == ChartType.PIE) {
                    String labelField = node.path("labelField").asText(null);
                    String valueField = node.path("valueField").asText(null);
                    pieLabelFieldCombo.setValue(labelField);
                    pieValueFieldCombo.setValue(valueField);
                }
            } catch (IOException e) {
                notifications.create("Cannot parse settingsJson: " + e.getMessage()).show();
            }
        }
    }

    // ---- COMMON ----

    private void loadDatasetAndFields() {
        dataset = dataManager.load(Dataset.class)
                .id(datasetId)
                .one();

        fieldNames.clear();

        if (dataset.getSchemaJson() != null) {
            try {
                JsonNode root = objectMapper.readTree(dataset.getSchemaJson());
                if (root.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode col : root) {
                        String name = col.path("name").asText();
                        String type = col.path("type").asText();
                        if (!name.isEmpty()) {
                            fieldNames.add(name);
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append(name);
                            if (!type.isEmpty()) {
                                sb.append(" (").append(type).append(")");
                            }
                        }
                    }
                    fieldsArea.setValue(sb.toString());
                }
            } catch (IOException e) {
                fieldsArea.setValue("Cannot parse schemaJson: " + e.getMessage());
            }
        }
    }

    private void setupLeftPane() {
        datasetNameLabel.setText(dataset.getName());
        chartTypeLabel.setText(chartType != null ? chartType.name() : "N/A");
    }

    private void setupMiddlePane() {
        // set options cho tất cả combobox
        barXFieldCombo.setItems(fieldNames);
        barYFieldCombo.setItems(fieldNames);
        pieLabelFieldCombo.setItems(fieldNames);
        pieValueFieldCombo.setItems(fieldNames);

        // show/hide tùy chart type
        if (chartType == ChartType.BAR) {
            barConfigBox.setVisible(true);
            pieConfigBox.setVisible(false);
        } else if (chartType == ChartType.PIE) {
            barConfigBox.setVisible(false);
            pieConfigBox.setVisible(true);
        } else {
            barConfigBox.setVisible(false);
            pieConfigBox.setVisible(false);
        }
    }

    @Subscribe(id = "previewBtn", subject = "clickListener")
    public void onPreviewBtnClick(final ClickEvent<JmixButton> event) {
        if (dataset == null) {
            notifications.create("Dataset is not loaded").show();
            return;
        }

        String settingsJson = buildSettingsJsonFromUi();
        if (settingsJson == null) {
            return; // đã show thông báo trong hàm build
        }

        // Xoá chart cũ
        chartContainer.removeAll();

        // Tạo chart preview
        Chart chart = chartConfigService.buildPreviewChart(
                dataset,
                chartType,
                settingsJson
        );

        if (chart == null) {
            notifications.create("Cannot build chart").show();
            return;
        }

        chart.setWidthFull();
        chart.setHeight("400px");
        chartContainer.add(chart);
    }

    private String buildSettingsJsonFromUi() {
        if (chartType == null) {
            notifications.create("Chart type is not set").show();
            return null;
        }

        String f1;
        String f2;

        if (chartType == ChartType.BAR) {
            f1 = barXFieldCombo.getValue();
            f2 = barYFieldCombo.getValue();
            if (f1 == null || f2 == null) {
                notifications.create("Please select X and Y fields").show();
                return null;
            }
        } else if (chartType == ChartType.PIE) {
            f1 = pieLabelFieldCombo.getValue();
            f2 = pieValueFieldCombo.getValue();
            if (f1 == null || f2 == null) {
                notifications.create("Please select Label and Value fields").show();
                return null;
            }
        } else {
            notifications.create("Unsupported chart type").show();
            return null;
        }

        ObjectNode node = objectMapper.createObjectNode();
        if (chartType == ChartType.BAR) {
            node.put("xField", f1);
            node.put("yField", f2);
        } else if (chartType == ChartType.PIE) {
            node.put("labelField", f1);
            node.put("valueField", f2);
        }

        return node.toString();
    }

    @Subscribe(id = "save", subject = "clickListener")
    public void onSaveClick(final ClickEvent<JmixButton> event) {
        if (dataset == null || chartType == null) {
            notifications.create("Dataset or chart type is empty").show();
            return;
        }

        String name = chartNameField.getValue();
        if (name == null || name.isBlank()) {
            notifications.create("Please enter chart name").show();
            return;
        }

        String settingsJson = buildSettingsJsonFromUi();
        if (settingsJson == null) {
            return;
        }

        ChartConfig config;
        if (editingConfig != null) {
            config = editingConfig;
        } else {
            config = dataManager.create(ChartConfig.class);
        }

        config.setName(name.trim());
        config.setDataset(dataset);
        config.setChartType(chartType);
        config.setSettingsJson(settingsJson);

        editingConfig = dataManager.save(config);

        notifications.create("ChartConfig has been saved").show();
    }
}
