package lsfusion.gwt.client;

import java.io.Serializable;

public class GRequestAttemptInfo implements Serializable {

    public int index;
    public String error;
    public int maxCount;

    public GRequestAttemptInfo() {
    }

    public GRequestAttemptInfo(int index, String error, int maxCount) {
        this.index = index;
        this.error = error;
        this.maxCount = maxCount;
    }

    @Override
    public String toString() {
        return "index = " + index + ", error = '" + error + "', maxCount=  " + maxCount;
    }
}
