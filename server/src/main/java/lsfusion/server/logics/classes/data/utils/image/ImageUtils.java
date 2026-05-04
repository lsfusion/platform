package lsfusion.server.logics.classes.data.utils.image;

import lsfusion.base.file.RawFileData;
import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.awt.image.BufferedImage.TYPE_BYTE_INDEXED;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class ImageUtils {

    public static RawFileData createThumbnails(RawFileData inputFile, BufferedImage image, double scale) throws IOException {
        return createThumbnails(inputFile, image, scale, scale);
    }

    public static RawFileData createThumbnails(RawFileData inputFile, BufferedImage image, double scaleWidth, double scaleHeight) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(inputFile.getInputStream()).scale(scaleWidth, scaleHeight);
            if (image.getType() == TYPE_BYTE_INDEXED) {
                builder.imageType(TYPE_INT_ARGB);
            }
            builder.toOutputStream(os);
            return new RawFileData(os);
        }
    }
}