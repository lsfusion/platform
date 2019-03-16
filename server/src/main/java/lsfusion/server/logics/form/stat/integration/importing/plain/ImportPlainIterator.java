package lsfusion.server.logics.form.stat.integration.importing.plain;

import com.google.common.base.Throwables;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.server.data.type.Type;

import java.io.IOException;
import java.text.ParseException;

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
    
    protected void finalizeInit() throws IOException {
        ImOrderSet<String> fileFields = readFields();
        boolean fieldCI = isFieldCI();

        mapping = getRequiredActualMap(fileFields, fieldTypes, fieldCI);
    }

    public static ImMap<String, String> getRequiredActualMap(ImOrderSet<String> actualFields, ImOrderMap<String, Type> requiredFieldTypes, boolean fieldCI) {
        int f = 0;
        ImOrderSet<String> keys = requiredFieldTypes.keyOrderSet();
        ImOrderValueMap<String, String> mMapping = keys.mapItOrderValues();
        for(int i = 0, size = keys.size(); i<size; i++) {
            String field = keys.get(i);
            String actualField;
            if(actualFields == null) {
                actualField = field;
            } else {
                Pair<String, Integer> actual = findActualField(field, actualFields, fieldCI);
                if (actual != null) {
                    actualField = actual.first;
                    f = actual.second;
                } else
                    actualField = actualFields.get(f);
                f++;
            }
            mMapping.mapValue(i, actualField);
        }
        return mMapping.immutableValueOrder().getMap();
    }

    protected abstract ImOrderSet<String> readFields() throws IOException;

    protected abstract boolean nextRow() throws IOException;
    
    protected abstract Object getPropValue(String name, Type type) throws lsfusion.server.logics.classes.data.ParseException, ParseException, IOException;
    
    public ImMap<String, Object> next() {
        try {
            if(!nextRow())
                return null;
            return mapping.mapValues(new GetKeyValue<Object, String, String>() {
                public Object getMapValue(String key, String value) {
                    try {
                        return getPropValue(value, fieldTypes.get(key));
                    } catch (lsfusion.server.logics.classes.data.ParseException | java.text.ParseException | IOException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    } 
    public abstract void release() throws IOException;

}