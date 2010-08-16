package platform.server.form.entity.filter;

import platform.server.form.entity.filter.FilterEntity;

import javax.swing.*;

public class RegularFilterEntity {
    public int ID;
    public transient FilterEntity filter;
    public String name = "";
    public KeyStroke key;
    public boolean showKey = true;

    public RegularFilterEntity(int iID, FilterEntity ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }
}
