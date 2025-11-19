package com.company.chartconfig.view.pieconfigfragment;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.ViewComponent;

import java.util.List;

@FragmentDescriptor("pie-config-fragment.xml")
public class PieConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private JmixComboBox<String> pieLabelFieldCombo;

    @ViewComponent
    private JmixComboBox<String> pieValueFieldCombo;

    public void setFields(List<String> fields) {
        pieLabelFieldCombo.setItems(fields);
        pieValueFieldCombo.setItems(fields);
    }

    public String getLabelField() {
        return pieLabelFieldCombo.getValue();
    }

    public String getValueField() {
        return pieValueFieldCombo.getValue();
    }

    // 🔥 THÊM 2 HÀM NÀY
    public void setLabelField(String value) {
        pieLabelFieldCombo.setValue(value);
    }

    public void setValueField(String value) {
        pieValueFieldCombo.setValue(value);
    }
}
