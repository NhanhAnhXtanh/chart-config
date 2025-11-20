package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.view.config.common.ChartConfigFragment; // Import Interface chung
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@FragmentDescriptor("pie-config-fragment.xml")
public class PieConfigFragment extends Fragment<VerticalLayout> implements ChartConfigFragment {

    @Autowired
    private ObjectMapper objectMapper;

    @ViewComponent
    private Div labelDropZone;

    @ViewComponent
    private Div valueDropZone;

    // Internal State (Lưu trữ dữ liệu)
    private String labelField;
    private String valueField;

    @Subscribe
    public void onReady(ReadyEvent event) {
        // 1. Setup Logic kéo thả
        DropZoneUtils.setup(labelDropZone, val -> this.labelField = val);
        DropZoneUtils.setup(valueDropZone, val -> this.valueField = val);

        // 2. Vẽ lại UI (quan trọng để hiển thị dữ liệu khi Edit)
        refreshUI();
    }

    private void refreshUI() {
        DropZoneUtils.updateVisuals(labelDropZone, this.labelField);
        DropZoneUtils.updateVisuals(valueDropZone, this.valueField);
    }

    // ============================================================
    // IMPLEMENT INTERFACE: ChartConfigFragment
    // ============================================================

    @Override
    public void setAvailableFields(List<String> fields) {
        // Pie chart có thể không cần lưu list này
    }

    @Override
    public ObjectNode getConfigurationJson() {
        // Đóng gói dữ liệu thành JSON để lưu
        ObjectNode node = objectMapper.createObjectNode();
        node.put("labelField", labelField);
        node.put("valueField", valueField);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        // Parse JSON đổ vào biến
        if (node == null) return;

        this.labelField = node.path("labelField").asText(null);
        this.valueField = node.path("valueField").asText(null);

        // Cập nhật UI nếu Fragment đã sẵn sàng
        if (labelDropZone != null) {
            refreshUI();
        }
    }

    @Override
    public boolean isValid() {
        // Validate bắt buộc phải có Label và Value
        return labelField != null && !labelField.isBlank()
                && valueField != null && !valueField.isBlank();
    }

    // ============================================================
    // MIGRATION SUPPORT (Chuyển đổi dữ liệu giữa các Chart)
    // ============================================================

    @Override
    public String getMainDimension() {
        // Với Pie, Dimension chính là Label (Phần chữ)
        return labelField;
    }

    @Override
    public void setMainDimension(String field) {
        this.labelField = field;
        if (labelDropZone != null) DropZoneUtils.updateVisuals(labelDropZone, field);
    }

    @Override
    public String getMainMetric() {
        // Với Pie, Metric chính là Value (Phần số)
        return valueField;
    }

    @Override
    public void setMainMetric(String field) {
        this.valueField = field;
        if (valueDropZone != null) DropZoneUtils.updateVisuals(valueDropZone, field);
    }
}