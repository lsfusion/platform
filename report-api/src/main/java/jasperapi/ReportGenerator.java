package jasperapi;

import lsfusion.base.BaseUtils;
import lsfusion.base.ByteArray;
import lsfusion.base.Pair;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportConstants;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.form.ReportGenerationDataType;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsAbstractExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSwapFile;

import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.Sides;
import java.awt.*;
import java.io.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.interop.form.ReportConstants.*;

public class ReportGenerator {
    private final ReportGenerationData generationData;

    private String rootID;

    private Map<String, List<String>> hierarchy;
    private Map<String, JasperDesign> designs;
    private Map<String, ClientReportData> data;
    private Map<String, List<List<Object>>> compositeColumnValues;
    private boolean toExcel;

    // Для того, чтобы в отчетах данные выводились по несколько раз, нужно создать в .jrxml файле parameter строкового типа
    // с таким именем, и в default value expression вписать имя field'а, который будет содержать количество копий,
    // в формате $F{fieldName} 
    private final String repeatPropertyFieldName = "REPORT_REPEAT_FIELD";

    public static final String SIDES_PROPERTY_NAME = "print-sides";
    public static final String TRAY_PROPERTY_NAME = "print-tray";
    public static final String SHEET_COLLATE_PROPERTY_NAME = "sheet-collate";

    public static final Map<String, Sides> SIDES_VALUES = new HashMap<>();
    public static final Map<String, MediaTray> TRAY_VALUES = new HashMap<>();
    
    public final JRSwapFileVirtualizer virtualizer;  
    
    static {
        SIDES_VALUES.put("one-sided", Sides.ONE_SIDED);
        SIDES_VALUES.put("two-sided-long-edge", Sides.TWO_SIDED_LONG_EDGE);
        SIDES_VALUES.put("two-sided-short-edge", Sides.TWO_SIDED_SHORT_EDGE);

        TRAY_VALUES.put("top", MediaTray.TOP);
        TRAY_VALUES.put("middle", MediaTray.MIDDLE);
        TRAY_VALUES.put("bottom", MediaTray.BOTTOM);
        TRAY_VALUES.put("envelope", MediaTray.ENVELOPE);
        TRAY_VALUES.put("manual", MediaTray.MANUAL);
        TRAY_VALUES.put("large-capacity", MediaTray.LARGE_CAPACITY);
        TRAY_VALUES.put("main", MediaTray.MAIN);
        TRAY_VALUES.put("side", MediaTray.SIDE);
    }

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

    public ReportGenerator(ReportGenerationData generationData) {
        this.generationData = generationData;
        // Числовые значения 2048 и 1024 взяты из примеров, 40 было выбрано в результате тестирования
        this.virtualizer = new JRSwapFileVirtualizer(40, new JRSwapFile(System.getProperty("java.io.tmpdir"), 2048, 1024));
        JRVirtualizationHelper.setThreadVirtualizer(virtualizer);
    }

    public JasperPrint createReport(boolean ignorePagination, Map<ByteArray, String> files) throws ClassNotFoundException, IOException, JRException {
        return createReport(ignorePagination, files, false);
    }

    public JasperPrint createReport(boolean ignorePagination, Map<ByteArray, String> files, boolean fixBoolean) throws ClassNotFoundException, IOException, JRException {
        Pair<String, Map<String, List<String>>> hpair = retrieveReportHierarchy(generationData.reportHierarchyData);
        return createReport(hpair, ignorePagination, files, fixBoolean);
    }

    private JasperPrint createReport(Pair<String, Map<String, List<String>>> hpair, boolean ignorePagination, Map<ByteArray, String> files, boolean fixBoolean) throws IOException, ClassNotFoundException, JRException {
        rootID = hpair.first;
        hierarchy = hpair.second;

        return createJasperPrint(ignorePagination, files, fixBoolean);
    }

