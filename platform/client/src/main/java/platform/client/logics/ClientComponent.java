package platform.client.logics;

import platform.base.IdentityObject;
import platform.client.descriptor.editor.ComponentEditor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.descriptor.nodes.ComponentNode;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ComponentDesign;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public abstract class ClientComponent extends IdentityObject implements Serializable, ClientIdentitySerializable {

    public int ID; // ID есть и у свойст и у объектов, так что чтобы не путаться

    public ComponentDesign design = new ComponentDesign();

    public ClientContainer container;
    public SimplexConstraints<ClientComponent> constraints = SimplexConstraints.DEFAULT_CONSTRAINT;

    public boolean defaultComponent = false;

    public boolean show = true;

    public ClientComponent() {
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);
        pool.writeObject(outStream, constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ClientComponent, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            pool.serializeObject(outStream, intersect.getKey());
            pool.writeObject(outStream, intersect.getValue());
        }

        outStream.writeBoolean(defaultComponent);
        outStream.writeBoolean(show);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        constraints = pool.readObject(inStream);

        constraints.intersects = new HashMap<ClientComponent, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientComponent view = pool.deserializeObject(inStream);
            DoNotIntersectSimplexConstraint constraint = pool.readObject(inStream);
            constraints.intersects.put(view, constraint);
        }

        defaultComponent = inStream.readBoolean();
        show = inStream.readBoolean();
    }

    public ComponentNode getNode() {
        return new ComponentNode(this);
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor("Компонент", this);
    }

    public boolean getShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
        IncrementDependency.update(this, "show");
    }

    public Color getBackground() {
        return design.background;
    }

    public void setBackground(Color background) {
        design.background = background;

        IncrementDependency.update(this, "background");
    }

    public Color getForeground() {
        return design.foreground;
    }

    public void setForeground(Color foreground) {
        design.foreground = foreground;
        IncrementDependency.update(this, "foreground");
    }

    public Font getFont() {
        return design.font;
    }

    public void setFont(Font font) {
        design.font = font;
        IncrementDependency.update(this, "font");
    }

    public void setDefaultComponent(boolean defaultComponent) {
        this.defaultComponent = defaultComponent;
        IncrementDependency.update(this, "defaultComponent");
    }

    public boolean getDefaultComponent() {
        return defaultComponent;
    }
}
