package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.company.chartconfig.utils.FilterRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@FragmentDescriptor("bar-config-fragment.xml")
public class BarConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired
    private ObjectMapper objectMapper;

    @ViewComponent private Div xDrop;
    @ViewComponent private Div metricsDrop;
    @ViewComponent private Div dimensionsDrop;
    @ViewComponent private Div filtersDrop;

    // Internal State
    private String xAxis;
    private final List<String> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        // Setup listeners
        DropZoneUtils.setup(xDrop, v -> this.xAxis = v);
        DropZoneUtils.setupMulti(metricsDrop, metrics);
        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        // Refresh UI from state (QUAN TRỌNG ĐỂ FIX LỖI LOAD DỮ LIỆU)
        refreshUI();
    }

    private void refreshUI() {
        DropZoneUtils.updateVisuals(xDrop, this.xAxis);
        DropZoneUtils.updateMulti(metricsDrop, this.metrics);
        DropZoneUtils.updateMulti(dimensionsDrop, this.dimensions);
        DropZoneUtils.updateFilters(filtersDrop, this.filters);
    }

    @Override
    public void setAvailableFields(List<String> fields) {
        // Bar chart logic không cần lưu list này, nhưng interface yêu cầu
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("xAxis", xAxis);

        ArrayNode mNode = node.putArray("metrics");
        metrics.forEach(mNode::add);

        ArrayNode dNode = node.putArray("dimensions");
        dimensions.forEach(dNode::add);

        ArrayNode fNode = node.putArray("filters");
        for (FilterRule f : filters) {
            ObjectNode fo = node.objectNode();
            fo.put("column", f.getColumn());
            fo.put("operator", f.getOperator());
            fo.put("value", f.getValue());
            fNode.add(fo);
        }
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;

        this.xAxis = node.path("xAxis").asText(null);

        this.metrics.clear();
        JsonNode mNode = node.path("metrics");
        if (mNode.isArray()) mNode.forEach(n -> this.metrics.add(n.asText()));

        this.dimensions.clear();
        JsonNode dNode = node.path("dimensions");
        if (dNode.isArray()) dNode.forEach(n -> this.dimensions.add(n.asText()));

        // Load Filters (Tự implement parsing JSON -> FilterRule)
        // ...

        // Update UI nếu đã attach
        if (xDrop != null) refreshUI();
    }

    @Override
    public boolean isValid() {
        return xAxis != null && !xAxis.isBlank() && !metrics.isEmpty();
    }

    // --- MIGRATION ---
    @Override
    public String getMainDimension() { return xAxis; }

    @Override
    public void setMainDimension(String field) {
        this.xAxis = field;
        if (xDrop != null) DropZoneUtils.updateVisuals(xDrop, field);
    }

    @Override
    public String getMainMetric() {
        return metrics.isEmpty() ? null : metrics.get(0);
    }

    @Override
    public void setMainMetric(String field) {
        this.metrics.clear();
        if (field != null) this.metrics.add(field);
        if (metricsDrop != null) DropZoneUtils.updateMulti(metricsDrop, metrics);
    }
}