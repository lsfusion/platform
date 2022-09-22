package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;

public class StaticImage extends BaseStaticImage {

    private static final boolean useFA = false;

    public String path;

    public StaticImage() {
    }

    private StaticImage(String path) {
        this(null, path);
    }

    private StaticImage(String fontClasses, String path) {
        super(fontClasses);
        this.path = path;
    }

    @Override
    public Element createImage() {
        return GwtClientUtils.createStaticImage(this);
    }

    public void setImageSrc(Element element, BaseStaticImage overrideImage) {
        GwtClientUtils.setStaticImageSrc(element, this, overrideImage);
    }

    public void setImageElementSrc(ImageElement imageElement, boolean enabled) {
        GwtClientUtils.setThemeImage(path, imageElement::setSrc);
    }

    public final static StaticImage RESET = new StaticImage("fa-solid fa-xmark", "reset.png");

    public final static StaticImage MINIMIZE = new StaticImage("fa-solid fa-compress", "minimize.png");
    public final static StaticImage MAXIMIZE = new StaticImage("fa-solid fa-expand", "maximize.png");

    public final static StaticImage EXPANDTREECURRENT = new StaticImage("bi bi-chevron-down", "expandTreeCurrent.png");
    public final static StaticImage COLLAPSETREECURRENT = new StaticImage("bi bi-chevron-up", "collapseTreeCurrent.png");
    public final static StaticImage EXPANDTREE = new StaticImage("bi bi-chevron-double-down", "expandTree.png");
    public final static StaticImage COLLAPSETREE = new StaticImage("bi bi-chevron-double-up", "collapseTree.png");

    public static final StaticImage LOADING_IMAGE_PATH = new StaticImage("fa-solid fa-spinner fa-spin",  "loading.gif");
    public static final StaticImage REFRESH_IMAGE_PATH = new StaticImage("bi bi-arrow-repeat", "refresh.png");

    public static final StaticImage OK = new StaticImage("bi bi-check", "ok.png");

    public static final StaticImage ADD_ICON_PATH = new StaticImage("fa-solid fa-plus", "filtadd.png");
    public static final StaticImage RESET_ICON_PATH = new StaticImage("fa-solid fa-xmark","filtreset.png");
    public static final StaticImage DELETE_ICON_PATH = new StaticImage("fa-solid fa-minus", "filtdel.png");
    public static final StaticImage SEPARATOR_ICON_PATH = new StaticImage(useFA ? "fa-solid fa-grip-lines-vertical" : "bi bi-pause", "filtseparator.png");

    public static final StaticImage GRID = new StaticImage(useFA ? "fa-solid fa-list-ul" : "bi bi-list-ul","grid.png");
    public static final StaticImage PIVOT = new StaticImage(useFA ? "fa-solid fa-table-cells-large" : "bi bi-grid-3x2", "pivot.png");
    public static final StaticImage CUSTOMVIEW = new StaticImage(useFA ? "fa-solid fa-laptop" : "bi bi-c-square", "custom_view.png");
    public static final StaticImage MAP = new StaticImage(useFA ? "fa-regular fa-map" : "bi bi-geo-alt", "map.png");
    public static final StaticImage CALENDAR = new StaticImage(useFA ? "fa-regular fa-calendar" : "bi bi-calendar4-week", "calendar_view.png");

    public static final StaticImage FILTER_ICON_PATH = new StaticImage("bi bi-funnel", "filt.png");

    public static final StaticImage USERPREFERENCES = new StaticImage(useFA ? "fa-solid fa-gear" : "bi bi-gear", "userPreferences.png");
    public static final StaticImage EXCELBW = new StaticImage(useFA ? "fa-regular fa-share-from-square" : "bi bi-download", "excelbw.png");
    public static final StaticImage UPDATE = new StaticImage(useFA ? "fa-solid fa-retweet" : "bi bi-repeat", "update.png");

    public static final StaticImage SUM = new StaticImage(useFA ? "fa-regular fa-square-plus" : "bi bi-plus-lg", "sum.png");
    public static final StaticImage QUANTITY = new StaticImage(useFA ? "fa-solid fa-list-ol" : "bi bi-123", "quantity.png");

    public static final StaticImage HAMBURGER = new StaticImage("fa-solid fa-bars", "hamburger.png");

    public static final StaticImage LOADING_BAR_GIF = new StaticImage("loading_bar.gif");
    public static final StaticImage LOADING_BAR = new StaticImage("loading_bar.png");
    public static final StaticImage LOADING_ASYNC = new StaticImage("fa-solid fa-spinner fa-spin", "loading_async.gif");

    public static final StaticImage SORTUP = new StaticImage("fa-solid fa-arrow-up-short-wide", "arrowup.png");
    public static final StaticImage SORTDOWN = new StaticImage("fa-solid fa-arrow-down-short-wide", "arrowdown.png");

    public static final StaticImage EXECUTE = new StaticImage("bi bi-play", "action.png");
    public static final StaticImage EMPTY = new StaticImage("empty.png");
    public static final StaticImage FILE = new StaticImage("bi bi-file-earmark", "file.png");
}
