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
    // 1. SINGLE VALUE (X-AXIS) - Click vào chip không làm gì (chỉ có nút X)
    // ============================================================
    public static void setup(Div zone, Consumer<String> onValueChange) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) {
                updateVisuals(zone, field, onValueChange);
                onValueChange.accept(field);
            }
        }));

        // Init
        updateVisuals(zone, null, onValueChange);
    }

    public static void updateVisuals(Div zone, String value, Consumer<String> onUpdate) {
        zone.removeAll();
        if (value == null || value.isBlank()) {
            renderEmptyState(zone, "Kéo thả vào đây");
        } else {
            zone.addClassName("filled");
            // createChip tham số cuối là null vì X-Axis không cần mở dialog
            Span chip = createChip(value, () -> {
                updateVisuals(zone, null, onUpdate);
                onUpdate.accept(null);
            }, null);
            zone.add(chip);
        }
    }

    // ============================================================
    // 2. METRIC SETUP - Click vào chip -> Mở Dialog
    // ============================================================
    public static void setupMetricZone(Div zone, List<MetricConfig> metrics, List<String> cols, Runnable onUpdate) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) {
                metrics.add(new MetricConfig(field));
                updateMetricVisuals(zone, metrics, cols, onUpdate);
                onUpdate.run();
            }
        }));
    }

    public static void updateMetricVisuals(Div zone, List<MetricConfig> metrics, List<String> cols, Runnable onUpdate) {
        zone.removeAll();

        if (metrics.isEmpty()) {
            renderEmptyState(zone, "Kéo thả Metric");
            return;
        }

        zone.addClassName("filled");

        for (MetricConfig m : metrics) {
            // --- QUAN TRỌNG: Định nghĩa 2 hành động ---

            // 1. Hành động Xóa
            Runnable onDelete = () -> {
                metrics.remove(m);
                updateMetricVisuals(zone, metrics, cols, onUpdate);
                onUpdate.run();
            };

            // 2. Hành động Click (Mở Dialog)
            Runnable onClick = () -> {
                new MetricConfigDialog(m, cols, (updated) -> {
                    updateMetricVisuals(zone, metrics, cols, onUpdate);
                    onUpdate.run();
                }).open();
            };

            // Tạo chip với cả 2 hành động
            Span chip = createChip(m.getLabel(), onDelete, onClick);
            zone.add(chip);
        }
    }

    // ============================================================
    // 3. MULTI STRING (DIMENSIONS) - Click chip không làm gì
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
        if (list.isEmpty()) {
            renderEmptyState(zone, "Kéo thả (Nhiều)");
        } else {
            zone.addClassName("filled");
            for (String v : new ArrayList<>(list)) {
                // createChip tham số cuối là null
                Span chip = createChip(v, () -> {
                    list.remove(v);
                    updateMulti(zone, list);
                }, null);
                zone.add(chip);
            }
        }
    }

    // ============================================================
    // 4. FILTER SETUP
    // ============================================================
    public static void setupFilter(Div zone, List<com.company.chartconfig.utils.FilterRule> filters) {
        setupCommonEvents(zone);
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String col = src.getElement().getAttribute("data-field-name");
            if (col != null) openFilterEditor(zone, filters, col);
        }));
        updateFilters(zone, filters);
    }

    public static void updateFilters(Div zone, List<com.company.chartconfig.utils.FilterRule> filters) {
        zone.removeAll();
        if (filters.isEmpty()) {
            renderEmptyState(zone, "Kéo thả Filter");
        } else {
            zone.addClassName("filled");
            for (com.company.chartconfig.utils.FilterRule f : new ArrayList<>(filters)) {
                String label = f.getColumn() + " " + f.getOperator() + " " + f.getValue();
                Span chip = createChip(label, () -> {
                    filters.remove(f);
                    updateFilters(zone, filters);
                }, null); // Filter tạm thời ko click để sửa (xóa rồi tạo lại)
                zone.add(chip);
            }
        }
    }

    // ============================================================
    // HELPER METHODS (LOGIC CHIP NẰM Ở ĐÂY)
    // ============================================================

    /**
     * Tạo Chip hiển thị
     * @param text Nội dung
     * @param onDeleteAction Hàm chạy khi bấm nút X
     * @param onClickAction Hàm chạy khi bấm vào thân chip (Mở Dialog). Nếu null thì ko click được.
     */
    private static Span createChip(String text, Runnable onDeleteAction, Runnable onClickAction) {
        Span chip = new Span();
        chip.setClassName("chart-drop-zone-chip");
        chip.setTitle(text);

        // 1. Text Label
        Span label = new Span(text);
        chip.add(label);

        // 2. Xử lý Click vào thân chip (Nếu có action)
        if (onClickAction != null) {
            chip.getStyle().set("cursor", "pointer");
            chip.addClickListener(e -> onClickAction.run());

            // Style hover để người dùng biết là click được
            chip.getStyle().set("transition", "background 0.2s");
        } else {
            chip.getStyle().set("cursor", "default");
        }

        // 3. Nút Xóa (X)
        Span removeBtn = new Span("×");
        removeBtn.setClassName("chart-chip-remove");
        removeBtn.setTitle("Xóa");

        // Chặn nổi bọt sự kiện bằng JS để click X không kích hoạt click thân chip
        removeBtn.getElement().addEventListener("click", e -> {
            onDeleteAction.run();
        }).addEventData("event.stopPropagation()");

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

    private static void openFilterEditor(Div zone, List<com.company.chartconfig.utils.FilterRule> list, String column) {
        Dialog dialog = new Dialog();
        TextField valueField = new TextField("Value");
      ComboBox<String> operator = new ComboBox<>("Operator");
        operator.setItems("=", "!=", ">", "<", "LIKE", "IN");
        operator.setValue("=");

        com.vaadin.flow.component.button.Button ok = new com.vaadin.flow.component.button.Button("Apply", ev -> {
            list.add(new com.company.chartconfig.utils.FilterRule(column, operator.getValue(), valueField.getValue()));
            dialog.close();
            updateFilters(zone, list);
        });
        ok.addThemeName("primary");

        dialog.add(new H3("Filter: "+column), operator, valueField, ok);
        dialog.open();
    }
}