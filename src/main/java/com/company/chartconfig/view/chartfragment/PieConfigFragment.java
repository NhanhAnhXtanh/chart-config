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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@FragmentDescriptor("pie-config-fragment.xml")
public class PieConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired
    private ObjectMapper objectMapper;
    @ViewComponent
    private Div dimensionDrop;
    @ViewComponent
    private Div metricDrop;
    @ViewComponent
    private Div filtersDrop;
    @ViewComponent
    private Checkbox donutCheckbox;
    @ViewComponent
    private ComboBox<Integer> seriesLimitField;

    private String dimension;
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();
    private List<String> availableFields = new ArrayList<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        // Setup Drop Zones
        DropZoneUtils.setup(dimensionDrop, val -> this.dimension = val);
        DropZoneUtils.setupMetricZone(metricDrop, metrics, availableFields, this::refreshUI);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        ChartUiUtils.setupSeriesLimitField(seriesLimitField);
        refreshUI();
    }

    private void refreshUI() {
        if (dimensionDrop != null) {
            DropZoneUtils.updateVisuals(dimensionDrop, dimension, v -> this.dimension = v);
            DropZoneUtils.updateMetricVisuals(metricDrop, metrics, availableFields, () -> {
            });
            DropZoneUtils.updateFilters(filtersDrop, filters);
        }
    }

    @Override
    public void setAvailableFields(List<String> fields) {
        this.availableFields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();

        // QUAN TRỌNG: Key là "dimension"
        node.put("dimension", dimension);

        node.put("isDonut", donutCheckbox.getValue());
        Integer limit = seriesLimitField.getValue();
        node.put(ChartConstants.JSON_FIELD_SERIES_LIMIT, limit != null ? limit : ChartConstants.LIMIT_NONE);

        ArrayNode mNode = node.putArray("metrics");
        metrics.forEach(mNode::addPOJO);

        ArrayNode fNode = node.putArray("filters");
        filters.forEach(fNode::addPOJO);

        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;

        //Đọc từ key "dimension"
        this.dimension = node.path("dimension").asText(null);

        this.donutCheckbox.setValue(node.path("isDonut").asBoolean(false));
        int limit = node.path(ChartConstants.JSON_FIELD_SERIES_LIMIT).asInt(ChartConstants.DEFAULT_LIMIT_VALUE);
        seriesLimitField.setValue(limit);

        metrics.clear();
        if (node.path("metrics").isArray()) {
            node.path("metrics").forEach(n -> {
                try {
                    metrics.add(objectMapper.treeToValue(n, MetricConfig.class));
                } catch (Exception e) {
                }
            });
        }

        filters.clear();
        if (node.path("filters").isArray()) {
            node.path("filters").forEach(n -> {
                try {
                    filters.add(objectMapper.treeToValue(n, FilterRule.class));
                } catch (Exception e) {
                }
            });
        }

        if (dimensionDrop != null) refreshUI();
    }

    @Override
    public boolean isValid() {
        return dimension != null && !dimension.isBlank() && !metrics.isEmpty();
    }

    // Interface Mapping
    @Override
    public String getMainDimension() {
        return dimension;
    }

    @Override
    public void setMainDimension(String field) {
        this.dimension = field;
        if (dimensionDrop != null) DropZoneUtils.updateVisuals(dimensionDrop, field, v -> this.dimension = v);
    }

    @Override
    public String getMainMetric() {
        return metrics.isEmpty() ? null : metrics.get(0).getColumn();
    }

    @Override
    public void setMainMetric(String field) {
        metrics.clear();
        if (field != null) metrics.add(new MetricConfig(field));
        if (metricDrop != null) refreshUI();
    }
}