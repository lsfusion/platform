package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.AbstractComponent;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import static java.lang.Math.max;
import static lsfusion.base.BaseUtils.nvl;

public class ComponentView extends IdentityObject implements ServerIdentitySerializable, AbstractComponent {

    private NFProperty<String> elementClass = NFFact.property();

    private NFProperty<Integer> width = NFFact.property();
    private NFProperty<Integer> height = NFFact.property();

    private NFProperty<Integer> span = NFFact.property();

    private NFProperty<Boolean> defaultComponent = NFFact.property();
    private NFProperty<Boolean> activated = NFFact.property();

    private NFProperty<Double> flex = NFFact.property();
    private NFProperty<FlexAlignment> alignment = NFFact.property();
    private NFProperty<Boolean> shrink = NFFact.property();
    private NFProperty<Boolean> alignShrink = NFFact.property();
    private NFProperty<Boolean> alignCaption = NFFact.property();
    private NFProperty<String> overflowHorz = NFFact.property();
    private NFProperty<String> overflowVert = NFFact.property();

    private NFProperty<Boolean> captionVertical = NFFact.property();
    private NFProperty<Boolean> captionLast = NFFact.property();
    private NFProperty<FlexAlignment> captionAlignmentHorz = NFFact.property();
    private NFProperty<FlexAlignment> captionAlignmentVert = NFFact.property();

    private NFProperty<Integer> marginTop = NFFact.property();
    private NFProperty<Integer> marginBottom = NFFact.property();
    private NFProperty<Integer> marginLeft = NFFact.property();
    private NFProperty<Integer> marginRight = NFFact.property();

    private NFProperty<FontInfo> font = NFFact.property();
    private NFProperty<FontInfo> captionFont = NFFact.property();
    private NFProperty<Color> background = NFFact.property();
    private NFProperty<Color> foreground = NFFact.property();

    @Override
    public String toString() {
        return getSID();
    }

    public PropertyObjectEntity propertyElementClass;

    public PropertyObjectEntity<?> showIf;

    public int getWidth(FormInstanceContext context) {
        Integer width = getWidth();
        if(width != null)
            return width;

        return getDefaultWidth(context);
    }

    public int getHeight(FormInstanceContext context) {
        Integer height = getHeight();
        if(height != null)
            return height;

        return getDefaultHeight(context);
    }

    protected int getDefaultWidth(FormInstanceContext context) {
        return -1;
    }

    protected int getDefaultHeight(FormInstanceContext context) {
        return -1;
    }

    protected boolean isDefaultCaptionVertical(FormInstanceContext context) {
        return true;
    }

    protected boolean isDefaultCaptionLast(FormInstanceContext context) {
        return false;
    }

    protected FlexAlignment getDefaultCaptionAlignmentHorz(FormInstanceContext context) {
        return FlexAlignment.START;
    }

    protected FlexAlignment getDefaultCaptionAlignmentVert(FormInstanceContext context) {
       return FlexAlignment.CENTER;
    }

    protected boolean isCaptionVertical(FormInstanceContext context) {
        Boolean captionVertical = getCaptionVertical();
        if(captionVertical != null)
            return captionVertical;

        return isDefaultCaptionVertical(context);
    }

    protected boolean isCaptionLast(FormInstanceContext context) {
        Boolean captionLast = getCaptionLast();
        if(captionLast != null)
            return captionLast;

        return isDefaultCaptionLast(context);
    }

    protected FlexAlignment getCaptionAlignmentHorz(FormInstanceContext context) {
        FlexAlignment captionAlignmentHorz = getCaptionAlignmentHorz();
        if(captionAlignmentHorz != null)
            return captionAlignmentHorz;

        return getDefaultCaptionAlignmentHorz(context);
    }

    protected FlexAlignment getCaptionAlignmentVert(FormInstanceContext context) {
        FlexAlignment captionAlignmentVert = getCaptionAlignmentVert();
        if(captionAlignmentVert != null)
            return captionAlignmentVert;

        return getDefaultCaptionAlignmentVert(context);
    }

    public String getElementClass(FormInstanceContext context) {
        String elementClass = getElementClass();
        if(elementClass != null)
            return elementClass;

        return getDefaultElementClass(context);
    }

    protected String getDefaultElementClass(FormInstanceContext context) {
        return null;
    }

    /* should be set when the component is shrinked automatically inside and can not overflow (it's needed for shadows, because sometimes they overflow, but should not be clipped) */
    /* however sometimes it's not possible to shrink component to zero (input, select, etc.), but we'll ignore this */
    public boolean isShrinkOverflowVisible(FormInstanceContext context) {
        return isDefaultShrinkOverflowVisible(context);
    }

    public boolean isDefaultShrinkOverflowVisible(FormInstanceContext context) {
        return false;
    }

