package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.aggregator.ChartDataAggregator;
import com.company.chartconfig.service.filter.ChartDataFilter;
import com.company.chartconfig.utils.FilterRule;
import com.company.chartconfig.view.common.MetricConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.GaugeSeries;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.chartsflowui.kit.component.model.shared.ItemStyle;
// [FIX] Xóa import LineStyle gây nhầm lẫn
import io.jmix.flowui.UiComponents;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class GaugeChartBuilder implements ChartBuilder {

    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    private final ChartDataFilter dataFilter;
    private final ChartDataAggregator aggregator;

    private static final String[] COLORS = {
            "#5470C6", "#91CC75", "#FAC858", "#EE6666", "#73C0DE",
            "#3BA272", "#FC8452", "#9A60B4", "#EA7CCC"
    };

    public GaugeChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper,
                             ChartDataFilter dataFilter, ChartDataAggregator aggregator) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataFilter = dataFilter;
        this.aggregator = aggregator;
    }

    @Override
    public boolean supports(ChartType type) {
        return type == ChartType.GAUGE;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        double minVal = root.path("minVal").asDouble(0.0);
        double maxVal = root.path("maxVal").asDouble(100.0);

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) {
            root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        }
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) {
            root.path("filters").forEach(n -> { try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {} });
        }

        if (metrics.isEmpty()) throw new IllegalStateException("Gauge cần ít nhất 1 Metric");

        List<MapDataItem> filteredData = dataFilter.filter(rawData, filters);
        List<GaugeSeries.DataItem> gaugeItems = new ArrayList<>();
        int colorIndex = 0;

        for (MetricConfig metric : metrics) {
            BigDecimal val = aggregator.calculateMetric(filteredData, metric.getColumn(), metric.getAggregate());
            double value = (val != null) ? val.doubleValue() : 0.0;

            String colorHex = COLORS[colorIndex % COLORS.length];
            colorIndex++;

            GaugeSeries.DataItem item = new GaugeSeries.DataItem()
                    .withName(metric.getLabel())
                    .withValue(value);

            ItemStyle itemStyle = new ItemStyle();
            itemStyle.setColor(new Color(colorHex));
            item.setItemStyle(itemStyle);

            gaugeItems.add(item);
        }

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%");
        chart.setHeight("100%");

        GaugeSeries series = new GaugeSeries();
        series.setName("Metrics");
        series.setMin((int) minVal);
        series.setMax((int) maxVal);
        series.setStartAngle(210);
        series.setEndAngle(-30);

        // 1. Trục đo (Vòng cung)
        GaugeSeries.AxisLine axisLine = new GaugeSeries.AxisLine();

        // [FIX] Sử dụng đúng class nội bộ: GaugeSeries.AxisLine.LineStyle
        GaugeSeries.AxisLine.LineStyle lineStyle = new GaugeSeries.AxisLine.LineStyle();
        lineStyle.setWidth(10);

        axisLine.setLineStyle(lineStyle);
        series.setAxisLine(axisLine);

        // 2. Kim chỉ
        GaugeSeries.Pointer pointer = new GaugeSeries.Pointer();
        pointer.setShow(true);
        pointer.setWidth(5);
        series.setPointer(pointer);

        // 3. Chốt giữa (Anchor)
        GaugeSeries.Anchor anchor = new GaugeSeries.Anchor();
        anchor.setShow(true);
        anchor.setShowAbove(true);
        anchor.setSize(15);

        ItemStyle anchorStyle = new ItemStyle();
        anchorStyle.setColor(new Color("#fff"));
        anchorStyle.setBorderWidth(2);
        anchorStyle.setBorderColor(new Color("#999"));
        anchor.setItemStyle(anchorStyle);

        series.setAnchor(anchor);

        // 4. Tiêu đề & Chi tiết
        GaugeSeries.Title title = new GaugeSeries.Title();
        title.setShow(true);
        title.setOffsetCenter("0%", "80%");
        title.setFontSize(14);
        series.setTitle(title);

        GaugeSeries.Detail detail = new GaugeSeries.Detail();
        detail.setValueAnimation(true);
        detail.setFormatter("{value}");
        detail.setOffsetCenter("0%", "100%");
        detail.setFontSize(20);
        detail.setFontWeight("bold");
        series.setDetail(detail);

        series.setProgress(new GaugeSeries.Progress().withShow(false));
        series.setData(gaugeItems);

        chart.addSeries(series);

        Legend legend = new Legend();
        legend.setShow(true);
        legend.setBottom("0");
        chart.withLegend(legend);

        chart.withTooltip(new io.jmix.chartsflowui.kit.component.model.Tooltip().withFormatter("{b}: {c}"));

        return chart;
    }
}