    private JasperPrint createJasperPrint( boolean ignorePagination, Map<ByteArray, String> files, boolean fixBoolean) throws ClassNotFoundException, IOException, JRException {
        designs = retrieveReportDesigns(generationData);

        SourcesGenerationOutput output = retrieveReportSources(generationData, files, fixBoolean);
        data = output.data;
        compositeColumnValues = output.compositeColumnValues;

        transformDesigns(ignorePagination);

        Pair<Map<String, Object>, JRDataSource> compileParams = prepareReportSources(virtualizer);

        JasperReport report = JasperCompileManager.compileReport(designs.get(rootID));
        
        JasperPrint print = JasperFillManager.fillReport(report, compileParams.first, compileParams.second);
        print.setProperty(SIDES_PROPERTY_NAME, designs.get(rootID).getProperty(SIDES_PROPERTY_NAME));
        print.setProperty(TRAY_PROPERTY_NAME, designs.get(rootID).getProperty(TRAY_PROPERTY_NAME));
        print.setProperty(SHEET_COLLATE_PROPERTY_NAME, designs.get(rootID).getProperty(SHEET_COLLATE_PROPERTY_NAME));
        return print;
    }

    private Pair<Map<String, Object>, JRDataSource> prepareReportSources(JRVirtualizer virtualizer) throws JRException {
        Map<String, Object> params = new HashMap<>();
        for (String childID : hierarchy.get(rootID)) {
            iterateChildSubreports(childID, params, virtualizer);
        }

        ReportRootDataSource rootSource = new ReportRootDataSource();
        return new Pair<Map<String, Object>, JRDataSource>(params, rootSource);
    }

    private String getRepeatCountPropName(String parentID) {
        String propName = null;
        JRParameter parameter = designs.get(parentID).getParametersMap().get(repeatPropertyFieldName);
        if (parameter != null) {
            propName = parameter.getDefaultValueExpression().getText();
            if (propName != null) {
                propName = propName.replaceAll("\\s", "");
                if (propName.length() > 4) {
                    propName = propName.substring(3, propName.length() - 1);
                } else {
                    propName = null;
                }
            }
        }
        return propName;
    }

    private ReportDependentDataSource iterateChildSubreports(String parentID, Map<String, Object> params, JRVirtualizer virtualizer) throws JRException {
        Map<String, Object> localParams = new HashMap<>();
        List<ReportDependentDataSource> childSources = new ArrayList<>();

        String repeatCountPropName = getRepeatCountPropName(parentID);
        ReportDependentDataSource source = new ReportDependentDataSource(data.get(parentID), childSources, repeatCountPropName);

        for (String childID : hierarchy.get(parentID)) {
            ReportDependentDataSource childSource = iterateChildSubreports(childID, localParams, virtualizer);
            childSources.add(childSource);
        }

        params.put(parentID + ReportConstants.reportSuffix, JasperCompileManager.compileReport(designs.get(parentID)));
        params.put(parentID + ReportConstants.sourceSuffix, source);
        params.put(parentID + ReportConstants.paramsSuffix, localParams);
        params.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);

