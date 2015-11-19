package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogPropertyActionProperty<P extends PropertyInterface> extends SystemExplicitActionProperty {

    private final CalcProperty<P> property;
    private final CalcProperty<P> messageProperty;

    public LogPropertyActionProperty(CalcProperty<P> property, CalcProperty<P> messageProperty) {
        super(property.caption);

        this.property = property;
        this.messageProperty = messageProperty;
    }

    @Override
    protected boolean isVolatile() { // нужно recognizeGroup читать
        return true;
    }

    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try (FormInstance<?> formInstance = context.createFormInstance(context.getBL().LM.getLogForm(property),
                MapFact.<ObjectEntity, DataObject>EMPTY(), context.getSession(), false, FormSessionScope.OLDSESSION, false, false, false, null)) {
            String caption = messageProperty == null ? null : (String) messageProperty.read(context);

            List<String> titleRow = new ArrayList<>();
            List<List<String>> data = new ArrayList<>();
            ImOrderSet<FormRow> formRows = formInstance.getFormData(30).rows;
            for (int i = 0; i < formRows.size(); i++)
                data.add(new ArrayList<String>());

            for (ObjectInstance object : formInstance.getObjects()) {
                titleRow.add(object.getCaption());

                for (int j = 0; j < formRows.size(); j++)
                    data.get(j).add(String.valueOf(formRows.get(j).keys.get(object)));
            }

            for (PropertyDrawInstance property : formInstance.getCalcProperties()) {
                boolean emptyColumn = true;
                for (int j = 0; j < formRows.size(); j++) {
                    if (!BaseUtils.toCaption(formRows.get(j).values.get(property)).isEmpty()) {
                        emptyColumn = false;
                        break;
                    }
                }
                if (!emptyColumn) {
                    titleRow.add(property.toString());
                    for (int j = 0; j < formRows.size(); j++)
                        data.get(j).add(BaseUtils.toCaption(formRows.get(j).values.get(property)));
                }
            }
    /*        for (FormRow formRow : formRows) {
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
                        ConcreteCustomClass currentClass = ((CustomObjectInstance) objSet).currentClass;
                        String caption = (currentClass == null ? "" : currentClass.getCaption());
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
            }*/
            context.delayUserInteraction(new LogMessageClientAction(caption == null ? (property.toString() + " :") : caption, titleRow, data, !context.getSession().isNoCancelInTransaction()));
        }

        // todo : Раскомментить для использования форм....
        // todo: пока это не будет работать, т.к. мы не можем вызвать блокирующий requestUserInteraction, т.к. находимся в транзакции
        // todo: но вызывать delayUserInteraction тоже бессмысленно, т.к. к моменту показа формы всё состояние, которое привело к констрейнту - откатиться вместе с откатом транзакции
//        DataSession session = context.getSession();
//        PropertyFormEntity form = new PropertyFormEntity(context.getBL().LM, property, recognizeGroup);
//        FormInstance<?> formInstance = context.createFormInstance(form, MapFact.<ObjectEntity, DataObject>EMPTY(), session, false, FormSessionScope.OLDSESSION, false, false, true, null);
//        RemoteForm newRemoteForm = context.createRemoteForm(formInstance);
//        context.delayUserInteraction(new FormClientAction(form.getSID(), newRemoteForm, ModalityType.MODAL));
    }
}
