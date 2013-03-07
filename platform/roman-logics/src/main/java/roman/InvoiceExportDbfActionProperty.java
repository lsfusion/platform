package roman;

import org.xBaseJ.Util;
import org.xBaseJ.fields.CharField;
import org.xBaseJ.fields.DateField;
import org.xBaseJ.fields.Field;
import org.xBaseJ.fields.NumField;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.interop.action.ExportFileClientAction;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.SecurityManager;
import platform.server.classes.ValueClass;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.UserActionProperty;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class InvoiceExportDbfActionProperty extends UserActionProperty {
    private BaseLogicsModule baseLM;
    private RomanLogicsModule RomanLM;

    private DBFExporter.CustomDBF dbfInvoice;
    private File tempDbfInvoice;

    public InvoiceExportDbfActionProperty(String sID, String caption, ValueClass importer, ValueClass freight, ValueClass invoiceType, RomanLogicsModule RomanLM, BaseLogicsModule baseLM) {
        super(sID, caption, new ValueClass[]{importer, freight, invoiceType});
        this.baseLM = baseLM;
        this.RomanLM = RomanLM;
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            InvoiceExporter exporter = new InvoiceExporter(context.getKeys());
            exporter.extractData(context);
            context.delayUserInterfaction(new ExportFileClientAction("invoice.dbf", IOUtils.getFileBytes(dbfInvoice.getFFile())));
            tempDbfInvoice.delete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private class InvoiceExporter extends DBFExporter {
        DataObject importerObject, freightObject, invoiceTypeObject;

        CharField invN = new CharField("INVN", 50);
        DateField date = new DateField("DATE");
        CharField boxN = new CharField("BOXN", 13);
        CharField art = new CharField("ART", 50);
        CharField name = new CharField("NAME", 50);
        CharField color = new CharField("COLOR", 50);
        CharField size = new CharField("SIZE", 50);
        CharField commonSize = new CharField("COMMONSIZE", 110);
        CharField comp = new CharField("COMP", 110);
        CharField countOrig = new CharField("COUNT_ORIG", 110);
        CharField countProd = new CharField("COUNT_PROD", 110);
        CharField barcode = new CharField("BARCODE", 13);
        CharField brand = new CharField("BRAND", 110);
        CharField gender = new CharField("GENDER", 50);
        CharField theme = new CharField("THEME", 110);
        CharField season = new CharField("SEASON", 110);
        CharField categ = new CharField("CATEG", 110);
        NumField quant = new NumField("QUANT", 4, 0);
        NumField price = new NumField("PRICE", 8, 2);
        NumField sum = new NumField("SUM", 8, 2);
        NumField rrp = new NumField("RRP", 8, 2);
        //CharField contractIn = new CharField("CONTIN", 20);
        //DateField dateConIn = new DateField("DATEIN");
        NumField priceIn = new NumField("PRICEIN", 8, 2);
        NumField sumIn = new NumField("SUMIN", 8, 2);
        CharField contract = new CharField("CONTRACT", 20);
        DateField dateCon = new DateField("DATECON");
        CharField imp = new CharField("IMPORTER", 110);

        public InvoiceExporter(ImMap<ClassPropertyInterface, DataObject> keys) throws IOException, xBaseJException {
            super(keys);
            ImOrderSet<ClassPropertyInterface> interfacesList = getOrderInterfaces();
            importerObject = keys.get(interfacesList.get(0));
            freightObject = keys.get(interfacesList.get(1));
            invoiceTypeObject = keys.get(interfacesList.get(2));

            tempDbfInvoice = File.createTempFile("dbfInvoice", ".DBF");
            dbfInvoice = new CustomDBF(tempDbfInvoice.getPath(), true, "Cp866");
            Util.setxBaseJProperty("ignoreDBFLengthCheck", "true");
            dbfInvoice.addField(new Field[]{invN, date, boxN, art, name, color, size, commonSize, comp, countOrig, countProd, barcode,
                    brand, gender, theme, season, categ, quant, price, sum, rrp, /*contractIn, dateConIn, */priceIn, sumIn, imp, contract, dateCon});
        }

        public void extractData(ExecutionContext<ClassPropertyInterface> context) throws SQLException, IOException, xBaseJException {
            getPropertyDraws(context);
            for (FormRow row : data.rows) {
                writeData(row);
            }
            dbfInvoice.close();
            session.close();
        }

        public void getPropertyDraws(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            session = context.createSession();
            map = new HashMap<Field, PropertyDrawInstance>();
            FormInstance formInstance = new FormInstance(RomanLM.invoiceExportForm, context.getLogicsInstance(), session, SecurityManager.serverSecurityPolicy, null, null, context.getDbManager().getServerComputerObject(), null);
            ObjectInstance objFreight = formInstance.instanceFactory.getInstance(RomanLM.invoiceExportForm.objFreight);
            ObjectInstance objImporter = formInstance.instanceFactory.getInstance(RomanLM.invoiceExportForm.objImporter);
            ObjectInstance objTypeInvoice = formInstance.instanceFactory.getInstance(RomanLM.invoiceExportForm.objTypeInvoice);
            ObjectInstance objFreightBox = formInstance.instanceFactory.getInstance(RomanLM.invoiceExportForm.objFreightBox);
            ObjectInstance objSku = formInstance.instanceFactory.getInstance(RomanLM.invoiceExportForm.objSku);
            objFreight.changeValue(session, freightObject);
            objImporter.changeValue(session, importerObject);
            objTypeInvoice.changeValue(session, invoiceTypeObject);

            map.put(invN, formInstance.getPropertyDraw(RomanLM.sidImporterFreightTypeInvoice));
            map.put(date, formInstance.getPropertyDraw(RomanLM.dateImporterFreightTypeInvoice));
            map.put(boxN, formInstance.getPropertyDraw(RomanLM.barcode, objFreightBox));
            map.put(art, formInstance.getPropertyDraw(RomanLM.sidArticleSku));
            map.put(name, formInstance.getPropertyDraw(RomanLM.originalNameArticleSku));
            map.put(color, formInstance.getPropertyDraw(RomanLM.sidColorSupplierItem));
            map.put(size, formInstance.getPropertyDraw(RomanLM.sidSizeSupplierItem));
            map.put(commonSize, formInstance.getPropertyDraw(RomanLM.nameCommonSizeSku));
            map.put(comp, formInstance.getPropertyDraw(RomanLM.mainCompositionFreightSku));
//            map.put(countOrig, formInstance.getPropertyDraw());
            map.put(countOrig, formInstance.getPropertyDraw(RomanLM.nameCountryBrandSupplierSku));
            map.put(countProd, formInstance.getPropertyDraw(RomanLM.nameCountryOfOriginFreightSku));
            map.put(barcode, formInstance.getPropertyDraw(RomanLM.barcode, objSku));
            map.put(brand, formInstance.getPropertyDraw(RomanLM.nameBrandSupplierArticleSku));
            map.put(gender, formInstance.getPropertyDraw(RomanLM.sidGenderArticleSku));
            map.put(theme, formInstance.getPropertyDraw(RomanLM.nameThemeSupplierArticleSku));
            map.put(season, formInstance.getPropertyDraw(RomanLM.nameSeasonYearArticleSku));
            map.put(categ, formInstance.getPropertyDraw(RomanLM.nameCategoryArticleSku));
            map.put(quant, formInstance.getPropertyDraw(RomanLM.quantityImporterStockSku));
            map.put(price, formInstance.getPropertyDraw(RomanLM.priceInvoiceImporterFreightSku));
            map.put(sum, formInstance.getPropertyDraw(RomanLM.sumInvoiceImporterStockSku));
            map.put(rrp, formInstance.getPropertyDraw(RomanLM.RRPInImporterFreightSku));
            //map.put(contractIn, formInstance.getPropertyDraw(BL.RomanLM.sidContractInProxyImporterStockSku));
            //map.put(dateConIn, formInstance.getPropertyDraw(BL.RomanLM.dateContractInProxyImporterStockSku));
            map.put(priceIn, formInstance.getPropertyDraw(RomanLM.priceInImporterFreightSku));
            map.put(sumIn, formInstance.getPropertyDraw(RomanLM.sumInImporterStockSku));
            map.put(imp, formInstance.getPropertyDraw(baseLM.name, objImporter.groupTo));
            map.put(contract, formInstance.getPropertyDraw(RomanLM.sidContractImporterFreight));
            map.put(dateCon, formInstance.getPropertyDraw(RomanLM.dateContractImporterFreight));

            data = formInstance.getFormData(map.values(), BaseUtils.toSet(objFreightBox.groupTo));
        }

        private void writeData(FormRow row) throws IOException, xBaseJException {
            putString(invN, row.values.get(map.get(invN)));
            putDate(date, row.values.get(map.get(date)));
            putString(boxN, row.values.get(map.get(boxN)));
            putString(art, row.values.get(map.get(art)));
            putString(name, row.values.get(map.get(name)));
            putString(color, row.values.get(map.get(color)));
            putString(size, row.values.get(map.get(size)));
            putString(commonSize, row.values.get(map.get(commonSize)));
            putString(comp, row.values.get(map.get(comp)));
            putString(countOrig, row.values.get(map.get(countOrig)));
            putString(countProd, row.values.get(map.get(countProd)));
            putString(barcode, row.values.get(map.get(barcode)));
            putString(brand, row.values.get(map.get(brand)));
            putString(gender, row.values.get(map.get(gender)));
            putString(theme, row.values.get(map.get(theme)));
            putString(season, row.values.get(map.get(season)));
            putString(categ, row.values.get(map.get(categ)));
            putDouble(quant, row.values.get(map.get(quant)));
            putDouble(price, row.values.get(map.get(price)));
            putDouble(sum, row.values.get(map.get(sum)));
            putDouble(rrp, row.values.get(map.get(rrp)));
            //putString(contractIn, row.values.get(map.get(contractIn)));
            //putDate(dateConIn, row.values.get(map.get(dateConIn)));
            putDouble(priceIn, row.values.get(map.get(priceIn)));
            putDouble(sumIn, row.values.get(map.get(sumIn)));
            putString(imp, row.values.get(map.get(imp)));
            //putString(contract, row.values.get(map.get(contract)));
            //putDate(dateCon, row.values.get(map.get(dateCon)));
            dbfInvoice.write();
        }
    }
}
