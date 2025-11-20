package com.company.chartconfig.utils;

import com.company.chartconfig.utils.FilterRule;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.function.Consumer;

public class DropZoneUtils {

    // --- 1. SINGLE VALUE SETUP ---
    public static void setup(Div zone, Consumer<String> onValueChange) {
        zone.setClassName("chart-drop-zone");
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) {
                updateVisuals(zone, field); // Update UI
                onValueChange.accept(field); // Update Data
            }
        }));

        setupCommonEvents(zone);

        zone.addClickListener(e -> {
            updateVisuals(zone, null);
            onValueChange.accept(null);
        });

        // Mặc định hiển thị rỗng
        updateVisuals(zone, null);
    }

    public static void updateVisuals(Div zone, String value) {
        zone.removeAll();

        if (value == null || value.isBlank()) {
            zone.removeClassName("filled"); // <-- Quan trọng: Để CSS căn giữa Hint
            Span hint = new Span("Kéo thả vào đây");
            hint.setClassName("chart-drop-zone-hint");
            zone.add(hint);
        } else {
            zone.addClassName("filled");    // <-- Quan trọng: Để CSS căn trái Chip
            Span chip = new Span(value);
            chip.setClassName("chart-drop-zone-chip");
            chip.setTitle(value); // Tooltip khi hover vào chip (nếu tên dài bị cắt)
            zone.add(chip);
        }
    }

    // --- 2. MULTI VALUE SETUP ---
    public static void setupMulti(Div zone, List<String> list) {
        zone.setClassName("chart-drop-zone");
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null && !list.contains(field)) {
                list.add(field);
                updateMulti(zone, list); // Update UI
            }
        }));

        setupCommonEvents(zone);

        zone.addClickListener(e -> {
            list.clear();
            updateMulti(zone, list);
        });

        updateMulti(zone, list);
    }

    public static void updateMulti(Div zone, List<String> list) {
        zone.removeAll();

        if (list == null || list.isEmpty()) {
            zone.removeClassName("filled");
            Span span = new Span("Kéo thả (Nhiều)");
            span.setClassName("chart-drop-zone-hint");
            zone.add(span);
        } else {
            zone.addClassName("filled");
            for (String v : list) {
                Span chip = new Span(v);
                chip.setClassName("chart-drop-zone-chip");
                chip.setTitle(v);
                zone.add(chip);
            }
        }
    }

    // --- 3. FILTER SETUP ---
    public static void setupFilter(Div zone, List<FilterRule> filters) {
        zone.setClassName("chart-drop-zone");
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String column = src.getElement().getAttribute("data-field-name");
            if (column != null) openFilterEditor(zone, filters, column);
        }));

        setupCommonEvents(zone);

        zone.addClickListener(e -> {
            filters.clear();
            updateFilters(zone, filters);
        });

        updateFilters(zone, filters);
    }

    public static void updateFilters(Div zone, List<FilterRule> filters) {
        zone.removeAll();
        if (filters == null || filters.isEmpty()) {
            zone.removeClassName("filled");
            Span s = new Span("Kéo thả Filter");
            s.setClassName("chart-drop-zone-hint");
            zone.add(s);
        } else {
            zone.addClassName("filled");
            for (FilterRule f : filters) {
                Span chip = new Span(f.getColumn() + " " + f.getOperator() + " " + f.getValue());
                chip.setClassName("chart-drop-zone-chip");
                zone.add(chip);
            }
        }
    }

    private static void openFilterEditor(Div zone, List<FilterRule> list, String column) {
        Dialog dialog = new Dialog();
        TextField valueField = new TextField("Value");
        ComboBox<String> operator = new ComboBox<>("Operator");
        operator.setItems("=", "!=", ">", "<", ">=", "<=", "LIKE", "IN");
        operator.setValue("=");

        Button ok = new Button("Apply", ev -> {
            list.add(new FilterRule(column, operator.getValue(), valueField.getValue()));
            dialog.close();
            updateFilters(zone, list);
        });

        dialog.add(new H3("Filter: " + column), operator, valueField, ok);
        dialog.open();
    }

    private static void setupCommonEvents(Div zone) {
        zone.getElement().addEventListener("dragenter", e -> zone.addClassName("drag-over"));
        zone.getElement().addEventListener("dragleave", e -> zone.removeClassName("drag-over"));
    }
}