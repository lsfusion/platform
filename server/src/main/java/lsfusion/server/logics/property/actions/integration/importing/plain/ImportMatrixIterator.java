package lsfusion.server.logics.property.actions.integration.importing.plain;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;

import java.io.IOException;

public abstract class ImportMatrixIterator extends ImportPlainIterator {

    private final boolean noHeader;

    public ImportMatrixIterator(final ImOrderMap<String, Type> fieldTypes, boolean noHeader) {
        super(fieldTypes);

        this.noHeader = noHeader;
    }

    private ImMap<String, Integer> fieldIndexMap;

    @Override
    protected ImOrderSet<String> readFields() throws IOException {
        if(!noHeader) {
            if(!nextRow())
                return SetFact.EMPTYORDER();

            ImOrderSet<String> fields = readHeader();
            fieldIndexMap = fields.mapOrderValues(new GetIndex<Integer>() {
                public Integer getMapValue(int i) {
                    return i;
                }
            });
            return fields;
        } else {
            ImOrderMap<String, Integer> sourceMap = nameToIndexColumnsMapping;
            fieldIndexMap = sourceMap.getMap();
            return sourceMap.keyOrderSet();
        }
    }

    private ImOrderSet<String> readHeader() {
        MOrderExclSet<String> mFields = SetFact.mOrderExclSet();
        int i = 0;
        while(true) {
            String nameValue = null;
            try {
                nameValue = (String) getPropValue(i++, nameClass);
            } catch (ParseException e) {
            }
            if(nameValue == null || nameValue.isEmpty())
                break;
            mFields.exclAdd(nameValue);
        }
        return mFields.immutableOrder();
    }

    public static final DataClass nameClass = StringClass.text;

    @Override
    protected Object getPropValue(String name, Type type) throws ParseException {
        Integer fieldIndex = fieldIndexMap.get(name);
        try {
            return getPropValue(fieldIndex, type);
        } catch (ParseException e) {
            if(!noHeader) // ignoring all exceptions
                throw ExceptionUtils.propagate(e, ParseException.class);
            return null;
        }
    }
    
    protected abstract Object getPropValue(Integer fieldIndex, Type type) throws ParseException;

    public final static ImOrderMap<String, Integer> nameToIndexColumnsMapping = ListFact.consecutiveList(256, 0).mapOrderKeys(new GetValue<String, Integer>() {
        public String getMapValue(Integer value) {
            return nameToIndex(value);
        }
    });

    private static String nameToIndex(int index) {
        String columnName = "";
        int resultLen = 1;
        final int LETTERS_CNT = 26;
        int sameLenCnt = LETTERS_CNT;
        while (sameLenCnt <= index) {
            ++resultLen;
            index -= sameLenCnt;
            sameLenCnt *= LETTERS_CNT;
        }

        for (int i = 0; i < resultLen; ++i) {
            columnName = (char)('A' + (index % LETTERS_CNT)) + columnName;
            index /= LETTERS_CNT;
        }
        return columnName;
    }
}
