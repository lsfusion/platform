package platform.fullclient.layout;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import platform.base.Pair;
import platform.client.Log;
import platform.interop.CompressingInputStream;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ReportConstants;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 16.09.2010
 * Time: 15:06:37
 */

public class ReportGenerator {
    private final String rootID;
    private final Map<String, List<String>> hierarchy;
    private final Map<String, JasperDesign> designs;
    private final Map<String, ClientReportData> data;

    RemoteFormInterface remoteForm;

    public ReportGenerator(RemoteFormInterface remoteForm, boolean toExcel) throws IOException, ClassNotFoundException, JRException {
        this.remoteForm = remoteForm;
        Pair<String, Map<String, List<String>>> hpair = retrieveReportHierarchy(remoteForm);
        rootID = hpair.first;
        hierarchy = hpair.second;
        designs = retrieveReportDesigns(remoteForm, toExcel);
        data = retrieveReportSources(remoteForm);
    }

    public JasperPrint createReport() throws JRException {
        Pair<Map<String, Object>, JRDataSource> compileParams =
                prepareReportSources();

        JasperReport report = JasperCompileManager.compileReport(designs.get(rootID));
        return JasperFillManager.fillReport(report, compileParams.first, compileParams.second);
    }

    private Pair<Map<String, Object>, JRDataSource> prepareReportSources() throws JRException {
        Map<String, Object> params = new HashMap<String, Object>();
        for (String childID : hierarchy.get(rootID)) {
            iterateChildSubreports(childID, params);
        }
        ReportRootDataSource rootSource = new ReportRootDataSource();
        return new Pair<Map<String, Object>, JRDataSource>(params, rootSource);
    }

    private ReportDependentDataSource iterateChildSubreports(String parentID, Map<String, Object> params) throws JRException {
        Map<String, Object> localParams = new HashMap<String, Object>();
        List<ReportDependentDataSource> childSources = new ArrayList<ReportDependentDataSource>();
        ReportDependentDataSource source = new ReportDependentDataSource(data.get(parentID), childSources);

        for (String childID : hierarchy.get(parentID)) {
            ReportDependentDataSource childSource = iterateChildSubreports(childID, localParams);
            childSources.add(childSource);
        }

        params.put(parentID + ReportConstants.reportSuffix, JasperCompileManager.compileReport(designs.get(parentID)));
        params.put(parentID + ReportConstants.sourceSuffix, source);
        params.put(parentID + ReportConstants.paramsSuffix, localParams);
        return source;
    }

    private static Pair<String, Map<String, java.util.List<String>>> retrieveReportHierarchy(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        byte[] hierarchyArray = remoteForm.getReportHierarchyByteArray();
        ObjectInputStream objStream = new ObjectInputStream(new CompressingInputStream(new ByteArrayInputStream(hierarchyArray)));
        String rootID = objStream.readUTF();
        Map<String, java.util.List<String>> hierarchy = (Map<String, java.util.List<String>>) objStream.readObject();
        return new Pair<String, Map<String, java.util.List<String>>>(rootID, hierarchy);
    }

    private static Map<String, JasperDesign> retrieveReportDesigns(RemoteFormInterface remoteForm, boolean toExcel) throws IOException, ClassNotFoundException {
        byte[] designsArray = remoteForm.getReportDesignsByteArray(toExcel);
        ObjectInputStream objStream = new ObjectInputStream(new CompressingInputStream(new ByteArrayInputStream(designsArray)));
        return (Map<String, JasperDesign>) objStream.readObject();
    }

    private static Map<String, ClientReportData> retrieveReportSources(RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        byte[] sourcesArray = remoteForm.getReportSourcesByteArray();
        DataInputStream dataStream = new DataInputStream(new CompressingInputStream(new ByteArrayInputStream(sourcesArray)));
        int size = dataStream.readInt();
        Map<String, ClientReportData> sources = new HashMap<String, ClientReportData>();
        for (int i = 0; i < size; i++) {
            String sid = dataStream.readUTF();
            ClientReportData reportData = new ClientReportData(dataStream);
            sources.put(sid, reportData);
        }
        return sources;
    }

}
