package jasperapi;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.ByteArray;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ReportConstants;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * User: DAle
 * Date: 16.09.2010
 * Time: 15:06:37
 */

public class ReportGenerator {
    private final RemoteFormInterface form;
    private final String rootID;
    private final Map<String, List<String>> hierarchy;
    private Map<String, JasperDesign> designs;
    private Map<String, ClientReportData> data;
    private Map<String, List<List<Object>>> compositeColumnValues;


    private static class SourcesGenerationOutput {
        public Map<String, ClientReportData> data;
        // данные для свойств с группами в колонках
        // объекты, от которых зависит свойство
        public Map<String, List<Integer>> compositeFieldsObjects;
        public Map<String, Map<List<Object>, Object>> compositeObjectValues;
        // объекты, идущие в колонки
        public Map<String, List<Integer>> compositeColumnObjects;
        public Map<String, List<List<Object>>> compositeColumnValues;
    }

    public ReportGenerator(RemoteFormInterface remoteForm) throws ClassNotFoundException, IOException {
        form = remoteForm;

        Pair<String, Map<String, List<String>>> hpair = retrieveReportHierarchy(form);
        rootID = hpair.first;
        hierarchy = hpair.second;

    }

    public JasperPrint createReport(boolean toExcel, boolean ignorePagination, Map<ByteArray, String> files) throws JRException, ClassNotFoundException, IOException {
        return createJasperPrint(hierarchy, toExcel, ignorePagination, null, files);
    }

    public JasperPrint createSingleGroupReport(Integer groupId, boolean toExcel, boolean ignorePagination, Map<ByteArray, String> files) throws IOException, ClassNotFoundException, JRException {
        // transform hierarchy
        Map<String, List<String>> localHierarchy = new HashMap<String, List<String>>();
        return createJasperPrint(localHierarchy, toExcel, ignorePagination, groupId, files);
    }

