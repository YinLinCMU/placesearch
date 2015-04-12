package com.mycompany.placesearch;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.w3c.dom.Text;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    AutoCompleteTextView autoPlace;
    PlaceSearch placeSearch;
    Parser parser;
    private LocationRequest mLocationRequest;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoPlace = (AutoCompleteTextView) findViewById(R.id.auto_places);
        autoPlace.setThreshold(1);

        autoPlace.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placeSearch = new PlaceSearch();
                placeSearch.execute(s.toString());

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                try {
                    geoCoder(getBaseContext());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds




    }


    //download data from google map api url
    @SuppressLint("LongLogTag")
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        StringBuffer sb = new StringBuffer();
        InputStreamReader in;
        try{
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            in  = new InputStreamReader(urlConnection.getInputStream());
            char[] buff = new char[1024];//in case of too large data
            int read;
            while((read = in.read(buff)) != -1){
                sb.append(buff,0,read);
            }
            iStream = urlConnection.getInputStream();
            data = sb.toString();
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());

        }finally{
            try{
                iStream.close();//avoid memory leak
                urlConnection.disconnect();
            }catch (IOException ex){
                Log.d("Exception while closing url", ex.toString());
            }

        }
        return data;
    }

    //get all locations from google places auto complete web
    private class PlaceSearch extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... place) {
            String data = null;
            //MY_GOOGLE_MAP_API_KEY
            String key = "key=AIzaSyCa1xsHVXbWp6LgvPj9dfemclGpIfzhOqo";
            String input="";
            try {
                //encode input text
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                Log.d("Exception while translate input", e1.toString());
            }

            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input+"&"+types+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

            try{
                data = null;
                data = downloadUrl(url);
            }catch(Exception e){
                Log.d("Exception while download",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            parser = new Parser();
            //parsing JSON format data
            parser.execute(result);
            }
        }

    //to parse JSON file download from google place web
    private class Parser extends AsyncTask<String, Integer, List<HashMap<String,String>>> {

        JSONObject jObject;
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;

            PlaceJSONParser placeJsonParser = new PlaceJSONParser();
            try {
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parse(jObject);

            } catch (Exception e) {
                Log.d("Exception in parser", e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
                String[] from = new String[]{"description"};
                int[] to = new int[]{android.R.id.text1};
            //for auto complete
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result, android.R.layout.simple_list_item_1, from, to);
            autoPlace.setAdapter(adapter);


        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void geoCoder(Context context) throws IOException {
        EditText tmp = (EditText)findViewById(R.id.auto_places);
        final String place = tmp.getText().toString();
        //Geocoder geocoder = new Geocoder(context);
        List<Address> addresses;
        addresses = new Geocoder(context).getFromLocationName(place, 10);
//ggeocoder.getFromLocationName(<String addresses>, 1);
        double latitude = 0, longitude = 0;
        if(addresses.size() > 0) {
            latitude= addresses.get(0).getLatitude();
            longitude= addresses.get(0).getLongitude();
            Log.d("lat=", String.valueOf(latitude));
            Log.d("log=", String.valueOf(longitude));
        }
        Log.d("addresses=", addresses.toString());
        TextView coor = (TextView)findViewById(R.id.coordinate);
        coor.setText("latitute = " + String.valueOf(latitude) + " logitute = " + String.valueOf(longitude));

        double distance = 0;
        Location locationA = new Location("A");
        locationA.setLatitude(latitude);
        locationA.setLongitude(longitude);
        Location locationB = new Location("B");
        locationB.setLatitude(37.4271813);//nasa
        locationB.setLongitude(-122.0619054);
        distance = locationA.distanceTo(locationB);//distance in meters
        TextView dis = (TextView)findViewById(R.id.distance);
        dis.setText("distance to nasa research park " + distance);
    }


    //curretn location
    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        //Location location = map.getMyLocation();
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d("location=", String.valueOf(location.getLatitude()));
        TextView loca_show = (TextView)findViewById(R.id.current_location);
        loca_show.setText("curretn lat="+ String.valueOf(location.getLatitude())+" log=" + String.valueOf(location.getLongitude()));
        if (location == null) {
            // Blank for a moment...
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        };
    }
    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    private void setUpMapIfNeeded() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

}