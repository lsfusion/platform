package lsfusion.gwt.client.form.design;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.jsni.HasNativeSID;
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

public class GComponent implements Serializable, HasNativeSID {
    public int ID;
    public String sID;
    // its key in the native maps (NativeSIDMap): "c" and its ID - the IDs of a form's components are one
    // generator's, so no two are alike, of whatever kind
    public String nativeSID;

    @Override
    public String getNativeSID() {
        return nativeSID;
    }

    public GContainer container;
    public boolean defaultComponent;

    // whether a CUSTOM REACT view places this component instead of drawing it (see isPlatformDrawn)
    public boolean platformDrawn;
    // its own custom view: a React component's name - the one a component that is not a container takes -, or a
    // container's design (GContainer)
    public String custom = null;
    public boolean react = false; // custom holds the React component name

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
    // mounted into a placeholder instead of being drawn from `data`. The flag as the server works it out
    // (FormView.isPlatformDrawn) - a component with a view of its own, an explicit custom (`custom = NULL`, NOCUSTOM, a
    // custom of its own - a React component's too), where a React view draws what it stands in: a component under the
    // React view above it, however deep under the containers that one draws itself (one REMOVEd from the design is
    // placed by nothing), and a GRID property, which has no place of its own, of a group such a view draws, wherever
    // its own component is (the renderers of its rows are placed by the view that draws them) - so the flag is the
    // answer
    public boolean isPlatformDrawn() {
        return platformDrawn;
    }

    public String getCustom() {
        return custom;
    }

    // a React component draws it (ComponentView.isReact): a container's children, or what a component that is not a
    // container shows - its rows, its buttons
    public boolean isReact() {
        return react;
    }

    // WHERE THIS COMPONENT IS: the React view that draws or places it - a component's whose custom is a React component
    // (isReact): a container's, or a grid's, which holds its record -, null when the platform draws it where it stands
    // - pure design data, no controller, no projection. Walked from the component's CONTAINER - a grid's record, which
    // has none, is in its grid (getHiddenContainer): a React view answers, a platform-drawn component ends the walk -
    // its view draws everything under it. Whether the component itself is platform-drawn says only which entry it has
    // there, its content or its descriptor, never where the entry goes. A React view inside what another one draws is
    // platform-drawn, so the first one reached is the only one there is (mirrors ComponentView.getReactPlace, which
    // ends the walk at any view of its own - the same answer)
    public GComponent getReactPlace() {
        for (GComponent parent = getHiddenContainer(); parent != null; parent = parent.getHiddenContainer()) {
            // the first React view answers - one REMOVEd from the design holds what it held, but nothing draws it
            if (parent.isReact())
                return parent.isInForm() ? parent : null;
            if (parent.platformDrawn)
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

    // React draws this component from data, so GWT builds no view of it - below it, only of the platform-drawn
    // components the react container places (GFormLayout.addPlacedContainers): it has a React place and is not
    // platform-drawn - the other half of what has one, a platform-drawn component there, React places (mirrors
    // ComponentView.isReactDrawn)
    public boolean isReactDrawn() {
        return !isPlatformDrawn() && getReactPlace() != null;
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

    private class GShowIfReader extends GComponentReader {
        public GShowIfReader() {
            super(GComponent.this, "_COMPONENT_SHOWIFREADER");
        }

        @Override
        public void updatePlatform(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
            controller.getFormLayout().setShowIfVisible(GComponent.this, !PValue.getBooleanValue(values.get(GGroupObjectValue.EMPTY)));
        }

        @Override
        public void update(GComponentController controller, NativeHashMap<GGroupObjectValue, PValue> values, boolean partial) {
            controller.updateShowIf(this, values, partial);
        }
    }
    public final GPropertyReader showIfReader = new GShowIfReader();

    private class GElementClassReader extends GComponentReader {
        public GElementClassReader() {
            super(GComponent.this, "_COMPONENT_ELEMENTCLASSREADER");
        }

        @Override
        public void updatePlatform(GFormController controller, NativeHashMap<GGroupObjectValue, PValue> values) {
            controller.getFormLayout().setElementClass(GComponent.this, PValue.getClassStringValue(values.get(GGroupObjectValue.EMPTY)));
        }
    }
    public final GPropertyReader elementClassReader = new GElementClassReader();
}
