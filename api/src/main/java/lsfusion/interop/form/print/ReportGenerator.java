package lsfusion.interop.form.print;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.classloader.RemoteClassLoader;
import lsfusion.base.classloader.WriteUsedClassLoader;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.export.XlsReportConfiguration;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;

import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.Sides;
import java.awt.*;
import java.io.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lsfusion.interop.form.print.ReportConstants.*;

public class ReportGenerator {
    private final ReportGenerationData generationData;

    private String rootID;

    private Map<String, List<String>> hierarchy;
    private Map<String, JasperDesign> designs;
    
    private Map<String, ClientKeyData> keyData;
    private ClientPropertyData propData;    

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

    public ReportGenerator(ReportGenerationData generationData) {
        this.generationData = generationData;
        // Числовые значения 2048 и 1024 взяты из примеров, 40 было выбрано в результате тестирования
        this.virtualizer = new JRSwapFileVirtualizer(40, new JRSwapFile(System.getProperty("java.io.tmpdir"), 2048, 1024));
        JRVirtualizationHelper.setThreadVirtualizer(virtualizer);
    }

    public JasperPrint createReport(FormPrintType printType, RemoteLogicsInterface remoteLogics) throws ClassNotFoundException, IOException, JRException {
        Pair<String, Map<String, List<String>>> hpair = retrieveReportHierarchy(generationData.reportHierarchyData);
        rootID = hpair.first;
        hierarchy = hpair.second;

        designs = retrieveReportDesigns(generationData);

        Pair<Map<String, ClientKeyData>, ClientPropertyData> output = retrieveReportSources(generationData);        
        keyData = output.first;
        propData = output.second;

        transformDesigns(printType.ignorePagination());

        Map<String, Object> params = new HashMap<>();
//        // external classloader required for correct Jasper report generation on clients
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        JasperPrint print;
        Thread.currentThread().setContextClassLoader(new WriteUsedClassLoader(retrieveClasses(generationData),
                originalClassloader instanceof RemoteClassLoader ? originalClassloader.getParent() : originalClassloader, //not possible to do via parent because it is used in a load class and not in a find
                remoteLogics));
        try {
            iterateChildReport(rootID, params, virtualizer);

            JasperReport report = (JasperReport) params.get(rootID + ReportConstants.reportSuffix);
            ReportDataSource source = (ReportDataSource) params.get(rootID + ReportConstants.sourceSuffix);
            Map<String, Object> childParams = (Map<String, Object>) params.get(rootID + ReportConstants.paramsSuffix);

            print = JasperFillManager.fillReport(report, childParams, source);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }

        JasperDesign rootDesign = designs.get(rootID);
        print.setProperty(SIDES_PROPERTY_NAME, rootDesign.getProperty(SIDES_PROPERTY_NAME));
        print.setProperty(TRAY_PROPERTY_NAME, rootDesign.getProperty(TRAY_PROPERTY_NAME));
        print.setProperty(SHEET_COLLATE_PROPERTY_NAME, rootDesign.getProperty(SHEET_COLLATE_PROPERTY_NAME));

        return print;
    }

    private ReportDataSource iterateChildReport(String reportID, Map<String, Object> params, JRVirtualizer virtualizer) throws JRException {
        String repeatCountPropName = getRepeatCountPropName(reportID);
        ReportDataSource source = new ReportDataSource(keyData.get(reportID), propData, repeatCountPropName);

        Map<String, Object> childParams = new HashMap<>();
        iterateChildReports(source, reportID, childParams, virtualizer);

        JasperReport report = JasperCompileManager.compileReport(designs.get(reportID));

        params.put(reportID + ReportConstants.reportSuffix, report);
        params.put(reportID + ReportConstants.sourceSuffix, source);
        params.put(reportID + ReportConstants.paramsSuffix, childParams);
        params.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);

