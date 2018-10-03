package lsfusion.server.logics.property.actions.integration.importing.plain;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
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
    
    private static Pair<String, Integer> findActualField(String field, ImOrderSet<String> fileFields, boolean isCI) {
        String actualField = null;
        Integer actualIndex = null;
        if(isCI) {
            String lowerField = field.toLowerCase();
            for(int i=0,size=fileFields.size();i<size;i++) {
                String fieldName = fileFields.get(i);
                if(lowerField.equals(fieldName.toLowerCase())) {
                    actualField = fieldName;
                    actualIndex = i;
                    break;
                }                    
            }
        } else {  // optimization
            if (fileFields.contains(field)) {
                actualField = field;
                actualIndex = fileFields.indexOf(field);
            }
        }
        if(actualField != null)
            return new Pair<>(actualField, actualIndex);
        return null;
    }
    
    protected boolean isFieldCI() {
        return false;
    }
    
    protected void finalizeInit() {
        ImOrderSet<String> fileFields = readFields();
        int f = 0;
        boolean fieldCI = isFieldCI();

        ImOrderSet<String> keys = fieldTypes.keyOrderSet();
        ImOrderValueMap<String, String> mMapping = keys.mapItOrderValues();
        for(int i = 0, size = keys.size(); i<size; i++) {
            String field = keys.get(i);
            String actualField;
            if(fileFields == null) { // hierarchical
                actualField = field;
            } else {
                Pair<String, Integer> actual = findActualField(field, fileFields, fieldCI);
                if (actual != null) {
                    actualField = actual.first;
                    f = actual.second;
                } else
                    actualField = fileFields.get(f);
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