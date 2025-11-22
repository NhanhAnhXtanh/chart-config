package com.company.chartconfig.view.chartconfig;

import com.company.chartconfig.entity.ChartConfig;
import com.company.chartconfig.entity.Dataset;
import com.company.chartconfig.enums.ChartType;
import com.company.chartconfig.model.FieldItem;
import com.company.chartconfig.service.ChartConfigService;
import com.company.chartconfig.view.config.ChartFragmentRegistry;
import com.company.chartconfig.view.config.common.ChartConfigFragment;
import com.company.chartconfig.view.chartfragment.BarConfigFragment;
import com.company.chartconfig.view.chartfragment.LineConfigFragment;
import com.company.chartconfig.view.chartfragment.PieConfigFragment;
import com.company.chartconfig.view.config.common.ChartConfigFragment; // Interface
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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.core.DataManager;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "chart-config-view", layout = MainView.class)
@ViewController(id = "ChartConfigView")
@ViewDescriptor(path = "chart-config-view.xml")
public class ChartConfigView extends StandardView {

    @Autowired
    private DataManager dataManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Notifications notifications;
    @Autowired
    private ChartConfigService chartConfigService;
    @Autowired
    private ViewNavigators viewNavigators;
    @Autowired private Fragments fragments;
    @Autowired private ChartFragmentRegistry fragmentRegistry;

    @ViewComponent
    private NativeLabel datasetNameLabel;
    @ViewComponent
    private TypedTextField<Object> chartNameField;
    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private ListBox<FieldItem> fieldsList;
    @ViewComponent
    private Tabs chartTypeTabs;
    @ViewComponent private VerticalLayout fragmentContainer;
    @ViewComponent
    private VerticalLayout chartContainer;
    @ViewComponent private JmixButton save;

    private UUID datasetId;
    private ChartType currentChartType;
    private Dataset dataset;
    private ChartConfig editingConfig;

    // Cache danh sách trường và metadata
    private final List<FieldItem> allFieldItems = new ArrayList<>();
    private final List<String> allColumnNames = new ArrayList<>();
    private final Map<String, String> allColumnTypes = new HashMap<>(); // (FIX) Map lưu kiểu

    private final Map<ChartType, ChartConfigFragment> fragmentCache = new HashMap<>();
    private final Map<Tab, ChartType> tabMap = new HashMap<>();
    @ViewComponent
    private LineConfigFragment lineConfig;

