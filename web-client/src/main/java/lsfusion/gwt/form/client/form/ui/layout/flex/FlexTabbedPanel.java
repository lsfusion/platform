package lsfusion.gwt.form.client.form.ui.layout.flex;

import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.FlexPanel;
import lsfusion.gwt.base.client.ui.GFlexAlignment;
import lsfusion.gwt.form.client.form.ui.layout.TabbedPanelBase;

public class FlexTabbedPanel extends TabbedPanelBase {

    private final FlexPanel panel;

    public FlexTabbedPanel() {
        FlexTabBar tabBar = new FlexTabBar();
        TabbedDeckPanel deck = new TabbedDeckPanel();

        panel = new FlexPanel(true);
        panel.add(tabBar, GFlexAlignment.STRETCH);
        panel.addFill(deck);

        initTabbedPanel(tabBar, deck, panel);

        setStyleName("gwt-TabPanel");
        deck.setStyleName("gwt-TabPanelBottom");
    }

    public void setMargins(int marginTop, int marginBottom, int marginLeft, int marginRight) {
        panel.setMargins(marginTop, marginBottom, marginLeft, marginRight);
    }

    private static class TabbedDeckPanel extends FlexPanel implements TabDeck {
        private Widget visibleWidget;

        private TabbedDeckPanel() {
            super(true);
        }

        @Override
        public void insert(Widget widget, int beforeIndex) {
            widget.setVisible(false);
            addFill(widget, beforeIndex);
        }

        @Override
        public boolean remove(Widget w) {
            int ind = getWidgetIndex(w);
            if (ind != -1) {
                if (visibleWidget == w) {
                    visibleWidget = null;
                }
                return super.remove(w);
            }

            return false;
        }

        public void showWidget(int index) {
            checkIndexBoundsForAccess(index);

            Widget oldWidget = visibleWidget;
            visibleWidget = getWidget(index);

            if (visibleWidget != oldWidget) {
                visibleWidget.setVisible(true);
                if (oldWidget != null) {
                    oldWidget.setVisible(false);
                }
            }
        }

        @Override
        public void onResize() {
            if (visibleWidget instanceof RequiresResize) {
                ((RequiresResize) visibleWidget).onResize();
            }
        }
    }
}
