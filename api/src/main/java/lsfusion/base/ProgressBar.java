package lsfusion.base;

import java.io.Serializable;

public class ProgressBar implements Serializable {
    public String message;
    public int progress;
    public int total;
    public String params;

    public ProgressBar(String message, int progress, int total) {
        this.message = message;
        this.progress = progress;
        this.total = total;
    }
}