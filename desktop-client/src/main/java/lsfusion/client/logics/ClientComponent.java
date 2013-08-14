package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ContextIdentityObject;
import lsfusion.base.serialization.IdentitySerializable;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.descriptor.nodes.ComponentNode;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.ComponentDesign;
import lsfusion.interop.form.layout.*;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public abstract class ClientComponent extends ContextIdentityObject implements Serializable, IdentitySerializable<ClientSerializationPool>, AbstractComponent<ClientContainer, ClientComponent> {

    public ComponentDesign design;

    public ClientContainer container;

    public Dimension minimumSize;
    public Dimension maximumSize;
    public Dimension preferredSize;

    public double flex = 0;
    public FlexAlignment alignment = FlexAlignment.LEADING;

    public boolean defaultComponent = false;

    //todo: remove after .lsf refactoring
    public SimplexConstraints<ClientComponent> constraints = new SimplexConstraints<ClientComponent>();

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
        design = new ComponentDesign(context);

        constraints.setContext(context);

        initDefaultConstraints();
    }

    protected void initDefaultConstraints() {
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);

        pool.writeObject(outStream, minimumSize);
        pool.writeObject(outStream, maximumSize);
        pool.writeObject(outStream, preferredSize);

        outStream.writeDouble(flex);
        pool.writeObject(outStream, alignment);

        outStream.writeBoolean(defaultComponent);

        pool.writeString(outStream, sID);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        minimumSize = pool.readObject(inStream);
        maximumSize = pool.readObject(inStream);
        preferredSize = pool.readObject(inStream);

        flex = inStream.readDouble();
        alignment = pool.readObject(inStream);

        defaultComponent = inStream.readBoolean();

        sID = pool.readString(inStream);
    }

    public ComponentNode getNode() {
        return new ComponentNode(this);
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    //todo: remove
    public SimplexConstraints<ClientComponent> getConstraints() {
        return constraints;
    }

    public void setConstraints(SimplexConstraints<ClientComponent> constraints) {
        this.constraints = constraints;
        updateDependency(this, "constraints");
    }

    public Map<ClientComponent, DoNotIntersectSimplexConstraint> getIntersects() {
        return constraints.intersects;
    }

    public void setIntersects(Map<ClientComponent, DoNotIntersectSimplexConstraint> intersects) {
        constraints.intersects = intersects;
        updateDependency(this.constraints, "intersects");
    }

    public DoNotIntersectSimplexConstraint getChildConstraints() {
        return constraints.getChildConstraints();
    }

    public void setDefaultComponent(boolean defaultComponent) {
        this.defaultComponent = defaultComponent;
        updateDependency(this, "defaultComponent");
    }

    public boolean getDefaultComponent() {
        return defaultComponent;
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

    public double getFlex() {
        return flex;
    }

    public void setFlex(double flex) {
        this.flex = flex;
        updateDependency(this, "flex");
    }

    public FlexAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(FlexAlignment alignment) {
        this.alignment = alignment;
        updateDependency(this, "alignment");
    }

    public abstract String getCaption();
}
