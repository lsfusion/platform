package lsfusion.server.base;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.base.file.AppImage;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.util.function.Supplier;

// constants

public class AppImages {

    public static final MAddExclMap<String, String> predefinedFontClasses = MapFact.mAddExclMap();

    static {
        AppImages.predefinedFontClasses.exclAdd("apply.png", "bi bi-save");
        AppImages.predefinedFontClasses.exclAdd("cancel.png", "bi bi-archive");
        AppImages.predefinedFontClasses.exclAdd("ok.png", "bi bi-check");
        AppImages.predefinedFontClasses.exclAdd("close.png", "bi bi-x");
        AppImages.predefinedFontClasses.exclAdd("editReport.png", "bi bi-pencil-square");
        AppImages.predefinedFontClasses.exclAdd("refresh.png", "bi bi-arrow-repeat");

        AppImages.predefinedFontClasses.exclAdd("add.png", "fa-solid fa-plus");
        AppImages.predefinedFontClasses.exclAdd("edit.png", "fa-solid fa-pen");
        AppImages.predefinedFontClasses.exclAdd("delete.png", "fa-solid fa-minus");

        AppImages.predefinedFontClasses.exclAdd("email.png", "fa-regular fa-envelope");

        AppImages.predefinedFontClasses.exclAdd("dialog.png", "fa-solid fa-ellipsis");
        AppImages.predefinedFontClasses.exclAdd("reset.png", "fa-solid fa-xmark");

        AppImages.predefinedFontClasses.exclAdd("print.png", "bi bi-printer");
        AppImages.predefinedFontClasses.exclAdd("form.png", "fa-regular fa-clone");
        AppImages.predefinedFontClasses.exclAdd("formTop.png", "fa-regular fa-clone");
        AppImages.predefinedFontClasses.exclAdd("action.png", "fa-regular fa-square-caret-right");
        AppImages.predefinedFontClasses.exclAdd("actionTop.png", "fa-regular fa-square-caret-right");
        AppImages.predefinedFontClasses.exclAdd("open.png", "fa-regular fa-folder");

        AppImages.predefinedFontClasses.exclAdd("tools.png", "fa-solid fa-screwdriver-wrench");
        AppImages.predefinedFontClasses.exclAdd("lock.png", "fa-solid bi-person-fill");
        AppImages.predefinedFontClasses.exclAdd("search.png", "fa-solid fa-magnifying-glass");

        AppImages.predefinedFontClasses.exclAdd("relogin.png", "fa-solid fa-elevator");
        AppImages.predefinedFontClasses.exclAdd("editProfile.png", "fa-solid fa-user-pen");
        AppImages.predefinedFontClasses.exclAdd("changePassword.png", "fa-solid fa-key");
        AppImages.predefinedFontClasses.exclAdd("logout.png", "fa-solid fa-right-from-bracket");

        AppImages.predefinedFontClasses.exclAdd("chat.png", "fa-solid fa-comment");
        AppImages.predefinedFontClasses.exclAdd("play.png", "fa-solid fa-play");

        AppImages.predefinedFontClasses.exclAdd("bootstrap.png", "fa-brands fa-bootstrap");
        AppImages.predefinedFontClasses.exclAdd("excel.png", "fa-solid fa-file-excel");

        AppImages.predefinedFontClasses.exclAdd("lightMode.png", "fa-solid fa-sun");
        AppImages.predefinedFontClasses.exclAdd("darkMode.png", "fa-solid fa-moon");

        AppImages.predefinedFontClasses.exclAdd("catalog.png", "fa-solid fa-list-check");

        AppImages.predefinedFontClasses.exclAdd("inventory.png", "fa-solid fa-warehouse");
        AppImages.predefinedFontClasses.exclAdd("invoicing.png", "fa-solid fa-money-bill");
        AppImages.predefinedFontClasses.exclAdd("purchase.png", "fa-solid fa-cart-flatbed");
        AppImages.predefinedFontClasses.exclAdd("sales.png", "fa-solid fa-hand-holding-dollar");
        AppImages.predefinedFontClasses.exclAdd("manufacturing.png", "fa-solid fa-industry");
        AppImages.predefinedFontClasses.exclAdd("retail.png", "fa-solid fa-bag-shopping");
        AppImages.predefinedFontClasses.exclAdd("projectManagement.png", "fa-solid fa-diagram-project");

        AppImages.predefinedFontClasses.exclAdd("logo.png", "lsfi lsfi-logo-horizontal");
    }

    private final static LRUSVSMap<String, AppImage> cachedImages = new LRUSVSMap<>(LRUUtil.G2);
    private final static LRUSVSMap<Pair<String, Float>, AppImage> cachedDefaultImages = new LRUSVSMap<>(LRUUtil.G2);


    // should be cached, because it is used in data images, default images, and can be used several times
    public static AppImage createImage(String imagePath) {
        if(imagePath == null)
            return null;

        AppImage result = cachedImages.get(imagePath);
        if(result == null) {
            result = new AppImage(imagePath, predefinedFontClasses.get(BaseUtils.getFileNameAndExtension(imagePath)));

            cachedImages.put(imagePath, result);
        }

        return result;
    }

    private static final AppImage NULL = new AppImage();
    // should be cached because it is used in default images
    private static AppImage createDefaultImage(String name, float rankingThreshold) {
        if(rankingThreshold >= 1.0f) // optimization
            return null;

        if(name == null)
            return null;

        Pair<String, Float> cacheKey = new Pair<>(name, rankingThreshold);
        AppImage result = cachedDefaultImages.get(cacheKey);
        if(result == null) {
            // turn name into a phrase
            // and then search lsf by calling the icon with the maximum search for his word in this phrase (taking into account morphology, stop words and synonyms)
            result = NULL; // temporary fix

            cachedDefaultImages.put(cacheKey, result);
        }
        return result == NULL ? null : result;
    }

    public static AppImage createDefaultImage(float rankingThreshold, String name, Supplier<AppImage> defaultImage) {
        AppImage autoImage = createDefaultImage(name, rankingThreshold);
        if(autoImage != null)
            return autoImage;

        return defaultImage.get();
    }

    public static final String ADD = "add.png"; // also is used in the input action
    public static final String EDIT =  "edit.png";
    public static final String DELETE = "delete.png";

    public static final String EMAIL = "email.png";

    public static AppImage createPropertyImage(String imagePath, ActionOrProperty property) {
        return createImage(imagePath);
    }

    public static AppImage createPropertyImage(String imagePath, PropertyDrawEntity property) {
        return createPropertyImage(imagePath, property.getInheritedProperty());
    }

    public static AppImage createPropertyImage(String imagePath, PropertyDrawView property) {
        return createPropertyImage(imagePath, property.entity);
    }

    public static AppImage createContainerImage(String imagePath, ContainerView container, FormView formView) {
        return createImage(imagePath);
    }

    public static final String FORMTOP = "formTop.png";
    public static final String FORM = "form.png"; // also used in the default container icon
    public static final String ACTIONTOP = "actionTop.png";
    public static final String ACTION = "action.png"; // also used in the default action icon
    public static final String OPENTOP = "openTop.png";
    public static final String OPEN = "open.png";

    public static AppImage createNavigatorImage(String imagePath, NavigatorElement navigator) {
        return createImage(imagePath);
    }

    public static final String DIALOG = "dialog.png";
    public static final String RESET = "reset.png";

    public static AppImage createActionImage(String imagePath) {
        return createImage(imagePath);
    }
}
