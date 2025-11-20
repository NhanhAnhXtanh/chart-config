package com.company.chartconfig.view.barconfigfragment;

import com.company.chartconfig.utils.DropZoneUtils; // Import class tiện ích
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
    private List<String> fields;
    // DATA HOLDER
    public void setFields(List<String> fields) {
        this.fields = fields;
    }
    private String xAxis;
    private final List<String> metrics = new ArrayList<>();
    private final List<String> dimensions = new ArrayList<>();
    private final List<FilterRule> filters = new ArrayList<>();

    @Subscribe
    public void onReady(ReadyEvent event) {

        DropZoneUtils.setup(xDrop, v -> this.xAxis = v);

        DropZoneUtils.setupMulti(metricsDrop, metrics);

        DropZoneUtils.setupMulti(dimensionsDrop, dimensions);

        DropZoneUtils.setupFilter(filtersDrop, filters);
    }

    public String getxAxis() {
        return xAxis;
    }

    public void setxAxis(String xAxis) {
        this.xAxis = xAxis;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public List<FilterRule> getFilters() {
        return filters;
    }

    public Div getxDrop() {
        return xDrop;
    }

    public void setxDrop(Div xDrop) {
        this.xDrop = xDrop;
    }

    public Div getMetricsDrop() {
        return metricsDrop;
    }

    public void setMetricsDrop(Div metricsDrop) {
        this.metricsDrop = metricsDrop;
    }

    public Div getDimensionsDrop() {
        return dimensionsDrop;
    }

    public void setDimensionsDrop(Div dimensionsDrop) {
        this.dimensionsDrop = dimensionsDrop;
    }
}