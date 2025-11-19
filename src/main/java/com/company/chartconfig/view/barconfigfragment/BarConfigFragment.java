package com.company.chartconfig.view.barconfigfragment;

import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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

    public void setXField(String value) {
        this.xValue = value;
        setZoneValue(xDropZone, value);
    }

    public void setYField(String value) {
        this.yValue = value;
        setZoneValue(yDropZone, value);
    }

    public String getXField() {
        return xValue;
    }

    public String getYField() {
        return yValue;
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        setupDropZone(xDropZone, true);
        setupDropZone(yDropZone, false);
    }

    private void setupDropZone(Div zone, boolean isX) {
        styleDropZone(zone);

        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        // Khi có drop field
        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            setZoneValue(zone, field);
            if (isX) xValue = field;
            else yValue = field;
        }));

        // Hover bằng sự kiện DOM (vì DropTarget không có addDragEnterListener)
        zone.getElement().addEventListener("dragenter", e -> {
            zone.getStyle().set("background-color", "#e3f2fd");
            zone.getStyle().set("border-color", "#42a5f5");
        });
        zone.getElement().addEventListener("dragleave", e -> {
            zone.getStyle().set("background-color", "#fafafa");
            zone.getStyle().set("border-color", "#b0bec5");
        });

        // Click để xóa giá trị
        zone.addClickListener(e -> {
            if (isX) xValue = null;
            else yValue = null;
            setZoneValue(zone, null);
        });

        setZoneValue(zone, null);
    }

    private void styleDropZone(Div zone) {
        zone.getStyle()
                .set("border", "2px dashed #b0bec5")
                .set("border-radius", "10px")
                .set("padding", "10px")
                .set("min-width", "180px")
                .set("min-height", "45px")
                .set("background-color", "#fafafa")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("cursor", "pointer")
                .set("transition", "all 0.25s ease");
    }

    private void setZoneValue(Div zone, String value) {
        zone.removeAll();
        Span span;
        if (value == null || value.isBlank()) {
            span = new Span("(drop here)");
            span.getStyle()
                    .set("color", "#999")
                    .set("font-style", "italic")
                    .set("user-select", "none");
        } else {
            span = new Span(value);
            span.getStyle()
                    .set("font-weight", "600")
                    .set("color", "#333");
        }
        zone.add(span);
    }
}
