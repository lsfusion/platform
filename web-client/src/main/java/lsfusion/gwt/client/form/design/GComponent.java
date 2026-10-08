package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.controller.GComponentController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GComponentReader;
import lsfusion.gwt.client.form.property.GPropertyReader;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GComponent implements Serializable {
    public int ID;
    public String sID;
    public GContainer container;
    public boolean defaultComponent;

    // meaningful only inside a CUSTOM REACT container, under the containers it draws itself (see isLsfView); ignored on
    // any other component
    public boolean lsf;

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

    // the platform draws this component and a CUSTOM REACT view places it: its real (server-built) lsFusion view is
    // mounted into a placeholder instead of being drawn from `data`. The flag as the server sends it, which has checked
    // where every lsf component is (FormView.checkLsfViews) - a container or a panel property is placed by the CUSTOM
    // REACT container above it, however deep under the containers that one draws itself (one REMOVEd from the design is
    // placed by nothing), and a GRID property, which has no place of its own, belongs to a group such a container
    // draws, wherever its own component is (the renderers of its rows are placed by the view that draws them) - so the
    // flag is the answer
    public boolean isLsfView() {
        return lsf;
    }

    // WHERE THIS COMPONENT IS: the react container that draws or places it, null when the platform draws it where it
    // stands - pure design data, no controller, no projection. Walked from the component's CONTAINER - a grid's record,
    // which has none, is in its grid (getHiddenContainer): a react container answers, an lsf component ends the walk -
    // the platform draws everything under it. The component's own `lsf` says only which entry it has there, its
    // content or its descriptor, never where the entry goes. A react container inside what another one draws is refused
    // when the form is built (FormView.checkCustomReactSwallowed), so the first one reached is the only one there is
    // (mirrors ComponentView.getReactPlace)
    public GContainer getReactPlace() {
        return getReactPlaceFrom(getHiddenContainer());
    }
    // ... the walk itself, up from a component: the walk of what a container holds starts at the container
    // (GContainer.getChildrenReactPlace)
    protected static GContainer getReactPlaceFrom(GComponent component) {
        for (GComponent parent = component; parent != null; parent = parent.getHiddenContainer()) {
            // the first react container answers - one REMOVEd from the design holds its children, but nothing draws it
            if (parent instanceof GContainer && ((GContainer) parent).isReact())
                return parent.isInForm() ? (GContainer) parent : null;
            if (parent.lsf)
                return null;
        }
        return null;
    }
    // whether the component is in the form's design at all: a REMOVEd one is not, and neither is what it held. A grid's
    // record has no container, and is where its grid is (getHiddenContainer) (mirrors ComponentView.isInForm)
    public boolean isInForm() {
        GComponent component = this;
        while (component.getHiddenContainer() != null)
            component = component.getHiddenContainer();
        return component instanceof GContainer && ((GContainer) component).main;
    }
    // the component this one is in: its container (GContainer: a grid's record, which has none, is in its grid -
    // mirrors ComponentView.getHiddenContainer)
    public GComponent getHiddenContainer() {
        return container;
    }
    // ... and what is inside it, walked down: a container's children, a grid's record (GGrid) - the twin of
    // getHiddenContainer
    public List<GComponent> getChildren() {
        return Collections.emptyList();
    }

    // React draws this component from data, so GWT builds no view of it - below it, only of the lsf components the
    // react container places (GFormLayout.addPlacedContainers): it has a React place and is not lsf - the other half of
    // what has one, an lsf component there, React places (mirrors ComponentView.isReactDrawn)
    public boolean isReactDrawn() {
        return !isLsfView() && getReactPlace() != null;
    }

    // a component's labels (caption / image) — their dynamic readers and static design values — exposed uniformly.
    // Base has none; GPropertyDraw and GContainer override.
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
    public GPropertyReader[] getLabelReaders() {
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
        public void updateLsf(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
            controller.getFormLayout().setShowIfVisible(GComponent.this, !PValue.getBooleanValue(values.get(GGroupObjectValue.EMPTY)));
        }

        @Override
        public void update(GComponentController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            controller.updateShowIf(this, values, partial);
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
        public void updateLsf(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
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
