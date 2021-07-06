package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GAbstractContainerView;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.tree.view.GTreeTable;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.GFooterReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public abstract class GAbstractTableController extends GPropertyController implements GTableController {
    protected final GToolbarView toolbarView;
    public GUserFilters filter;

    protected Widget gridView;
    protected ResizableSimplePanel gridContainerView;
    public Widget recordView;

    public void initGridView(boolean autoSize, GContainer record, GAbstractContainerView.UpdateLayoutListener listener) {
        ResizableSimplePanel gridContainerView = new ResizableSimplePanel();
        gridContainerView.setStyleName("gridResizePanel");
        if(autoSize) { // убираем default'ый minHeight
            gridContainerView.getElement().getStyle().setProperty("minHeight", "0px");
            gridContainerView.getElement().getStyle().setProperty("minWidth", "0px");
        }
        this.gridContainerView = gridContainerView;

        Widget gridView = gridContainerView;

        // proceeding recordView
        if(record != null) {
            GAbstractContainerView recordView = getFormLayout().getContainerView(record);
            recordView.addUpdateLayoutListener(listener);
            this.recordView = recordView.getView();

            // we need to add recordview somewhere, to attach it (events, listeners, etc.)
            ResizableComplexPanel virtualGridView = new ResizableComplexPanel();
            virtualGridView.setMain(gridView);
            setupFillParent(gridView.getElement());

            // need to wrap recordView to setVisible false recordView's parent and not recordView itself (since it will be moved and shown by table view implementation)
            ResizableSimplePanel virtualRecordView = new ResizableSimplePanel();
            virtualRecordView.add(this.recordView);
            virtualRecordView.setVisible(false);
            virtualGridView.add(virtualRecordView);

            gridView = virtualGridView;
        }

        this.gridView = new GridPanel(gridView, gridContainerView);

        getFormLayout().addBaseComponent(getGridComponent(), this.gridView, getDefaultFocusReceiver());

        configureToolbar();
    }

    protected abstract void configureToolbar();

    public void changeGridView(Widget widget) {
        gridContainerView.setFillWidget(widget);
    }

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

    public void addFilterButton() {
        filter = new GUserFilters(this) {
            @Override
            public void remoteApplyQuery() {
                changeFilter(new ArrayList<>(getConditions()));
            }

                @Override
                public void filterClosed() {
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

    // needed for auto grid sizing
    public static class GridPanel extends FlexPanel {
        private final Widget view;
        private final ResizableSimplePanel gridContainerView;

        public GridPanel(Widget view, ResizableSimplePanel gridContainerView) {
            super(true);

            this.view = view;
            addFill(view);

            this.gridContainerView = gridContainerView;
        }

        public void autoSize() {
            Widget widget = gridContainerView.getWidget();
            int autoSize;
            if(widget instanceof GGridPropertyTable) {
                GGridPropertyTable gridTable = (GGridPropertyTable)widget;
                if (gridTable instanceof GTreeTable) {
                    autoSize = gridTable.getMaxPreferredSize().height;
                } else {
                    autoSize = gridTable.getAutoSize();
                    if (autoSize <= 0) // еще не было layout'а, ставим эвристичный размер
                        autoSize = gridTable.getMaxPreferredSize().height;
                    else {
                        autoSize += view.getOffsetHeight() - gridTable.getViewportHeight(); // margin'ы и border'ы учитываем
                    }
                }
            } else
                autoSize = GTreeTable.DEFAULT_MAX_PREFERRED_HEIGHT;
            setChildFlexBasis(view, autoSize);
        }
    }
}
