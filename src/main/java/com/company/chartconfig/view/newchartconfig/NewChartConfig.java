package com.company.chartconfig.view.newchartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.view.chartconfig.ChartConfigView;
import com.company.chartconfig.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image; // [MỚI] Import Image
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent; // [MỚI] Để căn chỉnh
import com.vaadin.flow.component.orderedlayout.VerticalLayout; // [MỚI] Để căn chỉnh
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
        // [SỬA] Thêm trường "image" vào data
        List<KeyValueEntity> items = List.of(
                create("BAR", "icons/chart/bar.png", "Bar Chart", "Comparison"),
                create("PIE", "icons/chart/pie.png", "Pie Chart", "Distribution"),
                create("LINE", "icons/chart/line.png", "Line Chart", "Trend"),
                create("AREA", "icons/chart/area.png", "Area Chart", "Volume"),
                create("GAUGE", "icons/chart/gauge.png", "Gauge Chart", "Single Value"),
                create("SCATTER", "icons/chart/scatter.png", "Scatter", "Correlation")
        );

        chartTypeContainer.removeAll();

        for (KeyValueEntity item : items) {
            Component card = createClickableCard(item);
            chartTypeContainer.add(card);
        }
    }

    // [SỬA] Hàm create nhận path ảnh thay vì icon
    private KeyValueEntity create(String code, String imagePath, String name, String tags) {
        KeyValueEntity kv = dataManager.create(KeyValueEntity.class);
        kv.setValue("code", code);
        kv.setValue("image", imagePath);
        kv.setValue("name", name);
        kv.setValue("tags", tags);
        return kv;
    }

    private Component createClickableCard(KeyValueEntity item) {
        String code = item.getValue("code");
        String imagePath = item.getValue("image"); // Lấy đường dẫn ảnh
        String name = item.getValue("name");
        String tags = item.getValue("tags");

        Div card = new Div();
        card.getStyle()
                .set("background-color", "#ffffff")
                .set("border", "1px solid #e2e8f0") // Viền mỏng hơn cho đẹp
                .set("border-radius", "12px")
                .set("padding", "16px")
                .set("cursor", "pointer")
                .set("transition", "all 0.2s ease-in-out")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "space-between") // Dãn cách đều
                .set("height", "220px") // Tăng chiều cao để chứa ảnh
                .set("box-sizing", "border-box")
                .set("overflow", "hidden"); // Tránh ảnh tràn ra ngoài

        // [MỚI] Phần chứa ảnh (Image Container)
        Div imageContainer = new Div();
        imageContainer.getStyle()
                .set("width", "100%")
                .set("height", "120px") // Chiều cao cố định cho vùng ảnh
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("background-color", "#f8fafc") // Màu nền nhẹ cho ảnh
                .set("border-radius", "8px")
                .set("margin-bottom", "12px");

        // [MỚI] Tạo component Image
        Image image = new Image(imagePath, name);
        image.setWidth("100%");
        image.setHeight("100%");
        image.getStyle().set("object-fit", "contain"); // Giữ tỷ lệ ảnh, không bị méo
        imageContainer.add(image);

        // Phần Text
        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span title = new Span(name);
        title.getStyle()
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("color", "var(--lumo-header-text-color)");

        Span tagSpan = new Span(tags);
        tagSpan.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "4px");

        textLayout.add(title, tagSpan);

        card.add(imageContainer, textLayout);

        // Sự kiện Click & Hover (Giữ nguyên logic highlight)
        card.addClickListener(e -> {
            selectCode(code);
            highlightSelected(card);
        });

        card.getElement().addEventListener("mouseenter", e -> {
            if (selectedCardElement != card) {
                card.getStyle()
                        .set("transform", "translateY(-5px)")
                        .set("box-shadow", "0 10px 25px -5px rgba(0, 0, 0, 0.1)")
                        .set("border-color", "var(--lumo-primary-color-50pct)");
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

    // [SỬA] Hàm highlight chỉ cần đổi style Card, không cần đổi màu icon nữa
    private void highlightSelected(Div clickedCard) {
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
                .set("box-shadow", "0 0 0 2px var(--lumo-primary-color)") // Viền đậm hơn khi chọn
                .set("transform", "scale(1.02)");
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