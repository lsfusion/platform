package lsfusion.client.form.design.view;

import java.awt.*;

public interface ResizeHelper {

    int getChildCount();

    /*Element*/Component getChildElement(int index);
    /*Widget*/Component getChildWidget(int index);

    void resizeChild(int index, int delta);
    boolean isChildResizable(int index);
    boolean isChildVisible(int index);

    boolean isVertical();
}
