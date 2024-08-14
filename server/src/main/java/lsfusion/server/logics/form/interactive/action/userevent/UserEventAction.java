package lsfusion.server.logics.form.interactive.action.userevent;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class UserEventAction extends SystemExplicitAction {
    public static final String PROPERTY_KEY = "property";
    
    protected final GroupObjectEntity groupObject;

    private final ClassPropertyInterface fromInterface;

    public UserEventAction(GroupObjectEntity groupObject, ValueClass... valueClasses) {
        super(valueClasses);
        this.groupObject = groupObject;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();
        this.fromInterface = orderInterfaces.get(0);
    }
    
    protected List<JSONObject> readJSON(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        Object json = context.getKeyObject(fromInterface);
        if (json instanceof String) {
            json = JSONReader.readObject((String) json);
        } 
        if (json instanceof JSONObject) {
            return Collections.singletonList((JSONObject) json);
        } else if (json instanceof JSONArray) {
            List<JSONObject> list = new ArrayList<>();
            for (Object object : (JSONArray) json) {
                if (object instanceof JSONObject) {
                    list.add((JSONObject) object);
                }
            }
            return list;
        }
        return null;
    }
}
