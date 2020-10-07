package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RequiresResize;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGFEColorMatrixElement;
import org.vectomatic.dom.svg.OMSVGFilterElement;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.utils.OMSVGParser;

import java.util.*;

public class GMap extends GSimpleStateTableView implements RequiresResize {

    public GMap(GFormController form, GGridController grid) {
        super(form, grid);
    }

    private static class GroupMarker {

        public final String color;
        public final Object line;
        public final String icon;

        // should be polymorphed later
        public final Double latitude;
        public final Double longitude;
        public final String polygon;

        private final boolean groupEquals;

        public final GGroupObjectValue key;

        public GroupMarker(JavaScriptObject object, GGroupObjectValue key) {
            color = getMarkerColor(object);
            line = getLine(object);
            icon = getIcon(object);

            latitude = getLatitude(object);
            longitude = getLongitude(object);
            polygon = getPolygon(object);

            Boolean ge = getGroupEquals(object);
            groupEquals = ge != null && ge;

            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            GroupMarker that = (GroupMarker) o;

            if(groupEquals && that.groupEquals)
                return Objects.equals(color, that.color) &&
                    Objects.equals(line, that.line) &&
                    Objects.equals(icon, that.icon) &&
                    Objects.equals(latitude, that.latitude) &&
                    Objects.equals(longitude, that.longitude) &&
                    Objects.equals(polygon, that.polygon);

            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            if(groupEquals)
                return Objects.hash(color, line, icon, latitude, longitude, polygon);

            return key.hashCode();
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
    private Map<GroupMarker, JavaScriptObject> markers = new HashMap<>();
    private ArrayList<JavaScriptObject> lines = new ArrayList<>();
    @Override
    protected void render(Element renderElement, com.google.gwt.dom.client.Element recordElement, JsArray<JavaScriptObject> listObjects) {
        if(map == null) {
            markerClusters = createMarkerClusters();
            map = createMap(renderElement, markerClusters);
        }

        Map<Object, JsArray<JavaScriptObject>> routes = new HashMap<>();

        Set<GroupMarker> groupMarkerSet = new HashSet<>();
        List<GroupMarker> groupMarkers = new ArrayList<>();
        for(int i=0,size=listObjects.length();i<size;i++) {
            JavaScriptObject object = listObjects.get(i);
            GroupMarker groupMarker = new GroupMarker(object, getKey(object));

            if(groupMarkerSet.add(groupMarker))
                groupMarkers.add(groupMarker);
        }

        boolean markerCreated = false;
        Map<GroupMarker, JavaScriptObject> oldMarkers = new HashMap<>(markers);
        for(int i=0,size=groupMarkers.size();i<size;i++) {
            GroupMarker groupMarker = groupMarkers.get(i);

            JavaScriptObject marker = oldMarkers.remove(groupMarker);
            if(marker == null) {
                marker = createMarker(map, recordElement, groupMarker.polygon != null, fromObject(groupMarker.key), markerClusters);
                markerCreated = true;
                markers.put(groupMarker, marker);
            }

            boolean isReadOnly = true;
            if(!groupMarker.groupEquals && Objects.equals(groupMarker.key, getCurrentKey())) {
                if (groupMarker.polygon != null)
                    isReadOnly = isReadOnly("polygon", groupMarker.key);
                else
                    isReadOnly = isReadOnly("latitude", groupMarker.key) && isReadOnly("longitude", groupMarker.key);
            }

            String filterStyle = createFilter(groupMarker.color);
            updateMarker(map, marker, groupMarker.latitude, groupMarker.longitude, getLatLngs(groupMarker.polygon), isReadOnly, groupMarker.icon, groupMarker.color, filterStyle);

            if(groupMarker.line != null)
                routes.computeIfAbsent(groupMarker.line, o -> JavaScriptObject.createArray().cast()).push(marker);
        }
        for(Map.Entry<GroupMarker, JavaScriptObject> oldMarker : oldMarkers.entrySet()) {
            removeMarker(oldMarker.getValue(), markerClusters);
            markers.remove(oldMarker.getKey());
        }

        for(JavaScriptObject line : lines)
            removeLine(map, line);
        lines.clear();
        for(JsArray<JavaScriptObject> route : routes.values())
            if(route.length() > 1)
                lines.add(createLine(map, route));

        if(markerCreated && !markers.isEmpty())
            fitBounds(map, GwtSharedUtils.toArray(markers.values()));
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
                    colors.push(marker.color ? marker.color : "var(--focus-color)");
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

    protected native JavaScriptObject createMarker(JavaScriptObject map, com.google.gwt.dom.client.Element popupElement, boolean polygon, JavaScriptObject key, JavaScriptObject markerClusters)/*-{
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

        if (popupElement !== null)
            marker.bindPopup(popupElement, {maxWidth: Number.MAX_SAFE_INTEGER});
        marker.on('click', function (e) {
            thisObject.@GMap::changeSimpleGroupObject(*)(key, true);
        });

//        marker = marker.addTo(map);
        markerClusters.addLayer(marker);
        
        return marker;
    }-*/;

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

    protected static native Boolean getGroupEquals(JavaScriptObject element)/*-{
        return element.groupEquals;
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
    
    protected native void updateMarker(JavaScriptObject map, JavaScriptObject marker, Double latitude, Double longitude, JsArray<JavaScriptObject> poly, boolean isReadOnly, String icon, String color, String filterStyle)/*-{
        var L = $wnd.L;

        if(poly != null || marker.dragging != null) // we need to disable editing before changing icon + check for dragging because of clustering (when there is no dragging which is used by editing)
            marker.editing.disable();

        if(poly != null) {
            marker.setLatLngs(poly);
        } else {
            marker.setLatLng([latitude != null ? latitude : 0, longitude != null ? longitude : 0]);

            var iconUrl = icon != null ? icon : L.Icon.Default.prototype._getIconUrl('icon');
            var myIcon = L.divIcon({
                html: "<img src=" + iconUrl + " alt=\"\" tabindex=\"0\">",
                className: filterStyle ? filterStyle : ''
            });
            marker.setIcon(myIcon);
        }

        if(!isReadOnly) {
            if (poly != null)
                marker.editing = new L.Edit.Poly(marker); // there is a bug in plugin (with editing after setLatLngs) https://github.com/Leaflet/Leaflet.draw/issues/650
            if (poly != null || marker.dragging != null)
                marker.editing.enable()
        }

        marker.color = color;
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
