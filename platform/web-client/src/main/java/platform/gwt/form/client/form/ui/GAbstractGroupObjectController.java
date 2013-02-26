package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.ui.filter.GFilterController;
import platform.gwt.form.shared.view.GObject;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.GToolbar;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.filter.GPropertyFilter;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.reader.GFooterReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GAbstractGroupObjectController implements GGroupObjectLogicsSupplier {
    protected GFormController formController;
    protected GPanelController panel;
    protected GToolbarView toolbar;
    public GFilterController filter;

    public GAbstractGroupObjectController(GFormController formController, GFormLayout formLayout, GToolbar iToolbar) {
        this.formController = formController;

        panel = new GPanelController(formController, formLayout);

        toolbar = new GToolbarView();
        if (iToolbar != null && iToolbar.visible) {
            formLayout.add(iToolbar, toolbar);
        }
    }

    public boolean hasPanelProperty(GPropertyDraw property) {
        return panel.containsProperty(property);
    }

    @Override
    public void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values) {
    }

    public void addToToolbar(Widget tool) {
        toolbar.addTool(tool);
    }

    public void addFilterButton() {
        if (showFilter()) {
            filter = new GFilterController(this) {
                @Override
                public void remoteApplyQuery() {
                    changeFilter(new ArrayList<GPropertyFilter>(getConditions()));
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

    public void quickEditFilter(EditEvent editEvent) {
        quickEditFilter(editEvent, null);
    }

    public void quickEditFilter(EditEvent editEvent, GPropertyDraw propertyDraw) {
        filter.quickEditFilter(editEvent, propertyDraw);
    }

    protected abstract void changeFilter(List<GPropertyFilter> conditions);
    public abstract boolean focusFirstWidget();
}
