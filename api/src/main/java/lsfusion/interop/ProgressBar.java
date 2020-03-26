package lsfusion.interop;

import java.io.Serializable;

public class ProgressBar implements Serializable {
    public String message;
    public int progress;
    public int total;
    public String object;
    public String params;

    public ProgressBar(String message, int progress, int total) {
        this(message, progress, total, null);
    }

    public ProgressBar(String message, int progress, int total, String object) {
        this.message = message;
        this.progress = progress;
        this.total = total;
        this.object = object;
    }
    
    public String getParams() {
        if(object != null) {
            if(params != null)
                return object + ", " + params;
            return object;
        }
        if(params != null)
            return params;            
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s: %s of %s", message, progress, total);
    }
}