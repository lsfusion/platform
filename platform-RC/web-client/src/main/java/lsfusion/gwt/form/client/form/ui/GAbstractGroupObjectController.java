package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.form.client.form.ui.filter.GFilterController;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.shared.view.GObject;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.GToolbar;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;
import lsfusion.gwt.form.shared.view.filter.GPropertyFilter;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import lsfusion.gwt.form.shared.view.reader.GFooterReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GAbstractGroupObjectController implements GGroupObjectLogicsSupplier {
    protected final GFormController formController;
    protected final GPanelController panel;
    protected final GToolbarView toolbarView;
    public GFilterController filter;

    public GAbstractGroupObjectController(GFormController formController, GToolbar toolbar) {
        this.formController = formController;

        panel = new GPanelController(formController);

        if (toolbar == null || !toolbar.visible) {
            toolbarView = null;
        } else {
            toolbarView = new GToolbarView();
            getFormLayout().add(toolbar, toolbarView);
        }
    }

    public GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    public boolean hasPanelProperty(GPropertyDraw property) {
        return panel.containsProperty(property);
    }

    public void addToToolbar(Widget tool) {
        if (toolbarView != null) {
            toolbarView.addTool(tool);
        }
    }

    public void addFilterButton() {
        if (showFilter()) {
            filter = new GFilterController(this) {
                @Override
                public void remoteApplyQuery() {
                    changeFilter(new ArrayList<>(getConditions()));
                }

                @Override
                public void filterHidden() {
                    focusFirstWidget();
                }
            };

            addToToolbar(filter.getToolbarButton());
        }
    }

    public void setFilterVisible(boolean visible) {
        if (filter != null) {
            filter.setVisible(visible);
        }
    }

    @Override
    public List<GObject> getObjects() {
        return formController.getObjects();
    }

    @Override
    public List<GPropertyDraw> getPropertyDraws() {
        return formController.getPropertyDraws();
    }

    protected boolean showFilter() {
        return true;
    }

    public void quickEditFilter(EditEvent editEvent, GPropertyDraw propertyDraw) {
        filter.quickEditFilter(editEvent, propertyDraw);
    }

    public void replaceFilter() {
        filter.replaceConditionPressed();
    }

    public void addFilter() {
        filter.addPressed();
    }

    public void removeFilters() {
        filter.allRemovedPressed();
    }

    protected abstract void changeFilter(List<GPropertyFilter> conditions);
    public abstract boolean focusFirstWidget();

    @Override
    public void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values) {
    }
}
