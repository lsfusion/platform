package lsfusion.server.logics.property.actions.integration.importing.plain;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.data.type.Type;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

public abstract class ImportPlainIterator {
    
    protected ImOrderMap<String, Type> fieldTypes; // required, order is needed only in finalizeInit to build mapping    
    protected ImMap<String, String> mapping; // required - actual

    public ImportPlainIterator(ImOrderMap<String, Type> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }
    
    protected void finalizeInit() {
        ImOrderSet<String> fileFields = readFields();
        int f = 0;

        ImOrderSet<String> keys = fieldTypes.keyOrderSet();
        ImOrderValueMap<String, String> mMapping = keys.mapItOrderValues();
        for(int i = 0, size = keys.size(); i<size; i++) {
            String field = keys.get(i);
            String actualField;
            if(fileFields == null) { // hierarchical
                actualField = field;
            } else {
                if (fileFields.contains(field)) {
                    actualField = field;
                    f = fileFields.indexOf(field);
                } else {
//                    while (keys.contains(fileFields.get(f))) // looking for next not used 
//                        f++;
                    actualField = fileFields.get(f);
                }
                f++;
            }
            mMapping.mapValue(i, actualField);
        }
        mapping = mMapping.immutableValueOrder().getMap();
    }

    protected abstract ImOrderSet<String> readFields();

    protected abstract boolean nextRow() throws IOException;
    
    protected abstract Object getPropValue(String name, Type type) throws lsfusion.server.data.type.ParseException, ParseException;
    
    public ImMap<String, Object> next() {
        try {
            if(!nextRow())
                return null;
            return mapping.mapValues(new GetKeyValue<Object, String, String>() {
                public Object getMapValue(String key, String value) {
                    try {
                        return getPropValue(value, fieldTypes.get(key));
                    } catch (lsfusion.server.data.type.ParseException | java.text.ParseException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    } 
    public abstract void release() throws IOException;

    protected boolean isDate(Type type) {
        return type instanceof DateClass || type instanceof TimeClass || type instanceof DateTimeClass;
    }

    protected Date parseDate(String value) {
        Date result = null;
        try {
            if (value != null && !value.isEmpty() && !value.replace(".", "").trim().isEmpty()) {
                result = DateUtils.parseDate(value, "dd/MM/yyyy", "dd.MM.yyyy", "dd.MM.yyyy HH:mm", "dd.MM.yyyy HH:mm:ss");
            }
        } catch (ParseException ignored) {
        }
        return result;
    }
}