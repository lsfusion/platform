package lsfusion.server.remote;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.ResourceList;
import lsfusion.base.Result;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.SystemProperties;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.GroupObjectHierarchy;
import lsfusion.server.form.instance.*;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.view.report.ReportDesignGenerator;
import lsfusion.server.logics.BusinessLogics;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.logics.ServerResourceBundle.getString;

public class FormReportManager<T extends BusinessLogics<T>, F extends FormInstance<T>> {
    private static final Logger systemLogger = Logger.getLogger("SystemLogger");
    
    private final F form;
    private final String formSID;
    private final FormView richDesign;

    public FormReportManager(F form) {
        this.form = form;
        formSID = form.entity.getSID().replace('.', '_');
        this.richDesign = form.entity.getRichDesign();
    }

    public Map<String, String> getReportPath(final boolean toExcel, final Integer groupId, final FormUserPreferences userPreferences) {
        Map<String, String> ret = new HashMap<String, String>();

        List<String> reportsFileNames = getCustomReportsFileNames(toExcel, groupId);
        if (reportsFileNames != null) {
            for (String customReportDesignName : reportsFileNames) {
                boolean foundInUserDir = new File(SystemProperties.userDir + "/src/main/resources/" + customReportDesignName).exists();
                if(foundInUserDir) {
                    ret.put(
                            SystemProperties.userDir + "/src/main/resources/" + customReportDesignName,
                            SystemProperties.userDir + "/target/classes/" + customReportDesignName
                    );
                }   else {
                    String[] classPath = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
                    for(String path : classPath) {
                        String sourcePath = path + "/../../src/main/resources/" + customReportDesignName;
                        if(new File(sourcePath).exists()) {
                            ret.put(
                                    sourcePath,
                                    path + "/" + customReportDesignName
                            );
                            break;
                        }
                    }
                }
                 
            }
        } else {
            Set<Integer> hidedGroupsId = new HashSet<Integer>();
            for (GroupObjectInstance group : form.getGroups()) {
                if (group.curClassView == ClassViewType.HIDE || groupId != null && groupId != group.getID()) {
                    hidedGroupsId.add(group.getID());
                }
            }
            try {
                ReportDesignGenerator generator = new ReportDesignGenerator(richDesign, getReportHierarchy(groupId), hidedGroupsId, userPreferences, toExcel, null, groupId);
                Map<String, JasperDesign> designs = generator.generate();
                String sid = getDefaultReportSID(toExcel, groupId);
                String reportName;
                for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
                    String id = entry.getKey();
                    reportName = getAutoReportName(id, sid);
                    new File(reportName).getParentFile().mkdirs();
                    JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
                    ret.put(
                            SystemProperties.userDir + "/" + reportName,
                            SystemProperties.userDir + "/target/classes/reports/custom/" + sid
                    );

                }

            } catch (JRException e) {
                throw new RuntimeException(getString("form.instance.error.creating.design"), e);
            }
        }
        return ret;
    }

    public ReportGenerationData getReportData() {
        return getReportData(false);
    }

    public ReportGenerationData getReportData(boolean toExcel) {
        return getReportData(null, toExcel, null);
    }

    public ReportGenerationData getReportData(Integer groupId, boolean toExcel, FormUserPreferences userPreferences) {
        GroupObjectHierarchy.ReportHierarchy groupReportHierarchy = getReportHierarchy(groupId);
        GroupObjectHierarchy.ReportHierarchy fullReportHierarchy = getReportHierarchy(null);

        byte[] reportHierarchyByteArray = getReportHierarchyByteArray(groupReportHierarchy.getReportHierarchyMap());
        Result<Map<String, LinkedHashSet<List<Object>>>> columnGroupObjects = new Result<Map<String, LinkedHashSet<List<Object>>>>();
        byte[] reportSourcesByteArray = getReportSourcesByteArray(
                new ReportSourceGenerator<T>(form, groupReportHierarchy, fullReportHierarchy, getGridGroups(groupId), groupId, userPreferences)
        , columnGroupObjects);
        byte[] reportDesignsByteArray = getReportDesignsByteArray(toExcel, groupId, userPreferences, columnGroupObjects.result);

        return new ReportGenerationData(reportHierarchyByteArray, reportDesignsByteArray, reportSourcesByteArray);
    }

    private GroupObjectHierarchy.ReportHierarchy getReportHierarchy(Integer groupId) {
        if (groupId == null) {
            return form.entity.getReportHierarchy();
        } else {
            return form.entity.getSingleGroupReportHierarchy(groupId);
        }
    }

    private Set<Integer> getGridGroups(Integer groupId) {
        Set<Integer> gridGroupsId = new HashSet<Integer>();
        for (GroupObjectInstance group : form.getGroups()) {
            if (group.curClassView == ClassViewType.GRID && (groupId == null || groupId == group.getID())) {
                gridGroupsId.add(group.getID());
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

    private byte[] getReportDesignsByteArray(boolean toExcel, Integer groupId, FormUserPreferences userPreferences, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) {
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
        return prefix + (groupId == null ? "" : tablePrefix + form.getGroupObjectInstance(groupId).getSID() + "_");
    }

    private String getDefaultReportSID(boolean toExcel, Integer groupId) {
        return getReportPrefix(toExcel, groupId) + formSID;
    }

    private String findCustomReportDesignName(String name, String sid) {

        boolean root = name.equals(GroupObjectHierarchy.rootNodeName);
        String filePath = "".equals(sid) ? name : (sid + (root ? "" : ("_" + name))) + ".jrxml";
        
        Pattern pattern = Pattern.compile(".*reports/custom/.*\\.jrxml");

        Collection<String> result = ResourceList.getResources(pattern);
        
        for(String entry : result){
            if(entry.endsWith("/" + filePath))
                return "reports/custom/" + entry.split("reports/custom/")[1];
        }
        
        return "reports/custom/" + filePath;
    }

    private String getAutoReportName(String name, String sid) {
        if (name.equals(GroupObjectHierarchy.rootNodeName)) {
            return "reports/auto/" + sid + ".jrxml";
        } else {
            return "reports/auto/" + sid + "_" + name + ".jrxml";
        }
    }

    private Map<String, JasperDesign> getReportDesigns(boolean toExcel, Integer groupId, FormUserPreferences userPreferences, Map<String, LinkedHashSet<List<Object>>> columnGroupObjects) {
        String sid = getDefaultReportSID(toExcel, groupId);
        Map<String, JasperDesign> customDesigns = getCustomReportDesigns(toExcel, groupId);
        if (customDesigns != null) {
            return customDesigns;
        }

        Set<Integer> hidedGroupsId = new HashSet<Integer>();
        for (GroupObjectInstance group : form.getGroups()) {
            if (group.curClassView == ClassViewType.HIDE || groupId != null && groupId != group.getID()) {
                hidedGroupsId.add(group.getID());
            }
        }
        try {
            ReportDesignGenerator generator = new ReportDesignGenerator(richDesign, getReportHierarchy(groupId), hidedGroupsId, userPreferences, toExcel, columnGroupObjects, groupId);
            Map<String, JasperDesign> designs = generator.generate();
            for (Map.Entry<String, JasperDesign> entry : designs.entrySet()) {
                String id = entry.getKey();
                String reportName = getAutoReportName(id, sid);

                new File(reportName).getParentFile().mkdirs();

                JRXmlWriter.writeReport(JasperCompileManager.compileReport(entry.getValue()), reportName, "UTF-8");
            }
            return designs;
        } catch (JRException e) {
            throw new RuntimeException(getString("form.instance.error.creating.design"), e);
        }
    }

    private InputStream getCustomReportInputStream(String sid, GroupObjectHierarchy.ReportNode node, boolean toExcel, Integer groupId) throws SQLException, SQLHandledException {
        InputStream iStream = null;
        if (node != null) {
            String resourceName = getReportPathPropStreamResourceName(node.getGroupList().get(0).reportPathProp, toExcel, groupId);
            if (resourceName != null) {
                iStream = getClass().getResourceAsStream(resourceName);
            }
        }
        if (iStream == null && node == null) {
            String resourceName = getReportPathPropStreamResourceName(form.entity.reportPathProp, toExcel, groupId);
            if (resourceName != null) {
                iStream = getClass().getResourceAsStream(resourceName);
            }
        }
        if (iStream == null) {
            String resourceName = "/" + findCustomReportDesignName(sid, getDefaultReportSID(toExcel, groupId));
            iStream = getClass().getResourceAsStream(resourceName);
        }
        return iStream;
    }

    private String getReportPathPropStreamResourceName(CalcPropertyObjectEntity reportPathProp, boolean toExcel, Integer groupId) throws SQLException, SQLHandledException {
        if (reportPathProp != null) {
            CalcPropertyObjectInstance propInstance = form.instanceFactory.getInstance(reportPathProp);
            String reportPath = (String) propInstance.read(form);
            if (reportPath != null) {
                return "/" + findCustomReportDesignName(getReportPrefix(toExcel, groupId) + reportPath.trim(), "");
            }
        }
        return null;
    }
    
    private List<String> getCustomReportsFileNames(boolean toExcel, Integer groupId) {
        try {
            List<String> result = new ArrayList<String>();
            for (Pair<String, GroupObjectHierarchy.ReportNode> node : getReportNodes(groupId)) {
                String resourceName = null;
                if (node.second != null) {
                    resourceName = getReportPathPropStreamResourceName(node.second.getGroupList().get(0).reportPathProp, toExcel, groupId);
                }
                if (resourceName == null && node.second == null) {
                    resourceName = getReportPathPropStreamResourceName(form.entity.reportPathProp, toExcel, groupId);
                }
                if (resourceName == null) {
                    resourceName = "/" + findCustomReportDesignName(node.first, getDefaultReportSID(toExcel, groupId));
                }
                result.add(resourceName);
            }
            return result;
        } catch (Exception e) {
            systemLogger.error("Error loading custom report design: ", e);
            return null;
        }
    }

    private Map<String, JasperDesign> getCustomReportDesigns(boolean toExcel, Integer groupId) {
        try {
            Map<String, JasperDesign> designs = new HashMap<String, JasperDesign>();
            for (Pair<String, GroupObjectHierarchy.ReportNode> node : getReportNodes(groupId)) {
                InputStream iStream = getCustomReportInputStream(node.first, node.second, toExcel, groupId);
                // Если не нашли custom design для xls, пробуем найти обычный
                if (toExcel && iStream == null) {
                    iStream = getCustomReportInputStream(node.first, node.second, false, groupId);
                }
                if (iStream == null) {
                    return null;
                }
                JasperDesign subreport = JRXmlLoader.load(iStream);
                designs.put(node.first, subreport);
            }
            return designs;
        } catch (Exception e) {
            systemLogger.error("Error loading custom report design: ", e);
            return null;
        }
    }
    
    private List<Pair<String, GroupObjectHierarchy.ReportNode>> getReportNodes(Integer groupId) {
        GroupObjectHierarchy.ReportHierarchy hierarchy = getReportHierarchy(groupId);
        List<Pair<String, GroupObjectHierarchy.ReportNode>> nodes = new ArrayList<Pair<String, GroupObjectHierarchy.ReportNode>>();
        nodes.add(new Pair<String, GroupObjectHierarchy.ReportNode>(GroupObjectHierarchy.rootNodeName, null));
        for (GroupObjectHierarchy.ReportNode node : hierarchy.getAllNodes()) {
            nodes.add(new Pair<String, GroupObjectHierarchy.ReportNode>(node.getID(), node));
        }    
        return nodes;
    }

    private byte[] getReportSourcesByteArray(ReportSourceGenerator<T> sourceGenerator, Result<Map<String, LinkedHashSet<List<Object>>>> columnGroupObjects) {
        try {
            Map<String, ReportData> sources = sourceGenerator.generate();
            ReportSourceGenerator.ColumnGroupCaptionsData columnGroupCaptions = sourceGenerator.getColumnGroupCaptions();
            columnGroupObjects.set(columnGroupCaptions.columnData);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            dataStream.writeInt(sources.size());
            for (Map.Entry<String, ReportData> source : sources.entrySet()) {
                dataStream.writeUTF(source.getKey());
                source.getValue().serialize(dataStream);
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
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }

    private void serializePropertyObjects(DataOutputStream stream, Map<String, List<ObjectInstance>> objects) throws IOException {
        for (Map.Entry<String, List<ObjectInstance>> entry : objects.entrySet()) {
            stream.writeUTF(entry.getKey());
            stream.writeInt(entry.getValue().size());
            for (ObjectInstance object : entry.getValue()) {
                stream.writeInt(object.getID());
            }
        }
    }
}
