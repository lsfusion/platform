package lsfusion.server.logics.navigator.window;

import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.interop.navigator.window.WindowType;
import lsfusion.server.base.Custom;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

import java.io.DataOutputStream;
import java.io.IOException;

public class AbstractWindow {
    private String canonicalName;
    
    public LocalizedString caption = LocalizedString.NONAME;

    public boolean drawScrollBars = true;

    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown = true;

    public boolean visible = true;
    
    public Property propertyElementClass;
    public String elementClass;

    // CUSTOM: what draws this window instead of the standard view. What that means depends on the window's ROLE, and
    // only the role knows: a navigator window is drawn from its navigator elements, the forms window from the forms
    // open in it, the log window from the messages logged in it. All live here because the statement is one, and the
    // client reads one pair of fields either way
    private String custom;
    // CUSTOM <property>: the value the application computes, pushed with the rest of the navigator's dynamic
    // properties. The pair works the way elementClass / propertyElementClass does - this is what the client's one
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

    public boolean autoSize; 

    public AbstractWindow(String canonicalName, LocalizedString caption, int x, int y, int width, int height) {
        this(canonicalName, caption);

        setDockPosition(x, y, width, height);
    }

    public AbstractWindow(String canonicalName, LocalizedString caption, String borderConstraint) {
        this(canonicalName, caption);

        setBorderPosition(borderConstraint);
    }

    public AbstractWindow(String canonicalName, LocalizedString caption) {
        this.canonicalName = canonicalName;
        this.caption = caption;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(getCanonicalName());
        outStream.writeUTF(ThreadLocalContext.localize(caption));

        outStream.writeInt(position);
        if (position == WindowType.DOCKING_POSITION) {
            outStream.writeInt(x);
            outStream.writeInt(y);
            outStream.writeInt(width);
            outStream.writeInt(height);
        }
        if (position == WindowType.BORDER_POSITION) {
            outStream.writeUTF(borderConstraint);
        }

        outStream.writeBoolean(titleShown);
        outStream.writeBoolean(visible);

        outStream.writeBoolean(elementClass != null);
        if (elementClass != null) {
            outStream.writeUTF(elementClass);
        }
        outStream.writeBoolean(autoSize);

        SerializationUtil.writeString(outStream, custom);
        outStream.writeBoolean(isReact());
    }

    public void setDockPosition(int x, int y, int width, int height) {
        this.position = WindowType.DOCKING_POSITION;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    private void setBorderPosition(String borderConstraint) {
        this.position = WindowType.BORDER_POSITION;
        this.borderConstraint = borderConstraint;
    }

    public String getCanonicalName() {
        return canonicalName;
    }
    
    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    }
    
    public boolean isNamed() {
        return canonicalName != null;
    }
}
