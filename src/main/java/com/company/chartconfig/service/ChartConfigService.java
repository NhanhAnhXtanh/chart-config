package com.company.chartconfig.service;

import com.company.chartconfig.entity.Dataset;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChartConfigService {

    private final ObjectMapper objectMapper;

    public ChartConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse schemaJson → list column names
     */
    public List<String> extractColumnsFromSchema(String schemaJson) throws Exception {
        List<Map<String, Object>> schemaList = objectMapper.readValue(
                schemaJson,
                new TypeReference<List<Map<String, Object>>>() {}
        );

        List<String> columns = new ArrayList<>();
        for (Map<String, Object> col : schemaList) {
            Object name = col.get("name");
            if (name != null) columns.add(name.toString());
        }

        return columns;
    }

    /**
     * Render Bar Chart từ rawJson + X/Y
     */
    public void renderBarChart(Chart chart, Dataset dataset, String xField, String yField) throws Exception {

        // Parse JSON rows
        List<Map<String, Object>> rows = objectMapper.readValue(
                dataset.getRawJson(),
                new TypeReference<List<Map<String, Object>>>() {}
        );

        // Convert → DataItem
        List<MapDataItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MapDataItem item = new MapDataItem()
                    .add(xField, row.get(xField) != null ? row.get(xField).toString() : "")
                    .add(yField, row.get(yField) instanceof Number ? row.get(yField) : 0);

            items.add(item);
        }

        ListChartItems<MapDataItem> chartItems = new ListChartItems<>(items);

        // Clear old series
        if (chart.getSeries() != null) {
            for (var s : new ArrayList<>(chart.getSeries())) {
                chart.removeSeries(s);
            }
        }

        // DataSet
        DataSet dataSet = new DataSet()
                .withSource(
                        new DataSet.Source<MapDataItem>()
                                .withDataProvider(chartItems)
                                .withCategoryField(xField)
                                .withValueField(yField)
                );

        chart.setDataSet(dataSet);

        // Series
        BarSeries barSeries = new BarSeries();
        barSeries.setType(SeriesType.BAR);
        barSeries.setName(yField);
        chart.addSeries(barSeries);

        // Title
        Title title = new Title();
        title.setText(xField + " vs " + yField);
        chart.setTitle(title);

        // Legend
        Legend legend = new Legend();
        legend.setShow(true);
        chart.setLegend(legend);
    }
}
