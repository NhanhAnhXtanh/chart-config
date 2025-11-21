package com.company.chartconfig.utils;

import com.company.chartconfig.constants.ChartConstants;
import com.vaadin.flow.component.combobox.ComboBox; // Hoặc io.jmix.flowui.component.combobox.JmixComboBox

public class ChartUiUtils {

    /**
     * Cấu hình chuẩn cho ô nhập Series Limit
     * QUAN TRỌNG: Tham số phải là ComboBox<Integer> để khớp với định nghĩa ở Fragment
     */
    public static void setupSeriesLimitField(ComboBox<Integer> field) {
        // 1. Set danh sách item
        field.setItems(ChartConstants.DEFAULT_LIMIT_OPTIONS);

        // 2. Xử lý hiển thị (Dùng equals để an toàn với Integer object)
        field.setItemLabelGenerator(item -> {
            if (item != null && item.equals(ChartConstants.LIMIT_NONE)) {
                return "None (All)";
            }
            return String.valueOf(item);
        });

        // 3. Cho phép nhập số tay
        field.setAllowCustomValue(true);
        field.addCustomValueSetListener(e -> {
            try {
                int val = Integer.parseInt(e.getDetail());
                if (val < 0) val = 0;
                field.setValue(val);
            } catch (NumberFormatException ex) {
                // Reset về giá trị cũ nếu nhập sai
                field.setValue(field.getValue());
            }
        });

        // 4. Mặc định
        if (field.getValue() == null) {
            field.setValue(ChartConstants.DEFAULT_LIMIT_VALUE);
        }
    }
}