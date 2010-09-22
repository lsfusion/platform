package platform.fullclient.layout;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import platform.base.BaseUtils;
import platform.base.DateConverter;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientReportData implements JRDataSource {
    final List<String> objectNames = new ArrayList<String>();
    private final Map<String,Integer> objects = new HashMap<String,Integer>();
    private final Map<String,Integer> properties = new HashMap<String,Integer>();
    private final ListIterator<ClientRow> iterator;

    public ClientReportData(DataInputStream inStream) throws IOException {

        List<ClientRow> rows = new ArrayList<ClientRow>();
        if (!inStream.readBoolean()) {
            int count = inStream.readInt();
            for(int i=0;i<count;i++) {
                String name = inStream.readUTF();
                objectNames.add(name);
                objects.put(name, inStream.readInt());
            }

            count = inStream.readInt();
            for(int i=0;i<count;i++)
                properties.put(inStream.readUTF(),inStream.readInt());

            count = inStream.readInt();
            for(int i=0;i<count;i++)
                rows.add(new ClientRow(inStream));
        }
        iterator = rows.listIterator();
    }

    protected ClientRow currentRow;

    public boolean next() throws JRException {
        if(!iterator.hasNext()) return false;
        currentRow = iterator.next();
        return true;
    }

    public void revert() {
        assert iterator.hasPrevious();
        iterator.previous();
    }

    public Object getFieldValue(JRField jrField) throws JRException {

        String fieldName = jrField.getName();
        Object value;
        Integer objectID = objects.get(fieldName);
        if(objectID!=null)
            value = currentRow.keys.get(objectID);
        else {
            Integer propertyID = properties.get(fieldName);
            if (propertyID == null) throw new RuntimeException("Поле " + fieldName + " отсутствует в переданных данных");
            value = currentRow.values.get(propertyID);
        }

        if (Date.class.getName().equals(jrField.getValueClassName()) && value != null) {
            value = DateConverter.sqlToDate((java.sql.Date) value);
        }

        if(value instanceof String)
            value = ((String) value).trim();

        return value;
    }

    public Object getKeyValueByIndex(int index) {
        return currentRow.keys.get(objects.get(objectNames.get(index)));
    }

    class ClientRow {
        final Map<Integer,Object> keys = new HashMap<Integer, Object>();
        final Map<Integer,Object> values = new HashMap<Integer, Object>();

        ClientRow(DataInputStream inStream) throws IOException {
            for(int i=0;i<objects.size();i++)
                keys.put(inStream.readInt(),BaseUtils.deserializeObject(inStream));
            for(int i=0;i<properties.size();i++)
                values.put(inStream.readInt(),BaseUtils.deserializeObject(inStream));
        }
    }

}


