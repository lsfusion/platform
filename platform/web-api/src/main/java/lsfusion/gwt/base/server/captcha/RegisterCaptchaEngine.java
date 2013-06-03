package lsfusion.gwt.base.server.captcha;

import com.octo.captcha.component.image.backgroundgenerator.*;
import com.octo.captcha.component.image.color.RandomListColorGenerator;
import com.octo.captcha.component.image.fontgenerator.FontGenerator;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.textpaster.RandomTextPaster;
import com.octo.captcha.component.image.textpaster.TextPaster;
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.WordToImage;
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;
import com.octo.captcha.engine.image.ListImageCaptchaEngine;
import com.octo.captcha.image.gimpy.GimpyFactory;

import java.awt.*;

public class RegisterCaptchaEngine extends ListImageCaptchaEngine {
    protected void buildInitialFactories() {
        Font[] fontsList = new Font[]{new Font("Arial", 0, 11), new Font("Tahoma", 0, 11), new Font("Verdana", 0, 11)};
        FontGenerator fontGenerator = new RandomFontGenerator(16, 24, fontsList);

        BackgroundGenerator backgroundGenerator = new MultipleShapeBackgroundGenerator(220, 70, new Color(88, 88, 88),
                new Color(220, 220, 220), 12, 16, 30, 24, Color.LIGHT_GRAY, Color.GRAY, 11);

        WordGenerator wgen = new RandomWordGenerator("ABCDEFGHIJKLMNPQRSTUVWXYZ123456789");
        Color[] colors = {Color.RED, Color.ORANGE, Color.GREEN, Color.WHITE, Color.CYAN, Color.BLACK, Color.BLUE, Color.MAGENTA};
        RandomListColorGenerator cgen = new RandomListColorGenerator(colors);
        TextPaster textPaster = new RandomTextPaster(6, 8, cgen, true);

        WordToImage wordToImage = new ComposedWordToImage(fontGenerator, backgroundGenerator, textPaster);
        this.addFactory(new GimpyFactory(wgen, wordToImage));
    }
}