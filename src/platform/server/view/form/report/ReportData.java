package platform.server.view.form.report;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.io.Serializable;
import java.util.*;

import platform.base.DateConverter;

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 20.02.2009
 * Time: 12:15:18
 * To change this template use File | Settings | File Templates.
 */ // считанные данные (должен быть интерфейс Serialize)
public class ReportData implements JRDataSource, Serializable {

    public List<Map<Integer,Integer>> readOrder = new ArrayList<Map<Integer, Integer>>();
    public Map<String,Integer> objectsID = new HashMap<String, Integer>();
    public Map<String,Integer> propertiesID = new HashMap<String, Integer>();
    public Map<Integer,Map<Map<Integer,Integer>,Object>> properties = new HashMap<Integer, Map<Map<Integer, Integer>, Object>>();

    void Out() {
        for(Integer Object : readOrder.get(0).keySet())
            System.out.print("obj"+Object+" ");
        for(Integer Property : properties.keySet())
            System.out.print("prop"+Property+" ");
        System.out.println();

        for(Map<Integer,Integer> Row : readOrder) {
            for(Integer Object : readOrder.get(0).keySet())
                System.out.print(Row.get(Object)+" ");
            for(Integer Property : properties.keySet())
                System.out.print(properties.get(Property).get(Row)+" ");
            System.out.println();
        }
    }

    int CurrentRow = -1;
    public boolean next() throws JRException {
        CurrentRow++;
        return CurrentRow< readOrder.size();
    }

    public Object getFieldValue(JRField jrField) throws JRException {

        String fieldName = jrField.getName();
        Object value;
        if(objectsID.containsKey(fieldName))
            value = readOrder.get(CurrentRow).get(objectsID.get(fieldName));
        else {
            Integer propertyID = propertiesID.get(fieldName);
            if (propertyID == null) throw new RuntimeException("Поле " + fieldName + " отсутствует в переданных данных");
            value = properties.get(propertiesID.get(fieldName)).get(readOrder.get(CurrentRow));
        }

        if (Date.class.getName().equals(jrField.getValueClassName()) && value != null) {
            value = DateConverter.intToDate((Integer) value);
        }

        if(value instanceof String)
            value = ((String) value).trim();

/*        if(Value==null) {

            try {
                return BaseUtils.getDefaultValue(java.lang.Class.forName(jrField.getValueClassName()));
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            } catch (InstantiationException e) {
            } catch (IllegalAccessException e) {
            } catch (ClassNotFoundException e) {
            }
        } */

        return value;
    }
}
