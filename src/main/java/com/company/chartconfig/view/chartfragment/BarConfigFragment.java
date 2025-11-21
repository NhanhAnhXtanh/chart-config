package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.enums.TimeGrain;
import com.company.chartconfig.utils.ChartUiUtils;
import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FragmentDescriptor("bar-config-fragment.xml")
public class BarConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired private ObjectMapper objectMapper;

    @ViewComponent private Div xDrop;
    @ViewComponent private Checkbox forceCategoricalField; // MỚI: Checkbox Force Category

    @ViewComponent private Div metricsDrop;
    @ViewComponent private Div dimensionsDrop;
    @ViewComponent private Div filtersDrop;

    // --- 1. X-AXIS SORT UI ---
    @ViewComponent private VerticalLayout xAxisSortBox;
    @ViewComponent private ComboBox<String> xAxisSortByField;
    @ViewComponent private Checkbox xAxisSortAscField;

    // --- 2. QUERY SORT UI ---

    private MetricConfig querySortMetric = null;

    @ViewComponent private JmixComboBox<ContributionMode> contributionModeField;
    @ViewComponent private ComboBox<Integer> seriesLimitField;
    @ViewComponent private VerticalLayout timeGrainBox;
    @ViewComponent private JmixComboBox<TimeGrain> timeGrainField;

    private String xAxis;
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();
    private Map<String, String> fieldsTypeMap = new HashMap<>();
    @ViewComponent
    private Div querySortDrop;
    @ViewComponent
    private JmixCheckbox querySortDescField;

    @Subscribe
    public void onReady(ReadyEvent event) {
        ChartUiUtils.setupSeriesLimitField(seriesLimitField);
        if (contributionModeField.getValue() == null) contributionModeField.setValue(ContributionMode.NONE);
        if (xAxisSortAscField.getValue() == null) xAxisSortAscField.setValue(true);
        if (querySortDescField.getValue() == null) querySortDescField.setValue(true);
        if (forceCategoricalField.getValue() == null) forceCategoricalField.setValue(false);

        // Thêm listener cho Force Categorical
        forceCategoricalField.addValueChangeListener(e -> checkSortVisibility());

        DropZoneUtils.setup(xDrop, v -> {
            this.xAxis = v;
            checkTimeGrainVisibility();
            refreshXAxisSortOptions();
            checkSortVisibility(); // Check lại hiển thị khi drop
        });
        DropZoneUtils.setupMetricZone(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        DropZoneUtils.setupSingleMetricZone(querySortDrop, querySortMetric, new ArrayList<>(fieldsTypeMap.keySet()),
                (cfg) -> {
                    this.querySortMetric = cfg;
                    refreshUI();
                },
                () -> {
                    this.querySortMetric = null;
                    refreshUI();
                }
        );

        refreshUI();
    }

    private void refreshUI() {
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, xAxis, v -> {
                this.xAxis = v;
                checkTimeGrainVisibility();
                refreshXAxisSortOptions();
                checkSortVisibility();
            });
            DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshXAxisSortOptions);
            DropZoneUtils.updateMulti(dimensionsDrop, dimensions);
            DropZoneUtils.updateFilters(filtersDrop, filters);

            DropZoneUtils.updateSingleMetricVisuals(querySortDrop, querySortMetric, new ArrayList<>(fieldsTypeMap.keySet()),
                    (cfg) -> { this.querySortMetric = cfg; refreshUI(); },
                    () -> { this.querySortMetric = null; refreshUI(); }
            );

            refreshXAxisSortOptions();
            checkSortVisibility();
        }
    }

    /**
     * LOGIC HIỂN THỊ QUAN TRỌNG
     */
    private void checkSortVisibility() {
        if (xAxis == null || xAxis.isEmpty()) {
            forceCategoricalField.setVisible(false);
            xAxisSortBox.setVisible(false);
            return;
        }

        String type = fieldsTypeMap.getOrDefault(xAxis, "string").toLowerCase();
        boolean isNumber = type.contains("number") || type.contains("int") || type.contains("double") || type.contains("float") || type.contains("decimal");

        // 1. Logic cho Checkbox Force Categorical
        forceCategoricalField.setVisible(isNumber);

        // 2. Logic cho Sort Box
        if (isNumber) {
            // Nếu là số, chỉ hiện Sort Box khi "Force Categorical" được tick
            boolean forced = Boolean.TRUE.equals(forceCategoricalField.getValue());
            xAxisSortBox.setVisible(forced);
        } else {
            // Nếu là String/Date, luôn hiện Sort Box (vì nó luôn là Category)
            xAxisSortBox.setVisible(true);
        }
    }

    private void refreshXAxisSortOptions() {
        if (xAxisSortByField == null) return;
        List<String> options = new ArrayList<>();
        if (xAxis != null && !xAxis.isEmpty()) options.add(xAxis);
        for (MetricConfig m : metrics) options.add(m.getLabel());

        xAxisSortByField.setItems(options);
        String cur = xAxisSortByField.getValue();
        if (cur != null && !options.contains(cur)) xAxisSortByField.setValue(null);
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("xAxis", xAxis);
        if (contributionModeField.getValue() != null) node.put("contributionMode", contributionModeField.getValue().getId());
        Integer limit = seriesLimitField.getValue();
        node.put(ChartConstants.JSON_FIELD_SERIES_LIMIT, limit != null ? limit : ChartConstants.LIMIT_NONE);
        if (timeGrainBox.isVisible() && timeGrainField.getValue() != null) node.put("timeGrain", timeGrainField.getValue().getId());

        // Query Sort
        if (querySortMetric != null) node.set("querySortBy", objectMapper.valueToTree(querySortMetric));
        node.put("querySortDesc", Boolean.TRUE.equals(querySortDescField.getValue()));

        // X-Axis Config
        node.put("forceCategorical", Boolean.TRUE.equals(forceCategoricalField.getValue()));
        node.put("xAxisSortBy", xAxisSortByField.getValue());
        node.put("xAxisSortAsc", Boolean.TRUE.equals(xAxisSortAscField.getValue()));

        ArrayNode m = node.putArray("metrics"); metrics.forEach(m::addPOJO);
        ArrayNode d = node.putArray("dimensions"); dimensions.forEach(d::add);
        ArrayNode f = node.putArray("filters"); filters.forEach(f::addPOJO);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;
        this.xAxis = node.path("xAxis").asText(null);
        contributionModeField.setValue(ContributionMode.fromId(node.path("contributionMode").asText("none")));
        seriesLimitField.setValue(node.path(ChartConstants.JSON_FIELD_SERIES_LIMIT).asInt(0));
        timeGrainField.setValue(TimeGrain.fromId(node.path("timeGrain").asText(null)));

        metrics.clear();
        if (node.path("metrics").isArray()) node.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        dimensions.clear();
        if (node.path("dimensions").isArray()) node.path("dimensions").forEach(n -> dimensions.add(n.asText()));
        filters.clear();
        if (node.path("filters").isArray()) node.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xDrop != null) refreshUI();

        // Query Sort
        if (node.has("querySortBy")) {
            try { this.querySortMetric = objectMapper.treeToValue(node.path("querySortBy"), MetricConfig.class); } catch (Exception e) { this.querySortMetric = null; }
        } else this.querySortMetric = null;
        querySortDescField.setValue(node.path("querySortDesc").asBoolean(true));

        // X-Axis Config
        forceCategoricalField.setValue(node.path("forceCategorical").asBoolean(false));
        xAxisSortByField.setValue(node.path("xAxisSortBy").asText(null));
        xAxisSortAscField.setValue(node.path("xAxisSortAsc").asBoolean(true));

        checkTimeGrainVisibility();
        checkSortVisibility(); // Check lại lần cuối
    }

    // ... Boilerplate overrides ...
    @Override public void setAvailableFields(List<String> fields) {
        if (fieldsTypeMap.isEmpty() && fields != null) fields.forEach(f -> fieldsTypeMap.put(f, "string"));
        if (metricsDrop != null) DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
    }
    @Override public void setColumnTypes(Map<String, String> types) { this.fieldsTypeMap = types != null ? types : new HashMap<>(); checkTimeGrainVisibility(); checkSortVisibility(); }
    @Override public boolean isValid() { return xAxis != null && !metrics.isEmpty(); }
    @Override public String getMainDimension() { return xAxis; }
    @Override public void setMainDimension(String f) { xAxis = f; if(xDrop!=null) refreshUI(); checkTimeGrainVisibility(); checkSortVisibility();}
    @Override public String getMainMetric() { return metrics.isEmpty() ? null : metrics.get(0).getColumn(); }
    @Override public void setMainMetric(String f) { metrics.clear(); if(f!=null) metrics.add(new MetricConfig(f)); if(metricsDrop!=null) refreshUI(); }
    private void checkTimeGrainVisibility() {
        boolean isDate = false;
        if (xAxis != null && fieldsTypeMap.containsKey(xAxis)) {
            String type = fieldsTypeMap.get(xAxis);
            isDate = type != null && (type.toLowerCase().contains("date") || type.toLowerCase().contains("time"));
        }
        if (timeGrainBox != null) timeGrainBox.setVisible(isDate);
        if (!isDate && timeGrainField != null) timeGrainField.setValue(null);
        else if (isDate && timeGrainField != null && timeGrainField.getValue() == null) timeGrainField.setValue(TimeGrain.DAY);
    }
}