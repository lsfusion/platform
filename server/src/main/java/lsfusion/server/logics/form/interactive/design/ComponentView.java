package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.ServerIdentityObject;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import static java.lang.Math.max;
import static lsfusion.base.BaseUtils.nvl;

public abstract class ComponentView<This extends ComponentView<This, AddParent>, AddParent extends ServerIdentityObject<AddParent, ?>> extends IdentityView<This, AddParent> {

    protected NFProperty<String> elementClass = NFFact.property();
    protected NFProperty<PropertyObjectEntity> propertyElementClass = NFFact.property();

    protected NFProperty<Integer> width = NFFact.property();
    protected NFProperty<Integer> height = NFFact.property();

    protected NFProperty<Integer> span = NFFact.property();

    protected NFProperty<Boolean> defaultComponent = NFFact.property();
    protected NFProperty<Boolean> activated = NFFact.property();

    protected NFProperty<Double> flex = NFFact.property();
    protected NFProperty<FlexAlignment> alignment = NFFact.property();
    protected NFProperty<Boolean> shrink = NFFact.property();
    protected NFProperty<Boolean> alignShrink = NFFact.property();
    protected NFProperty<Boolean> alignCaption = NFFact.property();
    protected NFProperty<String> overflowHorz = NFFact.property();
    protected NFProperty<String> overflowVert = NFFact.property();

    protected NFProperty<Boolean> captionVertical = NFFact.property();
    protected NFProperty<Boolean> captionLast = NFFact.property();
    protected NFProperty<FlexAlignment> captionAlignmentHorz = NFFact.property();
    protected NFProperty<FlexAlignment> captionAlignmentVert = NFFact.property();

    protected NFProperty<Integer> marginTop = NFFact.property();
    protected NFProperty<Integer> marginBottom = NFFact.property();
    protected NFProperty<Integer> marginLeft = NFFact.property();
    protected NFProperty<Integer> marginRight = NFFact.property();

    protected NFProperty<FontInfo> font = NFFact.property();
    protected NFProperty<FontInfo> captionFont = NFFact.property();
    protected NFProperty<Color> background = NFFact.property();
    protected NFProperty<Color> foreground = NFFact.property();

    protected NFProperty<PropertyObjectEntity> showIf = NFFact.property();

    protected NFProperty<ContainerView> container = NFFact.property();
    public final NFProperty<Boolean> defaultContainer = NFFact.property();

    @Override
    public String toString() {
        return getSID();
    }

    public int getWidth(FormInstanceContext context) {
        Integer width = getWidth();
        if (width != null)
            return width;

        return getDefaultWidth(context);
    }

    public int getHeight(FormInstanceContext context) {
        Integer height = getHeight();
        if (height != null)
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
        if (captionVertical != null)
            return captionVertical;

        return isDefaultCaptionVertical(context);
    }

    protected boolean isCaptionLast(FormInstanceContext context) {
        Boolean captionLast = getCaptionLast();
        if (captionLast != null)
            return captionLast;

        return isDefaultCaptionLast(context);
    }

    protected FlexAlignment getCaptionAlignmentHorz(FormInstanceContext context) {
        FlexAlignment captionAlignmentHorz = getCaptionAlignmentHorz();
        if (captionAlignmentHorz != null)
            return captionAlignmentHorz;

        return getDefaultCaptionAlignmentHorz(context);
    }

    protected FlexAlignment getCaptionAlignmentVert(FormInstanceContext context) {
        FlexAlignment captionAlignmentVert = getCaptionAlignmentVert();
        if (captionAlignmentVert != null)
            return captionAlignmentVert;

        return getDefaultCaptionAlignmentVert(context);
    }

    public String getElementClass(FormInstanceContext context) {
        String elementClass = getElementClass();
        if (elementClass != null)
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
        if (shrink != null)
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
        if (alignShrink != null)
            return alignShrink;

        FlexAlignment alignment = getAlignment(context);
        if (alignment == FlexAlignment.STRETCH)
            return true;

        return isDefaultAlignShrink(context, explicit);
    }

    protected boolean isDefaultAlignShrink(FormInstanceContext context, boolean explicit) {
        return false;
    }

    public String getOverflowHorz(FormInstanceContext context) {
        String overflowHorz = getOverflowHorz();
        if (overflowHorz != null) {
            return overflowHorz;
        }

        if (isShrinkOverflowVisible(context))
            return "visible";

        return "auto";
    }

    public String getOverflowVert(FormInstanceContext context) {
        String overflowVert = getOverflowVert();
        if (overflowVert != null) {
            return overflowVert;
        }

        if (isShrinkOverflowVisible(context))
            return "visible";

        return "auto";
    }

    protected final FormEntity.ExProperty activeTab;

    public Property<?> getNFActiveTab(Version version) {
        return activeTab.getNF(version);
    }

    public Property<?> getActiveTab() {
        return activeTab.get();
    }

