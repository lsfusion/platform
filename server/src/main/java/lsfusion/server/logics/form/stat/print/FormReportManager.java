package lsfusion.server.logics.form.stat.print;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.ResourceUtils;
import lsfusion.base.Result;
import lsfusion.base.classloader.ReadUsedClassLoader;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.stat.*;
import lsfusion.server.logics.form.stat.print.design.ReportDesignGenerator;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public abstract class FormReportManager extends FormDataManager {
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
        String targetDir = ResourceUtils.getFileParentDirectoryPath(fileName);
        Path projDirPath = ResourceUtils.getTargetClassesParentPath(targetDir);
        if(projDirPath == null) {
            projDirPath = ResourceUtils.getOutProductionParentPath(targetDir);
        }

        //if nor target/classes nor out/production found, then project dir = target dir
        Path srcPath = projDirPath == null ? null : Paths.get(projDirPath.toString(), "src/main/lsfusion/");
        Path customPath = srcPath == null || !Files.exists(srcPath) ? Paths.get(targetDir, fileName) : Paths.get(srcPath.toString(), fileName);
        Path targetPath = Paths.get(targetDir, fileName);
        return new ReportPath(customPath.toString(), targetPath.toString());
    }

    // only for development / debug, если нет отчета и его нужно создать
    public ReportPath getDefaultCustomReportPath(String fileName) {
        return new ReportPath(ResourceUtils.getCustomPath(SystemProperties.userDir, fileName).toString(),
                ResourceUtils.getTargetPath(SystemProperties.userDir, fileName).toString());
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
        // report sources
        ReportStaticDataGenerator sourceGenerator = new ReportStaticDataGenerator(reportInterface);
        Pair<Map<GroupObjectEntity, StaticKeyData>, StaticPropertyData<PropertyReaderEntity>> sources = sourceGenerator.generate(selectTop);
        Map<GroupObjectEntity, StaticKeyData> keyData = sources.first;
        StaticPropertyData<PropertyReaderEntity> propData = sources.second;

        // report hierarchy and design prefix
        Result<String> reportPrefix = new Result<>();
        StaticDataGenerator.ReportHierarchy hierarchy = getReportHierarchy(reportPrefix);

        // report design
        Map<GroupObjectHierarchy.ReportNode, JasperDesign> designs = getReportDesigns(printType, reportPrefix.result, hierarchy, propData.columnData, propData.types);
        Map<String, byte[]> usedClasses = getUsedClasses(designs.values());

        // serializing
        byte[] reportHierarchyByteArray = getReportHierarchyByteArray(hierarchy.reportHierarchy);
        byte[] reportSourcesByteArray = getReportSourcesByteArray(hierarchy.reportHierarchy, keyData, propData);
        byte[] reportDesignsByteArray = getReportDesignsByteArray(designs);
        byte[] classesByteArray = getClassesByteArray(usedClasses);
        return new ReportGenerationData(reportHierarchyByteArray, reportDesignsByteArray, reportSourcesByteArray, Settings.get().isUseShowIfInReports(), classesByteArray);
    }

    private Map<String, byte[]> getUsedClasses(Collection<JasperDesign> designs) {
        ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ReadUsedClassLoader(originalClassloader));
            for (JasperDesign design : designs) {
                JasperCompileManager.compileReport(design);
            }
            return ReadUsedClassLoader.getClasses();
        } catch (JRException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassloader);
        }
    }

    private byte[] getClassesByteArray(Map<String, byte[]> classes) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            new ObjectOutputStream(outStream).writeObject(classes);
            return outStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected StaticDataGenerator.ReportHierarchy getReportHierarchy(Result<String> reportPrefix) {
        reportPrefix.set(reportInterface.getReportPrefix());
        return reportInterface.getHierarchy(true).getReportHierarchy();
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
        if(fileName.startsWith("/")) {
            //absolute path
            return ResourceUtils.getResource(fileName) != null ? fileName : null;
        } else {
            //relative path
            Collection<String> result = reportInterface.getBL().getAllCustomReports();

            for(String entry : result){
                if(entry.endsWith("/" + fileName))
                    return entry; //"/" + reportsDir + entry.split(reportsDir)[1]; // отрезаем путь reports/custom и далее
            }

            return null; // не нашли "/" + reportsDir + filePath;
        }
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
        if(recreateCustom)
            customReportFileNames = getCustomReportFileNames(hierarchy, printType, reportPrefix);

        for (Map.Entry<GroupObjectHierarchy.ReportNode, JasperDesign> entry : designs.entrySet()) {
            GroupObjectHierarchy.ReportNode node = entry.getKey();
            ReportPath defaultCustomReportPath;
            if(recreateCustom) {
                String nodeFileName = customReportFileNames.get(node);
                if(nodeFileName == null) // file (not file name) is provided (do nothing)
                    continue;
                defaultCustomReportPath = getCustomReportPath(nodeFileName);
            } else
                defaultCustomReportPath = getDefaultCustomReportPath("/" + reportsDir + getReportFileName(node, printType.getFormatPrefix() + reportPrefix));
            String reportName = defaultCustomReportPath.customPath;
            
            new File(reportName).getParentFile().mkdirs();
            JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
            
            if(!recreateCustom) // нужно скопировать в target чтобы его подцепил последующий getCustomReportPath
                Files.copy(Paths.get(reportName), Paths.get(defaultCustomReportPath.targetPath));                        
        }
    }

    private ReportSource getCustomReportPropSource(PropertyObjectEntity reportPathProp, String reportPrefix) throws SQLException, SQLHandledException {
        if (reportPathProp != null) {
            Object readReport = reportInterface.read(reportPathProp);
            if(readReport instanceof FileData) {
                FileData fileReport = (FileData) readReport;
                RawFileData rawFile = fileReport.getRawFile();
                if(fileReport.getExtension().equals("path"))
                    readReport = new String(rawFile.getBytes());
                else
                    readReport = rawFile;
            }

            if(readReport instanceof String) {
                String fileName = findCustomReportFileName(reportPrefix + ((String) readReport).trim());
                if(fileName != null)
                    return new ResourceReportSource(fileName);
            } else if(readReport instanceof RawFileData) {
                return new FileReportSource(((RawFileData) readReport).getBytes());
            }
        }
        return null;
    }

    // for debug / develop purposes
    private ImMap<GroupObjectHierarchy.ReportNode, String> getCustomReportFileNames(StaticDataGenerator.ReportHierarchy hierarchy, FormPrintType printType, String reportPrefix) throws SQLException, SQLHandledException {
        ImMap<GroupObjectHierarchy.ReportNode, ReportSource> reportSources = getCustomReportSources(hierarchy, printType, reportPrefix);
        return reportSources.filterFnValues(element -> element instanceof ResourceReportSource).mapValues(reportSource -> ((ResourceReportSource)reportSource).fileName);
    }
    private ImMap<GroupObjectHierarchy.ReportNode, ReportSource> getCustomReportSources(StaticDataGenerator.ReportHierarchy hierarchy, FormPrintType printType, String reportPrefix) throws SQLException, SQLHandledException {
        ImSet<GroupObjectHierarchy.ReportNode> reportNodes = hierarchy.reportHierarchy.getAllNodes();
        ImValueMap<GroupObjectHierarchy.ReportNode, ReportSource> mResult = reportNodes.mapItValues();
        for (int i=0,size=reportNodes.size();i<size;i++) {
            GroupObjectHierarchy.ReportNode reportNode = reportNodes.get(i);
            ReportSource reportSource = null;
            String formatPrefix = printType.getFormatPrefix();
            if(!formatPrefix.isEmpty()) // optimization
                reportSource = getCustomReportFileName(reportNode, formatPrefix + reportPrefix);
            if (reportSource == null)
                reportSource = getCustomReportFileName(reportNode, reportPrefix);
            mResult.mapValue(i, reportSource);
        }
        return mResult.immutableValue();
    }

    private interface ReportSource {
        InputStream getInputStream();
    }

    private static class ResourceReportSource implements ReportSource {
        public final String fileName;

        public ResourceReportSource(String fileName) {
            this.fileName = fileName;
        }

        public InputStream getInputStream() {
            return getClass().getResourceAsStream(fileName);
        }
    }

    private static class FileReportSource implements ReportSource {
        public final byte[] file;

        public FileReportSource(byte[] file) {
            this.file = file;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(file);
        }
    }

    private ReportSource getCustomReportFileName(GroupObjectHierarchy.ReportNode reportNode, String reportPrefix) throws SQLException, SQLHandledException {
        ReportSource reportSource = getCustomReportPropSource(reportNode.getReportPathProp(getFormEntity()), reportPrefix);
        if(reportSource != null)
            return reportSource;

        String fileName = findCustomReportFileName(getReportFileName(reportNode, reportPrefix));
        if(fileName != null)
            return new ResourceReportSource(fileName);

        return null;
    }

    private Map<GroupObjectHierarchy.ReportNode, JasperDesign> getCustomReportDesigns(StaticDataGenerator.ReportHierarchy hierarchy, FormPrintType printType, String reportPrefix) throws SQLException, SQLHandledException {
        try {
            Map<GroupObjectHierarchy.ReportNode, JasperDesign> designs = new HashMap<>();
            ImMap<GroupObjectHierarchy.ReportNode, ReportSource> fileNames = getCustomReportSources(hierarchy, printType, reportPrefix);
            for(int i=0,size=fileNames.size();i<size;i++) {
                ReportSource reportSource = fileNames.getValue(i);
                if(reportSource == null) // if some design is missing we'll consider that there's no custom design at all
                    return null;
                JasperDesign subreport = JRXmlLoader.load(reportSource.getInputStream());
                designs.put(fileNames.getKey(i), subreport);
            }
            return designs;
        } catch (JRException e) {
            throw new RuntimeException(localize("{form.instance.error.loading.design}"), e);
//            systemLogger.error("Error loading custom report design: ", e);
//            return null;
        }
    }

    private byte[] getReportSourcesByteArray(GroupObjectHierarchy.ReportHierarchy groupReportHierarchy, Map<GroupObjectEntity, StaticKeyData> keySources, StaticPropertyData<PropertyReaderEntity> propertySources) {
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
            int notNullCount = 0;
            for (int j=0,sizeJ=values.size();j<sizeJ;j++)
                if (values.getValue(j) != null)
                    notNullCount++;
            outStream.writeInt(notNullCount);
            for(int j=0,sizeJ=values.size();j<sizeJ;j++)
                if (values.getValue(j) != null) {
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
