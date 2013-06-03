package lsfusion.interop.form;

import java.io.Serializable;

public class ReportGenerationData implements Serializable {
    public byte[] reportHierarchyData;
    public byte[] reportDesignData;
    public byte[] reportSourceData;

    public ReportGenerationData(byte[] reportHierarchyData, byte[] reportDesignData, byte[] reportSourceData) {
        this.reportHierarchyData = reportHierarchyData;
        this.reportDesignData = reportDesignData;
        this.reportSourceData = reportSourceData;
    }
}

