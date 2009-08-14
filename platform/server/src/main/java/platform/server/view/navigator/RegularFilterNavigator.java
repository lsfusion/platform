package platform.server.view.navigator;

import platform.server.view.navigator.filter.FilterNavigator;
import platform.server.view.navigator.filter.CompareFilterNavigator;

import javax.swing.*;

public class RegularFilterNavigator {
    public int ID;
    public transient FilterNavigator filter;
    public String name = "";
    public KeyStroke key;

    public RegularFilterNavigator(int iID, CompareFilterNavigator ifilter, String iname, KeyStroke ikey) {
        ID = iID;
        filter = ifilter;
        name = iname;
        key = ikey;
    }
}
