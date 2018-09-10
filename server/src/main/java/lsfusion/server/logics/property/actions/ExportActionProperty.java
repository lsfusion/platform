package lsfusion.server.logics.property.actions;

import lsfusion.base.SFunctionSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.FormExportType;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.exporting.HierarchicalFormExporter;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;
import lsfusion.server.logics.property.actions.exporting.csv.CSVFormExporter;
import lsfusion.server.logics.property.actions.exporting.dbf.DBFFormExporter;
import lsfusion.server.logics.property.actions.exporting.json.JSONFormExporter;
import lsfusion.server.logics.property.actions.exporting.xml.XMLFormExporter;
import lsfusion.server.logics.property.actions.importing.xml.ImportFormXMLDataActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class ExportActionProperty<O extends ObjectSelector> extends FormStaticActionProperty<O, FormExportType> {

    // csv
    private final boolean noHeader;
    private final String separator;
    private final String charset;

    //xml/json
    private Map<String, List<String>> formObjectGroups;
    private Map<String, List<String>> formPropertyGroups;

    private static Map<String, List<String>> calcObjectGroups(FormEntity formEntity) {
        Map<String, List<String>> objectGroups = new HashMap<>();
        for(GroupObjectEntity formGroup : formEntity.getGroupsIt()) {
            List<String> parentExportGroups = ExportActionProperty.getParentExportGroups(formGroup.propertyGroup);
            if(!parentExportGroups.isEmpty())
                objectGroups.put(formGroup.getSID(), parentExportGroups);
        }
        return objectGroups;
    }
    @ManualLazy
    private Map<String, List<String>> getObjectGroups() {
        if(formObjectGroups == null)
            formObjectGroups = calcObjectGroups(form.getStaticForm());
        return formObjectGroups;
    }

    private static Map<String, List<String>> calcPropertyGroups(FormEntity formEntity) {
        Map<String, List<String>> propertyGroups = new HashMap<>();
        for(PropertyDrawEntity<?> formProperty : formEntity.getPropertyDrawsIt()) {
            String shortSID = formProperty.getShortSID();

            List<String> parentExportGroups = ExportActionProperty.getParentExportGroups(formProperty.group);
            if(!parentExportGroups.isEmpty())
                propertyGroups.put(shortSID, parentExportGroups);
        }
        return propertyGroups;
    }
    @ManualLazy
    private Map<String, List<String>> getPropertyGroups() {
        if(formPropertyGroups == null)
            formPropertyGroups = calcPropertyGroups(form.getStaticForm());
        return formPropertyGroups;
    }    

    //xml
    private Set<String> attrs;
    @ManualLazy
    private Set<String> getAttrs() {
        if(attrs == null)
            attrs = ImportFormXMLDataActionProperty.calcAttrs(form.getStaticForm());
        return attrs;
    }
    
    public static List<String> getParentExportGroups(AbstractGroup group) {
        if(group == null)
            return new ArrayList<>();
        
        return group.getParentGroups().filterList(new SFunctionSet<AbstractGroup>() {
            public boolean contains(AbstractGroup element) {
                return element.system;
            }
        }).mapListValues(new GetValue<String, AbstractGroup>() {
            public String getMapValue(AbstractGroup value) {
                return null;
            }
        }).toJavaList();        
    }

    public ExportActionProperty(LocalizedString caption, FormSelector<O> form, List<O> objectsToSet, List<Boolean> nulls, FormExportType staticType, LCP exportFile, boolean noHeader, String separator, String charset) {
        super(caption, form, objectsToSet, nulls, staticType, exportFile);
        
        this.noHeader = noHeader;
        this.separator = separator;
        this.charset = charset;
    }


    @Override
    protected Map<String, byte[]> exportPlain(ReportGenerationData reportData) throws IOException {
        PlainFormExporter exporter;
        if(staticType == FormExportType.CSV) {
            exporter = new CSVFormExporter(reportData, noHeader, separator, charset);
        } else {
            assert staticType == FormExportType.DBF;
            exporter = new DBFFormExporter(reportData, charset);
        }
        return exporter.export();
    }

    @Override
    protected byte[] exportHierarchical(ExecutionContext<ClassPropertyInterface> context, ReportGenerationData reportData) throws IOException {
        HierarchicalFormExporter exporter;
        Map<String, List<String>> propertyGroups = getPropertyGroups();
        Map<String, List<String>> objectGroups = getObjectGroups();
        if (staticType == FormExportType.XML) {
            exporter = new XMLFormExporter(reportData, form.getStaticForm().getName(), objectGroups, propertyGroups, getAttrs());
        } else {
            assert staticType == FormExportType.JSON;
            exporter = new JSONFormExporter(reportData, objectGroups, propertyGroups);
        }
        return exporter.export();
    }

    @Override
    protected void exportClient(ExecutionContext<ClassPropertyInterface> context, LocalizedString caption, ReportGenerationData reportData, List<ReportPath> reportPathList, String formSID) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException();
    }
}
