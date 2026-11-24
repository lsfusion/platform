package lsfusion.server.data.sql.exception;

import lsfusion.base.BaseUtils;
import lsfusion.server.base.controller.stack.ExecutionStackAspect;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.event.ApplySingleEvent;
import lsfusion.server.logics.property.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLTooLongQueryException extends SQLHandledException {

    private final int length;
    private final String select;
    private final List<List<Property>> properties;

    public SQLTooLongQueryException(int length, String select) {
        this.length = length;
        this.select = select;
        this.properties = getStackProperties();
    }

    private static List<List<Property>> getStackProperties() {
        Set<Property> named = new HashSet<>();
        List<List<Property>> result = new ArrayList<>();
        for(Object arg : ExecutionStackAspect.getStackArgs()) {
            if(arg instanceof ApplySingleEvent)
                arg = ((ApplySingleEvent) arg).getProperty();
            List<Property> group = new ArrayList<>();
            for(Object element : arg instanceof Iterable ? (Iterable<?>) arg : Collections.singleton(arg))
                if(element instanceof Property && named.add((Property) element))
                    group.add((Property) element);
            if(!group.isEmpty())
                result.add(group);
        }
        return result;
    }

    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) {
        return false;
    }

    @Override
    public String getMessage() {
        String message = "TOO LONG QUERY ROWS, LENGTH : " + length;
        if(!properties.isEmpty()) {
            List<Property> outer = properties.get(0);
            message += (outer.size() > 1 ? ", PROPERTIES : " : ", PROPERTY : ") + BaseUtils.toString(outer, "; ");
            if(properties.size() > 1) {
                List<Property> reading = new ArrayList<>();
                for(List<Property> group : properties.subList(1, properties.size()))
                    reading.addAll(group);
                message += ", READING : " + BaseUtils.toString(reading, "; ");
            }
        }
        return message + ", QUERY : " + select;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return "ln";
    }
}
