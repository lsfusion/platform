package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.LogMessageClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyFormEntity;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class LogPropertyActionProperty<P extends PropertyInterface> extends SystemActionProperty {

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
        ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
        FormInstance formInstance = context.createFormInstance(new PropertyFormEntity(property, recognizeGroup),
                new HashMap<ObjectEntity, DataObject>(), session, false, FormSessionScope.OLDSESSION, false, false);

        ArrayList<String> titleRow = new ArrayList<String>();
        for (FormRow formRow : formInstance.getFormData(30).rows) {
            ArrayList<String> propertyRow = new ArrayList<String>();
            for (Map.Entry<Set<ObjectInstance>, Collection<PropertyDrawInstance>> groupObj : BaseUtils.group(new BaseUtils.Group<Set<ObjectInstance>, PropertyDrawInstance>() {
                public Set<ObjectInstance> group(PropertyDrawInstance property) { // группируем по объектам
                    return new HashSet<ObjectInstance>(property.propertyObject.mapping.values());
                }
            }, formRow.values.keySet()).entrySet()) {
                String idResult = "";
                String titleResult = "";
                for (ObjectInstance objSet : groupObj.getKey()) {
                    String id = "id=" + String.valueOf(formRow.keys.get(objSet));
                    String caption = ((CustomObjectInstance) objSet).currentClass.getCaption();
                    idResult = (idResult.length() == 0 ? "" : idResult + ", ") + caption + ": " + id;
                    titleResult += (titleResult.length() == 0 ? "" : ", ") + caption;
                }
                propertyRow.add(idResult);
                titleRow = new ArrayList<String>();
                titleRow.add(titleResult);

                for (PropertyDrawInstance prop : groupObj.getValue()) {
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
