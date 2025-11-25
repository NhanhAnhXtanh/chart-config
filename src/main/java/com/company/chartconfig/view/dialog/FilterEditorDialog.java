package com.company.chartconfig.view.dialog;

import com.company.chartconfig.utils.FilterRule;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class FilterEditorDialog extends Dialog {

    private final FilterRule rule;
    private final Consumer<FilterRule> onSave;
    private final String type;
    private final boolean isDate;

    // UI Components
    private ComboBox<String> operatorField;
    private TextField valueField;
    private DatePicker startDateField;
    private DatePicker endDateField;
    private Div inputContainer;

    public FilterEditorDialog(FilterRule existingRule, String column, String rawType, Consumer<FilterRule> onSave) {
        this.rule = (existingRule != null) ? existingRule : new FilterRule(column, "=", "");
        this.onSave = onSave;
        this.type = (rawType == null ? "string" : rawType).toLowerCase();
        this.isDate = type.contains("date") || type.contains("time");

        setHeaderTitle("Filter: " + column);

        initUI();
        loadData();
    }

    private void initUI() {
        // 1. Layout chính
        VerticalLayout layout = new VerticalLayout();
        HorizontalLayout formLayout = new HorizontalLayout();
        formLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        // 2. Operator Field
        operatorField = new ComboBox<>("Operator");
        operatorField.setWidth("140px");
        setupOperators();

        // 3. Container chứa input (Text hoặc Date)
        inputContainer = new Div();
        inputContainer.getStyle().set("display", "flex").set("gap", "10px").set("align-items", "baseline");

        // 4. Khởi tạo các input components (chưa add vào layout vội)
        valueField = new TextField("Value");
        startDateField = new DatePicker("From Date");
        endDateField = new DatePicker("To Date");

        // 5. Sự kiện thay đổi Operator -> Đổi input tương ứng
        operatorField.addValueChangeListener(e -> updateInputVisibility());

        // 6. Nút bấm
        Button saveBtn = new Button("Apply", e -> doSave());
        saveBtn.addThemeName("primary");
        Button cancelBtn = new Button("Cancel", e -> this.close());

        // Add to layouts
        formLayout.add(operatorField, inputContainer);
        layout.add(formLayout);

        this.add(layout);
        this.getFooter().add(cancelBtn, saveBtn);
    }

    private void setupOperators() {
        List<String> ops;
        if (isDate) {
            ops = Arrays.asList("BETWEEN", "=", ">=", "<=");
        } else if (type.contains("number") || type.contains("int") || type.contains("double") || type.contains("decimal")) {
            ops = Arrays.asList("=", "!=", ">", "<", ">=", "<=", "IN");
        } else {
            ops = Arrays.asList("=", "!=", "LIKE", "IN");
        }
        operatorField.setItems(ops);
    }

    private void loadData() {
        // Set operator hiện tại
        String currentOp = rule.getOperator();
        if (currentOp != null && !currentOp.isEmpty()) {
            operatorField.setValue(currentOp);
        } else {
            operatorField.setValue(isDate ? "BETWEEN" : "=");
        }

        // Parse giá trị cũ vào input
        String val = rule.getValue();
        if (val == null) val = "";

        if (isDate) {
            try {
                String[] parts = val.split("\\|");
                if (parts.length > 0 && !parts[0].equals("null") && !parts[0].isEmpty())
                    startDateField.setValue(LocalDate.parse(parts[0]));
                if (parts.length > 1 && !parts[1].equals("null") && !parts[1].isEmpty())
                    endDateField.setValue(LocalDate.parse(parts[1]));
            } catch (Exception ignored) {}
        } else {
            valueField.setValue(val);
        }

        // Trigger update UI lần đầu
        updateInputVisibility();
    }

    private void updateInputVisibility() {
        inputContainer.removeAll();
        String op = operatorField.getValue();

        if (isDate) {
            inputContainer.add(startDateField);
            if ("BETWEEN".equals(op)) {
                inputContainer.add(endDateField);
            }
        } else {
            inputContainer.add(valueField);
        }
    }

    private void doSave() {
        rule.setOperator(operatorField.getValue());

        if (isDate) {
            String v1 = startDateField.getValue() != null ? startDateField.getValue().toString() : "null";
            String v2 = endDateField.getValue() != null ? endDateField.getValue().toString() : "null";

            if ("BETWEEN".equals(operatorField.getValue())) {
                rule.setValue(v1 + "|" + v2);
            } else {
                rule.setValue(v1);
            }
        } else {
            rule.setValue(valueField.getValue());
        }

        onSave.accept(rule);
        this.close();
    }
}