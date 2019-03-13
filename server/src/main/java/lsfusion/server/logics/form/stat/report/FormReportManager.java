package lsfusion.server.logics.form.stat.report;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.interop.form.stat.report.FormPrintType;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.user.FormUserPreferences;
import lsfusion.interop.form.stat.report.ReportGenerationData;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.*;
import lsfusion.server.logics.form.stat.report.design.ReportDesignGenerator;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.CalcPropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
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

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.context.ThreadLocalContext.localize;

public abstract class FormReportManager extends FormDataManager {
    private static final Logger systemLogger = Logger.getLogger("SystemLogger");

    protected final FormReportInterface reportInterface; // for multiple inhertiance

    public FormReportManager(FormReportInterface reportInterface) {
        super(reportInterface);
        this.reportInterface = reportInterface;
    }

    // only for development / debug
    public List<ReportPath> getCustomReportPathList(final FormPrintType printType) throws SQLException, SQLHandledException {
        Result<String> reportPrefix = new Result<>();
        return getCustomReportPathList(getReportHierarchy(reportPrefix), printType, reportPrefix.result);
    }
    public List<ReportPath> getCustomReportPathList(StaticDataGenerator.ReportHierarchy hierarchy, final FormPrintType printType, String reportPrefix) throws SQLException, SQLHandledException {
        List<ReportPath> ret = new ArrayList<>();

        ImMap<GroupObjectHierarchy.ReportNode, String> reportsFileNames = getCustomReportFileNames(hierarchy, printType, reportPrefix);
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
        return new ReportPath(Paths.get(projDir,"src/main/lsfusion/", fileName).toString(), Paths.get(target, fileName).toString());
    }

    public List<ReportPath> saveAndGetCustomReportPathList(final FormPrintType printType, boolean recreate) throws SQLException, SQLHandledException {
        Result<String> reportPrefix = new Result<>();
        StaticDataGenerator.ReportHierarchy reportHierarchy = getReportHierarchy(reportPrefix);

        getAndSaveAutoReportDesigns(recreate, printType, reportHierarchy, reportPrefix.result);
        return getCustomReportPathList(reportHierarchy, printType, reportPrefix.result); // обновляем пути
    }

    public Map<GroupObjectHierarchy.ReportNode, JasperDesign> getAutoReportDesigns(FormPrintType printType, StaticDataGenerator.ReportHierarchy hierarchy, MAddExclMap<PropertyDrawEntity, ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>>> columnGroupObjects, MAddExclMap<PropertyReaderEntity, Type> types) throws JRException {
        ReportDesignGenerator generator = new ReportDesignGenerator(getFormEntity().getRichDesign(), hierarchy, printType, columnGroupObjects, types, reportInterface);
        return generator.generate();
    }

    public ReportGenerationData getReportData(FormPrintType printType) throws SQLException, SQLHandledException {
        return getReportData(printType, 0);
    }

