package com.company.chartconfig.view.dialog;

import com.company.chartconfig.view.common.MetricConfig;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.List;
import java.util.function.Consumer;

public class MetricConfigDialog extends Dialog {
    private final MetricConfig config;
    private final Consumer<MetricConfig> onSave;
    private TabSheet tabSheet;
    private ComboBox<String> columnField, aggregateField;
    private TextArea sqlField;

    public MetricConfigDialog(MetricConfig config, List<String> columns, Consumer<MetricConfig> onSave) {
        this.config = config;
        this.onSave = onSave;
        setHeaderTitle("Cấu hình Metric");
        setWidth("500px");

        tabSheet = new TabSheet();
        tabSheet.setWidthFull();

        // --- Tab Simple ---
        VerticalLayout simple = new VerticalLayout();
        columnField = new ComboBox<>("Column", columns);
        columnField.setWidthFull();

        aggregateField = new ComboBox<>("Aggregate");
        aggregateField.setWidthFull();
        aggregateField.setItems("SUM", "AVG", "COUNT", "MIN", "MAX", "COUNT_DISTINCT");

        simple.add(columnField, aggregateField);
        Tab simpleTab = tabSheet.add("Simple", simple);

        // --- Tab SQL ---
        VerticalLayout sql = new VerticalLayout();
        sqlField = new TextArea("Custom SQL");
        sqlField.setWidthFull();
        sqlField.setHeight("120px");
        sqlField.setPlaceholder("Ví dụ: COUNT(id) * 1.5");

        sql.add(sqlField);
        Tab sqlTab = tabSheet.add("Custom SQL", sql);

        add(tabSheet);

        // --- LOAD DATA (ĐÃ SỬA LỖI NULL POINTER) ---

        // 1. Load Column (Kiểm tra null)
        if (config.getColumn() != null) {
            columnField.setValue(config.getColumn());
        }

        // 2. Load Aggregate (Mặc định COUNT nếu null)
        String agg = config.getAggregate();
        aggregateField.setValue(agg != null ? agg : "COUNT");

        // 3. Load SQL (QUAN TRỌNG: Chuyển null thành chuỗi rỗng "")
        String sqlVal = config.getSqlExpression();
        sqlField.setValue(sqlVal != null ? sqlVal : "");

        // 4. Chọn Tab phù hợp
        if (config.getType() == MetricConfig.Type.CUSTOM_SQL) {
            tabSheet.setSelectedTab(sqlTab);
        } else {
            tabSheet.setSelectedTab(simpleTab);
        }

        // --- Footer Buttons ---
        Button saveBtn = new Button("Save", e -> save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button closeBtn = new Button("Close", e -> close());

        HorizontalLayout footer = new HorizontalLayout(closeBtn, saveBtn);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();
        getFooter().add(footer);
    }

    private void save() {
        // tabSheet.getSelectedIndex() trả về index của tab đang active (0 là Simple, 1 là SQL)
        int selectedIndex = tabSheet.getSelectedIndex();

        if (selectedIndex == 0) {
            // Đang ở Tab Simple
            config.setType(MetricConfig.Type.SIMPLE);
            config.setColumn(columnField.getValue());

            // Nếu người dùng lỡ xóa trống aggregate, fallback về COUNT
            String agg = aggregateField.getValue();
            config.setAggregate(agg != null ? agg : "COUNT");
        } else {
            // Đang ở Tab SQL
            config.setType(MetricConfig.Type.CUSTOM_SQL);
            config.setSqlExpression(sqlField.getValue());
        }

        config.updateLabel(); // Cập nhật text hiển thị trên Chip
        onSave.accept(config);
        close();
    }
}