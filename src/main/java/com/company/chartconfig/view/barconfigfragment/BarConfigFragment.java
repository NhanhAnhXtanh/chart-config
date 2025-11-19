package com.company.chartconfig.view.barconfigfragment;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.Command;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import com.vaadin.flow.component.page.PendingJavaScriptResult;

import java.util.List;

@FragmentDescriptor("bar-config-fragment.xml")
public class BarConfigFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private Div xDropZone;
    @ViewComponent
    private Div yDropZone;

    private List<String> fields;

    private String xValue;
    private String yValue;

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public void setXField(String value) {
        this.xValue = value;
        setZoneValue(xDropZone, value);
    }

    public void setYField(String value) {
        this.yValue = value;
        setZoneValue(yDropZone, value);
    }

    public String getXField() {
        return xValue;
    }

    public String getYField() {
        return yValue;
    }

    @Subscribe
    public void onReady1(final ReadyEvent event) {
        setupZone(xDropZone, true);
        setupZone(yDropZone, false);
    }



    private void setupZone(Div zone, boolean isX) {
        styleDropZone(zone);

        // Thêm JS event
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
                    // gọi về server qua callFunction
                    $0.$server.onDropZoneChange($1, text);
                }
            });
            // Cho phép click để xóa
            zone.addEventListener('click', e => {
                zone.innerHTML = `<span style='color:#999;font-style:italic;'>(drop here)</span>`;
                $0.$server.onDropZoneChange($1, '');
            });
        """, getElement(), isX ? "x" : "y");
    }

    // JS gọi vào hàm này trên server
    @ClientCallable
    public void onDropZoneChange(String which, String value) {

        if ("x".equals(which)) {
            this.xValue = value;
            setZoneValue(xDropZone, value);
        } else {
            this.yValue = value;
            setZoneValue(yDropZone, value);
        }
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
            if (!document.querySelector('#hover-style')) {
                const style = document.createElement('style');
                style.id = 'hover-style';
                style.innerHTML = '.hovering { background-color:#e3f2fd !important; border-color:#42a5f5 !important; box-shadow:0 0 8px rgba(66,165,245,0.4);}';
                document.head.appendChild(style);
            }
        """);
    }

    private void setZoneValue(Div zone, String value) {
        zone.removeAll();
        if (value == null || value.isBlank()) {
            Span span = new Span("(drop here)");
            span.getStyle().set("color", "#999").set("font-style", "italic");
            zone.add(span);
        } else {
            Span span = new Span(value);
            span.getStyle().set("font-weight", "600").set("color", "#333");
            zone.add(span);
        }
    }
}
