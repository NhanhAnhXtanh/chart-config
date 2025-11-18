package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route(value = "chart-configs/:id", layout = MainView.class)
@ViewController("ChartConfig.detail")
@ViewDescriptor("chart-config-detail-view.xml")
@EditedEntityContainer("chartConfigDc")
public class ChartConfigDetailView extends StandardDetailView<ChartConfig> {

    @Autowired
    private ChartConfigService chartConfigService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private FetchPlans fetchPlans;
    @Autowired
    private UiComponents uiComponents;   // <-- BẮT BUỘC

    @ViewComponent
    private EntityPicker<Dataset> datasetField;
    @ViewComponent
    private JmixComboBox<String> xAxisField;
    @ViewComponent
    private JmixComboBox<String> yAxisField;
    @ViewComponent
    private VerticalLayout chartContainer;

    // -------------------------------------------------------------
    // Load columns khi chọn Dataset
    // -------------------------------------------------------------
    @Subscribe("datasetField")
    public void onDatasetFieldComponentValueChange(
            AbstractField.ComponentValueChangeEvent<EntityPicker<Dataset>, Dataset> event) {

        Dataset selectedDataset = event.getValue();
        if (selectedDataset == null) {
            xAxisField.clear();
            yAxisField.clear();
            xAxisField.setItems(new ArrayList<>());
            yAxisField.setItems(new ArrayList<>());
            return;
        }

        try {
            Dataset dataset = dataManager.load(Dataset.class)
                    .id(selectedDataset.getId())
                    .fetchPlan(fetchPlans.builder(Dataset.class)
                            .addAll("id", "name", "schemaJson", "rawJson")
                            .build())
                    .one();

            if (dataset.getSchemaJson() == null) {
                notifications.create("⚠ Dataset chưa có schemaJson.").show();
                return;
            }

            List<String> columns =
                    chartConfigService.extractColumnsFromSchema(dataset.getSchemaJson());

            xAxisField.setItems(columns);
            yAxisField.setItems(columns);

            notifications.create("✅ Loaded " + columns.size() + " columns").show();

        } catch (Exception e) {
            notifications.create("❌ Lỗi load schema: " + e.getMessage()).show();
        }
    }

    // -------------------------------------------------------------
    // Render Chart bằng Java
    // -------------------------------------------------------------
    @Subscribe(id = "renderChartBtn", subject = "clickListener")
    public void onRenderChartBtnClick(ClickEvent<JmixButton> event) {

        ChartConfig chartConfig = getEditedEntity();
        Dataset selected = datasetField.getValue();

        if (selected == null) {
            notifications.create("⚠ Hãy chọn Dataset!").show();
            return;
        }

        // Reload dataset mới nhất
        Dataset dataset = dataManager.load(Dataset.class)
                .id(selected.getId())
                .fetchPlan(fetchPlans.builder(Dataset.class)
                        .addAll("id", "name", "schemaJson", "rawJson")
                        .build())
                .one();

        String xField = xAxisField.getValue();
        String yField = yAxisField.getValue();

        if (xField == null || yField == null) {
            notifications.create("⚠ Chọn X Axis và Y Axis!").show();
            return;
        }

        try {
            // Parse rawJson -> List<MapDataItem>
            List<MapDataItem> items = chartConfigService.buildItems(dataset, xField, yField);

            // Clear container
            chartContainer.removeAll();

            Chart chart = null;

            // Tạo chart đúng type
            if ("BAR".equalsIgnoreCase(chartConfig.getChartType())) {
                chart = chartConfigService.createBarChart(xField, yField, items);
            }

            if (chart == null) {
                notifications.create("⚠ Chart Type chưa hỗ trợ!").show();
                return;
            }

            // Add vào UI
            chartContainer.add(chart);

            notifications.create("🚀 Render Chart thành công!").show();

        } catch (Exception e) {
            notifications.create("❌ Lỗi render chart: " + e.getMessage()).show();
        }
    }
}
