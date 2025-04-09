package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RequiresResize;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.COLUMN_ATTRIBUTE;

public class GMap extends GSimpleStateTableView<JavaScriptObject> implements RequiresResize {

    public GMap(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);
    }

    private static class GroupMarker {

        public final String name;
        public String color;
        public final Object line;
        public final String caption;
        public final BaseImage image;

        // should be polymorphed later
        public Double latitude;
        public Double longitude;
        public String polygon;

        public boolean isCurrent;
        public boolean isReadOnly;

        public boolean isEditing() {
            return isCurrent && !isReadOnly;
        }

        public GroupMarker(JavaScriptObject object) {
            name = getName(object);
            color = getMarkerColor(object);
            line = getLine(object);
            caption = getCaption(object, javaScriptObject -> null);
            image = getImage(object, () -> StaticImage.MARKER);

            latitude = getLatitude(object);
            longitude = getLongitude(object);
            polygon = getPolygon(object);
        }
    }

    protected void changePointProperty(JavaScriptObject object, Double lat, Double lng, GroupMarker groupMarker) {
        groupMarker.latitude = lat;
        groupMarker.longitude = lng;
        changeProperties(new String[]{"latitude", "longitude"}, new JavaScriptObject[]{object, object}, new PValue[]{PValue.getPValue(lat), PValue.getPValue(lng)});
    }

    protected void changePolygonProperty(JavaScriptObject object, JsArray<WrapperObject> latlngs, GroupMarker groupMarker) {
        String polygon = getPolygon(latlngs);
        groupMarker.polygon = polygon;
        changeProperty("polygon", object, PValue.getPValue(polygon));
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
    private JavaScriptObject mapOptions;
    private JavaScriptObject viewOptions;
    private JavaScriptObject markerClusterOptions;
    @Override
    protected void onUpdate(Element renderElement, JsArray<JavaScriptObject> listObjects) {
        if(map == null) {
            markerClusters = createMarkerClusters();
            map = initMap(renderElement);
        }

        updateMap(map, markerClusters, grid.getMapTileProvider(), getCustomOptions());

        Map<Object, JsArray<JavaScriptObject>> routes = new HashMap<>();

        boolean fitBounds = false;
        Map<GGroupObjectValue, JavaScriptObject> oldMarkers = new HashMap<>(markers);
        JsArray<JavaScriptObject> markersToRefresh = JavaScriptObject.createArray().cast();

        for(int i=0,size=listObjects.length();i<size;i++) {
            JavaScriptObject object = listObjects.get(i);
            GGroupObjectValue key = getObjects(object);

            GroupMarker groupMarker = new GroupMarker(object);
            if (groupMarker.color == null) {
                String rowBackgroundColor = getRowBackgroundColor(getObjects(object));
                if (rowBackgroundColor != null) {
                    groupMarker.color = rowBackgroundColor;
                }
            }
            groupMarker.isCurrent = isCurrentKey(key);
            groupMarker.isReadOnly = getReadOnly(key, groupMarker);

            GroupMarker oldGroupMarker = groupMarkers.put(key, groupMarker);

            JavaScriptObject marker = oldMarkers.remove(key);
            if(marker == null) {
                marker = createMarker(map, groupMarker.polygon != null, markerClusters, object);
                markers.put(key, marker);
                fitBounds = true;
            }
            setGroupMarker(marker, groupMarker); // we need to update model in the coordinates change

            boolean isPoly = groupMarker.polygon != null;

            if(oldGroupMarker != null && oldGroupMarker.isEditing())
                disableEditing(marker, isPoly);

            boolean refreshMarkers = false;

            if(oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.color, oldGroupMarker.color))) {
                updateColor(marker, groupMarker.color, groupMarker.color != null ? groupMarker.color : StyleDefaults.getComponentBackground());
                refreshMarkers = true;
            }

            if(oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.latitude, oldGroupMarker.latitude) && GwtClientUtils.nullEquals(groupMarker.longitude, oldGroupMarker.longitude) && GwtClientUtils.nullEquals(groupMarker.polygon, oldGroupMarker.polygon))) {
                updateLatLng(marker, groupMarker.latitude, groupMarker.longitude, getLatLngs(groupMarker.polygon));
                refreshMarkers = false; //false because "updateLatLng" implicitly makes refresh

                if (hasFitBoundsProperty(object))
                    fitBounds = true;
            }

            if (refreshMarkers)
                markersToRefresh.push(marker);

            if(!isPoly && (oldGroupMarker == null || !(
                    GwtClientUtils.nullEquals(groupMarker.image, oldGroupMarker.image) &&
                    GwtClientUtils.nullEquals(groupMarker.caption, oldGroupMarker.caption) &&
                    GwtClientUtils.nullEquals(groupMarker.color, oldGroupMarker.color) &&
                    groupMarker.isCurrent == oldGroupMarker.isCurrent)))
                updateIcon(groupMarker, marker);

            if(groupMarker.isEditing())
                enableEditing(marker, isPoly);

            if (oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.name, oldGroupMarker.name)))
                updateName(marker, groupMarker.name);

            if(groupMarker.line != null)
                routes.computeIfAbsent(groupMarker.line, o -> JavaScriptObject.createArray().cast()).push(marker);
        }

        refreshMarkerClusters(markerClusters, markersToRefresh);

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

        if(fitBounds && !markers.isEmpty())
            Scheduler.get().scheduleDeferred(() -> fitBounds(map, GwtSharedUtils.toArray(markers.values())));
    }

    private boolean getReadOnly(GGroupObjectValue key, GroupMarker groupMarker) {
        if (groupMarker.polygon != null)
            return isReadOnly("polygon", key, true);
        else
            return isReadOnly("latitude", key, true) && isReadOnly("longitude", key, true);
    }

    protected native static JavaScriptObject getMapOptions(JavaScriptObject customOptions)/*-{
        return customOptions != null ? {
            tileProvider: customOptions.tileProvider,
            options: customOptions.options
        } : null;
    }-*/;

    protected native static JavaScriptObject getViewOptions(JavaScriptObject customOptions)/*-{
        return customOptions != null ? {
            center: customOptions.center,
            zoom: customOptions.zoom
        } : null;
    }-*/;

    // example: {"markerClusterOptions": {"spiderfyDistanceMultiplier": 5}}
    protected native static JavaScriptObject getMarkerClusterOptions(JavaScriptObject customOptions)/*-{
        return customOptions != null ? customOptions.markerClusterOptions : null;
    }-*/;

    protected native boolean hasFitBoundsProperty(JavaScriptObject object)/*-{
        return object.hasOwnProperty('fitBounds') ? object.fitBounds : false;
    }-*/;

    protected native JavaScriptObject updateMap(JavaScriptObject map, JavaScriptObject markerClusters, String tileProvider, JavaScriptObject customOptions)/*-{
        var newMapOptions = @GMap::getMapOptions(*)(customOptions);
        var newViewOptions = @GMap::getViewOptions(*)(customOptions);
        var newMarkerClusterOptions = @GMap::getMarkerClusterOptions(*)(customOptions);

        if (!$wnd.deepEquals(this.@GMap::mapOptions, newMapOptions)) {
            var L = $wnd.L;

            if (map.tile != null)
                map.tile.removeFrom(map);

            tileProvider = newMapOptions != null && newMapOptions.tileProvider != null ? newMapOptions.tileProvider :
                tileProvider != null ? tileProvider : $wnd.lsfParams.defaultMapProvider;

            var tile;
            var options = newMapOptions != null ? newMapOptions.options : {};
            if (tileProvider === 'google') {
                tile = L.gridLayer.googleMutant(
                    $wnd.mergeObjects({
                        // In Leaflet.GridLayer.GoogleMutant it is possible to customize only two parameters: type and styles
                        type: "roadmap", // valid values are 'roadmap', 'satellite', 'terrain' and 'hybrid'
                        // all possible styles available in the documentation https://developers.google.com/maps/documentation/javascript/json-styling-overview
                        styles: [] //empty array is necessary to prevent "Map styles must be an array, but was passed {}" errors in the web browser console when rendering map
                    }, options));
            } else if (tileProvider === 'yandex') {
                tile = L.yandex(
                    $wnd.mergeObjects({
                        type: "map", //map, satellite, hybrid, map~vector
                        //all possible mapOptions available in the documentation in options parameter https://yandex.com/dev/jsapi-v2-1/doc/en/v2-1/ref/reference/Map
                        mapOptions: {} // see options https://yandex.com/dev/jsapi-v2-1/doc/en/v2-1/ref/reference/Map#field_detail
                    }, options));
            } else {
                tile = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                    $wnd.mergeObjects({attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'},
                        options));
            }

            map.tile = tile;
            tile.addTo(map);

            map.addLayer(markerClusters);

            this.@GMap::mapOptions = newMapOptions;
        }

        if (!$wnd.deepEquals(this.@GMap::viewOptions, newViewOptions)) {
            @GMap::setView(*)(map, newViewOptions);
            this.@GMap::viewOptions = newViewOptions;
        }
        
        if (!$wnd.deepEquals(this.@GMap::markerClusterOptions, newMarkerClusterOptions)) {
            markerClusters.options = Object.assign(markerClusters.options, newMarkerClusterOptions ? newMarkerClusterOptions : {});
            this.@GMap::markerClusterOptions = newMarkerClusterOptions;
        }
    }-*/;

    protected native JavaScriptObject initMap(com.google.gwt.dom.client.Element element)/*-{
        return $wnd.L.map(element);
    }-*/;

    protected static native JavaScriptObject setView(JavaScriptObject map, JavaScriptObject viewOptions)/*-{
        var center = $wnd.mergeObjects({lat: 0, lng: 0}, viewOptions != null ? viewOptions.center : {})
        var zoom = viewOptions != null && viewOptions.zoom != null ? viewOptions.zoom : 6;

        return map.setView(center, zoom); // we need to setView to have editing and dragging fields initialized;
    }-*/;

    protected native JavaScriptObject refreshMarkerClusters(JavaScriptObject markerClusters, JsArray<JavaScriptObject> markers)/*-{
        if (markers.length > 0)
            markerClusters.refreshClusters(markers);
    }-*/;

    protected native JavaScriptObject createMarkerClusters()/*-{
        var L = $wnd.L;
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

    protected JavaScriptObject showPopup(Element popupElement, JavaScriptObject popupElementClicked) {
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


    protected native JavaScriptObject setGroupMarker(JavaScriptObject marker, GroupMarker groupMarker)/*-{
        marker.groupMarker = groupMarker;
    }-*/;
    protected native JavaScriptObject createMarker(JavaScriptObject map, boolean polygon, JavaScriptObject markerClusters, JavaScriptObject object)/*-{
        var L = $wnd.L;

        var thisObject = this;

        var marker;
        if(polygon) {
            marker = L.polygon([L.latLng(0, 1), L.latLng(1, -1), L.latLng(-1, -1)]);

            marker.on('edit', function (e) {
                thisObject.@GMap::changePolygonProperty(*)(object, marker.getLatLngs()[0], marker.groupMarker); // https://github.com/Leaflet/Leaflet/issues/5212
            });
        } else {
            marker = L.marker([0, 0],{
                autoPan: true //Whether to pan the map when dragging this marker near its edge or not.
            });

            var superDragEnd = marker.editing._onDragEnd; // there is a bug with clustering, when you drag marker to cluster, nullpointer happens
            marker.editing._onDragEnd = function(t) {
                if(this._map != null)
                    superDragEnd.call(this, t);
            };

            marker.on('dragend', function (e) {
                var latlng = marker.getLatLng();
                thisObject.@GMap::changePointProperty(*)(object, latlng.lat, latlng.lng, marker.groupMarker);
            });
        }

        marker.on('click', function (e) {
            thisObject.@GMap::changeSimpleGroupObject(*)(object, true, marker); // we want "full rerender", at least for now
        });

        markerClusters.addLayer(marker);
        
        return marker;
    }-*/;

    @Override
    protected long changeGroupObject(GGroupObjectValue key, boolean rendered) {
        GGroupObjectValue oldKey = this.currentKey;

        long result = super.changeGroupObject(key, rendered);

        updateCurrent(oldKey, false);

        updateCurrent(key, true);

        return result;
    }

    protected void updateCurrent(GGroupObjectValue key, boolean isCurrent) {
        if(key != null) {
            GroupMarker groupMarker = groupMarkers.get(key);
            JavaScriptObject marker = markers.get(key);
            boolean isPoly = groupMarker.polygon != null;

            if (groupMarker.isEditing())
                disableEditing(marker, isPoly);

            groupMarker.isCurrent = isCurrent;
            if (!isPoly)
                updateIcon(groupMarker, marker);

            if(groupMarker.isEditing())
                enableEditing(marker, isPoly);
        }
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

    // name - deprecated
    protected native static String getName(JavaScriptObject element)/*-{
        return element.tooltip ? element.tooltip.toString() : (element.name ? element.name.toString() : null);
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

    // we need to disable editing before changing icon + check for dragging because of clustering (when there is no dragging which is used by editing)
    protected native void disableEditing(JavaScriptObject object, boolean poly)/*-{
        if (poly) {
            object.editing.disable();
            object.options.editable = false;
        } else {
            if (object.dragging != null)
                object.dragging.disable();

            object.options.draggable = false;
        }
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

    private static native void setMarkerColor(Element element, String color) /*-{
        element.style.setProperty("--marker-color", color);
    }-*/;

    protected void updateIcon(GroupMarker groupMarker, JavaScriptObject marker) {
        Element element = createImageCaptionElement(groupMarker.image, groupMarker.caption, ImageHtmlOrTextType.MAP);

        String color = groupMarker.color;
        if(color != null)
            setMarkerColor(element, color);
        element.setAttribute(COLUMN_ATTRIBUTE, "");
        element.setAttribute(MainFrame.IGNORE_DBLCLICK_CHECK, "");

        updateJsIcon(marker, element, groupMarker.isCurrent ? "focused-marker" : "");
    }

    protected native void updateJsIcon(JavaScriptObject marker, Element element, String className)/*-{
        marker.setIcon($wnd.L.divIcon({
            html: element,
            className: className
        }));
    }-*/;

    protected native void updateColor(JavaScriptObject marker, String color, String clusterColor)/*-{
        marker.color = color;
        marker.clusterColor = clusterColor;
    }-*/;

    protected native void enableEditing(JavaScriptObject object, boolean poly)/*-{
        if (poly) {
            var L = $wnd.L;
            object.editing = new L.Edit.Poly(object); // there is a bug in plugin (with editing after setLatLngs) https://github.com/Leaflet/Leaflet.draw/issues/650
            object.editing.enable();
            object.options.editable = true;
        } else {
            if (object.dragging != null)
                object.dragging.enable();

            object.options.draggable = true;
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
