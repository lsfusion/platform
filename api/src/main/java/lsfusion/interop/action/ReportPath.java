package lsfusion.interop.action;

import java.io.Serializable;

public class ReportPath implements Serializable {
    public String customPath;
    public String targetPath;

    public ReportPath(String customPath, String targetPath) {
        this.customPath = customPath;
        this.targetPath = targetPath;
    }
}