    public void updateActiveTabProperty(DataSession session, Boolean value) throws SQLException, SQLHandledException {
        Property<?> activeTab = getActiveTab();
        if (activeTab != null)
            activeTab.change(session, value);
    }

    protected String sID;

    public String getSID() {
        return sID;
    }

    public void setSID(String sID) {
        this.sID = sID;
    }

    public ComponentView() {
        activeTab = new FormEntity.ExProperty(() -> PropertyFact.createDataPropRev("ACTIVE TAB", this, LogicalClass.instance));
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

    public ComponentView findById(int id) {
        if(getID() == id)
            return this;
        return null;
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
        return this.getShowIf() != null;
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
        return getShowIf() != null || getPropertyElementClass() != null;
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

        pool.writeString(outStream, getSID());
    }

    public String getElementClass() {
        return elementClass.get();
    }
    public void setElementClass(String value, Version version) {
        elementClass.set(value, version);
    }

    public PropertyObjectEntity getPropertyElementClass() {
        return propertyElementClass.get();
    }
    public void setPropertyElementClass(PropertyObjectEntity value, Version version) {
        propertyElementClass.set(value, version);
    }

    public Integer getWidth() {
        return width.get();
    }
    public Integer getNFWidth(Version version) {
        return width.getNF(version);
    }
    public void setWidth(Integer value, Version version) {
        width.set(value, version);
    }

    public Integer getHeight() {
        return height.get();
    }
    public Integer getNFHeight(Version version) {
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
    public FontInfo getNFFont(Version version) {
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

    public PropertyObjectEntity getShowIf() {
        return showIf.get();
    }
    public void setShowIf(PropertyObjectEntity value, Version version) {
        showIf.set(value, version);
    }

    public ContainerView getContainer() {
        return container.get();
    }
    public ContainerView getNFContainer(Version version) {
        return container.getNF(version);
    }

    public void finalizeAroundInit() {
        elementClass.finalizeChanges();
        propertyElementClass.finalizeChanges();
        width.finalizeChanges();
        height.finalizeChanges();
        span.finalizeChanges();
        defaultComponent.finalizeChanges();
        activated.finalizeChanges();
        flex.finalizeChanges();
        alignment.finalizeChanges();
        shrink.finalizeChanges();
        alignShrink.finalizeChanges();
        alignCaption.finalizeChanges();
        overflowHorz.finalizeChanges();
        overflowVert.finalizeChanges();
        captionVertical.finalizeChanges();
        captionLast.finalizeChanges();
        captionAlignmentHorz.finalizeChanges();
        captionAlignmentVert.finalizeChanges();
        marginTop.finalizeChanges();
        marginBottom.finalizeChanges();
        marginLeft.finalizeChanges();
        marginRight.finalizeChanges();
        font.finalizeChanges();
        captionFont.finalizeChanges();
        background.finalizeChanges();
        foreground.finalizeChanges();
        showIf.finalizeChanges();
        container.finalizeChanges();
        defaultContainer.finalizeChanges();
    }

    public void prereadAutoIcons(FormView formView, ConnectionContext context) {
    }

    // copy-constructor
    protected ComponentView(This src, ObjectMapping mapping) {
        super(src, mapping);

        this.sID = src.sID;

        activeTab = mapping.get(src.activeTab);

    }
    @Override
    public void extend(This src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.set(container, src.container);
        mapping.sets(defaultContainer, src.defaultContainer);

        mapping.sets(elementClass, src.elementClass);
        mapping.set(propertyElementClass, src.propertyElementClass);

        mapping.sets(width, src.width);
        mapping.sets(height, src.height);

        mapping.sets(span, src.span);

        mapping.sets(defaultComponent,src.defaultComponent);
        mapping.sets(activated, src.activated);

        mapping.sets(flex, src.flex);
        mapping.sets(alignment, src.alignment);
        mapping.sets(shrink, src.shrink);
        mapping.sets(alignShrink,src.alignShrink);
        mapping.sets(alignCaption,src.alignCaption);
        mapping.sets(overflowHorz,src.overflowHorz);
        mapping.sets(overflowVert,src.overflowVert);

        mapping.sets(captionVertical,src.captionVertical);
        mapping.sets(captionLast,src.captionLast);
        mapping.sets(captionAlignmentHorz,src.captionAlignmentHorz);
        mapping.sets(captionAlignmentVert,src.captionAlignmentVert);

        mapping.sets(marginTop,src.marginTop);
        mapping.sets(marginBottom,src.marginBottom);
        mapping.sets(marginLeft,src.marginLeft);
        mapping.sets(marginRight,src.marginRight);

        mapping.sets(font, src.font);
        mapping.sets(captionFont, src.captionFont);
        mapping.sets(background, src.background);
        mapping.sets(foreground, src.foreground);

        mapping.set(showIf, src.showIf);
    }
    // no add
}
