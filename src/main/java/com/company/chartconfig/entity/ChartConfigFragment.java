package com.company.chartconfig.view.config.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public interface ChartConfigFragment {

    /**
     * Cung cấp danh sách các trường dữ liệu từ Dataset để Fragment dùng (nếu cần validate)
     */
    void setAvailableFields(List<String> fields);

    /**
     * Lấy cấu hình hiện tại dưới dạng JSON để lưu vào database
     */
    ObjectNode getConfigurationJson();

    /**
     * Đổ dữ liệu từ JSON vào giao diện Fragment (khi Edit hoặc Load)
     */
    void setConfigurationJson(JsonNode json);

    /**
     * Kiểm tra dữ liệu đã nhập đủ chưa
     */
    boolean isValid();

    /**
     * Phương thức của Component (Fragment kế thừa nó) để ẩn/hiện
     */
    void setVisible(boolean visible);

    // --- CÁC HÀM HỖ TRỢ CHUYỂN ĐỔI BIỂU ĐỒ (MIGRATION) ---
    // Giúp giữ lại dữ liệu khi user chuyển từ Bar -> Pie -> Line

    /**
     * Lấy trường Dimension chính (Ví dụ: X-Axis của Bar, Label của Pie)
     */
    String getMainDimension();

    /**
     * Set trường Dimension chính
     */
    void setMainDimension(String field);

    /**
     * Lấy trường Metric chính (Ví dụ: Y-Axis của Bar, Value của Pie)
     */
    String getMainMetric();

    /**
     * Set trường Metric chính
     */
    void setMainMetric(String field);
}