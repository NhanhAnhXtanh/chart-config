package com.company.chartconfig.view.newchartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.view.chartconfig.ChartConfigView;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

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
    private KeyValueCollectionContainer chartTypesDc;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private Notifications notifications;

    @Autowired
    private ViewNavigators viewNavigators;

    private Div selectedCardWrapper = null;

    @Subscribe
    public void onInit(InitEvent event) {
        chartConfigDc.setItem(dataManager.create(ChartConfig.class));
        loadChartTypes();
    }

    private void loadChartTypes() {
        List<KeyValueEntity> items = List.of(
                create("BAR", VaadinIcon.BAR_CHART, "SALES", "MONTHLY, IMPORTANT"),
                create("PIE", VaadinIcon.PIE_CHART, "DISTRIBUTION", "PERCENT")
        );

        chartTypesDc.setItems(items);
    }

    private KeyValueEntity create(String code, VaadinIcon icon, String category, String tags) {
        KeyValueEntity kv = dataManager.create(KeyValueEntity.class);
        kv.setValue("code", code);
        kv.setValue("icon", icon);
        kv.setValue("category", category);
        kv.setValue("tags", tags);
        return kv;
    }

    @Supply(to = "chartTypeGrid", subject = "renderer")
    public ComponentRenderer<Component, KeyValueEntity> renderer() {
        return new ComponentRenderer<>(this::createClickableCard);
    }

    private Component createClickableCard(KeyValueEntity item) {

        String code = item.getValue("code");
        VaadinIcon iconEnum = item.getValue("icon");
        String category = item.getValue("category");
        String tags = item.getValue("tags");

        JmixCard card = uiComponents.create(JmixCard.class);
        card.getStyle()
                .set("width", "165px")
                .set("height", "180px")
                .set("padding", "16px")
                .set("border-radius", "14px")
                .set("background", "white")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.05)")
                .set("transition", "all .20s ease")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("gap", "10px")
                .set("cursor", "pointer");

        // Hover effect
        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle().set("box-shadow", "0 4px 14px rgba(0,0,0,0.10)")
        );
        card.getElement().addEventListener("mouseleave", e -> {
            if (selectedCardWrapper == null || selectedCardWrapper.getChildren().findFirst().get() != card) {
                card.getStyle().set("box-shadow", "0 2px 6px rgba(0,0,0,0.05)");
            }
        });

        // Icon
        Icon icon = iconEnum.create();
        icon.setSize("42px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");
        card.setHeader(icon);

        // Title
        Span title = new Span(code);
        title.getStyle()
                .set("font-size", "15px")
                .set("font-weight", "600")
                .set("margin-top", "6px");
        card.setTitle(title);

        // Tags
        Span tagBadge = new Span(tags);
        tagBadge.getElement().getThemeList().add("badge contrast");
        card.add(tagBadge);

        Div wrapper = new Div(card);
        wrapper.getStyle().set("cursor", "pointer");

        wrapper.addClickListener(e -> {
            selectCode(code);
            highlightSelected(wrapper);
        });

        return wrapper;
    }


    private void highlightSelected(Div clicked) {

        if (selectedCardWrapper != null) {
            JmixCard prev = (JmixCard) selectedCardWrapper.getChildren().findFirst().get();
            prev.getStyle()
                    .set("background", "white")
                    .set("box-shadow", "0 2px 6px rgba(0,0,0,0.05)")
                    .set("border", "none");
        }

        selectedCardWrapper = clicked;
        JmixCard card = (JmixCard) clicked.getChildren().findFirst().get();

        card.getStyle()
                .set("background", "#eef5ff") // nền xanh nhẹ
                .set("border", "2px solid var(--lumo-primary-color)")
                .set("box-shadow", "0 6px 18px rgba(0,0,0,0.12)");
    }


    private void selectCode(String code) {
        chartConfigDc.getItem().setChartType(ChartType.valueOf(code));
        notifications.create("Selected Chart Type: " + code).show();
    }

    @Subscribe("createBtn")
    public void onCreateBtnClick(ClickEvent<JmixButton> event) {

        ChartConfig cfg = chartConfigDc.getItem();

        if (cfg.getDataset() == null) {
            notifications.create("Please select dataset").show();
            return;
        }

        if (cfg.getChartType() == null) {
            notifications.create("Please select chart type").show();
            return;
        }

        viewNavigators
                .view(this, ChartConfigView.class)
                .withAfterNavigationHandler(e -> {
                    ChartConfigView v = e.getView();
                    v.initParams(cfg.getDataset().getId(), cfg.getChartType());
                })
                .navigate();
    }
}
