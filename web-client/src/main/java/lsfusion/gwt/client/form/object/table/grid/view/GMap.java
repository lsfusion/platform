package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.user.client.ui.RequiresResize;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGFEColorMatrixElement;
import org.vectomatic.dom.svg.OMSVGFilterElement;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.utils.OMSVGParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.Math.pow;
import static lsfusion.gwt.client.base.view.ColorUtils.correctSB;
import static lsfusion.gwt.client.base.view.ColorUtils.mixColors;
import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.COLUMN_ATTRIBUTE;
import static lsfusion.gwt.client.view.StyleDefaults.getFocusColor;
import static lsfusion.gwt.client.view.StyleDefaults.getFocusedCellBackgroundColor;

public class GMap extends GSimpleStateTableView<JavaScriptObject> implements RequiresResize {
    // No need to support color themes here as we apply svg filters to the icon anyway.
    private final String DEFAULT_MARKER_ICON_URL = GwtClientUtils.getModuleImagePath("map_marker.png");

    public GMap(GFormController form, GGridController grid) {
        super(form, grid);
    }

    private static class GroupMarker {

        public final String name;
        public String color;
        public final Object line;
        public final String icon;

        // should be polymorphed later
        public final Double latitude;
        public final Double longitude;
        public final String polygon;

        public boolean isReadOnly;

        public GroupMarker(JavaScriptObject object) {
            name = getName(object);
            color = getMarkerColor(object);
            line = getLine(object);
            icon = getIcon(object);

            latitude = getLatitude(object);
            longitude = getLongitude(object);
            polygon = getPolygon(object);
        }
    }

    protected void changePointProperty(JavaScriptObject object, Double lat, Double lng) {
        changeProperty("latitude", lat, object);
        changeProperty("longitude", lng, object);
    }

    protected void changePolygonProperty(JavaScriptObject object, JsArray<WrapperObject> latlngs) {
        changeProperty("polygon", getPolygon(latlngs), object);
    }

    private static String getPolygon(JsArray<WrapperObject> latlngs) {
        String result = "";
        for(int i=0,size=latlngs.length();i<size;i++) {
            WrapperObject pointObject = latlngs.get(i);
            result = (result.isEmpty() ? "" : result + ",") + (pointObject.getValue("lat") + " " + pointObject.getValue("lng"));
        }
        return result;
    }

