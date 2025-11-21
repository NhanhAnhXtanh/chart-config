package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
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

    // --- UI COMPONENTS ---
    @ViewComponent private Div xDrop;
    @ViewComponent private Div metricsDrop;
    @ViewComponent private Div dimensionsDrop;
    @ViewComponent private Div filtersDrop;

    @ViewComponent private JmixComboBox<ContributionMode> contributionModeField;
    @ViewComponent private ComboBox<Integer> seriesLimitField;

    // --- INTERNAL STATE (SOURCE OF TRUTH) ---
    private String xAxis;
    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();

    private List<String> availableFields = new ArrayList<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        // 1. Setup Drop Zones (Sử dụng Utils để tái sử dụng logic)
        DropZoneUtils.setup(xDrop, v -> this.xAxis = v);

        // Metric Zone đặc biệt: Cần danh sách cột để hiện Dialog và Callback refresh
        DropZoneUtils.setupMetricZone(metricsDrop, metrics, availableFields, this::refreshUI);

        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        // 2. Setup UI Fields (Sử dụng Utils/Enum để tránh hard-code)
        ChartUiUtils.setupSeriesLimitField(seriesLimitField);

        // Contribution Mode (Items đã được load tự động qua itemsEnum trong XML)
        if (contributionModeField.getValue() == null) {
            contributionModeField.setValue(ContributionMode.NONE);
        }

        // 3. Render UI lần đầu
        refreshUI();
    }

    /**
     * Hàm đồng bộ dữ liệu từ biến State lên giao diện UI.
     * Được gọi khi: Init, Load JSON, hoặc sau khi Edit Metric Dialog.
     */
    private void refreshUI() {
        // Chỉ update khi component đã sẵn sàng (tránh null pointer nếu gọi quá sớm)
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, xAxis, v -> this.xAxis = v);
            DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, availableFields, () -> {}); // Callback rỗng vì setupMetricZone đã lo
            DropZoneUtils.updateMulti(dimensionsDrop, dimensions);
            DropZoneUtils.updateFilters(filtersDrop, filters);
        }
    }

    // ============================================================
    // IMPLEMENT INTERFACE: ChartConfigFragment
    // ============================================================

    @Override
    public void setAvailableFields(List<String> fields) {
        // Copy list để tránh reference issue
        this.availableFields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();

        // 1. Basic Config
        node.put("xAxis", xAxis);

        // 2. Settings (Enum & Constants)
        if (contributionModeField.getValue() != null) {
            node.put("contributionMode", contributionModeField.getValue().getId());
        }
        Integer limit = seriesLimitField.getValue();
        node.put(ChartConstants.JSON_FIELD_SERIES_LIMIT, limit != null ? limit : ChartConstants.LIMIT_NONE);

        // 3. Lists (Serialize POJO)
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

        // 1. Basic Config
        this.xAxis = node.path("xAxis").asText(null);

        // 2. Settings
        String modeId = node.path("contributionMode").asText("none");
        contributionModeField.setValue(ContributionMode.fromId(modeId));

        int limit = node.path(ChartConstants.JSON_FIELD_SERIES_LIMIT).asInt(ChartConstants.DEFAULT_LIMIT_VALUE);
        seriesLimitField.setValue(limit);

        // 3. Lists (Deserialize JSON -> Object)
        metrics.clear();
        if (node.path("metrics").isArray()) {
            node.path("metrics").forEach(n -> {
                try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) { /* Log error */ }
            });
        }

        dimensions.clear();
        if (node.path("dimensions").isArray()) {
            node.path("dimensions").forEach(n -> dimensions.add(n.asText()));
        }

        filters.clear();
        if (node.path("filters").isArray()) {
            node.path("filters").forEach(n -> {
                try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) { /* Log error */ }
            });
        }

        // Update UI ngay lập tức nếu Fragment đã được attach
        if (xDrop != null) {
            refreshUI();
        }
    }

    @Override
    public boolean isValid() {
        // Validate tối thiểu: Phải có Trục X và ít nhất 1 Metric
        return xAxis != null && !xAxis.isBlank() && !metrics.isEmpty();
    }

    // ============================================================
    // MIGRATION SUPPORT (Chuyển đổi dữ liệu giữa các Chart)
    // ============================================================

    @Override
    public String getMainDimension() {
        return xAxis;
    }

    @Override
    public void setMainDimension(String field) {
        this.xAxis = field;
        if (xDrop != null) DropZoneUtils.updateVisuals(xDrop, field, v -> this.xAxis = v);
    }

    @Override
    public String getMainMetric() {
        // Lấy metric đầu tiên làm đại diện (nếu có)
        return metrics.isEmpty() ? null : metrics.get(0).getColumn();
    }

    @Override
    public void setMainMetric(String field) {
        this.metrics.clear();
        if (field != null) {
            this.metrics.add(new MetricConfig(field));
        }
        if (metricsDrop != null) refreshUI();
    }
}