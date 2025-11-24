package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
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
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.Encode;
import io.jmix.chartsflowui.kit.component.model.series.LineSeries;
import io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AreaChartBuilder implements ChartBuilder {

    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final DataComponents dataComponents;
    private final ChartDataAggregator aggregator;
    private final ChartDataFilter dataFilter;
    private final ChartDataProcessor dataProcessor;

    public AreaChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper, DataComponents dataComponents,
                            ChartDataAggregator aggregator, ChartDataFilter dataFilter, ChartDataProcessor dataProcessor) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataComponents = dataComponents;
        this.aggregator = aggregator;
        this.dataFilter = dataFilter;
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean supports(ChartType type) {
        return type == ChartType.AREA;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xAxisField = settings.getXAxisField();

        // Is Stacked
        boolean isStacked = false;

        int rowLimit = settings.getRowLimit();
        int seriesLimit = settings.getSeriesLimit();

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) {
            root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        }
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) {
            root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });
        }

        if (xAxisField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu thông tin");

        // 1. AGGREGATE DATA
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // 2. SMART METRIC LIMIT
        dataProcessor.applyMetricLimit(metrics, chartData, seriesLimit);

        // 3. SORT DATE
        if (isDateColumn(chartData, xAxisField)) {
            chartData.sort(Comparator.comparing(item -> parseDateSortable(item.getValue(xAxisField))));
        }

        // 4. PROCESS DATA (ROW LIMIT)
        dataProcessor.applyTailLimit(chartData, rowLimit);
        dataProcessor.processContributionOnly(chartData, metrics, settings);

        // SMART LAYERING (SẮP XẾP LỚP VẼ)
        if (!isStacked) {
            // Tính tổng volume trước để tránh tính đi tính lại trong comparator
            Map<String, Double> volumeMap = new HashMap<>();
            for (MetricConfig m : metrics) {
                double sum = chartData.stream().mapToDouble(i -> getVal(i, m.getLabel())).sum();
                volumeMap.put(m.getLabel(), sum);
            }

            // Sort Metric
            metrics.sort((m1, m2) -> {
                Double v1 = volumeMap.getOrDefault(m1.getLabel(), 0.0);
                Double v2 = volumeMap.getOrDefault(m2.getLabel(), 0.0);
                return Double.compare(v2, v1);
            });
        }

        // 5. BUILD UI CONTAINER
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();
        container.addProperty(xAxisField, String.class);
        for (MetricConfig m : metrics) container.addProperty(m.getLabel(), Double.class);

        List<KeyValueEntity> entities = new ArrayList<>();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = new KeyValueEntity();
            kv.setValue(xAxisField, item.getValue(xAxisField) != null ? item.getValue(xAxisField).toString() : "Unknown");
            for (MetricConfig m : metrics) {
                Object v = item.getValue(m.getLabel());
                kv.setValue(m.getLabel(), v instanceof Number ? ((Number) v).doubleValue() : 0.0);
            }
            entities.add(kv);
        }
        container.setItems(entities);

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%"); chart.setHeight("100%");

        String[] valFields = metrics.stream().map(MetricConfig::getLabel).toArray(String[]::new);
        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<EntityDataItem>()
                        .withDataProvider(new ContainerChartItems<>(container))
                        .withCategoryField(xAxisField)
                        .withValueFields(valFields)
        );
        chart.setDataSet(dataSet);

        chart.addXAxis(new XAxis().withName(xAxisField).withType(AxisType.CATEGORY));
        chart.addYAxis(new YAxis().withType(AxisType.VALUE));

        // Vẽ Series
        for (MetricConfig m : metrics) {
            LineSeries s = new LineSeries();
            s.setName(m.getLabel());
            s.setSmooth(0.5);
            s.setConnectNulls(true);

            // [FIX 3] CẤU HÌNH STYLE
            LineSeries.AreaStyle areaStyle = new LineSeries.AreaStyle();

            if (isStacked) {
                s.setStack("total");
                areaStyle.setOpacity(0.7);
            } else {
                s.setStack(null);
                areaStyle.setOpacity(0.5);
            }
            s.setAreaStyle(areaStyle);

            Encode encode = new Encode();
            encode.setX(xAxisField);
            encode.setY(m.getLabel());
            s.setEncode(encode);

            chart.addSeries(s);
        }
        chart.withLegend(new Legend());

        // Tooltip hiển thị cả giá trị để dễ so sánh
        Tooltip tooltip = new Tooltip().withTrigger(AbstractTooltip.Trigger.AXIS);
        chart.withTooltip(tooltip);

        return chart;
    }

    // Helper
    private double getVal(MapDataItem item, String key) {
        Object v = item.getValue(key);
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private boolean isDateColumn(List<MapDataItem> data, String field) {
        if (data == null || data.isEmpty()) return false;
        Object v = data.get(0).getValue(field);
        return v != null && v.toString().matches(".*\\d{4}.*[-/.].*");
    }

    private Comparable parseDateSortable(Object val) {
        if (val == null) return null;
        if (val instanceof Comparable && !(val instanceof String)) return (Comparable) val;
        String s = val.toString().trim().split(" ")[0];
        String clean = s.replace("-", "/").replace(".", "/");
        if (clean.matches("^\\d{4}/.*")) return clean;
        try { return new java.text.SimpleDateFormat("dd/MM/yyyy").parse(clean); } catch (Exception e) { return s; }
    }
}