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
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@FragmentDescriptor("line-config-fragment.xml")
public class LineConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired private ObjectMapper objectMapper;

    @ViewComponent private Div xDrop;
    @ViewComponent private Div metricsDrop;
    @ViewComponent private Div dimensionsDrop;
    @ViewComponent private Div filtersDrop;

    @ViewComponent private ComboBox<Integer> seriesLimitField;
    @ViewComponent private ComboBox<Integer> rowLimitField;

    // State
    private String xAxis;
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();

    private List<String> availableFields = new ArrayList<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        // Setup Drop Zones
        DropZoneUtils.setup(xDrop, v -> this.xAxis = v);
        DropZoneUtils.setupMetricZone(metricsDrop, metrics, availableFields, this::refreshUI);
        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        // Setup Limits
        ChartUiUtils.setupSeriesLimitField(seriesLimitField);

        // Setup Row Limit (Tái sử dụng logic setup combo box)
        ChartUiUtils.setupSeriesLimitField(rowLimitField);
        // Mặc định Row Limit có thể cao hơn
        if (rowLimitField.getValue() == null || rowLimitField.getValue().equals(ChartConstants.DEFAULT_LIMIT_VALUE)) {
            rowLimitField.setValue(10000);
        }

        refreshUI();
    }

    private void refreshUI() {
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, xAxis, v -> this.xAxis = v);
            DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, availableFields, () -> {});
            DropZoneUtils.updateMulti(dimensionsDrop, dimensions);
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

        node.put("xAxis", xAxis);

        Integer sLimit = seriesLimitField.getValue();
        node.put("seriesLimit", sLimit != null ? sLimit : 0);

        Integer rLimit = rowLimitField.getValue();
        node.put("rowLimit", rLimit != null ? rLimit : 10000);

        ArrayNode mNode = node.putArray("metrics");
        metrics.forEach(mNode::addPOJO);

        ArrayNode dNode = node.putArray("dimensions");
        dimensions.forEach(dNode::add);

        ArrayNode fNode = node.putArray("filters");
        filters.forEach(fNode::addPOJO);

        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;

        this.xAxis = node.path("xAxis").asText(null);

        seriesLimitField.setValue(node.path("seriesLimit").asInt(100));
        rowLimitField.setValue(node.path("rowLimit").asInt(10000));

        metrics.clear();
        if (node.path("metrics").isArray()) {
            node.path("metrics").forEach(n -> {
                try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {}
            });
        }

        dimensions.clear();
        if (node.path("dimensions").isArray()) {
            node.path("dimensions").forEach(n -> dimensions.add(n.asText()));
        }

        filters.clear();
        if (node.path("filters").isArray()) {
            node.path("filters").forEach(n -> {
                try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {}
            });
        }

        if (xDrop != null) refreshUI();
    }

    @Override
    public boolean isValid() {
        return xAxis != null && !xAxis.isBlank() && !metrics.isEmpty();
    }

    @Override
    public String getMainDimension() { return xAxis; }

    @Override
    public void setMainDimension(String field) {
        this.xAxis = field;
        if (xDrop != null) DropZoneUtils.updateVisuals(xDrop, field, v -> this.xAxis = v);
    }

    @Override
    public String getMainMetric() { return metrics.isEmpty() ? null : metrics.get(0).getColumn(); }

    @Override
    public void setMainMetric(String field) {
        this.metrics.clear();
        if (field != null) this.metrics.add(new MetricConfig(field));
        if (metricsDrop != null) refreshUI();
    }
}