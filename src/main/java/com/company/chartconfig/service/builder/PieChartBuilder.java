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
    public boolean supports(ChartType type) {
        return type == ChartType.PIE;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        // 1. Parse Config
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String dimension = settings.getDimensionField();

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) {
            root.path("metrics").forEach(n -> {
                try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {}
            });
        }

        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) {
            root.path("filters").forEach(n -> {
                try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {}
            });
        }

        if (dimension == null || metrics.isEmpty()) {
            throw new IllegalStateException("Pie Chart yêu cầu Dimension và Metric");
        }

        MetricConfig metric = metrics.get(0);
        String metricLabel = metric.getLabel();

        // 2. Process Data
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(rawData, metrics, settings);
        dataProcessor.process(chartData, metrics, settings);

        // 3. Build UI Container
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();
        // Đặt tên field là "amount" để không trùng keyword "value"
        container.addProperty("key", String.class);
        container.addProperty("amount", Double.class);

        List<KeyValueEntity> entities = new ArrayList<>();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = new KeyValueEntity();
            Object dimVal = item.getValue(dimension);
            Object metVal = item.getValue(metricLabel);

            String keyStr = (dimVal != null) ? dimVal.toString() : "N/A";
            Double valDbl = (metVal instanceof Number) ? ((Number) metVal).doubleValue() : 0.0;

            kv.setValue("key", keyStr);
            kv.setValue("amount", valDbl); // Set vào amount

            entities.add(kv);
        }
        container.setItems(entities);

        // 4. Create Chart
        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%");
        chart.setHeight("100%");

        // --- DATASET ---
        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<EntityDataItem>()
                        .withDataProvider(new ContainerChartItems<>(container))
                        .withCategoryField("key")
                        .withValueFields("amount") // Map cột amount
        );
        chart.setDataSet(dataSet);

        // --- SERIES ---
        PieSeries series = new PieSeries();
        series.setName(dimension);

        // --- LABEL ---
        Label label = new Label();
        label.setShow(true);
        label.setFormatter("{b}: {d}%");
        series.setLabel(label);

        // Radius
        if (settings.isDonut()) {
            series.setRadius("50%", "70%");
        } else {
            series.setRadius("0%", "70%");
        }
        chart.addSeries(series);

        // --- LEGEND ---
        Legend legend = new Legend();
        legend.setShow(true);
        legend.setOrientation(Orientation.HORIZONTAL);
        legend.setTop("0");
        legend.setLeft("center");
        chart.withLegend(legend);

        // --- TOOLTIP ---
        Tooltip tooltip = new Tooltip();

        tooltip.setFormatter("{b}");

        chart.withTooltip(tooltip);

        return chart;
    }
}