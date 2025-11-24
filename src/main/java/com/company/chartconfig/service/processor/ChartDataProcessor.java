package com.company.chartconfig.service.processor;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.view.common.MetricConfig;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.List;

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

        // 2. Series Limit (Cắt đầu - Top N)
        int seriesLimit = settings.getSeriesLimit();
        if (seriesLimit > ChartConstants.LIMIT_NONE) {
            // Sort DESC để lấy Top N
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

    // Xử lý cắt Metric (Series Limit)
    public void applyMetricLimit(List<MetricConfig> metrics, int limit) {
        if (limit > 0 && metrics.size() > limit) {
            metrics.subList(limit, metrics.size()).clear();
        }
    }
}