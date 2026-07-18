package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;

    // meaningful only for a direct child of a CUSTOM REACT container (see isDelegated); ignored on any other component
    public boolean delegate;

    public String elementClass;

    public int width = -1;
    public int height = -1;

    public boolean captionVertical;
    public boolean captionLast;
    public GFlexAlignment captionAlignmentHorz;
    public GFlexAlignment captionAlignmentVert;

    public int span = 1;

    protected double flex = 0;
    protected GFlexAlignment alignment;
    public boolean shrink;
    public boolean alignShrink;
    public Boolean alignCaption;
    public String overflowHorz;
    public String overflowVert;

    public ColorDTO background;
    public ColorDTO foreground;

    public String getBackground() {
        return background != null ? background.toString() : null;
    }

    public String getForeground() {
        return foreground != null ? foreground.toString() : null;
    }

    public GFont font;
    public GFont captionFont;

    public GSize getWidth() {
        int size = width;
        if(size == -2)
            return getDefaultWidth();
        if (size == -1 || size == -3)
            return null;
        return GSize.getComponentSize(size);
    }
    public GSize getHeight() {
        int size = height;
        if(size == -2)
            return getDefaultHeight();
        if (size == -1 || size == -3)
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

    public boolean isInCustom() {
        return container != null && container.isCustom();
    }

    public boolean isInReact() { // a direct child of a CUSTOM REACT container (sibling of isInCustom)
        return container != null && container.isReact();
    }

    // the delegate flag is only meaningful for a direct child of a CUSTOM REACT container: such a child keeps its real (server-built) GWT view and React mounts it into a placeholder instead of owning/replacing it
    public boolean isDelegated() {
        return delegate && isInReact();
    }

    // the complement of isDelegated within a react container: a child React DRAWS (from data), so GWT builds no view for
    // it and it is react-owned. A child outside a react container is neither delegated nor react-projected.
    public boolean isReactProjected() {
        return !delegate && isInReact();
    }

    // a component's semantic presentation descriptors (caption / image) — their dynamic readers and static design
    // values — exposed uniformly. Base has none; GPropertyDraw and GContainer override.
    public GPropertyReader getCaptionReader() {
        return null;
    }
    public GPropertyReader getImageReader() {
        return null;
    }
    public String getStaticCaption() {
        return null;
    }
    public BaseImage getStaticImage() {
        return null;
    }
    public String getStaticImageHTML() { // the static design image (appImage) as an <img> HTML string, or null
        BaseImage image = getStaticImage();
        return image != null ? image.createImageHTML() : null;
    }

    // Each reader self-declares its field, conversion and static fallback, like a property's meta readers.
    public GPropertyReader[] getComponentReaders() {
        return new GPropertyReader[] { getCaptionReader(), getImageReader() };
    }

    public boolean isFlex() {
        return flex > 0;
    }
    public double getFlex(RendererType rendererType) {
        return flex;
    }

    public void setFlex(double flex) {
        this.flex = flex;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }

    public boolean isCaptionLast() {
        return captionLast;
    }

    public GFlexAlignment getCaptionAlignmentHorz() {
        return captionAlignmentHorz;
    }

    public GFlexAlignment getCaptionAlignmentVert() {
        return captionAlignmentVert;
    }

    public boolean isShrink() {
        return shrink;
    }

    public boolean isAlignShrink() {
        return alignShrink;
    }

    public String getOverflowHorz() {
        return overflowHorz;
    }

    public String getOverflowVert() {
        return overflowVert;
    }

    public void setAlignment(GFlexAlignment alignment) {
        this.alignment = alignment;
    }

    public boolean isAlignCaption() {
        if(alignCaption != null)
            return alignCaption;

        return isDefautAlignCaption();
    }

    public boolean isDefautAlignCaption() {
        return false;
    }

    public int getSpan() {
        return span;
    }

    private class GShowIfReader implements GComponentReader {
        private String sID;

        public GShowIfReader() {
        }

        @Override
        public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
            controller.getFormLayout().setShowIfVisible(GComponent.this, !PValue.getBooleanValue(values.get(GGroupObjectValue.EMPTY)));
        }

        @Override
        public GComponent getReaderComponent() {
            return GComponent.this;
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

    private class GElementClassReader implements GComponentReader {
        private String sID;

        public GElementClassReader() {
        }

        @Override
        public void update(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean updateKeys) {
            controller.getFormLayout().setElementClass(GComponent.this, PValue.getClassStringValue(values.get(GGroupObjectValue.EMPTY)));
        }

        @Override
        public GComponent getReaderComponent() {
            return GComponent.this;
        }

        @Override
        public String getNativeSID() {
            if(sID == null) {
                sID = "_COMPONENT_" + "ELEMENTCLASSREADER" + "_" + GComponent.this.sID;
            }
            return sID;
        }
    }
    public final GPropertyReader elementClassReader = new GElementClassReader();
}
