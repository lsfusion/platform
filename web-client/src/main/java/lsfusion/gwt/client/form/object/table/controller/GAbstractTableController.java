package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.Result;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GInputEvent;
import lsfusion.gwt.client.form.filter.user.GFilter;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GFilterController;
import lsfusion.gwt.client.form.filter.user.view.GFilterConditionView;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.GFooterReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.List;

public abstract class GAbstractTableController extends GPropertyController implements GTableController {
    protected final GToolbarView toolbarView;
    public GFilterController filter;

    protected GridContainerPanel gridView;

    public void initGridView(boolean autoSize) {

        // we need to wrap into simple panel to make layout independent from property value (make flex-basis 0 for upper components)
        // plus we need this panel to change views
        this.gridView = new GridContainerPanel(autoSize, formController);

        getFormLayout().addBaseComponent(getGridComponent(), this.gridView, getDefaultFocusReceiver());

        configureToolbar();
    }

    protected abstract void configureToolbar();

    public void changeGridView(Widget widget, boolean boxed) {
        gridView.changeWidget(widget, boxed);
    }

    public static class GridContainerPanel extends ResizableSimplePanel implements HasMaxPreferredSize {
        private final boolean autoSize;
        private final GFormController form;

        public GridContainerPanel(boolean autoSize, GFormController form) {
            this.autoSize = autoSize;

            setStyleName("gridContainerPanel");

            this.form = form;

            DataGrid.initSinkFocusEvents(this);
        }

        public void changeWidget(Widget widget, boolean boxed) {
            setSizedWidget(widget, autoSize);

            if(boxed)
                addStyleName("gridContainerPanelBoxed");
            else
                removeStyleName("gridContainerPanelBoxed");
        }

        @Override
        public void onBrowserEvent(Event event) {
            Element target = DataGrid.getTargetAndCheck(getElement(), event);
            if(target == null)
                return;
            if(!form.previewEvent(target, event))
                return;

            super.onBrowserEvent(event);

            if(!DataGrid.checkSinkFocusEvents(event))
                return;

            String eventType = event.getType();
            if (BrowserEvents.FOCUS.equals(eventType))
                onFocus(target, event);
            else if (BrowserEvents.BLUR.equals(eventType))
                onBlur(target, event);

            form.propagateFocusEvent(event);
        }

        private boolean isFocused;
        protected void onFocus(Element target, Event event) {
            Widget widget = getWidget();
            if(widget instanceof DataGrid) { // we have to propagate focus to grid, since GWT proceeds the FOCUS event for the first widget that have eventListener (i.e initSinkEvents is called)
                DataGrid<?> grid = (DataGrid<?>) widget;
                grid.onFocus();
                grid.onGridBrowserEvent(target, event);
            }

            if(isFocused)
                return;
            isFocused = true;
            addStyleName("gridContainerPanelFocused");
        }

        protected void onBlur(Element target, Event event) {
            // should be before isFakeBlur check to propagate the event to the cell editor
            Widget widget = getWidget();
            if(widget instanceof DataGrid) {
                DataGrid<?> grid = (DataGrid<?>) widget;
                grid.onBlur(event);
                grid.onGridBrowserEvent(target, event);
            }

            if(!isFocused || DataGrid.isFakeBlur(event, getElement())) {
                return;
            }
            isFocused = false;
            removeStyleName("gridContainerPanelFocused");
        }

        public void setPreferredSize(boolean set, Result<Integer> grids) {
            if(!autoSize)
                changePercentFillWidget(set);

            Widget widget = getWidget();
            if(widget instanceof HasMaxPreferredSize) // needed to setPreferredSize for grid
                ((HasMaxPreferredSize) widget).setPreferredSize(set, grids);
        }
    }

    public GAbstractTableController(GFormController formController, GToolbar toolbar, boolean isList) {
        super(formController);

        if (toolbar == null || !toolbar.visible || !isList) {
            toolbarView = null;
        } else {
            toolbarView = new GToolbarView();
            toolbarView.addStyleName("gridToolbarContainerPanel");
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

    public abstract List<GFilter> getFilters();

    public void initFilters() {
        filter = new GFilterController(this, getFilters(), getFormLayout().getContainerView(getFiltersContainer()) != null) {
            @Override
            public void applyFilters(ArrayList<GPropertyFilter> conditions, ArrayList<GFilterConditionView> changed, boolean focusFirstComponent) {
                long requestIndex = changeFilter(conditions);

                for(GFilterConditionView filterView : changed)
                    formController.setLoading(filterView, requestIndex);

                if (focusFirstComponent) {
                    Scheduler.get().scheduleDeferred(() -> focusFirstWidget());
                }
            }

            @Override
            public void addBinding(GInputEvent event, GBindingEnv env, GFormController.BindingExec pressed, Widget component) {
                formController.addBinding(event, env, pressed, component, getSelectedGroupObject());
            }
        };

        addToToolbar(filter.getToolbarButton());
        GToolbarButton addFilterConditionButton = filter.getAddFilterConditionButton();
        if (addFilterConditionButton != null) {
            addToToolbar(addFilterConditionButton);
        }
        addToToolbar(filter.getResetFiltersButton());
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
        if (filter != null && filter.hasFiltersContainer()) {
            filter.quickEditFilter(editEvent, propertyDraw, columnKey);
        }
    }

    public void replaceFilter(Event event) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.addCondition(event, true);
        }
    }

    public void addFilter(Event event) {
        if (filter != null && filter.hasFiltersContainer()) {
            filter.addCondition(event, false);
        }
    }

    public void resetFilters() {
        if (filter != null) {
            filter.resetAllConditions();
        }
    }

    protected abstract long changeFilter(ArrayList<GPropertyFilter> conditions);
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
            Element childElement = getFormLayout().getContainerView(container).getView().getElement().getFirstChildElement();
            if (childElement != null && childElement.getScrollTop() != 0) {
                childElement.setScrollTop(0);
            }
            scrollToTop(container.container);
        }
    }

    public abstract void updateKeys(GGroupObject group, ArrayList<GGroupObjectValue> keys, GFormChanges fc);
    public abstract void updateCurrentKey(GGroupObjectValue currentKey);
}
