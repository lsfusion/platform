package lsfusion.interop.form.print;

import java.io.Serializable;

public class ReportGenerationData implements Serializable {
    public byte[] reportHierarchyData;
    public byte[] reportDesignData;
    public byte[] reportSourceData;
    
    public boolean useShowIf;
    public byte[] classes;

    public ReportGenerationData(byte[] reportHierarchyData, byte[] reportDesignData, byte[] reportSourceData, boolean useShowIf, byte[] classes) {
        this.reportHierarchyData = reportHierarchyData;
        this.reportDesignData = reportDesignData;
        this.reportSourceData = reportSourceData;
        
        this.useShowIf = useShowIf;
        this.classes = classes;
    }
}

