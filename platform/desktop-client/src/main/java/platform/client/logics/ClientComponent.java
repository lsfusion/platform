package platform.client.logics;

import platform.base.context.ApplicationContext;
import platform.base.context.ContextIdentityObject;
import platform.base.serialization.IdentitySerializable;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ComponentEditor;
import platform.client.descriptor.nodes.ComponentNode;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ComponentDesign;
import platform.interop.form.layout.AbstractComponent;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class ClientComponent extends ContextIdentityObject implements Serializable, IdentitySerializable<ClientSerializationPool>, AbstractComponent<ClientContainer, ClientComponent> {

    public ComponentDesign design;

    public ClientContainer container;

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    public SimplexConstraints<ClientComponent> constraints;

    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return new SimplexConstraints<ClientComponent>();
    }

    public boolean defaultComponent = false;

    public ClientComponent() {
    }

    public ClientComponent(ApplicationContext context) {
        super(context);
        initAggregateObjects(context);
    }

    public ClientComponent(int ID, ApplicationContext context) {
        super(ID, context);
        initAggregateObjects(context);
    }

    protected void initAggregateObjects(ApplicationContext context) {
        constraints = getDefaultConstraints();
        constraints.setContext(context);
        design = new ComponentDesign(context);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        pool.writeObject(outStream, constraints);
        pool.writeString(outStream, sID);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ClientComponent, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            pool.serializeObject(outStream, intersect.getKey());
            pool.writeObject(outStream, intersect.getValue());
        }

        outStream.writeBoolean(defaultComponent);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        constraints = pool.readObject(inStream);
        sID = pool.readString(inStream);

        constraints.intersects = new HashMap<ClientComponent, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientComponent view = pool.deserializeObject(inStream);
            DoNotIntersectSimplexConstraint constraint = pool.readObject(inStream);
            constraints.intersects.put(view, constraint);
        }

        defaultComponent = inStream.readBoolean();
    }

    public ComponentNode getNode() {
        return new ComponentNode(this);
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    public SimplexConstraints<ClientComponent> getConstraints() {
        return constraints;
    }

    public void setConstraints(SimplexConstraints<ClientComponent> constraints) {
        this.constraints = constraints;
        updateDependency(this, "constraints");
    }

    public void setDefaultComponent(boolean defaultComponent) {
        this.defaultComponent = defaultComponent;
        updateDependency(this, "defaultComponent");
    }

    public boolean getDefaultComponent() {
        return defaultComponent;
    }

    public boolean shouldBeDeclared() {
        return !constraints.equals(getDefaultConstraints()) || !design.isDefaultDesign();
    }

    public Map<ClientComponent, DoNotIntersectSimplexConstraint> getIntersects() {
        return constraints.intersects;
    }

    public void setIntersects(Map<ClientComponent, DoNotIntersectSimplexConstraint> intersects) {
        constraints.intersects = intersects;
        updateDependency(this.constraints, "intersects");
    }

    public String getMinimumWidth() {
        return String.valueOf(minimumSize != null ? minimumSize.width : -1);
    }

    private Dimension changeWidth(Dimension dimension, String value) {
        Dimension newDimension = new Dimension(Integer.decode(value), (dimension == null) ? -1 : dimension.height);
        if (newDimension.width == -1 && newDimension.height == -1) {
            return null;
        } else {
            return newDimension;
        }
    }

    private Dimension changeHeight(Dimension dimension, String value) {
        Dimension newDimension = new Dimension((dimension == null) ? -1 : dimension.width, Integer.decode(value));
        if (newDimension.width == -1 && newDimension.height == -1) {
            return null;
        } else {
            return newDimension;
        }
    }

    public void setMinimumWidth(String minimumWidth) {
        minimumSize = changeWidth(minimumSize, minimumWidth);
        updateDependency(this, "minimumWidth");
    }

    public String getMinimumHeight() {
        return String.valueOf(minimumSize != null ? minimumSize.height : -1);
    }

    public void setMinimumHeight(String minimumHeight) {
        minimumSize = changeHeight(minimumSize, minimumHeight);
        updateDependency(this, "minimumHeight");
    }

    public String getMaximumWidth() {
        return String.valueOf(maximumSize != null ? maximumSize.width : -1);
    }

    public void setMaximumWidth(String maximumWidth) {
        maximumSize = changeWidth(maximumSize, maximumWidth);
        updateDependency(this, "maximumWidth");
    }

    public String getMaximumHeight() {
        return String.valueOf(maximumSize != null ? maximumSize.height : -1);
    }

    public void setMaximumHeight(String maximumHeight) {
        maximumSize = changeHeight(maximumSize, maximumHeight);
        updateDependency(this, "maximumHeight");
    }

    public String getPreferredWidth() {
        return String.valueOf(preferredSize != null ? preferredSize.width : -1);
    }

    public void setPreferredWidth(String preferredWidth) {
        preferredSize = changeWidth(preferredSize, preferredWidth);
        updateDependency(this, "preferredWidth");
    }

    public String getPreferredHeight() {
        return String.valueOf(preferredSize != null ? preferredSize.height : -1);
    }

    public void setPreferredHeight(String preferredHeight) {
        preferredSize = changeHeight(preferredSize, preferredHeight);
        updateDependency(this, "preferredHeight");
    }

    public abstract String getCaption();

    public String getCodeConstructor() {
        return "CreateContainer()";
    }

    public abstract String getCodeClass();

    public String getVariableName(FormDescriptor form) {
        String className = getCodeClass();
        if (sID == null || (sID.charAt(sID.length() - 1) > '9')) {
            return className.substring(0, 1).toLowerCase() + className.substring(1, className.length()) + getSID();
        }
        String temp = "";
        int i = sID.length() - 1;

        while (sID.charAt(i) >= '0' && sID.charAt(i) <= '9') {
            temp = sID.charAt(i--) + temp;
        }

        int groupId = Integer.parseInt(temp);
        String name = form.getGroupObject(groupId).getClassNames();
        name = name.substring(0, 1).toUpperCase() + name.substring(1);

        return sID.substring(0, i + 1) + name;
    }

    public DoNotIntersectSimplexConstraint getChildConstraints() {
        return constraints.getChildConstraints();
    }
}