    @Subscribe
    public void onInit(final InitEvent event) {
        setupDynamicTabs();
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> doSearch());
    }

    private void setupDynamicTabs() {
        chartTypeTabs.removeAll();
        tabMap.clear();
        for (ChartType type : ChartType.values()) {
            Tab tab = new Tab();
            Icon icon = type.getIcon().create();
            icon.setSize("18px");
            Span label = new Span(type.name());
            label.getStyle().set("font-size", "11px");
            tab.add(icon, label);
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

        ChartConfigFragment oldFrag = getCurrentFragment();
        String oldDim = (oldFrag != null) ? oldFrag.getMainDimension() : null;
        String oldMet = (oldFrag != null) ? oldFrag.getMainMetric() : null;

        this.currentChartType = newType;
        ChartConfigFragment newFrag = getOrCreateFragment(newType);

        fragmentContainer.removeAll();
        if (newFrag instanceof Component comp) {
            fragmentContainer.add(comp);
        }

        // (FIX QUAN TRỌNG) Truyền cả danh sách tên và Map Type
        newFrag.setAvailableFields(allColumnNames);
        newFrag.setColumnTypes(allColumnTypes);

        if (oldDim != null && newFrag.getMainDimension() == null) newFrag.setMainDimension(oldDim);
        if (oldMet != null && newFrag.getMainMetric() == null) newFrag.setMainMetric(oldMet);
    }

    private ChartConfigFragment getOrCreateFragment(ChartType type) {
        if (fragmentCache.containsKey(type)) return fragmentCache.get(type);

        Class<? extends Fragment<?>> clazz = fragmentRegistry.getFragmentClass(type);
        if (clazz == null) {
            notifications.create("Chưa hỗ trợ loại: " + type).show();
            return null;
        }
        Fragment<?> fragment = fragments.create(this, clazz);
        if (fragment instanceof ChartConfigFragment configFrag) {
            fragmentCache.put(type, configFrag);
            return configFrag;
        }
        throw new IllegalStateException("Fragment must implement ChartConfigFragment");
    }

    private ChartConfigFragment getCurrentFragment() {
        return fragmentCache.get(currentChartType);
    }

    public void initParams(UUID datasetId, ChartType chartType) {
        this.editingConfig = null;
        this.datasetId = datasetId;
        loadDatasetCommon();
        selectTabByType(chartType);
    }

    public void initFromExisting(UUID chartConfigId) {
        this.editingConfig = dataManager.load(ChartConfig.class).id(chartConfigId).one();
        this.datasetId = editingConfig.getDataset().getId();
        this.chartNameField.setValue(editingConfig.getName());
        loadDatasetCommon();
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
        if (currentChartType != type) {
            switchChartType(type);
        }
    }

    private void loadDatasetCommon() {
        dataset = dataManager.load(Dataset.class).id(datasetId).one();
        datasetNameLabel.setText(dataset.getName());

        allFieldItems.clear();
        allColumnNames.clear();
        allColumnTypes.clear();

        try {
            JsonNode root = objectMapper.readTree(dataset.getSchemaJson());
            if (root.isArray()) {
                for (JsonNode col : root) {
                    String name = col.path("name").asText();
                    String type = col.path("type").asText("string");

                    allFieldItems.add(new FieldItem(name, type));
                    allColumnNames.add(name);
                    allColumnTypes.put(name, type); // (FIX) Lưu metadata
                }
            }
        } catch (Exception e) {
            notifications.create("Lỗi đọc schema: " + e.getMessage()).show();
        }

        fieldsList.setItems(allFieldItems);
        fieldsList.setRenderer(new ComponentRenderer<>(item -> {
            NativeLabel lbl = new NativeLabel(getIconForType(item.getType()) + " " + item.getName());
            lbl.getStyle()
                    .setDisplay(Style.Display.FLEX).setWidth("100%")
                    .setPadding("6px 12px").setBoxSizing(Style.BoxSizing.BORDER_BOX)
                    .setCursor("grab").setMarginBottom("4px")
                    .setBorder("1px solid #e0e0e0").setBorderRadius("6px")
                    .setBackgroundColor("white").setFontSize("13px").setFontWeight("500");

            lbl.getElement().setAttribute("data-field-name", item.getName());
            DragSource.create(lbl).setDragData(item.getName());
            return lbl;
        }));

        // Cập nhật dữ liệu cho các fragment đã load
        fragmentCache.values().forEach(f -> {
            f.setAvailableFields(allColumnNames);
            f.setColumnTypes(allColumnTypes);
        });
    }

    private String getIconForType(String type) {
        if (type.contains("date")) return "📅";
        if (type.contains("number")) return "#";
        return "Aa";
    }

    private void doSearch() {
        String keyword = searchField.getValue() != null ? searchField.getValue().toLowerCase().trim() : "";
        fieldsList.setItems(keyword.isEmpty() ? allFieldItems :
                allFieldItems.stream().filter(i -> i.getName().toLowerCase().contains(keyword)).collect(Collectors.toList()));
    }

    @Subscribe(id = "previewBtn", subject = "clickListener")
    public void onPreviewBtnClick(final ClickEvent<JmixButton> event) {
        ChartConfigFragment frag = getCurrentFragment();
        if (frag == null || !frag.isValid()) {
            notifications.create("Cấu hình chưa hợp lệ").show();
            return;
        }
        chartContainer.removeAll();
        try {
            Chart chart = chartConfigService.buildPreviewChart(dataset, currentChartType, frag.getConfigurationJson().toString());
            chart.setWidthFull(); chart.setHeight("100%");
            chartContainer.add(chart);
        } catch (Exception e) {
            notifications.create("Render error: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @Subscribe(id = "save", subject = "clickListener")
    public void onSaveClick(final ClickEvent<JmixButton> event) {
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
        config.setName(chartNameField.getValue());
        config.setDataset(dataset);
        config.setChartType(currentChartType);
        config.setSettingsJson(fragment.getConfigurationJson().toString());

        dataManager.save(config);
        notifications.create("Đã lưu!").show();
        viewNavigators.view(this, ChartConfigListView.class).navigate(); // Đổi class view list của bạn
    }
}