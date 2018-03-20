package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.form.ReportGenerationDataType;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.GroupObjectHierarchy;
import lsfusion.server.form.instance.ReportData;
import lsfusion.server.form.instance.ReportSourceGenerator;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.view.report.ReportDesignGenerator;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.base.BaseUtils.isRedundantString;
import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.context.ThreadLocalContext.localize;

public class FormReportManager<PropertyDraw extends PropertyReaderInstance, GroupObject, PropertyObject, CalcPropertyObject extends Order, Order, Obj extends Order, PropertyReaderInstance> {
    private static final Logger systemLogger = Logger.getLogger("SystemLogger");
    
    private final String formSID;
    private final FormView richDesign;

    private final FormReportInterface<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> formInterface;

    public FormReportManager(FormReportInterface<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> formInterface) {
        this.formInterface = formInterface;
        formSID = formInterface.getEntity().getSID().replace('.', '_');
        this.richDesign = formInterface.getEntity().getRichDesign();
    }

    // only for development / debug
    public List<ReportPath> getCustomReportPathList(final boolean toExcel, final Integer groupId, final FormUserPreferences userPreferences) throws SQLException, SQLHandledException {
        List<ReportPath> ret = new ArrayList<>();

        ImOrderMap<String, String> reportsFileNames = getCustomReportFileNames(toExcel, groupId);
        for (String customReportDesignName : reportsFileNames.valueIt()) {
            if(customReportDesignName != null) {
                ret.add(getCustomReportPath(customReportDesignName));
            }
        }
        return ret;
    }
    // only for development / debug
    public ReportPath getCustomReportPath(String fileName) {
        URL resource = getClass().getResource(fileName);
        
        String projDir;
        String target;
        
        String fullPath = "";
        if(resource != null) {
            try {
                fullPath = FilenameUtils.separatorsToUnix(Paths.get(resource.toURI()).toString());                
            } catch (URISyntaxException e) {                
            }
        }

        assert fullPath.substring(fullPath.length() - fileName.length(), fullPath.length()).equals(fileName);
        target = fullPath.substring(0, fullPath.length() - fileName.length());

        projDir = Paths.get(target, "../..").toString();

        return getCustomReportPath(fileName, projDir, target);
    }
    // only for development / debug, если нет отчета и его нужно создать
    public ReportPath getDefaultCustomReportPath(String fileName) {
        String projDir;
        String target;
        projDir = SystemProperties.userDir;

        Path targetPath = Paths.get(projDir, "target/classes");
        if(!Files.exists(targetPath)) // если не мавен, значит из idea
            targetPath = Paths.get(projDir, "out/production");

        target = targetPath.toString();

        return getCustomReportPath(fileName, projDir, target);
    }
    public ReportPath getCustomReportPath(String fileName, String projDir, String target) {
        return new ReportPath(Paths.get(projDir,"src/main/resources/", fileName).toString(), Paths.get(target, fileName).toString());
    }

    public List<ReportPath> saveAndGetCustomReportPathList(final boolean toExcel, final Integer groupId, final FormUserPreferences userPreferences, boolean recreate) throws SQLException, SQLHandledException {
        getAndSaveAutoReportDesigns(true, recreate, toExcel, groupId, userPreferences, null);
        return getCustomReportPathList(toExcel, groupId, userPreferences); // обновляем пути
    }

    public Map<String, JasperDesign> getAutoReportDesigns(boolean toExcel, Integer groupId, FormUserPreferences userPreferences, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) throws JRException {
        ReportDesignGenerator generator = new ReportDesignGenerator(richDesign, getReportHierarchy(groupId),
                getHiddenGroups(groupId), userPreferences, toExcel, columnGroupObjects, groupId, groupId != null ? formInterface.getFormInstance() : null);
        return generator.generate();
    }

    public ReportGenerationData getReportData(boolean toExcel) throws SQLException, SQLHandledException {
        return getReportData(null, toExcel, ReportGenerationDataType.PRINTJASPER, null, 0);
    }

