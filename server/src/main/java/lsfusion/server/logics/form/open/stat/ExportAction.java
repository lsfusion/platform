package lsfusion.server.logics.form.open.stat;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.open.ObjectSelector;
import lsfusion.server.logics.form.stat.*;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.StaticExportData;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.io.IOException;
import java.sql.SQLException;

public abstract class ExportAction<O extends ObjectSelector> extends FormStaticAction<O, FormIntegrationType> {
    private final ClassPropertyInterface selectTopInterface;
    private final ImOrderMap<GroupObjectEntity, ClassPropertyInterface> selectTopsInterfaces;

    protected String charset;
    
    public ExportAction(LocalizedString caption, FormSelector<O> form, ImList<O> objectsToSet, ImList<Boolean> nulls, ImOrderSet<PropertyInterface> orderContextInterfaces,
                        ImSet<ContextFilterSelector<PropertyInterface, O>> contextFilters, FormIntegrationType staticType, ValueClass selectTop, ImOrderMap<GroupObjectEntity, ValueClass> selectTops, String charset, ValueClass... extraParams) {
        super(caption, form, objectsToSet, nulls, orderContextInterfaces, contextFilters, staticType, selectTop, selectTops, extraParams);
        this.charset = charset;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();

        assert selectTop == null || selectTops == null;

        this.selectTopInterface = selectTop != null ? orderInterfaces.get(orderInterfaces.size() - extraParams.length) : null;

        if (selectTops != null) {
            MOrderMap<GroupObjectEntity, ClassPropertyInterface> mSelectTopInterfaces = MapFact.mOrderMap();
            for (int i = 0; i < selectTops.size(); i++) {
                mSelectTopInterfaces.add(selectTops.getKey(i), orderInterfaces.get(orderInterfaces.size() - extraParams.length + i));
            }
            this.selectTopsInterfaces = mSelectTopInterfaces.immutableOrder();
        } else {
            this.selectTopsInterfaces = null;
        }
    }
    
    protected abstract void export(ExecutionContext<ClassPropertyInterface> context, StaticExportData exportData, StaticDataGenerator.Hierarchy hierarchy) throws IOException, SQLException, SQLHandledException;

    @Override
    protected void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects, ImSet<ContextFilterInstance> contextFilters) throws SQLException, SQLHandledException {
        Integer selectTop = selectTopInterface != null ? (Integer) context.getKeyObject(selectTopInterface) : null;

        ImOrderMap<GroupObjectEntity, Integer> selectTops = MapFact.EMPTYORDER();
        if(selectTopsInterfaces != null) {
            for (int i = 0; i < selectTopsInterfaces.size(); i++) {
                selectTops = selectTops.addOrderExcl(selectTopsInterfaces.getKey(i), (Integer) context.getKeyObject(selectTopsInterfaces.getValue(i)));
            }
        }

        StaticFormDataManager formDataManager = new StaticFormDataManager(form, mapObjectValues, context, contextFilters);
        FormDataManager.ExportResult exportData = formDataManager.getExportData(new SelectTop(selectTop, selectTops));
        try {
            export(context, new StaticExportData(exportData.keys, exportData.properties), exportData.hierarchy);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
