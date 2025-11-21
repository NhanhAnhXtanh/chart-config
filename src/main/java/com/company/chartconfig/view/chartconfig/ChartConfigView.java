package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.model.FieldItem;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.config.ChartFragmentRegistry;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.company.chartconfig.view.main.MainView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.core.DataManager;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "chart-config-view", layout = MainView.class)
@ViewController(id = "ChartConfigView")
@ViewDescriptor(path = "chart-config-view.xml")
public class ChartConfigView extends StandardView {

    // --- INJECT BEANS ---
    @Autowired private DataManager dataManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private Notifications notifications;
    @Autowired private ChartConfigService chartConfigService;
    @Autowired private ViewNavigators viewNavigators;
    @Autowired private Fragments fragments; // Để tạo Fragment động
    @Autowired private ChartFragmentRegistry fragmentRegistry; // Map Enum -> Class

    // --- UI COMPONENTS ---
    @ViewComponent private NativeLabel datasetNameLabel;
    @ViewComponent private TypedTextField<Object> chartNameField;
    @ViewComponent private TypedTextField<String> searchField;

    @ViewComponent private ListBox<FieldItem> fieldsList; // ListBox kéo thả
    @ViewComponent private Tabs chartTypeTabs; // Tabs động
    @ViewComponent private VerticalLayout fragmentContainer; // Chỗ chứa Fragment
    @ViewComponent private VerticalLayout chartContainer; // Chỗ chứa Chart Preview

    // --- STATE ---
    private UUID datasetId;
    private ChartType currentChartType;
    private Dataset dataset;
    private ChartConfig editingConfig;

    private final List<FieldItem> allFieldItems = new ArrayList<>();
    private final List<String> allColumnNames = new ArrayList<>();

    // Cache Fragment để không phải tạo lại
    private final Map<ChartType, ChartConfigFragment> fragmentCache = new HashMap<>();
    // Map Tab -> Enum
    private final Map<Tab, ChartType> tabMap = new HashMap<>();

