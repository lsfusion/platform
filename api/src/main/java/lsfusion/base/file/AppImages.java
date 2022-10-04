package lsfusion.base.file;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;

// constants

public class AppImages {

    private static final boolean useFA = false;
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
        AppImages.predefinedFontClasses.exclAdd("lock.png", "fa-solid fa-lock");
        AppImages.predefinedFontClasses.exclAdd("search.png", "fa-solid fa-magnifying-glass");

        AppImages.predefinedFontClasses.exclAdd("relogin.png", "fa-solid fa-elevator");
        AppImages.predefinedFontClasses.exclAdd("editProfile.png", "fa-solid fa-user-pen");
        AppImages.predefinedFontClasses.exclAdd("changePassword.png", "fa-solid fa-key");
        AppImages.predefinedFontClasses.exclAdd("logout.png", "fa-solid fa-right-from-bracket");

        AppImages.predefinedFontClasses.exclAdd("chat.png", "fa-solid fa-comment");
        AppImages.predefinedFontClasses.exclAdd("play.png", "fa-solid fa-play");

        AppImages.predefinedFontClasses.exclAdd("catalog.png", "fa-solid fa-list-check");
    }

    public static final AppImage FORMTOP = new AppImage("formTop.png");
    public static final AppImage FORM = new AppImage("form.png");
    public static final AppImage ACTIONTOP = new AppImage("actionTop.png");
    public static final AppImage ACTION = new AppImage("action.png");
    public static final AppImage OPENTOP = new AppImage("openTop.png");
    public static final AppImage OPEN = new AppImage("open.png");

    public static final AppImage ADD = new AppImage("add.png");
    public static final AppImage EDIT = new AppImage( "edit.png");
    public static final AppImage DELETE = new AppImage( "delete.png");

    public static final AppImage EMAIL = new AppImage( "email.png");

    public static final AppImage DIALOG = new AppImage( "dialog.png");
    public static final AppImage RESET = new AppImage( "reset.png");
}
