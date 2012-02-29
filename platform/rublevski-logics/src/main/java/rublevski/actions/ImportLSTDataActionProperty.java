package rublevski.actions;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.integration.*;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import rublevski.RublevskiBusinessLogics;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class ImportLSTDataActionProperty extends ScriptingActionProperty {
    private ScriptingLogicsModule rublevskiLM;

    public ImportLSTDataActionProperty(RublevskiBusinessLogics BL) {
        super(BL);
        rublevskiLM = BL.getLM();

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        String path = rublevskiLM.getLPByName("importLSTDirectory").read(context).toString().trim();
        importItemGroups(path + "//_sprgrt.dbf");
        importParentGroups(path + "//_sprgrt.dbf");

        importItems(path + "//_sprgrm.dbf");
    }

    private void importParentGroups(String path) {
        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, true);

            ImportField itemGroupID = new ImportField(BL.LM.extSID);
            ImportField parentGroupID = new ImportField(BL.LM.extSID);

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupID));
            ImportKey<?> parentGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(parentGroupID));

            List<ImportProperty<?>> propsParent = new ArrayList<ImportProperty<?>>();
            propsParent.add(new ImportProperty(parentGroupID, rublevskiLM.getLPByName("parentItemGroup").getMapping(itemGroupKey),
                    BL.LM.object(rublevskiLM.getClassByName("itemGroup")).getMapping(parentGroupKey)));
            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, parentGroupID), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey, parentGroupKey), propsParent);
            service.synchronize(true, false);
            if (session.hasChanges())
                session.apply(BL);
            session.close();

        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void importItemGroups(String path) throws SQLException {

        try {
            List<List<Object>> data = importItemGroupsFromDBF(path, false);

            ImportField itemGroupID = new ImportField(BL.LM.extSID);
            ImportField itemGroupName = new ImportField(BL.LM.name);

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupID));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();
            props.add(new ImportProperty(itemGroupID, BL.LM.extSID.getMapping(itemGroupKey)));
            props.add(new ImportProperty(itemGroupName, BL.LM.name.getMapping(itemGroupKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemGroupID, itemGroupName), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemGroupKey), props);
            service.synchronize(true, false);
            if (session.hasChanges())
                session.apply(BL);
            session.close();

        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void importItems(String path) throws SQLException {

        try {
            List<List<Object>> data = importItemsFromDBF(path);

            ImportField itemIDField = new ImportField(BL.LM.extSID);
            ImportField itemGroupIDField = new ImportField(BL.LM.extSID);
            ImportField itemCaptionField = new ImportField(BL.LM.name);
            ImportField unitOfMeasureField = new ImportField(BL.LM.name);
            ImportField brandField = new ImportField(BL.LM.name);
            ImportField countryField = new ImportField(rublevskiLM.getLPByName("extSIDCountry"));

            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("item"),
                    BL.LM.extSIDToObject.getMapping(itemIDField));

            ImportKey<?> itemGroupKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("itemGroup"),
                    BL.LM.extSIDToObject.getMapping(itemGroupIDField));
            
            ImportKey<?> unitOfMeasureKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("unitOfMeasure"),
                    BL.LM.extSIDToObject.getMapping(unitOfMeasureField));

            ImportKey<?> brandKey = new ImportKey((ConcreteCustomClass) rublevskiLM.getClassByName("brand"),
                    BL.LM.extSIDToObject.getMapping(brandField));

            ImportKey<?> countryKey = new ImportKey(BL.LM.country,
                    rublevskiLM.getLPByName("extSIDToCountry").getMapping(countryField));

            List<ImportProperty<?>> props = new ArrayList<ImportProperty<?>>();

            props.add(new ImportProperty(itemGroupIDField, rublevskiLM.getLPByName("itemGroupSku").getMapping(itemKey),
                    BL.LM.object(rublevskiLM.getClassByName("itemGroup")).getMapping(itemGroupKey)));
            
            props.add(new ImportProperty(itemIDField, BL.LM.extSID.getMapping(itemKey)));
            props.add(new ImportProperty(itemCaptionField, rublevskiLM.getLPByName("captionItem").getMapping(itemKey)));

            props.add(new ImportProperty(unitOfMeasureField, BL.LM.extSID.getMapping(unitOfMeasureKey)));
            props.add(new ImportProperty(unitOfMeasureField, BL.LM.name.getMapping(unitOfMeasureKey)));
            props.add(new ImportProperty(unitOfMeasureField,rublevskiLM.getLPByName("shortName").getMapping(unitOfMeasureKey)));
            props.add(new ImportProperty(unitOfMeasureField, rublevskiLM.getLPByName("unitOfMeasureItem").getMapping(itemKey),
                    BL.LM.object(rublevskiLM.getClassByName("unitOfMeasure")).getMapping(unitOfMeasureKey)));

            props.add(new ImportProperty(brandField, BL.LM.name.getMapping(brandKey)));
            props.add(new ImportProperty(brandField, BL.LM.extSID.getMapping(brandKey)));
            props.add(new ImportProperty(brandField, rublevskiLM.getLPByName("brandItem").getMapping(itemKey),
                    BL.LM.object(rublevskiLM.getClassByName("brand")).getMapping(brandKey)));

            props.add(new ImportProperty(countryField, rublevskiLM.getLPByName("extSIDCountry").getMapping(countryKey)));
            props.add(new ImportProperty(countryField, BL.LM.name.getMapping(countryKey)));
            props.add(new ImportProperty(countryField, rublevskiLM.getLPByName("countryItem").getMapping(itemKey),
                    BL.LM.object(BL.LM.country).getMapping(countryKey)));

            ImportTable table = new ImportTable(Arrays.asList(itemIDField, itemGroupIDField, itemCaptionField, unitOfMeasureField, brandField, countryField), data);

            DataSession session = BL.createSession();
            IntegrationService service = new IntegrationService(session, table, Arrays.asList(itemKey, itemGroupKey, unitOfMeasureKey, brandKey, countryKey), props);
            service.synchronize(true, false);
            if (session.hasChanges()) {
                String result = session.apply(BL);
                if(result!=null)
                    result = result.substring(0,result.length());
            }

            session.close();

        } catch (xBaseJException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    private List<List<Object>> data;

    private List<List<Object>> importItemGroupsFromDBF(String path, Boolean parents) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < recordCount; i++) {

            importFile.read();
            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String group1 = new String(importFile.getField("GROUP1").getBytes(), "Cp1251").trim();
            String group2 = new String(importFile.getField("GROUP2").getBytes(), "Cp1251").trim();
            String group3 = new String(importFile.getField("GROUP3").getBytes(), "Cp1251").trim();

            if ((!"".equals(group1)) && (!"".equals(group2)) && (!"".equals(group3))) {
                if (!parents) {
                    //sid - name
                    addIfNotContains(Arrays.asList((Object) group3, group3));
                    addIfNotContains(Arrays.asList((Object) (group2 + "/" + group3.substring(0, 3)), group2));
                    addIfNotContains(Arrays.asList((Object) (group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3)), group1));
                    addIfNotContains(Arrays.asList((Object) k_grtov, pol_naim));
                } else {
                    //sid - parentSID
                    addIfNotContains(Arrays.asList((Object) group3, null));
                    addIfNotContains(Arrays.asList((Object) (group2 + "/" + group3.substring(0, 3)), group3));
                    addIfNotContains(Arrays.asList((Object) (group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3)), group2 + "/" + group3.substring(0, 3)));
                    addIfNotContains(Arrays.asList((Object) k_grtov, group1 + "/" + group2.substring(0, 3) + "/" + group3.substring(0, 3)));
                }
            }
        }
        return data;
    }

    private List<List<Object>> importItemsFromDBF(String path) throws IOException, xBaseJException {

        DBF importFile = new DBF(path);
        int recordCount = importFile.getRecordCount();

        data = new ArrayList<List<Object>>();

        for (int i = 0; i < 100; i++) {

            importFile.read();
            String k_grmat = new String(importFile.getField("K_GRMAT").getBytes(), "Cp1251").trim();
            String pol_naim = new String(importFile.getField("POL_NAIM").getBytes(), "Cp1251").trim();
            String k_grtov = new String(importFile.getField("K_GRTOV").getBytes(), "Cp1251").trim();
            String unitOfMeasure = new String(importFile.getField("K_IZM").getBytes(), "Cp1251").trim();
            String brand = new String(importFile.getField("BRAND").getBytes(), "Cp1251").trim();
            String country = new String(importFile.getField("MANFR").getBytes(), "Cp1251").trim();

            addIfNotContains(Arrays.asList((Object) k_grmat, k_grtov, pol_naim, unitOfMeasure, brand, country));
        }
        return data;
    }

    private void addIfNotContains(List<Object> element) {
        if (!data.contains(element))
            data.add(element);
    }
}