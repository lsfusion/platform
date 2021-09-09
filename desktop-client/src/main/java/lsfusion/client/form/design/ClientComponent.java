package lsfusion.client.form.design;

import lsfusion.base.context.ContextIdentityObject;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.AbstractComponent;
import lsfusion.interop.form.design.ComponentDesign;
import lsfusion.interop.form.remote.serialization.IdentitySerializable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

public abstract class ClientComponent extends ContextIdentityObject implements IdentitySerializable<ClientSerializationPool>, AbstractComponent {

    public ComponentDesign design;

    public ClientContainer container;
    
    public Dimension size;
    
    public boolean autoSize;

    public double flex;
    public FlexAlignment alignment;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public boolean defaultComponent;

    public ClientComponent() {
    }

    public Integer getSize(boolean vertical) {
//        if (child.alignment == FlexAlignment.STRETCH) {
//            if (child.container.isVertical()) {
//                if (child.size.width == 0)
//                    child.size = new Dimension(-1, child.size.height);
//            } else {
//                if (child.size.height == 0)
//                    child.size = new Dimension(child.size.width, -1);
//            }
//        }

        int size = vertical ? this.size.height : this.size.width;
        if (size != -1)
            return size;
        return null;
    }

    public boolean isTab() {
        return container != null && container.isTabbed();
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);

        pool.writeObject(outStream, size);
        
        outStream.writeBoolean(autoSize);

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

    public double getFlex() {
        return flex;
    }

    public void setFlex(double flex) {
        this.flex = flex;
        updateDependency(this, "flex");
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

    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }

    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }
}
