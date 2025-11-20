package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.chartfragment.BarConfigFragment;
import com.company.chartconfig.view.chartfragment.PieConfigFragment;
import com.company.chartconfig.view.config.common.ChartConfigFragment; // Interface
import com.company.chartconfig.view.main.MainView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Route(value = "chart-config-view", layout = MainView.class)
@ViewController(id = "ChartConfigView")
@ViewDescriptor(path = "chart-config-view.xml")
public class ChartConfigView extends StandardView {

    // Map quản lý các Strategy (Fragment)
    private final Map<ChartType, ChartConfigFragment> fragmentMap = new HashMap<>();

    private UUID datasetId;
    private ChartType chartType;
    private Dataset dataset;
    private ChartConfig editingConfig;

    @Autowired private DataManager dataManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private Notifications notifications;
    @Autowired private ChartConfigService chartConfigService;
    @Autowired private ViewNavigators viewNavigators;

    // UI Components
    @ViewComponent private NativeLabel datasetNameLabel;
    @ViewComponent private NativeLabel chartTypeLabel;
    @ViewComponent private TypedTextField<Object> chartNameField;

    @ViewComponent private VerticalLayout fieldsList;
    @ViewComponent private TypedTextField<String> searchField; // Ô tìm kiếm

    @ViewComponent private VerticalLayout chartContainer;

    // Inject Fragments
    @ViewComponent private BarConfigFragment barConfig;
    @ViewComponent private PieConfigFragment pieConfig;

    @Subscribe
    public void onInit(final InitEvent event) {
        // 1. Đăng ký các fragment vào Map
        fragmentMap.put(ChartType.BAR, barConfig);
        fragmentMap.put(ChartType.PIE, pieConfig);

        // 2. Setup tìm kiếm (Enter hoặc Text ChangeMode.EAGER nếu muốn)
        searchField.addKeyPressListener(Key.ENTER, e -> doSearch());
        // Hoặc tìm kiếm ngay khi gõ (Bỏ comment dòng dưới nếu muốn)
        // searchField.addValueChangeListener(e -> doSearch());
    }

