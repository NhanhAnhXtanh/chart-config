package com.company.chartconfig.service;

import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.AxisType;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.flowui.UiComponents;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ChartConfigService {
    private final ObjectMapper objectMapper;
    private final UiComponents uiComponents;

    public ChartConfigService(ObjectMapper objectMapper, UiComponents uiComponents) {
        this.objectMapper = objectMapper;
        this.uiComponents = uiComponents;
    }

    // -------------------------------------------------------------
    // 1) Extract column names từ schemaJson (bắt buộc)
    // -------------------------------------------------------------
    public List<String> extractColumnsFromSchema(String schemaJson) {
        try {
            JsonNode root = objectMapper.readTree(schemaJson);

            if (!root.isArray()) {
                throw new RuntimeException("schemaJson phải là array");
            }

            List<String> columns = new ArrayList<>();

            for (JsonNode field : root) {
                columns.add(field.get("name").asText());
            }

            return columns;

        } catch (Exception e) {
            throw new RuntimeException("Schema JSON invalid: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // 2) Build items từ rawJson -> List<MapDataItem>
    // -------------------------------------------------------------
    public List<MapDataItem> buildItems(Dataset dataset) {

        try {
            JsonNode array = objectMapper.readTree(dataset.getRawJson());

            if (!array.isArray()) {
                throw new RuntimeException("rawJson phải là array");
            }

            List<MapDataItem> result = new ArrayList<>();

            for (JsonNode item : array) {
                MapDataItem mapItem = new MapDataItem();

                Iterator<String> names = item.fieldNames();
                while (names.hasNext()) {
                    String key = names.next();
                    JsonNode val = item.get(key);

                    if (val.isNumber()) mapItem.add(key, val.numberValue());
                    else if (val.isTextual()) mapItem.add(key, val.textValue());
                    else if (val.isBoolean()) mapItem.add(key, val.booleanValue());
                    else mapItem.add(key, val.toString());
                }

                result.add(mapItem);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("rawJson invalid: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // 3) Create BAR CHART bằng Java
    // -------------------------------------------------------------
    public Chart createBarChart(String xField, String yField, List<MapDataItem> items) {

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%");
        chart.setHeight("450px");

        // DataSet
        DataSet dataSet = new DataSet()
                .withSource(
                        new DataSet.Source<MapDataItem>()
                                .withDataProvider(new ListChartItems<>(items))
                                .withCategoryField(xField)
                                .withValueField(yField)
                );

        chart.setDataSet(dataSet);

        // X Axis
        XAxis xAxis = new XAxis();
        xAxis.setName(xField);
        xAxis.setType(AxisType.CATEGORY);
        chart.addXAxis(xAxis);

        // Y Axis
        YAxis yAxis = new YAxis();
        yAxis.setName(yField);
        yAxis.setType(AxisType.VALUE);
        chart.addYAxis(yAxis);

        // Series Bar
        BarSeries barSeries = new BarSeries();
        barSeries.setType(SeriesType.BAR);
        barSeries.setName(yField);
        barSeries.withDatasetIndex(0);

        chart.addSeries(barSeries);

        return chart;
    }

    public Chart createPieChart(String labelField, String valueField, List<MapDataItem> items) {

        Chart chart = uiComponents.create(Chart.class);
        chart.setWidth("100%");
        chart.setHeight("450px");

        DataSet dataSet = new DataSet()
                .withSource(
                        new DataSet.Source<MapDataItem>()
                                .withDataProvider(new ListChartItems<>(items))
                                .withCategoryField(labelField)
                                .withValueField(valueField)
                );

        chart.setDataSet(dataSet);

        PieSeries series = new PieSeries();
        series.setType(SeriesType.PIE);
        series.withDatasetIndex(0);

        chart.addSeries(series);

        return chart;
    }

    public Chart buildPreviewChart(Dataset dataset, ChartType type, String settingsJson) {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("ChartType must not be null");
        }
        if (settingsJson == null || settingsJson.isBlank()) {
            throw new IllegalArgumentException("settingsJson must not be empty");
        }

        try {
            JsonNode root = objectMapper.readTree(settingsJson);

            // Lấy items từ rawJson của dataset
            List<MapDataItem> items = buildItems(dataset);

            switch (type) {
                case BAR -> {
                    String xField = root.path("xField").asText(null);
                    String yField = root.path("yField").asText(null);

                    if (xField == null || yField == null) {
                        throw new IllegalStateException("BAR chart requires xField and yField in settingsJson");
                    }

                    return createBarChart(xField, yField, items);
                }
                case PIE -> {
                    String labelField = root.path("labelField").asText(null);
                    String valueField = root.path("valueField").asText(null);

                    if (labelField == null || valueField == null) {
                        throw new IllegalStateException("PIE chart requires labelField and valueField in settingsJson");
                    }

                    return createPieChart(labelField, valueField, items);
                }
                default -> throw new UnsupportedOperationException("Unsupported chart type: " + type);
            }

        } catch (Exception e) {
            throw new RuntimeException("Cannot build preview chart: " + e.getMessage(), e);
        }
    }




}