    @Subscribe
    public void onInit(final InitEvent event) {
        // 1. Setup Tabs động từ Enum ChartType
        setupDynamicTabs();

        // 2. Setup Search
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> doSearch());
    }

    // ==========================================================
    // DYNAMIC TABS & FRAGMENT SWITCHING
    // ==========================================================
    private void setupDynamicTabs() {
        chartTypeTabs.removeAll();
        tabMap.clear();

        for (ChartType type : ChartType.values()) {
            // Tạo Tab
            Tab tab = new Tab();

            // Tạo Icon
            Icon icon = type.getIcon().create();
            icon.setSize("18px");

            // Tạo Label
            Span label = new Span(type.name());
            label.getStyle().set("font-size", "11px");

            // Add vào Tab
            tab.add(icon, label);
            // Style: Icon nằm trên chữ
            tab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);

            chartTypeTabs.add(tab);
            tabMap.put(tab, type);
        }

        chartTypeTabs.addSelectedChangeListener(e -> {
            ChartType newType = tabMap.get(e.getSelectedTab());
            switchChartType(newType);
        });
    }

    private void switchChartType(ChartType newType) {
        if (newType == null || newType == this.currentChartType) return;

        // A. Data Migration (Giữ lại dữ liệu khi đổi chart)
        ChartConfigFragment oldFrag = getCurrentFragment();
        String oldDim = (oldFrag != null) ? oldFrag.getMainDimension() : null;
        String oldMet = (oldFrag != null) ? oldFrag.getMainMetric() : null;

        this.currentChartType = newType;

        // B. Get/Create New Fragment
        ChartConfigFragment newFrag = getOrCreateFragment(newType);

        // C. Render Fragment
        fragmentContainer.removeAll();
        if (newFrag instanceof Component comp) {
            fragmentContainer.add(comp);
        }

        // D. Apply Migration Data
        // QUAN TRỌNG: Phải set available fields để Dialog Metric có dữ liệu
        newFrag.setAvailableFields(allColumnNames);

        if (oldDim != null && newFrag.getMainDimension() == null) newFrag.setMainDimension(oldDim);
        if (oldMet != null && newFrag.getMainMetric() == null) newFrag.setMainMetric(oldMet);
    }

    private ChartConfigFragment getOrCreateFragment(ChartType type) {
        if (fragmentCache.containsKey(type)) return fragmentCache.get(type);

        Class<? extends io.jmix.flowui.fragment.Fragment<?>> clazz = fragmentRegistry.getFragmentClass(type);
        if (clazz == null) {
            notifications.create("Chưa hỗ trợ loại: " + type).show();
            return null;
        }

        // Tạo Fragment động thông qua Bean Fragments
        io.jmix.flowui.fragment.Fragment<?> fragment = fragments.create(this, clazz);

        if (fragment instanceof ChartConfigFragment configFrag) {
            // Nạp dữ liệu cột ngay khi tạo mới
            configFrag.setAvailableFields(allColumnNames);

            fragmentCache.put(type, configFrag);
            return configFrag;
        }
        throw new IllegalStateException("Fragment must implement ChartConfigFragment");
    }

    private ChartConfigFragment getCurrentFragment() {
        return fragmentCache.get(currentChartType);
    }

    // ==========================================================
    // INIT DATA
    // ==========================================================
    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null;
        this.datasetId = datasetId;
        loadDatasetCommon();

        // Chọn tab
        selectTabByType(chartType);
    }

    public void initFromExisting(UUID chartConfigId) {
        this.editingConfig = dataManager.load(ChartConfig.class).id(chartConfigId).one();
        this.datasetId = editingConfig.getDataset().getId();
        this.chartNameField.setValue(editingConfig.getName());

        loadDatasetCommon();

        // Chọn tab & Load JSON
        selectTabByType(editingConfig.getChartType());

        try {
            JsonNode node = objectMapper.readTree(editingConfig.getSettingsJson());
            if (getCurrentFragment() != null) {
                getCurrentFragment().setConfigurationJson(node);
            }
        } catch (Exception e) {
            notifications.create("Error loading config").show();
        }
    }

    private void selectTabByType(ChartType type) {
        tabMap.forEach((tab, t) -> {
            if (t == type) chartTypeTabs.setSelectedTab(tab);
        });
        // Nếu tab không đổi (vẫn là tab mặc định), listener ko chạy -> gọi thủ công
        if (currentChartType != type) {
            switchChartType(type);
        }
    }

    private void loadDatasetCommon() {
        dataset = dataManager.load(Dataset.class).id(datasetId).one();
        datasetNameLabel.setText(dataset.getName());

        allFieldItems.clear();
        allColumnNames.clear();

        try {
            JsonNode root = objectMapper.readTree(dataset.getSchemaJson());
            if (root.isArray()) {
                for (JsonNode col : root) {
                    String name = col.path("name").asText();
                    String type = col.path("type").asText("string");
                    allFieldItems.add(new FieldItem(name, type));
                    allColumnNames.add(name);
                }
            }
        } catch (Exception e) {}

        // Setup ListBox Renderer (Drag Source)
        fieldsList.setItems(allFieldItems);
        fieldsList.setRenderer(new ComponentRenderer<>(item -> {
            NativeLabel lbl = new NativeLabel("# " + item.getName());
            lbl.getStyle()
                    .setDisplay(Style.Display.FLEX).setWidth("100%")
                    .setPadding("6px 12px").setBoxSizing(Style.BoxSizing.BORDER_BOX)
                    .setCursor("grab").setMarginBottom("4px")
                    .setBorder("1px solid #e0e0e0").setBorderRadius("6px")
                    .setBackgroundColor("white").setFontSize("13px").setFontWeight("500");

            // Metadata for DropZoneUtils
            lbl.getElement().setAttribute("data-field-name", item.getName());
            DragSource.create(lbl).setDragData(item.getName());
            return lbl;
        }));

        // Cập nhật columns cho các fragment đã cache (nếu có)
        fragmentCache.values().forEach(f -> f.setAvailableFields(allColumnNames));
    }

    private void doSearch() {
        String keyword = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";
        if (keyword.isEmpty()) {
            fieldsList.setItems(allFieldItems);
        } else {
            List<FieldItem> filtered = allFieldItems.stream()
                    .filter(i -> i.getName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
            fieldsList.setItems(filtered);
        }
    }

    // ==========================================================
    // ACTIONS
    // ==========================================================
    @Subscribe("previewBtn")
    public void onPreview() {
        ChartConfigFragment frag = getCurrentFragment();
        if (frag == null || !frag.isValid()) {
            notifications.create("Cấu hình chưa hợp lệ").show();
            return;
        }
        String json = frag.getConfigurationJson().toString();

        chartContainer.removeAll();
        try {
            Chart chart = chartConfigService.buildPreviewChart(dataset, currentChartType, json);
            chart.setWidthFull();
            chart.setHeight("100%");
            chartContainer.add(chart);
        } catch (Exception e) {
            notifications.create("Render error: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @Subscribe("save")
    public void onSave() {
        if (chartNameField.isEmpty()) {
            notifications.create("Nhập tên biểu đồ").show();
            return;
        }
        ChartConfigFragment frag = getCurrentFragment();
        if (frag == null || !frag.isValid()) {
            notifications.create("Cấu hình chưa hợp lệ").show();
            return;
        }

        ChartConfig config = (editingConfig != null) ? editingConfig : dataManager.create(ChartConfig.class);
        config.setName(chartNameField.getValue().toString());
        config.setDataset(dataset);
        config.setChartType(currentChartType);
        config.setSettingsJson(frag.getConfigurationJson().toString());

        dataManager.save(config);
        notifications.create("Đã lưu!").show();
        viewNavigators.view(this, ChartConfigListView.class).navigate();
    }
}