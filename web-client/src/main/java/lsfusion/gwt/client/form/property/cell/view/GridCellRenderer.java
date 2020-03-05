package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.design.GFont;

public interface GridCellRenderer {
    default void render(Element element, GFont font, Object value, boolean isSingle) {
        renderStatic(element, font, isSingle);
        renderDynamic(element, font, value, isSingle);
    }

    default void renderStatic(Element element, GFont font, boolean isSingle) {}

    default void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
    }
}
