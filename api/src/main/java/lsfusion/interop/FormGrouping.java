package lsfusion.interop;

import java.io.Serializable;
import java.util.List;

public class FormGrouping implements Serializable {
    public String name;
    public String groupObjectSID;
    public Boolean showItemQuantity;
    public List<PropertyGrouping> propertyGroupings;
    
    public FormGrouping(String name, String groupObjectSID, Boolean showItemQuantity, List<PropertyGrouping> propertyGroupings) {
        this.name = name;
        this.groupObjectSID = groupObjectSID;
        this.showItemQuantity = showItemQuantity;
        this.propertyGroupings = propertyGroupings;
    }

    public class PropertyGrouping implements Serializable {
        public String propertySID;
        public Integer groupingOrder;
        public Boolean sum;
        public Boolean max;

        public PropertyGrouping(String propertySID, Integer groupingOrder, Boolean sum, Boolean max) {
            this.propertySID = propertySID;
            this.groupingOrder = groupingOrder;
            this.sum = sum;
            this.max = max;
        }
    }
}