    // --- LOGIC TÌM KIẾM TRƯỜNG ---
    private void doSearch() {
        String keyword = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";

        fieldsList.getChildren().forEach(component -> {
            // Kiểm tra xem component có phải NativeLabel không
            if (component instanceof NativeLabel lbl) {
                String fieldName = lbl.getElement().getAttribute("data-field-name");
                boolean match = fieldName != null && fieldName.toLowerCase().contains(keyword);
                lbl.setVisible(match);
            }
        });
    }
    // =============================
    // INIT PARAMS (CREATE MODE)
    // =============================
    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null;
        this.datasetId = datasetId;
        this.chartType = chartType;
        loadDatasetCommon();
        switchFragment(chartType);
    }

    // =============================
    // INIT FROM EXISTING (EDIT MODE)
    // =============================
    public void initFromExisting(UUID chartConfigId) {
        this.editingConfig = dataManager.load(ChartConfig.class).id(chartConfigId).one();
        this.datasetId = editingConfig.getDataset().getId();
        this.chartType = editingConfig.getChartType();
        this.chartNameField.setValue(editingConfig.getName());

        loadDatasetCommon();
        switchFragment(chartType);

        // Load JSON vào Fragment hiện tại
        try {
            JsonNode node = objectMapper.readTree(editingConfig.getSettingsJson());
            getCurrentFragment().setConfigurationJson(node);
        } catch (Exception e) {
            notifications.create("Error loading settings: " + e.getMessage()).show();
        }
    }

    private void switchFragment(ChartType type) {
        // Ẩn tất cả, chỉ hiện cái cần thiết
        fragmentMap.values().forEach(f -> f.setVisible(false));

        ChartConfigFragment fragment = fragmentMap.get(type);
        if (fragment != null) {
            fragment.setVisible(true);
        }

        if (chartTypeLabel != null) chartTypeLabel.setText(type.name());
    }

    private ChartConfigFragment getCurrentFragment() {
        return fragmentMap.get(chartType);
    }

    private void loadDatasetCommon() {
        dataset = dataManager.load(Dataset.class).id(datasetId).one();
        datasetNameLabel.setText(dataset.getName());

        fieldsList.removeAll();

        // 1. Tạo danh sách chứa tên cột để truyền cho Fragment
        List<String> allColumns = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(dataset.getSchemaJson());
            if (root.isArray()) {
                for (JsonNode col : root) {
                    String name = col.path("name").asText();
                    String type = col.path("type").asText("string");

                    // Tạo UI kéo thả
                    addDraggableField(name, type);

                    // 2. Thêm vào danh sách cột
                    allColumns.add(name);
                }
            }
        } catch (Exception e) {
            notifications.create("Lỗi đọc Schema").show();
        }

        // 3. QUAN TRỌNG: Truyền danh sách cột vào TẤT CẢ Fragment
        // Nếu thiếu dòng này, Dialog sẽ bị null list và gây lỗi
        if (!fragmentMap.isEmpty()) {
            fragmentMap.values().forEach(frag -> frag.setAvailableFields(allColumns));
        }
    }

    @Subscribe(id = "previewBtn", subject = "clickListener")
    public void onPreviewBtnClick(ClickEvent<JmixButton> event) {
        ChartConfigFragment fragment = getCurrentFragment();

        if (!fragment.isValid()) {
            notifications.create("Vui lòng điền đầy đủ thông tin cấu hình").show();
            return;
        }

        String json = fragment.getConfigurationJson().toString();

        chartContainer.removeAll();
        try {
            Chart chart = chartConfigService.buildPreviewChart(dataset, chartType, json);
            chart.setWidthFull();
            chart.setHeight("400px");
            chartContainer.add(chart);
        } catch (Exception e) {
            notifications.create("Render error: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @Subscribe(id = "save", subject = "clickListener")
    public void onSaveClick(ClickEvent<JmixButton> event) {
        if (chartNameField.isEmpty()) {
            notifications.create("Nhập tên biểu đồ").show();
            return;
        }

        ChartConfigFragment fragment = getCurrentFragment();
        if (!fragment.isValid()) {
            notifications.create("Cấu hình không hợp lệ").show();
            return;
        }

        ChartConfig config = (editingConfig != null) ? editingConfig : dataManager.create(ChartConfig.class);
        config.setName(chartNameField.getValue().toString());
        config.setDataset(dataset);
        config.setChartType(chartType);
        config.setSettingsJson(fragment.getConfigurationJson().toString());

        dataManager.save(config);
        notifications.create("✅ Saved!").show();
        viewNavigators.view(this, ChartConfigListView.class).navigate();
    }

    private void addDraggableField(String name, String type) {
        NativeLabel lbl = new NativeLabel("# " + name); // Thêm icon text tượng trưng

        // Style giống Card nhỏ
        lbl.getStyle()
                .setDisplay(Style.Display.FLEX)
                .setWidth("100%")
                .setHeight("32px") // Cao hơn chút cho dễ nhìn
                .setBoxSizing(Style.BoxSizing.BORDER_BOX)
                .setAlignItems(Style.AlignItems.CENTER)
                .setPadding("0 12px")
                .setCursor("grab") // SỬA TYPO: crab -> grab
                .setMarginBottom("6px")
                .setBorderRadius("6px")
                .setBackgroundColor("#ffffff")
                .setBorder("1px solid #e0e0e0")
                .setFontSize("13px")
                .setFontWeight("500")
                .set("user-select", "none")
                .set("transition", "all 0.2s");

        // Hover effect
        lbl.getElement().addEventListener("mouseenter", e ->
                lbl.getStyle().setBorder("1px solid var(--lumo-primary-color)"));
        lbl.getElement().addEventListener("mouseleave", e ->
                lbl.getStyle().setBorder("1px solid #e0e0e0"));

        lbl.getElement().setAttribute("data-field-name", name);

        DragSource<NativeLabel> dragSource = DragSource.create(lbl);
        dragSource.setDragData(name);

        fieldsList.add(lbl);
    }
}