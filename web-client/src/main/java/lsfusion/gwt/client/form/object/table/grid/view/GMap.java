package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RequiresResize;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.COLUMN_ATTRIBUTE;

public class GMap extends GSimpleStateTableView<JavaScriptObject> implements RequiresResize {
    // No need to support color themes here as we apply svg filters to the icon anyway.
    private final String DEFAULT_MARKER_ICON = "map_marker.png";

    public GMap(GFormController form, GGridController grid, TableContainer tableContainer) {
        super(form, grid, tableContainer);
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

        public boolean isCurrent;
        public boolean isReadOnly;

        public boolean isEditing() {
            return isCurrent && !isReadOnly;
        }

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
        changeProperties(new String[]{"latitude", "longitude"}, new JavaScriptObject[]{object, object}, new PValue[]{PValue.getPValue(lat), PValue.getPValue(lng)});
    }

    protected void changePolygonProperty(JavaScriptObject object, JsArray<WrapperObject> latlngs) {
        changeProperty("polygon", object, PValue.getPValue(getPolygon(latlngs)));
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
    protected void onUpdate(Element renderElement, JsArray<JavaScriptObject> listObjects) {
        if(map == null) {
            markerClusters = createMarkerClusters();
            map = createMap(renderElement, markerClusters, grid.getMapTileProvider());
        }

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

            if(!isPoly && (oldGroupMarker == null || !(GwtClientUtils.nullEquals(groupMarker.icon, oldGroupMarker.icon) && GwtClientUtils.nullEquals(groupMarker.color, oldGroupMarker.color) && groupMarker.isCurrent == oldGroupMarker.isCurrent)))
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
            return isReadOnly("polygon", key);
        else
            return isReadOnly("latitude", key) && isReadOnly("longitude", key);
    }

    protected native boolean hasFitBoundsProperty(JavaScriptObject object)/*-{
        return object.hasOwnProperty('fitBounds') ? object.fitBounds : false;
    }-*/;

    protected native JavaScriptObject createMap(com.google.gwt.dom.client.Element element, JavaScriptObject markerClusters, String tileProvider)/*-{
        var L = $wnd.L;
        var map = L.map(element);

        if (tileProvider === 'google') {
            L.gridLayer
                .googleMutant({
                    type: "roadmap" // valid values are 'roadmap', 'satellite', 'terrain' and 'hybrid'
                }).addTo(map);
        } else if (tileProvider === 'yandex') {
            $wnd.lsfParams.yandexMapAPI.addTo(map);
        } else {
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);
        }

        map.setView([0,0], 6); // we need to set view to have editing and dragging fields initialized

        map.addLayer(markerClusters);

        return map;
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


    protected native JavaScriptObject createMarker(JavaScriptObject map, boolean polygon, JavaScriptObject markerClusters, JavaScriptObject object)/*-{
        var L = $wnd.L;

        var thisObject = this;

        var marker;
        if(polygon) {
            marker = L.polygon([L.latLng(0, 1), L.latLng(1, -1), L.latLng(-1, -1)]);

            marker.on('edit', function (e) {
                thisObject.@GMap::changePolygonProperty(*)(object, marker.getLatLngs()[0]); // https://github.com/Leaflet/Leaflet/issues/5212
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
                thisObject.@GMap::changePointProperty(*)(object, latlng.lat, latlng.lng);
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

    protected static native String getIcon(JavaScriptObject element)/*-{
        return element.icon;
    }-*/;

    protected native static String getName(JavaScriptObject element)/*-{
        return element.name ? element.name.toString() : null;
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

    protected void updateIcon(GroupMarker groupMarker, JavaScriptObject marker) {
        updateJsIcon(marker, groupMarker.icon != null ? groupMarker.icon : GwtClientUtils.getStaticImageURL(DEFAULT_MARKER_ICON),
                groupMarker.color, groupMarker.isCurrent);
    }

    protected native void updateJsIcon(JavaScriptObject marker, String iconUrl, String backgroundColor, boolean isCurrent)/*-{
        var L = $wnd.L;
        var myIcon = L.divIcon({
            html: "<img " + (isCurrent ? "class=\"marker-background-focused\"" : "") +
                " style=\"background-image:" +
                (backgroundColor ? "linear-gradient(" + backgroundColor + "," + backgroundColor + "), " : "") +
                "url(" + iconUrl + "); -webkit-mask-image:url(" + iconUrl + "); height:42px; width:42px;\"" +
                " alt=\"\" tabindex=\"0\" " +
                @lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder::COLUMN_ATTRIBUTE + "=\"true\" " +
                @lsfusion.gwt.client.view.MainFrame::IGNORE_DBLCLICK_CHECK + "=\"true\">",
            className: ''
        });
        marker.setIcon(myIcon);
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
