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
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.axis.*;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.Encode;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
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
    private final ChartDataAggregator aggregator;
    private final DataComponents dataComponents;
    private final ChartDataFilter dataFilter;
    private final ChartDataProcessor dataProcessor;
    @Autowired
    protected DataManager dataManager;

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
        int rowLimit = settings.getRowLimit();

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });

        if (xField == null || metrics.isEmpty()) throw new IllegalStateException("Thiếu thông tin");

        // 1. Aggregate
        List<MapDataItem> filtered = dataFilter.filter(rawData, filters);
        List<MapDataItem> chartData = aggregator.aggregate(filtered, metrics, settings);
        if (chartData == null) chartData = new ArrayList<>();

        // 2. Sort (Sort DESC theo metric đầu tiên để lấy Top N nếu chưa có sort)
        if (settings.getQuerySortMetric() == null && !metrics.isEmpty()) {
            String sortKey = metrics.get(0).getLabel();
            chartData.sort((o1, o2) -> Double.compare(getDouble(o2, sortKey), getDouble(o1, sortKey)));
        }

        // 3. Visual Sort (X-Axis Sort)
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

        // 4. Limit (Top N)
        dataProcessor.applyHeadLimit(chartData, rowLimit);

        // 5. Contribution
        dataProcessor.processContributionOnly(chartData, metrics, settings);

        // 6. BUILD CONTAINER (FIX DỮ LIỆU NULL)
        KeyValueCollectionContainer container = dataComponents.createKeyValueCollectionContainer();

        // Add property cho Category (luôn là String để an toàn hiển thị)
        container.addProperty(xField, String.class);

        for (MetricConfig m : metrics) container.addProperty(m.getLabel(), Double.class);

        List<KeyValueEntity> entities = new ArrayList<>();
        for (MapDataItem item : chartData) {
            KeyValueEntity kv = dataManager.create(KeyValueEntity.class);

            // --- [FIX QUAN TRỌNG] ---
            Object rawCat = item.getValue(xField);
            String safeCat = (rawCat != null) ? rawCat.toString() : "Unknown"; // Không bao giờ để null
            kv.setValue(xField, safeCat);
            // ------------------------

            for (MetricConfig m : metrics) kv.setValue(m.getLabel(), item.getValue(m.getLabel()));
            entities.add(kv);
        }
        container.setItems(entities);

        // 7. BUILD CHART
        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%"); chart.setHeight("100%");

        // Grid: ContainLabel=true để không mất chữ
        Grid grid = new Grid();
        grid.setContainLabel(true);
        grid.setBottom("10%"); // Đẩy lên 1 chút
        chart.withGrid(grid);

        String[] valFields = metrics.stream().map(MetricConfig::getLabel).toArray(String[]::new);

        DataSet dataSet = new DataSet().withSource(
                new DataSet.Source<EntityDataItem>()
                        .withDataProvider(new ContainerChartItems<>(container))
                        .withCategoryField(xField) // Mapping đúng trường category
                        .withValueFields(valFields)
        );
        chart.setDataSet(dataSet);

        // X-Axis
        XAxis xAxis = new XAxis()
                .withName(xField)
                .withNameLocation(HasAxisName.NameLocation.CENTER)
                .withNameGap(35);

        // Bar Chart bắt buộc dùng CATEGORY cho trục X để hiển thị tên cột
        xAxis.withType(AxisType.CATEGORY);

        AxisLabel xAxisLabel = new AxisLabel();
        xAxisLabel.setInterval(0);
        xAxisLabel.setFormatterFunction("function(value) { return value; }");
        xAxis.setAxisLabel(xAxisLabel);
        chart.addXAxis(xAxis);

        // Y-Axis
        YAxis yAxis = new YAxis().withType(AxisType.VALUE);
        AxisLabel yAxisLabel = new AxisLabel();
        if (settings.getContributionMode() != ContributionMode.NONE) yAxisLabel.setFormatter("{value} %");
        else yAxisLabel.setFormatterFunction(ChartFormatterUtils.getUniversalValueFormatter());
        yAxis.setAxisLabel(yAxisLabel);
        chart.addYAxis(yAxis);

        // Series
        for (MetricConfig m : metrics) {
            BarSeries s = new BarSeries();
            s.setName(m.getLabel());
            if (settings.getContributionMode() == ContributionMode.ROW) s.setStack("total");

            Encode encode = new Encode();
            encode.setX(xField);     // Map trục X
            encode.setY(m.getLabel()); // Map trục Y
            s.setEncode(encode);
            chart.addSeries(s);
        }

        Legend legend = new Legend();
        legend.setShow(true);
        legend.setTop("0");
        chart.withLegend(legend);

        io.jmix.chartsflowui.kit.component.model.Tooltip tooltip = new io.jmix.chartsflowui.kit.component.model.Tooltip();
        tooltip.setTrigger(io.jmix.chartsflowui.kit.component.model.shared.AbstractTooltip.Trigger.AXIS);
        if (settings.getContributionMode() != ContributionMode.NONE) tooltip.setValueFormatterFunction("function(value) { return value ? Number(value).toFixed(2) + ' %' : '0 %'; }");
        else tooltip.setValueFormatterFunction(ChartFormatterUtils.getTooltipNumberFormatter());
        chart.withTooltip(tooltip);

        return chart;
    }

    private double getDouble(MapDataItem item, String col) {
        Object v = item.getValue(col);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return 0.0;
    }
}