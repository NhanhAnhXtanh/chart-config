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
import java.util.List;

@Component
public class LineChartBuilder implements ChartBuilder {

    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final DataComponents dataComponents;
    private final ChartDataAggregator aggregator;
    private final ChartDataFilter dataFilter;
    private final ChartDataProcessor dataProcessor;

    public LineChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper, DataComponents dataComponents,
                            ChartDataAggregator aggregator, ChartDataFilter dataFilter, ChartDataProcessor dataProcessor) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataComponents = dataComponents;
        this.aggregator = aggregator;
        this.dataFilter = dataFilter;
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean supports(ChartType type) { return type == ChartType.LINE; }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xAxisField = settings.getXAxisField();
        int rowLimit = settings.getRowLimit();

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xAxisField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu thông tin");

        // 1. Aggregate
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // 2. Sort Time (ASC)
        chartData.sort((o1, o2) -> {
            Object v1 = o1.getValue(xAxisField);
            Object v2 = o2.getValue(xAxisField);
            if (v1 == null) return -1; if (v2 == null) return 1;
            if (v1 instanceof Comparable && v2 instanceof Comparable) return ((Comparable) v1).compareTo(v2);
            return v1.toString().compareTo(v2.toString());
        });

        // 3. Row Limit (Tail)
        dataProcessor.applyTailLimit(chartData, rowLimit);

        // 4. Contribution
        dataProcessor.processContributionOnly(chartData, metrics, settings);

        // Container
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
        DataSet dataSet = new DataSet().withSource(new DataSet.Source<EntityDataItem>().withDataProvider(new ContainerChartItems<>(container)).withCategoryField(xAxisField).withValueFields(valFields));
        chart.setDataSet(dataSet);

        // Config Chart (Basic - No Formatter)
        chart.addXAxis(new XAxis().withName(xAxisField).withType(AxisType.CATEGORY));
        chart.addYAxis(new YAxis().withType(AxisType.VALUE));

        for (MetricConfig m : metrics) {
            LineSeries s = new LineSeries();
            s.setName(m.getLabel());
            s.setSmooth(0.5);
            Encode encode = new Encode();
            encode.setX(xAxisField);
            encode.setY(m.getLabel());
            s.setEncode(encode);
            chart.addSeries(s);
        }
        chart.withLegend(new Legend());
        chart.withTooltip(new Tooltip().withTrigger(AbstractTooltip.Trigger.AXIS));

        return chart;
    }
}