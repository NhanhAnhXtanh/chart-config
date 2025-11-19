package com.company.chartconfig.view.barconfigfragment;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.ViewComponent;

import java.util.List;

@FragmentDescriptor("bar-config-fragment.xml")
public class BarConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private JmixComboBox<String> barXFieldCombo;

    @ViewComponent
    private JmixComboBox<String> barYFieldCombo;

    public void setFields(List<String> fields) {
        barXFieldCombo.setItems(fields);
        barYFieldCombo.setItems(fields);
    }

    public String getXField() {
        return barXFieldCombo.getValue();
    }

    public String getYField() {
        return barYFieldCombo.getValue();
    }

    // 🔥 THÊM 2 HÀM NÀY
    public void setXField(String value) {
        barXFieldCombo.setValue(value);
    }

    public void setYField(String value) {
        barYFieldCombo.setValue(value);
    }
}
