package lsfusion.server.form.window;

import lsfusion.interop.navigator.WindowType;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.logics.CanonicalNameUtils;
import lsfusion.server.logics.i18n.LocalizedString;

import java.io.DataOutputStream;
import java.io.IOException;

public class AbstractWindow {
    private String canonicalName;
    
    public LocalizedString caption = LocalizedString.NONAME;

    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown = true;

    public boolean visible = true;

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
