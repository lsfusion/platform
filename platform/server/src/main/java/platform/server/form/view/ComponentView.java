package platform.server.form.view;

import platform.base.identity.IdentityObject;
import platform.interop.ComponentDesign;
import platform.interop.form.layout.AbstractComponent;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.caches.IdentityLazy;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ComponentView extends IdentityObject implements ServerIdentitySerializable, AbstractComponent<ContainerView, ComponentView> {

    public ComponentDesign design = new ComponentDesign();

    protected ContainerView container;

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    public SimplexConstraints<ComponentView> constraints = getDefaultConstraints();

    public boolean defaultComponent = false;

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

    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return new SimplexConstraints<ComponentView>();
    }

    public SimplexConstraints<ComponentView> getConstraints() {
        return constraints;
    }

    public ComponentView findById(int id) {
        if(ID==id)
            return this;
        return null;
    }

    public ContainerView getContainer() {
        return container;
    }

    @IdentityLazy
    public ComponentView getTabContainer() {
        ContainerView parent = getContainer();
        if(parent == null)
            return null;
        if(parent.type == ContainerType.TABBED_PANE)
            return this;
        return parent.getTabContainer();
    }

    public boolean isAncestorOf(ComponentView container) {
        return equals(container);
    }

    void setContainer(ContainerView container) {
        this.container = container;
    }

    public boolean removeFromParent() {
        return container != null && container.remove(this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container, serializationType);

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        pool.writeObject(outStream, constraints);
        pool.writeString(outStream, sID);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ComponentView, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            pool.serializeObject(outStream, intersect.getKey(), serializationType);
            pool.writeObject(outStream, intersect.getValue());
        }
        outStream.writeBoolean(defaultComponent);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        constraints = pool.readObject(inStream);
        sID = pool.readString(inStream);

        constraints.intersects = new HashMap<ComponentView, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ComponentView view = pool.deserializeObject(inStream);
            DoNotIntersectSimplexConstraint constraint = pool.readObject(inStream);
            constraints.intersects.put(view, constraint);
        }

        defaultComponent = inStream.readBoolean();
    }
}
