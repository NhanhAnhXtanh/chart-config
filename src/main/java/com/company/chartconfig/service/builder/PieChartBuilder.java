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
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.Label;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.shared.Orientation;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PieChartBuilder implements ChartBuilder {

    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final DataComponents dataComponents;
    private final ChartDataAggregator aggregator;
    private final ChartDataFilter dataFilter;
    private final ChartDataProcessor dataProcessor;

    public PieChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper, DataComponents dataComponents,
                           ChartDataAggregator aggregator, ChartDataFilter dataFilter, ChartDataProcessor dataProcessor) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataComponents = dataComponents;
        this.aggregator = aggregator;
        this.dataFilter = dataFilter;
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean supports(ChartType type) { return type == ChartType.PIE; }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String dimension = settings.getDimensionField();
        int limit = settings.getSeriesLimit(); // [PIE] Dùng Series Limit (Top N)

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (dimension == null || metrics.isEmpty()) throw new IllegalStateException("Pie Chart thiếu dữ liệu");
        MetricConfig metric = metrics.get(0);

        // 1. Aggregate
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // Sort Value DESC để lấy Top N
        String key = metric.getLabel();
        chartData.sort((o1, o2) -> Double.compare(getDouble(o2, key), getDouble(o1, key)));

        // 2. Limit
        dataProcessor.applyHeadLimit(chartData, limit);

        // 3. Contribution
        dataProcessor.processContributionOnly(chartData, metrics, settings);

        // 4. Container & Chart
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();
        container.addProperty("key", String.class);
        container.addProperty("amount", Double.class);
        List<KeyValueEntity> entities = new ArrayList<>();
        String metricLabel = metric.getLabel();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = new KeyValueEntity();
            Object dimVal = item.getValue(dimension);
            Object metVal = item.getValue(metricLabel);
            kv.setValue("key", (dimVal != null) ? dimVal.toString() : "N/A");
            kv.setValue("amount", (metVal instanceof Number) ? ((Number) metVal).doubleValue() : 0.0);
            entities.add(kv);
        }
        container.setItems(entities);

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%"); chart.setHeight("100%");
        DataSet dataSet = new DataSet().withSource(new DataSet.Source<EntityDataItem>().withDataProvider(new ContainerChartItems<>(container)).withCategoryField("key").withValueFields("amount"));
        chart.setDataSet(dataSet);

        PieSeries series = new PieSeries();
        series.setName(dimension);
        Label label = new Label(); label.setShow(true); label.setFormatter("{b}: {d}%"); series.setLabel(label);
        if (settings.isDonut()) series.setRadius("50%", "70%"); else series.setRadius("0%", "70%");
        chart.addSeries(series);

        Legend legend = new Legend(); legend.setShow(true); legend.setOrientation(Orientation.HORIZONTAL); legend.setTop("0"); legend.setLeft("center"); chart.withLegend(legend);
        Tooltip tooltip = new Tooltip(); tooltip.setFormatter("{b}: {c} ({d}%)"); chart.withTooltip(tooltip);

        return chart;
    }

    private double getDouble(MapDataItem item, String col) {
        Object v = item.getValue(col);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.0;
    }
}