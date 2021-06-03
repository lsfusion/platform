package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GInputEvent;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.GFooterReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.List;

public abstract class GAbstractTableController extends GPropertyController implements GTableController {
    protected final GToolbarView toolbarView;
    public GUserFilters userFilters;

    public GAbstractTableController(GFormController formController, GToolbar toolbar, boolean isList) {
        super(formController);

        if (toolbar == null || !toolbar.visible || !isList) {
            toolbarView = null;
        } else {
            toolbarView = new GToolbarView();
            getFormLayout().addBaseComponent(toolbar, toolbarView, null);
        }
    }

    @Override
    public GFormController getForm() {
        return formController;
    }

    protected DefaultFocusReceiver getDefaultFocusReceiver() {
        return () -> {
            boolean focused = focusFirstWidget();
            if (focused) {
                scrollToTop();
            }
            return focused;
        };
    }

    public void addToToolbar(Widget tool) {
        if (toolbarView != null) {
            toolbarView.addComponent(tool);
        }
    }
    
    public void addToolbarSeparator() {
        if (toolbarView != null) {
            toolbarView.addSeparator();
        }
    }

    public abstract GFilter getFilterComponent();

    public void addUserFilterComponent() {
        userFilters = new GUserFilters(this, getFilterComponent()) {
            @Override
            public void applyQuery() {
                changeFilter(new ArrayList<>(getConditions()));
                focusFirstWidget();
            }

            @Override
            public void checkCommitEditing() {
                formController.checkCommitEditing();
            }

            @Override
            public void addBinding(GInputEvent event, GBindingEnv env, GFormController.BindingExec pressed, Widget component) {
                formController.addBinding(event, env, pressed, component, getSelectedGroupObject());
            }
        };
        getFormLayout().addBaseComponent(getFilterComponent(), userFilters.getView(), null);
        
        addToToolbar(userFilters.getToolbarButton());
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

    public void quickEditFilter(Event editEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (userFilters != null) {
            userFilters.quickEditFilter(editEvent, propertyDraw, columnKey);
        }
    }

    public void replaceFilter(Event event) {
        if (userFilters != null) {
            userFilters.addConditionPressed(true, event);
        }
    }

    public void addFilter(Event event) {
        if (userFilters != null) {
            userFilters.addConditionPressed(false, event);
        }
    }

    public void removeFilters() {
        if (userFilters != null) {
            userFilters.allRemovedPressed();
        }
    }

    protected abstract void changeFilter(ArrayList<GPropertyFilter> conditions);
    // eventually is called either on form opening / form tab selection / filter dialog close
    public abstract boolean focusFirstWidget();
    public abstract GComponent getGridComponent();

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

    public abstract void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values);
    public abstract void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values);

    // вызов focus() у getFocusHolderElement() грида по какой-то причине приводит к подскролливанию нашего скролла
    // (если грид заключён в скролл и не влезает по высоте) до первого ряда таблицы, скрывая заголовок (видимо вызывается scrollIntoView(), 
    // который, кстати, продолжает вызываться и при последующих изменениях фокуса в IE).
    // поэтому крутим все скроллы-предки вверх при открытии формы.
    // неоднозначное решение, т.к. вовсе необязательно фокусный компонент находится вверху скролла, но пока должно хватать. 
    public void scrollToTop() {
        GComponent gridComponent = getGridComponent();
        if (gridComponent != null) {
            scrollToTop(gridComponent.container);
        }
    }

    private void scrollToTop(GContainer container) {
        if (container != null) {
            if (container.isScroll()) {
                Element childElement = getFormLayout().getContainerView(container).getView().getElement().getFirstChildElement();
                if (childElement != null && childElement.getScrollTop() != 0) {
                    childElement.setScrollTop(0);
                }
            }
            scrollToTop(container.container);
        }
    }

    public abstract void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc);
    public abstract void updateCurrentKey(GGroupObjectValue currentKey);
}
