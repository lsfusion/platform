package lsfusion.base;

import java.io.Serializable;

public class ProgressBar implements Serializable {
    public String message;
    public int progress;
    public int total;
    public String params;

    public ProgressBar(String message, int progress, int total) {
        this(message, progress, total, null);
    }

    public ProgressBar(String message, int progress, int total, String params) {
        this.message = message;
        this.progress = progress;
        this.total = total;
        this.params = params;
    }

    @Override
    public String toString() {
        return String.format("%s: %s of %s", message, progress, total);
    }
}