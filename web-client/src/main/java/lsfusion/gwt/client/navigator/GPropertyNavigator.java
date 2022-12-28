package lsfusion.gwt.client.navigator;

import com.google.gwt.user.client.rpc.IsSerializable;
import lsfusion.gwt.client.base.jsni.HasNativeSID;

import java.io.Serializable;

public abstract class GPropertyNavigator implements Serializable, IsSerializable {

    public GPropertyNavigator() {
    }

    public abstract void update(GNavigatorElement root, Object value);
}