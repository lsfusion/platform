package lsfusion.gwt.client.base.resize;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

public interface ResizeHelper {

    int getChildCount();

    Element getChildElement(int index);
    Widget getChildWidget(int index);

    void resizeChild(int index, int delta);
    boolean isChildResizable(int index);
    boolean isChildVisible(int index);

    boolean isVertical();
}
