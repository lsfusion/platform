package platform.server.form.view;

import platform.base.identity.IdentityObject;
import platform.interop.ComponentDesign;
import platform.interop.form.layout.AbstractComponent;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.form.entity.GroupObjectEntity;
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

    public void setFixedSize(Dimension size) {
        minimumSize = size;
        maximumSize = size;
        preferredSize = size;
    }

    public SimplexConstraints<ComponentView> constraints = getDefaultConstraints();

    public GroupObjectEntity keyBindingGroup = null;

    public boolean drawToToolbar;

    public SimplexConstraints<ComponentView> getDefaultConstraints() {
        return new SimplexConstraints<ComponentView>();
    }

    public SimplexConstraints<ComponentView> getConstraints() {
        return constraints;
    }

    public boolean defaultComponent = false;

    public ComponentView() {

    }

    public ComponentView(int ID) {
        this.ID = ID;
    }

    public ContainerView getContainer() {
        return container;
    }

    void setContainer(ContainerView container) {
        this.container = container;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container, serializationType);

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        pool.writeObject(outStream, constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ComponentView, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            pool.serializeObject(outStream, intersect.getKey(), serializationType);
            pool.writeObject(outStream, intersect.getValue());
        }
        outStream.writeBoolean(defaultComponent);
        pool.serializeObject(outStream, pool.context.view.getGroupObject(keyBindingGroup));
        outStream.writeBoolean(drawToToolbar);
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        constraints = pool.readObject(inStream);

        constraints.intersects = new HashMap<ComponentView, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ComponentView view = pool.deserializeObject(inStream);
            DoNotIntersectSimplexConstraint constraint = pool.readObject(inStream);
            constraints.intersects.put(view, constraint);
        }

        defaultComponent = inStream.readBoolean();

        GroupObjectView keyBindingGroupView = pool.deserializeObject(inStream);
        if (keyBindingGroupView != null) {
            keyBindingGroup = keyBindingGroupView.entity;
        }

        drawToToolbar = inStream.readBoolean();
    }
}
