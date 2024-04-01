package lsfusion.client.form.object.table.controller;

import lsfusion.base.Pair;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.ClientFormLayout;
import lsfusion.client.form.filter.user.controller.FilterController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientObject;
import lsfusion.client.form.object.panel.controller.PanelController;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.client.form.object.table.view.ToolbarView;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.KeyStrokes;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractTableController implements TableController {
    protected final ClientFormController formController;
    protected final ClientFormLayout formLayout;

    protected PanelController panel;
    protected final ToolbarView toolbarView;
    protected FilterController filter;

    public AbstractTableController(ClientFormController formController, ClientFormLayout formLayout, ClientToolbar toolbar) {
        this.formController = formController;
        this.formLayout = formLayout;

        if (toolbar == null || !toolbar.visible) {
            toolbarView = null;
        } else {
            toolbarView = new ToolbarView(toolbar);
            formLayout.addBaseComponent(toolbar, toolbarView);
        }
    }

    @Override
    public ClientFormController getFormController() {
        return formController;
    }
    
    public PanelController getPanelController() {
        return panel;
    }

    @Override
    public List<ClientObject> getObjects() {
        return formController.form.getObjects();
    }
    
    public void initFilterButtons() {
        addToToolbar(filter.getToolbarButton());
    }

    public void addToToolbar(Component component) {
        if (toolbarView != null) {
            toolbarView.addComponent(component);
        }
    }
    
    public void addToolbarSeparator() {
        if (toolbarView != null) {
            toolbarView.addSeparator();
        }
    }
    
    public void addFilterBindings(ClientGroupObject groupObject) {
        addFilterBinding(groupObject, new KeyInputEvent(KeyStrokes.getFilterKeyStroke(0)), this::addFilter);
        addFilterBinding(groupObject, new KeyInputEvent(KeyStrokes.getFilterKeyStroke(InputEvent.ALT_DOWN_MASK)), this::replaceFilter);
        addFilterBinding(groupObject, new KeyInputEvent(KeyStrokes.getFilterKeyStroke(InputEvent.SHIFT_DOWN_MASK)), this::resetFilers);
        addFilterBinding(groupObject, null, new KeyInputEvent(KeyStrokes.getRemoveFiltersKeyStroke()), this::resetFilers);
    }

    public void addFilterBinding(ClientGroupObject groupObject, KeyInputEvent inputEvent, Callable<Boolean> pressedCall) {
        addFilterBinding(groupObject, BindingMode.NO, inputEvent, pressedCall);
    }

    public void addFilterBinding(ClientGroupObject groupObject, BindingMode bindPreview, KeyInputEvent inputEvent, Callable<Boolean> pressedCall) {
        ClientFormController.Binding binding = new ClientFormController.Binding(groupObject, 0) {
            @Override
            public boolean pressed(InputEvent ke) {
                try {
                    return pressedCall.call();
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public boolean showing() {
                return true;
            }
        };
        binding.bindPreview = bindPreview;
        binding.bindGroup = BindingMode.ONLY;
        binding.bindEditing = BindingMode.NO;
        formController.addBinding(inputEvent, binding);
    }

    public Boolean addFilter() {
        if (filter != null && filter.hasFiltersContainer()) {
            boolean added = filter.addCondition(false, true);
            if (added) {
                filter.setControlsVisible(true);
            }
            return added;
        }
        return false;
    }

    public Boolean replaceFilter() {
        if (filter != null && filter.hasFiltersContainer()) {
            boolean added = filter.addCondition(true);
            if (added) {
                filter.setControlsVisible(true);
            }
            return added;
        }
        return false;
    }

    public Boolean resetFilers() {
        if (filter != null) {
            boolean hadFilters = filter.hasFilters();;
            filter.resetConditions();
            return hadFilters;
        }
        return false;
    }

    @Override
    public Pair<ClientPropertyDraw, ClientGroupObjectValue> getFilterColumn(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        ClientPropertyDraw actualProperty = property;
        ClientGroupObjectValue actualColumnKey = columnKey;

        if (actualProperty == null) {
            actualProperty = getSelectedFilterProperty();
            if (actualProperty != null) {
                actualColumnKey = getSelectedColumn();
            }
        }
        return Pair.create(actualProperty, actualColumnKey);
    }
}
