package lsfusion.gwt.client.base;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GAsync implements IsSerializable, Serializable {
    public String displayString;
    public String rawString;

    public static final GAsync RECHECK = new GAsync("RECHECK", "RECHECK");
    public static final GAsync CANCELED = new GAsync("CANCELED", "CANCELED");

    public GAsync() {
    }

    public GAsync(String displayString, String rawString) {
        this.displayString = displayString;
        this.rawString = rawString;
    }
}
