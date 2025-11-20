package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.chartfragment.BarConfigFragment;
import com.company.chartconfig.view.main.MainView;
import com.company.chartconfig.view.chartfragment.PieConfigFragment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Route(value = "chart-config-view", layout = MainView.class)
@ViewController(id = "ChartConfigView")
@ViewDescriptor(path = "chart-config-view.xml")
public class ChartConfigView extends StandardView {
    @ViewComponent
    private TypedTextField<String> searchField;

    @Subscribe
    public void onInit(final InitEvent event) {
        searchField.addKeyPressListener(Key.ENTER, keyPressEvent -> doSearch());

    }
    private void doSearch() {
        String searchValue = searchField.getValue();

    }
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

    // Components
    @ViewComponent private NativeLabel datasetNameLabel;
    @ViewComponent private NativeLabel chartTypeLabel;
    @ViewComponent private BarConfigFragment barConfig;
    @ViewComponent private PieConfigFragment pieConfig;
    @ViewComponent private VerticalLayout chartContainer;
    @ViewComponent private TypedTextField<Object> chartNameField;
    @ViewComponent private VerticalLayout fieldsList;

    // =============================
    // CREATE MODE
    // =============================
    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null;
        this.datasetId = datasetId;
        this.chartType = chartType;
        loadDatasetAndFields();
        setupLeftPane();
        setupMiddlePane();
    }

    // =============================
    // EDIT MODE
    // =============================
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

                // 1. Load X Axis
                String x = node.path("xAxis").asText(null);
                barConfig.setXAxisValue(x); // Dùng Setter mới

                // 2. Load Metrics
                List<String> loadedMetrics = new ArrayList<>();
                JsonNode metricsNode = node.path("metrics");
                if (metricsNode.isArray()) {
                    metricsNode.forEach(m -> loadedMetrics.add(m.asText()));
                }
                barConfig.setMetricsList(loadedMetrics); // Dùng Setter mới

                // 3. Load Dimensions
                List<String> loadedDims = new ArrayList<>();
                JsonNode dimsNode = node.path("dimensions");
                if (dimsNode.isArray()) {
                    dimsNode.forEach(d -> loadedDims.add(d.asText()));
                }
                barConfig.setDimensionsList(loadedDims); // Dùng Setter mới
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

    // =============================
    // LOAD DATASET FIELDS
    // =============================
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

    // =============================
    // ADD DRAGGABLE FIELD
    // =============================
    private void addDraggableField(String name, String type) {
        String displayText ="#    "+ name ;
        NativeLabel lbl = new NativeLabel(displayText);

        lbl.getStyle()
                // --- Layout Fix ---
                .setDisplay(Style.Display.FLEX)
                .setWidth("100%")
                .setHeight("24px")
                .setBoxSizing(Style.BoxSizing.BORDER_BOX)
                .setJustifyContent(Style.JustifyContent.SPACE_BETWEEN)
                .setFontWeight(Style.FontWeight.BOLD)
                .setAlignItems(Style.AlignItems.CENTER)
                .setPadding("0 4px")
                .setCursor("crab")
                .set("transform", "translate(0,0)")
                .setBorderRadius("4px")
                .setBackgroundColor("#f7f7f7")
                .setFontSize("14px");


       lbl.getElement().setAttribute("data-field-name", name);
        DragSource<NativeLabel> dragSource = DragSource.create(lbl);
        dragSource.setDragData(name);

        fieldsList.add(lbl);
    }

    // =============================
    // LEFT + MIDDLE PANES
    // =============================
    private void setupLeftPane() {
        datasetNameLabel.setText(dataset.getName());
        chartTypeLabel.setText(chartType.name());
    }

    private void setupMiddlePane() {
        if (chartType == ChartType.BAR) {
            barConfig.setFields(fieldNames);
            barConfig.setVisible(true);
            pieConfig.setVisible(false);
        } else if (chartType == ChartType.PIE) {
            pieConfig.setFields(fieldNames);
            pieConfig.setVisible(true);
            barConfig.setVisible(false);
        }
    }

    // =============================
    // PREVIEW
    // =============================
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

    // =============================
    // BUILD SETTINGS JSON
    // =============================
    private String buildSettingsJson() {
        ObjectNode node = objectMapper.createObjectNode();

        if (chartType == ChartType.BAR) {
            String x = barConfig.getxAxis();
            List<String> metrics = barConfig.getMetrics();
            List<String> dims = barConfig.getDimensions();
            List<FilterRule> filters = barConfig.getFilters();

            if (x == null || x.isBlank()) {
                notifications.create("Please select X").show();
                return null;
            }

            node.put("xAxis", x);

            ArrayNode metricsNode = node.putArray("metrics");
            metrics.forEach(metricsNode::add);

            ArrayNode dimsNode = node.putArray("dimensions");
            dims.forEach(dimsNode::add);

            ArrayNode filterNode = node.putArray("filters");
            for (FilterRule f : filters) {
                ObjectNode fo = node.objectNode();
                fo.put("column", f.getColumn());
                fo.put("operator", f.getOperator());
                fo.put("value", f.getValue());
                filterNode.add(fo);
            }

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

    // =============================
    // SAVE
    // =============================
    @Subscribe(id = "save", subject = "clickListener")
    public void onSaveClick(ClickEvent<JmixButton> event) {
        if (chartNameField.getValue() == null || chartNameField.getValue().toString().isBlank()) {
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
        notifications.create("✅ ChartConfig saved!").show();
        viewNavigators.view(this, ChartConfigListView.class).navigate();
    }
}
