package com.company.chartconfig.view.pieconfigfragment;

import com.company.chartconfig.utils.DropZoneUtils; // Import class tiện ích vừa tạo
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;

import java.util.List;

@FragmentDescriptor("pie-config-fragment.xml")
public class PieConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private Div labelDropZone;
    @ViewComponent
    private Div valueDropZone;

    private List<String> fields;
    private String labelValue;
    private String valueValue;

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    // --- Getter / Setter ---
    public void setLabelField(String value) {
        this.labelValue = value;
        DropZoneUtils.updateVisuals(labelDropZone, value); // Update giao diện
    }

    public void setValueField(String value) {
        this.valueValue = value;
        DropZoneUtils.updateVisuals(valueDropZone, value); // Update giao diện
    }

    public String getLabelField() { return labelValue; }
    public String getValueField() { return valueValue; }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // --- SỬ DỤNG UTILS Ở ĐÂY ---

        // Setup Label Zone: cập nhật biến labelValue khi có thay đổi
        DropZoneUtils.setup(labelDropZone, val -> this.labelValue = val);

        // Setup Value Zone: cập nhật biến valueValue khi có thay đổi
        DropZoneUtils.setup(valueDropZone, val -> this.valueValue = val);
    }
}