package lsfusion.server.form.view;

import lsfusion.base.col.MapFact;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.ComponentDesign;
import lsfusion.interop.form.layout.AbstractComponent;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFProperty;
import lsfusion.server.logics.property.CalcPropertyRevImplement;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.serialization.ServerIdentitySerializable;
import lsfusion.server.serialization.ServerSerializationPool;
import lsfusion.server.session.DataSession;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import static java.lang.Math.max;

public class ComponentView extends IdentityObject implements ServerIdentitySerializable, AbstractComponent<ContainerView, ComponentView> {

    public ComponentDesign design = new ComponentDesign();

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    public double flex = 0;
    public FlexAlignment alignment = FlexAlignment.LEADING;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public boolean defaultComponent = false;

    private CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> tabActive;

    public CalcPropertyRevImplement<ClassPropertyInterface, ObjectEntity> getTabActive() {
        if (tabActive == null) {
            tabActive = DerivedProperty.createDataPropRev(this.toString(), MapFact.<ObjectEntity, ValueClass>EMPTY(), LogicalClass.instance);
        }
        return tabActive;
    }

    public void updateTabActiveProperty(DataSession session, Boolean value) throws SQLException, SQLHandledException {
        if(tabActive != null)
            tabActive.property.change(session, value);
    }

    public ComponentView() {
    }

    public ComponentView(int ID) {
        this.ID = ID;
    }

    public void setFixedSize(Dimension size) {
        minimumSize = size;
        maximumSize = size;
        preferredSize = size;
    }

    public void setFlex(double flex) {
        this.flex = flex;
    }

    public void setAlignment(FlexAlignment alignment) {
        this.alignment = alignment;
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

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, getContainer(), serializationType);

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        outStream.writeDouble(flex);
        pool.writeObject(outStream, alignment);
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

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

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
