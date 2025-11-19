package com.company.chartconfig.view.pieconfigfragment;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;

import java.util.List;

@FragmentDescriptor("pie-config-fragment.xml")
public class PieConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private Div labelDropZone;

    @ViewComponent
    private Div valueDropZone;

    @ViewComponent
    private Span labelValue;

    @ViewComponent
    private Span valueValue;

    private List<String> fields;

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public void setLabelField(String value) { labelValue.setText(value == null ? "" : value); }
    public void setValueField(String value) { valueValue.setText(value == null ? "" : value); }

    public String getLabelField() { return labelValue.getText(); }
    public String getValueField() { return valueValue.getText(); }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        enableDrop(labelDropZone, labelValue);
        enableDrop(valueDropZone, valueValue);
    }



    private void enableDrop(Div zone, Span label) {
        zone.getElement().setAttribute("ondragover", "event.preventDefault()");
        zone.getElement().setAttribute("ondrop",
                "const text = event.dataTransfer.getData('text/plain'); " +
                        "this.querySelector('span').innerText = text;");
    }
}
