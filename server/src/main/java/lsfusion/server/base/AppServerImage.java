package lsfusion.server.base;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.AppImage;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ServerSerializationPool;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AppServerImage {

    public static final MAddExclMap<String, String> predefinedFontClasses = MapFact.mAddExclMap();

    static {
        AppServerImage.predefinedFontClasses.exclAdd("apply.png", "bi bi-save");
        AppServerImage.predefinedFontClasses.exclAdd("cancel.png", "bi bi-archive");
        AppServerImage.predefinedFontClasses.exclAdd("ok.png", "bi bi-check");
        AppServerImage.predefinedFontClasses.exclAdd("close.png", "bi bi-x");
        AppServerImage.predefinedFontClasses.exclAdd("editReport.png", "bi bi-pencil-square");
        AppServerImage.predefinedFontClasses.exclAdd("refresh.png", "bi bi-arrow-repeat");

        AppServerImage.predefinedFontClasses.exclAdd("add.png", "fa-solid fa-plus");
        AppServerImage.predefinedFontClasses.exclAdd("edit.png", "fa-solid fa-pen");
        AppServerImage.predefinedFontClasses.exclAdd("delete.png", "fa-solid fa-minus");

        AppServerImage.predefinedFontClasses.exclAdd("email.png", "fa-regular fa-envelope");

        AppServerImage.predefinedFontClasses.exclAdd("dialog.png", "fa-solid fa-ellipsis");
        AppServerImage.predefinedFontClasses.exclAdd("reset.png", "fa-solid fa-xmark");

        AppServerImage.predefinedFontClasses.exclAdd("print.png", "bi bi-printer");
        AppServerImage.predefinedFontClasses.exclAdd("form.png", "fa-regular fa-clone");
        AppServerImage.predefinedFontClasses.exclAdd("formTop.png", "fa-regular fa-clone");
        AppServerImage.predefinedFontClasses.exclAdd("action.png", "fa-regular fa-square-caret-right");
        AppServerImage.predefinedFontClasses.exclAdd("actionTop.png", "fa-regular fa-square-caret-right");
        AppServerImage.predefinedFontClasses.exclAdd("open.png", "fa-regular fa-folder");

        AppServerImage.predefinedFontClasses.exclAdd("tools.png", "fa-solid fa-screwdriver-wrench");
        AppServerImage.predefinedFontClasses.exclAdd("lock.png", "fa-solid bi-person-fill");
        AppServerImage.predefinedFontClasses.exclAdd("search.png", "fa-solid fa-magnifying-glass");

        AppServerImage.predefinedFontClasses.exclAdd("relogin.png", "fa-solid fa-elevator");
        AppServerImage.predefinedFontClasses.exclAdd("editProfile.png", "fa-solid fa-user-pen");
        AppServerImage.predefinedFontClasses.exclAdd("changePassword.png", "fa-solid fa-key");
        AppServerImage.predefinedFontClasses.exclAdd("logout.png", "fa-solid fa-right-from-bracket");

        AppServerImage.predefinedFontClasses.exclAdd("chat.png", "fa-solid fa-comment");
        AppServerImage.predefinedFontClasses.exclAdd("play.png", "fa-solid fa-play");

        AppServerImage.predefinedFontClasses.exclAdd("bootstrap.png", "fa-brands fa-bootstrap");
        AppServerImage.predefinedFontClasses.exclAdd("excel.png", "fa-solid fa-file-excel");

        AppServerImage.predefinedFontClasses.exclAdd("lightMode.png", "fa-solid fa-sun");
        AppServerImage.predefinedFontClasses.exclAdd("darkMode.png", "fa-solid fa-moon");

        AppServerImage.predefinedFontClasses.exclAdd("catalog.png", "fa-solid fa-list-check");

        AppServerImage.predefinedFontClasses.exclAdd("inventory.png", "fa-solid fa-warehouse");
        AppServerImage.predefinedFontClasses.exclAdd("invoicing.png", "fa-solid fa-money-bill");
        AppServerImage.predefinedFontClasses.exclAdd("purchase.png", "fa-solid fa-cart-flatbed");
        AppServerImage.predefinedFontClasses.exclAdd("sales.png", "fa-solid fa-hand-holding-dollar");
        AppServerImage.predefinedFontClasses.exclAdd("manufacturing.png", "fa-solid fa-industry");
        AppServerImage.predefinedFontClasses.exclAdd("retail.png", "fa-solid fa-bag-shopping");
        AppServerImage.predefinedFontClasses.exclAdd("projectManagement.png", "fa-solid fa-diagram-project");

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

    // should be cached because it is used in default images
    private static AppServerImage createDefaultImage(String name, float rankingThreshold) {
        if(rankingThreshold >= 1.0f) // optimization
            return null;

        if(name == null)
            return null;

        Pair<String, Float> cacheKey = new Pair<>(name, rankingThreshold);
        AppServerImage result = cachedDefaultImages.get(cacheKey);
        if(result == null) {
            // turn name into a phrase
            // and then search lsf by calling the icon with the maximum search for his word in this phrase (taking into account morphology, stop words and synonyms)
            result = NULL; // temporary fix

            cachedDefaultImages.put(cacheKey, result);
        }
        return result == NULL ? null : result;
    }

    public static AppServerImage createDefaultImage(float rankingThreshold, String name, Supplier<AppServerImage> defaultImage) {
        AppServerImage autoImage = createDefaultImage(name, rankingThreshold);
        if(autoImage != null)
            return autoImage;

        return defaultImage.get();
    }

    public static final String ADD = "add.png"; // also is used in the input action
    public static final String EDIT =  "edit.png";
    public static final String DELETE = "delete.png";
    public static final String EMAIL = "email.png";

    public static AppServerImage createPropertyImage(String imagePath, ActionOrProperty property) {
        return createImage(imagePath);
    }

    public static AppServerImage createPropertyImage(String imagePath, PropertyDrawEntity property) {
        return createPropertyImage(imagePath, property.getInheritedProperty());
    }

    public static AppServerImage createPropertyImage(String imagePath, PropertyDrawView property) {
        return createPropertyImage(imagePath, property.entity);
    }

    public static AppServerImage createContainerImage(String imagePath, ContainerView container, FormView formView) {
        return createImage(imagePath);
    }

    public static final String FORMTOP = "formTop.png";
    public static final String FORM = "form.png"; // also used in the default container icon
    public static final String ACTIONTOP = "actionTop.png";
    public static final String ACTION = "action.png"; // also used in the default action icon
    public static final String OPENTOP = "openTop.png";
    public static final String OPEN = "open.png";

    public static AppServerImage createNavigatorImage(String imagePath, NavigatorElement navigator) {
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
