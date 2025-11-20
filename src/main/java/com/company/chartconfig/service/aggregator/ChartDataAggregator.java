package com.company.chartconfig.service.aggregator;

import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.view.common.MetricConfig;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChartDataAggregator {

    public List<MapDataItem> aggregate(List<MapDataItem> rawData,
                                       String dimension,
                                       List<MetricConfig> metrics,
                                       ContributionMode contributionMode) {

        // 1. Group by Dimension
        Map<Object, List<MapDataItem>> grouped = rawData.stream()
                .collect(Collectors.groupingBy(item -> {
                    Object val = item.getValue(dimension);
                    return val != null ? val : "Unknown";
                }));

        List<MapDataItem> result = new ArrayList<>();

        // 2. Tính toán Metrics cơ bản
        for (Map.Entry<Object, List<MapDataItem>> entry : grouped.entrySet()) {
            MapDataItem row = new MapDataItem();
            row.add(dimension, entry.getKey());

            for (MetricConfig m : metrics) {
                double val = calculateMetric(entry.getValue(), m.getColumn(), m.getAggregate());
                row.add(m.getLabel(), val);
            }
            result.add(row);
        }

        // 3. Tính toán Contribution (Dùng Enum Switch case chuẩn)
        if (contributionMode == null) contributionMode = ContributionMode.NONE;

        switch (contributionMode) {
            case SERIES -> applySeriesContribution(result, metrics);
            case ROW -> applyRowContribution(result, metrics);
            default -> {} // None: Do nothing
        }

        return result;
    }

    // Mode SERIES: % của dòng so với tổng cột
    private void applySeriesContribution(List<MapDataItem> data, List<MetricConfig> metrics) {
        for (MetricConfig m : metrics) {
            double total = data.stream().mapToDouble(item -> (double) item.getValue(m.getLabel())).sum();
            if (total != 0) {
                for (MapDataItem item : data) {
                    double val = (double) item.getValue(m.getLabel());
                    item.add(m.getLabel(), (val / total) * 100.0);
                }
            }
        }
    }

    // Mode ROW: % của metric so với tổng dòng
    private void applyRowContribution(List<MapDataItem> data, List<MetricConfig> metrics) {
        for (MapDataItem item : data) {
            double rowTotal = 0;
            for (MetricConfig m : metrics) {
                rowTotal += (double) item.getValue(m.getLabel());
            }
            if (rowTotal != 0) {
                for (MetricConfig m : metrics) {
                    double val = (double) item.getValue(m.getLabel());
                    item.add(m.getLabel(), (val / rowTotal) * 100.0);
                }
            }
        }
    }

    private double calculateMetric(List<MapDataItem> items, String col, String aggType) {
        if (aggType == null) aggType = "COUNT";
        return switch (aggType.toUpperCase()) {
            case "SUM" -> items.stream().mapToDouble(i -> getDouble(i, col)).sum();
            case "AVG" -> items.stream().mapToDouble(i -> getDouble(i, col)).average().orElse(0);
            case "MAX" -> items.stream().mapToDouble(i -> getDouble(i, col)).max().orElse(0);
            case "MIN" -> items.stream().mapToDouble(i -> getDouble(i, col)).min().orElse(0);
            case "COUNT" -> items.size();
            default -> 0;
        };
    }

    private double getDouble(MapDataItem item, String col) {
        if (col == null) return 0.0;
        Object v = item.getValue(col);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { if (v instanceof String) return Double.parseDouble((String) v); } catch (Exception e) {}
        return 0.0;
    }
}