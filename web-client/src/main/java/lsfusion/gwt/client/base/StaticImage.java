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

    public final static StaticImage EXPANDTREECURRENT = new StaticImage("expandTreeCurrent.png");
    public final static StaticImage COLLAPSETREECURRENT = new StaticImage("collapseTreeCurrent.png");
    public final static StaticImage EXPANDTREE = new StaticImage("expandTree.png");
    public final static StaticImage COLLAPSETREE = new StaticImage("collapseTree.png");

    public static final StaticImage LOADING_IMAGE_PATH = new StaticImage("fa-solid fa-spinner fa-spin",  "loading.gif");
    public static final StaticImage REFRESH_IMAGE_PATH = new StaticImage("refresh.png");

    public static final StaticImage OK = new StaticImage("ok.png");

    public static final StaticImage ADD_ICON_PATH = new StaticImage("fa-solid fa-plus", "filtadd.png");
    public static final StaticImage RESET_ICON_PATH = new StaticImage("filtreset.png");
    public static final StaticImage FILTER_ICON_PATH = new StaticImage("filt.png");
    public static final StaticImage DELETE_ICON_PATH = new StaticImage("filtdel.png");
    public static final StaticImage SEPARATOR_ICON_PATH = new StaticImage("filtseparator.png");

    public static final StaticImage GRID = new StaticImage(useFA ? "fa-solid fa-list-ul" : "bi bi-list-ul","grid.png");
    public static final StaticImage PIVOT = new StaticImage(useFA ? "fa-solid fa-table-cells" : "bi bi-table", "pivot.png");
    public static final StaticImage CUSTOMVIEW = new StaticImage("custom_view.png");
    public static final StaticImage MAP = new StaticImage("map.png");
    public static final StaticImage CALENDAR = new StaticImage("calendar_view.png");

    public static final StaticImage USERPREFERENCES = new StaticImage("userPreferences.png");
    public static final StaticImage HAMBURGER = new StaticImage("hamburger.png");
    public static final StaticImage EXCELBW = new StaticImage("excelbw.png");
    public static final StaticImage UPDATE = new StaticImage("update.png");

    public static final StaticImage SUM = new StaticImage("sum.png");
    public static final StaticImage QUANTITY = new StaticImage("quantity.png");

    public static final StaticImage LOADING_BAR_GIF = new StaticImage("loading_bar.gif");
    public static final StaticImage LOADING_BAR = new StaticImage("loading_bar.png");
    public static final StaticImage LOADING_ASYNC = new StaticImage("loading_async.gif");

    public static final StaticImage ARROWUP = new StaticImage("arrowup.png");
    public static final StaticImage ARROWDOWN = new StaticImage("arrowdown.png");

    public static final StaticImage EXECUTE = new StaticImage("action.png");
    public static final StaticImage EMPTY = new StaticImage("empty.png");
    public static final StaticImage FILE = new StaticImage("file.png");
}
