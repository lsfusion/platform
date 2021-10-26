package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.GFlexAlignment;

public interface LinearCaptionContainer {
    void put(Widget columnCaptionWidget, Widget actualCaptionWidget, Pair<Integer, Integer> valueSizes, GFlexAlignment alignment);
}
