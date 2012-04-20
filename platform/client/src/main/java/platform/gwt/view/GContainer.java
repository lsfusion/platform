package platform.gwt.view;

import java.util.ArrayList;

public class GContainer extends GComponent {
    public ArrayList<GComponent> children = new ArrayList<GComponent>();
    public String title;
    public String description;
    public String sID;
    public boolean tabbedPane;
    public byte type;
    public boolean gwtIsLayout;
    public boolean gwtVertical;
}
