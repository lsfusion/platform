package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ContextIdentityObject;
import lsfusion.base.serialization.IdentitySerializable;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.descriptor.nodes.ComponentNode;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.ComponentDesign;
import lsfusion.interop.form.layout.AbstractComponent;
import lsfusion.interop.form.layout.FlexAlignment;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

public abstract class ClientComponent extends ContextIdentityObject implements IdentitySerializable<ClientSerializationPool>, AbstractComponent<ClientContainer, ClientComponent> {

    public ComponentDesign design;

    public ClientContainer container;

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
        outStream.writeInt(marginTop);
        outStream.writeInt(marginBottom);
        outStream.writeInt(marginLeft);
        outStream.writeInt(marginRight);

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
        marginTop = inStream.readInt();
        marginBottom = inStream.readInt();
        marginLeft = inStream.readInt();
        marginRight = inStream.readInt();

        defaultComponent = inStream.readBoolean();

        sID = pool.readString(inStream);
    }

    public ComponentNode getNode() {
        return new ComponentNode(this);
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
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

    public int getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(int marginTop) {
        this.marginTop = marginTop;
        updateDependency(this, "marginTop");
    }

    public int getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
        updateDependency(this, "marginBottom");
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public void setMarginLeft(int marginLeft) {
        this.marginLeft = marginLeft;
        updateDependency(this, "marginLeft");
    }

    public int getMarginRight() {
        return marginRight;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
        updateDependency(this, "marginRight");
    }

    public void setMargin(int margin) {
        setMarginTop(margin);
        setMarginBottom(margin);
        setMarginLeft(margin);
        setMarginRight(margin);
    }

    public void installMargins(JComponent view) {
        if (marginTop != 0 || marginLeft != 0 || marginBottom != 0 || marginRight != 0) {
            Border marginBorder = createEmptyBorder(marginTop, marginLeft, marginBottom, marginRight);
            Border originalBorder = view.getBorder();
            view.setBorder(createCompoundBorder(marginBorder, originalBorder));
        }
    }

    public abstract String getCaption();
}
