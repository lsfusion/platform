package lsfusion.gwt.form.client.form.ui.layout;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.base.client.ui.ResizableSimplePanel;
import lsfusion.gwt.base.client.ui.ResizableTabPanel;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;

import java.util.ArrayList;

import static lsfusion.gwt.base.client.GwtClientUtils.setupFillParent;
import static lsfusion.gwt.base.shared.GwtSharedUtils.relativePosition;

public class GTabbedContainerView extends GAbstractContainerView {

    private final ResizableTabPanel tabsPanel;
    private final Widget view;

    private final ArrayList<GComponent> visibleChildren = new ArrayList<GComponent>();

    private boolean initialTabSet = false;

    public GTabbedContainerView(final GFormController formController, final GContainer container) {
        super(container);

        tabsPanel = new ResizableTabPanel();
        tabsPanel.getDeckPanel().setHeight("100%");

        tabsPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> e) {
                if (initialTabSet) {
                    int index = e.getSelectedItem();
                    formController.setTabVisible(container, visibleChildren.get(index));
                }
            }
        });

        view = new TabbedView();
        view.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    }

    @Override
    protected void addImpl(int index, GComponent child, Widget view) {
        //adding is done in updateLayout()
    }

    @Override
    protected void removeImpl(int index, GComponent child, Widget view) {
        int visibleIndex = visibleChildren.indexOf(child);
        if (visibleIndex != -1) {
            tabsPanel.remove(visibleIndex);
            visibleChildren.remove(visibleIndex);
        }
    }

    @Override
    public Widget getView() {
        return view;
    }

    @Override
    void updateLayout() {
        int childCnt = childrenViews.size();
        for (int i = 0; i < childCnt; i++) {
            GComponent child = children.get(i);
            Widget childView = childrenViews.get(i);

            int index = visibleChildren.indexOf(child);
            if (childView.isVisible()) {
                if (index == -1) {
                    index = relativePosition(child, children, visibleChildren);
                    visibleChildren.add(index, child);

                    ResizableSimplePanel proxyPanel = ResizableSimplePanel.wrapPanel100(childView);
                    tabsPanel.insert(proxyPanel, getTabTitle(child), index);
                }
            } else if (index != -1) {
                visibleChildren.remove(index);
                tabsPanel.remove(index);
            }
        }
        ensureTabSelection();
    }

    private void ensureTabSelection() {
        if (tabsPanel.getTabBar().getSelectedTab() == -1 && tabsPanel.getWidgetCount() != 0) {
            tabsPanel.selectTab(0);
            if (!initialTabSet) {
                initialTabSet = true;
            }
        }
    }

    private String getTabTitle(GComponent child) {
        String tabCaption = null;
        if (child instanceof GContainer) {
            tabCaption = ((GContainer) child).caption;
        }
        if (tabCaption == null) {
            tabCaption = "";
        }
        return tabCaption;
    }

    private class TabbedView extends ResizableSimplePanel implements FlexPanel.FlexAware {
        private final ResizableSimplePanel innerPanel = new ResizableSimplePanel();

        private TabbedView() {
            add(tabsPanel);
        }

        @Override
        public void addedToFlexPanel(FlexPanel parent, GFlexAlignment alignment, double flex) {
            remove(tabsPanel);
            add(innerPanel);

            innerPanel.add(tabsPanel);

            if (flex > 0 && alignment == GFlexAlignment.STRETCH) {
                setupFillParent(getElement(), innerPanel.getElement());

                Style tabsStyle = tabsPanel.getElement().getStyle();
                tabsStyle.setPosition(Style.Position.ABSOLUTE);
                tabsStyle.setWidth(100, Style.Unit.PCT);
                tabsStyle.setHeight(100, Style.Unit.PCT);
            } else if (flex > 0) {
                parent.setChildAlignment(this, GFlexAlignment.STRETCH);

                setupFillParent(getElement(), innerPanel.getElement());

                Style tabsStyle = tabsPanel.getElement().getStyle();
                tabsStyle.setPosition(Style.Position.ABSOLUTE);
                if (alignment == GFlexAlignment.TRAILING) {
                    tabsStyle.setRight(0, Style.Unit.PX);
                }

                if (parent.isVertical()) {
                    tabsStyle.setHeight(100, Style.Unit.PCT);
                } else {
                    tabsStyle.setWidth(100, Style.Unit.PCT);
                }
            } else if (alignment == GFlexAlignment.STRETCH) {
                Style tabsStyle = tabsPanel.getElement().getStyle();
                if (parent.isVertical()) {
                    tabsStyle.setWidth(100, Style.Unit.PCT);
                } else {
                    //todo: такой простой вариант на самом деле не работает - element не расширяется по вертикали...
                    tabsStyle.setHeight(100, Style.Unit.PCT);
                }
            } else {
                //flex == 0 alignment != STRETCH
            }
        }
    }
}
