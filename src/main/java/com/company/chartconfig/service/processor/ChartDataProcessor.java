package com.company.chartconfig.service.processor;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.view.common.MetricConfig;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChartDataProcessor {

    /**
     * Xử lý FULL: Row Limit -> Series Limit -> Contribution
     */
    public void process(List<MapDataItem> data, List<MetricConfig> metrics, ChartCommonSettings settings) {
        if (data == null || data.isEmpty() || metrics.isEmpty()) return;

        // 1. Row Limit (Cắt đuôi)
        int rowLimit = settings.getRowLimit();
        if (rowLimit > ChartConstants.LIMIT_NONE) {
            applyTailLimit(data, rowLimit);
        }

        int seriesLimit = settings.getSeriesLimit();
        if (seriesLimit > ChartConstants.LIMIT_NONE) {
            String sortKey = metrics.get(0).getLabel();
            data.sort((o1, o2) -> {
                double v1 = getDouble(o1, sortKey);
                double v2 = getDouble(o2, sortKey);
                return Double.compare(v2, v1);
            });
            applyHeadLimit(data, seriesLimit);
        }

        // 3. Tính %
        applyContribution(data, metrics, settings.getContributionMode());
    }

    /**
     * [FIX] Thêm hàm này để LineChartBuilder gọi được
     * Chỉ tính toán %, không cắt gọt dữ liệu
     */
    public void processContributionOnly(List<MapDataItem> data, List<MetricConfig> metrics, ChartCommonSettings settings) {
        if (data == null || data.isEmpty() || metrics.isEmpty()) return;
        applyContribution(data, metrics, settings.getContributionMode());
    }

    // --- LIMIT UTILS ---

    public void applyHeadLimit(List<MapDataItem> data, int limit) {
        if (limit > 0 && data.size() > limit) {
            data.subList(limit, data.size()).clear();
        }
    }

    public void applyTailLimit(List<MapDataItem> data, int limit) {
        if (limit > 0 && data.size() > limit) {
            int cutPoint = data.size() - limit;
            data.subList(0, cutPoint).clear();
        }
    }

    // --- INTERNAL LOGIC ---

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

    private double getDouble(MapDataItem item, String col) {
        Object v = item.getValue(col);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { if (v != null) return Double.parseDouble(v.toString()); } catch (Exception e) {}
        return 0.0;
    }

    public void applyMetricLimit(List<MetricConfig> metrics, List<MapDataItem> data, int limit) {
        // 1. Validate: Nếu không có limit hoặc limit quá lớn thì bỏ qua
        if (limit <= ChartConstants.LIMIT_NONE || metrics.size() <= limit || data == null || data.isEmpty()) {
            return;
        }

        // 2. Tính tổng giá trị (Total Volume) cho từng Metric
        // Key: Tên Metric, Value: Tổng giá trị
        Map<String, Double> metricTotals = new HashMap<>();

        for (MetricConfig m : metrics) {
            String key = m.getLabel();
            double total = 0;

            // Duyệt qua toàn bộ dữ liệu để cộng dồn
            for (MapDataItem item : data) {
                Object v = item.getValue(key);
                if (v instanceof Number) {
                    total += ((Number) v).doubleValue();
                }
            }
            metricTotals.put(key, total);
        }

        // 3. Sắp xếp danh sách Metrics: Giá trị TO lên đầu, BÉ xuống cuối
        metrics.sort((m1, m2) -> {
            double t1 = metricTotals.getOrDefault(m1.getLabel(), 0.0);
            double t2 = metricTotals.getOrDefault(m2.getLabel(), 0.0);
            // So sánh DESC (Giảm dần)
            return Double.compare(t2, t1);
        });

        // 4. Cắt danh sách (Chỉ giữ lại Top N phần tử đầu tiên)
        // Các phần tử từ vị trí 'limit' trở đi sẽ bị xóa khỏi list
        metrics.subList(limit, metrics.size()).clear();
    }
}