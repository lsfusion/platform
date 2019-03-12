package lsfusion.server.classes;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.logics.property.actions.integration.FormIntegrationType;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ExcelClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "EXCELFILE";
    }

    private static Collection<ExcelClass> instances = new ArrayList<>();

    public static ExcelClass get() {
        return get(false, false);
    }
    
    public static ExcelClass get(boolean multiple, boolean storeName) {
        for (ExcelClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        ExcelClass instance = new ExcelClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private ExcelClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.EXCEL;
    }

    public String getOpenExtension(RawFileData file) {
        try {
            return DocumentFactoryHelper.hasOOXMLHeader(file.getInputStream()) ? "xlsx" : "xls";
        } catch (IOException e) {
            return "xls";
        }
    }

    @Override
    public String getExtension() {
        return "xls";
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        return FormIntegrationType.XLS;
    }
}
