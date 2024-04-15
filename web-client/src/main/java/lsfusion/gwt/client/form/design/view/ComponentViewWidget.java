package lsfusion.gwt.client.form.design.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.SizedWidget;

public interface ComponentViewWidget {

    SizedWidget getSingleWidget();

    // needed for binding (to check if the component is shown)
    Widget getShowingWidget();

    void setShowIfVisible(boolean visible);

    void setVisible(boolean visible);

    boolean isVisible();

    void setDebugInfo(String sID);

    // CUSTOM HTML container usages

    void attach(ResizableComplexPanel attachContainer);

    void replace(ResizableComplexPanel panel, String sID);

    void remove(ResizableComplexPanel panel);

    int getWidgetCount();

    // CUSTOM PLAIN container usages

    void add(ResizableComplexPanel panel, int beforeIndex);

    void remove(ResizableComplexPanel panel, int containerIndex);

    // LINEAR / TABBED container usages (however "inline" can be only for simple linear containers)

    void add(SizedFlexPanel panel, int beforeIndex, GSize width, GSize height, double flex, boolean shrink, GFlexAlignment alignment, boolean alignShrink);

    void remove(SizedFlexPanel panel, int containerIndex);

    // COLUMNS container + PROPERTY PANEL

    default void add(SizedFlexPanel panel, int beforeIndex) {
        add(panel, beforeIndex, null, null, 1.0, true, GFlexAlignment.STRETCH, true);
    }

    void remove(SizedFlexPanel panel);
}