    public double getFlex(FormInstanceContext context) {
        Double flex = getFlex();
        if (flex != null)
            return flex;

        ContainerView container = getLayoutParamContainer();
        if (container != null && container.isTabbed()) {
            return 1;
        }
        return getDefaultFlex(context);
    }

    protected double getDefaultFlex(FormInstanceContext context) {
        return 0;
    }

    public FlexAlignment getAlignment(FormInstanceContext context) {
        FlexAlignment alignment = getAlignment();
        if (alignment != null)
            return alignment;

        ContainerView container = getLayoutParamContainer();
        if (container != null && container.isTabbed()) {
            return FlexAlignment.STRETCH;
        }
        return getDefaultAlignment(context);
    }

    protected FlexAlignment getDefaultAlignment(FormInstanceContext context) {
        return FlexAlignment.START;
    }

    public boolean isShrink(FormInstanceContext context) {
        return isShrink(context, false);
    }
    // second parameter is needed to break the recursion in container default heuristics
    public boolean isShrink(FormInstanceContext context, boolean explicit) {
        Boolean shrink = getShrink();
        if(shrink != null)
            return shrink;
        return isDefaultShrink(context, explicit);
    }

    protected boolean isDefaultShrink(FormInstanceContext context, boolean explicit) {
        return false;
    }

    public boolean isAlignShrink(FormInstanceContext context) {
        return isAlignShrink(context, false);
    }
    // second parameter is needed to break the recursion in container default heuristics
    public boolean isAlignShrink(FormInstanceContext context, boolean explicit) {
        Boolean alignShrink = getAlignShrink();
        if(alignShrink != null)
            return alignShrink;

        FlexAlignment alignment = getAlignment(context);
        if(alignment == FlexAlignment.STRETCH)
            return true;

        return isDefaultAlignShrink(context, explicit);
    }

