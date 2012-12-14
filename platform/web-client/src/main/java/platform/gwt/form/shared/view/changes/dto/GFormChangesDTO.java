package platform.gwt.form.shared.view.changes.dto;

import platform.gwt.form.shared.view.GClassViewType;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GFormChangesDTO implements Serializable {
    public HashMap<Integer, GClassViewType> classViews = new HashMap<Integer, GClassViewType>();
    public HashMap<Integer, GGroupObjectValue> objects = new HashMap<Integer, GGroupObjectValue>();
    public HashMap<Integer, ArrayList<GGroupObjectValue>> gridObjects = new HashMap<Integer, ArrayList<GGroupObjectValue>>();
    public HashMap<Integer, ArrayList<GGroupObjectValue>> parentObjects = new HashMap<Integer, ArrayList<GGroupObjectValue>>();
    public HashMap<GPropertyReaderDTO, HashMap<GGroupObjectValue, Object>> properties = new HashMap<GPropertyReaderDTO, HashMap<GGroupObjectValue, Object>>();
    public HashSet<GPropertyReaderDTO> panelProperties = new HashSet<GPropertyReaderDTO>();
    public HashSet<Integer> dropProperties = new HashSet<Integer>();
}
