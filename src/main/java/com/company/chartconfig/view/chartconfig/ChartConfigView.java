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
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.Label;
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

import java.util.*;

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
    @Autowired private ViewNavigators viewNavigators;

    // LEFT + MIDDLE + RIGHT
    @ViewComponent private NativeLabel datasetNameLabel;
    @ViewComponent private NativeLabel chartTypeLabel;

    @ViewComponent private BarConfigFragment barConfig;
    @ViewComponent private PieConfigFragment pieConfig;

    @ViewComponent private VerticalLayout chartContainer;
    @ViewComponent private TypedTextField<Object> chartNameField;

    @ViewComponent private VerticalLayout fieldsList;


    // =====================================
    // CREATE MODE
    // =====================================
    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null;
        this.datasetId = datasetId;
        this.chartType = chartType;

        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();
    }


    // =====================================
    // EDIT MODE
    // =====================================
    public void initFromExisting(UUID chartConfigId) {

        ChartConfig cfg = dataManager.load(ChartConfig.class)
                .id(chartConfigId).one();

        this.editingConfig = cfg;
        this.datasetId = cfg.getDataset().getId();
        this.chartType = cfg.getChartType();

        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();

        chartNameField.setValue(cfg.getName());

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


    // =====================================
    // LOAD DATASET FIELDS
    // =====================================
    private void loadDatasetAndFields() {
        dataset = dataManager.load(Dataset.class).id(datasetId).one();
        fieldNames.clear();
        fieldsList.removeAll();

        try {
            JsonNode root = objectMapper.readTree(dataset.getSchemaJson());
            if (root.isArray()) {
                for (JsonNode col : root) {
                    String name = col.path("name").asText();
                    String type = col.path("type").asText("string");
                    fieldNames.add(name);
                    addDraggableField(name, type);
                }
            }
        } catch (Exception e) {
            notifications.create("Cannot parse schemaJson").show();
        }
    }

    private void addDraggableField(String name, String type) {
        String displayText = name + " (" + type + ")";
        NativeLabel lbl = new NativeLabel(displayText);

        lbl.getStyle()
                .set("padding", "6px 10px")
                .set("border", "1px solid #ccc")
                .set("cursor", "grab")
                .set("border-radius", "14px")
                .set("background-color", "#e7d7d7")
                .set("font-size", "22px")
                .set("user-select", "none")
                .set("margin-bottom", "4px");

        // Kích hoạt draggable
        lbl.getElement().setAttribute("draggable", "true");
        lbl.getElement().setAttribute("data-field-name", name);

        // JS event: thêm dataTransfer khi người dùng bắt đầu kéo
        lbl.getElement().executeJs("""
        this.addEventListener('dragstart', e => {
            e.dataTransfer.setData('text/plain', this.getAttribute('data-field-name'));
        });
    """);

        fieldsList.add(lbl);
    }





    // =====================================
    // LEFT + MIDDLE PANES
    // =====================================
    private void setupLeftPane() {
        datasetNameLabel.setText(dataset.getName());
        chartTypeLabel.setText(chartType.name());
    }

    private void setupMiddlePane() {

        if (chartType == ChartType.BAR) {
            barConfig.setFields(fieldNames);
            barConfig.setVisible(true);
            pieConfig.setVisible(false);
        }

        if (chartType == ChartType.PIE) {
            pieConfig.setFields(fieldNames);
            pieConfig.setVisible(true);
            barConfig.setVisible(false);
        }
    }


    // =====================================
    // PREVIEW CHART
    // =====================================
    @Subscribe(id = "previewBtn", subject = "clickListener")
    public void onPreviewBtnClick(ClickEvent<JmixButton> event) {
        String settingsJson = buildSettingsJson();
        if (settingsJson == null) return;

        chartContainer.removeAll();

        try {
            Chart chart = chartConfigService.buildPreviewChart(dataset, chartType, settingsJson);
            chart.setWidthFull();
            chart.setHeight("400px");
            chartContainer.add(chart);
        } catch (Exception e) {
            e.printStackTrace();
            notifications.create("⚠️ Failed to render chart preview: " + e.getMessage()).show();
        }
    }



    // =====================================
    // BUILD JSON SETTINGS
    // =====================================
    private String buildSettingsJson() {
        ObjectNode node = objectMapper.createObjectNode();

        if (chartType == ChartType.BAR) {
            // ép Vaadin flush giá trị client-side
            getUI().ifPresent(ui -> ui.getPage().executeJs("window.getSelection()?.removeAllRanges();"));

            String x = barConfig.getXField();
            String y = barConfig.getYField();

            if (x == null || x.isBlank() || y == null || y.isBlank()) {
                notifications.create("Please select X and Y").show();
                return null;
            }

            node.put("xField", x);
            node.put("yField", y);
        }
        if (chartType == ChartType.PIE) {
            String label = pieConfig.getLabelField();
            String value = pieConfig.getValueField();

            if (label == null || label.isBlank() || value == null || value.isBlank()) {
                notifications.create("Please select Label and Value").show();
                return null;
            }

            node.put("labelField", label);
            node.put("valueField", value);
        }
        return node.toString();
    }


    // =====================================
    // SAVE
    // =====================================
    @Subscribe(id = "save", subject = "clickListener")
    public void onSaveClick(ClickEvent<JmixButton> event) {

        if (chartNameField.getValue() == null ||
                chartNameField.getValue().toString().isBlank()) {
            notifications.create("Please enter chart name").show();
            return;
        }

        String settingsJson = buildSettingsJson();
        if (settingsJson == null) return;

        ChartConfig config = (editingConfig != null)
                ? editingConfig
                : dataManager.create(ChartConfig.class);

        config.setName(chartNameField.getValue().toString());
        config.setDataset(dataset);
        config.setChartType(chartType);
        config.setSettingsJson(settingsJson);

        dataManager.save(config);

        notifications.create("ChartConfig saved!").show();

        viewNavigators.view(this, ChartConfigListView.class).navigate();
    }
}
