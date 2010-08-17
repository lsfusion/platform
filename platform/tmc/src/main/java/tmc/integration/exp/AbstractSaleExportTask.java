package tmc.integration.exp;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import platform.server.auth.PolicyManager;
import platform.server.form.instance.*;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.session.DataSession;
import platform.server.logics.DataObject;
import platform.server.logics.scheduler.FlagSemaphoreTask;
import platform.base.BaseUtils;
import platform.base.DateConverter;

import java.util.Map;
import java.util.Calendar;
import java.util.HashMap;
import java.text.DecimalFormatSymbols;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.sql.SQLException;

import tmc.VEDBusinessLogics;

public abstract class AbstractSaleExportTask extends FlagSemaphoreTask {

    VEDBusinessLogics BL;
    String path;

    protected AbstractSaleExportTask(VEDBusinessLogics BL, String path) {
        this.BL = BL;
        this.path = path;
    }

    protected abstract String getDbfName();
    protected abstract void setRemoteFormFilter(FormInstance formInstance) throws ParseException;
    protected abstract void updateRemoteFormProperties(FormInstance formInstance) throws SQLException;

    CharField barField;
    CharField nameField;
    NumField cenField;
    NumField kolField;
    CharField dokField;
    DateField dateField;
    CharField timeField;
    CharField nkassField;
    CharField nposField;
    CharField sklField;
    CharField platField;
    NumField summField;
    CharField discField;
    NumField percentField;

    DBF outDbf;
    private void createDBF() throws Exception {

        outDbf = new DBF(path + "\\" + getDbfName(), true, "Cp866");

        barField = new CharField("bar", 12);
        outDbf.addField(barField);

        nameField = new CharField("name", 40);
        outDbf.addField(nameField);

        cenField = new NumField("cen", 17, 2);
        outDbf.addField(cenField);

        kolField = new NumField("kol", 19, 3);
        outDbf.addField(kolField);

        dokField = new CharField("dok", 8);
        outDbf.addField(dokField);

        dateField = new DateField("date");
        outDbf.addField(dateField);

        timeField = new CharField("time", 10);
        outDbf.addField(timeField);

        nkassField = new CharField("nkass", 2);
        outDbf.addField(nkassField);

        nposField = new CharField("npos", 2);
        outDbf.addField(nposField);

        sklField = new CharField("skl", 4);
        outDbf.addField(sklField);

        platField = new CharField("plat", 1);
        outDbf.addField(platField);

        summField = new NumField("summ", 17, 2);
        outDbf.addField(summField);

        discField = new CharField("disc", 10);
        outDbf.addField(discField);

        percentField = new NumField("percent", 16, 2);
        outDbf.addField(percentField);
    }

    private FormData getDataSale(DataSession session, Map<Field, PropertyDrawInstance> map) throws Exception {

        // Выгружаем продажи по кассе
        FormInstance formInstance = new FormInstance(BL.commitSaleBrowseForm, BL, session, PolicyManager.defaultSecurityPolicy, null, null, new DataObject(BL.getServerComputer(), BL.computer)); // здесь надо переделать на нормальный компьютер

        setRemoteFormFilter(formInstance);

        PropertyDrawInstance quantity = formInstance.getPropertyDraw(BL.articleQuantity);
        quantity.toDraw.addTempFilter(new NotNullFilterInstance(quantity.propertyObject));

        ObjectInstance doc = formInstance.instanceFactory.getInstance(BL.commitSaleBrowseForm.objDoc);
        ObjectInstance art = formInstance.instanceFactory.getInstance(BL.commitSaleBrowseForm.objArt);

        map.put(barField, formInstance.getPropertyDraw(BL.barcode));
        map.put(nameField, formInstance.getPropertyDraw(BL.name));
        map.put(cenField, formInstance.getPropertyDraw(BL.orderSalePrice));
        map.put(kolField, formInstance.getPropertyDraw(BL.articleQuantity));
        map.put(dateField, formInstance.getPropertyDraw(BL.date));
        map.put(summField, formInstance.getPropertyDraw(BL.orderArticleSaleSumCoeff));
        map.put(percentField, formInstance.getPropertyDraw(BL.orderArticleSaleDiscount));

        FormData data = formInstance.getFormData(BaseUtils.toSetElements(doc.groupTo, art.groupTo), BaseUtils.toSetElements(doc.groupTo, art.groupTo));

        updateRemoteFormProperties(formInstance);

        return data;
    }

    private FormData getDataCert(DataSession session, Map<Field, PropertyDrawInstance> map) throws Exception {

        // Выгружаем продажи по кассе
        FormInstance formInstance = new FormInstance(BL.saleCheckCertBrowseForm, BL, session, PolicyManager.defaultSecurityPolicy, null, null, new DataObject(BL.getServerComputer(), BL.computer)); // здесь надо переделать на нормальный компьютер

        setRemoteFormFilter(formInstance);

        PropertyDrawInstance issued = formInstance.getPropertyDraw(BL.issueObligation);
        issued.toDraw.addTempFilter(new NotNullFilterInstance(issued.propertyObject));

        ObjectInstance doc = formInstance.instanceFactory.getInstance(BL.saleCheckCertBrowseForm.objDoc);
        ObjectInstance obligation = formInstance.instanceFactory.getInstance(BL.saleCheckCertBrowseForm.objObligation);

        map.put(barField, formInstance.getPropertyDraw(BL.barcode));
        map.put(nameField, formInstance.getPropertyDraw(BL.name));
        map.put(cenField, formInstance.getPropertyDraw(BL.obligationSum));
        map.put(dateField, formInstance.getPropertyDraw(BL.date));
        map.put(summField, formInstance.getPropertyDraw(BL.obligationSum));

        FormData data = formInstance.getFormData(BaseUtils.toSetElements(doc.groupTo, obligation.groupTo), BaseUtils.toSetElements(doc.groupTo, obligation.groupTo));

        updateRemoteFormProperties(formInstance);

        return data;
    }