        return source;
    }

    public static Pair<String, Map<String, java.util.List<String>>> retrieveReportHierarchy(byte[] array) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(array));
        String rootID = objStream.readUTF();
        Map<String, java.util.List<String>> hierarchy = (Map<String, java.util.List<String>>) objStream.readObject();
        return new Pair<>(rootID, hierarchy);
    }

    public static Map<String, Map<String, String>> retrievePropertyCaptions(ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(generationData.reportDesignData));
        return (Map<String, Map<String, String>>) objStream.readObject();
    }

    private static Map<String, JasperDesign> retrieveReportDesigns(ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(generationData.reportDesignData));
        return (Map<String, JasperDesign>) objStream.readObject();
    }

    public static SourcesGenerationOutput retrieveReportSources(ReportGenerationData generationData, Map<ByteArray, String> files) throws IOException {
        return retrieveReportSources(generationData, files, ReportGenerationDataType.PRINTJASPER, false);
    }

    public static SourcesGenerationOutput retrieveReportSources(ReportGenerationData generationData, Map<ByteArray, String> files, boolean fixBoolean) throws IOException {
        return retrieveReportSources(generationData, files, ReportGenerationDataType.PRINTJASPER, fixBoolean);
    }

    public static SourcesGenerationOutput retrieveReportSources(ReportGenerationData generationData, Map<ByteArray, String> files, ReportGenerationDataType reportType) throws IOException {
       return retrieveReportSources(generationData, files, reportType, false);
    }

    //corresponding serialization is in lsfusion.server.remote.FormReportManager.getReportSourcesByteArray()
    public static SourcesGenerationOutput retrieveReportSources(ReportGenerationData generationData, Map<ByteArray, String> files, ReportGenerationDataType reportType, boolean fixBoolean) throws IOException {
        SourcesGenerationOutput output = new SourcesGenerationOutput();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(generationData.reportSourceData));
        int size = dataStream.readInt();
        output.data = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String sid = dataStream.readUTF();
            ClientReportData reportData = new ClientReportData(dataStream, files, reportType, fixBoolean);
            output.data.put(sid, reportData);
        }

        int compositePropsCnt = dataStream.readInt();

        output.compositeFieldsObjects = retrievePropertyObjects(dataStream, compositePropsCnt);

        int compositeFieldsCnt = dataStream.readInt();
        output.compositeObjectValues = new HashMap<>();
        for (int i = 0; i < compositeFieldsCnt; i++) {
            String fieldId = dataStream.readUTF();
            Map<List<Object>, Object> data = new HashMap<>();
            int valuesCnt = dataStream.readInt();
            for (int j = 0; j < valuesCnt; j++) {
                List<Object> values = new ArrayList<>();
                String dataFieldId = fieldId;
                if (fieldId.endsWith(headerSuffix)) {
                    dataFieldId = fieldId.substring(0, fieldId.length() - headerSuffix.length());
                } else if (fieldId.endsWith(footerSuffix)) {
                    dataFieldId = fieldId.substring(0, fieldId.length() - footerSuffix.length());
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

        output.compositeColumnValues = new HashMap<>();
        for (int i = 0; i < compositePropsCnt; i++) {
            String fieldId = dataStream.readUTF();
            List<List<Object>> data = new ArrayList<>();
            int valuesCnt = dataStream.readInt();
            for (int j = 0; j < valuesCnt; j++) {
                List<Object> values = new ArrayList<>();
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
        Map<String, List<Integer>> objects = new HashMap<>();
        for (int i = 0; i < propCnt; i++) {
            String fieldId = stream.readUTF();
            List<Integer> objectsId = new ArrayList<>();
            int objectCnt = stream.readInt();
            for (int j = 0; j < objectCnt; j++) {
                objectsId.add(stream.readInt());
            }
            objects.put(fieldId, objectsId);
        }
        return objects;
    }

    private void transformDesigns(boolean ignorePagination) throws JRException {
        for (JasperDesign design : designs.values()) {
            transformDesign(design, ignorePagination);
            design.setIgnorePagination(ignorePagination);
        }
    }

    private String indexSuffix(int i) {
        return beginIndexMarker + i + endIndexMarker;
    }
    
    public static final Pattern fieldPattern = Pattern.compile("\\$F\\{[\\w.\\[\\](),]+\\}");

    private String getFieldName(String fieldString) {
        return fieldString.substring(3, fieldString.length() - 1);
    }
    
    // "fieldName.header" -> "fieldName"
    private String getBaseFieldNameFromName(String fieldName) {
        if (fieldName.endsWith(headerSuffix)) {
            return fieldName.substring(0, fieldName.length() - headerSuffix.length());
        } else if (fieldName.endsWith(footerSuffix)) {
            return fieldName.substring(0, fieldName.length() - footerSuffix.length());
        } else {
            return fieldName;
        }
    }
    
    // "$F{fieldName.header}" -> "fieldName" 
    private String getBaseFieldName(String fieldString) {
        return getBaseFieldNameFromName(getFieldName(fieldString));
    }
    
    private boolean isColumnPropertyField(String fieldName) {
        String baseFieldName = getBaseFieldNameFromName(fieldName);
        return compositeColumnValues.containsKey(baseFieldName);
    }
    
    private String findColumnFieldName(JRExpression expr) {
        String exprText = expr.getText();
        Matcher match = fieldPattern.matcher(exprText);
        while (match.find()) {
            String fieldName = getFieldName(match.group());
            if (isColumnPropertyField(fieldName)) {
                return fieldName;
            }
        }
        return null;
    }
    
    private String findFieldNameToSplitStyle(JRStyle style) {
        final JRConditionalStyle[] condStyles = style.getConditionalStyles();
        if (condStyles != null) {
            for (JRConditionalStyle condStyle : condStyles) {
                String columnFieldName = findColumnFieldName(condStyle.getConditionExpression());
                if (columnFieldName != null) {
                    return columnFieldName;
                } 
            }
        }
        return null;
    }
    
    private void transformExpression(JRDesignExpression expr, int index) {
        String exprText = expr.getText();
        Set<String> columnFieldsStr = new HashSet<>();
        Matcher match = fieldPattern.matcher(exprText);
        while (match.find()) {  
            String fieldStr = match.group();
            if (isColumnPropertyField(getFieldName(fieldStr))) {
                columnFieldsStr.add(fieldStr);
            }
        }
        
        for (String fieldStr : columnFieldsStr) {
            String newFieldStr = fieldStr.substring(0, fieldStr.length() - 1) + indexSuffix(index) + "}";
            exprText = exprText.replace(fieldStr, newFieldStr);
        }
        expr.setText(exprText);
    }
    
    private void transformStyleExpressions(JRDesignStyle style, int index) {
        final JRConditionalStyle[] condStyles = style.getConditionalStyles();
        if (condStyles != null) {
            for (JRConditionalStyle condStyle : condStyles) {
                if (condStyle.getConditionExpression() instanceof JRDesignExpression) {
                    transformExpression((JRDesignExpression) condStyle.getConditionExpression(), index);
                }                
            }
        }
    }
    
    private Set<String> transformColumnDependentStyles(JasperDesign design) throws JRException {
        Set<String> styleNames = new HashSet<>();
        for (JRStyle style : design.getStyles()) {
            if (style instanceof JRDesignStyle) {
                String fieldName = findFieldNameToSplitStyle(style);
                if (fieldName != null) {
                    String baseFieldName = getBaseFieldNameFromName(fieldName);
                    styleNames.add(style.getName());
                    for (int i = 0; i < compositeColumnValues.get(baseFieldName).size(); ++i) {
                        JRDesignStyle newStyle = (JRDesignStyle) style.clone();
                        newStyle.setName(style.getName() + indexSuffix(i));
                        transformStyleExpressions(newStyle, i);
                        design.addStyle(newStyle);
                    }
                }
            }
        }
        return styleNames;
    } 
    
    private List<JRBand> getBands(JRSection section) {
        if (section instanceof JRDesignSection) {
            return ((JRDesignSection) section).getBandsList();
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<JRBand> getBands(JasperDesign design) {
        List<JRBand> bands = new ArrayList<>();
        bands.add(design.getTitle());
        bands.add(design.getPageHeader());
        bands.add(design.getColumnHeader());
        bands.add(design.getColumnFooter());
        bands.add(design.getPageFooter());
        bands.add(design.getLastPageFooter());
        bands.add(design.getSummary());
        
        bands.addAll(getBands(design.getDetailSection()));
        
        for (JRGroup group : design.getGroups()) {
            bands.addAll(getBands(group.getGroupHeaderSection()));
            bands.addAll(getBands(group.getGroupFooterSection()));
        }
        return bands;
    }
    
    private void transformFields(JasperDesign design) throws JRException {
        for (JRField f : design.getFields()) {
            if (f instanceof JRDesignField) {
                JRDesignField field = (JRDesignField) f;
                String fieldName = field.getName();
                String baseFieldName = getBaseFieldNameFromName(fieldName);
                if (compositeColumnValues.containsKey(baseFieldName)) { 
                    JRField removedField = design.removeField(fieldName);

                    for (int i = 0; i < compositeColumnValues.get(baseFieldName).size(); ++i) {
                        String newFieldName = fieldName + indexSuffix(i);
                        JRDesignField designField = new JRDesignField();
                        designField.setName(newFieldName);
                        designField.setValueClassName(removedField.getValueClassName());

                        design.addField(designField);
                    }                        
                }
            }
        }
    }

    private void transformDesign(JasperDesign design, boolean ignorePagination) throws JRException {
        Set<String> transformedStyleNames = transformColumnDependentStyles(design);
        transformFields(design);
        for (JRBand band : getBands(design)) {
            transformBand(design, band, ignorePagination, transformedStyleNames);
        }
    }

    private void transformBand(JasperDesign design, JRBand band, boolean ignorePagination, Set<String> transformedStyleNames) {
        if (band instanceof JRDesignBand) {
            JRDesignBand designBand = (JRDesignBand) band;
            List<JRDesignElement> toDelete = new ArrayList<>();
            List<JRDesignElement> toAdd = new ArrayList<>();

            Map<String, List<JRDesignTextField>> fieldsInGroup = new HashMap<>();
            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignTextField) {
                    String groupKey = element.getKey();
                    if (groupKey != null) {
                        if (!fieldsInGroup.containsKey(groupKey)) {
                            fieldsInGroup.put(groupKey, new ArrayList<JRDesignTextField>());
                        }
                        fieldsInGroup.get(groupKey).add((JRDesignTextField) element);
                    }
                }
            }
            
            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignTextField) {
                    JRDesignTextField textField = (JRDesignTextField) element;
                    if (textField.getExpression() != null) {
                        transformTextField(design, textField, fieldsInGroup, toAdd, toDelete, transformedStyleNames);
                    }
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

    private void transformTextFieldExpressions(JRDesignTextField oldField, JRDesignTextField newField, int i) {
        if (oldField.getExpression() != null) {
            JRDesignExpression subExpr = new JRDesignExpression(oldField.getExpression().getText());
            transformExpression(subExpr, i);
            newField.setExpression(subExpr);
        }
        
        if (oldField.getPrintWhenExpression() != null && oldField.getPrintWhenExpression().getText() != null) {
            JRDesignExpression subPWExpr = new JRDesignExpression(oldField.getPrintWhenExpression().getText());
            transformExpression(subPWExpr, i);
            newField.setPrintWhenExpression(subPWExpr);
        }

        if (oldField.getPatternExpression() != null && oldField.getPatternExpression().getText() != null) {
            JRDesignExpression subPatExpr = new JRDesignExpression(oldField.getPatternExpression().getText());
            transformExpression(subPatExpr, i);
            newField.setPatternExpression(subPatExpr);
        }
    }
    
    private void transformTextField(JasperDesign design, JRDesignTextField textField, Map<String, List<JRDesignTextField>> fieldsInGroup,
                                    List<JRDesignElement> toAdd, List<JRDesignElement> toDelete, Set<String> transformedStyleNames) {
        String fieldName = findColumnFieldName(textField.getExpression());

        if (fieldName != null) {
            String baseFieldName = getBaseFieldNameFromName(fieldName);
            toDelete.add(textField);
            int newFieldsCount = compositeColumnValues.get(baseFieldName).size();
            if (newFieldsCount > 0) {
                List<JRDesignTextField> subFields = makeTextFieldPartition(textField, newFieldsCount, fieldsInGroup);
                String oldStyleName = textField.getStyle() == null ? null : textField.getStyle().getName();

                for (int i = 0; i < newFieldsCount; i++) {
                    transformTextFieldExpressions(textField, subFields.get(i), i);
                    if (oldStyleName != null && transformedStyleNames.contains(oldStyleName)) {
                        subFields.get(i).setStyle(design.getStylesMap().get(oldStyleName + indexSuffix(i)));
                    }
                }
                toAdd.addAll(subFields);
            }
        }
        if (textField.getPattern() == null) {
            Class<?> valueClass = textField.getExpression().getValueClass();
            DateFormat format = null;
            if (valueClass == Timestamp.class) {
                format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            } else if (valueClass == Date.class) {
                format = DateFormat.getDateInstance(DateFormat.SHORT);
            } else if (valueClass == Time.class) {
                format = DateFormat.getTimeInstance(DateFormat.SHORT);
            }
            
            if (format != null) {
                textField.setPattern(((SimpleDateFormat) format).toPattern());
            }
        }
    }

    private static Rectangle getBoundRectangle(JRDesignTextField textField, Map<String, List<JRDesignTextField>> fields) {
        if (!fields.containsKey(textField.getKey())) {
            return new Rectangle(textField.getX(), textField.getY(), textField.getWidth(), textField.getHeight());
        } else {
            boolean first = true;
            int minX = 0, minY = 0 , maxX = 0, maxY = 0;
            for (JRDesignTextField field : fields.get(textField.getKey())) {
                if (first) {
                    minX = field.getX(); minY = field.getY();
                    maxX = field.getX() + field.getWidth(); maxY = field.getY() + field.getHeight();
                    first = false;
                } else {
                    minX = Math.min(minX, field.getX());
                    maxX = Math.max(maxX, field.getX() + field.getWidth());
                    minY = Math.min(minY, field.getY());
                    maxY = Math.max(maxY, field.getY() + field.getHeight());
                }
            }
            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }
    }

    // Разбивает поле на cnt полей с примерно одинаковой шириной
    private static List<JRDesignTextField> makeTextFieldPartition(JRDesignTextField textField, int cnt, Map<String, List<JRDesignTextField>> fields) {
        List<JRDesignTextField> res = new ArrayList<>();

        Rectangle boundRect = getBoundRectangle(textField, fields);
        int baseX = (int) boundRect.getX();
        
        int xShift = (textField.getX() - baseX) / cnt;
        int xEndShift = (textField.getX() + textField.getWidth() - baseX) / cnt;
        int subWidth = (int)boundRect.getWidth() / cnt;
        boolean rightmost = (textField.getX() + textField.getWidth() == boundRect.getX() + boundRect.getWidth());
        
        for (int i = 0; i < cnt; ++i) {
            JRDesignTextField subField = (JRDesignTextField) textField.clone();
            subField.setX(baseX + xShift);
            int rightBound = (i+1 < cnt ? baseX + subWidth : (int)boundRect.getX() + (int)boundRect.getWidth());
            if (rightmost) {
                subField.setWidth(rightBound - subField.getX());
            } else {
                subField.setWidth(xEndShift - xShift);
            }
            res.add(subField);
            baseX += subWidth;
        }
        
        return res;
    }

    public static void exportAndOpen(ReportGenerationData generationData, FormPrintType type, boolean fixBoolean) {
        try {
            File tempFile = exportToFile(generationData, type, fixBoolean);

            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(tempFile);
                }
            } finally {
                tempFile.deleteOnExit();
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте в " + type, e);
        }
    }

    public static File exportToXls(ReportGenerationData generationData) throws ClassNotFoundException, IOException, JRException {
        return exportToFile(generationData, new JRXlsExporter(), "xls", true);
    }
    
    public static File exportToXlsx(ReportGenerationData generationData) throws IOException, ClassNotFoundException, JRException {
        return exportToFile(generationData, new JRXlsxExporter(), "xlsx", true);
    }
    
    public static File exportToPdf(ReportGenerationData generationData) throws IOException, ClassNotFoundException, JRException {
        return exportToFile(generationData, new JRPdfExporter(), "pdf", false);
    }

    public static File exportToDoc(ReportGenerationData generationData) throws IOException, ClassNotFoundException, JRException {
        return exportToFile(generationData, new JRDocxExporter(), "doc", false);
    }

    public static File exportToDocx(ReportGenerationData generationData) throws IOException, ClassNotFoundException, JRException {
        return exportToFile(generationData, new JRDocxExporter(), "docx", false);
    }
    
    private static JRAbstractExporter getExporter(FormPrintType printType) {
        switch (printType) {
            case XLS:
                return new JRXlsExporter();
            case XLSX:
                return new JRXlsxExporter();
            case PDF:
                return new JRPdfExporter();
            case DOC:
                return new JRDocxExporter();
            case DOCX:
                return new JRDocxExporter();
        }
        throw new UnsupportedOperationException();
    }

    public static File exportToFile(ReportGenerationData generationData, FormPrintType type) throws ClassNotFoundException, IOException, JRException {
        return exportToFile(generationData, type, false);
    }

    public static File exportToFile(ReportGenerationData generationData, FormPrintType type, boolean fixBoolean) throws ClassNotFoundException, IOException, JRException {
        return exportToFile(generationData, getExporter(type), type.getExtension(), type.isExcel(), fixBoolean);
    }
    
    private static File exportToFile(ReportGenerationData generationData, JRAbstractExporter exporter, String extension, boolean ignorePagination) throws IOException, JRException, ClassNotFoundException {
        return exportToFile(generationData, exporter, extension, ignorePagination, false);
    }

    private static File exportToFile(ReportGenerationData generationData, JRAbstractExporter exporter, String extension, boolean ignorePagination, boolean fixBoolean) throws IOException, JRException, ClassNotFoundException {
        File tempFile = File.createTempFile("lsf", "." + extension);

        ReportGenerator report = new ReportGenerator(generationData);

        JasperPrint print = report.createReport(ignorePagination, null, fixBoolean);
        print.setProperty(JRXlsAbstractExporterParameter.PROPERTY_DETECT_CELL_TYPE, "true");

        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
        exporter.exportReport();

        return tempFile;    
    }

    public static byte[] exportToExcelByteArray(ReportGenerationData generationData, FormPrintType type) {
        try {
            assert type.isExcel();
            File tempFile = type == FormPrintType.XLS ? exportToXls(generationData) : exportToXlsx(generationData);
            FileInputStream fis = new FileInputStream(tempFile);
            byte[] array = new byte[(int) tempFile.length()];
            //noinspection ResultOfMethodCallIgnored
            fis.read(array);
            JRVirtualizationHelper.clearThreadVirtualizer();
            return array;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте в Excel", e);
        }
    }
}