    // backward compatibility
    @Deprecated
    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences preferences) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException();
    }

    public ReportGenerationData getReportData(FormPrintType printType, int selectTop) throws SQLException, SQLHandledException {
        // report hierarchy and design prefix
        Result<String> reportPrefix = new Result<>();
        StaticDataGenerator.ReportHierarchy hierarchy = getReportHierarchy(reportPrefix);

        // report sources
        FullStaticDataGenerator sourceGenerator = new FullStaticDataGenerator(reportInterface, hierarchy.hierarchy, true);
        Pair<Map<GroupObjectEntity, StaticKeyData>, StaticPropertyData<PropertyReaderEntity>> sources = sourceGenerator.generate(selectTop);
        Map<GroupObjectEntity, StaticKeyData> keyData = sources.first;
        StaticPropertyData<PropertyReaderEntity> propData = sources.second;

        // report design
        Map<GroupObjectHierarchy.ReportNode, JasperDesign> designs = getReportDesigns(printType, reportPrefix.result, hierarchy, propData.columnData, propData.types);

        // serializing
        byte[] reportHierarchyByteArray = getReportHierarchyByteArray(hierarchy.reportHierarchy);
        byte[] reportSourcesByteArray = getReportSourcesByteArray(hierarchy.reportHierarchy, keyData, propData);
        byte[] reportDesignsByteArray = getReportDesignsByteArray(designs);
        return new ReportGenerationData(reportHierarchyByteArray, reportDesignsByteArray, reportSourcesByteArray, Settings.get().isUseShowIfInReports());
    }

    protected StaticDataGenerator.ReportHierarchy getReportHierarchy(Result<String> reportPrefix) {
        reportPrefix.set(reportInterface.getReportPrefix());
        return dataInterface.getHierarchy(true).getReportHierarchy();
    }

    private byte[] getReportHierarchyByteArray(GroupObjectHierarchy.ReportHierarchy reportHierarchy) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            objOut.writeUTF(reportHierarchy.rootNode.getID());
            objOut.writeObject(reportHierarchy.getReportHierarchyMap());
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getReportDesignsByteArray(Map<GroupObjectHierarchy.ReportNode, JasperDesign> design) {
        Map<String, JasperDesign> designMap = new HashMap<>();
        for(Map.Entry<GroupObjectHierarchy.ReportNode, JasperDesign> entry : design.entrySet())
            designMap.put(entry.getKey().getID(), entry.getValue());
        
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(outStream);
            objOut.writeObject(designMap);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final static String reportsDir = ""; // "reports/" 
    
    private String findCustomReportFileName(String fileName) {

        Collection<String> result = reportInterface.getBL().getAllCustomReports();
        
        for(String entry : result){
            if(entry.endsWith("/" + fileName))
                return entry; //"/" + reportsDir + entry.split(reportsDir)[1]; // отрезаем путь reports/custom и далее
        }
        
        return null; // не нашли "/" + reportsDir + filePath;
    }

    private String getReportFileName(GroupObjectHierarchy.ReportNode reportNode, String reportPrefix) {
        return reportNode.getFileName(reportPrefix + getFormEntity().getSID().replace('.', '_'));
    }

    private Map<GroupObjectHierarchy.ReportNode, JasperDesign> getReportDesigns(FormPrintType printType, String reportPrefix, StaticDataGenerator.ReportHierarchy hierarchy, MAddExclMap<PropertyDrawEntity, ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>>> columnGroupObjects, MAddExclMap<PropertyReaderEntity, Type> types) throws SQLException, SQLHandledException {
        Map<GroupObjectHierarchy.ReportNode, JasperDesign> customDesigns = getCustomReportDesigns(hierarchy, printType, reportPrefix);
        if (customDesigns != null) {
            return customDesigns;
        }

        try {
            return getAutoReportDesigns(printType, hierarchy, columnGroupObjects, types);
        } catch (JRException e) {
            throw new RuntimeException(localize("{form.instance.error.creating.design}"), e);
        }
    }

    private Map<GroupObjectHierarchy.ReportNode, JasperDesign> getAndSaveAutoReportDesigns(boolean recreateCustom, FormPrintType printType, StaticDataGenerator.ReportHierarchy hierarchy, String reportPrefix) {
        try {
            Map<GroupObjectHierarchy.ReportNode, JasperDesign> designs = getAutoReportDesigns(printType, hierarchy, null, null);
            saveAutoReportDesigns(designs, hierarchy, recreateCustom, printType, reportPrefix);
            return designs;
        } catch (JRException | IOException | SQLException | SQLHandledException e) {
            throw new RuntimeException(localize("{form.instance.error.creating.design}"), e);
        }
    }

    // актуально только для разработки
    private void saveAutoReportDesigns(Map<GroupObjectHierarchy.ReportNode, JasperDesign> designs, StaticDataGenerator.ReportHierarchy hierarchy, boolean recreateCustom, FormPrintType printType, String reportPrefix) throws JRException, IOException, SQLException, SQLHandledException {

        ImMap<GroupObjectHierarchy.ReportNode, String> customReportFileNames = null;
        if(recreateCustom) {
            customReportFileNames = getCustomReportFileNames(hierarchy, printType, reportPrefix);
        }
            
        for (Map.Entry<GroupObjectHierarchy.ReportNode, JasperDesign> entry : designs.entrySet()) {
            GroupObjectHierarchy.ReportNode node = entry.getKey();
            ReportPath defaultCustomReportPath = recreateCustom ? getCustomReportPath(customReportFileNames.get(node)) : 
                                                                    getDefaultCustomReportPath("/" + reportsDir + getReportFileName(node, printType.getFormatPrefix() + reportPrefix));
            String reportName = defaultCustomReportPath.customPath;
            
            new File(reportName).getParentFile().mkdirs();
            JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
            
            if(!recreateCustom) // нужно скопировать в target чтобы его подцепил последующий getCustomReportPath
                Files.copy(Paths.get(reportName), Paths.get(defaultCustomReportPath.targetPath));                        
        }
    }

    private String getCustomReportPropFileName(CalcPropertyObjectEntity reportPathProp, String reportPrefix) throws SQLException, SQLHandledException {
        if (reportPathProp != null) {
            String reportPath = (String) reportInterface.read(reportPathProp);
            if (reportPath != null) {
                return findCustomReportFileName(reportPrefix + reportPath.trim());
            }
        }
        return null;
    }
    
    private ImMap<GroupObjectHierarchy.ReportNode, String> getCustomReportFileNames(StaticDataGenerator.ReportHierarchy hierarchy, FormPrintType printType, String reportPrefix) throws SQLException, SQLHandledException {
        ImSet<GroupObjectHierarchy.ReportNode> reportNodes = hierarchy.reportHierarchy.getAllNodes();
        ImValueMap<GroupObjectHierarchy.ReportNode, String> mResult = reportNodes.mapItValues();
        for (int i=0,size=reportNodes.size();i<size;i++) {
            GroupObjectHierarchy.ReportNode reportNode = reportNodes.get(i);
            String fileName = null;
            String formatPrefix = printType.getFormatPrefix();
            if(!formatPrefix.isEmpty()) // optimization
                fileName = getCustomReportFileName(reportNode, formatPrefix + reportPrefix);
            if (fileName == null)
                fileName = getCustomReportFileName(reportNode, reportPrefix);
            mResult.mapValue(i, fileName);
        }
        return mResult.immutableValue();
    }

    private String getCustomReportFileName(GroupObjectHierarchy.ReportNode reportNode, String reportPrefix) throws SQLException, SQLHandledException {
        String resourceName = getCustomReportPropFileName(reportNode.getReportPathProp(getFormEntity()), reportPrefix);
        if (resourceName == null) {
            resourceName = findCustomReportFileName(getReportFileName(reportNode, reportPrefix));
        }
        return resourceName;
    }

    private Map<GroupObjectHierarchy.ReportNode, JasperDesign> getCustomReportDesigns(StaticDataGenerator.ReportHierarchy hierarchy, FormPrintType printType, String reportPrefix) throws SQLException, SQLHandledException {
        try {
            Map<GroupObjectHierarchy.ReportNode, JasperDesign> designs = new HashMap<>();
            ImMap<GroupObjectHierarchy.ReportNode, String> fileNames = getCustomReportFileNames(hierarchy, printType, reportPrefix);
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

    private byte[] getReportSourcesByteArray(GroupObjectHierarchy.ReportHierarchy groupReportHierarchy, Map<GroupObjectEntity, StaticKeyData> keySources, StaticPropertyData<PropertyReaderEntity> propertySources) throws SQLException, SQLHandledException {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            // serializing keys
            ImSet<GroupObjectHierarchy.ReportNode> allNodes = groupReportHierarchy.getAllNodes();
            dataStream.writeInt(allNodes.size());
            for(GroupObjectHierarchy.ReportNode reportNode : allNodes) {
                dataStream.writeUTF(reportNode.getID());
                serializeKeys(dataStream, keySources.get(reportNode.getLastGroup()));
            }
            // serializing properties            
            serializeProperties(dataStream, propertySources);

            return outStream.toByteArray();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
    
    private void serializeKeys(DataOutputStream outStream, StaticKeyData keyData) throws IOException {

        ImOrderSet<ImMap<ObjectEntity, Object>> data = keyData.data;
        ImOrderSet<ObjectEntity> keys = keyData.objects;

        outStream.writeInt(keys.size());
        for(ObjectEntity object : keys) {
            outStream.writeUTF(object.getSID());
            outStream.writeInt(object.getID());
        }

        outStream.writeInt(data.size());
        for (int i = 0; i < data.size(); i++)
            serializeObjectValues(outStream, keys, data.get(i));
    }

    private void serializeProperties(DataOutputStream outStream, StaticPropertyData<PropertyReaderEntity> propData) throws IOException {

        // serializing property info + values
        outStream.writeInt(propData.data.size());
        for(int i=0,size=propData.data.size();i<size;i++) {
            PropertyReaderEntity propertyData = propData.data.getKey(i);

            // serializing property info
            outStream.writeUTF(propertyData.getReportSID());

            // serializing values
            ImOrderSet<ObjectEntity> objects = serializeObjects(outStream, propData.objects.get(propertyData));

            ImMap<ImMap<ObjectEntity, Object>, Object> values = propData.data.getValue(i);
            outStream.writeInt(values.size());
            for(int j=0,sizeJ=values.size();j<sizeJ;j++) {
                serializeObjectValues(outStream, objects, values.getKey(j));
                BaseUtils.serializeObject(outStream, values.getValue(j));
            }
        }

        // serializing property draws
        outStream.writeInt(propData.columnData.size());
        for(int i=0,size=propData.columnData.size();i<size;i++) {
            PropertyDrawEntity propertyData = propData.columnData.getKey(i);

            outStream.writeUTF(propertyData.getSID());

            Pair<ImSet<ObjectEntity>, ImSet<ObjectEntity>> columnObjects = propData.columnObjects.get(propertyData);
            ImOrderSet<ObjectEntity> parentColumnObjects = serializeObjects(outStream, columnObjects.first);
            ImOrderSet<ObjectEntity> thisColumnObjects = serializeObjects(outStream, columnObjects.second);
            
            ImMap<ImMap<ObjectEntity, Object>, ImOrderSet<ImMap<ObjectEntity, Object>>> values = propData.columnData.getValue(i);
            outStream.writeInt(values.size());
            for(int j=0,sizeJ=values.size();j<sizeJ;j++) {
                serializeObjectValues(outStream, parentColumnObjects, values.getKey(j));

                ImOrderSet<ImMap<ObjectEntity, Object>> thisColumnRow = values.getValue(j);
                outStream.writeInt(thisColumnRow.size());
                for(ImMap<ObjectEntity, Object> row : thisColumnRow)
                    serializeObjectValues(outStream, thisColumnObjects, row);
            }
        }
    }

    private void serializeObjectValues(DataOutputStream outStream, ImOrderSet<ObjectEntity> objects, ImMap<ObjectEntity, Object> row) throws IOException {
        for (ObjectEntity object : objects)
            BaseUtils.serializeObject(outStream, row.get(object));
    }

    private ImOrderSet<ObjectEntity> serializeObjects(DataOutputStream outStream, ImSet<ObjectEntity> objectEntities) throws IOException {
        ImOrderSet<ObjectEntity> objects = objectEntities.toOrderSet();
        outStream.writeInt(objects.size());
        for(ObjectEntity object : objects)
            outStream.writeInt(object.getID());
        return objects;
    }
}
