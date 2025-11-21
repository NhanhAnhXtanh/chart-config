package com.company.chartconfig.service.processor;

import com.company.chartconfig.constants.ChartConstants;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.view.common.MetricConfig;
import io.jmix.chartsflowui.data.item.MapDataItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ChartDataProcessor {

    /**
     * Xử lý hậu kỳ dữ liệu: Tính % -> Sắp xếp -> Cắt Limit
     */
    public void process(List<MapDataItem> data, List<MetricConfig> metrics, ChartCommonSettings settings) {
        if (data == null || data.isEmpty() || metrics.isEmpty()) return;

        // 1. Xử lý Contribution (Phần trăm)
        applyContribution(data, metrics, settings.getContributionMode());

        int limit = settings.getSeriesLimit();

        // Nếu limit > 0 và nhỏ hơn kích thước data thì mới cắt
        if (limit > ChartConstants.LIMIT_NONE && data.size() > limit) {
            applyLimit(data, metrics, limit);
        }
    }

    // --- LOGIC LIMIT ---
    private void applyLimit(List<MapDataItem> data, List<MetricConfig> metrics, int limit) {
        // 1. Sort DESC theo Metric chính
        if (!metrics.isEmpty()) {
            String sortKey = metrics.get(0).getLabel();
            data.sort((o1, o2) -> {
                double v1 = getDouble(o1, sortKey);
                double v2 = getDouble(o2, sortKey);
                return Double.compare(v2, v1);
            });
        }

        // 2. Cut list (Hiệu năng cao)
        data.subList(limit, data.size()).clear();
    }


    // --- LOGIC CONTRIBUTION ---
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
                    for (MetricConfig m : metrics) {
                        item.add(m.getLabel(), (getDouble(item, m.getLabel()) / rowTotal) * 100);
                    }
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
}