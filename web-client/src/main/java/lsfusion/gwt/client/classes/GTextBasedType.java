package lsfusion.gwt.client.classes;

import lsfusion.gwt.client.classes.data.GDataType;

public abstract class GTextBasedType extends GDataType {

    public GTextBasedType() {
    }

    @Override
    public String getVertTextAlignment(boolean isInput) {
        if(isInput)
           return "baseline"; // MIDDLE / CENTER works odd - gives some extra paaings in input (when rendered in an auto sized div)
        return "center";
    }
}
