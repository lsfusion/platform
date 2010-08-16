package platform.server.form.instance.filter;

import platform.server.form.instance.filter.FilterInstance;

import javax.swing.*;
import java.io.Serializable;

public class RegularFilterInstance implements Serializable {

    public int ID;
    public transient FilterInstance filter;
    public String name;
    public KeyStroke key;
    public boolean showKey = true;

    public RegularFilterInstance(int iID, FilterInstance ifilter, String iname, KeyStroke ikey, boolean ishowKey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
        showKey = ishowKey; 
    }

    public String toString() {
        return name;
    }
}
