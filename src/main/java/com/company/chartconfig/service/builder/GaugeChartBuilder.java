package com.company.chartconfig.service.builder;

import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.model.ChartCommonSettings;
import com.company.chartconfig.service.processor.ChartDataProcessor;
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
import io.jmix.flowui.UiComponents;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GaugeChartBuilder implements ChartBuilder {

    private final UiComponents uiComponents;
    private final ObjectMapper objectMapper;
    // Dùng Processor để lọc dữ liệu
    private final ChartDataProcessor dataProcessor;

    private static final String[] COLORS = {
            "#5470C6", "#91CC75", "#FAC858", "#EE6666", "#73C0DE",
            "#3BA272", "#FC8452", "#9A60B4", "#EA7CCC"
    };

    public GaugeChartBuilder(UiComponents uiComponents, ObjectMapper objectMapper,
                             ChartDataProcessor dataProcessor) {
        this.uiComponents = uiComponents;
        this.objectMapper = objectMapper;
        this.dataProcessor = dataProcessor;
    }

    @Override
    public boolean supports(ChartType type) {
        return type == ChartType.GAUGE;
    }

    @Override
    public Chart build(JsonNode root, List<MapDataItem> rawData) {
        double minVal = root.path("minVal").asDouble(0.0);
        double maxVal = root.path("maxVal").asDouble(100.0);
        ChartCommonSettings settings = new ChartCommonSettings(root); // Dummy settings để chạy hàm aggregate

        List<MetricConfig> metrics = new ArrayList<>();
        if (root.path("metrics").isArray()) {
            root.path("metrics").forEach(n -> { try { metrics.add(objectMapper.treeToValue(n, MetricConfig.class)); } catch (Exception e) {} });
        }
        List<FilterRule> filters = new ArrayList<>();
        if (root.path("filters").isArray()) {
            root.path("filters").forEach(n -> {
                try { filters.add(objectMapper.treeToValue(n, FilterRule.class)); } catch (Exception e) {}
            });
        }

        if (metrics.isEmpty()) throw new IllegalStateException("Gauge cần ít nhất 1 Metric");

        // --- SỬ DỤNG PROCESSOR ĐỂ LẤY SỐ LIỆU TỔNG ---
        // Với Gauge, ta coi toàn bộ dữ liệu là 1 nhóm (không có Dimension)
        settings.setxAxisField(null);

        // Gọi hàm prepareAggregatedData để lấy ra 1 dòng tổng duy nhất (do không group by gì cả)
        List<MapDataItem> aggregatedData = dataProcessor.prepareAggregatedData(rawData, metrics, filters, settings);

        // Lấy dòng đầu tiên (chứa tổng)
        MapDataItem totalRow = aggregatedData.isEmpty() ? new MapDataItem() : aggregatedData.get(0);
        // ----------------------------------------------

        List<GaugeSeries.DataItem> gaugeItems = new ArrayList<>();
        int colorIndex = 0;

        for (MetricConfig metric : metrics) {
            Object valObj = totalRow.getValue(metric.getLabel());
            double value = (valObj instanceof Number) ? ((Number) valObj).doubleValue() : 0.0;

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
        chart.setWidth("100%"); chart.setHeight("100%");

        GaugeSeries series = new GaugeSeries();
        series.setName("Metrics");
        series.setMin((int) minVal);
        series.setMax((int) maxVal);
        series.setStartAngle(210);
        series.setEndAngle(-30);

        GaugeSeries.AxisLine axisLine = new GaugeSeries.AxisLine();
        GaugeSeries.AxisLine.LineStyle lineStyle = new GaugeSeries.AxisLine.LineStyle();
        lineStyle.setWidth(10);
        axisLine.setLineStyle(lineStyle);
        series.setAxisLine(axisLine);

        GaugeSeries.Pointer pointer = new GaugeSeries.Pointer();
        pointer.setShow(true); pointer.setWidth(5);
        series.setPointer(pointer);

        GaugeSeries.Anchor anchor = new GaugeSeries.Anchor();
        anchor.setShow(true); anchor.setShowAbove(true); anchor.setSize(15);
        ItemStyle anchorStyle = new ItemStyle();
        anchorStyle.setColor(new Color("#fff"));
        anchorStyle.setBorderWidth(2);
        anchorStyle.setBorderColor(new Color("#999"));
        anchor.setItemStyle(anchorStyle);
        series.setAnchor(anchor);

        GaugeSeries.Title title = new GaugeSeries.Title();
        title.setShow(true); title.setOffsetCenter("0%", "80%"); title.setFontSize(14);
        series.setTitle(title);

        GaugeSeries.Detail detail = new GaugeSeries.Detail();
        detail.setValueAnimation(true); detail.setFormatter("{value}");
        detail.setOffsetCenter("0%", "100%"); detail.setFontSize(20); detail.setFontWeight("bold");
        series.setDetail(detail);

        series.setProgress(new GaugeSeries.Progress().withShow(false));
        series.setData(gaugeItems);

        chart.addSeries(series);
        Legend legend = new Legend();
        legend.setShow(true); legend.setBottom("0");
        chart.withLegend(legend);
        chart.withTooltip(new io.jmix.chartsflowui.kit.component.model.Tooltip().withFormatter("{b}: {c}"));

        return chart;
    }
}