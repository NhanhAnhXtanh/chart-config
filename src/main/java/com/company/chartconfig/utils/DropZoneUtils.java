package com.company.chartconfig.utils;

import com.company.chartconfig.view.common.MetricConfig;
import com.company.chartconfig.view.dialog.MetricConfigDialog;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropZoneUtils {

    // ============================================================
    // 1. SINGLE VALUE (X-AXIS)
    // ============================================================
    public static void setup(Div zone, Consumer<String> onValueChange) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);
        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) { onValueChange.accept(field); updateVisuals(zone, field, onValueChange); }
        }));
        updateVisuals(zone, null, onValueChange);
    }

    public static void updateVisuals(Div zone, String value, Consumer<String> onUpdate) {
        zone.removeAll();
        if (value == null || value.isBlank()) {
            renderEmptyState(zone, "Kéo thả vào đây");
        } else {
            zone.addClassName("filled");
            Span chip = createChip(value, () -> {
                updateVisuals(zone, null, onUpdate);
                onUpdate.accept(null);
            }, null);
            zone.add(chip);
        }
    }

    // ============================================================
    // 2. METRIC LIST (Y-AXIS)
    // ============================================================
    public static void setupMetricZone(Div zone, List<MetricConfig> metrics, List<String> cols, Runnable onUpdate) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) {
                MetricConfig newConfig = new MetricConfig(field);
                new MetricConfigDialog(newConfig, cols, (savedConfig) -> {
                    metrics.add(savedConfig);
                    updateMetricVisuals(zone, metrics, cols, onUpdate);
                    onUpdate.run();
                }).open();
            }
        }));
    }

    public static void updateMetricVisuals(Div zone, List<MetricConfig> metrics, List<String> cols, Runnable onUpdate) {
        zone.removeAll();
        if (metrics.isEmpty()) { renderEmptyState(zone, "Kéo thả Metric"); return; }
        zone.addClassName("filled");
        for (MetricConfig m : metrics) {
            Span chip = createChip(m.getLabel(), () -> {
                metrics.remove(m);
                updateMetricVisuals(zone, metrics, cols, onUpdate);
                onUpdate.run();
            }, () -> {
                new MetricConfigDialog(m, cols, (updated) -> {
                    updateMetricVisuals(zone, metrics, cols, onUpdate);
                    onUpdate.run();
                }).open();
            });
            zone.add(chip);
        }
    }

    // ============================================================
    // 3. SINGLE METRIC (SORT BY) - MỚI
    // ============================================================
    public static void setupSingleMetricZone(Div zone, MetricConfig currentMetric, List<String> cols, Consumer<MetricConfig> onSave, Runnable onClear) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) {
                MetricConfig newConfig = new MetricConfig(field);
                new MetricConfigDialog(newConfig, cols, (savedConfig) -> {
                    onSave.accept(savedConfig);
                }).open();
            }
        }));
        updateSingleMetricVisuals(zone, currentMetric, cols, onSave, onClear);
    }

    public static void updateSingleMetricVisuals(Div zone, MetricConfig metric, List<String> cols, Consumer<MetricConfig> onSave, Runnable onClear) {
        zone.removeAll();
        if (metric == null) {
            renderEmptyState(zone, "Kéo thả Metric Sort");
        } else {
            zone.addClassName("filled");
            Span chip = createChip(metric.getLabel(), onClear, () -> {
                new MetricConfigDialog(metric, cols, onSave).open();
            });
            zone.add(chip);
        }
    }

    // ============================================================
    // 4. HELPER METHODS (Multi, Filters, Chip...)
    // ============================================================
    public static void setupMulti(Div zone, List<String> list) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);
        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null && !list.contains(field)) {
                list.add(field);
                updateMulti(zone, list);
            }
        }));
        updateMulti(zone, list);
    }

    public static void updateMulti(Div zone, List<String> list) {
        zone.removeAll();
        if (list.isEmpty()) renderEmptyState(zone, "Kéo thả (Nhiều)");
        else { zone.addClassName("filled"); list.forEach(v -> zone.add(createChip(v, ()->{list.remove(v); updateMulti(zone,list);}, null))); }
    }

    public static void setupFilter(Div zone, List<com.company.chartconfig.utils.FilterRule> filters) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String col = src.getElement().getAttribute("data-field-name");
            if (col != null) {
                // Mở dialog tạo mới
                openFilterEditor(null, col, rule -> {
                    filters.add(rule);
                    updateFilters(zone, filters);
                });
            }
        }));
        updateFilters(zone, filters);
    }

    public static void updateFilters(Div zone, List<com.company.chartconfig.utils.FilterRule> filters) {
        zone.removeAll();
        if (filters.isEmpty()) {
            renderEmptyState(zone, "Kéo thả Filter");
        } else {
            zone.addClassName("filled");
            for (com.company.chartconfig.utils.FilterRule f : filters) {
                String label = f.getColumn() + " " + f.getOperator() + " " + f.getValue();

                // Tạo Chip: Xóa thì remove khỏi list, Click thì mở dialog sửa
                zone.add(createChip(label,
                        () -> { filters.remove(f); updateFilters(zone, filters); }, // On Delete
                        () -> openFilterEditor(f, f.getColumn(), rule -> {          // On Click (Edit)
                            // Vì FilterRule là Mutable (Class), ta chỉ cần update UI
                            // (Hàm openFilterEditor đã set giá trị mới vào f rồi)
                            updateFilters(zone, filters);
                        })
                ));
            }
        }
    }


    private static Span createChip(String text, Runnable onDeleteAction, Runnable onClickAction) {
        Span chip = new Span();
        chip.setClassName("chart-drop-zone-chip");
        chip.setTitle(text);
        Span label = new Span(text);
        chip.add(label);

        if (onClickAction != null) {
            chip.getStyle().set("cursor", "pointer");
            chip.addClickListener(e -> onClickAction.run());
            chip.getStyle().set("transition", "background 0.2s");
        } else chip.getStyle().set("cursor", "default");

        Span removeBtn = new Span("×");
        removeBtn.setClassName("chart-chip-remove");
        removeBtn.getElement().addEventListener("click", e -> onDeleteAction.run()).addEventData("event.stopPropagation()");
        chip.add(removeBtn);
        return chip;
    }

    private static void renderEmptyState(Div zone, String hintText) {
        zone.removeClassName("filled");
        Span hint = new Span(hintText);
        hint.setClassName("chart-drop-zone-hint");
        zone.add(hint);
    }

    private static void setupCommonEvents(Div zone) {
        zone.setClassName("chart-drop-zone");
        zone.getElement().addEventListener("dragenter", e -> zone.addClassName("drag-over"));
        zone.getElement().addEventListener("dragleave", e -> zone.removeClassName("drag-over"));
        zone.getElement().addEventListener("drop", e -> zone.removeClassName("drag-over"));
    }

    private static void openFilterEditor(com.company.chartconfig.utils.FilterRule existingRule, String column, Consumer<com.company.chartconfig.utils.FilterRule> onSave) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Filter: " + column);

        // Nếu là Sửa (existingRule != null) thì dùng lại object cũ, nếu Tạo mới thì new object
        com.company.chartconfig.utils.FilterRule rule = (existingRule != null)
                ? existingRule
                : new com.company.chartconfig.utils.FilterRule(column, "=", "");

        TextField valueField = new TextField("Value");
        valueField.setValue(rule.getValue() == null ? "" : rule.getValue());

        ComboBox<String> operator = new ComboBox<>("Operator");
        operator.setItems("=", "!=", ">", "<", ">=", "<=", "LIKE", "IN");
        operator.setValue(rule.getOperator());
        operator.setWidth("120px");

        com.vaadin.flow.component.button.Button ok = new com.vaadin.flow.component.button.Button("Apply", ev -> {
            rule.setOperator(operator.getValue());
            rule.setValue(valueField.getValue());
            onSave.accept(rule);
            dialog.close();
        });
        ok.addThemeName("primary");

        // Layout nhanh
        com.vaadin.flow.component.orderedlayout.HorizontalLayout form = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(operator, valueField);
        form.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);

        dialog.add(form);
        dialog.getFooter().add(new com.vaadin.flow.component.button.Button("Cancel", e -> dialog.close()), ok);
        dialog.open();
    }
}