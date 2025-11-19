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

    private List<String> fields;

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public void setLabelField(String value) {
        setZoneValue(labelDropZone, value);
    }

    public void setValueField(String value) {
        setZoneValue(valueDropZone, value);
    }

    public String getLabelField() {
        return labelDropZone.getElement().getProperty("serverValue", "");
    }

    public String getValueField() {
        return valueDropZone.getElement().getProperty("serverValue", "");
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        setupZone(labelDropZone);
        setupZone(valueDropZone);
    }

    // ===== Setup drop zone logic (JS + server sync)
    private void setupZone(Div zone) {
        styleDropZone(zone);

        zone.getElement().executeJs("""
            const zone = this;
            zone.addEventListener('dragover', e => {
                e.preventDefault();
                zone.classList.add('hovering');
            });
            zone.addEventListener('dragleave', e => {
                zone.classList.remove('hovering');
            });
            zone.addEventListener('drop', e => {
                e.preventDefault();
                zone.classList.remove('hovering');
                const text = e.dataTransfer.getData('text/plain');
                if (text && text.trim().length > 0) {
                    zone.innerHTML = `<span style="font-weight:600;color:#333">${text}</span>`;
                    zone.dispatchEvent(new CustomEvent('field-drop', { detail: { value: text } }));
                }
            });
        """);

        // Sync value server-side
        zone.getElement().addEventListener("field-drop", e -> {
            String newValue = e.getEventData().getString("event.detail.value");
            zone.getElement().setProperty("serverValue", newValue);
        }).addEventData("event.detail.value");
    }

    private void styleDropZone(Div zone) {
        zone.getStyle()
                .set("border", "2px dashed #b0bec5")
                .set("border-radius", "8px")
                .set("padding", "10px")
                .set("min-width", "180px")
                .set("min-height", "45px")
                .set("background-color", "#fafafa")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("transition", "all 0.25s ease");

        zone.getElement().executeJs("""
            const style = document.createElement('style');
            style.innerHTML = '.hovering { background-color:#e3f2fd !important; border-color:#42a5f5 !important; box-shadow:0 0 8px rgba(66,165,245,0.4);}';
            document.head.appendChild(style);
        """);
    }

    private void setZoneValue(Div zone, String value) {
        zone.removeAll();
        Span span = new Span(value == null ? "" : value);
        span.getStyle().set("font-weight", "600").set("color", "#333");
        zone.add(span);
        zone.getElement().setProperty("serverValue", value == null ? "" : value);
    }
}
