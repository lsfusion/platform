package lsfusion.interop;

import java.io.Serializable;

public class VMOptions implements Serializable {
    private String initHeapSize;
    private String maxHeapSize;
    private String maxHeapFreeRatio;

    public VMOptions(String initHeapSize, String maxHeapSize, String maxHeapFreeRatio) {
        this.initHeapSize = initHeapSize;
        this.maxHeapSize = maxHeapSize;
        this.maxHeapFreeRatio = maxHeapFreeRatio;
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

    public String getMaxHeapFreeRatio() {
        return maxHeapFreeRatio;
    }

    public void setMaxHeapFreeRatio(String maxHeapFreeRatio) {
        this.maxHeapFreeRatio = maxHeapFreeRatio;
    }
}
