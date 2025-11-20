package com.company.chartconfig.service;

import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.flowui.UiComponents;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartConfigService {
    private final ObjectMapper objectMapper;
    private final UiComponents uiComponents;

    public ChartConfigService(ObjectMapper objectMapper, UiComponents uiComponents) {
        this.objectMapper = objectMapper;
        this.uiComponents = uiComponents;
    }

    // 1. Build Items từ Raw JSON (Giữ nguyên)
    public List<MapDataItem> buildItems(Dataset dataset) {
        try {
            JsonNode array = objectMapper.readTree(dataset.getRawJson());
            List<MapDataItem> result = new ArrayList<>();
            for (JsonNode item : array) {
                MapDataItem mapItem = new MapDataItem();
                Iterator<String> names = item.fieldNames();
                while (names.hasNext()) {
                    String key = names.next();
                    JsonNode val = item.get(key);
                    if (val.isNumber()) mapItem.add(key, val.numberValue());
                    else if (val.isTextual()) mapItem.add(key, val.textValue());
                    else if (val.isBoolean()) mapItem.add(key, val.booleanValue());
                    else mapItem.add(key, val.toString());
                }
                result.add(mapItem);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Dataset rawJson invalid", e);
        }
    }

    // 2. Hàm tính toán Aggregation (QUAN TRỌNG MỚI THÊM)
    // Giúp biến data thô thành data đã Group By + SUM/AVG/COUNT
    private List<MapDataItem> aggregateData(List<MapDataItem> rawData, String dimension, List<MetricConfig> metrics) {
        // Group by Dimension
        Map<Object, List<MapDataItem>> grouped = rawData.stream()
                .collect(Collectors.groupingBy(item -> item.getValue(dimension)));

        List<MapDataItem> aggregatedResult = new ArrayList<>();

        for (Map.Entry<Object, List<MapDataItem>> entry : grouped.entrySet()) {
            MapDataItem row = new MapDataItem();
            row.add(dimension, entry.getKey()); // Key (VD: Tên sản phẩm)

            // Tính toán từng Metric
            for (MetricConfig m : metrics) {
                String col = m.getColumn();
                String aggType = m.getAggregate(); // SUM, AVG, COUNT...
                String resultKey = m.getLabel();   // Tên cột kết quả (VD: "SUM(price)")

                double val = 0;
                List<MapDataItem> groupItems = entry.getValue();

                if ("COUNT".equalsIgnoreCase(aggType)) {
                    val = groupItems.size();
                }
                else if ("SUM".equalsIgnoreCase(aggType)) {
                    val = groupItems.stream().mapToDouble(i -> getDouble(i, col)).sum();
                }
                else if ("AVG".equalsIgnoreCase(aggType)) {
                    val = groupItems.stream().mapToDouble(i -> getDouble(i, col)).average().orElse(0);
                }
                else if ("MAX".equalsIgnoreCase(aggType)) {
                    val = groupItems.stream().mapToDouble(i -> getDouble(i, col)).max().orElse(0);
                }
                else if ("MIN".equalsIgnoreCase(aggType)) {
                    val = groupItems.stream().mapToDouble(i -> getDouble(i, col)).min().orElse(0);
                }

                row.add(resultKey, val);
            }
            aggregatedResult.add(row);
        }
        return aggregatedResult;
    }

    private double getDouble(MapDataItem item, String col) {
        Object v = item.getValue(col);
        return (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
    }

    // 3. Build Preview Chart
    public Chart buildPreviewChart(Dataset dataset, ChartType type, String settingsJson) {
        try {
            JsonNode root = objectMapper.readTree(settingsJson);
            List<MapDataItem> rawItems = buildItems(dataset);

            if (type == ChartType.BAR) {
                // Lấy cấu hình X-Axis
                String xField = root.path("xAxis").asText();
                if (xField == null || xField.isBlank()) throw new IllegalArgumentException("Chưa chọn trục X");

                // Lấy danh sách Metric (Parse từ JSON sang Object)
                List<MetricConfig> metrics = new ArrayList<>();
                if (root.path("metrics").isArray()) {
                    for (JsonNode n : root.path("metrics")) {
                        metrics.add(objectMapper.treeToValue(n, MetricConfig.class));
                    }
                }
                if (metrics.isEmpty()) throw new IllegalArgumentException("Chưa chọn Metric");

                // --- BƯỚC QUAN TRỌNG: TÍNH TOÁN DỮ LIỆU ---
                List<MapDataItem> chartData = aggregateData(rawItems, xField, metrics);

                return createBarChart(xField, metrics, chartData);
            }
            else if (type == ChartType.PIE) {
                // Logic cho Pie...
                return null; // Tạm thời
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Error building chart: " + e.getMessage(), e);
        }
    }

    // 4. Create Bar Chart (Hỗ trợ nhiều Series Metric)
    private Chart createBarChart(String xField, List<MetricConfig> metrics, List<MapDataItem> data) {
        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%");
        chart.setHeight("450px");

        // DataSet
        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<MapDataItem>()
                        .withDataProvider(new ListChartItems<>(data))
                        .withCategoryField(xField)
        );
        chart.setDataSet(dataSet);

        // Axes
        chart.addXAxis(new XAxis().withName(xField).withType(AxisType.CATEGORY));
        chart.addYAxis(new YAxis().withType(AxisType.VALUE));

        // Tạo Series cho từng Metric (Vẽ nhiều cột nếu có nhiều metric)
        for (MetricConfig m : metrics) {
            BarSeries series = new BarSeries();
            series.setName(m.getLabel());      // Tên series = Label của metric (VD: "SUM(price)")
            series.set(m.getLabel()); // Mapping đúng vào cột dữ liệu đã tính toán
            chart.addSeries(series);
        }

        // Tooltip & Legend
        chart.withTooltip(new io.jmix.chartsflowui.kit.component.model.Tooltip());
        chart.withLegend(new io.jmix.chartsflowui.kit.component.model.legend.Legend());

        return chart;
    }
}