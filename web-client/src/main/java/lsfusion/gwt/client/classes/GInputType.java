package lsfusion.gwt.client.classes;

import java.io.Serializable;

public class GInputType implements Serializable {

    private String name;

    public GInputType() {
    }

    public GInputType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isStretch() {
        return !isLogical() && !name.equals("range");
    }
// !name.equals("color")
    public boolean isStretchText() {
        return isStretch();
    }

    public boolean isMultilineText() {
        return name.equals("textarea");
    }

    public boolean isLogical() {
        return name.equals("checkbox");
    }

    public boolean isYear() {
        return name.equals("year");
    }

    public boolean hasNativePopup() {
        switch (name) {
            case "date":
            case "datetime-local":
            case "time":
            case "week":
            case "month":
                return true;
        }

        return false;
    }

    public boolean isSelectAll() { // important not to be logical, because in that case we will cancel the CHANGE event
        return isStretchText() && !hasNativePopup() && !isYear();
    }

    public boolean isRemoveAllPMB() {
        return isStretchText();
    }

    public boolean isNumber() {
        return name.equals("number");
    }
}
