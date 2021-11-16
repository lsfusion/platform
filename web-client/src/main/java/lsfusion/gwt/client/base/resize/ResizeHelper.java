package lsfusion.gwt.client.base.resize;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;

public interface ResizeHelper {

    int getChildCount();

    int getChildAbsolutePosition(int index, boolean left);
    void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement);

    void resizeChild(int index, int delta);
    boolean isChildResizable(int index);

    boolean isVertical();
}
