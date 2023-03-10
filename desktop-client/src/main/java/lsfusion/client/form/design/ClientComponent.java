package lsfusion.client.form.design;

import lsfusion.base.context.ContextIdentityObject;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.AbstractComponent;
import lsfusion.interop.form.design.ComponentDesign;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.interop.form.remote.serialization.IdentitySerializable;

import javax.swing.*;
import javax.swing.border.Border;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

public abstract class ClientComponent extends ContextIdentityObject implements IdentitySerializable<ClientSerializationPool>, AbstractComponent {

    public ComponentDesign design;

    public ClientContainer container;

    public String elementClass;

    public int width;
    public int height;

    public int span = 1;

    public double flex;
    public FlexAlignment alignment;
    public boolean shrink;
    public boolean alignShrink;
    public Boolean alignCaption;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public boolean defaultComponent;

    public ClientComponent() {
    }

    public Integer getSize(boolean vertical) {
        int size = vertical ? height : width;
        if(size == -2)
            return vertical ? getDefaultHeight() : getDefaultWidth();
        if (size == -1)
            return null;
        return size;
    }

    protected Integer getDefaultWidth() {
        throw new UnsupportedOperationException();
    }

    protected Integer getDefaultHeight() {
        throw new UnsupportedOperationException();
    }

    public boolean isTab() {
        return container != null && container.tabbed;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);

        outStream.writeInt(width);
        outStream.writeInt(height);

        outStream.writeInt(span);

        outStream.writeDouble(flex);
        pool.writeObject(outStream, alignment);
        outStream.writeBoolean(shrink);
        outStream.writeBoolean(alignShrink);
        pool.writeObject(outStream, alignCaption);
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

        elementClass = pool.readString(inStream);

        width = inStream.readInt();
        height = inStream.readInt();
        
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

    public double getFlex() {
        return flex;
    }

    public void setFlex(double flex) {
        this.flex = flex;
        updateDependency(this, "flex");
    }

    public boolean isShrink() {
        return shrink;
    }

    @Override
    public void setShrink(boolean shrink) {
        this.shrink = shrink;
        updateDependency(this, "shrink");
    }

    public boolean isAlignShrink() {
        return alignShrink;
    }

    @Override
    public void setAlignShrink(boolean alignShrink) {
        this.alignShrink = alignShrink;
    }

    public boolean isStretch() {
        return getAlignment() == FlexAlignment.STRETCH;
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

    public final ClientPropertyReader showIfReader = new ClientPropertyReader() {
        public ClientGroupObject getGroupObject() {
            return null;
        }

        public void update(Map<ClientGroupObjectValue, Object> values, boolean updateKeys, TableController controller) {
            controller.getFormController().getLayout().setShowIfVisible(ClientComponent.this, values.get(ClientGroupObjectValue.EMPTY) == null);
        }

        public int getID() {
            return ClientComponent.this.getID();
        }

        public byte getType() {
            return PropertyReadType.COMPONENT_SHOWIF;
        }
    };

    public final ClientPropertyReader elementClassReader = new ClientPropertyReader() {
        public ClientGroupObject getGroupObject() {
            return null;
        }

        public void update(Map<ClientGroupObjectValue, Object> values, boolean updateKeys, TableController controller) {
        }

        public int getID() {
            return ClientComponent.this.getID();
        }

        public byte getType() {
            return PropertyReadType.COMPONENT_ELEMENTCLASS;
        }
    };

    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }

    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }
}
