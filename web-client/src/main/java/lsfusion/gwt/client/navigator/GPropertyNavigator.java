package lsfusion.gwt.client.navigator;

import com.google.gwt.user.client.rpc.IsSerializable;
import lsfusion.gwt.client.base.jsni.HasNativeSID;

import java.io.Serializable;

public abstract class GPropertyNavigator implements Serializable, IsSerializable {
    public String canonicalName;

    public GPropertyNavigator() {
    }

    public GPropertyNavigator(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public abstract void update(GNavigatorElement root, Object value);
}