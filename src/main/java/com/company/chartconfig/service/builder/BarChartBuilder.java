package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.service.aggregator.ChartDataAggregator;
import com.company.chartconfig.service.filter.ChartDataFilter;
import com.company.chartconfig.service.processor.ChartDataProcessor;
import com.company.chartconfig.utils.ChartFormatterUtils;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.ContainerChartItems;
import io.jmix.chartsflowui.data.item.EntityDataItem;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.*;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.*;
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
    public boolean supports(ChartType type) { return type == ChartType.BAR; }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xField = settings.getXAxisField();
        int seriesLimit = settings.getSeriesLimit(); // [BAR] Dùng Series Limit (Top N)

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu thông tin");

        // 1. Aggregate
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // 2. Limit (Top N)
        dataProcessor.applyHeadLimit(chartData, seriesLimit);

        // 3. Contribution
        dataProcessor.processContributionOnly(chartData, metrics, settings);

        // 4. Visual Sort
        String xAxisSortBy = settings.getXAxisSortBy();
        if (xAxisSortBy != null && !xAxisSortBy.isEmpty()) {
            boolean isMetric = metrics.stream().anyMatch(m -> m.getLabel().equals(xAxisSortBy));
            if (isMetric) {
                chartData.sort(Comparator.comparingDouble(item -> {
                    Object v = item.getValue(xAxisSortBy);
                    return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
                }));
            } else {
                chartData.sort(Comparator.comparing(item -> {
                    Object v = item.getValue(xField);
                    return v != null ? v.toString() : "";
                }));
            }
            if (!settings.isXAxisSortAsc()) Collections.reverse(chartData);
        }

        // 5. Container & Chart
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

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%"); chart.setHeight("100%");
        String[] valFields = metrics.stream().map(MetricConfig::getLabel).toArray(String[]::new);
        DataSet dataSet = new DataSet().withSource(new DataSet.Source<EntityDataItem>().withDataProvider(new ContainerChartItems<>(container)).withCategoryField(xField).withValueFields(valFields));
        chart.setDataSet(dataSet);

        // Axis Config (Utils)
        XAxis xAxis = new XAxis().withName(xField);
        if (settings.isForceCategorical()) xAxis.withType(AxisType.CATEGORY); else xAxis.withType(AxisType.CATEGORY);
        AxisLabel xAxisLabel = new AxisLabel();
        ChartFormatterUtils.configXAxisLabel(xAxisLabel);
        xAxis.setAxisLabel(xAxisLabel);
        chart.addXAxis(xAxis);

        YAxis yAxis = new YAxis().withType(AxisType.VALUE);
        AxisLabel yAxisLabel = new AxisLabel();
        if (settings.getContributionMode() != ContributionMode.NONE) yAxisLabel.setFormatter("{value} %");
        else yAxisLabel.setFormatterFunction(ChartFormatterUtils.getUniversalValueFormatter());
        yAxis.setAxisLabel(yAxisLabel);
        chart.addYAxis(yAxis);

        for (MetricConfig m : metrics) {
            BarSeries s = new BarSeries();
            s.setName(m.getLabel());
            if (settings.getContributionMode() == ContributionMode.ROW) s.setStack("total");
            Encode encode = new Encode();
            encode.setX(xField);
            encode.setY(m.getLabel());
            s.setEncode(encode);
            chart.addSeries(s);
        }
        chart.withLegend(new Legend());
        io.jmix.chartsflowui.kit.component.model.Tooltip tooltip = new io.jmix.chartsflowui.kit.component.model.Tooltip();
        tooltip.setTrigger(io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip.Trigger.AXIS);
        if (settings.getContributionMode() != ContributionMode.NONE) tooltip.setValueFormatterFunction("function(value) { return value ? Number(value).toFixed(2) + ' %' : '0 %'; }");
        else tooltip.setValueFormatterFunction(ChartFormatterUtils.getTooltipNumberFormatter());
        chart.withTooltip(tooltip);

        return chart;
    }
}