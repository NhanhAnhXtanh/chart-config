package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.company.chartconfig.view.common.MetricConfig;
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
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();

    private List<String> availableFields = new ArrayList<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        // 1. Setup Listeners
        DropZoneUtils.setup(xDrop, v -> this.xAxis = v);

        // Metric dùng setup riêng biệt (Có Dialog)
        DropZoneUtils.setupMetricZone(metricsDrop, metrics, availableFields, this::refreshUI);

        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        // 2. Refresh UI (Để hiển thị dữ liệu nếu được load từ DB trước khi Ready)
        refreshUI();
    }

    private void refreshUI() {
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, xAxis, val -> this.xAxis = val); // Truyền callback update
            DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, availableFields, () -> {});
            DropZoneUtils.updateMulti(dimensionsDrop, dimensions);
            DropZoneUtils.updateFilters(filtersDrop, filters);
        }
    }

    @Override
    public void setAvailableFields(List<String> fields) {
        this.availableFields = fields;
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("xAxis", xAxis);

        // Serialize List<MetricConfig>
        ArrayNode mNode = node.putArray("metrics");
        metrics.forEach(mNode::addPOJO);

        // Serialize List<String>
        ArrayNode dNode = node.putArray("dimensions");
        dimensions.forEach(dNode::add);

        // Serialize List<FilterRule>
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

        // Deserialize Metrics
        this.metrics.clear();
        JsonNode mNode = node.path("metrics");
        if (mNode.isArray()) {
            mNode.forEach(n -> {
                try {
                    metrics.add(objectMapper.treeToValue(n, MetricConfig.class));
                } catch (Exception e) { e.printStackTrace(); }
            });
        }

        // Deserialize Dimensions
        this.dimensions.clear();
        JsonNode dNode = node.path("dimensions");
        if (dNode.isArray()) {
            dNode.forEach(n -> dimensions.add(n.asText()));
        }

        // Deserialize Filters (Nếu cần)
        // ...

        // Update UI ngay lập tức nếu đã attach
        if (xDrop != null) refreshUI();
    }

    @Override
    public boolean isValid() {
        return xAxis != null && !xAxis.isBlank() && !metrics.isEmpty();
    }

    // --- MIGRATION SUPPORT ---
    @Override
    public String getMainDimension() { return xAxis; }

    @Override
    public void setMainDimension(String field) {
        this.xAxis = field;
        if (xDrop != null) refreshUI();
    }

    @Override
    public String getMainMetric() {
        return metrics.isEmpty() ? null : metrics.get(0).getColumn();
    }

    @Override
    public void setMainMetric(String field) {
        this.metrics.clear();
        if (field != null) this.metrics.add(new MetricConfig(field));
        if (metricsDrop != null) refreshUI();
    }
}