    private FormData getDataReturn(DataSession session, Map<Field, PropertyDrawInstance> map) throws Exception {

        // Выгружаем продажи по кассе
        FormInstance formInstance = new FormInstance(BL.returnSaleCheckRetailBrowse, BL, session, PolicyManager.defaultSecurityPolicy, null, null, new DataObject(BL.getServerComputer(), BL.computer)); // здесь надо переделать на нормальный компьютер

        setRemoteFormFilter(formInstance);

        PropertyDrawInstance returnQuantity = formInstance.getPropertyDraw(BL.returnInnerQuantity);
        returnQuantity.toDraw.addTempFilter(new NotNullFilterInstance(returnQuantity.propertyObject));

        ObjectInstance doc = formInstance.instanceFactory.getInstance(BL.returnSaleCheckRetailBrowse.objDoc);
        ObjectInstance inner = formInstance.instanceFactory.getInstance(BL.returnSaleCheckRetailBrowse.objInner);
        ObjectInstance article = formInstance.instanceFactory.getInstance(BL.returnSaleCheckRetailBrowse.objArt);

        map.put(barField, formInstance.getPropertyDraw(BL.barcode));
        map.put(nameField, formInstance.getPropertyDraw(BL.name));
        map.put(cenField, formInstance.getPropertyDraw(BL.orderSalePrice));
        map.put(kolField, formInstance.getPropertyDraw(BL.returnInnerQuantity));
        map.put(dateField, formInstance.getPropertyDraw(BL.date));
        map.put(summField, formInstance.getPropertyDraw(BL.returnArticleSalePay));
        map.put(percentField, formInstance.getPropertyDraw(BL.orderArticleSaleDiscount));

        FormData result = formInstance.getFormData(BaseUtils.toSetElements(doc.groupTo, inner.groupTo, article.groupTo), BaseUtils.toSetElements(doc.groupTo, inner.groupTo, article.groupTo));

        updateRemoteFormProperties(formInstance);

        return result;
    }

    private void writeToDbf(FormData data, Map<Field, PropertyDrawInstance> map, boolean reverse) throws Exception {

        Calendar calendar = Calendar.getInstance();

        for (FormRow row : data.rows) {

            barField.put(((String)row.values.get(map.get(barField))).trim());
            nameField.put(((String)row.values.get(map.get(nameField))).trim());
            putDouble(cenField, (Double)row.values.get(map.get(cenField)));

            if (map.get(kolField) != null)
                putDouble(kolField, (Double)row.values.get(map.get(kolField)) * (reverse ? -1 : 1));
            else
                putDouble(kolField, 1.0);

            calendar.setTime(DateConverter.sqlToDate((java.sql.Date)row.values.get(map.get(dateField))));
            dateField.put(calendar);

            putDouble(summField, (Double)row.values.get(map.get(summField)) * (reverse ? -1 : 1));

            if (map.get(percentField) != null) {
                Object percentValue = row.values.get(map.get(percentField));
                percentField.put(percentValue != null ? percentValue.toString() : "");
            }

            outDbf.write();
        }
    }

    private void putDouble(NumField fld, Double val) throws xBaseJException {
        fld.put(valueOfField(fld, val).getBytes());
    }

    // метод нужен лишь потому, что xBaseJ не умеет нормально писать в dbf-файлы Double
    private String valueOfField(NumField fld, Double val) {

        int intlen = fld.getLength() - fld.getDecimalPositionCount() - 1;
        int declen = fld.getDecimalPositionCount();

        String pattern = "";
        for (int i = 0; i < intlen-1; i++) pattern += "#";
        pattern += "0.";
        for (int i = 0; i < declen; i++) pattern += "0";

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        return format.format(val);
    }

    protected void run() throws Exception {

        try {

            createDBF();

            DataSession session = BL.createSession();

            Map<Field, PropertyDrawInstance> mapSale = new HashMap<Field, PropertyDrawInstance>();
            FormData dataSale = getDataSale(session, mapSale);
            writeToDbf(dataSale, mapSale, false);

            Map<Field, PropertyDrawInstance> mapCert = new HashMap<Field, PropertyDrawInstance>();
            FormData dataCert = getDataCert(session, mapCert);
            writeToDbf(dataCert, mapCert, false);

            Map<Field, PropertyDrawInstance> mapReturn = new HashMap<Field, PropertyDrawInstance>();
            FormData dataReturn = getDataReturn(session, mapReturn);
            writeToDbf(dataReturn, mapReturn, true);

            session.apply(BL);

        } finally {
            if (outDbf != null)
                outDbf.close();
        }

    }

}
