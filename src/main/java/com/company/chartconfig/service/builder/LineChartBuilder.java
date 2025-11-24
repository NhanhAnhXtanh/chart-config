package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
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
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;
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
    public boolean supports(ChartType type) {
        return type == ChartType.LINE;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xAxisField = settings.getXAxisField();

        // 1. Parse config
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

        // 2. SERIES LIMIT (Cắt Metrics)
        dataProcessor.applyMetricLimit(metrics, seriesLimit);

        // 3. Aggregate
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // 4. [XỬ LÝ NGÀY THÁNG NGẮN GỌN]
        if (isDateColumn(chartData, xAxisField)) {
            chartData.sort(Comparator.comparing(item -> parseDateSortable(item.getValue(xAxisField))));
        }

        // 5. ROW LIMIT & Contribution
        dataProcessor.applyTailLimit(chartData, rowLimit);
        dataProcessor.processContributionOnly(chartData, metrics, settings);

        // 6. Build UI
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

        XAxis xAxis = new XAxis().withName(xAxisField).withType(AxisType.CATEGORY);
        AxisLabel xAxisLabel = new AxisLabel();
        xAxisLabel.setFormatterFunction("function(value) { return value; }");
        xAxis.setAxisLabel(xAxisLabel);
        chart.addXAxis(xAxis);

        YAxis yAxis = new YAxis().withType(AxisType.VALUE);
        AxisLabel yAxisLabel = new AxisLabel();
        yAxisLabel.setFormatterFunction(ChartFormatterUtils.getUniversalValueFormatter());
        yAxis.setAxisLabel(yAxisLabel);
        chart.addYAxis(yAxis);

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
        Tooltip tooltip = new Tooltip();
        tooltip.setTrigger(AbstractTooltip.Trigger.AXIS);
        tooltip.setValueFormatterFunction(ChartFormatterUtils.getTooltipNumberFormatter());
        chart.withTooltip(tooltip);

        return chart;
    }

    // Xủ lý date
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

        try {
            return new java.text.SimpleDateFormat("dd/MM/yyyy").parse(clean);
        } catch (Exception e) {
            return s;
        }
    }
}