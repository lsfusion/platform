package lsfusion.server.logics.classes.data.file;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.classes.DataType;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.stat.struct.FormIntegrationType;
import lsfusion.server.logics.form.stat.struct.export.plain.xls.ExportXLSWriter;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

public class ImageClass extends StaticFormatFileClass {

    protected String getFileSID() {
        return "IMAGEFILE";
    }

    private static Collection<ImageClass> instances = new ArrayList<>();

    public static ImageClass get() {
        return get(false, false);
    }
    public static ImageClass get(boolean multiple, boolean storeName) {
        for (ImageClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        ImageClass instance = new ImageClass(multiple, storeName);
        instances.add(instance);
        DataClass.storeClass(instance);
        return instance;
    }

    private ImageClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public byte getTypeID() {
        return DataType.IMAGE;
    }

    public String getOpenExtension(RawFileData file) {
        String extension = null;
        if (file.getBytes().length >= 2) {
            if (file.getBytes()[0] == (byte) 0x89 && file.getBytes()[1] == (byte) 0x50) {
                extension = "png";
            } else if (file.getBytes()[0] == (byte) 0x42 && file.getBytes()[1] == (byte) 0x4D) {
                extension = "bmp";
            }
        }
        return extension == null ? "jpg" : extension;
    }

    @Override
    public String getExtension() {
        return "jpg";
    }

    @Override
    public Class getReportJavaClass() {
        return InputStream.class;
    }

    @Override
    public int getReportPreferredWidth() {
        return 60;
    }

    @Override
    public FormIntegrationType getIntegrationType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void formatXLS(RawFileData object, Cell cell, ExportXLSWriter.Styles styles) {

        if (object != null) {
            String extension = getOpenExtension(object);
            int format = extension.equals("jpg") ? Workbook.PICTURE_TYPE_JPEG : extension.equals("png") ? Workbook.PICTURE_TYPE_PNG : 0;
            if (format != 0) {
                int inputImagePicture = cell.getSheet().getWorkbook().addPicture(object.getBytes(), format);
                ClientAnchor anchor = cell instanceof XSSFCell ?
                        new XSSFClientAnchor(0, 0, 0, 0, cell.getColumnIndex(), cell.getRowIndex(), cell.getColumnIndex() + 1, cell.getRowIndex() + 1) :
                        new HSSFClientAnchor(0, 0, 0, 0, (short) cell.getColumnIndex(), cell.getRowIndex(), (short) (cell.getColumnIndex() + 1), cell.getRowIndex() + 1);
                cell.getSheet().createDrawingPatriarch().createPicture(anchor, inputImagePicture);
                return;
            }
        }

        super.formatXLS(object, cell, styles);
    }
}
