package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.MapFact;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.design.ComponentDesign;
import lsfusion.interop.form.layout.AbstractComponent;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.logics.classes.LogicalClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.property.implement.CalcPropertyRevImplement;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.form.interactive.serialization.ServerIdentitySerializable;
import lsfusion.server.logics.form.interactive.serialization.ServerSerializationPool;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.LocalNestedType;

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

    public Dimension size;
    
    public boolean autoSize = false;

    protected Double flex = null;
    private FlexAlignment alignment = null;

    public Dimension getSize() {
        if(size == null) {
            ContainerView container = getContainer();
            if(container != null && container.isScroll()) {
                return new Dimension(-1, 1);
            }
        }
        return size;
    }
    
    public double getFlex(FormEntity formEntity) {
        ContainerView container = getContainer();
        if (container != null) {
            if (container.isScroll() || container.isSplit())
                return flex != null && flex != 0 ? flex : 1;
        }

        if (flex != null)
            return flex;

        return getDefaultFlex(formEntity);
    }

    public double getDefaultFlex(FormEntity formEntity) {
        ContainerView container = getContainer();
        if (container != null) {
            if (container.isTabbedPane())
                return 1;
        }
        return getBaseDefaultFlex(formEntity);
    }
    public double getBaseDefaultFlex(FormEntity formEntity) {
        return 0;
    }

    public FlexAlignment getAlignment(FormEntity formEntity) {
        if (alignment != null)
            return alignment;
        
        return getDefaultAlignment(formEntity);
    }

    public FlexAlignment getDefaultAlignment(FormEntity formEntity) {
        ContainerView container = getContainer();
        if (container != null) {
            if ((container.isScroll() || container.isSplit() || container.isTabbedPane()))
                return FlexAlignment.STRETCH;
        }
        return getBaseDefaultAlignment(formEntity);
    }
    public FlexAlignment getBaseDefaultAlignment(FormEntity formEntity) {
        return FlexAlignment.START;
    }

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public boolean defaultComponent = false;

    private CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> activeTab;

    public CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> getActiveTab() {
        if (activeTab == null) {
            activeTab = DerivedProperty.createDataPropRev(LocalizedString.create(this.toString()), MapFact.<ObjectEntity, ValueClass>EMPTY(), LogicalClass.instance, LocalNestedType.ALL);
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

    public void setAlignment(FlexAlignment alignment) {
        this.alignment = alignment;
    }

    public void setSize(Dimension size) {
        this.size = size;
    }

    public void setHeight(int prefHeight) {
        if (this.size == null) {
            this.size = new Dimension(-1, prefHeight);
        } else {
            this.size.height = prefHeight;
        }
    }

    public void setWidth(int prefWidth) {
        if (this.size == null) {
            this.size = new Dimension(prefWidth, -1);
        } else {
            this.size.width = prefWidth;
        }
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

    @IdentityLazy
    public boolean isDesignHidden() {
        ContainerView parent = getContainer();
        if(parent == null)
            return true;
        if(parent.main)
            return false;
        return parent.isDesignHidden();

    }
    @IdentityLazy
    public ComponentView getLocalHideableContainer() { // show if or tabbed
        ContainerView parent = getContainer();
        assert parent != null; // эквивалентно !isDesignHidden();
        if(parent.main)
            return null;
        if(parent.showIf != null)
            return parent;
        if(parent.isTabbedPane())
            return this;
        return parent.getLocalHideableContainer();
    }

    @IdentityLazy
    public ComponentView getTabContainer() {
        ContainerView parent = getContainer();
        // assert !isNotTabHidden - то есть design + showif не hidden
        assert parent != null; // частный случай (!isDesignHidden) верхнего assertion'а
        if(parent.main)
            return null;
        if(parent.isTabbedPane())
            return this;
        return parent.getTabContainer();
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

    public boolean removeFromParent(Version version) {
        ContainerView nf = container.getNF(version);
        return nf != null && nf.remove(this, version);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, getContainer());

        pool.writeObject(outStream, getSize());
        
        outStream.writeBoolean(autoSize);

        outStream.writeDouble(getFlex(pool.context.entity));
        pool.writeObject(outStream, getAlignment(pool.context.entity));

        outStream.writeInt(marginTop);
        outStream.writeInt(marginBottom);
        outStream.writeInt(marginLeft);
        outStream.writeInt(marginRight);

        outStream.writeBoolean(defaultComponent);

        pool.writeString(outStream, sID);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = NFFact.finalProperty(pool.<ContainerView>deserializeObject(inStream));

        size = pool.readObject(inStream);
        
        autoSize = inStream.readBoolean();

        flex = inStream.readDouble();
        alignment = pool.readObject(inStream);
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
