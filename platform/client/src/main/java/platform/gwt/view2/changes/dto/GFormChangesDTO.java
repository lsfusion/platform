package platform.gwt.view2.changes.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GFormChangesDTO implements Serializable {
    public HashMap<Integer, String> classViews = new HashMap<Integer, String>();
    public HashMap<Integer, GGroupObjectValueDTO> objects = new HashMap<Integer, GGroupObjectValueDTO>();
    public HashMap<Integer, ArrayList<GGroupObjectValueDTO>> gridObjects = new HashMap<Integer, ArrayList<GGroupObjectValueDTO>>();
    public HashMap<Integer, ArrayList<GGroupObjectValueDTO>> parentObjects = new HashMap<Integer, ArrayList<GGroupObjectValueDTO>>();
    public HashMap<GPropertyReaderDTO, HashMap<GGroupObjectValueDTO, Object>> properties = new HashMap<GPropertyReaderDTO, HashMap<GGroupObjectValueDTO, Object>>();
    public HashSet<GPropertyReaderDTO> panelProperties = new HashSet<GPropertyReaderDTO>();
    public HashSet<Integer> dropProperties = new HashSet<Integer>();
}
