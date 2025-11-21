package com.company.chartconfig.view.config.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

public interface ChartConfigFragment {
    /**
     * Cập nhật danh sách các trường có sẵn từ Dataset
     */
    void setAvailableFields(List<String> fields);

    /**
     * (MỚI) Cập nhật Metadata kiểu dữ liệu (Name -> Type)
     * Để Fragment biết cột nào là Date để hiện cấu hình TimeGrain
     */
    void setColumnTypes(Map<String, String> types);

    /**
     * Lấy JSON cấu hình hiện tại
     */
    ObjectNode getConfigurationJson();

    /**
     * Nạp lại cấu hình từ JSON đã lưu
     */
    void setConfigurationJson(JsonNode json);

    boolean isValid();

    String getMainDimension();
    void setMainDimension(String field);

    String getMainMetric();
    void setMainMetric(String field);
}