package com.company.chartconfig.view.newchartconfig;


import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.view.chartconfig.ChartConfigView;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Route(value = "new-chart-config", layout = MainView.class)
@ViewController(id = "NewChartConfig")
@ViewDescriptor(path = "new-chart-config.xml")
public class NewChartConfig extends StandardView {

    @ViewComponent
    private InstanceContainer<ChartConfig> chartConfigDc;

    @Autowired
    private ViewNavigators viewNavigators;

    @Autowired
    private Notifications notifications;

    @Subscribe
    public void onInit(InitEvent event) {
        // Tạo 1 ChartConfig rỗng để bind với entityPicker/comboBox
        chartConfigDc.setItem(new ChartConfig());
    }

    @Subscribe(id = "createBtn", subject = "clickListener")
    public void onCreateBtnClick(final ClickEvent<JmixButton> event) {
        ChartConfig cfg = chartConfigDc.getItemOrNull();
        if (cfg == null) {
            notifications.create("Config is empty").show();
            return;
        }
        if (cfg.getDataset() == null) {
            notifications.create("Please choose a dataset").show();
            return;
        }
        if (cfg.getChartType() == null) {
            notifications.create("Please choose chart type").show();
            return;
        }

        viewNavigators
                .view(this, ChartConfigView.class)
                .withAfterNavigationHandler(e -> {
                    ChartConfigView v = e.getView();
                    v.initParams(
                            cfg.getDataset().getId(),
                            cfg.getChartType()
                    );
                })
                .navigate();
    }

}