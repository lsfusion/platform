package lsfusion.server.base;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
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
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServerImage {

    public static final MAddExclMap<String, String> predefinedFontClasses = MapFact.mAddExclMap();

    static {
        AppServerImage.predefinedFontClasses.exclAdd("apply.png", "bi bi-save");
        AppServerImage.predefinedFontClasses.exclAdd("cancel.png", "bi bi-archive");
        AppServerImage.predefinedFontClasses.exclAdd("ok.png", "bi bi-check");
        AppServerImage.predefinedFontClasses.exclAdd("close.png", "bi bi-x");
        AppServerImage.predefinedFontClasses.exclAdd("editReport.png", "bi bi-pencil-square");
        AppServerImage.predefinedFontClasses.exclAdd("refresh.png", "bi bi-arrow-repeat");

        AppServerImage.predefinedFontClasses.exclAdd("add.png", "fa fa-plus");
        AppServerImage.predefinedFontClasses.exclAdd("edit.png", "fa fa-pen");
        AppServerImage.predefinedFontClasses.exclAdd("delete.png", "fa fa-minus");

        AppServerImage.predefinedFontClasses.exclAdd("email.png", "fa fa-envelope");

        AppServerImage.predefinedFontClasses.exclAdd("dialog.png", "fa fa-ellipsis");
        AppServerImage.predefinedFontClasses.exclAdd("reset.png", "fa fa-xmark");

        AppServerImage.predefinedFontClasses.exclAdd("print.png", "bi bi-printer");
        AppServerImage.predefinedFontClasses.exclAdd("form.png", "fa fa-clone");
        AppServerImage.predefinedFontClasses.exclAdd("formTop.png", "fa fa-clone");
        AppServerImage.predefinedFontClasses.exclAdd("action.png", "fa fa-square-caret-right");
        AppServerImage.predefinedFontClasses.exclAdd("actionTop.png", "fa fa-square-caret-right");
        AppServerImage.predefinedFontClasses.exclAdd("open.png", "fa fa-folder");

        AppServerImage.predefinedFontClasses.exclAdd("tools.png", "fa fa-screwdriver-wrench");
        AppServerImage.predefinedFontClasses.exclAdd("lock.png", "fa bi-person-fill");
        AppServerImage.predefinedFontClasses.exclAdd("search.png", "fa fa-magnifying-glass");

        AppServerImage.predefinedFontClasses.exclAdd("relogin.png", "fa fa-elevator");
        AppServerImage.predefinedFontClasses.exclAdd("editProfile.png", "fa fa-user-pen");
        AppServerImage.predefinedFontClasses.exclAdd("changePassword.png", "fa fa-key");
        AppServerImage.predefinedFontClasses.exclAdd("logout.png", "fa fa-right-from-bracket");

        AppServerImage.predefinedFontClasses.exclAdd("chat.png", "fa fa-comment");
        AppServerImage.predefinedFontClasses.exclAdd("play.png", "fa fa-play");

        AppServerImage.predefinedFontClasses.exclAdd("bootstrap.png", "fa-brands fa-bootstrap");
        AppServerImage.predefinedFontClasses.exclAdd("excel.png", "fa fa-table-list");

        AppServerImage.predefinedFontClasses.exclAdd("lightMode.png", "fa fa-sun");
        AppServerImage.predefinedFontClasses.exclAdd("darkMode.png", "fa fa-moon");

        AppServerImage.predefinedFontClasses.exclAdd("catalog.png", "fa fa-list-check");

        AppServerImage.predefinedFontClasses.exclAdd("inventory.png", "fa fa-warehouse");
        AppServerImage.predefinedFontClasses.exclAdd("invoicing.png", "fa fa-money-bill");
        AppServerImage.predefinedFontClasses.exclAdd("purchase.png", "fa fa-cart-flatbed");
        AppServerImage.predefinedFontClasses.exclAdd("sales.png", "fa fa-hand-holding-dollar");
        AppServerImage.predefinedFontClasses.exclAdd("manufacturing.png", "fa fa-industry");
        AppServerImage.predefinedFontClasses.exclAdd("retail.png", "fa fa-bag-shopping");
        AppServerImage.predefinedFontClasses.exclAdd("projectManagement.png", "fa fa-diagram-project");

        AppServerImage.predefinedFontClasses.exclAdd("logo.png", "lsfi lsfi-logo-horizontal");
    }

    private final static LRUSVSMap<String, AppServerImage> cachedImages = new LRUSVSMap<>(LRUUtil.G2);
    private final static LRUSVSMap<Pair<String, Float>, AppServerImage> cachedDefaultImages = new LRUSVSMap<>(LRUUtil.G2);
    private static final AppServerImage NULL = new AppServerImage();

    public AppServerImage() {
    }

    public String imagePath;
    public String fontClasses;

    public AppServerImage(String imagePath, String fontClasses) {
        this.imagePath = imagePath;
        this.fontClasses = fontClasses;
    }

    private AppImage appImage;

    // should be cached, because it is used in data images, default images, and can be used several times
    public static AppServerImage createImage(String imagePath) {
        if(imagePath == null)
            return null;

        AppServerImage result = cachedImages.get(imagePath);
        if(result == null) { // caching here is needed to cache getAppImage
            result = calculateImage(imagePath);

            cachedImages.put(imagePath, result);
        }

        return result;
    }

    private static AppServerImage calculateImage(String imagePath) {
        if(imagePath.contains(";")) {
            String[] split = imagePath.split(";");
            return new AppServerImage(split[0], split[1]);
        } else {
            if(imagePath.contains("/") || imagePath.contains(".")) // it's a path
                return new AppServerImage(imagePath, null);
            else // it's an icon
                return new AppServerImage(null, imagePath);
        }
    }

    private final static ThreadLocal<MSet<Pair<String, Float>>> prereadDefault = new ThreadLocal<>();
    public static void prereadDefaultImages(BusinessLogics BL, DBManager dbManager, Runnable run) {
        prereadDefault.set(SetFact.mSet());
        try {
            run.run();

            MSet<Pair<String, Float>> mImages = prereadDefault.get();
            ImMap<Pair<String, Float>, AppServerImage> readImages = readDefaultImages(BL, dbManager, mImages.immutable());
            for(int i = 0, size = readImages.size(); i < size; i++)
                cachedDefaultImages.put(readImages.getKey(i), readImages.getValue(i));
        } finally {
            prereadDefault.set(null);
        }
    }

    private static ImMap<Pair<String, Float>, AppServerImage> readDefaultImages(BusinessLogics BL, DBManager dbManager, ImSet<Pair<String, Float>> cacheKeys) {
        IconLogicsModule iconLM = BL.iconLM;
        if(iconLM.bestIconNames == null)
            return cacheKeys.mapValues(() -> NULL);

        ImSet<String> names = cacheKeys.mapMergeSetValues(cacheKey -> cacheKey.first);

        try(DataSession session = dbManager.createSession()) {
            ExecutionEnvironment env = session;

            iconLM.bestIconNames.change(session, env, names.mapValues(() -> true), StringClass.instance, LogicalClass.instance);

            iconLM.getBestIcons.execute(env, ThreadLocalContext.getStack());

            ImMap<ImList<Object>, Object> bestIconRanks = iconLM.bestIconRanks.readAll(env);
            ImMap<ImList<Object>, Object> bestIconClasses = iconLM.bestIconClasses.readAll(env);

            return cacheKeys.mapValues((Pair<String, Float> key) -> {
                ImList<Object> nameKey = ListFact.singleton(key.first);
                Double bestIconRank = (Double) bestIconRanks.get(nameKey);
                return bestIconRank != null && bestIconRank > key.second ? new AppServerImage(null, (String) bestIconClasses.get(nameKey)) : NULL;
            });
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }

    // should be cached because it is used in default images
    private static AppServerImage createDefaultImage(String name, float rankingThreshold) {
        if(rankingThreshold >= 1.0f) // optimization
            return null;

        if(name == null)
            return null;

        Pair<String, Float> cacheKey = new Pair<>(name, rankingThreshold);
        AppServerImage result = cachedDefaultImages.get(cacheKey);
        if(result == null) {
            MSet<Pair<String, Float>> prereadSet = prereadDefault.get();
            if(prereadSet != null) {
                prereadSet.add(cacheKey);
                return null;
            }

            result = readDefaultImages(ThreadLocalContext.getBusinessLogics(), ThreadLocalContext.getDbManager(), SetFact.singleton(new Pair<>(name, rankingThreshold))).singleValue();

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

    public static AppServerImage createDefaultImage(float rankingThreshold, String name, Supplier<AppServerImage> defaultImage) {
        AppServerImage autoImage = createDefaultImage(name, rankingThreshold);
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

    public static AppServerImage createPropertyImage(String imagePath, ActionOrProperty property) {
        if(AUTO.equals(imagePath))
            return property.getDefaultImage(Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON);
            
        return createImage(imagePath);
    }

    public static AppServerImage createPropertyImage(String imagePath, PropertyDrawEntity property) {
        return createPropertyImage(imagePath, property.getInheritedProperty());
    }

    public static AppServerImage createPropertyImage(String imagePath, PropertyDrawView property) {
        return createPropertyImage(imagePath, property.entity);
    }

    public static AppServerImage createContainerImage(String imagePath, ContainerView container, FormView formView) {
        if(AUTO.equals(imagePath))
            return container.getDefaultImage(Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON, formView);

        return createImage(imagePath);
    }

    public static final String FORMTOP = "formTop.png";
    public static final String FORM = "form.png"; // also used in the default container icon
    public static final String ACTIONTOP = "actionTop.png";
    public static final String ACTION = "action.png"; // also used in the default action icon
    public static final String OPENTOP = "openTop.png";
    public static final String OPEN = "open.png";

    public static AppServerImage createNavigatorImage(String imagePath, NavigatorElement navigator) {
        if(AUTO.equals(imagePath))
            return navigator.getDefaultImage(Settings.get().getDefaultAutoImageRankingThreshold(), AUTO_ICON);

        return createImage(imagePath);
    }

    public static final String DIALOG = "dialog.png";
    public static final String RESET = "reset.png";

    public static AppServerImage createActionImage(String imagePath) {
        return createImage(imagePath);
    }

    @ManualLazy
    public AppImage getAppImage() throws IOException {
        if(appImage == null) {
            String imagePath = this.imagePath;
            String fontClasses = this.fontClasses;

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

            if(fontClasses == null)
                fontClasses = predefinedFontClasses.get(BaseUtils.getFileNameAndExtension(imagePath));

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