    private static double safeParse(String[] array, int index) {
        if(index >= array.length)
            return 0;

        try {
            return Double.parseDouble(array[index]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static JsArray<JavaScriptObject> getLatLngs(String polygon) {
        if(polygon == null)
            return null;

        JsArray<JavaScriptObject> result = JavaScriptObject.createArray().cast();
        for(String pointString : polygon.split(",")) {
            String[] pointArray = pointString.trim().split(" ");
            result.push(getLatLng(safeParse(pointArray, 0), safeParse(pointArray, 1)));
        }
        return result;
    }

    private JavaScriptObject map;
    private JavaScriptObject markerClusters;
    private Map<GGroupObjectValue, JavaScriptObject> markers = new HashMap<>();
    private Map<GGroupObjectValue, GroupMarker> groupMarkers = new HashMap<>();
    private ArrayList<JavaScriptObject> lines = new ArrayList<>(); // later also should be
    @Override
    protected void render(Element renderElement, JsArray<JavaScriptObject> listObjects) {
        if(map == null) {
            markerClusters = createMarkerClusters();
            map = createMap(renderElement, markerClusters);
        }

        Map<Object, JsArray<JavaScriptObject>> routes = new HashMap<>();

        boolean markerCreated = false;
        Map<GGroupObjectValue, JavaScriptObject> oldMarkers = new HashMap<>(markers);
        for(int i=0,size=listObjects.length();i<size;i++) {
            JavaScriptObject object = listObjects.get(i);
            GGroupObjectValue key = getKey(object);

            GroupMarker groupMarker = new GroupMarker(object);
            if (groupMarker.color == null) {
                Object rowBackgroundColor = getRowBackgroundColor(getKey(object));
                if (rowBackgroundColor != null) {
                    groupMarker.color = rowBackgroundColor.toString();
                }
            }
            groupMarker.isReadOnly = getReadOnly(key, groupMarker);

            GroupMarker oldGroupMarker = groupMarkers.put(key, groupMarker);

            JavaScriptObject marker = oldMarkers.remove(key);
            if(marker == null) {
                marker = createMarker(map, groupMarker.polygon != null, fromObject(key), markerClusters);
                markers.put(key, marker);
                markerCreated = true;
            }

            boolean isPoly = groupMarker.polygon != null;

            if(oldGroupMarker != null && !oldGroupMarker.isReadOnly)
                disableEditing(marker, isPoly);

            if(oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.latitude, oldGroupMarker.latitude) && GwtClientUtils.nullEquals(groupMarker.longitude, oldGroupMarker.longitude) && GwtClientUtils.nullEquals(groupMarker.polygon, oldGroupMarker.polygon)))
                updateLatLng(marker, groupMarker.latitude, groupMarker.longitude, getLatLngs(groupMarker.polygon));

            if(!isPoly && (oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.icon, oldGroupMarker.icon) && GwtClientUtils.nullEquals(groupMarker.color, oldGroupMarker.color)))) {
                updateIcon(marker, groupMarker.icon, getDisplayColorFilter(key));
            }

            if(oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.color, oldGroupMarker.color)))
                updateColor(marker, groupMarker.color, getDisplayClusterColor(groupMarker.color));

            if(!groupMarker.isReadOnly)
                updateEditing(marker, isPoly);

            if (oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.name, oldGroupMarker.name))) {
                updateName(marker, groupMarker.name);
            }

            if(groupMarker.line != null)
                routes.computeIfAbsent(groupMarker.line, o -> JavaScriptObject.createArray().cast()).push(marker);
        }
        for(Map.Entry<GGroupObjectValue, JavaScriptObject> oldMarker : oldMarkers.entrySet()) {
            removeMarker(oldMarker.getValue(), markerClusters);
            markers.remove(oldMarker.getKey());
            groupMarkers.remove(oldMarker.getKey());
        }

        for(JavaScriptObject line : lines)
            removeLine(map, line);
        lines.clear();
        for(JsArray<JavaScriptObject> route : routes.values())
            if(route.length() > 1)
                lines.add(createLine(map, route));

        if(markerCreated && !markers.isEmpty())
            Scheduler.get().scheduleDeferred(() -> fitBounds(map, GwtSharedUtils.toArray(markers.values())));
    }

    private boolean getReadOnly(GGroupObjectValue key, GroupMarker groupMarker) {
        if(Objects.equals(key, getCurrentKey())) {
            if (groupMarker.polygon != null)
                return isReadOnly("polygon", key);
            else
                return isReadOnly("latitude", key) && isReadOnly("longitude", key);
        }
        return true;
    }

    protected native JavaScriptObject createMap(com.google.gwt.dom.client.Element element, JavaScriptObject markerClusters)/*-{
        var L = $wnd.L;
        var map = L.map(element);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        map.setView([0,0], 6); // we need to set view to have editing and dragging fields initialized

        map.addLayer(markerClusters);

        return map;
    }-*/;

    protected native JavaScriptObject createMarkerClusters()/*-{
        var L = $wnd.L;
        var browser = navigator.userAgent.toLowerCase();
        if (browser.indexOf('firefox') > -1) {
            return L.markerClusterGroup();
        }
        return L.markerClusterGroup({
            iconCreateFunction: function (cluster) {
                var colors = [];
                cluster.getAllChildMarkers().forEach(function (marker) {
                    colors.push(marker.clusterColor);
                });
                var colorsSetArray = Array.from(new Set(colors));

                var backgroundColor = ""
                if (colorsSetArray.length === 1) {
                    backgroundColor = colorsSetArray[0];
                } else {
                    backgroundColor = "conic-gradient("
                    var prevPercent = 0;
                    colorsSetArray.forEach(function (color) {
                        backgroundColor += color;
                        if (colorsSetArray.indexOf(color) !== 0) {
                            backgroundColor += " 0"
                        }

                        if (colorsSetArray.indexOf(color) < colorsSetArray.length - 1) {
                            var newPercent = prevPercent + (colors.filter(function (c) {
                                return c === color;
                            }).length / colors.length * 100);
                            backgroundColor += " " + newPercent + "%,";
                            prevPercent = newPercent;
                        }
                    });
                    backgroundColor += ")"
                }

                return L.divIcon({
                    html: "<div class=\"leaflet-marker-cluster\" style=\"background:" + backgroundColor + ";\">" +
                        "<div class=\"leaflet-marker-cluster-text\">" + cluster.getChildCount() + "</div>" +
                        "</div>",
                    className: '' // removing leaflet-div-icon because we don't want it as white box
                });
            }
        });
    }-*/;

    protected static native JavaScriptObject getLatLng(Double latitude, Double longitude)/*-{
        var L = $wnd.L;
        return L.latLng(latitude, longitude);
    }-*/;

    protected JavaScriptObject showPopup(JavaScriptObject popupElementClicked, Element popupElement) {
        return showMapPopup(popupElementClicked, popupElement);
    }

    protected void hidePopup(JavaScriptObject popup) {
        hideMapPopup(popup);
    }

    protected native JavaScriptObject showMapPopup(JavaScriptObject marker, Element popupElement)/*-{
        marker.bindPopup(popupElement).openPopup();
        return marker;
    }-*/;

    protected native void hideMapPopup(JavaScriptObject marker)/*-{
        if(marker.isPopupOpen())
            marker.closePopup();
        marker.unbindPopup();
    }-*/;


    protected native JavaScriptObject createMarker(JavaScriptObject map, boolean polygon, JavaScriptObject key, JavaScriptObject markerClusters)/*-{
        var L = $wnd.L;

        var thisObject = this;

        var marker;
        if(polygon) {
            marker = L.polygon([L.latLng(0, 1), L.latLng(1, -1), L.latLng(-1, -1)]);

            marker.on('edit', function (e) {
                thisObject.@GMap::changePolygonProperty(*)(key, marker.getLatLngs()[0]); // https://github.com/Leaflet/Leaflet/issues/5212
            });
        } else {
            marker = L.marker([0, 0]);

            var superDragEnd = marker.editing._onDragEnd; // there is a bug with clustering, when you drag marker to cluster, nullpointer happens
            marker.editing._onDragEnd = function(t) {
                if(this._map != null)
                    superDragEnd.call(this, t);
            };

            marker.on('dragend', function (e) {
                var latlng = marker.getLatLng();
                thisObject.@GMap::changePointProperty(*)(key, latlng.lat, latlng.lng);
            });
        }

        marker.on('click', function (e) {
            var oldKey = thisObject.@GMap::getCurrentKey()();

            thisObject.@GMap::changeSimpleGroupObject(*)(key, true, marker); // we want "full rerender", at least for now

            if (!@Objects::equals(Ljava/lang/Object;Ljava/lang/Object;)(oldKey, key)) {
                thisObject.@GMap::updateIconFilter(*)(oldKey);
                thisObject.@GMap::updateIconFilter(*)(key);
            }

        });

//        marker = marker.addTo(map);
        markerClusters.addLayer(marker);
        
        return marker;
    }-*/;
    
    protected void updateIconFilter(JavaScriptObject keyObject) {
        GGroupObjectValue key = toObject(keyObject);
        updateIcon(markers.get(key), groupMarkers.get(key).icon, getDisplayColorFilter(key));
    }

    protected String getDisplayColorFilter(GGroupObjectValue key) {
        GroupMarker groupMarker = groupMarkers.get(key);
        String color = groupMarker.color;
        
        if (color == null) {
            if (groupMarker.icon == null) {
                color = getFocusColor(true);
                if (isCurrentKey(key)) {
                    color = darken(mixColors(color, getFocusedCellBackgroundColor(true)));
                }
            } else if (isCurrentKey(key)) {
                color = darken(getFocusedCellBackgroundColor(true));
            }
        } else {
            if (isCurrentKey(key)) {
                color = darken(mixColors(color, getFocusedCellBackgroundColor(true)), 2);
            } else {
                color = darken(color);
            }
        }
        
        return createFilter(color);
    }

    protected String getDisplayClusterColor(String color) {
        return color == null ? getFocusColor(false) : darken(color);
    }

    protected String darken(String color) {
        return darken(color, 1);
    }
    
    protected String darken(String color, int times) {
        if (color != null) {
            return correctSB(color, (float) pow(1.8f, times), (float) pow(0.8f, times));
        }
        return null;
    }

    protected native void removeMarker(JavaScriptObject marker, JavaScriptObject markerClusters)/*-{
        markerClusters.removeLayer(marker);
//        marker.remove();
    }-*/;

    protected native void appendSVG(JavaScriptObject map, com.google.gwt.dom.client.Element svg)/*-{
        map._container.appendChild(svg)
    }-*/;

    protected native static String getMarkerColor(JavaScriptObject element)/*-{
        return element.color;
    }-*/;

    protected native static Object getLine(JavaScriptObject element)/*-{
        return element.line;
    }-*/;

    protected native static Double getLatitude(JavaScriptObject element)/*-{
        return element.latitude;
    }-*/;

    protected native static Double getLongitude(JavaScriptObject element)/*-{
        return element.longitude;
    }-*/;

    protected native static String getPolygon(JavaScriptObject element)/*-{
        return element.polygon;
    }-*/;

    protected static native String getIcon(JavaScriptObject element)/*-{
        return element.icon;
    }-*/;

    protected native static String getName(JavaScriptObject element)/*-{
        return element.name.toString();
    }-*/;

    protected native JavaScriptObject createLine(JavaScriptObject map, JsArray<JavaScriptObject> markers)/*-{
        var L = $wnd.L;

        var points = [];
        markers.forEach(function (marker) {
            var latlng = marker.getLatLng();
            points.push([latlng.lat, latlng.lng]);
        });
        var line = L.polyline(points).addTo(map);
        var lineArrow = L.polylineDecorator(line, {
            patterns: [
                { offset: '100%', repeat: 0, symbol: L.Symbol.arrowHead({pathOptions: {stroke: true}}) }
            ]
        }).addTo(map);
        return {line : line, lineArrow : lineArrow};
    }-*/;

    protected native void removeLine(JavaScriptObject map, JavaScriptObject line)/*-{
        map.removeLayer(line.line);
        map.removeLayer(line.lineArrow);
    }-*/;

    protected String createFilter(String colorStr) {
        String svgStyle = null;
        if (colorStr != null) {
            int red = Integer.valueOf(colorStr.substring(1, 3), 16);
            int green = Integer.valueOf(colorStr.substring(3, 5), 16);
            int blue = Integer.valueOf(colorStr.substring(5, 7), 16);
            String svgID = "svg_" + red + "_" + green + "_" + blue;
            svgStyle = svgID + "-style";

            com.google.gwt.dom.client.Element svgEl = Document.get().getElementById(svgID);
            if (svgEl == null) {
                OMSVGDocument doc = OMSVGParser.currentDocument();

                OMSVGSVGElement svgElement = doc.createSVGSVGElement();
                OMSVGFilterElement svgFilterElement = doc.createSVGFilterElement();
                svgFilterElement.setId(svgID);
                svgFilterElement.setAttribute("color-interpolation-filters", "sRGB");

                OMSVGFEColorMatrixElement svgfeColorMatrixElement = doc.createSVGFEColorMatrixElement();
                svgfeColorMatrixElement.setAttribute("type", "matrix");
                svgfeColorMatrixElement.setAttribute("values", (float) red / 256 + " 0 0 0  0 \n" +
                        (float) green / 256 + " 0 0 0  0  \n" +
                        (float) blue / 256 + " 0 0 0  0 \n" +
                        "0 0 0 1  0");
                svgFilterElement.appendChild(svgfeColorMatrixElement);
                svgElement.appendChild(svgFilterElement);

                appendSVG(map, svgElement.getElement());

                StyleElement styleElement = Document.get().createStyleElement();
                styleElement.setType("text/css");
                styleElement.setInnerHTML("." + svgStyle + " { filter: url(#" + svgID + ") }");
                Document.get().getElementsByTagName("head").getItem(0).appendChild(styleElement);
            }
        }
        return svgStyle;
    }

    // we need to disable editing before changing icon + check for dragging because of clustering (when there is no dragging which is used by editing)
    protected native void disableEditing(JavaScriptObject marker, boolean poly)/*-{
        if(poly || marker.dragging != null)
            marker.editing.disable();
    }-*/;

    protected native void updateLatLng(JavaScriptObject marker, Double latitude, Double longitude, JsArray<JavaScriptObject> poly)/*-{
        if(poly != null)
            marker.setLatLngs(poly);
        else
            marker.setLatLng([latitude != null ? latitude : 0, longitude != null ? longitude : 0]);
    }-*/;

    protected Element getCellParent(Element target) {
        return GwtClientUtils.getParentWithAttribute(target, COLUMN_ATTRIBUTE);
    }

    protected native void updateIcon(JavaScriptObject marker, String icon, String filterStyle)/*-{
        var L = $wnd.L;
        var iconUrl = icon != null ? icon : this.@GMap::DEFAULT_MARKER_ICON_URL;
        if (iconUrl == null) {
            iconUrl = L.Icon.Default.prototype._getIconUrl('icon'); 
        }
        var myIcon = L.divIcon({
            html: "<img class=\"" + (filterStyle ? filterStyle : "") + "\" src=" + iconUrl + " alt=\"\" tabindex=\"0\" " +
                @lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder::COLUMN_ATTRIBUTE + "=\"true\" " +
                @lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder::IGNORE_DBLCLICK_CHECK + "=\"true\">",
            className: ''
        });
        marker.setIcon(myIcon);
    }-*/;

    protected native void updateColor(JavaScriptObject marker, String color, String clusterColor)/*-{
        marker.color = color;
        marker.clusterColor = clusterColor;
    }-*/;

    protected native void updateEditing(JavaScriptObject marker, boolean poly)/*-{
        if (poly || marker.dragging != null) {
            if (poly) {
                var L = $wnd.L;
                marker.editing = new L.Edit.Poly(marker); // there is a bug in plugin (with editing after setLatLngs) https://github.com/Leaflet/Leaflet.draw/issues/650
            }
            marker.editing.enable()
        }
    }-*/;

    protected native void updateName(JavaScriptObject marker, String name)/*-{
        var tooltip = marker.getTooltip();
        if (tooltip == null && name != null)
            marker.bindTooltip(name, {
                permanent: true,
                offset: new $wnd.L.Point(0, 10),
                direction: 'bottom'
            });
        if (tooltip != null && name == null)
            marker.unbindTooltip();
        if (tooltip != null && name != null)
            tooltip.setContent(name);
    }-*/;

    protected native void fitBounds(JavaScriptObject map, JsArray<JavaScriptObject> markers)/*-{
        var L = $wnd.L;

        map.fitBounds(new L.featureGroup(markers).getBounds());
    }-*/;

    @Override
    public void onResize() {
        resized(map);
    }

    protected native void resized(JavaScriptObject map)/*-{
        if (map) {
            map._onResize();
        }
    }-*/;
}
