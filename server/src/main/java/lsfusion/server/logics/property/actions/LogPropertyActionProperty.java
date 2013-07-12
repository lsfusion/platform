package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyFormEntity;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogPropertyActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {

    private final CalcProperty<P> property;
    private final AbstractGroup recognizeGroup;

    public LogPropertyActionProperty(CalcProperty<P> property, AbstractGroup recognizeGroup) {
        super("LogProp" + property.getSID(), property.caption, new ValueClass[]{});

        this.property = property;
        this.recognizeGroup = recognizeGroup;
    }

    @Override
    protected boolean isVolatile() { // нужно recognizeGroup читать
        return true;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        DataSession session = context.getSession();
        List<List<String>> data = new ArrayList<List<String>>();
        FormInstance formInstance = context.createFormInstance(new PropertyFormEntity(property, recognizeGroup),
                MapFact.<ObjectEntity, DataObject>EMPTY(), session, false, FormSessionScope.OLDSESSION, false, false, false, null);

        List<String> titleRow = new ArrayList<String>();
        for (FormRow formRow : formInstance.getFormData(30).rows) {
            ImMap<ImSet<ObjectInstance>, ImSet<PropertyDrawInstance>> groupRows = formRow.values.keys().group(new BaseUtils.Group<ImSet<ObjectInstance>, PropertyDrawInstance>() {
                public ImSet<ObjectInstance> group(PropertyDrawInstance property) { // группируем по объектам
                    return property.propertyObject.mapping.values().toSet();
                }});
            List<String> propertyRow = new ArrayList<String>();
            for (int i=0,size=groupRows.size();i<size;i++) {
                String idResult = "";
                String titleResult = "";
                for (ObjectInstance objSet : groupRows.getKey(i)) {
                    String id = "id=" + String.valueOf(formRow.keys.get(objSet));
                    String caption = ((CustomObjectInstance) objSet).currentClass.getCaption();
                    idResult = (idResult.length() == 0 ? "" : idResult + ", ") + caption + ": " + id;
                    titleResult += (titleResult.length() == 0 ? "" : ", ") + caption;
                }
                propertyRow.add(idResult);
                titleRow = new ArrayList<String>();
                titleRow.add(titleResult);

                for (PropertyDrawInstance prop : groupRows.getValue(i)) {
                    propertyRow.add(BaseUtils.toCaption(formRow.values.get(prop)));
                    titleRow.add(prop.toString());
                }
                data.add(propertyRow);
            }
        }
        context.delayUserInteraction(new LogMessageClientAction(property.toString(), titleRow, data, true));

        formInstance.close();
    }
}
