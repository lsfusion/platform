package lsfusion.gwt.client.base.resize;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

public interface ResizeHelper {

    int getChildCount();

    int getChildAbsolutePosition(int index, boolean left);
    void propagateChildResizeEvent(int index, NativeEvent event, Element cursorElement);

    double resizeChild(int index, int delta);
    boolean isChildResizable(int index);

    boolean isResizeOnScroll(int index, NativeEvent event);
    int getScrollSize(int index);

    boolean isVertical();
}
