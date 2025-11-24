package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.utils.ChartUiUtils;
import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.entity.KeyValueEntity;
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

@FragmentDescriptor("line-config-fragment.xml")
public class LineConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired private ObjectMapper objectMapper;
    @ViewComponent private InstanceContainer<KeyValueEntity> lineSettingsDc;

    @ViewComponent private Div xDrop;
    @ViewComponent private Div metricsDrop;
    @ViewComponent private Div dimensionsDrop;
    @ViewComponent private Div filtersDrop;

    // CÓ CẢ 2 COMBOBOX
    @ViewComponent private ComboBox<Integer> seriesLimitField;
    @ViewComponent private ComboBox<Integer> rowLimitField;

    private String xAxis;
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();
    private Map<String, String> fieldsTypeMap = new HashMap<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        // Setup cả 2
        ChartUiUtils.setupSeriesLimitField(seriesLimitField);
        ChartUiUtils.setupRowsLimitField(rowLimitField);

        // Init Data
        KeyValueEntity entity = new KeyValueEntity();
        entity.setValue("seriesLimit", ChartConstants.DEFAULT_LIMIT_VALUE);     // Mặc định 0
        entity.setValue("rowLimit",ChartConstants.DEFAULT_LIMIT_VALUE);    // Mặc định 10000
        lineSettingsDc.setItem(entity);

        DropZoneUtils.setup(xDrop, v -> {
            this.xAxis = v;
            if (lineSettingsDc.getItemOrNull() != null) {
                lineSettingsDc.getItem().setValue("xAxis", v);
            }
        });
        DropZoneUtils.setupMetricZone(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        refreshUI();
    }

    private void refreshUI() {
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, xAxis, v -> {
                this.xAxis = v;
                if (lineSettingsDc.getItemOrNull() != null) lineSettingsDc.getItem().setValue("xAxis", v);
            });
            DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), () -> {});
            DropZoneUtils.updateMulti(dimensionsDrop, dimensions);
            DropZoneUtils.updateFilters(filtersDrop, filters);
        }
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        KeyValueEntity entity = lineSettingsDc.getItem();

        node.put("xAxis", (String) entity.getValue("xAxis"));
        // Lưu cả 2
        node.put("seriesLimit", (Integer) entity.getValue("seriesLimit"));
        node.put("rowLimit", (Integer) entity.getValue("rowLimit"));

        ArrayNode mNode = node.putArray("metrics"); metrics.forEach(mNode::addPOJO);
        ArrayNode dNode = node.putArray("dimensions"); dimensions.forEach(dNode::add);
        ArrayNode fNode = node.putArray("filters"); filters.forEach(fNode::addPOJO);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;
        KeyValueEntity entity = lineSettingsDc.getItem();

        this.xAxis = node.path("xAxis").asText(null);
        entity.setValue("xAxis", this.xAxis);

        // Đọc cả 2
        entity.setValue("seriesLimit", node.path("seriesLimit").asInt(ChartConstants.DEFAULT_LIMIT_VALUE));
        entity.setValue("rowLimit", node.path("rowLimit").asInt(ChartConstants.DEFAULT_LIMIT_VALUE));

        metrics.clear();
        if (node.path("metrics").isArray()) node.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        dimensions.clear();
        if (node.path("dimensions").isArray()) node.path("dimensions").forEach(n -> dimensions.add(n.asText()));
        filters.clear();
        if (node.path("filters").isArray()) node.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xDrop != null) refreshUI();
    }

    // ... Boilerplate ...
    @Override public void setAvailableFields(List<String> fields) { if (fieldsTypeMap.isEmpty() && fields != null) fields.forEach(f -> fieldsTypeMap.put(f, "string")); if (metricsDrop != null) DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI); }
    @Override public void setColumnTypes(Map<String, String> types) { this.fieldsTypeMap = types != null ? types : new HashMap<>(); }
    @Override public boolean isValid() { return xAxis != null && !xAxis.isBlank() && !metrics.isEmpty(); }
    @Override public String getMainDimension() { return xAxis; }
    @Override public void setMainDimension(String f) { xAxis = f; if (lineSettingsDc.getItemOrNull() != null) lineSettingsDc.getItem().setValue("xAxis", f); if (xDrop != null) refreshUI(); }
    @Override public String getMainMetric() { return metrics.isEmpty() ? null : metrics.get(0).getColumn(); }
    @Override public void setMainMetric(String f) { metrics.clear(); if (f != null) metrics.add(new MetricConfig(f)); if (metricsDrop != null) refreshUI(); }
}