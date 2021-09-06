package lsfusion.client.form.design.view;

import java.awt.*;

public interface ResizeHelper {

    int getChildCount(FlexPanel panel);

    /*Element*/Component getChildElement(FlexPanel panel, int index);
    /*Widget*/Component getChildWidget(FlexPanel panel, int index);

    void resizeChild(FlexPanel panel, int index, int delta);
    boolean isChildResizable(FlexPanel panel, int index);
    boolean isChildVisible(FlexPanel panel, int index);

    boolean isVertical();
}
