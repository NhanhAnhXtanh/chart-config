package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.common.MetricConfig;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@FragmentDescriptor("gauge-config-fragment.xml")
public class GaugeConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired
    private ObjectMapper objectMapper;
    @ViewComponent
    private InstanceContainer<KeyValueEntity> gaugeSettingsDc;
    @ViewComponent
    private Div metricsDrop;
    @ViewComponent
    private Div filtersDrop;

    private final List<MetricConfig> metrics = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();
    private Map<String, String> fieldsTypeMap = new HashMap<>();

    @Subscribe
    public void onReady(ReadyEvent event) {
        KeyValueEntity entity = new KeyValueEntity();
        entity.setValue("minVal", 0.0);
        entity.setValue("maxVal", 100.0);
        gaugeSettingsDc.setItem(entity);

        DropZoneUtils.setupMetricZone(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), () -> {
        });
        DropZoneUtils.setupFilter(filtersDrop, filters, fieldsTypeMap);
        refreshUI();
    }

    private void refreshUI() {
        if (metricsDrop != null) {
            DropZoneUtils.updateMetricVisuals(metricsDrop, metrics, new ArrayList<>(fieldsTypeMap.keySet()), () -> {
            });
            DropZoneUtils.updateFilters(filtersDrop, filters, fieldsTypeMap);
        }
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        KeyValueEntity entity = gaugeSettingsDc.getItem();

        // Lưu Min/Max
        node.put("minVal", (Double) entity.getValue("minVal"));
        node.put("maxVal", (Double) entity.getValue("maxVal"));

        ArrayNode m = node.putArray("metrics");
        metrics.forEach(m::addPOJO);
        ArrayNode f = node.putArray("filters");
        filters.forEach(f::addPOJO);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;
        KeyValueEntity entity = gaugeSettingsDc.getItem();

        entity.setValue("minVal", node.path("minVal").asDouble(0.0));
        entity.setValue("maxVal", node.path("maxVal").asDouble(100.0));

        metrics.clear();
        if (node.path("metrics").isArray()) node.path("metrics").forEach(n -> {
            try {
                metrics.add(objectMapper.treeToValue(n, MetricConfig.class));
            } catch (Exception e) {
            }
        });
        filters.clear();
        if (node.path("filters").isArray()) node.path("filters").forEach(n -> {
            try {
                filters.add(objectMapper.treeToValue(n, FilterRule.class));
            } catch (Exception e) {
            }
        });

        refreshUI();
    }

    // --- Boilerplate ---
    @Override
    public void setAvailableFields(List<String> fields) {
        if (fieldsTypeMap.isEmpty() && fields != null) fields.forEach(f -> fieldsTypeMap.put(f, "string"));
        if (metricsDrop != null) refreshUI();
    }

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

    @Override
    public boolean isValid() {
        return !metrics.isEmpty();
    } // Gauge chỉ cần Metric, ko cần Dimension

    @Override
    public String getMainDimension() {
        return null;
    }

    @Override
    public void setMainDimension(String f) {
    }

    @Override
    public String getMainMetric() {
        return metrics.isEmpty() ? null : metrics.get(0).getColumn();
    }

    @Override
    public void setMainMetric(String f) {
        metrics.clear();
        if (f != null) metrics.add(new MetricConfig(f));
        refreshUI();
    }
}