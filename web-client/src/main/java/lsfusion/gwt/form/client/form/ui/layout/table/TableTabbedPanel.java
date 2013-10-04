package lsfusion.gwt.form.client.form.ui.layout.table;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.form.client.form.ui.layout.TabbedPanelBase;

public class TableTabbedPanel extends TabbedPanelBase {

    public TableTabbedPanel() {
        TableTabBar tabBar = new TableTabBar();
        tabBar.setWidth("100%");

        OuterPanel panel = new OuterPanel(tabBar);

        initTabbedPanel(tabBar, new TabDeckImpl(panel), panel);

        setStyleName("gwt-TabPanel");
        DOM.setElementProperty(panel.mainTD, "className", "gwt-TabPanelBottom");
    }

    private static class OuterPanel extends CellPanel implements InsertPanel, RequiresResize {
        private Widget visibleWidget;
        private final Element mainTD;

        public OuterPanel(TableTabBar tabBar) {
            DOM.setElementProperty(getTable(), "cellSpacing", "0");
            DOM.setElementProperty(getTable(), "cellPadding", "0");

            Element tabTR = DOM.createTR();
            Element tabTD = DOM.createTD();
            DOM.appendChild(tabTR, tabTD);
            DOM.appendChild(getBody(), tabTR);
            add(tabBar, tabTD);

            Element tr = DOM.createTR();
            mainTD = DOM.createTD();
            mainTD.setPropertyString("width", "100%");
            mainTD.setPropertyString("height", "100%");
            DOM.appendChild(tr, mainTD);
            DOM.appendChild(getBody(), tr);
        }

        @Override
        public void add(Widget w) {
            insert(w, getWidgetCount());
        }

        public void insert(Widget w, int beforeIndex) {
            checkIndexBoundsForInsertion(beforeIndex);

            Element container = createWidgetContainer();

            DOM.appendChild(mainTD, container);

            insert(w, container, beforeIndex, false);

            w.setSize("100%", "100%");
        }

        private Element createWidgetContainer() {
            Element container = DOM.createDiv();
            DOM.setStyleAttribute(container, "width", "100%");
            DOM.setStyleAttribute(container, "height", "100%");
            DOM.setStyleAttribute(container, "padding", "0px");
            DOM.setStyleAttribute(container, "margin", "0px");
            UIObject.setVisible(container, false);
            return container;
        }

        @Override
        public boolean remove(Widget w) {
            if (visibleWidget == w) {
                visibleWidget = null;
            }
            w.getElement().getParentElement().removeFromParent();
            return super.remove(w);
        }

        public void showWidget(int index) {
            if (index <= 0 || index >= getWidgetCount()) {
                throw new IndexOutOfBoundsException();
            }

            Widget oldWidget = visibleWidget;
            visibleWidget = getWidget(index);

            if (visibleWidget != oldWidget) {
                UIObject.setVisible(visibleWidget.getElement().getParentElement(), true);
                if (oldWidget != null) {
                    UIObject.setVisible(oldWidget.getElement().getParentElement(), false);
                }
            }
        }

        public void onResize() {
            if (visibleWidget instanceof RequiresResize) {
                ((RequiresResize) visibleWidget).onResize();
            }
        }
    }

    // у OuterPanel один из чилдренов - TabBar, поэтому нужно изменять индексы на 1
    private class TabDeckImpl implements TabDeck {
        private OuterPanel panel;

        public TabDeckImpl(OuterPanel panel) {
            this.panel = panel;
        }

        @Override
        public void showWidget(int tabIndex) {
            panel.showWidget(tabIndex + 1);
        }

        @Override
        public void insert(Widget widget, int beforeIndex) {
            panel.insert(widget, beforeIndex + 1);
        }

        @Override
        public boolean remove(int index) {
            return panel.remove(index + 1);
        }

        @Override
        public int getWidgetCount() {
            return panel.getWidgetCount() - 1;
        }

        @Override
        public int getWidgetIndex(Widget widget) {
            int ind = panel.getWidgetIndex(widget);
            return ind > 0 ? ind - 1 : -1;
        }

        @Override
        public Widget getWidget(int index) {
            return panel.getWidget(index + 1);
        }

        @Override
        public void onResize() {
            panel.onResize();
        }
    }
}