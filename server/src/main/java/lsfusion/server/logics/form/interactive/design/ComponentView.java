package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.MapFact;
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
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;

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

    public PropertyObjectEntity<?> showIf;

    public int getWidth(FormEntity entity) {
        if(width != null)
            return width;

        return getDefaultWidth(entity);
    }

    public int getHeight(FormEntity entity) {
        if(height != null)
            return height;

        return getDefaultHeight(entity);
    }

    protected int getDefaultWidth(FormEntity entity) {
        return -1;
    }

    protected int getDefaultHeight(FormEntity entity) {
        return -1;
    }

    public double getFlex(FormEntity formEntity) {
        ContainerView container = getLayoutParamContainer();
        if (container != null) {
            if (container.isScroll() || container.isSplit())
                return flex != null && flex != 0 ? flex : 1;
        }

        if (flex != null)
            return flex;

        if (container != null) {
            if (container.isTabbed())
                return 1;
        }
        return getDefaultFlex(formEntity);
    }

    protected double getDefaultFlex(FormEntity formEntity) {
        return 0;
    }

    public FlexAlignment getAlignment(FormEntity formEntity) {
        if (alignment != null)
            return alignment;

        ContainerView container = getLayoutParamContainer();
        if (container != null) {
            if ((container.isScroll() || container.isSplit() || container.isTabbed()))
                return FlexAlignment.STRETCH;
        }
        return getDefaultAlignment(formEntity);
    }

    protected FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.START;
    }

    public boolean isShrink(FormEntity formEntity) {
        return isShrink(formEntity, false);
    }
    // second parameter is needed to break the recursion in container default heuristics
    public boolean isShrink(FormEntity formEntity, boolean explicit) {
        if(shrink != null)
            return shrink;

        ContainerView container = getLayoutParamContainer();
        if(container != null && container.isSplit())
            return true;

        return isDefaultShrink(formEntity, explicit);
    }

    protected boolean isDefaultShrink(FormEntity formEntity, boolean explicit) {
        return false;
    }

    public boolean isAlignShrink(FormEntity formEntity) {
        return isAlignShrink(formEntity, false);
    }
    // second parameter is needed to break the recursion in container default heuristics
    public boolean isAlignShrink(FormEntity formEntity, boolean explicit) {
        if(alignShrink != null)
            return alignShrink;

        ContainerView container = getLayoutParamContainer();
        FlexAlignment alignment = getAlignment(formEntity);
        if(alignment == FlexAlignment.STRETCH && (container == null || !container.isScroll()))
            return true;

        return isDefaultAlignShrink(formEntity, explicit);
    }

    protected boolean isDefaultAlignShrink(FormEntity formEntity, boolean explicit) {
        return false;
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

    private PropertyRevImplement<ClassPropertyInterface, ObjectEntity> activeTab;

    public PropertyRevImplement<ClassPropertyInterface, ObjectEntity> getActiveTab() {
        if (activeTab == null) {
            activeTab = PropertyFact.createDataPropRev(LocalizedString.create(this.toString()), MapFact.EMPTY(), LogicalClass.instance, LocalNestedType.ALL);
        }
        return activeTab;
    }

    public void updateActiveTabProperty(DataSession session, Boolean value) throws SQLException, SQLHandledException {
        if(activeTab != null)
            activeTab.property.change(session, value);
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

        pool.writeString(outStream, elementClass);

        outStream.writeInt(getWidth(pool.context.entity));
        outStream.writeInt(getHeight(pool.context.entity));

        outStream.writeInt(span);

        outStream.writeDouble(getFlex(pool.context.entity));
        pool.writeObject(outStream, getAlignment(pool.context.entity));
        outStream.writeBoolean(isShrink(pool.context.entity));
        outStream.writeBoolean(isAlignShrink(pool.context.entity));
        pool.writeObject(outStream, alignCaption);

        outStream.writeInt(marginTop);
        outStream.writeInt(marginBottom);
        outStream.writeInt(marginLeft);
        outStream.writeInt(marginRight);

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
}
