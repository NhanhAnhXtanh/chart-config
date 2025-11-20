package com.company.chartconfig.view.newchartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.view.chartconfig.ChartConfigView;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "new-chart-config", layout = MainView.class)
@ViewController("NewChartConfig")
@ViewDescriptor("new-chart-config.xml")
public class NewChartConfig extends StandardView {

    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private InstanceContainer<ChartConfig> chartConfigDc;

    @ViewComponent
    private Div chartTypeContainer;

    @Autowired
    private Notifications notifications;

    @Autowired
    private ViewNavigators viewNavigators;

    private Div selectedCardElement = null;

    @Subscribe
    public void onInit(InitEvent event) {
        chartConfigDc.setItem(dataManager.create(ChartConfig.class));
        loadChartTypes();
    }

    private void loadChartTypes() {
        List<KeyValueEntity> items = List.of(
                create("BAR", VaadinIcon.BAR_CHART, "Bar Chart", "Comparison"),
                create("PIE", VaadinIcon.PIE_CHART, "Pie Chart", "Distribution"),
                create("LINE", VaadinIcon.LINE_CHART, "Line Chart", "Trend"),
                create("AREA", VaadinIcon.AREA_SELECT, "Area Chart", "Volume"),
                create("SCATTER", VaadinIcon.SCATTER_CHART, "Scatter", "Correlation"),
                create("DONUT", VaadinIcon.CIRCLE_THIN, "Donut", "Proportion")
        );

        chartTypeContainer.removeAll();

        for (KeyValueEntity item : items) {
            Component card = createClickableCard(item);
            chartTypeContainer.add(card);
        }
    }

    private KeyValueEntity create(String code, VaadinIcon icon, String name, String tags) {
        KeyValueEntity kv = dataManager.create(KeyValueEntity.class);
        kv.setValue("code", code);
        kv.setValue("icon", icon);
        kv.setValue("name", name);
        kv.setValue("tags", tags);
        return kv;
    }

    private Component createClickableCard(KeyValueEntity item) {
        String code = item.getValue("code");
        VaadinIcon iconEnum = item.getValue("icon");
        String name = item.getValue("name");
        String tags = item.getValue("tags");

        Div card = new Div();
        card.getStyle()
                .set("background-color", "#ffffff")
                .set("border", "2px solid #e2e8f0")
                .set("border-radius", "16px")
                .set("padding", "20px")
                .set("cursor", "pointer")
                .set("transition", "all 0.2s ease-in-out")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("gap", "12px")
                .set("min-height", "160px")
                .set("box-sizing", "border-box");

        Div iconWrapper = new Div();
        iconWrapper.getStyle()
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("width", "56px")
                .set("height", "56px")
                .set("border-radius", "50%")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        Icon icon = iconEnum.create();
        icon.setSize("26px");
        icon.getStyle().set("color", "var(--lumo-secondary-text-color)");
        iconWrapper.add(icon);

        Span title = new Span(name);
        title.getStyle()
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("color", "var(--lumo-header-text-color)");

        Span tagSpan = new Span(tags);
        tagSpan.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "2px 8px")
                .set("border-radius", "10px");

        card.add(iconWrapper, title, tagSpan);

        card.addClickListener(e -> {
            selectCode(code);
            highlightSelected(card, iconWrapper, icon);
        });

        card.getElement().addEventListener("mouseenter", e -> {
            if (selectedCardElement != card) {
                card.getStyle()
                        .set("transform", "translateY(-5px)")
                        .set("box-shadow", "0 10px 15px -3px rgba(0, 0, 0, 0.1)")
                        .set("border-color", "#cbd5e0");
            }
        });

        card.getElement().addEventListener("mouseleave", e -> {
            if (selectedCardElement != card) {
                card.getStyle()
                        .set("transform", "none")
                        .set("box-shadow", "none")
                        .set("border-color", "#e2e8f0");
            }
        });

        return card;
    }

    private void highlightSelected(Div clickedCard, Div iconWrapper, Icon icon) {
        if (selectedCardElement != null && selectedCardElement != clickedCard) {
            selectedCardElement.getStyle()
                    .set("border-color", "#e2e8f0")
                    .set("background-color", "#ffffff")
                    .set("transform", "none")
                    .set("box-shadow", "none");
        }

        selectedCardElement = clickedCard;
        clickedCard.getStyle()
                .set("border-color", "var(--lumo-primary-color)")
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("box-shadow", "0 0 0 1px var(--lumo-primary-color)")
                .set("transform", "scale(1.02)");

        icon.getStyle().set("color", "var(--lumo-primary-color)");
        iconWrapper.getStyle().set("background-color", "#ffffff");
    }

    private void selectCode(String code) {
        try {
            chartConfigDc.getItem().setChartType(ChartType.valueOf(code));
        } catch (IllegalArgumentException e) {
            notifications.create("Error: " + code).show();
        }
    }

    @Subscribe("createBtn")
    public void onCreateBtnClick(ClickEvent<JmixButton> event) {
        ChartConfig cfg = chartConfigDc.getItem();
        if (cfg.getDataset() == null) {
            notifications.create("Vui lòng chọn Dataset").withType(Notifications.Type.WARNING).show();
            return;
        }
        if (cfg.getChartType() == null) {
            notifications.create("Vui lòng chọn loại biểu đồ").withType(Notifications.Type.WARNING).show();
            return;
        }
        viewNavigators.view(this, ChartConfigView.class)
                .withAfterNavigationHandler(e -> e.getView().initParams(cfg.getDataset().getId(), cfg.getChartType()))
                .navigate();
    }
}