    protected boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        return false;
    }

    public String getOverflowHorz(FormInstanceContext context) {
        String overflowHorz = getOverflowHorz();
        if(overflowHorz != null) {
            return overflowHorz;
        }

        if(isShrinkOverflowVisible(context))
            return "visible";

        return "auto";
    }

    public String getOverflowVert(FormInstanceContext context) {
        String overflowVert = getOverflowVert();
        if(overflowVert != null) {
            return overflowVert;
        }

        if(isShrinkOverflowVisible(context))
            return "visible";

        return "auto";
    }

    public PropertyObjectEntity<?> getShowIf() {
        return showIf;
    }

    private Property<?> activeTab;

    public Property<?> getActiveTab() {
        if (activeTab == null) {
            activeTab = PropertyFact.createDataPropRev("ACTIVE TAB", this, LogicalClass.instance);
        }
        return activeTab;
    }

    public void updateActiveTabProperty(DataSession session, Boolean value) throws SQLException, SQLHandledException {
        if(activeTab != null)
            activeTab.change(session, value);
    }

    public ComponentView() {
    }

    public ComponentView(int ID) {
        this.ID = ID;
    }

    public void setPropertyElementClass(PropertyObjectEntity<?> propertyElementClass) {
        this.propertyElementClass = propertyElementClass;
    }

    public void setSize(Dimension size, Version version) {
        this.setWidth(size.width, version);
        this.setHeight(size.height, version);
    }

    public void setMargin(int margin, Version version) {
        setMarginTop(margin, version);
        setMarginBottom(margin, version);
        setMarginLeft(margin, version);
        setMarginRight(margin, version);
    }

    public void setShowIf(PropertyObjectEntity<?> showIf) {
        this.showIf = showIf;
    }

    public ComponentView findById(int id) {
        if(ID==id)
            return this;
        return null;
    }

    protected NFProperty<ContainerView> container = NFFact.property();
    public ContainerView getContainer() {
        return container.get();
    }
    public ContainerView getNFContainer(Version version) {
        return container.getNF(version);
    }

    public ComponentView getHiddenContainer() { // when used for hidden optimization
        return getContainer();
    }
    public ContainerView getLayoutParamContainer() { // for using in default layouting parameters
        return getContainer();
    }

    @IdentityLazy
    public boolean isDesignHidden() {
        ComponentView parent = getHiddenContainer();
        if(parent == null)
            return true;
        if(parent.isMain())
            return false;
        return parent.isDesignHidden();

    }
    @IdentityLazy
    public ComponentView getDynamicHidableContainer() { // show if or user hideable
        if(isMain())
            return null;

        if(isDynamicHidable())
            return this;

        return getHiddenContainer().getDynamicHidableContainer();
    }

    @IdentityLazy
    public ComponentView getUserHidableContainer() {
        if (isMain())
            return null;

        if(isUserHidable())
            return this;

        return getHiddenContainer().getUserHidableContainer();
    }

    @IdentityLazy
    public ComponentView getShowIfHidableContainer() {
        if (isMain())
            return null;

        if(isShowIfHidable())
            return this;

        return getHiddenContainer().getShowIfHidableContainer();
    }

    public boolean isMain() {
        return this instanceof ContainerView && ((ContainerView) this).main;
    }

    public boolean isDynamicHidable() {
        return isShowIfHidable() || isUserHidable();
    }

    public boolean isShowIfHidable() {
        return this.showIf != null;
    }

    public boolean isUserHidable() {
        ComponentView parent = getHiddenContainer();
        assert parent != null;
        if (parent instanceof ContainerView && ((ContainerView) parent).isTabbed())
            return true;

        if (this instanceof ContainerView && ((ContainerView) this).isCollapsible())
            return true;

        return false;
    }

    protected boolean hasPropertyComponent() {
        return showIf != null || propertyElementClass != null;
    }

    public void fillPropertyComponents(MExclSet<ComponentView> mComponents) {
        if(hasPropertyComponent())
            mComponents.exclAdd(this);
    }

    public boolean isAncestorOf(ComponentView container) {
        return equals(container);
    }

    public boolean isNFAncestorOf(ComponentView container, Version version) {
        return equals(container);
    }

    void setContainer(ContainerView container, Version version) {
        this.container.set(container, version);
    }

    public void removeFromParent(Version version) {
        ContainerView nf = getNFContainer(version);
        if(nf != null) {
            nf.children.remove(this, version);
            setContainer(null, version);
        }
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, getFont());
        pool.writeObject(outStream, getCaptionFont());
        pool.writeObject(outStream, getBackground());
        pool.writeObject(outStream, getForeground());
        pool.serializeObject(outStream, getContainer());

        pool.writeString(outStream, getElementClass(pool.context));

        outStream.writeInt(getWidth(pool.context));
        outStream.writeInt(getHeight(pool.context));

        outStream.writeInt(getSpan());

        outStream.writeDouble(getFlex(pool.context));
        pool.writeObject(outStream, getAlignment(pool.context));
        outStream.writeBoolean(isShrink(pool.context));
        outStream.writeBoolean(isAlignShrink(pool.context));
        pool.writeObject(outStream, getAlignCaption());
        pool.writeString(outStream, getOverflowHorz(pool.context));
        pool.writeString(outStream, getOverflowVert(pool.context));

        outStream.writeInt(getMarginTop());
        outStream.writeInt(getMarginBottom());
        outStream.writeInt(getMarginLeft());
        outStream.writeInt(getMarginRight());

        outStream.writeBoolean(isCaptionVertical(pool.context));
        outStream.writeBoolean(isCaptionLast(pool.context));
        pool.writeObject(outStream, getCaptionAlignmentHorz(pool.context));
        pool.writeObject(outStream, getCaptionAlignmentVert(pool.context));

        outStream.writeBoolean(isDefaultComponent());

        pool.writeString(outStream, sID);
    }

    public String getElementClass() {
        return elementClass.get();
    }
    public void setElementClass(String value, Version version) {
        elementClass.set(value, version);
    }

    public Integer getWidth() {
        return width.get();
    }
    public Integer getWidthNF(Version version) {
        return width.getNF(version);
    }
    public void setWidth(Integer value, Version version) {
        width.set(value, version);
    }

    public Integer getHeight() {
        return height.get();
    }
    public Integer getHeightNF(Version version) {
        return height.getNF(version);
    }
    public void setHeight(Integer value, Version version) {
        height.set(value, version);
    }

    public Integer getSpan() {
        return nvl(span.get(), 1);
    }
    public void setSpan(Integer value, Version version) {
        span.set(value, version);
    }

    public boolean isDefaultComponent() {
        return nvl(defaultComponent.get(), false);
    }
    public void setDefaultComponent(Boolean value, Version version) {
        defaultComponent.set(value, version);
    }

    public boolean isActivated() {
        return nvl(activated.get(), false);
    }
    public void setActivated(Boolean value, Version version) {
        activated.set(value, version);
    }

    public Double getFlex() {
        return flex.get();
    }
    public void setFlex(Double value, Version version) {
        flex.set(value, version);
    }

    public FlexAlignment getAlignment() {
        return alignment.get();
    }
    public void setAlignment(FlexAlignment value, Version version) {
        alignment.set(value, version);
    }

    public Boolean getShrink() {
        return shrink.get();
    }
    public void setShrink(Boolean value, Version version) {
        shrink.set(value, version);
    }

    public Boolean getAlignShrink() {
        return alignShrink.get();
    }
    public void setAlignShrink(Boolean value, Version version) {
        alignShrink.set(value, version);
    }

    public Boolean getAlignCaption() {
        return alignCaption.get();
    }
    public void setAlignCaption(Boolean value, Version version) {
        alignCaption.set(value, version);
    }

    public String getOverflowHorz() {
        return overflowHorz.get();
    }
    public void setOverflowHorz(String value, Version version) {
        overflowHorz.set(value, version);
    }

    public String getOverflowVert() {
        return overflowVert.get();
    }
    public void setOverflowVert(String value, Version version) {
        overflowVert.set(value, version);
    }

    public Boolean getCaptionVertical() {
        return captionVertical.get();
    }
    public void setCaptionVertical(Boolean value, Version version) {
        captionVertical.set(value, version);
    }

    public Boolean getCaptionLast() {
        return captionLast.get();
    }
    public void setCaptionLast(Boolean value, Version version) {
        captionLast.set(value, version);
    }

    public FlexAlignment getCaptionAlignmentHorz() {
        return captionAlignmentHorz.get();
    }
    public void setCaptionAlignmentHorz(FlexAlignment value, Version version) {
        captionAlignmentHorz.set(value, version);
    }

    public FlexAlignment getCaptionAlignmentVert() {
        return captionAlignmentVert.get();
    }
    public void setCaptionAlignmentVert(FlexAlignment value, Version version) {
        captionAlignmentVert.set(value, version);
    }

    public int getMarginTop() {
        return nvl(marginTop.get(), 0);
    }
    public void setMarginTop(int value, Version version) {
        marginTop.set(max(0, value), version);
    }

    public int getMarginBottom() {
        return nvl(marginBottom.get(), 0);
    }
    public void setMarginBottom(int value, Version version) {
        marginBottom.set(max(0, value), version);
    }

    public int getMarginLeft() {
        return nvl(marginLeft.get(), 0);
    }
    public void setMarginLeft(int value, Version version) {
        marginLeft.set(max(0, value), version);
    }

    public int getMarginRight() {
        return nvl(marginRight.get(), 0);
    }
    public void setMarginRight(int value, Version version) {
        marginRight.set(max(0, value), version);
    }

    public FontInfo getFont() {
        return font.get();
    }
    public FontInfo getFontNF(Version version) {
        return font.getNF(version);
    }
    public void setFont(FontInfo value, Version version) {
        font.set(value, version);
    }

    public FontInfo getCaptionFont() {
        return captionFont.get();
    }
    public void setCaptionFont(FontInfo value, Version version) {
        captionFont.set(value, version);
    }

    public Color getBackground() {
        return background.get();
    }
    public void setBackground(Color value, Version version) {
        background.set(value, version);
    }

    public Color getForeground() {
        return foreground.get();
    }
    public void setForeground(Color value, Version version) {
        foreground.set(value, version);
    }

    public void finalizeAroundInit() {
        container.finalizeChanges();
    }

    public void prereadAutoIcons(FormView formView, ConnectionContext context) {
    }

    // copy-constructor
    public ComponentView(ComponentView src, ObjectMapping mapping) {
        super(src);

        mapping.put(src, this);

        elementClass.set(src.elementClass.get(), mapping.version);

        width.set(src.width.get(), mapping.version);
        height.set(src.height.get(), mapping.version);

        span.set(src.span.get(), mapping.version);

        defaultComponent.set(src.defaultComponent.get(), mapping.version);
        activated.set(src.activated.get(), mapping.version);

        flex.set(src.flex.get(), mapping.version);
        alignment.set(src.alignment.get(), mapping.version);
        shrink.set(src.shrink.get(), mapping.version);
        alignShrink.set(src.alignShrink.get(), mapping.version);
        alignCaption.set(src.alignCaption.get(), mapping.version);
        overflowHorz.set(src.overflowHorz.get(), mapping.version);
        overflowVert.set(src.overflowVert.get(), mapping.version);

        captionVertical.set(src.captionVertical.get(), mapping.version);
        captionLast.set(src.captionLast.get(), mapping.version);
        captionAlignmentHorz.set(src.captionAlignmentHorz.get(), mapping.version);
        captionAlignmentVert.set(src.captionAlignmentVert.get(), mapping.version);

        marginTop.set(src.marginTop.get(), mapping.version);
        marginBottom.set(src.marginBottom.get(), mapping.version);
        marginLeft.set(src.marginLeft.get(), mapping.version);
        marginRight.set(src.marginRight.get(), mapping.version);

        font.set(src.font.get(), mapping.version);
        captionFont.set(src.captionFont.get(), mapping.version);
        background.set(src.background.get(), mapping.version);
        foreground.set(src.foreground.get(), mapping.version);

        activeTab = src.activeTab;

        propertyElementClass = mapping.get(src.propertyElementClass);
        showIf = mapping.get(src.showIf);
        container.set(src.container, mapping::get, mapping.version);
    }
}
