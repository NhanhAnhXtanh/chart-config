package com.company.chartconfig.view.config.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public interface ChartConfigFragment {
    // Nạp danh sách cột từ Dataset để dùng cho Dialog/Validate
    void setAvailableFields(List<String> fields);

    // Xuất JSON config để lưu
    ObjectNode getConfigurationJson();

    // Nạp JSON config để hiển thị (Edit mode)
    void setConfigurationJson(JsonNode json);

    // Kiểm tra hợp lệ
    boolean isValid();

    // Kế thừa từ Component
    void setVisible(boolean visible);

    // Migration Support
    String getMainDimension();
    void setMainDimension(String field);
    String getMainMetric();
    void setMainMetric(String field);
}