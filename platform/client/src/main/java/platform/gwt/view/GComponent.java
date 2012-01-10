package platform.gwt.view;

import platform.interop.PanelLocation;

import java.io.Serializable;

public class GComponent implements Serializable {
    public int ID;
    public GContainer container;
    public boolean defaultComponent;
    public boolean drawToToolbar;
}
