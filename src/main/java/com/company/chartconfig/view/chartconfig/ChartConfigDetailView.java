package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlans;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route(value = "chart-configs/:id", layout = MainView.class)
@ViewController("ChartConfig.detail")
@ViewDescriptor("chart-config-detail-view.xml")
@EditedEntityContainer("chartConfigDc")
public class ChartConfigDetailView extends StandardDetailView<ChartConfig> {

    private static final Logger log = LoggerFactory.getLogger(ChartConfigDetailView.class);
    @Autowired
    private ChartConfigService chartConfigService;
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
    public void onDatasetFieldComponentValueChange(
            AbstractField.ComponentValueChangeEvent<EntityPicker<Dataset>, Dataset> event) {

        Dataset selectedDataset = event.getValue();
        if (selectedDataset == null) {
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


    @Subscribe(id = "renderChartBtn", subject = "clickListener")
    public void onRenderChartBtnClick(ClickEvent<JmixButton> event) {
        Dataset selected = datasetField.getValue();

        if (selected == null) {
            notifications.create("⚠ Hãy chọn Dataset!").show();
            return;
        }

        Dataset dataset = dataManager.load(Dataset.class)
                .id(selected.getId())
                .fetchPlan(fetchPlans.builder(Dataset.class)
                        .addAll("id", "name", "schemaJson", "rawJson")
                        .build())
                .one();

        if (xAxisField.getValue() == null || yAxisField.getValue() == null) {
            notifications.create("⚠ Chọn X và Y Axis!").show();
            return;
        }

        try {
            chartConfigService.renderBarChart(
                    chart,
                    dataset,
                    xAxisField.getValue(),
                    yAxisField.getValue()
            );

            notifications.create("🚀 Render thành công!").show();

        } catch (Exception e) {
            notifications.create("❌ Lỗi render chart: " + e.getMessage()).show();
        }
    }

}
