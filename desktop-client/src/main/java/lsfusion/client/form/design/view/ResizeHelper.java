package lsfusion.client.form.design.view;

import lsfusion.client.form.design.view.widget.Widget;

import java.awt.*;

public interface ResizeHelper {

    int getChildCount();

    /*Element*/Component getChildElement(int index);
    /*Widget*/Widget getChildWidget(int index);

    double resizeChild(int index, int delta);
    boolean isChildResizable(int index);
    boolean isChildVisible(int index);

    boolean isVertical();
}
