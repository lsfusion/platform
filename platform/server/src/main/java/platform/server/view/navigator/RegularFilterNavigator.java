package platform.server.view.navigator;

import javax.swing.*;

public class RegularFilterNavigator {
    public int ID;
    public transient FilterNavigator filter;
    public String name = "";
    public KeyStroke key;

    public RegularFilterNavigator(int iID, FilterNavigator ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }
}
