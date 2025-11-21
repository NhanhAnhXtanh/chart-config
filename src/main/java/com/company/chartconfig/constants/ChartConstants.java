package com.company.chartconfig.constants;

import java.util.List;

public class ChartConstants {
    public static final String JSON_FIELD_SERIES_LIMIT = "seriesLimit";

    // Danh sách gợi ý mặc định
    public static final List<Integer> DEFAULT_LIMIT_OPTIONS = List.of(5, 10, 25, 50, 100, 500, 1000, 5000, 10000);

    // Giá trị mặc định nếu user không chọn
    public static final int DEFAULT_LIMIT_VALUE = 100;

    // Giá trị đại diện cho "None" (Không giới hạn)
    public static final int LIMIT_NONE = 0;
}