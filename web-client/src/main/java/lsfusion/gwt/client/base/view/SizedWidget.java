package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.view.ComponentViewWidget;
import lsfusion.gwt.client.form.design.view.GFormLayout;

public class SizedWidget {
    public final Widget widget;
    public final GSize width;
    public final GSize height;

    public SizedWidget(Widget widget) {
        this(widget, null, null);
    }

    public SizedWidget(Widget widget, GSize width, GSize height) {
        this.widget = widget;
        this.width = width;
        this.height = height;
    }

    public void add(SizedFlexPanel panel, GFlexAlignment alignment, double flex, boolean shrink) {
        add(panel, panel.getWidgetCount(), alignment, flex, shrink);
    }

    public void add(SizedFlexPanel panel, int beforeIndex, double flex, boolean shrink, GFlexAlignment alignment, boolean alignShrink) {
        boolean vertical = panel.isVertical();
        panel.addSized(widget, beforeIndex, flex, shrink, vertical ? height : width, alignment, alignShrink, vertical ? width : height);
    }

    private void add(SizedFlexPanel panel, int beforeIndex, GFlexAlignment alignment, double flex, boolean shrink) {
        add(panel, beforeIndex, flex, shrink, alignment, alignment.isShrink());
    }

    public void addFill(SizedFlexPanel panel) {
        add(panel, GFlexAlignment.STRETCH, 1, true);
    }

    public void add(SizedFlexPanel panel, GFlexAlignment alignment) {
        add(panel, alignment, 0, false);
    }

    public void add(SizedFlexPanel panel, int beforeIndex, GFlexAlignment alignment) {
        add(panel, beforeIndex, alignment, 0, false);
    }

    public SizedWidget override(Widget view) {
        return new SizedWidget(view, width, height);
    }

    public SizedWidget override(GSize width, GSize height) {
        if(width == null && height == null) // optimization
            return this;
        return new SizedWidget(widget, width != null ? width : this.width, height != null ? height : this.height);
    }

    public final ComponentViewWidget view = new ComponentViewWidget() {
        public SizedWidget getSingleWidget() {
            return SizedWidget.this;
        }

        // needed for binding (to check if the component is shown)
        public Widget getShowingWidget() {
            return widget;
        }

        public void setShowIfVisible(boolean visible) {
            GwtClientUtils.setShowIfVisible(widget, visible);
        }

        public void setVisible(boolean visible) {
            widget.setVisible(visible);
        }

        public boolean isVisible() {
            return widget.isVisible();
        }

        public void setDebugInfo(String sID) {
            GFormLayout.setDebugInfo(widget, sID);
        }

        // CUSTOM container usages
        public void attach(ResizableComplexPanel attachContainer) {
            attachContainer.add(widget);
        }

        public void replace(ResizableComplexPanel panel, String sID) {
            Element panelElement = panel.getElement();
            Element panelChild = panelElement.getElementsByTagName(sID).getItem(0);
            if (panelChild != null) {
                panelChild.getParentElement().replaceChild(widget.getElement(), panelChild);
            }
        }

        public void remove(ResizableComplexPanel panel) {
            panel.remove(widget);
        }

        // CUSTOM SIMPLE container usages

        @Override
        public void add(ResizableComplexPanel panel, int beforeIndex) {
            panel.insert(widget, beforeIndex);
        }

        @Override
        public void remove(ResizableComplexPanel panel, int containerIndex) {
            panel.remove(containerIndex);
        }

        // REGULAR container usages (however inline can be only for simple linear containers)


        @Override
        public void add(SizedFlexPanel panel, int beforeIndex, GSize width, GSize height, double flex, boolean shrink, GFlexAlignment alignment, boolean alignShrink) {
            override(width, height).add(panel, beforeIndex, flex, shrink, alignment, alignShrink);
        }

        public void remove(SizedFlexPanel panel, int containerIndex) {
            panel.removeSized(containerIndex);
        }

        public void remove(SizedFlexPanel panel) {
            panel.removeSized(widget);
        }

        public int getWidgetCount() {
            return 1;
        }
    };
}
