package lsfusion.client.form.filter.user.controller;

import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.ClientFilter;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.filter.user.view.FilterView;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.object.table.grid.user.toolbar.view.ToolbarGridButton;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public abstract class FilterController {
    private ClientFilter filter;

    private final FilterView view;

    private final TableController logicsSupplier;
    private final ToolbarGridButton toolbarButton;
    
    public FilterController(TableController logicsSupplier, ClientFilter filter) {
        this.logicsSupplier = logicsSupplier;
        this.filter = filter;

        view = new FilterView(this, filter);

        toolbarButton = new ToolbarGridButton(FilterView.FILTER_ICON_PATH, getString("form.queries.filter.tools.show"));
        toolbarButton.addActionListener(ae -> {
            view.toggleToolsVisible();
            toolbarButton.setToolTipText(view.isToolsVisible() ? getString("form.queries.filter.tools.hide") : getString("form.queries.filter.tools.show"));
            toolbarButton.showBackground(view.isToolsVisible());
        });
    }

    public JButton getToolbarButton() {
        return toolbarButton;
    }

    public FilterView getView() {
        return view;
    }
    
    public void addView(ClientFormLayout layout) {
        layout.addBaseComponent(filter, getView());
    }

    public TableController getLogicsSupplier() {
        return logicsSupplier;
    }

    public void removeAllConditions(boolean focusFirstComponent) {
        applyFilters(Collections.emptyList(), focusFirstComponent);
    }
    
    public ClientPropertyFilter getNewCondition(ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        ClientPropertyDraw filterProperty = propertyDraw;
        ClientGroupObjectValue filterColumnKey = columnKey;

        if (filterProperty == null) {
            filterProperty = logicsSupplier.getSelectedProperty();
            if (filterProperty != null) {
                filterColumnKey = logicsSupplier.getSelectedColumn();
            }
        }

        if (filterProperty == null) {
            return null;
        }

        return new ClientPropertyFilter(logicsSupplier.getSelectedGroupObject(), filterProperty, filterColumnKey, null, filterProperty.getDefaultCompare());
    }

    public void quickEditFilter(KeyEvent initFilterKeyEvent, ClientPropertyDraw propertyDraw, ClientGroupObjectValue columnKey) {
        view.addCondition(propertyDraw, columnKey, initFilterKeyEvent, true, true);
    }

    public abstract void applyFilters(List<ClientPropertyFilter> conditions, boolean focusFirstComponent);

    public void setVisible(boolean visible) {
        getView().setVisible(visible);
    }

    public void update() {
        view.update();
    }
}
