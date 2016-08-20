package Modules;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mai Thanh Hiep on 4/3/2016.
 */
public class DirectionFinder {
    private static final String DIRECTION_URL_API = "https://maps.googleapis.com/maps/api/directions/json?";
    private static final String GOOGLE_API_KEY = "AIzaSyBZYKePGV98mJwNop_T-kNUctc2hK1Cfw4";
    private DirectionFinderListener listener;
    private String origin;
    private String destination;
    private double destination_x, destination_y, origin_x,  origin_y;

    public DirectionFinder(DirectionFinderListener listener, String origin, String destination) {
        this.listener = listener;
        this.origin = origin;
        this.destination = destination;
    }

    public void execute() throws UnsupportedEncodingException {
        listener.onDirectionFinderStart();
        new DownloadRawData().execute(createUrl());
    }

    private String createUrl() throws UnsupportedEncodingException {
        String urlOrigin = URLEncoder.encode(origin, "utf-8");
        String urlDestination = URLEncoder.encode(destination, "utf-8");

        return DIRECTION_URL_API + "origin=" + urlOrigin + "&destination=" + urlDestination + "&key=" + GOOGLE_API_KEY;
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------------
    public DirectionFinder(DirectionFinderListener listener, double origin_x ,double origin_y,double destination_x,double destination_y) {
        this.listener = listener;
        this.origin_x = origin_x;
        this.origin_y = origin_y;
        this.destination_x = destination_x;
        this.destination_y = destination_y;
    }

    public void test_execute() throws UnsupportedEncodingException {
        listener.onDirectionFinderStart();
        new DownloadRawData().execute(test_createUrl());
    }

    private String test_createUrl() throws UnsupportedEncodingException {
        String urlOrigin_x = String.valueOf(origin_x);
        String urlOrigin_y = String.valueOf(origin_y);
        String urlDestination_x = String.valueOf(destination_x);
        String urlDestination_y = String.valueOf(destination_y);

        String urlOriginx = URLEncoder.encode(urlOrigin_x, "utf-8");
        String urlOriginy = URLEncoder.encode(urlOrigin_y, "utf-8");
        String urlDestinationx = URLEncoder.encode(urlDestination_x, "utf-8");
        String urlDestinationy = URLEncoder.encode(urlDestination_y, "utf-8");

        return DIRECTION_URL_API + "origin=" + urlOriginx + ","+ urlOriginy +"&destination=" + urlDestinationx + ","+urlDestinationy + "&key=" + GOOGLE_API_KEY;
    }
    //-----------------------------------------------------------------------------------------------------------------------------------------------------
    private class DownloadRawData extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String link = params[0];
            try {
                URL url = new URL(link);
                InputStream is = url.openConnection().getInputStream();
                StringBuffer buffer = new StringBuffer();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                parseJSon(res);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseJSon(String data) throws JSONException {
        if (data == null)
            return;

        List<Route> routes = new ArrayList<Route>();
        JSONObject jsonData = new JSONObject(data);
        JSONArray jsonRoutes = jsonData.getJSONArray("routes");
        //"routes"裡面有"bounds" 、"copyrights"、 "legs"、"overview_polyline"、
        // "summary"、"warnings" 、"waypoint_order"
        //用到"legs"、"overview_polyline"
        for (int i = 0; i < jsonRoutes.length(); i++) {
            JSONObject jsonRoute = jsonRoutes.getJSONObject(i);
            Route route = new Route();

            JSONObject overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
            JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
            //"legs"裡面有"distance" 、 "duration"、 "end_address"、"end_location"
            //"start_address"、"start_location"、"steps"、"traffic_speed_entry"、"via_waypoint"

            //"steps" 裡面有詳細路徑每一步裡面下列屬性"distance" 、 "duration"、"end_location"
            //""html_instructions""、"start_location"、"travel_mode"
            JSONObject jsonLeg = jsonLegs.getJSONObject(0);
            JSONObject jsonDistance = jsonLeg.getJSONObject("distance");
            JSONObject jsonDuration = jsonLeg.getJSONObject("duration");
            JSONObject jsonEndLocation = jsonLeg.getJSONObject("end_location");
            JSONObject jsonStartLocation = jsonLeg.getJSONObject("start_location");

            JSONObject jsonSteps = jsonLeg.getJSONObject("steps");////////////////////////////////////////
            JSONObject stepduration = jsonSteps.getJSONObject("duration");////////////////////////////////

            route.distance = new Distance(jsonDistance.getString("text"), jsonDistance.getInt("value"));
            route.duration = new Duration(jsonDuration.getString("text"), jsonDuration.getInt("value"));
            route.endAddress = jsonLeg.getString("end_address");
            route.startAddress = jsonLeg.getString("start_address");
            route.startLocation = new LatLng(jsonStartLocation.getDouble("lat"), jsonStartLocation.getDouble("lng"));
            route.endLocation = new LatLng(jsonEndLocation.getDouble("lat"), jsonEndLocation.getDouble("lng"));
            route.points = decodePolyLine(overview_polylineJson.getString("points"));

            route.stepdistance[i] = new Distance(jsonDistance.getString("text"), jsonDistance.getInt("value"));/////////////////////

            routes.add(route);
        }
        listener.onDirectionFinderSuccess(routes);
    }

    private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }

        return decoded;
    }
}
