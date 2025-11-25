package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.enums.ContributionMode;
import com.company.chartconfig.model.ChartCommonSettings;
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
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.axis.AxisLabel;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.HasAxisName;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.Encode;
import io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class BarChartBuilder implements ChartBuilder {

    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final DataComponents dataComponents;
    // Bỏ Filter & Aggregator trực tiếp, dùng Processor
    private final ChartDataProcessor dataProcessor;
    @Autowired
    protected DataManager dataManager;

    public BarChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper,
                           DataComponents dataComponents, ChartDataProcessor dataProcessor) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataComponents = dataComponents;
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean supports(ChartType type) {
        return type == ChartType.BAR;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        ChartCommonSettings settings = new ChartCommonSettings(root);
        String xField = settings.getXAxisField();

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

        if (xField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu thông tin");

        // --- SỬ DỤNG PROCESSOR MỚI ---
        List<MapDataItem> chartData = dataProcessor.processFullPipeline(rawData, metrics, filters, settings);
        // ------------------------------

        // 3. Visual Sort (X-Axis Sort) - Logic riêng của Bar Chart
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
            if (!settings.isXAxisSortAsc()) {
                Collections.reverse(chartData);
            }
        }

        // --- BUILD CHART UI ---
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();
        container.addProperty(xField, String.class);
        for (MetricConfig m : metrics) container.addProperty(m.getLabel(), Double.class);

        List<KeyValueEntity> entities = new ArrayList<>();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = dataManager.create(KeyValueEntity.class);
            Object rawCat = item.getValue(xField);
            String safeCat = (rawCat != null) ? rawCat.toString() : "Unknown";
            kv.setValue(xField, safeCat);
            for (MetricConfig m : metrics) kv.setValue(m.getLabel(), item.getValue(m.getLabel()));
            entities.add(kv);
        }
        container.setItems(entities);

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%"); chart.setHeight("100%");

        Grid grid = new Grid();
        grid.setContainLabel(true);
        grid.setBottom("10%");
        chart.withGrid(grid);

        String[] valFields = metrics.stream().map(MetricConfig::getLabel).toArray(String[]::new);
        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<EntityDataItem>()
                        .withDataProvider(new ContainerChartItems<>(container))
                        .withCategoryField(xField)
                        .withValueFields(valFields)
        );
        chart.setDataSet(dataSet);

        XAxis xAxis = new XAxis()
                .withName(xField)
                .withNameLocation(HasAxisName.NameLocation.CENTER)
                .withNameGap(35)
                .withType(AxisType.CATEGORY);
        AxisLabel xAxisLabel = new AxisLabel();
        xAxisLabel.setInterval(0);
        xAxisLabel.setFormatterFunction("function(value) { return value; }");
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

        Legend legend = new Legend();
        legend.setShow(true);
        legend.setTop("0");
        chart.withLegend(legend);

        Tooltip tooltip = new Tooltip();
        tooltip.setTrigger(AbstractTooltip.Trigger.AXIS);
        if (settings.getContributionMode() != ContributionMode.NONE)
            tooltip.setValueFormatterFunction("function(value) { return value ? Number(value).toFixed(2) + ' %' : '0 %'; }");
        else tooltip.setValueFormatterFunction(ChartFormatterUtils.getTooltipNumberFormatter());
        chart.withTooltip(tooltip);

        return chart;
    }
}