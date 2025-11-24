package com.company.chartconfig.utils;

import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;

public class ChartFormatterUtils {

    /**
     * 1. FORMATTER ĐA NĂNG CHO TRỤC SỐ (Y-AXIS)
     * - Logic: Kiểm tra xem có phải là số không.
     * - Nếu là Số: Format K/M/B, thêm dấu phẩy.
     * - Nếu là Chữ: Giữ nguyên (ví dụ: "High", "Low").
     */
    public static String getUniversalValueFormatter() {
        return "function(value) {" +
                "  if (value === null || value === undefined) return '';" +

                "  // 1. Cố gắng ép kiểu về số" +
                "  var num = Number(value);" +

                "  // 2. Nếu là số hợp lệ" +
                "  if (!isNaN(num)) {" +
                "      // Số cực lớn (Tỷ/Triệu/Nghìn)" +
                "      if (Math.abs(num) >= 1.0e+9) return (num / 1.0e+9).toFixed(1) + 'B';" +
                "      if (Math.abs(num) >= 1.0e+6) return (num / 1.0e+6).toFixed(1) + 'M';" +
                "      if (Math.abs(num) >= 1000) return (num / 1000).toFixed(1) + 'k';" +
                "      // Số nhỏ hoặc thập phân" +
                "      return num.toLocaleString();" +
                "  }" +

                "  // 3. Nếu không phải số, trả về string gốc" +
                "  return value;" +
                "}";
    }

    /**
     * 2. FORMATTER ĐA NĂNG CHO TRỤC DANH MỤC (X-AXIS)
     * - Logic: Xử lý Ngày tháng, Cắt chuỗi nếu quá dài (tránh vỡ giao diện).
     */
    public static String getUniversalCategoryFormatter() {
        return "function(value) {" +
                "  try {" +
                "      if (value === null || value === undefined) return value;" +
                "      var str = String(value);" +
                // Nhận diện date ISO và date số đầu chuỗi
                "      if (str.length >= 10 && !isNaN(Date.parse(str)) && /^\\d{4}/.test(str)) {" +
                "          var d = new Date(str);" +
                "          return ('0' + d.getDate()).slice(-2) + '/' + ('0' + (d.getMonth()+1)).slice(-2) + '/' + d.getFullYear();" +
                "      }" +
                // Cắt chuỗi dài hơn 20 ký tự
                "      if (str.length > 20) {" +
                "          return str.substring(0, 18) + '...';" +
                "      }" +
                "      return str;" +
                "  } catch (e) {" +
                "      return String(value);" +
                "  }" +
                "}";
    }

    /**
     * 3. FORMATTER CHO TOOLTIP
     * - Tooltip thì cần hiện ĐẦY ĐỦ (không được cắt bớt, không được rút gọn K/M/B quá mức).
     */
    public static String getTooltipNumberFormatter() {
        return "function(value) {" +
                "   if (value === null || value === undefined) return '';" +
                "   var num = Number(value);" +
                "   if (!isNaN(num)) {" +
                "       return num.toLocaleString();" + // Hiện full số: 1,234,567.89
                "   }" +
                "   return value;" + // Hiện full chữ
                "}";
    }

    /**
     * 4. CẤU HÌNH TRỤC X
     * - Bắt buộc xoay chữ (Rotate) để tránh đè nhau nếu có nhiều cột.
     * - Bắt buộc hiện hết (Interval 0) để không bị ẩn mất dữ liệu.
     */
    public static void configXAxisLabel(AxisLabel label) {
        label.setInterval(0); // Hiện tất cả nhãn
        label.setFormatterFunction(getUniversalCategoryFormatter());
    }
}