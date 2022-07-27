package lsfusion.interop.form.print;

import java.io.Serializable;

public class ReportGenerationData implements Serializable {
    public byte[] reportHierarchyData;
    public byte[] reportDesignData;
    public byte[] reportSourceData;
    
    public boolean useShowIf;
    int jasperReportsGovernorMaxPages;
    long jasperReportsGovernorTimeout;

    public byte[] classes;

    public ReportGenerationData(byte[] reportHierarchyData, byte[] reportDesignData, byte[] reportSourceData,
                                boolean useShowIf, int jasperReportsGovernorMaxPages, long jasperReportsGovernorTimeout,
                                byte[] classes) {
        this.reportHierarchyData = reportHierarchyData;
        this.reportDesignData = reportDesignData;
        this.reportSourceData = reportSourceData;
        
        this.useShowIf = useShowIf;
        this.jasperReportsGovernorMaxPages = jasperReportsGovernorMaxPages;
        this.jasperReportsGovernorTimeout = jasperReportsGovernorTimeout;

        this.classes = classes;
    }
}

