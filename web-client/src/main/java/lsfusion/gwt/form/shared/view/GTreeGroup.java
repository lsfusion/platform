package lsfusion.gwt.form.shared.view;

import java.util.ArrayList;
import java.util.List;

public class GTreeGroup extends GComponent {
    public List<GGroupObject> groups = new ArrayList<>();

    public GToolbar toolbar;
    public GFilter filter;
    
    public boolean expandOnClick;
}