    private JasperPrint createJasperPrint(Map<String, List<String>> hierarchy, boolean toExcel, boolean ignorePagination,
                                          Integer singleGroupId, Map<ByteArray, String> files) throws ClassNotFoundException, IOException, JRException {
        designs = retrieveReportDesigns(form, toExcel);

        SourcesGenerationOutput output = retrieveReportSources(form, files);
        data = output.data;
        compositeColumnValues = output.compositeColumnValues;

        transformDesigns(ignorePagination);

        Pair<Map<String, Object>, JRDataSource> compileParams = prepareReportSources();

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
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(hierarchyArray));
        String rootID = objStream.readUTF();
        Map<String, java.util.List<String>> hierarchy = (Map<String, java.util.List<String>>) objStream.readObject();
        return new Pair<String, Map<String, java.util.List<String>>>(rootID, hierarchy);
    }

    private static Map<String, JasperDesign> retrieveReportDesigns(RemoteFormInterface remoteForm, boolean toExcel) throws IOException, ClassNotFoundException {
        byte[] designsArray = remoteForm.getReportDesignsByteArray(toExcel);
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(designsArray));
        return (Map<String, JasperDesign>) objStream.readObject();
    }

    private static SourcesGenerationOutput retrieveReportSources(RemoteFormInterface remoteForm, Map<ByteArray,String> files) throws IOException, ClassNotFoundException {
        SourcesGenerationOutput output = new SourcesGenerationOutput();
        byte[] sourcesArray = remoteForm.getReportSourcesByteArray();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(sourcesArray));
        int size = dataStream.readInt();
        output.data = new HashMap<String, ClientReportData>();
        for (int i = 0; i < size; i++) {
            String sid = dataStream.readUTF();
            ClientReportData reportData = new ClientReportData(dataStream, files);
            output.data.put(sid, reportData);
        }

        int compositePropsCnt = dataStream.readInt();

        output.compositeFieldsObjects = retrievePropertyObjects(dataStream, compositePropsCnt);

        int compositeFieldsCnt = dataStream.readInt();
        output.compositeObjectValues = new HashMap<String, Map<List<Object>, Object>>();
        for (int i = 0; i < compositeFieldsCnt; i++) {
            String fieldId = dataStream.readUTF();
            Map<List<Object>, Object> data = new HashMap<List<Object>, Object>();
            int valuesCnt = dataStream.readInt();
            for (int j = 0; j < valuesCnt; j++) {
                List<Object> values = new ArrayList<Object>();
                String dataFieldId = fieldId;
                if (fieldId.endsWith(ReportConstants.captionSuffix)) {
                    dataFieldId = fieldId.substring(0, fieldId.length() - ReportConstants.captionSuffix.length());
                }
                int objCnt = output.compositeFieldsObjects.get(dataFieldId).size();
                for (int k = 0; k < objCnt; k++) {
                    values.add(BaseUtils.deserializeObject(dataStream));
                }
                data.put(values, BaseUtils.deserializeObject(dataStream));
            }
            output.compositeObjectValues.put(fieldId, data);
        }

        output.compositeColumnObjects = retrievePropertyObjects(dataStream, compositePropsCnt);

        output.compositeColumnValues = new HashMap<String, List<List<Object>>>();
        for (int i = 0; i < compositePropsCnt; i++) {
            String fieldId = dataStream.readUTF();
            List<List<Object>> data = new ArrayList<List<Object>>();
            int valuesCnt = dataStream.readInt();
            for (int j = 0; j < valuesCnt; j++) {
                List<Object> values = new ArrayList<Object>();
                int objCnt = output.compositeColumnObjects.get(fieldId).size();
                for (int k = 0; k < objCnt; k++) {
                    values.add(BaseUtils.deserializeObject(dataStream));
                }
                data.add(values);
            }
            output.compositeColumnValues.put(fieldId, data);
        }

        for (ClientReportData data : output.data.values()) {
            data.setCompositeData(output.compositeFieldsObjects, output.compositeObjectValues,
                    output.compositeColumnObjects, output.compositeColumnValues);
        }
        return output;
    }

    private static Map<String, List<Integer>> retrievePropertyObjects(DataInputStream stream, int propCnt) throws IOException {
        Map<String, List<Integer>> objects = new HashMap<String, List<Integer>>();
        for (int i = 0; i < propCnt; i++) {
            String fieldId = stream.readUTF();
            List<Integer> objectsId = new ArrayList<Integer>();
            int objectCnt = stream.readInt();
            for (int j = 0; j < objectCnt; j++) {
                objectsId.add(stream.readInt());
            }
            objects.put(fieldId, objectsId);
        }
        return objects;
    }

    // Пока что добавляет только свойства с группами в колонках
    private void transformDesigns(boolean ignorePagination) {
        for (JasperDesign design : designs.values()) {
            transformDesign(design);
            design.setIgnorePagination(ignorePagination);
        }
    }

    private void transformDesign(JasperDesign design) {
        transformBand(design, design.getPageHeader());
        transformBand(design, design.getPageFooter());

        transformSection(design, design.getDetailSection());
        for (JRGroup group : design.getGroups()) {
            transformSection(design, group.getGroupHeaderSection());
            transformSection(design, group.getGroupFooterSection());
        }
    }

    private void transformSection(JasperDesign design, JRSection section) {
        if (section instanceof JRDesignSection) {
            JRDesignSection designSection = (JRDesignSection) section;
            List bands = designSection.getBandsList();
            for (Object band : bands) {
                if (band instanceof JRBand) {
                    transformBand(design, (JRBand) band);
                }
            }
        }
    }

    private void transformBand(JasperDesign design, JRBand band) {
        if (band instanceof JRDesignBand) {
            JRDesignBand designBand = (JRDesignBand) band;
            List<JRDesignElement> toDelete = new ArrayList<JRDesignElement>();
            List<JRDesignElement> toAdd = new ArrayList<JRDesignElement>();

            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignTextField) {
                    JRDesignTextField textField = (JRDesignTextField) element;
                    transformTextField(design, textField, toAdd, toDelete);
                }
            }
            for (JRDesignElement element : toDelete) {
                designBand.removeElement(element);
            }
            for (JRDesignElement element : toAdd) {
                designBand.addElement(element);
            }
        }
    }

    private void transformTextField(JasperDesign design, JRDesignTextField textField,
                                    List<JRDesignElement> toAdd, List<JRDesignElement> toDelete) {
        String exprText = textField.getExpression().getText();
        String id = null;
        if (exprText.startsWith("$F{") && exprText.endsWith("}")) {
            id = exprText.substring(3, exprText.length()-1);
        } else if (exprText.startsWith("\"") && exprText.endsWith("\"")) {
            id = exprText.substring(1, exprText.length()-1);
        }

        if (id != null) {
            String dataId = id;
            if (id.endsWith(ReportConstants.captionSuffix)) {
                dataId = id.substring(0, id.length() - ReportConstants.captionSuffix.length());
            }
            if (compositeColumnValues.containsKey(dataId)) {
                toDelete.add(textField);
                design.removeField(id);
                int newFieldsCount = compositeColumnValues.get(dataId).size();
                List<JRDesignTextField> subFields = makeFieldPartition(textField, newFieldsCount);

                for (int i = 0; i < newFieldsCount; i++) {
                    JRDesignExpression subExpr = new JRDesignExpression();
                    subExpr.setValueClassName(textField.getExpression().getValueClassName());
                    if (exprText.startsWith("\"")) {  // caption без property
                        subExpr.setText(exprText);
                    } else {
                        String fieldName = id + ClientReportData.beginMarker + i + ClientReportData.endMarker;
                        subExpr.setText("$F{" + fieldName + "}");
                        JRDesignField designField = new JRDesignField();
                        designField.setName(fieldName);
                        designField.setValueClassName(subExpr.getValueClassName());
                        try {
                            design.addField(designField);
                        } catch (JRException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    subFields.get(i).setExpression(subExpr);
                }
                toAdd.addAll(subFields);
            }
        }
    }

    // Разбивает поле на cnt полей с примерно одинаковой шириной
    private static List<JRDesignTextField> makeFieldPartition(JRDesignTextField textField, int cnt) {
        List<JRDesignTextField> res = new ArrayList<JRDesignTextField>();
        int widthLeft = textField.getWidth();
        int x = textField.getX();
        int fieldsLeft = cnt;

        for (int i = 0; i < cnt; i++) {
            JRDesignTextField subField = (JRDesignTextField) textField.clone();

            int subWidth = widthLeft / fieldsLeft;
            subField.setWidth(subWidth);
            subField.setX(x);

            x += subWidth;
            widthLeft -= subWidth;
            --fieldsLeft;

            res.add(subField);
        }
        return res;
    }

    public static void exportToExcel(RemoteFormInterface remoteForm) {
        try {

            File tempFile = File.createTempFile("lsf", ".xls");

            JExcelApiExporter xlsExporter = new JExcelApiExporter();

            ReportGenerator report = new ReportGenerator(remoteForm);
            JasperPrint print = report.createReport(true, true, null);
            print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

            xlsExporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            xlsExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
            xlsExporter.exportReport();

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            }

            tempFile.deleteOnExit();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте в Excel", e);
        }
    }
}
