package com.company.chartconfig.view.chartfragment;

import com.company.chartconfig.utils.DropZoneUtils;
import com.company.chartconfig.utils.FilterRule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;

import java.util.ArrayList;
import java.util.List;

@FragmentDescriptor("bar-config-fragment.xml")
public class BarConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private Div xDrop;
    @ViewComponent
    private Div metricsDrop;
    @ViewComponent
    private Div dimensionsDrop;
    @ViewComponent
    private Div filtersDrop;

    private List<String> fields; // Danh sách tất cả các trường (nếu cần validate)

    // STATE VARIABLES (Nơi lưu trữ dữ liệu thực sự)
    private String xAxis;
    private final List<String> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    // =============================================================
    // 1. SETTERS: Để View cha gọi khi load dữ liệu (Edit Mode)
    // =============================================================

    public void setXAxisValue(String value) {
        this.xAxis = value;
        // Nếu UI đã sẵn sàng, cập nhật luôn. Nếu chưa, onReady sẽ lo.
        if (xDrop != null) {
            DropZoneUtils.updateVisuals(xDrop, value);
        }
    }

    public void setMetricsList(List<String> values) {
        this.metrics.clear();
        if (values != null) this.metrics.addAll(values);
        if (metricsDrop != null) {
            DropZoneUtils.updateMulti(metricsDrop, this.metrics);
        }
    }

    public void setDimensionsList(List<String> values) {
        this.dimensions.clear();
        if (values != null) this.dimensions.addAll(values);
        if (dimensionsDrop != null) {
            DropZoneUtils.updateMulti(dimensionsDrop, this.dimensions);
        }
    }

    // Tương tự cho Filters nếu cần load từ JSON...

    // =============================================================
    // 2. GETTERS: Để View cha lấy dữ liệu khi Save
    // =============================================================
    public String getxAxis() { return xAxis; }
    public List<String> getMetrics() { return metrics; }
    public List<String> getDimensions() { return dimensions; }
    public List<FilterRule> getFilters() { return filters; }

    // =============================================================
    // 3. LIFECYCLE: Setup logic kéo thả
    // =============================================================
    @Subscribe
    public void onReady(ReadyEvent event) {
        // A. Setup logic kéo thả và gán callback cập nhật biến
        DropZoneUtils.setup(xDrop, v -> this.xAxis = v);
        DropZoneUtils.setupMulti(metricsDrop, metrics);
        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);
        DropZoneUtils.setupFilter(filtersDrop, filters);

        // B. QUAN TRỌNG: Vẽ lại dữ liệu hiện có lên màn hình
        // (Vì hàm setup ở trên có thể đã reset UI về "Drop here")
        DropZoneUtils.updateVisuals(xDrop, this.xAxis);
        DropZoneUtils.updateMulti(metricsDrop, this.metrics);
        DropZoneUtils.updateMulti(dimensionsDrop, this.dimensions);
        // DropZoneUtils.updateFilters(filtersDrop, this.filters);
    }
}