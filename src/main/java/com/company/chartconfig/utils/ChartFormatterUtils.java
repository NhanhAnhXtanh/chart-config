package com.company.chartconfig.utils;

import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;

public class ChartFormatterUtils {

    /**
     * 1. FORMATTER ĐA NĂNG CHO TRỤC GIÁ TRỊ (Y-AXIS)
     * - Tự động phát hiện số để rút gọn (K, M, B).
     * - Tự động thêm dấu phẩy ngăn cách hàng nghìn.
     */
    public static String getUniversalValueFormatter() {
        return "function(value) {" +
                "  if (value === null || value === undefined) return '';" +
                "  if (typeof value === 'number') {" +
                "      if (Math.abs(value) >= 1000000000) return (value / 1000000000).toFixed(1) + 'B';" +
                "      if (Math.abs(value) >= 1000000) return (value / 1000000).toFixed(1) + 'M';" +
                "      if (Math.abs(value) >= 1000) return (value / 1000).toFixed(1) + 'k';" +
                "      return Number(value).toLocaleString();" +
                "  }" +
                "  if (!isNaN(parseFloat(value)) && isFinite(value)) {" +
                "      return Number(value).toLocaleString();" +
                "  }" +
                "  return value;" +
                "}";
    }

    /**
     * 2. FORMATTER CHO TRỤC DANH MỤC (X-AXIS)
     * - Tự động phát hiện chuỗi ngày tháng (ISO) để format đẹp.
     */
    public static String getUniversalCategoryFormatter() {
        return "function(value) {" +
                "  if (!value) return '';" +
                "  var str = value.toString();" +
                "  if (str.match(/^\\d{4}-\\d{2}-\\d{2}/)) {" +
                "      var d = new Date(str);" +
                "      if (!isNaN(d.getTime())) {" +
                "          return d.toLocaleDateString();" +
                "      }" +
                "  }" +
                "  return str;" +
                "}";
    }

    /**
     * 3. FORMATTER CHO TOOLTIP (SỐ)
     * - Hiển thị số đầy đủ (không rút gọn) có dấu phẩy.
     */
    public static String getTooltipNumberFormatter() {
        return "function(value) { return value ? Number(value).toLocaleString() : '0'; }";
    }

    /**
     * 4. CẤU HÌNH CHUẨN CHO TRỤC X
     * - Xoay chữ 30 độ, hiện tất cả nhãn.
     */
    public static void configXAxisLabel(AxisLabel label) {
        label.setInterval(0);
        label.setFormatterFunction(getUniversalCategoryFormatter());
    }
}