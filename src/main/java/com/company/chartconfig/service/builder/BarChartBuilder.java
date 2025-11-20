package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.enums.ContributionMode; // Import Enum
import com.company.chartconfig.service.aggregator.ChartDataAggregator;
import com.company.chartconfig.service.filter.ChartDataFilter;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.ContainerChartItems;
import io.jmix.chartsflowui.data.item.EntityDataItem;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
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
import java.util.List;

@Component
public class BarChartBuilder implements ChartBuilder {
    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final ChartDataAggregator aggregator;
    private final DataComponents dataComponents;
    private final ChartDataFilter dataFilter;

    public BarChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper, ChartDataAggregator aggregator, DataComponents dataComponents, ChartDataFilter dataFilter) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.aggregator = aggregator;
        this.dataComponents = dataComponents;
        this.dataFilter = dataFilter;
    }

    @Override public boolean supports(ChartType type) { return type == ChartType.BAR; }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        String xField = root.path("xAxis").asText();

        // Parse Enum Contribution Mode
        String modeStr = root.path("contributionMode").asText("none");
        ContributionMode contribMode = ContributionMode.fromId(modeStr);

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });

        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu X hoặc Metric");

        // 1. Filter & Aggregate
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);

        // Truyền Enum vào Aggregator
        List<MapDataItem> chartData = aggregator.aggregate(filtered, xField, metrics, contribMode);

        if (chartData == null) chartData = new ArrayList<>();

        // 2. Create Container (Jmix Serializer)
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

        // 3. UI
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

        chart.addXAxis(new XAxis().withName(xField).withType(AxisType.CATEGORY));
        chart.addYAxis(new YAxis().withType(AxisType.VALUE));

        for (MetricConfig m : metrics) {
            BarSeries s = new BarSeries();
            s.setType(SeriesType.BAR);

            // Logic Stack dựa trên Enum
            if (contribMode == ContributionMode.ROW) {
                s.setStack("total");
                s.setName(m.getLabel() + " (%)");
            } else if (contribMode == ContributionMode.SERIES) {
                s.setName(m.getLabel() + " (%)");
            } else {
                s.setName(m.getLabel());
            }

            Encode encode = new Encode();
            encode.setX(xField); encode.setY(m.getLabel());
            s.setEncode(encode);
            chart.addSeries(s);
        }
        chart.withLegend(new Legend());
        chart.withTooltip(new io.jmix.chartsflowui.kit.component.model.Tooltip());
        return chart;
    }
}