package lsfusion.client.form.design.view.flex;

import lsfusion.base.Pair;
import lsfusion.client.form.design.view.widget.Widget;
import lsfusion.interop.base.view.FlexAlignment;

public interface LinearCaptionContainer {
    void put(Widget widget, Pair<Integer, Integer> valueSizes, FlexAlignment alignment);
}
