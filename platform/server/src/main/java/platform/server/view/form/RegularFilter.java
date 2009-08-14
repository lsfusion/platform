package platform.server.view.form;

import platform.server.view.form.filter.Filter;

import javax.swing.*;
import java.io.Serializable;

public class RegularFilter implements Serializable {

    public int ID;
    public transient Filter filter;
    public String name = "";
    public KeyStroke key;

    public RegularFilter(int iID, Filter ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }

    public String toString() {
        return name;
    }
}
