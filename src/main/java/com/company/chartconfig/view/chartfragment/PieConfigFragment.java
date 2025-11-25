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

@FragmentDescriptor("pie-config-fragment.xml")
public class PieConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired private ObjectMapper objectMapper;
    @ViewComponent private InstanceContainer<KeyValueEntity> pieSettingsDc;

    @ViewComponent private Div dimensionDrop;
    @ViewComponent private Div metricDrop;
    @ViewComponent private Div filtersDrop;
    @ViewComponent private ComboBox<Integer> seriesLimitField;

    private String dimension;
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();
    private Map<String, String> fieldsTypeMap = new HashMap<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        ChartUiUtils.setupSeriesLimitField(seriesLimitField);

        KeyValueEntity entity = new KeyValueEntity();
        entity.setValue("isDonut", false);
        entity.setValue("seriesLimit", ChartConstants.DEFAULT_LIMIT_VALUE);
        pieSettingsDc.setItem(entity);

        DropZoneUtils.setup(dimensionDrop, val -> {
            this.dimension = val;
            pieSettingsDc.getItem().setValue("dimension", val);
        });
        DropZoneUtils.setupMetricZone(metricDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
        DropZoneUtils.setupFilter(filtersDrop, filters, fieldsTypeMap);

        refreshUI();
    }

    private void refreshUI() {
        if (dimensionDrop != null) {
            DropZoneUtils.updateVisuals(dimensionDrop, dimension, v -> { this.dimension = v; pieSettingsDc.getItem().setValue("dimension", v); });
            DropZoneUtils.updateMetricVisuals(metricDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), () -> {});
            DropZoneUtils.updateFilters(filtersDrop, filters, fieldsTypeMap);
        }
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        KeyValueEntity entity = pieSettingsDc.getItem();

        node.put("dimension", (String) entity.getValue("dimension"));
        node.put("isDonut", (Boolean) entity.getValue("isDonut"));
        // Map UI seriesLimit -> JSON rowLimit (Quy chuẩn chung)
        node.put("rowLimit", (Integer) entity.getValue("seriesLimit"));

        ArrayNode mNode = node.putArray("metrics"); metrics.forEach(mNode::addPOJO);
        ArrayNode fNode = node.putArray("filters"); filters.forEach(fNode::addPOJO);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;
        KeyValueEntity entity = pieSettingsDc.getItem();

        this.dimension = node.path("dimension").asText(null);
        entity.setValue("dimension", this.dimension);
        entity.setValue("isDonut", node.path("isDonut").asBoolean(false));

        // Map JSON rowLimit -> UI seriesLimit
        entity.setValue("seriesLimit", node.path("rowLimit").asInt(ChartConstants.DEFAULT_LIMIT_VALUE));

        metrics.clear();
        if (node.path("metrics").isArray()) node.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        filters.clear();
        if (node.path("filters").isArray()) node.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (dimensionDrop != null) refreshUI();
    }

    // ... Boilerplate ...
    @Override
    public void setColumnTypes(Map<String, String> types) {
        // [QUAN TRỌNG] Không được gán map mới (this.fieldsTypeMap = ...),
        // vì DropZone đang giữ tham chiếu cũ. Phải xóa nội dung và thêm mới.
        this.fieldsTypeMap.clear();
        if (types != null) {
            this.fieldsTypeMap.putAll(types);
        }

        // Cập nhật lại UI Filter để nó nhận type mới (để biết hiện Date Picker hay Number input)
        if (filtersDrop != null) {
            DropZoneUtils.updateFilters(filtersDrop, filters, fieldsTypeMap);
        }
    }

    @Override public void setAvailableFields(List<String> fields) {
        if (fieldsTypeMap.isEmpty() && fields != null) fields.forEach(f -> fieldsTypeMap.put(f, "string"));
        if (metricDrop != null) DropZoneUtils.updateMetricVisuals(metricDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), this::refreshUI);
    }
    @Override public boolean isValid() { return dimension != null && !metrics.isEmpty(); }
    @Override public String getMainDimension() { return dimension; }
    @Override public void setMainDimension(String f) { dimension = f; pieSettingsDc.getItem().setValue("dimension", f); if (dimensionDrop != null) refreshUI(); }
    @Override public String getMainMetric() { return metrics.isEmpty() ? null : metrics.get(0).getColumn(); }
    @Override public void setMainMetric(String f) { metrics.clear(); if (f != null) metrics.add(new MetricConfig(f)); if (metricDrop != null) refreshUI(); }
}