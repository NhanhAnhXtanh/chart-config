package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;

import java.util.List;

@FragmentDescriptor("bar-config-fragment.xml")
public class BarConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private Div xDropZone;

    @ViewComponent
    private Div yDropZone;

    private List<String> fields;
    private String xValue;
    private String yValue;

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    // --- Getter / Setter ---

    public void setXField(String value) {
        this.xValue = value;
        // Cập nhật giao diện thông qua Utils
        DropZoneUtils.updateVisuals(xDropZone, value);
    }

    public void setYField(String value) {
        this.yValue = value;
        // Cập nhật giao diện thông qua Utils
        DropZoneUtils.updateVisuals(yDropZone, value);
    }

    public String getXField() {
        return xValue;
    }

    public String getYField() {
        return yValue;
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // --- SỬ DỤNG UTILS ĐỂ SETUP ---

        // Setup cho Trục X: Khi drop/clear thì cập nhật biến xValue
        DropZoneUtils.setup(xDropZone, val -> this.xValue = val);

        // Setup cho Trục Y: Khi drop/clear thì cập nhật biến yValue
        DropZoneUtils.setup(yDropZone, val -> this.yValue = val);
    }
}