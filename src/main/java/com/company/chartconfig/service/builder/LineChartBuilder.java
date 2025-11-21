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

    // Không dùng Processor mặc định để tránh việc nó sort lại theo Metric
    // private final ChartDataProcessor dataProcessor;

    public LineChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper, DataComponents dataComponents,
                            ChartDataAggregator aggregator, ChartDataFilter dataFilter) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataComponents = dataComponents;
        this.aggregator = aggregator;
        this.dataFilter = dataFilter;
    }

    @Override
    public boolean supports(ChartType type) {
        return type == ChartType.LINE;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        // 1. Parse Config
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xAxisField = settings.getXAxisField();

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

        if (xAxisField == null || metrics.isEmpty()) {
            throw new IllegalStateException("Line Chart requires X-Axis and at least one Metric");
        }

        // 2. Process Data
        // 2.1 Filter
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);

        // 2.2 Aggregate
        List<MapDataItem> chartData = aggregator.aggregate(filtered, xAxisField, metrics);

        // 2.3 [QUAN TRỌNG] SORT BY X-AXIS ASCENDING
        // Logic này đảm bảo ngày tháng/số thứ tự tăng dần
        chartData.sort((o1, o2) -> {
            Object v1 = o1.getValue(xAxisField);
            Object v2 = o2.getValue(xAxisField);

            if (v1 == null && v2 == null) return 0;
            if (v1 == null) return -1;
            if (v2 == null) return 1;

            // So sánh an toàn (String Date, Number, hoặc String thường)
            if (v1 instanceof Comparable && v2 instanceof Comparable) {
                return ((Comparable) v1).compareTo(v2);
            }
            // Fallback về String comparison
            return v1.toString().compareTo(v2.toString());
        });

        // *Lưu ý*: Ta bỏ qua bước dataProcessor.process() vì nó sẽ sort lại theo Metric giảm dần (Top N)
        // Nếu cần tính năng Contribution Mode cho Line, ta có thể gọi riêng hàm đó, nhưng thường Line ít dùng stack 100%.

        // 3. Build UI Container
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();

        container.addProperty(xAxisField, String.class);
        for (MetricConfig m : metrics) {
            container.addProperty(m.getLabel(), Double.class);
        }

        List<KeyValueEntity> entities = new ArrayList<>();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = new KeyValueEntity();

            // Set X-Axis Value
            Object xVal = item.getValue(xAxisField);
            kv.setValue(xAxisField, xVal != null ? xVal.toString() : "Unknown");

            // Set Metric Values
            for (MetricConfig m : metrics) {
                Object mVal = item.getValue(m.getLabel());
                Double valDbl = (mVal instanceof Number) ? ((Number) mVal).doubleValue() : 0.0;
                kv.setValue(m.getLabel(), valDbl);
            }

            entities.add(kv);
        }
        container.setItems(entities);

        // 4. Create Chart
        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%");
        chart.setHeight("100%");

        // --- DATASET ---
        String[] valueFields = metrics.stream().map(MetricConfig::getLabel).toArray(String[]::new);

        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<EntityDataItem>()
                        .withDataProvider(new ContainerChartItems<>(container))
                        .withCategoryField(xAxisField)
                        .withValueFields(valueFields)
        );
        chart.setDataSet(dataSet);

        // --- AXES ---
        chart.addXAxis(new XAxis().withName(xAxisField).withType(AxisType.CATEGORY));
        chart.addYAxis(new YAxis().withType(AxisType.VALUE));

        // --- SERIES ---
        for (MetricConfig m : metrics) {
            LineSeries series = new LineSeries();
            series.setName(m.getLabel());
//            series.setSmooth(true); // Làm mềm đường

            Encode encode = new Encode();
            encode.setX(xAxisField);
            encode.setY(m.getLabel());
            series.setEncode(encode);

            chart.addSeries(series);
        }

        // --- LEGEND ---
        Legend legend = new Legend();
        legend.setShow(true);
        legend.setBottom("0");
        chart.withLegend(legend);

        // --- TOOLTIP ---
        Tooltip tooltip = new Tooltip();
        tooltip.setTrigger(AbstractTooltip.Trigger.AXIS);
        chart.withTooltip(tooltip);

        return chart;
    }
}