    public ReportGenerationData getReportData(boolean toExcel, ReportGenerationDataType reportType, int selectTop) throws SQLException, SQLHandledException {
        return getReportData(null, toExcel, reportType, null, selectTop);
    }

    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences userPreferences) throws SQLException, SQLHandledException {
        return getReportData(groupId, toExcel, ReportGenerationDataType.PRINTJASPER, userPreferences, 0);
    }

    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, ReportGenerationDataType reportType, FormUserPreferences userPreferences, int selectTop) throws SQLException, SQLHandledException {
        GroupObjectHierarchy.ReportHierarchy groupReportHierarchy = getReportHierarchy(groupId, !reportType.isPrintJasper());
         
        GroupObjectHierarchy.ReportHierarchy fullReportHierarchy = getReportHierarchy(null, !reportType.isPrintJasper());

        byte[] reportHierarchyByteArray = getReportHierarchyByteArray(groupReportHierarchy.getReportHierarchyMap());
        Result<Map<String, LinkedHashSet<List<Object>>>> columnGroupObjects = new Result<>();
        ReportSourceGenerator<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> sourceGenerator =
                new ReportSourceGenerator<>(formInterface, groupReportHierarchy, fullReportHierarchy, getGridGroups(groupId), groupId, userPreferences);
        byte[] reportSourcesByteArray = getReportSourcesByteArray(sourceGenerator, columnGroupObjects, reportType, selectTop);
        byte[] reportDesignsByteArray = reportType.isExport() ? null :
                reportType.isPrintMessage() ? getPropertyCaptionsMapByteArray(sourceGenerator, reportType) :
                        getReportDesignsByteArray(toExcel, groupId, userPreferences, columnGroupObjects.result);

        return new ReportGenerationData(reportHierarchyByteArray, reportDesignsByteArray, reportSourcesByteArray);
    }

    public GroupObjectHierarchy.ReportHierarchy getReportHierarchy(Integer groupId) {
       return getReportHierarchy(groupId, false);
    }

    public GroupObjectHierarchy.ReportHierarchy getReportHierarchy(Integer groupId, boolean forceGroupNonJoinable) {
        if (groupId == null) {
            return getFormEntity().getReportHierarchy(forceGroupNonJoinable);
        } else {
            return getFormEntity().getSingleGroupReportHierarchy(groupId, forceGroupNonJoinable);
        }
    }

    public FormEntity getFormEntity() {
        return formInterface.getEntity();
    }

    private Set<Integer> getGridGroups(Integer groupId) {
        Set<Integer> gridGroupsId = new HashSet<>();
        for (GroupObject group : formInterface.getGroups()) {
            int groupObjectID = formInterface.getGroupID(group);
            if (formInterface.getGroupViewType(group).isGrid() && (groupId == null || groupId == groupObjectID)) {
                gridGroupsId.add(groupObjectID);
            }
        }
        return gridGroupsId;
    }

    private byte[] getReportHierarchyByteArray(Map<String, List<String>> dependencies) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            objOut.writeUTF(GroupObjectHierarchy.rootNodeName);
            objOut.writeObject(dependencies);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getReportDesignsByteArray(boolean toExcel, Integer groupId, FormUserPreferences userPreferences, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) throws SQLException, SQLHandledException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            Map<String, JasperDesign> res = getReportDesigns(toExcel, groupId, userPreferences, columnGroupObjects);
            objOut.writeObject(res);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String xlsPrefix = "xls_";

    private static final String tablePrefix = "table";
    private String getReportPrefix(boolean toExcel, Integer groupId) {
        String prefix = (toExcel ? xlsPrefix : "");
        return prefix + (groupId == null ? "" : tablePrefix + formInterface.getGroupSID(formInterface.getGroupByID(groupId)) + "_");
    }

    private String getDefaultReportSID(boolean toExcel, Integer groupId) {
        return getReportPrefix(toExcel, groupId) + formSID;
    }

    private final static String reportsDir = "reports/custom/"; 
    
    private String findCustomReportFileName(String fileName) {

        Collection<String> result = formInterface.getBL().getAllCustomReports();
        
        for(String entry : result){
            if(entry.endsWith("/" + fileName))
                return "/" + reportsDir + entry.split(reportsDir)[1]; // отрезаем путь reports/custom и далее
        }
        
        return null; // не нашли "/" + reportsDir + filePath;
    }

    private String getReportName(String goSID, boolean toExcel, Integer goId) {
        String formSID = getDefaultReportSID(toExcel, goId);
        if (goSID.equals(GroupObjectHierarchy.rootNodeName)) {
            return formSID + ".jrxml";
        } else {
            return formSID + "_" + goSID + ".jrxml";
        }
    }

    private Map<String, JasperDesign> getReportDesigns(boolean toExcel, Integer groupId, FormUserPreferences userPreferences, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) throws SQLException, SQLHandledException {
        Map<String, JasperDesign> customDesigns = getCustomReportDesigns(toExcel, groupId);
        if (customDesigns != null) {
            return customDesigns;
        }

        // сохраняем пока в reports/auto (в принципе смысла уже нет, так как есть пользовательский интерфейс)
        return getAndSaveAutoReportDesigns(false, false, toExcel, groupId, userPreferences, columnGroupObjects);
    }

    private Map<String, JasperDesign> getAndSaveAutoReportDesigns(boolean custom, boolean recreateCustom, boolean toExcel, Integer groupId, FormUserPreferences userPreferences, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) {
        try {
            Map<String, JasperDesign> designs = getAutoReportDesigns(toExcel, groupId, userPreferences, columnGroupObjects);
            saveAutoReportDesigns(designs, custom, recreateCustom, toExcel, groupId);
            return designs;
        } catch (JRException | IOException | SQLException | SQLHandledException e) {
            throw new RuntimeException(localize("{form.instance.error.creating.design}"), e);
        }
    }

    // актуально только для разработки
    private void saveAutoReportDesigns(Map<String, JasperDesign> designs, boolean custom, boolean recreateCustom, boolean toExcel, Integer groupId) throws JRException, IOException, SQLException, SQLHandledException {

        ImOrderMap<String, String> customReportFileNames = null;
        if(custom && recreateCustom) {
            customReportFileNames = getCustomReportFileNames(toExcel, groupId);
        }
            
        for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
            String id = entry.getKey();

            String reportName = (custom ? "reports/custom/" : "reports/auto/") + getReportName(id, toExcel, groupId);
            ReportPath defaultCustomReportPath = null;
            if(custom) {
                defaultCustomReportPath = recreateCustom ? getCustomReportPath(customReportFileNames.get(entry.getKey())) : getDefaultCustomReportPath(reportName);
                reportName = defaultCustomReportPath.customPath;
            }                
            
            new File(reportName).getParentFile().mkdirs();
            JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
            
            if(custom && !recreateCustom) // нужно скопировать в target чтобы его подцепил последующий getCustomReportPath
                Files.copy(Paths.get(reportName), Paths.get(defaultCustomReportPath.targetPath));                        
        }
    }

    private Set<Integer> getHiddenGroups(Integer groupId) {
        Set<Integer> hidedGroupsId = new HashSet<>();
        for (GroupObject group : formInterface.getGroups()) {
            int groupObjectID = formInterface.getGroupID(group);
            if (formInterface.getGroupViewType(group).isHidden() || groupId != null && groupId != groupObjectID) {
                hidedGroupsId.add(groupObjectID);
            }
        }
        return hidedGroupsId;
    }

    private String getCustomReportPropFileName(CalcPropertyObjectEntity reportPathProp, boolean toExcel, Integer groupId) throws SQLException, SQLHandledException {
        if (reportPathProp != null) {
            String reportPath = (String) formInterface.read(reportPathProp);
            if (reportPath != null) {
                return findCustomReportFileName(getReportPrefix(toExcel, groupId) + reportPath.trim());
            }
        }
        return null;
    }
    
    private ImOrderMap<String, String> getCustomReportFileNames(boolean toExcel, Integer groupId) throws SQLException, SQLHandledException {
        List<Pair<String, GroupObjectHierarchy.ReportNode>> reportNodes = getReportNodes(groupId);
        MOrderExclMap<String, String> mResult = MapFact.mOrderExclMap(reportNodes.size());
        for (Pair<String, GroupObjectHierarchy.ReportNode> node : reportNodes) {
            String sid = node.first;
            // Если не нашли custom design для xls, пробуем найти обычный
            String fileName = getCustomReportFileName(sid, node.second, toExcel, groupId);
            if (toExcel && fileName == null) {
                fileName = getCustomReportFileName(node.first, node.second, false, groupId);
            }
            mResult.exclAdd(sid, fileName);
        }
        return mResult.immutableOrder();
    }

    private String getCustomReportFileName(String sid, GroupObjectHierarchy.ReportNode reportNode, boolean toExcel, Integer groupId) throws SQLException, SQLHandledException {
        String resourceName = null;
        if (reportNode != null) {
            resourceName = getCustomReportPropFileName(reportNode.getGroupList().get(0).reportPathProp, toExcel, groupId);
        }
        if (resourceName == null && reportNode == null) {
            resourceName = getCustomReportPropFileName(getFormEntity().reportPathProp, toExcel, groupId);
        }
        if (resourceName == null) {
            resourceName = findCustomReportFileName(getReportName(sid, toExcel, groupId));
        }
        return resourceName;
    }

    private Map<String, JasperDesign> getCustomReportDesigns(boolean toExcel, Integer groupId) throws SQLException, SQLHandledException {
        try {
            Map<String, JasperDesign> designs = new HashMap<>();
            ImOrderMap<String, String> fileNames = getCustomReportFileNames(toExcel, groupId);
            for(int i=0,size=fileNames.size();i<size;i++) {
                String fileName = fileNames.getValue(i);
                if(fileName == null) // если какого-то не хватает считаем что custom design'а нет
                    return null;
                JasperDesign subreport = JRXmlLoader.load(getClass().getResourceAsStream(fileName));
                designs.put(fileNames.getKey(i), subreport);
            }
            return designs;
        } catch (JRException e) {
            systemLogger.error("Error loading custom report design: ", e);
            return null;
        }
    }
    
    private List<Pair<String, GroupObjectHierarchy.ReportNode>> getReportNodes(Integer groupId) {
        GroupObjectHierarchy.ReportHierarchy hierarchy = getReportHierarchy(groupId);
        List<Pair<String, GroupObjectHierarchy.ReportNode>> nodes = new ArrayList<>();
        nodes.add(new Pair<String, GroupObjectHierarchy.ReportNode>(GroupObjectHierarchy.rootNodeName, null));
        for (GroupObjectHierarchy.ReportNode node : hierarchy.getAllNodes()) {
            nodes.add(new Pair<>(node.getID(), node));
        }    
        return nodes;
    }

    private byte[] getReportSourcesByteArray(ReportSourceGenerator<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> sourceGenerator, Result<Map<String, LinkedHashSet<List<Object>>>> columnGroupObjects, ReportGenerationDataType reportType, int selectTop) throws SQLException, SQLHandledException {
        try {
            Map<String, ReportData> sources = sourceGenerator.generate(reportType, selectTop);
            ReportSourceGenerator.ColumnGroupCaptionsData<Obj> columnGroupCaptions = sourceGenerator.getColumnGroupCaptions();
            columnGroupObjects.set(columnGroupCaptions.columnData);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            dataStream.writeInt(sources.size());
            for (Map.Entry<String, ReportData> source : sources.entrySet()) {
                dataStream.writeUTF(source.getKey());
                source.getValue().serialize(dataStream, reportType, formInterface);
            }

            int columnPropertiesCount = columnGroupCaptions.propertyObjects.size();
            dataStream.writeInt(columnPropertiesCount);

            serializePropertyObjects(dataStream, columnGroupCaptions.propertyObjects);

            dataStream.writeInt(columnGroupCaptions.data.size());
            for (Map.Entry<String, Map<List<Object>, Object>> entry : columnGroupCaptions.data.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                Map<List<Object>, Object> value = entry.getValue();
                dataStream.writeInt(value.size());
                for (Map.Entry<List<Object>, Object> valueEntry : value.entrySet()) {
                    for (Object obj : valueEntry.getKey()) {
                        serializeObject(dataStream, obj);
                    }
                    serializeObject(dataStream, valueEntry.getValue());
                }
            }

            serializePropertyObjects(dataStream, columnGroupCaptions.columnObjects);

            for (Map.Entry<String, LinkedHashSet<List<Object>>> entry : columnGroupCaptions.columnData.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                LinkedHashSet<List<Object>> value = entry.getValue();
                dataStream.writeInt(value.size());
                for (List<Object> list : value) {
                    for (Object obj : list) {
                        serializeObject(dataStream, obj);
                    }
                }
            }

            return outStream.toByteArray();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private byte[] getPropertyCaptionsMapByteArray(ReportSourceGenerator<PropertyDraw, GroupObject, PropertyObject, CalcPropertyObject, Order, Obj, PropertyReaderInstance> sourceGenerator, ReportGenerationDataType reportType) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Map<String, ReportData> sources = sourceGenerator.generate(reportType);
            Map<String, Map<String, String>> propertyCaptionsMap = new HashMap<>();
            for (Map.Entry<String, ReportData> source : sources.entrySet()) {
                propertyCaptionsMap.put(source.getKey(), source.getValue().getPropertyCaptionsMap(getFormEntity().getRichDesign(), reportType, sourceGenerator.getFormInterface()));
            }
            new ObjectOutputStream(outStream).writeObject(propertyCaptionsMap);
            return outStream.toByteArray();
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }

    private void serializePropertyObjects(DataOutputStream stream, Map<String, List<Obj>> objects) throws IOException {
        for (Map.Entry<String, List<Obj>> entry : objects.entrySet()) {
            stream.writeUTF(entry.getKey());
            stream.writeInt(entry.getValue().size());
            for (Obj object : entry.getValue()) {
                stream.writeInt(formInterface.getObjectID(object));
            }
        }
    }    
}
