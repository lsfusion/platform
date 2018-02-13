package lsfusion.interop.action;

import java.io.Serializable;

public class ReportPath implements Serializable {
    public String customPath;
    public String autoPath;
    public String targetPath;

    public ReportPath(String customPath, String targetPath) {
        this(customPath, null, targetPath);
    }

    public ReportPath(String customPath, String autoPath, String targetPath) {
        this.customPath = customPath;
        this.autoPath = autoPath;
        this.targetPath = targetPath;
    }
}