        return source;        
    }
    
    private void iterateChildReports(ReportDataSource source, String parentID, Map<String, Object> params, JRVirtualizer virtualizer) throws JRException {
        for (String childID : hierarchy.get(parentID)) {
            ReportDataSource childSource = iterateChildReport(childID, params, virtualizer);
            
            source.addSubReportSource(childSource);
        }
    }

    private String getRepeatCountPropName(String reportID) {
        String propName = null;
        JRParameter parameter = designs.get(reportID).getParametersMap().get(repeatPropertyFieldName);
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

    public static Pair<String, Map<String, java.util.List<String>>> retrieveReportHierarchy(byte[] array) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(array));
        String rootID = objStream.readUTF();
        Map<String, java.util.List<String>> hierarchy = (Map<String, java.util.List<String>>) objStream.readObject();
        return new Pair<>(rootID, hierarchy);
    }

    private static Map<String, byte[]> retrieveClasses(ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        return (Map<String, byte[]>) new ObjectInputStream(new ByteArrayInputStream(generationData.classes)).readObject();
    }

    private static Map<String, JasperDesign> retrieveReportDesigns(ReportGenerationData generationData) throws IOException, ClassNotFoundException {
        ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(generationData.reportDesignData));
        return (Map<String, JasperDesign>) objStream.readObject();
    }

    public static Pair<Map<String, ClientKeyData>, ClientPropertyData> retrieveReportSources(ReportGenerationData generationData) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(generationData.reportSourceData));

        Map<Map<Integer, Object>, Map<Integer, Object>> cache = new HashMap<>();

        // deserialize keys
        Map<String, ClientKeyData> keyData = new HashMap<>();
        int size = dataStream.readInt();
        for (int i = 0; i < size; i++) {
            String sid = dataStream.readUTF();
            ClientKeyData reportData = new ClientKeyData(dataStream, cache);
            keyData.put(sid, reportData);
        }
        
        // deserializeProperties
        ClientPropertyData propData = new ClientPropertyData(dataStream, cache);
        
        return new Pair<>(keyData, propData);
    }

    private void transformDesigns(boolean ignorePagination) throws JRException {
        for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
            JasperDesign design = entry.getValue();
            String subreportID = entry.getKey();
            transformDesign(design, subreportID, ignorePagination);
            design.setIgnorePagination(ignorePagination);
        }
    }

    private String indexSuffix(int i) {
        return beginIndexMarker + i + endIndexMarker;
    }
    
    private static final Pattern fieldPattern = Pattern.compile("\\$F\\{[\\w.\\[\\](),]+\\}");

    // "$F{fieldName}" -> "fieldName"
    private String getFieldName(String fieldString) {
        return fieldString.substring(3, fieldString.length() - 1);
    }
    
    // "fieldName.header" -> "fieldName"
    public static String getBaseFieldName(String fullFieldName) {
        String fieldName = removeIndexMarkerIfExists(fullFieldName);
        for (ReportFieldExtraType type : ReportFieldExtraType.values()) {
            if (isCorrespondingFieldName(fieldName, type)) {
                String suffix = type.getReportFieldNameSuffix();
                return fieldName.substring(0, fieldName.length() - suffix.length());
            }
        }
        return fieldName;
    }

    private String findColumnFieldName(JRExpression expr, String subreportID) {
        String exprText = expr.getText();
        Matcher match = fieldPattern.matcher(exprText);
        while (match.find()) {
            String fieldName = getFieldName(match.group());
            if (hasColumns(fieldName, subreportID)) {
                return fieldName;
            }
        }
        return null;
    }

    private boolean hasColumns(String fieldName, String subreportID) { // optimization
        Result<Integer> minColumnsCount = new Result<>();
        int columnsCount = getColumnsCount(fieldName, subreportID, minColumnsCount);
        if(columnsCount != 1)
            return true;
        return !minColumnsCount.result.equals(1);
    }
    
    private int getColumnsCount(String fieldName, String subreportID) {
        assert hasColumns(fieldName, subreportID);
        return getColumnsCount(fieldName, subreportID, null);
    }
    
    private int getColumnsCount(String fieldName, String subreportID, Result<Integer> minColumnsCount) {
        Integer columnsCount = keyData.get(subreportID).getColumnsCount(fieldName, minColumnsCount);
        if(columnsCount != null)
            return columnsCount;

        return propData.getColumnsCount(fieldName, minColumnsCount);
    }

    private String findFieldNameToSplitStyle(JRStyle style, String subreportID) {
        final JRConditionalStyle[] condStyles = style.getConditionalStyles();
        if (condStyles != null) {
            for (JRConditionalStyle condStyle : condStyles) {
                String columnFieldName = findColumnFieldName(condStyle.getConditionExpression(), subreportID);
                if (columnFieldName != null) {
                    return columnFieldName;
                } 
            }
        }
        return null;
    }
    
    private void transformExpression(JRDesignExpression expr, int index, String subreportID) {
        String exprText = expr.getText();
        Set<String> columnFieldsStr = new HashSet<>();
        Matcher match = fieldPattern.matcher(exprText);
        while (match.find()) {  
            String fieldStr = match.group();
            if (hasColumns(getFieldName(fieldStr), subreportID)) {
                columnFieldsStr.add(fieldStr);
            }
        }
        
        for (String fieldStr : columnFieldsStr) {
            String newFieldStr = fieldStr.substring(0, fieldStr.length() - 1) + indexSuffix(index) + "}";
            exprText = exprText.replace(fieldStr, newFieldStr);
        }
        expr.setText(exprText);
    }
    
    private void transformStyleExpressions(JRDesignStyle style, int index, String subreportID) {
        final JRConditionalStyle[] condStyles = style.getConditionalStyles();
        if (condStyles != null) {
            for (JRConditionalStyle condStyle : condStyles) {
                if (condStyle.getConditionExpression() instanceof JRDesignExpression) {
                    transformExpression((JRDesignExpression) condStyle.getConditionExpression(), index, subreportID);
                }                
            }
        }
    }
    
    private Set<String> transformColumnDependentStyles(JasperDesign design, String subreportID) throws JRException {
        Set<String> styleNames = new HashSet<>();
        for (JRStyle style : design.getStyles()) {
            if (style instanceof JRDesignStyle) {
                String fieldName = findFieldNameToSplitStyle(style, subreportID);
                if (fieldName != null) {
                    styleNames.add(style.getName());
                    for (int i = 0, size = getColumnsCount(fieldName, subreportID); i < size; ++i) {
                        JRDesignStyle newStyle = (JRDesignStyle) style.clone();
                        newStyle.setName(style.getName() + indexSuffix(i));
                        transformStyleExpressions(newStyle, i, subreportID);
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
    
    private void transformFields(JasperDesign design, String subreportID) throws JRException {
        for (JRField f : design.getFields()) {
            if (f instanceof JRDesignField) {
                JRDesignField field = (JRDesignField) f;
                String fieldName = field.getName();
                if (fieldName != null && hasColumns(fieldName, subreportID)) { 
                    JRField removedField = design.removeField(fieldName);

                    for (int i = 0, size = getColumnsCount(fieldName, subreportID); i < size; ++i) {
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

    private static class Segment {
        public int left, right;
        
        public Segment(int left, int right) {
            this.left = left;
            this.right = right;
        }
        
        public int width() {
            return right - left;
        }
        
        public void shiftLeft(int distance) {
            left -= distance;
            right -= distance;
        } 
    }

    public static class FieldXBorder implements Comparable<FieldXBorder> {
        public int x;
        public boolean isLeft;

        public FieldXBorder(int x, boolean isLeft) {
            this.x = x;
            this.isLeft = isLeft;
        }

        @Override
        public int compareTo(FieldXBorder o) {
            if (x < o.x) return -1;
            if (x > o.x) return 1;
            if (isLeft && !o.isLeft) return -1;
            if (!isLeft && o.isLeft) return 1;
            return 0;
        }
    }

    private void transformDesign(JasperDesign design, String subreportID, boolean ignorePagination) throws JRException {
        Set<String> transformedStyleNames = transformColumnDependentStyles(design, subreportID);
        transformFields(design, subreportID);
        for (JRBand band : getBands(design)) {
            transformBand(design, band, ignorePagination, transformedStyleNames, subreportID);
        }
        
        if (generationData.useShowIf) {
            Collection<JRTextField> fieldsToHide = findTextFieldsToHide(design, subreportID);
            if (!fieldsToHide.isEmpty()) {
                cutSegments(design, findCutSegments(fieldsToHide));
            }
        }
    }

    private List<Segment> findCutSegments(Collection<JRTextField> fieldsToHide) {
        List<FieldXBorder> fieldsBorders = createSortedFieldsBordersList(fieldsToHide);
        return findCutSegments(fieldsBorders);
    }

    private List<FieldXBorder> createSortedFieldsBordersList(Collection<JRTextField> fieldsToHide) {
        List<FieldXBorder> borders = new ArrayList<>();
        for (JRTextField field : fieldsToHide) {
            borders.add(new FieldXBorder(field.getX(), true));
            borders.add(new FieldXBorder(field.getX() + field.getWidth(), false));
        }
        Collections.sort(borders);
        return borders;
    }
    
    private List<Segment> findCutSegments(List<FieldXBorder> borders) {
        List<Segment> segments = new ArrayList<>();
        int openedSegments = 0;
        int curLeft = 0;
        for (FieldXBorder ie : borders) {
            if (ie.isLeft) {
                ++openedSegments;
                if (openedSegments == 1) {
                    curLeft = ie.x;
                }
            } else {
                --openedSegments;
                if (openedSegments == 0) {
                    segments.add(new Segment(curLeft, ie.x));
                }
            }
        }
        return segments;
    }
    
    private Collection<JRTextField> findTextFieldsToHide(JasperDesign design, String subreportID) {
        return findTextFieldsToHide(design, getHidingFieldsNames(design, subreportID));
    }
    
    private List<JRTextField> findTextFieldsToHide(JasperDesign design, Set<String> hidingFieldsNames) {
        List<JRTextField> textFieldsToHide = new ArrayList<>();
        for (JRElement element : getDesignElements(design)) {
            if (element instanceof JRTextField) {
                JRTextField textElement = (JRTextField) element;
                if (textElement.getExpression() != null) {
                    String exprText = textElement.getExpression().getText();
                    if (exprText.startsWith("$F{")) {
                        String fieldName = getFieldName(exprText);
                        // We need to check already divided fields as well
                        if (hidingFieldsNames.contains(removeIndexMarkerIfExists(fieldName))) {
                            textFieldsToHide.add(textElement);
                        }
                    }
                }
            }
        }
        return textFieldsToHide;
    }
    
    private Set<String> getHidingFieldsNames(JasperDesign design, String subreportID) {
        Set<String> hidingFieldsNames = new HashSet<>();
        for (JRField field : design.getFieldsList()) {
            if (isActiveShowIfField(field.getName(), subreportID)) {
                String baseFieldName = getBaseFieldName(field.getName());
                hidingFieldsNames.add(baseFieldName);
            }
        }
        return hidingFieldsNames;        
    }
    
    private boolean isActiveShowIfField(final String fieldName, String subreportID) {
        if (isCorrespondingFieldName(fieldName, ReportFieldExtraType.SHOWIF)) {
            ClientKeyData clientKeyData = keyData.get(subreportID);
            if (!clientKeyData.keyRowsIsEmpty()) {
                return propData.getFieldValue(clientKeyData.getKeyRowsFirst(), fieldName) == null;
            }
        }
        return false;
    }

    private Collection<JRElement> getDesignElements(JasperDesign design) {
        Collection<JRElement> elements = new ArrayList<>();
        for (JRBand band : design.getAllBands()) {
            if (band instanceof JRDesignBand) {
                elements.addAll(Arrays.asList(band.getElements()));
            }
        }
        return elements;
    }
    
    private void cutSegments(JasperDesign design, List<Segment> segments) {
        Collection<JRElement> elements = getDesignElements(design);
        int totalShift = 0;
        for (Segment segment : segments) {
            segment.shiftLeft(totalShift);
            for (JRElement element : elements) {
                if (element.getX() >= segment.right) {   
                    element.setX(element.getX() - segment.width());
                } else if (element.getX() <= segment.left && getRightX(element) > segment.left) { 
                    element.setWidth(element.getWidth() - Math.min(segment.width(), getRightX(element) - segment.left));
                } else if (element.getX() > segment.left){
                    element.setWidth(element.getWidth() - Math.min(element.getWidth(), segment.right - element.getX()));
                    element.setX(segment.left);
                }
                assert element.getWidth() >= 0;
            }
            totalShift += segment.width(); 
        }
        removeZeroWidthElements(design);
    }

    @Deprecated
    public static File exportToXlsx(ReportGenerationData generationData) throws IOException, ClassNotFoundException, JRException {
        return exportToFile(generationData, FormPrintType.XLSX, null, null, null);
    }

    private void removeZeroWidthElements(JasperDesign design) {
        for (JRBand band : design.getAllBands()) {
            if (band instanceof JRDesignBand) {
                List<JRDesignElement> removedElements = new ArrayList<>();
                for (JRElement element : band.getElements()) {
                    if (element instanceof JRDesignElement) {
                        JRDesignElement designElement = (JRDesignElement) element;
                        if (designElement.getWidth() == 0) {
                            removedElements.add(designElement);
                        }
                    }
                }
                for (JRDesignElement element : removedElements) {
                    ((JRDesignBand) band).removeElement(element);
                }
            }
        }
    }

    private int getRightX(JRElement element) {
        return element.getX() + element.getWidth();
    }
    
    private void transformBand(JasperDesign design, JRBand band, boolean ignorePagination, Set<String> transformedStyleNames, String subreportID) {
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
                            fieldsInGroup.put(groupKey, new ArrayList<>());
                        }
                        fieldsInGroup.get(groupKey).add((JRDesignTextField) element);
                    }
                }
            }
            
            for (JRElement element : band.getElements()) {
                if (element instanceof JRDesignTextField) {
                    JRDesignTextField textField = (JRDesignTextField) element;
                    if (textField.getExpression() != null) {
                        transformTextField(design, textField, fieldsInGroup, toAdd, toDelete, transformedStyleNames, subreportID);
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

    private void transformTextFieldExpressions(JRDesignTextField oldField, JRDesignTextField newField, int i, String subreportID) {
        if (oldField.getExpression() != null) {
            JRDesignExpression subExpr = new JRDesignExpression(oldField.getExpression().getText());
            transformExpression(subExpr, i, subreportID);
            newField.setExpression(subExpr);
        }
        
        if (oldField.getPrintWhenExpression() != null && oldField.getPrintWhenExpression().getText() != null) {
            JRDesignExpression subPWExpr = new JRDesignExpression(oldField.getPrintWhenExpression().getText());
            transformExpression(subPWExpr, i, subreportID);
            newField.setPrintWhenExpression(subPWExpr);
        }

        if (oldField.getPatternExpression() != null && oldField.getPatternExpression().getText() != null) {
            JRDesignExpression subPatExpr = new JRDesignExpression(oldField.getPatternExpression().getText());
            transformExpression(subPatExpr, i, subreportID);
            newField.setPatternExpression(subPatExpr);
        }

        JRPropertyExpression[] propertyExpressions = oldField.getPropertyExpressions();
        if(propertyExpressions != null) {
            JRPropertyExpression[] newPropertyExpressions = newField.getPropertyExpressions();
            for (int j = 0; j < propertyExpressions.length; j++) {
                JRPropertyExpression propExpr = propertyExpressions[j];
                JRPropertyExpression newPropExpr = newPropertyExpressions[j];
                if (propExpr instanceof JRDesignPropertyExpression) {
                    JRDesignExpression subDesignExpr = new JRDesignExpression(((JRDesignPropertyExpression) propExpr).getValueExpression().getText());
                    transformExpression(subDesignExpr, i, subreportID);
                    ((JRDesignPropertyExpression)newPropExpr).setValueExpression(subDesignExpr);
                }
            }
        }
    }
    
    private void transformTextField(JasperDesign design, JRDesignTextField textField, Map<String, List<JRDesignTextField>> fieldsInGroup, List<JRDesignElement> toAdd, List<JRDesignElement> toDelete, Set<String> transformedStyleNames, String subreportID) {
        String fieldName = findColumnFieldName(textField.getExpression(), subreportID);

        if (fieldName != null) {
            toDelete.add(textField);
            if (hasColumns(fieldName, subreportID)) {
                int newFieldsCount = getColumnsCount(fieldName, subreportID);
                if(newFieldsCount > 0) { // optimization + otherwise where will be division by zero in makeTextFieldPartition
                    List<JRDesignTextField> subFields = makeTextFieldPartition(textField, newFieldsCount, fieldsInGroup);
                    String oldStyleName = textField.getStyle() == null ? null : textField.getStyle().getName();

                    for (int i = 0; i < newFieldsCount; i++) {
                        transformTextFieldExpressions(textField, subFields.get(i), i, subreportID);
                        if (oldStyleName != null && transformedStyleNames.contains(oldStyleName)) {
                            subFields.get(i).setStyle(design.getStylesMap().get(oldStyleName + indexSuffix(i)));
                        }
                    }
                    toAdd.addAll(subFields);
                }
            }
        }

        transformTextFieldPattern(textField);
    }
    
    private void transformTextFieldPattern(JRDesignTextField textField) {
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
            int setWidth;
            if (rightmost) {
                int rightBound = (i+1 < cnt ? baseX + subWidth : (int)boundRect.getX() + (int)boundRect.getWidth());
                setWidth = rightBound - subField.getX();
            } else {
                setWidth = xEndShift - xShift;
            }
            subField.setWidth(BaseUtils.max(setWidth,4)); // setting some minimum width, because otherwise there are some unclear errors while layouting
            res.add(subField);
            
            baseX += subWidth;
        }
        
        return res;
    }

    public static void exportAndOpen(ReportGenerationData generationData, FormPrintType type, boolean fixBoolean, RemoteLogicsInterface remoteLogics) {
        exportAndOpen(generationData, type, null, null, remoteLogics);
    }

    public static void exportAndOpen(ReportGenerationData generationData, FormPrintType type, String sheetName, String password, RemoteLogicsInterface remoteLogics) {
        try {
            File tempFile = exportToFile(generationData, type, sheetName, password, remoteLogics);

            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(tempFile);
                }
            } finally {
                tempFile.deleteOnExit();
            }
        } catch (Exception e) {
            throw new RuntimeException(ApiResourceBundle.getString("exceptions.error.exporting.to", type), e);
        }
    }

    private static JRAbstractExporter getExporter(FormPrintType printType) {
        switch (printType) {
            case XLS:
                return new JRXlsExporter();
            case XLSX:
                return new JRXlsxExporter();
            case DOC:
                return new JRDocxExporter();
            case DOCX:
                return new JRDocxExporter();
            case RTF:
                return new JRRtfExporter();
            case HTML:
                return new ReportHTMLExporter();
            default:
                return new JRPdfExporter(); // by default exporting to pdf
        }
    }

    public static File exportToFile(ReportGenerationData generationData, FormPrintType type, String sheetName, String password, RemoteLogicsInterface remoteLogics) throws ClassNotFoundException, IOException, JRException {
        String extension = type.getExtension();
        File tempFile = File.createTempFile("lsf", "." + extension);

        ReportGenerator report = new ReportGenerator(generationData);

        JasperPrint print = report.createReport(type, remoteLogics);
        print.setProperty(XlsReportConfiguration.PROPERTY_DETECT_CELL_TYPE, "true");
        if(type == FormPrintType.XLSX) {
            print.setProperty(XlsReportConfiguration.PROPERTY_MAXIMUM_ROWS_PER_SHEET, "1048576");
        }

        JRAbstractExporter exporter = getExporter(type);
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, tempFile.getAbsolutePath());
        exporter.exportReport();

        return processFile(tempFile, extension, sheetName, password);
    }

    private static File processFile(File tempFile, String extension, String sheetName, String password) throws IOException {
        if(sheetName != null || password != null) {
            switch (extension) {
                case "xls": {
                    HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(IOUtils.getFileBytes(tempFile)));
                    for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext(); ) {
                        Sheet sheet = it.next();
                        if(password != null) {
                            sheet.protectSheet(password);
                        }
                        if(sheetName != null) {
                            wb.setSheetName(wb.getSheetIndex(sheet), sheetName);
                        }
                    }
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        wb.write(fos);
                    }
                    break;
                }
                case "xlsx": {
                    XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(IOUtils.getFileBytes(tempFile)));
                    for (Iterator<Sheet> it = wb.sheetIterator(); it.hasNext(); ) {
                        Sheet sheet = it.next();
                        if(password != null) {
                            sheet.protectSheet(password);
                            //allow resize images
                            CTSheetProtection protection = ReflectionUtils.invokePrivateMethod(sheet.getClass(), sheet, "safeGetProtectionField", new Class<?>[0]);
                            if(protection != null) {
                                protection.setObjects(false);
                            }
                        }
                        if(sheetName != null) {
                            wb.setSheetName(wb.getSheetIndex(sheet), sheetName);
                        }
                    }
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        wb.write(fos);
                    }
                    break;
                }
            }
        }
        return tempFile;
    }

    public static RawFileData exportToFileByteArray(ReportGenerationData generationData, FormPrintType type, RemoteLogicsInterface remoteLogics) {
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();

        RemoteClassLoader remoteClassLoader = new RemoteClassLoader(originalClassloader);
        remoteClassLoader.setRemoteLogics(remoteLogics);

        Thread.currentThread().setContextClassLoader(remoteClassLoader);
        try {
            return exportToFileByteArray(generationData, type, null, null, remoteLogics);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }
    }
    
    public static RawFileData exportToFileByteArray(ReportGenerationData generationData, FormPrintType type, String sheetName, String password, RemoteLogicsInterface remoteLogics) {
        try {
            try {
                return new RawFileData(exportToFile(generationData, type, sheetName, password, remoteLogics));
            } finally {
                JRVirtualizationHelper.clearThreadVirtualizer();
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
