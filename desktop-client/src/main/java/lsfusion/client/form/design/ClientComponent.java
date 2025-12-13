package lsfusion.client.form.design;

import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.ClientIdentityObject;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyReader;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.property.PropertyReadType;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;

public abstract class ClientComponent extends ClientIdentityObject {
    public FontInfo font;
    public FontInfo captionFont;
    public Color background;
    public Color foreground;

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
    public String overflowHorz;
    public String overflowVert;

    public int marginTop;
    public int marginBottom;
    public int marginLeft;
    public int marginRight;

    public boolean captionVertical;
    public boolean captionLast;
    public FlexAlignment captionAlignmentHorz;
    public FlexAlignment captionAlignmentVert;

    public boolean defaultComponent;

    public ClientComponent() {
    }

    public Integer getSize(boolean vertical) {
        int size = vertical ? height : width;
        if(size == -2)
            return vertical ? getDefaultHeight() : getDefaultWidth();
        if (size == -1 || size == -3)
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

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        font = pool.readObject(inStream);
        captionFont = pool.readObject(inStream);
        background = pool.readObject(inStream);
        foreground = pool.readObject(inStream);

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
        overflowHorz = pool.readString(inStream);
        overflowVert = pool.readString(inStream);
        marginTop = inStream.readInt();
        marginBottom = inStream.readInt();
        marginLeft = inStream.readInt();
        marginRight = inStream.readInt();

        captionVertical = inStream.readBoolean();
        captionLast = inStream.readBoolean();
        captionAlignmentHorz = pool.readObject(inStream);
        captionAlignmentVert = pool.readObject(inStream);

        defaultComponent = inStream.readBoolean();

        sID = pool.readString(inStream);
    }

    public double getFlex() {
        return flex;
    }

    public boolean isShrink() {
        return shrink;
    }

    public boolean isAlignShrink() {
        return alignShrink;
    }

    public void setAlignShrink(boolean alignShrink) {
        this.alignShrink = alignShrink;
    }

    public boolean isStretch() {
        return getAlignment() == FlexAlignment.STRETCH;
    }

    public FlexAlignment getAlignment() {
        return alignment;
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

    public final ClientPropertyReader elementClassReader = new ExtraReader(PropertyReadType.COMPONENT_ELEMENTCLASS) {
        @Override
        public int getID() {
            return ClientComponent.this.getID();
        }
    };

    public int getVerticalMargin() {
        return marginTop + marginBottom;
    }

    public int getHorizontalMargin() {
        return marginLeft + marginRight;
    }

    public abstract static class ExtraReader implements ClientPropertyReader {
        final byte type;

        public ExtraReader(byte type) {
            this.type = type;
        }

        public ClientGroupObject getGroupObject() {
            return null;
        }

        public void update(Map<ClientGroupObjectValue, Object> readKeys, boolean updateKeys, TableController controller) {
        }

        public byte getType() {
            return type;
        }
    }
}
