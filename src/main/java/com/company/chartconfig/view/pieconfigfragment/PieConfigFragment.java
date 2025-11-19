package com.company.chartconfig.view.pieconfigfragment;

import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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

    public void setLabelField(String value) {
        this.labelValue = value;
        setZoneValue(labelDropZone, value);
    }

    public void setValueField(String value) {
        this.valueValue = value;
        setZoneValue(valueDropZone, value);
    }

    public String getLabelField() {
        return labelValue;
    }

    public String getValueField() {
        return valueValue;
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        setupDropZone(labelDropZone, true);
        setupDropZone(valueDropZone, false);
    }

    // ==========================================
    // Setup DropZone
    // ==========================================
    private void setupDropZone(Div zone, boolean isLabel) {
        styleDropZone(zone);

        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        // Khi có field thả vào
        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            setZoneValue(zone, field);
            if (isLabel) labelValue = field;
            else valueValue = field;
        }));

        // Hover effect dùng DOM event
        zone.getElement().addEventListener("dragenter", e -> {
            zone.getStyle().set("background-color", "#e3f2fd");
            zone.getStyle().set("border-color", "#42a5f5");
        });
        zone.getElement().addEventListener("dragleave", e -> {
            zone.getStyle().set("background-color", "#fafafa");
            zone.getStyle().set("border-color", "#b0bec5");
        });

        // Click để xóa field
        zone.addClickListener(e -> {
            if (isLabel) labelValue = null;
            else valueValue = null;
            setZoneValue(zone, null);
        });

        // Hiển thị mặc định
        setZoneValue(zone, null);
    }

    // ==========================================
    // Style zone
    // ==========================================
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

    // ==========================================
    // Hiển thị giá trị trong zone
    // ==========================================
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
