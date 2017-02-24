package lsfusion.server.logics.property.actions;

import jasperapi.ReportGenerator;
import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.interop.FormExportType;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.FormStaticType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.server.ServerLoggers;
import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.exporting.HierarchicalFormExporter;
import lsfusion.server.logics.property.actions.exporting.PlainFormExporter;
import lsfusion.server.logics.property.actions.exporting.csv.CSVFormExporter;
import lsfusion.server.logics.property.actions.exporting.dbf.DBFFormExporter;
import lsfusion.server.logics.property.actions.exporting.json.JSONFormExporter;
import lsfusion.server.logics.property.actions.exporting.xml.XMLFormExporter;
import lsfusion.server.remote.FormReportManager;
import lsfusion.server.remote.InteractiveFormReportManager;
import lsfusion.server.remote.StaticFormReportManager;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public abstract class FormActionProperty extends SystemExplicitActionProperty {

    public final FormEntity<?> form;
    public final ImRevMap<ObjectEntity, ClassPropertyInterface> mapObjects;

    private static ValueClass[] getValueClasses(ObjectEntity[] objects, Property... extraProps) {
        int extraPropInterfaces = 0;
        for(Property extraProp : extraProps)
            if(extraProp != null)
                extraPropInterfaces += extraProp.interfaces.size();
            
        ValueClass[] valueClasses = new ValueClass[objects.length
                + extraPropInterfaces];
        for (int i = 0; i < objects.length; i++) {
            valueClasses[i] = objects[i].baseClass;
        }

        for(Property extraProp : extraProps) 
            if(extraProp != null) {
                ImMap<PropertyInterface, ValueClass> interfaceClasses = extraProp.getInterfaceClasses(ClassType.formPolicy);
                ImOrderSet<PropertyInterface> propInterfaces = extraProp.getFriendlyPropertyOrderInterfaces();
                for (int i = 0; i < propInterfaces.size(); ++i) {
                    valueClasses[objects.length + i] = interfaceClasses.get(propInterfaces.get(i));
                }
            }
        return valueClasses;
    }

    @Override
    protected boolean allowNulls() { // temporary
        return allowNullValue;
    }

    //assert objects из form
    //assert getProperties одинаковой длины
    //getProperties привязаны к форме, содержащей свойство...
    public FormActionProperty(LocalizedString caption,
                              FormEntity form,
                              final ObjectEntity[] objectsToSet,
                              boolean allowNulls, Property... extraProps) {
        super(caption, getValueClasses(objectsToSet, extraProps));
        
        this.allowNullValue = false; //allowNulls;

        mapObjects = getOrderInterfaces()
                .subOrder(0, objectsToSet.length)
                .mapOrderRevKeys(new GetIndex<ObjectEntity>() { // такой же дебилизм и в SessionDataProperty
                    public ObjectEntity getMapValue(int i) {
                        return objectsToSet[i];
                    }
                });
        this.form = form;
    }

    protected abstract void executeCustom(ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException;

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        executeCustom(mapObjects.join(context.getKeys()), context);
    }
}
