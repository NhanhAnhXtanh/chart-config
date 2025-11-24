package com.company.chartconfig.utils;

import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;

public class ChartFormatterUtils {

    /**
     * 1. FORMATTER TRỤC Y: Tự động rút gọn số lớn (K, M, B)
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
                "  return value;" +
                "}";
    }

    /**
     * 2. FORMATTER TRỤC X: Giữ nguyên giá trị, xử lý ngày tháng ISO nếu cần
     */
    public static String getUniversalCategoryFormatter() {
        return "function(value) {" +
                "  if (!value) return '';" +
                "  var str = value.toString();" +
                "  if (str.match(/^\\d{4}-\\d{2}-\\d{2}/)) {" +
                "      var d = new Date(str);" +
                "      if (!isNaN(d.getTime())) return d.toLocaleDateString();" +
                "  }" +
                "  return str;" +
                "}";
    }

    /**
     * 3. FORMATTER TOOLTIP: Hiển thị số đầy đủ có dấu phẩy (không rút gọn)
     */
    public static String getTooltipNumberFormatter() {
        return "function(value) { return value ? Number(value).toLocaleString() : '0'; }";
    }

    /**
     * 4. CẤU HÌNH TRỤC X: Xoay chữ 30 độ để không đè nhau
     */
    public static void configXAxisLabel(AxisLabel label) {
        label.setInterval(0);
        label.setFormatterFunction(getUniversalCategoryFormatter());
    }
}