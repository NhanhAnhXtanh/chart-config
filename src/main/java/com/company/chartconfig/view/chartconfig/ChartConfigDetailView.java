package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.main.MainView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "chart-configs/:id", layout = MainView.class)
@ViewController("ChartConfig.detail")
@ViewDescriptor("chart-config-detail-view.xml")
@EditedEntityContainer("chartConfigDc")
public class ChartConfigDetailView extends StandardDetailView<ChartConfig> {

    @Autowired private UiComponents ui;
    @Autowired private DataManager dataManager;
    @Autowired private FetchPlans fetchPlans;
    @Autowired private Notifications notifications;
    @Autowired private ChartConfigService chartService;
    @Autowired private ObjectMapper mapper;

    @ViewComponent private EntityPicker<Dataset> datasetField;
    @ViewComponent private JmixComboBox<ChartType> chartTypeField;
    @ViewComponent private VerticalLayout settingsBox;
    @ViewComponent private VerticalLayout chartContainer;

    private JmixComboBox<String> field1;
    private JmixComboBox<String> field2;

    @Subscribe
    public void onReady(ReadyEvent event) {
        buildSettingsForm();
    }

    @Subscribe("datasetField")
    public void onDatasetChanged(AbstractField.ComponentValueChangeEvent<?, ?> event) {
        buildSettingsForm();
    }

    @Subscribe("chartTypeField")
    public void onChartTypeChanged(AbstractField.ComponentValueChangeEvent<?, ?> event) {
        buildSettingsForm();
    }

    private void buildSettingsForm() {
        settingsBox.removeAll();

        Dataset ds = datasetField.getValue();
        com.company.chartconfig.enums.ChartType type = chartTypeField.getValue();
        if (ds == null || type == null) return;

        if (ds.getSchemaJson() == null) {
            notifications.create("Dataset chưa parse Schema").show();
            return;
        }

        List<String> columns = chartService.extractColumnsFromSchema(ds.getSchemaJson());

        field1 = ui.create(JmixComboBox.class);
        field2 = ui.create(JmixComboBox.class);

        field1.setItems(columns);
        field2.setItems(columns);

        switch (type) {
            case BAR -> {
                field1.setLabel("X Axis");
                field2.setLabel("Y Axis");
                settingsBox.add(field1, field2);
            }
            case PIE -> {
                field1.setLabel("Label Field");
                field2.setLabel("Value Field");
                settingsBox.add(field1, field2);
            }
        }
    }

    @Subscribe(id = "renderChartBtn", subject = "clickListener")
    public void onRenderChart(ClickEvent<JmixButton> e) {
        Dataset ds = datasetField.getValue();
        ChartType type = chartTypeField.getValue();

        if (ds == null || type == null) {
            notifications.create("Select dataset + type").show();
            return;
        }

        String f1 = field1.getValue();
        String f2 = field2.getValue();

        if (f1 == null || f2 == null) {
            notifications.create("Chưa chọn đủ field").show();
            return;
        }

        // Save settings into JSON
        ObjectNode node = mapper.createObjectNode();
        if (type == ChartType.BAR) {
            node.put("xField", f1);
            node.put("yField", f2);
        }
        if (type == ChartType.PIE) {
            node.put("labelField", f1);
            node.put("valueField", f2);
        }

        getEditedEntity().setSettingsJson(node.toString());

        // Load dataset items
        List<MapDataItem> items =
                chartService.buildItems(ds, f1, f2);

        chartContainer.removeAll();

        Chart chart = null;

        if (type == ChartType.BAR) {
            chart = chartService.createBarChart(f1, f2, items);
        }
        if (type == ChartType.PIE) {
            chart = chartService.createPieChart(f1, f2, items);
        }

        if (chart != null)
            chartContainer.add(chart);

        notifications.create("Render thành công").show();
    }
}
