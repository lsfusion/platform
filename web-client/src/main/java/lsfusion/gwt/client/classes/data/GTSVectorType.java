package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class GTSVectorType extends GTextSearchType {
    public static GTSVectorType instance = new GTSVectorType();

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        return GSize.CONST(150);
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTSVectorCaption();
    }
}
