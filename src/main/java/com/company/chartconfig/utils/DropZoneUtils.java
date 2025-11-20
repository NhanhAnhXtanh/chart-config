package com.company.chartconfig.utils; // Nhớ đổi package cho đúng

import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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
}