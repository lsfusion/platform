package lsfusion.client.form.property.cell.classes.view;

import com.google.common.base.Throwables;
import lsfusion.base.file.AppFileDataImage;
import lsfusion.base.file.RawFileData;
import lsfusion.client.form.property.ClientPropertyDraw;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PDFPropertyRenderer extends ImagePropertyRenderer {
    public PDFPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);

        setIcon(value != null ? convertValue(((AppFileDataImage) value)) : null);
    }

    public static void expandImage(final AppFileDataImage value) {
        if (value != null) {
            expandImage(convertValue(value));
        }
    }

    public static Image convertValue(AppFileDataImage value) {
        try {
            PDDocument pd = PDDocument.load(RawFileData.toRawFileData(value.data).getInputStream());
            PDFRenderer pr = new PDFRenderer(pd);
            BufferedImage bi = pr.renderImageWithDPI(0, 300);

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            ImageIO.write(bi, "jpeg", os);

            return ImageIO.read(ImageIO.createImageInputStream(new ByteArrayInputStream(os.toByteArray())));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
