package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;


@Route(value = "chart-configs", layout = MainView.class)
@ViewController(id = "ChartConfig.list")
@ViewDescriptor(path = "chart-config-list-view.xml")
@LookupComponent("chartConfigsDataGrid")
@DialogMode(width = "64em")
public class ChartConfigListView extends StandardListView<ChartConfig> {
    @ViewComponent
    private JmixButton createButton;

    @Subscribe(id = "createButton", subject = "clickListener")
    public void onCreateButtonClick(final ClickEvent<JmixButton> event) {

    }
}