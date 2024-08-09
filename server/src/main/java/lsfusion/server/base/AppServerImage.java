package lsfusion.server.base;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptedStringUtils;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.icon.IconLogicsModule;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServerImage {

    private final static LRUSVSMap<String, AppServerImage> cachedImages = new LRUSVSMap<>(LRUUtil.G3);
    private final static LRUSVSMap<String, Pair<String, Double>> cachedBestIcons = new LRUSVSMap<>(LRUUtil.G3);
    private final static LRUSVSMap<Pair<String, Float>, AppServerImage> cachedDefaultImages = new LRUSVSMap<>(LRUUtil.G3);
    private static final AppServerImage NULLIMAGE = new AppServerImage();

    public AppServerImage() {
    }

    public String imagePath;
    public String fontClasses;

    // should be effectively cached to make appImage cache work properlu
    public AppServerImage(String imagePath, String fontClasses) {
        this.imagePath = imagePath;
        this.fontClasses = fontClasses;
    }

    private AppImage appImage;

    private enum StringType {
        FULL, PATH, ICON, NAME_OR_AUTO;

        public static StringType get(String imageString) {
            if(imageString.contains(";"))
                return FULL;
            else if (imageString.contains("/") || imageString.contains("."))
                return PATH;
            else if (isIcon(imageString))
                return ICON;

            return NAME_OR_AUTO;
        }

        private static boolean isIcon(String imageString) {
            for(String value : imageString.split(" ")) {
                if(value.equals("fa") || value.equals("bi") || value.startsWith("fa-") || value.startsWith("bi-")) {
                    return true;
                }
            }
            return false;
        }
    }

    public interface Reader {
        AppServerImage get(ConnectionContext context);
    }

    // should be cached, because it is used in data images, default images, and can be used several times
    public static Reader createImage(String imageString, Style style, BiFunction<String, ConnectionContext, AppServerImage> autoImage) {
        assert imageString != null;
        StringType type = StringType.get(imageString);
        switch (type) { // should not be cached because is cached inside
            case NAME_OR_AUTO:
                return context -> autoImage.apply(imageString, context);
            case PATH:
                return context -> createDefaultImage(imageString, style, true, Settings.get().getDefaultImagePathRankingThreshold(), context);
        }

        AppServerImage result = cachedImages.get(imageString);
        if(result == null) { // caching here is needed to cache getAppImage
            switch (type) {
                case FULL:
                    String[] split = imageString.split(";");
                    result = new AppServerImage(split[0], split[1]);
                    break;
                case ICON:
                    result = new AppServerImage(null, imageString);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            cachedImages.put(imageString, result);
        }

        AppServerImage fResult = result;
        return context -> fResult;
    }

    public final static ThreadLocal<MSet<String>> prereadBestIcons = new ThreadLocal<>();

    public static void prereadBestIcons(BusinessLogics BL, DBManager dbManager, ImSet<String> images) {
        ImMap<String, Pair<String, Double>> readImages = readBestIcons(BL, dbManager, images);
        for(int i = 0, size = readImages.size(); i < size; i++)
            cachedBestIcons.put(readImages.getKey(i), readImages.getValue(i));
    }

    public static Pair<String, Double> getBestIcon(String name) {
        Pair<String, Double> result = cachedBestIcons.get(name);
        if(result == null) {
            MSet<String> prereadSet = prereadBestIcons.get();
            if(prereadSet != null) {
                prereadSet.add(name);
                return null;
            }

            result = readBestIcons(ThreadLocalContext.getBusinessLogics(), ThreadLocalContext.getDbManager(), SetFact.singleton(name)).singleValue();

            cachedBestIcons.put(name, result);
        }

        return result;
    }

    private static final Pair<String, Double> NOICON = new Pair<>(null, 0.0);
    private static ImMap<String, Pair<String, Double>> readBestIcons(BusinessLogics BL, DBManager dbManager, ImSet<String> cacheKeys) {
        IconLogicsModule iconLM = BL.iconLM;
        if(iconLM.bestIconNames == null)
            return cacheKeys.mapValues(() -> NOICON);

        try(DataSession session = dbManager.createSession()) {
            ExecutionEnvironment env = session;

            iconLM.bestIconNames.change(session, env, cacheKeys.mapValues(() -> true));

            iconLM.getBestIcons.execute(env, ThreadLocalContext.getStack());

            ImOrderMap<ImMap<Integer, Object>, ImMap<Integer, Object>> bestIcons = LP.readAll(new LP[]{iconLM.bestIconClasses, iconLM.bestIconRanks}, env);

            return cacheKeys.mapValues((String key) -> {
                ImMap<Integer, Object> nameKey = MapFact.singleton(0, key);
                ImMap<Integer, Object> bestIcon = bestIcons.get(nameKey);
                if(bestIcon == null)
                    return NOICON;
                return new Pair<>((String) bestIcon.get(0), BaseUtils.nvl((Double) bestIcon.get(1), 0.0));
            });
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    public enum Style {
        PROPERTY, CONTAINER, FORM, NAVIGATORELEMENT;

        public String getSearchName(ConnectionContext context) { // should correspond Icon.searchStyles / Icon.iconClass properties
            if(this == PROPERTY || this == CONTAINER || !context.useBootstrap)
                return "regular";

            // FORM / NAVIGATORELEMENT
            return "solid";
        }
    }

    // should be cached because it is used in default images
    private static AppServerImage createDefaultImage(String name, Style style, boolean path, float rankingThreshold, ConnectionContext context) {
        String searchName = style.getSearchName(context);
        Pair<String, Float> cacheKey = new Pair<>(name + "," + searchName, rankingThreshold);
        AppServerImage result = cachedDefaultImages.get(cacheKey);
        if(result == null) {
            Pair<String, Double> bestIcon = getBestIcon((path ? BaseUtils.getFileName(name) : name) + "," + searchName);
            if(bestIcon == null)
                return null;

            if(bestIcon.first == null || bestIcon.second < rankingThreshold || bestIcon.first.equals(AppServerImage.NULL))
                result = path ? new AppServerImage(name, null) : NULLIMAGE;
            else
                result = new AppServerImage(path ? name : null, bestIcon.first);

            cachedDefaultImages.put(cacheKey, result);
        }
        return result == NULLIMAGE ? null : result;
    }

    private static final Pattern camelCasePattern = Pattern.compile("(([A-Z]?[a-z]+)|([A-Z]))");
    public static List<String> splitCamelCase(String text) {
        Matcher matcher = camelCasePattern.matcher(text);
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group(0));
        }
        return words;
    }

    public static AppServerImage createDefaultImage(float rankingThreshold, String name, Style style, AutoName autoName, Reader defaultImage, ConnectionContext context) {
        if(name.equals(AppServerImage.NULL))
            return null;
        else if(name.equals(AppServerImage.AUTO)) {
            name = autoName.get();
        }

        if(name != null) {
            AppServerImage autoImage = createDefaultImage(name, style, false, rankingThreshold, context);
            if (autoImage != null)
                return autoImage;
        }

        return defaultImage.get(context);
    }
    
    public static final String AUTO = "auto";
    public static final String NULL = "null";
    private static final boolean AUTO_ICON = true;

    public static final String ADD = "add.png"; // also is used in the input action
    public static final String EDIT =  "edit.png";
    public static final String DELETE = "delete.png";
    public static final String EMAIL = "email.png";

    private static final Pattern isEnglishCaption = Pattern.compile("(\\w|\\s|\\p{Punct})+");
    private static String replaceAll(String text, Function<Matcher, String> replace) {
        Matcher m = toCamelCase.matcher(text);

        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(text.substring(last, m.start()));
            sb.append(replace.apply(m));
            last = m.end();
        }
        sb.append(text.substring(last));

        return sb.toString();
    }

    private static final Pattern toCamelCase = Pattern.compile("(?:\\s|\\p{Punct})+(\\w|\\z)");
    private static String toCamelCase(String text) {
        return replaceAll(text, m -> m.group(1).toUpperCase());
    }

    // lowering sequential capital letters and removing words with length <= 2
    private static String lowerSequentialAndRemoveShort(String string) {
        if(string.isEmpty())
            return string;

        StringBuilder result = new StringBuilder();

        int i = 0;
        while(true) {
            // we're at the capital
            StringBuilder word = new StringBuilder();
            word.append(Character.toUpperCase(string.charAt(i)));

            // looking for the next character
            i++;
            if(i < string.length()) {
                char c = string.charAt(i);
                if (Character.isUpperCase(c)) {
                    while (true) {
                        i++;
                        if (i >= string.length()) {
                            word.append(Character.toLowerCase(c));
                            break;
                        }

                        char nextC = string.charAt(i);
                        if (!Character.isUpperCase(nextC)) {
                            i--;
                            break;
                        }

                        word.append(Character.toLowerCase(c));
                        c = nextC;
                    }
                } else {
                    while (true) {
                        word.append(c);

                        i++;
                        if (i >= string.length())
                            break;

                        char nextC = string.charAt(i);
                        if (Character.isUpperCase(nextC))
                            break;
                        else
                            c = nextC;
                    }
                }
            }

            // we don't want auto short names (like i, b, ac, which are often used for the object aliasing)
            if(word.length() > 2)
                result.append(word);

            if(i >= string.length())
                break;
        }
        return result.toString();
    }

    public interface AutoName {
        String get();
    }

    public static AutoName getAutoName(Supplier<LocalizedString> caption, Supplier<String> name) {
        return () -> {
            String camelCased = null;
            LocalizedString readCaption = caption.get();
            if(readCaption != null && !readCaption.isEmpty()) {
                String englishCaption = ThreadLocalContext.localize(readCaption, Locale.ENGLISH);
                if(isEnglishCaption.matcher(englishCaption).matches()) {
                    camelCased = toCamelCase(englishCaption);
//                    can't use it for now, because it requires Java 9
//                  camelCased = toCamelCase.matcher(englishCaption).replaceAll(match -> match.group(2).toUpperCase());
                }
            }
            if(camelCased == null)
                camelCased = name.get();

            if(camelCased == null)
                return null;

            camelCased = lowerSequentialAndRemoveShort(camelCased);
            if(camelCased.isEmpty())
                return null;

            return camelCased;
        };
    }

    public static Reader createPropertyImage(String imagePath, AutoName autoName) {
        return createImage(imagePath, Style.PROPERTY, (name, context) -> ActionOrProperty.getDefaultImage(name, autoName, Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON, context));
    }

    public static Reader createPropertyImage(String imagePath, PropertyDrawView property) {
        return createPropertyImage(imagePath, property.getAutoName());
    }

    public static Reader createContainerImage(String imagePath, ContainerView container, FormView formView) {
        return createImage(imagePath, container.main ? Style.FORM : Style.CONTAINER, (name, context) -> container.getDefaultImage(name, Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON, formView, context));
    }

    public static final String FORMTOP = "formTop.png";
    public static final String FORM = "form.png"; // also used in the default container icon
    public static final String ACTIONTOP = "actionTop.png";
    public static final String ACTION = "action.png"; // also used in the default action icon
    public static final String OPENTOP = "openTop.png";
    public static final String OPEN = "open.png";

    public static Reader createNavigatorImage(String imagePath, NavigatorElement navigator) {
        return createImage(imagePath, Style.NAVIGATORELEMENT, (name, context) -> navigator.getDefaultImage(name, Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON, context));
    }

    public static final String DIALOG = "dialog.png";
    public static final String RESET = "reset.png";

    public static Reader createActionImage(String imagePath) {
        return createImage(imagePath, Style.PROPERTY, (name, context) -> AppServerImage.createDefaultImage(Settings.get().getDefaultAutoImageRankingThreshold(), name, Style.PROPERTY, AppServerImage.getAutoName(() -> null, () -> name), defaultContext -> AUTO_ICON ? createActionImage(ACTION).get(defaultContext) : null, context));
    }

    @ManualLazy
    public AppImage getAppImage() throws IOException {
        if(appImage == null) {
            String imagePath = this.imagePath;

            Map<ColorTheme, RawFileData> images = null;
            String fullImagePath = null;
            if(imagePath != null) {
                images = new HashMap<>();

                for (ColorTheme colorTheme : ColorTheme.values()) {
                    Result<String> fullPath = new Result<>();
                    boolean defaultTheme = colorTheme.isDefault();
                    // multipleUsages true, because one imagePath is often used a lot of times
                    RawFileData themedImageFile = ResourceUtils.findResourceAsFileData(colorTheme.getImagePath(imagePath), !defaultTheme, true, fullPath, "images");
                    if (defaultTheme || themedImageFile != null) {
                        if(themedImageFile == null)
                            throw new FileNotFoundException(imagePath);
                        themedImageFile.getID(); // to calculate the cache
                        images.put(colorTheme, themedImageFile);
                    }
                    if (defaultTheme)
                        fullImagePath = fullPath.result;
                }
            }

            String fontClasses = this.fontClasses;
//            if(fontClasses == null)
//                fontClasses = predefinedFontClasses.get(BaseUtils.getFileNameAndExtension(imagePath));

            appImage = new AppImage(fontClasses, fullImagePath, images);
        }

        return appImage;
    }

    public static AppImage getAppImage(AppServerImage appImage) throws IOException {
        return appImage != null ? appImage.getAppImage() : null;
    }

    public static void serialize(AppServerImage image, DataOutputStream dataOutputStream) throws IOException {
        IOUtils.writeAppImage(dataOutputStream, getAppImage(image));
    }
    public static void serialize(AppServerImage image, DataOutputStream outStream, ServerSerializationPool pool) throws IOException {
        pool.writeImageIcon(outStream, getAppImage(image));
    }

    public static String convertFileValue(AppServerImage image, boolean asImage) throws IOException {
        if(asImage)
            return ScriptedStringUtils.wrapSerializedImage(IOUtils.serializeAppImage(getAppImage(image)));
        else
            return ScriptedStringUtils.wrapResource(image.imagePath);
    }
}
