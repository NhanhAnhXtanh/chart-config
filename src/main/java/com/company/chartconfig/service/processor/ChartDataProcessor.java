package com.company.chartconfig.service.processor;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.service.aggregator.ChartDataAggregator;
import com.company.chartconfig.service.filter.ChartDataFilter;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.common.MetricConfig;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChartDataProcessor {

    private final ChartDataFilter dataFilter;
    private final ChartDataAggregator aggregator;

    public ChartDataProcessor(ChartDataFilter dataFilter, ChartDataAggregator aggregator) {
        this.dataFilter = dataFilter;
        this.aggregator = aggregator;
    }

    /**
     * [WRAPPER] TÁI SỬ DỤNG 2 HÀM BÊN DƯỚI
     * Dùng cho: Bar, Line, Pie, Area (Cần trọn bộ quy trình)
     */
    public List<MapDataItem> processFullPipeline(List<MapDataItem> rawData,
                                                 List<MetricConfig> metrics,
                                                 List<FilterRule> allFilters,
                                                 ChartCommonSettings settings) {
        // 1. Tái sử dụng logic tính toán
        List<MapDataItem> chartData = prepareAggregatedData(rawData, metrics, allFilters, settings);

        // 2. Tái sử dụng logic hiển thị
        processVisuals(chartData, metrics, settings);

        return chartData;
    }

    /**
     * [CORE LOGIC 1] CHỈ LỌC VÀ TÍNH TOÁN
     * Dùng cho: Gauge Chart, KPI, Export Excel (Cần số liệu chuẩn, không cần cắt top/bottom)
     */
    public List<MapDataItem> prepareAggregatedData(List<MapDataItem> rawData,
                                                   List<MetricConfig> metrics,
                                                   List<FilterRule> allFilters,
                                                   ChartCommonSettings settings) {
        if (rawData == null || rawData.isEmpty()) return new ArrayList<>();

        // A. Phân loại Filter
        List<FilterRule> whereRules = new ArrayList<>();
        List<FilterRule> havingRules = new ArrayList<>();

        if (allFilters != null) {
            for (FilterRule rule : allFilters) {
                boolean isMetricFilter = metrics.stream()
                        .anyMatch(m -> m.getColumn().equals(rule.getColumn()));
                if (isMetricFilter) havingRules.add(rule);
                else whereRules.add(rule);
            }
        }

        // B. Lọc thô (WHERE)
        List<MapDataItem> processedData = dataFilter.filter(rawData, whereRules);

        // C. Tính tổng (AGGREGATE)
        List<MapDataItem> chartData = aggregator.aggregate(processedData, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // D. Lọc kết quả (HAVING)
        if (!havingRules.isEmpty() && !chartData.isEmpty()) {
            List<FilterRule> finalHavingRules = new ArrayList<>();
            for (FilterRule rule : havingRules) {
                MetricConfig targetMetric = metrics.stream()
                        .filter(m -> m.getColumn().equals(rule.getColumn()))
                        .findFirst().orElse(null);

                if (targetMetric != null) {
                    finalHavingRules.add(new FilterRule(targetMetric.getLabel(), rule.getOperator(), rule.getValue()));
                }
            }
            chartData = dataFilter.filter(chartData, finalHavingRules);
        }
        return chartData;
    }

    /**
     * [CORE LOGIC 2] CHỈ XỬ LÝ HIỂN THỊ (SORT, LIMIT, %)
     * Tách ra để tái sử dụng hoặc gọi lẻ nếu cần
     */
    public void processVisuals(List<MapDataItem> data, List<MetricConfig> metrics, ChartCommonSettings settings) {
        if (data == null || data.isEmpty() || metrics.isEmpty()) return;

        // A. Sort mặc định
        if (settings.getQuerySortMetric() == null && !metrics.isEmpty()) {
            String sortKey = metrics.get(0).getLabel();
            data.sort((o1, o2) -> Double.compare(getDouble(o2, sortKey), getDouble(o1, sortKey)));
        }

        // B. Row Limit
        int rowLimit = settings.getRowLimit();
        if (rowLimit > ChartConstants.LIMIT_NONE) {
            applyHeadLimit(data, rowLimit);
        }

        // C. Series Limit
        int seriesLimit = settings.getSeriesLimit();
        if (seriesLimit > ChartConstants.LIMIT_NONE) {
            applyMetricLimit(metrics, data, seriesLimit);
        }

        // D. Contribution
        applyContribution(data, metrics, settings.getContributionMode());
    }

    // --- Helper Utils (Giữ nguyên) ---
    public void processContributionOnly(List<MapDataItem> data, List<MetricConfig> metrics, ChartCommonSettings settings) {
        if (data == null || data.isEmpty() || metrics.isEmpty()) return;
        applyContribution(data, metrics, settings.getContributionMode());
    }

    private void applyHeadLimit(List<MapDataItem> data, int limit) {
        if (limit > 0 && data.size() > limit) data.subList(limit, data.size()).clear();
    }

    private void applyContribution(List<MapDataItem> data, List<MetricConfig> metrics, ContributionMode mode) {
        if (mode == ContributionMode.SERIES) {
            for (MetricConfig m : metrics) {
                String key = m.getLabel();
                double total = data.stream().mapToDouble(i -> getDouble(i, key)).sum();
                if (total != 0) data.forEach(i -> i.add(key, (getDouble(i, key) / total) * 100));
            }
        } else if (mode == ContributionMode.ROW) {
            for (MapDataItem item : data) {
                double rowTotal = 0;
                for (MetricConfig m : metrics) rowTotal += getDouble(item, m.getLabel());
                if (rowTotal != 0) {
                    for (MetricConfig m : metrics) item.add(m.getLabel(), (getDouble(item, m.getLabel()) / rowTotal) * 100);
                }
            }
        }
    }

    public void applyMetricLimit(List<MetricConfig> metrics, List<MapDataItem> data, int limit) {
        if (limit <= ChartConstants.LIMIT_NONE || metrics.size() <= limit || data == null || data.isEmpty()) return;
        Map<String, Double> metricTotals = new HashMap<>();
        for (MetricConfig m : metrics) {
            String key = m.getLabel();
            double total = 0;
            for (MapDataItem item : data) {
                Object v = item.getValue(key);
                if (v instanceof Number) total += ((Number) v).doubleValue();
            }
            metricTotals.put(key, total);
        }
        metrics.sort((m1, m2) -> Double.compare(metricTotals.getOrDefault(m2.getLabel(), 0.0), metricTotals.getOrDefault(m1.getLabel(), 0.0)));
        metrics.subList(limit, metrics.size()).clear();
    }

    private double getDouble(MapDataItem item, String col) {
        Object v = item.getValue(col);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { if (v != null) return Double.parseDouble(v.toString()); } catch (Exception e) {}
        return 0.0;
    }
}