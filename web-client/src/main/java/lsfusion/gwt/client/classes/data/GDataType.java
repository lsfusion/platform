package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.GFontWidthString;
import lsfusion.gwt.client.form.design.GWidthStringProcessor;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.form.filter.user.GCompare.*;

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
        return GFontMetrics.getStringWidth(fontWidthString);
    }

    public int getFullWidthString(String widthString, GFont font, GWidthStringProcessor widthStringProcessor) {
        return getFullWidthString(font, widthString, widthStringProcessor) + GwtClientUtils.CELL_HORIZONTAL_PADDING * 2;
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
