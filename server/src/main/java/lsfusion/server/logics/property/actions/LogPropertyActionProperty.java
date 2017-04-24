package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.instance.*;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.*;

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

        try (FormInstance<?> formInstance = context.createFormInstance(context.getBL().LM.getLogForm(property, null))) {
            formInstance.local = true;
            
            String caption = messageProperty == null ? null : (String) messageProperty.read(context);

            List<String> titleRow = new ArrayList<>();
            List<List<String>> data = new ArrayList<>();
            ImOrderSet<FormRow> formRows = formInstance.getFormData(30).rows;
            for (int i = 0; i < formRows.size(); i++)
                data.add(new ArrayList<String>());

            for (ObjectInstance object : formInstance.getObjects()) {
                titleRow.add(ThreadLocalContext.localize(object.getCaption()));

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

                if (!data.isEmpty()) {
                    Set<Integer> duplicateColumns = new HashSet<>();
                    for (int j = data.get(0).size() - 1; j > 0; j--)
                        duplicateColumns.add(j);
                    for (List<String> row : data) {
                        for (int j1 = row.size() - 1; j1 > 0; j1--) {
                            boolean duplicate = false;
                            for (int j2 = j1 - 1; j2 >= 0; j2--) {
                                if (j1 != j2 && row.get(j1).compareTo(row.get(j2)) == 0)
                                    duplicate = true;
                            }
                            if (!duplicate)
                                duplicateColumns.remove(j1);
                        }
                    }
                    List<Integer> duplicateColumnsList = new ArrayList(duplicateColumns);
                    Collections.sort(duplicateColumnsList, Collections.reverseOrder());
                    for (int index : duplicateColumnsList) {
                        for (List<String> row : data)
                            row.remove(index);
                        titleRow.remove(index);
                    }
                }

            }
            context.delayUserInteraction(new LogMessageClientAction(caption == null ? (property.toString() + " :") : caption, titleRow, data, !context.getSession().isNoCancelInTransaction()));
        }
    }
}
