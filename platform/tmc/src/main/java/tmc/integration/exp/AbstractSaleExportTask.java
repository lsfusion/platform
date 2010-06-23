package tmc.integration.exp;

import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import platform.server.view.form.*;
import platform.server.view.form.filter.NotNullFilter;
import platform.server.session.DataSession;
import platform.server.auth.AuthPolicy;
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
    protected abstract void setRemoteFormFilter(RemoteForm remoteForm) throws ParseException;
    protected abstract void updateRemoteFormProperties(RemoteForm remoteForm) throws SQLException;

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

    private FormData getDataSale(DataSession session, Map<Field, PropertyView> map) throws Exception {

        // Выгружаем продажи по кассе
        RemoteForm remoteForm = new RemoteForm(BL.commitSaleBrowse, BL, session, AuthPolicy.defaultSecurityPolicy, null, null, new DataObject(BL.getComputers().iterator().next(), BL.computer)); // здесь надо переделать на нормальный компьютер

        setRemoteFormFilter(remoteForm);

        PropertyView quantity = remoteForm.getPropertyView(BL.articleInnerQuantity);
        quantity.toDraw.addTempFilter(new NotNullFilter(quantity.view));

        ObjectImplement doc = remoteForm.mapper.mapObject(BL.commitSaleBrowse.objDoc);
        ObjectImplement art = remoteForm.mapper.mapObject(BL.commitSaleBrowse.objArt);

        map.put(barField, remoteForm.getPropertyView(BL.barcode));
        map.put(nameField, remoteForm.getPropertyView(BL.name));
        map.put(cenField, remoteForm.getPropertyView(BL.orderSalePrice));
        map.put(kolField, remoteForm.getPropertyView(BL.articleInnerQuantity));
        map.put(dateField, remoteForm.getPropertyView(BL.date));
        map.put(summField, remoteForm.getPropertyView(BL.orderArticleSaleSumCoeff));
        map.put(percentField, remoteForm.getPropertyView(BL.orderArticleSaleDiscount));

        FormData data = remoteForm.getFormData(BaseUtils.toSetElements(doc.groupTo, art.groupTo), BaseUtils.toSetElements(doc.groupTo, art.groupTo));

        updateRemoteFormProperties(remoteForm);

        return data;
    }

    private FormData getDataCert(DataSession session, Map<Field, PropertyView> map) throws Exception {

        // Выгружаем продажи по кассе
        RemoteForm remoteForm = new RemoteForm(BL.saleCheckCertBrowse, BL, session, AuthPolicy.defaultSecurityPolicy, null, null, new DataObject(BL.getComputers().iterator().next(), BL.computer)); // здесь надо переделать на нормальный компьютер

        setRemoteFormFilter(remoteForm);

        PropertyView issued = remoteForm.getPropertyView(BL.issueObligation);
        issued.toDraw.addTempFilter(new NotNullFilter(issued.view));

        ObjectImplement doc = remoteForm.mapper.mapObject(BL.saleCheckCertBrowse.objDoc);
        ObjectImplement obligation = remoteForm.mapper.mapObject(BL.saleCheckCertBrowse.objObligation);

        map.put(barField, remoteForm.getPropertyView(BL.barcode));
        map.put(nameField, remoteForm.getPropertyView(BL.name));
        map.put(cenField, remoteForm.getPropertyView(BL.obligationSum));
        map.put(dateField, remoteForm.getPropertyView(BL.date));
        map.put(summField, remoteForm.getPropertyView(BL.obligationSum));

        FormData data = remoteForm.getFormData(BaseUtils.toSetElements(doc.groupTo, obligation.groupTo), BaseUtils.toSetElements(doc.groupTo, obligation.groupTo));

        updateRemoteFormProperties(remoteForm);

        return data;
    }

    private FormData getDataReturn(DataSession session, Map<Field, PropertyView> map) throws Exception {

        // Выгружаем продажи по кассе
        RemoteForm remoteForm = new RemoteForm(BL.returnSaleCheckRetailBrowse, BL, session, AuthPolicy.defaultSecurityPolicy, null, null, new DataObject(BL.getComputers().iterator().next(), BL.computer)); // здесь надо переделать на нормальный компьютер

        setRemoteFormFilter(remoteForm);

        PropertyView returnQuantity = remoteForm.getPropertyView(BL.returnInnerQuantity);
        returnQuantity.toDraw.addTempFilter(new NotNullFilter(returnQuantity.view));

        ObjectImplement doc = remoteForm.mapper.mapObject(BL.returnSaleCheckRetailBrowse.objDoc);
        ObjectImplement inner = remoteForm.mapper.mapObject(BL.returnSaleCheckRetailBrowse.objInner);
        ObjectImplement article = remoteForm.mapper.mapObject(BL.returnSaleCheckRetailBrowse.objArt);

        map.put(barField, remoteForm.getPropertyView(BL.barcode));
        map.put(nameField, remoteForm.getPropertyView(BL.name));
        map.put(cenField, remoteForm.getPropertyView(BL.orderSalePrice));
        map.put(kolField, remoteForm.getPropertyView(BL.returnInnerQuantity));
        map.put(dateField, remoteForm.getPropertyView(BL.date));
        map.put(summField, remoteForm.getPropertyView(BL.returnArticleSalePay));
        map.put(percentField, remoteForm.getPropertyView(BL.orderArticleSaleDiscount));

        FormData result = remoteForm.getFormData(BaseUtils.toSetElements(doc.groupTo, inner.groupTo, article.groupTo), BaseUtils.toSetElements(doc.groupTo, inner.groupTo, article.groupTo));

        updateRemoteFormProperties(remoteForm);

        return result;
    }

    private void writeToDbf(FormData data, Map<Field, PropertyView> map, boolean reverse) throws Exception {

        Calendar calendar = Calendar.getInstance();

        for (FormRow row : data.rows) {

            barField.put(((String)row.values.get(map.get(barField))).trim());
            nameField.put(((String)row.values.get(map.get(nameField))).trim());
            putDouble(cenField, (Double)row.values.get(map.get(cenField)));

            if (map.get(kolField) != null)
                putDouble(kolField, (Double)row.values.get(map.get(kolField)) * (reverse ? -1 : 1));
            else
                putDouble(kolField, 1.0);

            calendar.setTime(DateConverter.intToDate((Integer)row.values.get(map.get(dateField))));
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

            Map<Field, PropertyView> mapSale = new HashMap<Field, PropertyView>();
            FormData dataSale = getDataSale(session, mapSale);
            writeToDbf(dataSale, mapSale, false);

            Map<Field, PropertyView> mapCert = new HashMap<Field, PropertyView>();
            FormData dataCert = getDataCert(session, mapCert);
            writeToDbf(dataCert, mapCert, false);

            Map<Field, PropertyView> mapReturn = new HashMap<Field, PropertyView>();
            FormData dataReturn = getDataReturn(session, mapReturn);
            writeToDbf(dataReturn, mapReturn, true);

            session.apply(BL);

        } finally {
            if (outDbf != null)
                outDbf.close();
        }

    }

}
