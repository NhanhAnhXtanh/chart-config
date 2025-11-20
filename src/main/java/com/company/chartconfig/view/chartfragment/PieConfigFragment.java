package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
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

    // Internal State
    private String labelField;
    private String valueField;

    @Subscribe
    public void onReady(ReadyEvent event) {
        // Setup Listeners
        DropZoneUtils.setup(labelDropZone, val -> this.labelField = val);
        DropZoneUtils.setup(valueDropZone, val -> this.valueField = val);

        refreshUI();
    }

    private void refreshUI() {
        // Cập nhật UI và truyền callback để xử lý nút Xóa
        if (labelDropZone != null) {
            DropZoneUtils.updateVisuals(labelDropZone, this.labelField, val -> this.labelField = val);
            DropZoneUtils.updateVisuals(valueDropZone, this.valueField, val -> this.valueField = val);
        }
    }

    // ============================================================
    // IMPLEMENT INTERFACE
    // ============================================================

    @Override
    public void setAvailableFields(List<String> fields) {
        // Pie chart đơn giản, chưa cần list này (trừ khi mở rộng sau này)
    }

    @Override
    public ObjectNode getConfigurationJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("labelField", labelField);
        node.put("valueField", valueField);
        return node;
    }

    @Override
    public void setConfigurationJson(JsonNode node) {
        if (node == null) return;

        this.labelField = node.path("labelField").asText(null);
        this.valueField = node.path("valueField").asText(null);

        if (labelDropZone != null) {
            refreshUI();
        }
    }

    @Override
    public boolean isValid() {
        return labelField != null && !labelField.isBlank()
                && valueField != null && !valueField.isBlank();
    }

    // --- MIGRATION SUPPORT ---

    @Override
    public String getMainDimension() {
        return labelField;
    }

    @Override
    public void setMainDimension(String field) {
        this.labelField = field;
        if (labelDropZone != null) refreshUI();
    }

    @Override
    public String getMainMetric() {
        return valueField;
    }

    @Override
    public void setMainMetric(String field) {
        this.valueField = field;
        if (valueDropZone != null) refreshUI();
    }
}