package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.AbstractComponent;
import lsfusion.interop.form.design.ComponentDesign;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import static java.lang.Math.max;

public class ComponentView extends IdentityObject implements ServerIdentitySerializable, AbstractComponent {

    @Override
    public String toString() {
        return getSID();
    }

    public ComponentDesign design = new ComponentDesign();

    public PropertyObjectEntity propertyElementClass;
    public String elementClass;

    public Integer width;
    public Integer height;

    public int span = 1;

    protected Double flex = null;
    private FlexAlignment alignment = null;
    protected Boolean shrink = null;
    protected Boolean alignShrink = null;
    protected Boolean alignCaption = null;
    protected String overflowHorz;
    protected String overflowVert;

    public Boolean panelCaptionVertical;
    public Boolean panelCaptionLast;
    public FlexAlignment panelCaptionAlignment;

    public PropertyObjectEntity<?> showIf;

    public int getWidth(FormInstanceContext context) {
        if(width != null)
            return width;

        return getDefaultWidth(context);
    }

    public int getHeight(FormInstanceContext context) {
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

    protected boolean isDefaultPanelCaptionVertical(FormInstanceContext context) {
        return true;
    }

    protected boolean isDefaultPanelCaptionLast(FormInstanceContext context) {
        return false;
    }

    protected FlexAlignment getDefaultPanelCaptionAlignment(FormInstanceContext context) {
        if(!isPanelCaptionVertical(context))
           return FlexAlignment.CENTER;

        return FlexAlignment.START;
    }

    protected boolean isPanelCaptionVertical(FormInstanceContext context) {
        if(panelCaptionVertical != null)
            return panelCaptionVertical;

        return isDefaultPanelCaptionVertical(context);
    }

    protected boolean isPanelCaptionLast(FormInstanceContext context) {
        if(panelCaptionLast != null)
            return panelCaptionLast;

        return isDefaultPanelCaptionLast(context);
    }

    protected FlexAlignment getPanelCaptionAlignment(FormInstanceContext context) {
        if(panelCaptionAlignment != null)
            return panelCaptionAlignment;

        return getDefaultPanelCaptionAlignment(context);
    }

    public String getElementClass(FormInstanceContext context) {
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
        if(overflowHorz != null) {
            return overflowHorz;
        }

        if(isShrinkOverflowVisible(context))
            return "visible";

        return null;
    }

    public String getOverflowVert(FormInstanceContext context) {
        if(overflowVert != null) {
            return overflowVert;
        }

        if(isShrinkOverflowVisible(context))
            return "visible";

        return null;
    }

    public Boolean getAlignCaption() {
        return alignCaption;
    }

    public PropertyObjectEntity<?> getShowIf() {
        return showIf;
    }

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public boolean defaultComponent = false;

    public boolean activated; // as tab

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

    public void setFlex(double flex) {
        this.flex = flex;
    }

    public void setShrink(boolean shrink) {
        this.shrink = shrink;
    }

    public void setAlignShrink(boolean alignShrink) {
        this.alignShrink = alignShrink;
    }

    public void setAlignment(FlexAlignment alignment) {
        this.alignment = alignment;
    }

    public void setElementClass(String elementClass) {
        this.elementClass = elementClass;
    }

    public void setPropertyElementClass(PropertyObjectEntity<?> propertyElementClass) {
        this.propertyElementClass = propertyElementClass;
    }

    public void setAlignCaption(boolean alignCaption) {
        this.alignCaption = alignCaption;
    }

    public void setOverflowHorz(String overflowHorz) {
        this.overflowHorz = overflowHorz;
    }

    public void setOverflowVert(String overflowVert) {
        this.overflowVert = overflowVert;
    }

    public void setSize(Dimension size) {
        this.width = size.width;
        this.height = size.height;
    }

    public void setHeight(int prefHeight) {
        this.height = prefHeight;
    }

    public void setWidth(int prefWidth) {
        this.width = prefWidth;
    }

    public void setMarginTop(int marginTop) {
        this.marginTop = max(0, marginTop);
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = max(0, marginBottom);
    }

    public void setMarginLeft(int marginLeft) {
        this.marginLeft = max(0, marginLeft);
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = max(0, marginRight);
    }

    public void setMargin(int margin) {
        setMarginTop(margin);
        setMarginBottom(margin);
        setMarginLeft(margin);
        setMarginRight(margin);
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
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, getContainer());

        pool.writeString(outStream, getElementClass(pool.context));

        outStream.writeInt(getWidth(pool.context));
        outStream.writeInt(getHeight(pool.context));

        outStream.writeInt(span);

        outStream.writeDouble(getFlex(pool.context));
        pool.writeObject(outStream, getAlignment(pool.context));
        outStream.writeBoolean(isShrink(pool.context));
        outStream.writeBoolean(isAlignShrink(pool.context));
        pool.writeObject(outStream, alignCaption);
        pool.writeString(outStream, getOverflowHorz(pool.context));
        pool.writeString(outStream, getOverflowVert(pool.context));

        outStream.writeInt(marginTop);
        outStream.writeInt(marginBottom);
        outStream.writeInt(marginLeft);
        outStream.writeInt(marginRight);

        outStream.writeBoolean(isPanelCaptionVertical(pool.context));
        outStream.writeBoolean(isPanelCaptionLast(pool.context));
        pool.writeObject(outStream, getPanelCaptionAlignment(pool.context));

        outStream.writeBoolean(defaultComponent);

        pool.writeString(outStream, sID);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = NFFact.finalProperty(pool.deserializeObject(inStream));

        width = pool.readInt(inStream);
        height = pool.readInt(inStream);

        span = inStream.readInt();

        flex = inStream.readDouble();
        alignment = pool.readObject(inStream);
        shrink = inStream.readBoolean();
        alignShrink = inStream.readBoolean();
        alignCaption = pool.readObject(inStream);
        overflowHorz = pool.readString(inStream);
        overflowVert = pool.readString(inStream);
        marginTop = inStream.readInt();
        marginBottom = inStream.readInt();
        marginLeft = inStream.readInt();
        marginRight = inStream.readInt();

        defaultComponent = inStream.readBoolean();

        sID = pool.readString(inStream);
    }

    public void finalizeAroundInit() {
        container.finalizeChanges();
    }

    public void prereadAutoIcons(FormView formView, ConnectionContext context) {
    }
}
