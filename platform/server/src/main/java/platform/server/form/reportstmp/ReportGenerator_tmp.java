package platform.server.form.reportstmp;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.ByteArray;
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
 * Date: 03.01.11
 * Time: 16:50
 */

public class ReportGenerator_tmp {
    private final String rootID;
    private final Map<String, List<String>> hierarchy;
    private final Map<String, JasperDesign> designs;
    private final Map<String, ClientReportData_tmp> data;
    private final Map<String, List<List<Object>>> compositeColumnValues;

    private static class SourcesGenerationOutput {
        public Map<String, ClientReportData_tmp> data;
        // данные для свойств с группами в колонках
        // объекты, от которых зависит свойство
        public Map<String, List<Integer>> compositeFieldsObjects;
        public Map<String, Map<List<Object>, Object>> compositeObjectValues;
        // объекты, идущие в колонки
        public Map<String, List<Integer>> compositeColumnObjects;
        public Map<String, List<List<Object>>> compositeColumnValues;
    }

    public ReportGenerator_tmp(RemoteFormInterface remoteForm, boolean toExcel, boolean ignorePagination) throws IOException, ClassNotFoundException, JRException {
        this(remoteForm, toExcel, ignorePagination, new HashMap<ByteArray, String>());
    }

    public ReportGenerator_tmp(RemoteFormInterface remoteForm, boolean toExcel, boolean ignorePagination, Map<ByteArray, String> files) throws IOException, ClassNotFoundException, JRException {
        Pair<String, Map<String, List<String>>> hpair = retrieveReportHierarchy(remoteForm);
        rootID = hpair.first;
        hierarchy = hpair.second;
        designs = retrieveReportDesigns(remoteForm, toExcel);

        SourcesGenerationOutput output = retrieveReportSources(remoteForm, files);
        data = output.data;
        compositeColumnValues = output.compositeColumnValues;

        transformDesigns(ignorePagination);
    }

    public JasperPrint createReport() throws JRException {

        Pair<Map<String, Object>, JRDataSource> compileParams = prepareReportSources();

        JasperReport report = JasperCompileManager.compileReport(designs.get(rootID));
        return JasperFillManager.fillReport(report, compileParams.first, compileParams.second);
    }

    private Pair<Map<String, Object>, JRDataSource> prepareReportSources() throws JRException {
        Map<String, Object> params = new HashMap<String, Object>();
        for (String childID : hierarchy.get(rootID)) {
            iterateChildSubreports(childID, params);
        }

        ReportRootDataSource_tmp rootSource = new ReportRootDataSource_tmp();
        return new Pair<Map<String, Object>, JRDataSource>(params, rootSource);
    }

    private ReportDependentDataSource_tmp iterateChildSubreports(String parentID, Map<String, Object> params) throws JRException {
        Map<String, Object> localParams = new HashMap<String, Object>();
        List<ReportDependentDataSource_tmp> childSources = new ArrayList<ReportDependentDataSource_tmp>();
        ReportDependentDataSource_tmp source = new ReportDependentDataSource_tmp(data.get(parentID), childSources);

        for (String childID : hierarchy.get(parentID)) {
            ReportDependentDataSource_tmp childSource = iterateChildSubreports(childID, localParams);
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

    private static SourcesGenerationOutput retrieveReportSources(RemoteFormInterface remoteForm, Map<ByteArray, String> files) throws IOException, ClassNotFoundException {
        SourcesGenerationOutput output = new SourcesGenerationOutput();
        byte[] sourcesArray = remoteForm.getReportSourcesByteArray();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(sourcesArray));
        int size = dataStream.readInt();
        output.data = new HashMap<String, ClientReportData_tmp>();
        for (int i = 0; i < size; i++) {
            String sid = dataStream.readUTF();
            ClientReportData_tmp reportData = new ClientReportData_tmp(dataStream, files);
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

        for (ClientReportData_tmp data : output.data.values()) {
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
        String id;
        if (exprText.startsWith("$F{")) {
            id = exprText.substring(3, exprText.length()-1);
        } else {
            assert exprText.startsWith("\"");  // Текст должен содержаться в кавычках
            id = exprText.substring(1, exprText.length()-1);
        }

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
                    String fieldName = id + ClientReportData_tmp.beginMarker + i + ClientReportData_tmp.endMarker;
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
}
