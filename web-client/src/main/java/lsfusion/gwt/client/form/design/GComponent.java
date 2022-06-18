package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;

    public int width = -1;
    public int height = -1;
    
    public int span = 1;

    protected double flex = 0;
    protected GFlexAlignment alignment;
    public boolean shrink;
    public boolean alignShrink;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public ColorDTO background;
    public ColorDTO foreground;

    public GFont font;
    public GFont captionFont;

    public GSize getSize(boolean vertical) {
        int size = vertical ? height : width;
        if(size == -2)
            return vertical ? getDefaultHeight() : getDefaultWidth();
        if (size == -1)
            return null;
        return GSize.getComponentSize(size);
    }

    protected GSize getDefaultWidth() {
        throw new UnsupportedOperationException();
    }

    protected GSize getDefaultHeight() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        String className = this.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className + "{" +
               "sID='" + sID + '\'' +
               ", defaultComponent=" + defaultComponent +
               '}';
    }

    public boolean isTab() {
        return container != null && container.tabbed;
    }

    public double getFlex() {
        return flex;
    }

    public void setFlex(double flex) {
        this.flex = flex;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }

    public boolean isShrink() {
        return shrink;
    }

    public boolean isAlignShrink() {
        return alignShrink;
    }

    public void setAlignment(GFlexAlignment alignment) {
        this.alignment = alignment;
    }

    public boolean hasMargins() {
        return marginTop != 0 || marginBottom != 0 || marginLeft != 0 || marginRight != 0;
    }

    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }

    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }

    public int getMargins(boolean vertical) {
        return vertical ? getVerticalMargin() : getHorizontalMargin();
    }

    public boolean isAlignCaption() {
        return false;
    }

    public int getSpan() {
        return span;
    }

    private class GShowIfReader implements GPropertyReader {
        private String sID;

        public GShowIfReader() {
        }

        @Override
        public void update(GFormController controller, NativeHashMap<GGroupObjectValue, Object> values, boolean updateKeys) {
            controller.getFormLayout().setShowIfVisible(GComponent.this, values.get(GGroupObjectValue.EMPTY) == null);
        }

        @Override
        public String getNativeSID() {
            if(sID == null) {
                sID = "_COMPONENT_" + "SHOWIFREADER" + "_" + GComponent.this.sID;
            }
            return sID;
        }
    }
    public final GPropertyReader showIfReader = new GShowIfReader();
}