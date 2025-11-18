package com.company.chartconfig.view.dataset;

import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;


@Route(value = "datasets", layout = MainView.class)
@ViewController(id = "Dataset.list")
@ViewDescriptor(path = "dataset-list-view.xml")
@LookupComponent("datasetsDataGrid")
@DialogMode(width = "64em")
public class DatasetListView extends StandardListView<Dataset> {
}