package jasperapi;

import lsfusion.base.BaseUtils;
import lsfusion.base.ByteArray;
import lsfusion.base.Pair;
import lsfusion.interop.form.ReportConstants;
import lsfusion.interop.form.ReportGenerationData;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;

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
    private final ReportGenerationData generationData;

    private String rootID;
    private TimeZone reportTimeZone;

    private Map<String, List<String>> hierarchy;
    private Map<String, JasperDesign> designs;
    private Map<String, ClientReportData> data;
    private Map<String, List<List<Object>>> compositeColumnValues;
    private boolean toExcel;

    // Для того, чтобы в отчетах данные выводились по несколько раз, нужно создать в .jrxml файле parameter строкового типа
    // с таким именем, и в default value expression вписать имя field'а, который будет содержать количество копий
    // имя должно быть, как строковая константа, в двойных кавычках
    private final String repeatPropertyFieldName = "REPORT_REPEAT_FIELD";

    public static final String SIDES_PROPERTY_NAME = "print-sides";
    public static final String TRAY_PROPERTY_NAME = "print-tray";

    public static class SourcesGenerationOutput {
        public Map<String, ClientReportData> data;
        // данные для свойств с группами в колонках
        // объекты, от которых зависит свойство
        public Map<String, List<Integer>> compositeFieldsObjects;
        public Map<String, Map<List<Object>, Object>> compositeObjectValues;
        // объекты, идущие в колонки
        public Map<String, List<Integer>> compositeColumnObjects;
        public Map<String, List<List<Object>>> compositeColumnValues;
    }

    public ReportGenerator(ReportGenerationData generationData, TimeZone timeZone) {
        this.generationData = generationData;
        this.reportTimeZone = timeZone;
    }

    public JasperPrint createReport(boolean ignorePagination, Map<ByteArray, String> files) throws ClassNotFoundException, IOException, JRException {
        Pair<String, Map<String, List<String>>> hpair = retrieveReportHierarchy(generationData.reportHierarchyData);
        return createReport(hpair, ignorePagination, files);
    }

    private JasperPrint createReport(Pair<String, Map<String, List<String>>> hpair, boolean ignorePagination, Map<ByteArray, String> files) throws IOException, ClassNotFoundException, JRException {
        rootID = hpair.first;
        hierarchy = hpair.second;

        return createJasperPrint(ignorePagination, files);
    }

    private JasperPrint createJasperPrint( boolean ignorePagination, Map<ByteArray, String> files) throws ClassNotFoundException, IOException, JRException {
        designs = retrieveReportDesigns(generationData);

        SourcesGenerationOutput output = retrieveReportSources(generationData, files);
        data = output.data;
        compositeColumnValues = output.compositeColumnValues;

        transformDesigns(ignorePagination);

        Pair<Map<String, Object>, JRDataSource> compileParams = prepareReportSources();

        JasperReport report = JasperCompileManager.compileReport(designs.get(rootID));
        
        JasperPrint print = JasperFillManager.fillReport(report, compileParams.first, compileParams.second);
        print.setProperty(SIDES_PROPERTY_NAME, designs.get(rootID).getProperty(SIDES_PROPERTY_NAME));
        print.setProperty(TRAY_PROPERTY_NAME, designs.get(rootID).getProperty(TRAY_PROPERTY_NAME));
        return print;
    }

    private Pair<Map<String, Object>, JRDataSource> prepareReportSources() throws JRException {
        Map<String, Object> params = new HashMap<String, Object>();
        for (String childID : hierarchy.get(rootID)) {
            iterateChildSubreports(childID, params);
        }

        ReportRootDataSource rootSource = new ReportRootDataSource();
        return new Pair<Map<String, Object>, JRDataSource>(params, rootSource);
    }

    private String getRepeatCountPropName(String parentID) {
        String propName = null;
        JRParameter parameter = designs.get(parentID).getParametersMap().get(repeatPropertyFieldName);
        if (parameter != null) {
            propName = parameter.getDefaultValueExpression().getText();
            if (propName != null && propName.length() > 1) {
                propName = propName.substring(1, propName.length()-1);
            } else {
                propName = null;
            }
        }
        return propName;
    }

    private ReportDependentDataSource iterateChildSubreports(String parentID, Map<String, Object> params) throws JRException {
        Map<String, Object> localParams = new HashMap<String, Object>();
        List<ReportDependentDataSource> childSources = new ArrayList<ReportDependentDataSource>();

        String repeatCountPropName = getRepeatCountPropName(parentID);
        ReportDependentDataSource source = new ReportDependentDataSource(data.get(parentID), childSources, repeatCountPropName);

        for (String childID : hierarchy.get(parentID)) {
            ReportDependentDataSource childSource = iterateChildSubreports(childID, localParams);
            childSources.add(childSource);
        }

        params.put(parentID + ReportConstants.reportSuffix, JasperCompileManager.compileReport(designs.get(parentID)));
        params.put(parentID + ReportConstants.sourceSuffix, source);
        params.put(parentID + ReportConstants.paramsSuffix, localParams);
        params.put(JRParameter.REPORT_TIME_ZONE, reportTimeZone);
        return source;
    }

    public static Pair<String, Map<String, java.util.List<String>>> retrieveReportHierarchy(byte[] array) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(array));
        String rootID = objStream.readUTF();
        Map<String, java.util.List<String>> hierarchy = (Map<String, java.util.List<String>>) objStream.readObject();
        return new Pair<String, Map<String, java.util.List<String>>>(rootID, hierarchy);
    }

    private static Map<String, JasperDesign> retrieveReportDesigns(ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(generationData.reportDesignData));
        return (Map<String, JasperDesign>) objStream.readObject();
    }

    public static SourcesGenerationOutput retrieveReportSources(ReportGenerationData generationData, Map<ByteArray, String> files) throws IOException, ClassNotFoundException {
        SourcesGenerationOutput output = new SourcesGenerationOutput();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(generationData.reportSourceData));
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
                } else if (fieldId.endsWith(ReportConstants.footerSuffix)) {
                    dataFieldId = fieldId.substring(0, fieldId.length() - ReportConstants.footerSuffix.length());
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

    private void transformDesigns(boolean ignorePagination) {
        for (JasperDesign design : designs.values()) {
            transformDesign(design, ignorePagination);
            design.setIgnorePagination(ignorePagination);
        }
    }

    private void transformDesign(JasperDesign design, boolean ignorePagination) {
        transformBand(design, design.getTitle(), ignorePagination);
        transformBand(design, design.getPageHeader(), ignorePagination);
        transformBand(design, design.getColumnHeader(), ignorePagination);
        transformBand(design, design.getColumnFooter(), ignorePagination);
        transformBand(design, design.getPageFooter(), ignorePagination);
        transformBand(design, design.getLastPageFooter(), ignorePagination);
        transformBand(design, design.getSummary(), ignorePagination);

        transformSection(design, design.getDetailSection(), ignorePagination);
        for (JRGroup group : design.getGroups()) {
            transformSection(design, group.getGroupHeaderSection(), ignorePagination);
            transformSection(design, group.getGroupFooterSection(), ignorePagination);
        }
    }

    private void transformSection(JasperDesign design, JRSection section, boolean ignorePagination) {
        if (section instanceof JRDesignSection) {
            JRDesignSection designSection = (JRDesignSection) section;
            List bands = designSection.getBandsList();
            for (Object band : bands) {
                if (band instanceof JRBand) {
                    transformBand(design, (JRBand) band, ignorePagination);
                }
            }
        }
    }

    private void transformBand(JasperDesign design, JRBand band, boolean ignorePagination) {
        if (band instanceof JRDesignBand) {
            JRDesignBand designBand = (JRDesignBand) band;
            List<JRDesignElement> toDelete = new ArrayList<JRDesignElement>();
            List<JRDesignElement> toAdd = new ArrayList<JRDesignElement>();

            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignTextField) {
                    JRDesignTextField textField = (JRDesignTextField) element;
                    transformTextField(design, textField, toAdd, toDelete);
                } else if (ignorePagination && element instanceof JRDesignBreak) {
                    toDelete.add((JRDesignBreak) element);
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
            } else if (id.endsWith(ReportConstants.footerSuffix)) {
                dataId = id.substring(0, id.length() - ReportConstants.footerSuffix.length());
            }
            if (compositeColumnValues.containsKey(dataId)) {
                toDelete.add(textField);
                JRField removedField = design.removeField(id);
                int newFieldsCount = compositeColumnValues.get(dataId).size();
                List<JRDesignTextField> subFields = makeFieldPartition(textField, newFieldsCount);

                for (int i = 0; i < newFieldsCount; i++) {
                    JRDesignExpression subExpr = new JRDesignExpression();
                    if (exprText.startsWith("\"")) {  // caption без property
                        subExpr.setText(exprText);
                    } else {
                        String fieldName = id + ClientReportData.beginMarker + i + ClientReportData.endMarker;
                        subExpr.setText("$F{" + fieldName + "}");
                        JRDesignField designField = new JRDesignField();
                        designField.setName(fieldName);
                        designField.setValueClassName(removedField.getValueClassName());

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

    public static void exportToExcelAndOpen(ReportGenerationData generationData, TimeZone timeZone) {
        try {
            File tempFile = exportToExcel(generationData, timeZone);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile);
            }

            tempFile.deleteOnExit();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте в Excel", e);
        }
    }

    private static File exportToExcel(ReportGenerationData generationData, TimeZone timeZone) throws IOException, ClassNotFoundException, JRException {
        File tempFile = File.createTempFile("lsf", ".xls");

        JExcelApiExporter xlsExporter = new JExcelApiExporter();

        ReportGenerator report = new ReportGenerator(generationData, timeZone);

        JasperPrint print = report.createReport(true, null);
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

        xlsExporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        xlsExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
        xlsExporter.exportReport();

        return tempFile;
    }

    public static byte[] exportToExcelByteArray(ReportGenerationData generationData, TimeZone timeZone) {
        try {
            File tempFile = exportToExcel(generationData, timeZone);
            FileInputStream fis = new FileInputStream(tempFile);
            byte[] array = new byte[(int) tempFile.length()];
            fis.read(array);
            return array;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте в Excel", e);
        }
    }
}
