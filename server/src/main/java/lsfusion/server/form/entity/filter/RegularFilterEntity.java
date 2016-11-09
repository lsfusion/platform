package lsfusion.server.form.entity.filter;

import lsfusion.base.identity.IdentityObject;

import javax.swing.*;

public class RegularFilterEntity extends IdentityObject {
    public transient FilterEntity filter;
    public String name = "";
    public KeyStroke key;
    public boolean showKey = true;

    public RegularFilterEntity() {

    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, String iname) {
        this(iID, ifilter, iname, null);
    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }
}
