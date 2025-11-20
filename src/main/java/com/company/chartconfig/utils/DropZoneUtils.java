package com.company.chartconfig.utils; // Nhớ đổi package cho đúng

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

    /**
     * Cấu hình một Div thành DropZone
     * @param zone Div cần cấu hình
     * @param onValueChange Callback để cập nhật giá trị vào biến (ví dụ: val -> this.xField = val)
     */
    public static void setup(Div zone, Consumer<String> onValueChange) {
        // 1. Đảm bảo CSS class
        zone.setClassName("chart-drop-zone");

        // 2. Cấu hình DropTarget
        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);

        // 3. Xử lý Drop
        dropTarget.addDropListener(e -> e.getDragSourceComponent().ifPresent(src -> {
            String field = src.getElement().getAttribute("data-field-name");
            if (field != null) {
                updateVisuals(zone, field);
                onValueChange.accept(field); // Gọi callback để lưu giá trị
            }
        }));

        // 4. Hiệu ứng Drag Enter/Leave (dùng CSS class)
        zone.getElement().addEventListener("dragenter", e -> zone.addClassName("drag-over"));
        zone.getElement().addEventListener("dragleave", e -> zone.removeClassName("drag-over"));

        // 5. Click để xóa (Clear)
        zone.addClickListener(e -> {
            updateVisuals(zone, null);
            onValueChange.accept(null); // Reset giá trị về null
            zone.removeClassName("drag-over");
        });

        // 6. Khởi tạo trạng thái rỗng
        updateVisuals(zone, null);
    }

    /**
     * Cập nhật hiển thị (Label hoặc Hint)
     */
    public static void updateVisuals(Div zone, String value) {
        zone.removeAll();

        if (value == null || value.isBlank()) {
            // Chưa có dữ liệu
            zone.removeClassName("filled");
            Span hint = new Span("Kéo thả vào đây");
            hint.setClassName("chart-drop-zone-hint");
            zone.add(hint);
        } else {
            // Đã có dữ liệu
            zone.addClassName("filled");
            Span chip = new Span(value);
            chip.setClassName("chart-drop-zone-chip");
            zone.add(chip);
        }
    }

    public static void setupMulti(Div zone, List<String> list) {
        zone.setClassName("chart-drop-zone");

        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true);  // ⭐ IMPORTANT ⭐

        dropTarget.addDropListener(e ->
                e.getDragSourceComponent().ifPresent(src -> {
                    String field = src.getElement().getAttribute("data-field-name");
                    if (field != null && !list.contains(field)) {
                        list.add(field);
                        updateMulti(zone, list);
                    }
                })
        );

        zone.addClickListener(e -> {
            list.clear();
            updateMulti(zone, list);
        });

        updateMulti(zone, list);
    }


    public static void updateMulti(Div zone, List<String> list) {
        zone.removeAll();

        if (list.isEmpty()) {
            Span span = new Span("Kéo thả vào đây");
            span.setClassName("chart-drop-zone-hint");
            zone.add(span);
            return;
        }

        for (String v : list) {
            Span chip = new Span(v);
            chip.setClassName("chart-drop-zone-chip");
            zone.add(chip);
        }
    }

    public static void setupFilter(Div zone, List<FilterRule> filters) {

        DropTarget<Div> dropTarget = DropTarget.configure(zone);
        dropTarget.setActive(true); // ⭐ REQUIRED ⭐

        dropTarget.addDropListener(e ->
                e.getDragSourceComponent().ifPresent(src -> {
                    String column = src.getElement().getAttribute("data-field-name");
                    if (column != null) openFilterEditor(zone, filters, column);
                })
        );

        updateFilters(zone, filters);
    }


    private static void openFilterEditor(Div zone, List<FilterRule> list, String column) {
        // popup UI
        Dialog dialog = new Dialog();

        TextField valueField = new TextField("Value");
        ComboBox<String> operator = new ComboBox<>("Operator");
        operator.setItems("=", "!=", ">", "<", ">=", "<=", "LIKE", "IN");

        Button ok = new Button("Apply", ev -> {
            list.add(new FilterRule(column, operator.getValue(), valueField.getValue()));
            dialog.close();
            updateFilters(zone, list);
        });

        dialog.add(new H3("Add Filter: " + column), operator, valueField, ok);
        dialog.open();
    }

    private static void updateFilters(Div zone, List<FilterRule> filters) {
        zone.removeAll();

        if (filters.isEmpty()) {
            Span s = new Span("Kéo thả để thêm filter");
            s.setClassName("chart-drop-zone-hint");
            zone.add(s);
            return;
        }

        for (FilterRule f : filters) {
            Span chip = new Span(f.getColumn() + " " + f.getOperator() + " " + f.getValue());
            chip.setClassName("chart-drop-zone-chip");
            zone.add(chip);
        }
    }


}