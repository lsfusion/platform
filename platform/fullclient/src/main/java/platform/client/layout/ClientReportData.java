package platform.client.layout;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.EmptyIterator;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientReportData implements JRDataSource {

    private final Map<String,Integer> objects = new HashMap<String,Integer>();
    private final Map<String,Integer> properties = new HashMap<String,Integer>();
    private Iterator<ClientRow> iterator;

    public ClientReportData(DataInputStream inStream) throws IOException {

        if(inStream.readBoolean()) {
            iterator = new EmptyIterator<ClientRow>();
            return;
        }

        int count = inStream.readInt();
        for(int i=0;i<count;i++)
            objects.put(inStream.readUTF(),inStream.readInt());

        count = inStream.readInt();
        for(int i=0;i<count;i++)
            properties.put(inStream.readUTF(),inStream.readInt());

        Collection<ClientRow> rows = new ArrayList<ClientRow>();
        count = inStream.readInt();
        for(int i=0;i<count;i++)
            rows.add(new ClientRow(inStream));
        iterator = rows.iterator();
    }

    private ClientRow currentRow;
    public boolean next() throws JRException {
        if(!iterator.hasNext()) return false;
        currentRow = iterator.next();
        return true;
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
            value = DateConverter.intToDate((java.sql.Date) value);
        }

        if(value instanceof String)
            value = ((String) value).trim();

        return value;
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


