package com.company.chartconfig.service.filter;

import com.company.chartconfig.utils.FilterRule;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
                return false; // Chỉ cần 1 rule sai là loại bỏ (Logic AND)
            }
        }
        return true;
    }

    private boolean checkRule(MapDataItem item, FilterRule rule) {
        String col = rule.getColumn();
        Object itemValue = item.getValue(col); // Giá trị trong DB (Number, String, Boolean...)
        String filterValue = rule.getValue();// Giá trị user nhập (Luôn là String)
        String op = rule.getOperator();

        if (itemValue == null) return false; // Tạm thời loại bỏ null

        // Xử lý so sánh số (Number)
        if (itemValue instanceof Number) {
            return checkNumber((Number) itemValue, op, filterValue);
        }

        // Xử lý so sánh chuỗi (String)
        return checkString(itemValue.toString(), op, filterValue);
    }

    private boolean checkNumber(Number val, String op, String filterStr) {
        try {
            double v1 = val.doubleValue();
            double v2 = Double.parseDouble(filterStr);

            return switch (op) {
                case "=" -> v1 == v2;
                case "!=" -> v1 != v2;
                case ">" -> v1 > v2;
                case "<" -> v1 < v2;
                case ">=" -> v1 >= v2;
                case "<=" -> v1 <= v2;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false; // User nhập không phải số
        }
    }

    private boolean checkString(String val, String op, String filterStr) {
        // So sánh không phân biệt hoa thường cho tiện
        String s1 = val.toLowerCase();
        String s2 = filterStr.toLowerCase();

        return switch (op) {
            case "=" -> s1.equals(s2);
            case "!=" -> !s1.equals(s2);
            case "LIKE" -> s1.contains(s2);
            case "IN" -> {
                // User nhập: "A, B, C" -> Split ra check
                List<String> options = Arrays.stream(s2.split(","))
                        .map(String::trim)
                        .toList();
                yield options.contains(s1);
            }
            default -> false;
        };
    }
}