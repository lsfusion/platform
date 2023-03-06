package lsfusion.gwt.client.navigator.window;

import lsfusion.gwt.client.base.jsni.HasNativeSID;

import java.io.Serializable;

public class GAbstractWindow implements Serializable, com.google.gwt.user.client.rpc.IsSerializable, HasNativeSID {
    public String canonicalName;
    public String caption;
    public int position;

    public int x;
    public int y;
    public int width;
    public int height;

    public String borderConstraint;

    public boolean titleShown;
    public boolean visible;

    public String elementClass;

    public boolean autoSize;

    @Override
    public String getNativeSID() {
        return canonicalName;
    }
}
