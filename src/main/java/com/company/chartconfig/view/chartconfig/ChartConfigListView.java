package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.view.main.MainView;
import com.company.chartconfig.view.newchartconfig.NewChartConfig;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;


@Route(value = "chart-configs", layout = MainView.class)
@ViewController(id = "ChartConfig.list")
@ViewDescriptor(path = "chart-config-list-view.xml")
@LookupComponent("chartConfigsDataGrid")
@DialogMode(width = "64em")
public class ChartConfigListView extends StandardListView<ChartConfig> {
    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private DataGrid<ChartConfig> chartConfigsDataGrid;


    @Subscribe("chartConfigsDataGrid.createAction")
    public void onChartConfigsDataGridCreateAction(final ActionPerformedEvent event) {
        viewNavigators
                .view(this, NewChartConfig.class)
                .navigate();
    }

    @Subscribe("chartConfigsDataGrid.editAction")
    public void onChartConfigsDataGridEditAction(final ActionPerformedEvent event) {
        ChartConfig selected = chartConfigsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.create("Please select a chart").show();
            return;
        }

        viewNavigators
                .view(this, ChartConfigView.class)
                .withAfterNavigationHandler(navEvent -> {
                    ChartConfigView view = navEvent.getView();
                    view.initFromExisting(selected.getId());
                })
                .navigate();
    }

//    @Subscribe("chartConfigsDataGrid")
//    public void onChartConfigsDataGridItemDoubleClick(final ItemDoubleClickEvent<ChartConfig> event) {
//        ChartConfig selected = chartConfigsDataGrid.getSingleSelectedItem();
//        if (selected == null) {
//            notifications.create("Please select a chart").show();
//            return;
//        }
//
//        viewNavigators
//                .view(this, ChartConfigView.class)
//                .withAfterNavigationHandler(navEvent -> {
//                    ChartConfigView view = navEvent.getView();
//                    view.initFromExisting(selected.getId());
//                })
//                .navigate();
//    }

    @Subscribe("chartConfigsDataGrid")
    public void onChartConfigsDataGridItemClick(final ItemClickEvent<ChartConfig> event) {
        ChartConfig selected = chartConfigsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.create("Please select a chart").show();
            return;
        }

        viewNavigators
                .view(this, ChartConfigView.class)
                .withAfterNavigationHandler(navEvent -> {
                    ChartConfigView view = navEvent.getView();
                    view.initFromExisting(selected.getId());
                })
                .navigate();
    }
}