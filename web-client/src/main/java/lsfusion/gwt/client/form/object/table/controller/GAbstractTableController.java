package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.GFooterReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.List;

public abstract class GAbstractTableController implements GTableController {
    protected final GFormController formController;
    protected final GPanelController panel;
    protected final GToolbarView toolbarView;
    public GUserFilters filter;

    public GAbstractTableController(GFormController formController, GToolbar toolbar, boolean isList) {
        this.formController = formController;

        panel = new GPanelController(formController);

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

    public GFormLayout getFormLayout() {
        return formController.formLayout;
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

    public void addFilterButton() {
        filter = new GUserFilters(this) {
            @Override
            public void remoteApplyQuery() {
                changeFilter(new ArrayList<>(getConditions()));
            }

                @Override
                public void filterHidden() {
                    focusFirstWidget();
                }

                @Override
                public void checkCommitEditing() {
                    formController.checkCommitEditing();
                }
            };

        addToToolbar(filter.getToolbarButton());
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
        filter.quickEditFilter(editEvent, propertyDraw, columnKey);
    }

    public void replaceFilter() {
        if (filter != null) {
            filter.addConditionPressed(true);
        }
    }

    public void addFilter() {
        filter.addConditionPressed(false);
    }

    public void removeFilters() {
        filter.allRemovedPressed();
    }

    protected abstract void changeFilter(ArrayList<GPropertyFilter> conditions);
    // eventually is called either on form opening / form tab selection / filter dialog close
    public abstract boolean focusFirstWidget();
    public abstract GComponent getGridComponent();

    @Override
    public void updateFooterValues(GFooterReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
    }

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

    @Override
    public void setContainerCaption(GContainer container, String caption) {
        formController.setContainerCaption(container, caption);
    }
}
