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
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.InstanceContainer;
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

    // --- CONTAINER ---
    @ViewComponent private InstanceContainer<KeyValueEntity> barSettingsDc;

    // --- DROP ZONES ---
    @ViewComponent private Div xDrop;
    @ViewComponent private Div metricsDrop;
    @ViewComponent private Div dimensionsDrop;
    @ViewComponent private Div filtersDrop;
    @ViewComponent private Div querySortDrop;

    // --- UI COMPONENTS (Để set visible/items) ---
    @ViewComponent private Checkbox forceCategoricalField;
    @ViewComponent private VerticalLayout xAxisSortBox;
    @ViewComponent private ComboBox<String> xAxisSortByField;
    @ViewComponent private Checkbox querySortDescField;
    @ViewComponent private VerticalLayout timeGrainBox;
    @ViewComponent private ComboBox<Integer> seriesLimitField;

    // State
    private String xAxis; // Vẫn cần biến local để DropZone dùng
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();
    private MetricConfig querySortMetric = null;
    private Map<String, String> fieldsTypeMap = new HashMap<>();
    @ViewComponent
    private JmixComboBox<TimeGrain> timeGrainField;
    @ViewComponent
    private JmixComboBox<ContributionMode> contributionModeField;

    @Subscribe
    public void onReady(ReadyEvent event) {
        ChartUiUtils.setupSeriesLimitField(seriesLimitField);
         contributionModeField.setValue(ContributionMode.NONE);
        timeGrainField.setValue(TimeGrain.DAY);
        // 1. Init Container Data (Default Values)
        KeyValueEntity entity = new KeyValueEntity();
        entity.setValue("xAxisSortAsc", true);
        entity.setValue("querySortDesc", true);
        entity.setValue("forceCategorical", false);
        entity.setValue("seriesLimit", ChartConstants.DEFAULT_LIMIT_VALUE);

        // Set Enum Object (Không set String ID)
        entity.setValue("contributionMode", ContributionMode.NONE);
        entity.setValue("timeGrain", TimeGrain.DAY);
        barSettingsDc.setItem(entity);

        // 2. Listener Container (Xử lý logic hiển thị phụ thuộc)
        barSettingsDc.addItemPropertyChangeListener(e -> {
            if ("forceCategorical".equals(e.getProperty())) checkSortVisibility();
        });

        // 3. Setup DropZones
        DropZoneUtils.setup(xDrop, v -> {
            this.xAxis = v;
            // Sync vào container
            barSettingsDc.getItem().setValue("xAxis", v);

            checkTimeGrainVisibility();
            refreshXAxisSortOptions();
            checkSortVisibility();
        });
        DropZoneUtils.setupMetricZone(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);
        DropZoneUtils.setupSingleMetricZone(querySortDrop, querySortMetric, new ArrayList<>(fieldsTypeMap.keySet()),
                (cfg) -> { this.querySortMetric = cfg; refreshUI(); },
                () -> { this.querySortMetric = null; refreshUI(); }
        );

        refreshUI();
    }

    private void refreshUI() {
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, xAxis, v -> {
                this.xAxis = v;
                barSettingsDc.getItem().setValue("xAxis", v);
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

            if (querySortDescField != null) querySortDescField.setVisible(querySortMetric != null);
        }
    }

    // --- LOGIC UI ---
    private void checkSortVisibility() {
        if (xAxis == null || xAxis.isEmpty()) {
            forceCategoricalField.setVisible(false);
            xAxisSortBox.setVisible(false);
            return;
        }
        String type = fieldsTypeMap.getOrDefault(xAxis, "string").toLowerCase();
        boolean isNumber = type.contains("number") || type.contains("int") || type.contains("double");

        forceCategoricalField.setVisible(isNumber);

        if (isNumber) {
            Boolean forced = barSettingsDc.getItem().getValue("forceCategorical");
            xAxisSortBox.setVisible(Boolean.TRUE.equals(forced));
        } else {
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

    // --- LOAD/SAVE JSON ---

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        KeyValueEntity entity = barSettingsDc.getItem();

        // 1. Lấy từ Container (Primitive types)
        node.put("xAxis", (String) entity.getValue("xAxis"));
        node.put("forceCategorical", (Boolean) entity.getValue("forceCategorical"));
        node.put("xAxisSortBy", (String) entity.getValue("xAxisSortBy"));
        node.put("xAxisSortAsc", (Boolean) entity.getValue("xAxisSortAsc"));
        node.put("querySortDesc", (Boolean) entity.getValue("querySortDesc"));
        node.put("seriesLimit", (Integer) entity.getValue("seriesLimit"));

        // 2. Lấy từ Container (Enum -> ID String)
        TimeGrain grain = timeGrainField.getValue();
        if (grain != null) node.put("timeGrain", grain.getId());

        ContributionMode mode = contributionModeField.getValue();
        if (mode != null) node.put("contributionMode", mode.getId());

        // 3. Lấy từ Local State (Collections & Object)
        if (querySortMetric != null) node.set("querySortBy", objectMapper.valueToTree(querySortMetric));

        ArrayNode m = node.putArray("metrics"); metrics.forEach(m::addPOJO);
        ArrayNode d = node.putArray("dimensions"); dimensions.forEach(d::add);
        ArrayNode f = node.putArray("filters"); filters.forEach(f::addPOJO);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;
        KeyValueEntity entity = barSettingsDc.getItem();

        // 1. Set vào Container (Primitive types)
        this.xAxis = node.path("xAxis").asText(null);
        entity.setValue("xAxis", this.xAxis);

        entity.setValue("forceCategorical", node.path("forceCategorical").asBoolean(false));
        entity.setValue("xAxisSortBy", node.path("xAxisSortBy").asText(null));
        entity.setValue("xAxisSortAsc", node.path("xAxisSortAsc").asBoolean(true));
        entity.setValue("querySortDesc", node.path("querySortDesc").asBoolean(true));
        entity.setValue("seriesLimit", node.path("seriesLimit").asInt(ChartConstants.DEFAULT_LIMIT_VALUE));

        // 2. Set vào Container (ID String -> Enum Object)
        String grainId = node.path("timeGrain").asText(null);
        entity.setValue("timeGrain", TimeGrain.fromId(grainId));

        String modeId = node.path("contributionMode").asText(null);
        entity.setValue("contributionMode", ContributionMode.fromId(modeId));

        // 3. Set vào Local State
        if (node.has("querySortBy")) {
            try { this.querySortMetric = objectMapper.treeToValue(node.path("querySortBy"), MetricConfig.class); } catch (Exception e) { this.querySortMetric = null; }
        } else this.querySortMetric = null;

        metrics.clear();
        if (node.path("metrics").isArray()) node.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        dimensions.clear();
        if (node.path("dimensions").isArray()) node.path("dimensions").forEach(n -> dimensions.add(n.asText()));
        filters.clear();
        if (node.path("filters").isArray()) node.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xDrop != null) refreshUI();
        checkTimeGrainVisibility();
        checkSortVisibility();
    }

    // ... Boilerplate interface methods ...
    @Override public void setAvailableFields(List<String> fields) {
        if (fieldsTypeMap.isEmpty() && fields != null) fields.forEach(f -> fieldsTypeMap.put(f, "string"));
        if (metricsDrop != null) DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
    }
    @Override public void setColumnTypes(Map<String, String> types) { this.fieldsTypeMap = types != null ? types : new HashMap<>(); checkTimeGrainVisibility(); checkSortVisibility(); }
    @Override public boolean isValid() { return xAxis != null && !metrics.isEmpty(); }
    @Override public String getMainDimension() { return xAxis; }
    @Override public void setMainDimension(String f) { xAxis = f; barSettingsDc.getItem().setValue("xAxis", f); if(xDrop!=null) refreshUI(); checkTimeGrainVisibility(); checkSortVisibility();}
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