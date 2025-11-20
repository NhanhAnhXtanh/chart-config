package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.enums.ContributionMode; // Import Enum
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
import io.jmix.flowui.component.combobox.JmixComboBox;
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

    // Dùng JmixComboBox cho Contribution Mode
    @ViewComponent
    private JmixComboBox<ContributionMode> contributionModeField;

    // Data State
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

        // Setup Contribution Mode ComboBox
        contributionModeField.setItems(ContributionMode.values());

        // Hiển thị Label đẹp hơn (thay vì tên Enum mặc định)
        contributionModeField.setItemLabelGenerator(mode -> switch (mode) {
            case NONE -> "None";
            case ROW -> "Row (100% Stacked)";
            case SERIES -> "Series (% Total)";
        });

        // Set mặc định nếu chưa có
        if (contributionModeField.getValue() == null) {
            contributionModeField.setValue(ContributionMode.NONE);
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

    // ... (Các phần Override setAvailableFields, JSON, Migration giữ nguyên như cũ) ...
    @Override
    public void setAvailableFields(List<String> fields) {
        this.availableFields = fields != null ? fields : new ArrayList<>();
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("xAxis", xAxis);

        if (contributionModeField.getValue() != null) {
            node.put("contributionMode", contributionModeField.getValue().getId());
        }

        ArrayNode m = node.putArray("metrics"); metrics.forEach(m::addPOJO);
        ArrayNode d = node.putArray("dimensions"); dimensions.forEach(d::add);
        ArrayNode f = node.putArray("filters"); filters.forEach(f::addPOJO);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;
        this.xAxis = node.path("xAxis").asText(null);

        String modeId = node.path("contributionMode").asText("none");
        contributionModeField.setValue(ContributionMode.fromId(modeId));

        metrics.clear();
        if (node.path("metrics").isArray()) node.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });

        dimensions.clear();
        if (node.path("dimensions").isArray()) node.path("dimensions").forEach(n -> dimensions.add(n.asText()));

        filters.clear();
        if (node.path("filters").isArray()) node.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xDrop != null) refreshUI();
    }

    @Override public boolean isValid() { return xAxis != null && !metrics.isEmpty(); }
    @Override public String getMainDimension() { return xAxis; }
    @Override public void setMainDimension(String f) { xAxis = f; if(xDrop!=null) refreshUI(); }
    @Override public String getMainMetric() { return metrics.isEmpty() ? null : metrics.get(0).getColumn(); }
    @Override public void setMainMetric(String f) { metrics.clear(); if(f!=null) metrics.add(new MetricConfig(f)); if(metricsDrop!=null) refreshUI(); }
}