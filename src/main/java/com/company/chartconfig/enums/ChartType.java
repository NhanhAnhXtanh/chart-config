package com.company.chartconfig.enums;

import com.vaadin.flow.component.icon.VaadinIcon; // <-- IMPORT QUAN TRỌNG
import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum ChartType implements EnumClass<String> {

    // Thêm tham số Icon vào đây
    BAR("BAR", VaadinIcon.BAR_CHART),
    PIE("PIE", VaadinIcon.PIE_CHART),
    LINE("LINE", VaadinIcon.LINE_CHART),
    AREA("AREA", VaadinIcon.AREA_SELECT),
    GAUGE("GAUGE", VaadinIcon.DASHBOARD);

    private final String id;
    private final VaadinIcon icon; // <-- Thêm field icon

    // Cập nhật Constructor nhận thêm icon
    ChartType(String id, VaadinIcon icon) {
        this.id = id;
        this.icon = icon;
    }

    // <-- Thêm Getter này để hết lỗi đỏ bên View
    public VaadinIcon getIcon() {
        return icon;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static ChartType fromId(String id) {
        for (ChartType at : ChartType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}