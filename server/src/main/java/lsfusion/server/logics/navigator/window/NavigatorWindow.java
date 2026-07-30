package lsfusion.server.logics.navigator.window;

import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.base.Custom;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;

public class NavigatorWindow extends AbstractWindow {

    public int type;
    public boolean showSelect = true;

    public int verticalTextPosition = SwingConstants.BOTTOM;
    public int horizontalTextPosition = SwingConstants.CENTER;

    public int verticalAlignment = SwingConstants.CENTER;
    public int horizontalAlignment = SwingConstants.CENTER;

    public float alignmentY = JToolBar.TOP_ALIGNMENT;
    public float alignmentX = JToolBar.LEFT_ALIGNMENT;

    // CUSTOM: the component name (React) or the HTML template putting the window's elements into its <Lsf:name>
    // places; react is INFERRED from it, the same rule a DESIGN container's custom follows
    private String custom;
    // CUSTOM <property>: the template the application computes, pushed with the rest of the navigator's dynamic
    // properties. The pair works the way elementClass / propertyElementClass does: this is what the client's ONE
    // `custom` field starts as, and the property overwrites that same field as it recomputes
    private Property propertyCustom;

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    public Property getPropertyCustom() {
        return propertyCustom;
    }

    public void setPropertyCustom(Property propertyCustom) {
        this.propertyCustom = propertyCustom;
        if (custom == null) // the view is chosen before the first value arrives, so SOMETHING has to be there; a
            custom = Custom.EMPTY_TEMPLATE; // literal, if one was written, is that something and is left alone
    }

    public boolean isReact() {
        return Custom.isReactComponent(custom);
    }

    public NavigatorWindow(int type, String canonicalName, LocalizedString caption, int x, int y, int width, int height) {
        super(canonicalName, caption, x, y, width, height);

        setType(type);
    }

    public NavigatorWindow(int type, String canonicalName, LocalizedString caption, String borderConstraint) {
        super(canonicalName, caption, borderConstraint);

        setType(type);
    }

    public NavigatorWindow(int type, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);

        setType(type);
    }

    private void setType(int type) {
        this.type = type;
        if (this.type == JToolBar.VERTICAL) {
            verticalTextPosition = SwingConstants.CENTER;
            horizontalTextPosition = SwingConstants.TRAILING;

            horizontalAlignment = SwingConstants.LEFT;
        }
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(type);
        outStream.writeBoolean(showSelect);

        outStream.writeInt(verticalTextPosition);
        outStream.writeInt(horizontalTextPosition);

        outStream.writeInt(verticalAlignment);
        outStream.writeInt(horizontalAlignment);

        outStream.writeFloat(alignmentY);
        outStream.writeFloat(alignmentX);

        outStream.writeBoolean(drawScrollBars);

        SerializationUtil.writeString(outStream, custom);
        outStream.writeBoolean(isReact());
    }
}
