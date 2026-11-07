package lsfusion.server.logics.navigator.window;

import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.interop.navigator.window.WindowType;
import lsfusion.server.base.Custom;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFProperty;
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
    // client reads one pair of fields either way. The halves are NF at the versions of the modules giving them
    // (WINDOW, EXTEND WINDOW ... CUSTOM) : modules that do not require each other are parsed at once, and of two giving
    // one window a half the later in the module order wins
    private final NFProperty<String> custom = NFFact.property();
    // CUSTOM <property>: the value the application computes, pushed with the rest of the navigator's dynamic
    // properties. The pair works the way elementClass / propertyElementClass does - this is what the client's one
    // `custom` field starts as, and the property overwrites that same field as it recomputes
    private final NFProperty<Property> propertyCustom = NFFact.property();

    public String getCustom() {
        String custom = this.custom.get();
        if (custom == null && getPropertyCustom() != null) // the view is chosen before the first value arrives, so SOMETHING has
            return Custom.EMPTY_TEMPLATE;                   // to be there; a literal, if one was given, is that something
        return custom;
    }

    public void setCustom(String custom, Version version) {
        this.custom.set(custom, version);
    }

    public Property getPropertyCustom() {
        return propertyCustom.get();
    }

    public void setPropertyCustom(Property propertyCustom, Version version) {
        this.propertyCustom.set(propertyCustom, version);
    }

    // the halves a statement brings, each written if given - unless the statement gives a React component and a property,
    // which is refused : that component is returned and nothing written. The halves of two statements meet in the final
    // window, checked once every module has run (checkCustoms) : the two can come from modules that do not require each
    // other, and a later one can give the window another literal
    public String setCustoms(String custom, Property propertyCustom, Version version) {
        if (propertyCustom != null && Custom.isReactComponent(custom))
            return custom;

        if (custom != null)
            setCustom(custom, version);
        if (propertyCustom != null)
            setPropertyCustom(propertyCustom, version);
        return null;
    }

    public boolean isReact() {
        return Custom.isReactComponent(custom.get());
    }

    // the final window : a React component draws it itself, so no property computes its template (see setCustoms)
    public void checkCustoms() {
        if (isReact() && getPropertyCustom() != null)
            throw new RuntimeException("CUSTOM of window " + getCanonicalName() + " is both the React component " + custom.get()
                    + " and a property; a component draws the window itself, so a computed template would never be drawn");
    }

    public boolean vertical; // the window's own orientation: AUTOSIZE hugs ACROSS it
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

        SerializationUtil.writeString(outStream, getCustom());
        outStream.writeBoolean(isReact());
        // one form at a time, drawn alone. Which windows hold forms at all the client knows from where they
        // arrive - the one counted list of forms windows the navigator writes, System.forms among them - so that
        // is not written
        outStream.writeBoolean(isSingleForm());
        outStream.writeInt(getFormCloseDelay());
        outStream.writeBoolean(vertical);
    }

    public boolean isSingleForm() {
        return false;
    }

    // how long the form a new one displaces is kept before it is asked to close, in seconds; 0 at once,
    // FormsWindow.NOCLOSE never
    public int getFormCloseDelay() {
        return 0;
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
