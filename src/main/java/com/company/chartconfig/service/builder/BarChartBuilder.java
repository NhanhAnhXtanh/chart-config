package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.service.aggregator.ChartDataAggregator;
import com.company.chartconfig.service.filter.ChartDataFilter;
import com.company.chartconfig.service.processor.ChartDataProcessor;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.ContainerChartItems;
import io.jmix.chartsflowui.data.item.EntityDataItem;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.Encode;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class BarChartBuilder implements ChartBuilder {
    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final ChartDataAggregator aggregator;
    private final DataComponents dataComponents;
    private final ChartDataFilter dataFilter;
    private final ChartDataProcessor dataProcessor;

    public BarChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper, ChartDataAggregator aggregator, DataComponents dataComponents, ChartDataFilter dataFilter, ChartDataProcessor dataProcessor) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.aggregator = aggregator;
        this.dataComponents = dataComponents;
        this.dataFilter = dataFilter;
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean supports(ChartType type) {
        return type == ChartType.BAR;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        // 1. Parse Settings
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xField = settings.getXAxisField();

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });

        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu thông tin trục X hoặc Metrics");

        // 2. PIPELINE: Filter -> Aggregate (Query Sort) -> Process (Limit/Top N)
        // Lưu ý: Query Sort đã được xử lý bên trong aggregator rồi (để lấy đúng Top N)
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        dataProcessor.process(chartData, metrics, settings);

        // 3. VISUAL SORT (X-AXIS SORT)
        // Sắp xếp lại thứ tự hiển thị sau khi đã lấy được dữ liệu
        String xAxisSortBy = settings.getXAxisSortBy();
        if (xAxisSortBy != null && !xAxisSortBy.isEmpty()) {
            boolean isMetric = metrics.stream().anyMatch(m -> m.getLabel().equals(xAxisSortBy));

            if (isMetric) {
                // Sort theo giá trị Metric (VD: Doanh thu)
                chartData.sort(Comparator.comparingDouble(item -> {
                    Object v = item.getValue(xAxisSortBy);
                    return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
                }));
            } else {
                // Sort theo Tên Trục X (VD: Tên sản phẩm)
                chartData.sort(Comparator.comparing(item -> {
                    Object v = item.getValue(xField);
                    return v != null ? v.toString() : "";
                }));
            }

            // Đảo ngược nếu không phải Ascending
            if (!settings.isXAxisSortAsc()) {
                Collections.reverse(chartData);
            }
        }

        // 4. Create Container
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();
        container.addProperty(xField, String.class);
        for (MetricConfig m : metrics) container.addProperty(m.getLabel(), Double.class);

        List<KeyValueEntity> entities = new ArrayList<>();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = new KeyValueEntity();
            kv.setValue(xField, item.getValue(xField));
            for (MetricConfig m : metrics) kv.setValue(m.getLabel(), item.getValue(m.getLabel()));
            entities.add(kv);
        }
        container.setItems(entities);

        // 5. Config Chart UI
        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%"); chart.setHeight("100%");

        String[] valFields = metrics.stream().map(MetricConfig::getLabel).toArray(String[]::new);
        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<EntityDataItem>()
                        .withDataProvider(new ContainerChartItems<>(container))
                        .withCategoryField(xField)
                        .withValueFields(valFields)
        );
        chart.setDataSet(dataSet);

        // --- X-AXIS CONFIG ---
        // --- X-AXIS CONFIG ---
        XAxis xAxis = new XAxis().withName(xField);

        // Quyết định kiểu trục
        if (settings.isForceCategorical()) {
            xAxis.withType(AxisType.CATEGORY);
        }

        AxisLabel xAxisLabel = new AxisLabel();

        // --- GIẢI PHÁP: XOAY CHỮ & HIỆN FULL ---

        // 1. Xoay chữ 30 độ (hoặc 45) để không bị chồng chéo
        xAxisLabel.setRotate(30);

        // 2. Tự động tính toán khoảng cách để không bị mất chữ
        xAxisLabel.setInterval(0); // 0 nghĩa là hiện tất cả, không ẩn nhãn nào

        // 3. Formatter: Trả về giá trị nguyên bản (KHÔNG CẮT CÚT NỮA)
        xAxisLabel.setFormatterFunction(
                "function(value) { " +
                        "   return value;" + // <--- Trả về chính xác 100% giá trị
                        "}"
        );

        xAxis.setAxisLabel(xAxisLabel);
        chart.addXAxis(xAxis);

        // --- Y-AXIS CONFIG ---
        YAxis yAxis = new YAxis().withType(AxisType.VALUE);
        AxisLabel yAxisLabel = new AxisLabel();

        if (settings.getContributionMode() != ContributionMode.NONE) {
            yAxisLabel.setFormatter("{value} %");
            if (settings.getContributionMode() == ContributionMode.ROW) yAxis.setMax("100");
        } else {
            // Format số lớn (1k, 1M, 1B)
            yAxisLabel.setFormatterFunction(
                    "function(value) { " +
                            "   if (Math.abs(value) >= 1000) {" +
                            "      if (Math.abs(value) >= 1000000000) return (value / 1000000000).toFixed(1) + 'B';" +
                            "      if (Math.abs(value) >= 1000000) return (value / 1000000).toFixed(1) + 'M';" +
                            "      if (Math.abs(value) >= 1000) return (value / 1000).toFixed(1) + 'k';" +
                            "   }" +
                            "   return value.toLocaleString();" +
                            "}"
            );
        }
        yAxis.setAxisLabel(yAxisLabel);
        chart.addYAxis(yAxis);

        // --- SERIES ---
        for (MetricConfig m : metrics) {
            BarSeries s = new BarSeries();
            s.setType(SeriesType.BAR);
            s.setName(m.getLabel());

            if (settings.getContributionMode() == ContributionMode.ROW) {
                s.setStack("total");
                s.setName(m.getLabel() + " (%)");
            } else if (settings.getContributionMode() == ContributionMode.SERIES) {
                s.setName(m.getLabel() + " (%)");
            }

            Encode encode = new Encode();
            encode.setX(xField);
            encode.setY(m.getLabel());
            s.setEncode(encode);
            chart.addSeries(s);
        }

        chart.withLegend(new Legend());

        // --- TOOLTIP ---
        io.jmix.chartsflowui.kit.component.model.Tooltip tooltip = new io.jmix.chartsflowui.kit.component.model.Tooltip();
        tooltip.setTrigger(io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip.Trigger.AXIS);

        if (settings.getContributionMode() != ContributionMode.NONE) {
            tooltip.setValueFormatterFunction("function(value) { return value ? Number(value).toFixed(2) + ' %' : '0 %'; }");
        } else {
            tooltip.setValueFormatterFunction("function(value) { return value ? Number(value).toLocaleString() : '0'; }");
        }
        chart.withTooltip(tooltip);

        return chart;
    }
}