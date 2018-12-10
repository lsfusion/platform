package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.shared.form.view.*;
import lsfusion.gwt.shared.view.*;
import lsfusion.gwt.shared.view.filter.GCompare;

import static lsfusion.gwt.shared.view.filter.GCompare.*;

public abstract class GDataType extends GType implements GClass {
    @Override
    public boolean hasChildren() {
        return false;
    }

    public static int getFullWidthString(GFont font, String widthString, GWidthStringProcessor widthStringProcessor) {
        GFontWidthString fontWidthString = new GFontWidthString(font == null ? GFont.DEFAULT_FONT : font, widthString);
        if(widthStringProcessor != null) {
            widthStringProcessor.addWidthString(fontWidthString);
            return 0;
        }
        return GFontMetrics.getStringWidth(fontWidthString) + 8;
    }

    public int getFullWidthString(String widthString, GFont font, GWidthStringProcessor widthStringProcessor) {
        return getFullWidthString(font, widthString, widthStringProcessor);
    }

    protected int getDefaultCharWidth() {
        return 0;
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor) {
        return getFullWidthString(getDefaultWidthString(propertyDraw), font, widthStringProcessor);
    }

    protected String getDefaultWidthString(GPropertyDraw propertyDraw) {
        int defaultCharWidth = getDefaultCharWidth();
        if(defaultCharWidth != 0)
            return GwtSharedUtils.replicate('0', defaultCharWidth);
        throw new UnsupportedOperationException();
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }
}
