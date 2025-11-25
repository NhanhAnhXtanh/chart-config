package com.company.chartconfig.service.filter;

import com.company.chartconfig.utils.FilterRule;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChartDataFilter {

    public List<MapDataItem> filter(List<MapDataItem> rawData, List<FilterRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return rawData;
        }
        return rawData.stream()
                .filter(item -> matchesAllRules(item, rules))
                .collect(Collectors.toList());
    }

    private boolean matchesAllRules(MapDataItem item, List<FilterRule> rules) {
        for (FilterRule rule : rules) {
            if (!checkRule(item, rule)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkRule(MapDataItem item, FilterRule rule) {
        String col = rule.getColumn();
        Object itemValue = item.getValue(col);
        String filterValue = rule.getValue();
        String op = rule.getOperator();

        if (itemValue == null) return false;

        // 1. Ưu tiên xử lý các toán tử đặc thù của String trước
        if ("LIKE".equalsIgnoreCase(op) || "IN".equalsIgnoreCase(op)) {
            return checkString(itemValue.toString(), op, filterValue);
        }

        // 2. Cố gắng so sánh SỐ (Kể cả khi dữ liệu là String "123")
        Double num1 = tryParseDouble(itemValue);
        Double num2 = tryParseDouble(filterValue);

        if (num1 != null && num2 != null) {
            return checkNumber(num1, op, num2);
        }

        // 3. Fallback: Nếu không phải số, so sánh CHUỖI (theo Alpha-beta)
        return checkString(itemValue.toString(), op, filterValue);
    }

    /**
     * So sánh 2 số thực
     */
    private boolean checkNumber(Double v1, String op, Double v2) {
        return switch (op) {
            case "=" -> Double.compare(v1, v2) == 0;
            case "!=" -> Double.compare(v1, v2) != 0;
            case ">" -> v1 > v2;
            case "<" -> v1 < v2;
            case ">=" -> v1 >= v2;
            case "<=" -> v1 <= v2;
            default -> false;
        };
    }

    /**
     * So sánh 2 chuỗi (Hỗ trợ cả >, < cho chữ cái)
     */
    private boolean checkString(String s1, String op, String s2) {
        String val = s1.toLowerCase().trim();
        String filter = s2.toLowerCase().trim();

        int compareResult = val.compareTo(filter); // <0: nhỏ hơn, 0: bằng, >0: lớn hơn

        return switch (op) {
            case "=" -> val.equals(filter);
            case "!=" -> !val.equals(filter);
            case "LIKE" -> val.contains(filter);
            case "IN" -> {
                // Tách chuỗi bằng dấu phẩy
                List<String> options = Arrays.stream(s2.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toList();
                yield options.contains(val);
            }
            // Hỗ trợ so sánh chữ cái (Ví dụ: Ngày tháng dạng text, hoặc Tên)
            case ">" -> compareResult > 0;
            case "<" -> compareResult < 0;
            case ">=" -> compareResult >= 0;
            case "<=" -> compareResult <= 0;
            default -> false;
        };
    }

    /**
     * Helper: Cố gắng ép kiểu về Double, nếu lỗi trả về null
     */
    private Double tryParseDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString().trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }
}