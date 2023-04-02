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
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.icon.IconLogicsModule;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServerImage {

    private final static LRUSVSMap<String, AppServerImage> cachedImages = new LRUSVSMap<>(LRUUtil.G2);
    private final static LRUSVSMap<String, Pair<String, Double>> cachedBestIcons = new LRUSVSMap<>(LRUUtil.G2);
    private final static LRUSVSMap<Pair<String, Float>, AppServerImage> cachedDefaultImages = new LRUSVSMap<>(LRUUtil.G2);
    private static final AppServerImage NULL = new AppServerImage();

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
            else if (imageString.contains(" ") || imageString.contains("-"))
                return ICON;

            return NAME_OR_AUTO;
        }
    }
    // should be cached, because it is used in data images, default images, and can be used several times
    public static Supplier<AppServerImage> createImage(String imageString, Style style, Function<String, AppServerImage> autoImage) {
        assert imageString != null;
        StringType type = StringType.get(imageString);
        switch (type) { // should not be cached because is cached inside
            case NAME_OR_AUTO:
                return () -> autoImage.apply(imageString);
            case PATH:
                return () -> createDefaultImage(imageString, style, true, Settings.get().getDefaultImagePathRankingThreshold());
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
        return () -> fResult;
    }

    private final static ThreadLocal<MSet<String>> prereadBestIcons = new ThreadLocal<>();
    public static void prereadBestIcons(BusinessLogics BL, DBManager dbManager, Runnable run) {
        prereadBestIcons.set(SetFact.mSet());
        try {
            run.run();

            MSet<String> mImages = prereadBestIcons.get();
            ImMap<String, Pair<String, Double>> readImages = readBestIcons(BL, dbManager, mImages.immutable());
            for(int i = 0, size = readImages.size(); i < size; i++)
                cachedBestIcons.put(readImages.getKey(i), readImages.getValue(i));
        } finally {
            prereadBestIcons.set(null);
        }
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

            iconLM.bestIconNames.change(session, env, cacheKeys.mapValues(() -> true), StringClass.instance, LogicalClass.instance);

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
        REGULAR, SOLID;

        public static final Style PROPERTY = REGULAR;
        public static final Style CONTAINER = REGULAR;
        public static final Style FORM = SOLID;
        public static final Style NAVIGATORELEMENT = SOLID;

        public String getSearchName() { // should correspond Icon.searchStyles / Icon.iconClass properties
            return this == REGULAR ? "Lsfreg" : "Lsfsol";
        }
    }

    // should be cached because it is used in default images
    private static AppServerImage createDefaultImage(String name, Style style, boolean path, float rankingThreshold) {
        if(rankingThreshold >= 1.0f) // optimization
            return null;

        Pair<String, Float> cacheKey = new Pair<>(name + "<->" + style, rankingThreshold);
        AppServerImage result = cachedDefaultImages.get(cacheKey);
        if(result == null) {
            Pair<String, Double> bestIcon = getBestIcon((path ? BaseUtils.getFileName(name) : name) + style.getSearchName()); // should correspond Icon.iconClass / Icon.match
            if(bestIcon == null)
                return null;

            if(bestIcon.first == null || bestIcon.second < rankingThreshold)
                result = path ? new AppServerImage(name, null) : NULL;
            else
                result = new AppServerImage(path ? name : null, bestIcon.first);

            cachedDefaultImages.put(cacheKey, result);
        }
        return result == NULL ? null : result;
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

    public static AppServerImage createDefaultImage(float rankingThreshold, String name, Style style, Supplier<AppServerImage> defaultImage) {
        if(name == null)
            return null;

        AppServerImage autoImage = createDefaultImage(name, style, false, rankingThreshold);
        if(autoImage != null)
            return autoImage;

        return defaultImage.get();
    }
    
    public static final String AUTO = "auto";
    private static final boolean AUTO_ICON = true;

    public static final String ADD = "add.png"; // also is used in the input action
    public static final String EDIT =  "edit.png";
    public static final String DELETE = "delete.png";
    public static final String EMAIL = "email.png";

    public static Supplier<AppServerImage> createPropertyImage(String imagePath, ActionOrProperty property) {
        return createImage(imagePath, Style.PROPERTY, name -> property.getDefaultImage(name, Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON));
    }

    public static Supplier<AppServerImage> createPropertyImage(String imagePath, PropertyDrawEntity property) {
        return createPropertyImage(imagePath, property.getInheritedProperty());
    }

    public static Supplier<AppServerImage> createPropertyImage(String imagePath, PropertyDrawView property) {
        return createPropertyImage(imagePath, property.entity);
    }

    public static Supplier<AppServerImage> createContainerImage(String imagePath, ContainerView container, FormView formView) {
        return createImage(imagePath, container.main ? Style.FORM : Style.CONTAINER, name -> container.getDefaultImage(name, Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON, formView));
    }

    public static final String FORMTOP = "formTop.png";
    public static final String FORM = "form.png"; // also used in the default container icon
    public static final String ACTIONTOP = "actionTop.png";
    public static final String ACTION = "action.png"; // also used in the default action icon
    public static final String OPENTOP = "openTop.png";
    public static final String OPEN = "open.png";

    public static Supplier<AppServerImage> createNavigatorImage(String imagePath, NavigatorElement navigator) {
        return createImage(imagePath, Style.NAVIGATORELEMENT, name -> navigator.getDefaultImage(name, Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON));
    }

    public static final String DIALOG = "dialog.png";
    public static final String RESET = "reset.png";

    public static Supplier<AppServerImage> createActionImage(String imagePath) {
        return createImage(imagePath, Style.PROPERTY, name -> AppServerImage.createDefaultImage(Settings.get().getDefaultAutoImageRankingThreshold(), name, Style.PROPERTY, () -> AUTO_ICON ? createActionImage(ACTION).get() : null));
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
        IOUtils.writeImageIcon(dataOutputStream, getAppImage(image));
    }
    public static void serialize(AppServerImage image, DataOutputStream outStream, ServerSerializationPool pool) throws IOException {
        pool.writeImageIcon(outStream, getAppImage(image));
    }
}
