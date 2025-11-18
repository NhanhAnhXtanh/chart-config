package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.view.main.MainView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.axis.XAxis;
import io.jmix.chartsflowui.kit.component.model.axis.YAxis;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.BarSeries;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Route(value = "chart-configs/:id", layout = MainView.class)
@ViewController("ChartConfig.detail")
@ViewDescriptor("chart-config-detail-view.xml")
@EditedEntityContainer("chartConfigDc")
public class ChartConfigDetailView extends StandardDetailView<ChartConfig> {

    private static final Logger log = LoggerFactory.getLogger(ChartConfigDetailView.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Notifications notifications;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private FetchPlans fetchPlans;

    @ViewComponent
    private EntityPicker<Dataset> datasetField;
    @ViewComponent
    private JmixComboBox<String> xAxisField;
    @ViewComponent
    private JmixComboBox<String> yAxisField;
    @ViewComponent
    private Chart chart;

    @Subscribe("datasetField")
    public void onDatasetFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<EntityPicker<Dataset>, Dataset> event) {
        Dataset selectedDataset = event.getValue();
        if (selectedDataset == null) {
            xAxisField.setItems(new ArrayList<>());
            yAxisField.setItems(new ArrayList<>());
            return;
        }

        try {
            log.info("Selected Dataset ID = {}", selectedDataset.getId());

            // 🔹 Load lại dataset đầy đủ
            Dataset dataset = dataManager.load(Dataset.class)
                    .id(selectedDataset.getId())
                    .fetchPlan(fetchPlans.builder(Dataset.class)
                            .addAll("id", "name", "schemaJson", "rawJson")
                            .build())
                    .one();

            log.info("Loaded dataset name={}, schemaJson={}", dataset.getName(), dataset.getSchemaJson());

            String schemaJson = dataset.getSchemaJson();
            if (schemaJson == null || schemaJson.isBlank()) {
                notifications.create("⚠ Dataset chưa có schemaJson, hãy Parse JSON trong Dataset Detail trước!")
                        .withType(Notifications.Type.WARNING)
                        .show();
                return;
            }

            // Parse schemaJson → [{"name":"country","type":"string"}, ...]
            List<Map<String, Object>> schemaList = objectMapper.readValue(schemaJson,
                    new TypeReference<List<Map<String, Object>>>() {});

            List<String> columns = new ArrayList<>();
            for (Map<String, Object> col : schemaList) {
                Object name = col.get("name");
                if (name != null)
                    columns.add(name.toString());
            }

            if (columns.isEmpty()) {
                notifications.create("⚠ Không tìm thấy cột nào trong schemaJson!")
                        .withType(Notifications.Type.WARNING)
                        .show();
            } else {
                xAxisField.setItems(columns);
                yAxisField.setItems(columns);
                notifications.create("✅ Đã load " + columns.size() + " cột từ Dataset").show();
            }

        } catch (Exception e) {
            log.error("Lỗi khi parse schemaJson: ", e);
            notifications.create("❌ Lỗi khi parse schemaJson: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    @Subscribe(id = "renderChartBtn", subject = "clickListener")
    public void onRenderChartBtnClick(final ClickEvent<JmixButton> event) {
        Dataset selected = datasetField.getValue();
        if (selected == null) {
            notifications.create("⚠ Hãy chọn Dataset trước!").show();
            return;
        }

        // ALWAYS reload latest dataset from DB
        Dataset dataset = dataManager.load(Dataset.class)
                .id(selected.getId())
                .fetchPlan(fetchPlans.builder(Dataset.class)
                        .addAll("id", "name", "schemaJson", "rawJson")
                        .build())
                .one();

        String xField = xAxisField.getValue();
        String yField = yAxisField.getValue();

        if (xField == null || yField == null) {
            notifications.create("⚠ Hãy chọn X Axis và Y Axis!").show();
            return;
        }

        try {
            // Parse JSON dataset (always newest)
            List<Map<String, Object>> rows = objectMapper.readValue(
                    dataset.getRawJson(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Convert JSON rows → List<MapDataItem>
            List<MapDataItem> items = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                MapDataItem item = new MapDataItem()
                        .add(xField, row.get(xField) != null ? row.get(xField).toString() : "")
                        .add(yField, row.get(yField) instanceof Number ? row.get(yField) : 0);

                items.add(item);
            }

            ListChartItems<MapDataItem> chartItems = new ListChartItems<>(items);

            // Clear old series safely
            if (chart.getSeries() != null) {
                for (var s : new ArrayList<>(chart.getSeries())) {
                    chart.removeSeries(s);
                }
            }

            // DataSet mới (không set null)
            DataSet dataSet = new DataSet()
                    .withSource(
                            new DataSet.Source<MapDataItem>()
                                    .withDataProvider(chartItems)
                                    .withCategoryField(xField)
                                    .withValueField(yField)
                    );

            chart.setDataSet(dataSet);

            // Bar Series
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

            notifications.create("🚀 Render thành công").show();

        } catch (Exception ex) {
            notifications.create("❌ Lỗi render chart: " + ex.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }


}
