package lsfusion.interop;

import java.io.Serializable;

public class VMOptions implements Serializable {
    private String initHeapSize;
    private String maxHeapSize;

    public VMOptions(String initHeapSize, String maxHeapSize) {
        this.initHeapSize = initHeapSize;
        this.maxHeapSize = maxHeapSize;
    }

    public String getInitHeapSize() {
        return initHeapSize;
    }

    public void setInitHeapSize(String initHeapSize) {
        this.initHeapSize = initHeapSize;
    }

    public String getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(String maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }
}
