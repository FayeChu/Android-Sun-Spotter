package edu.uw.xfchu.sunspotter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.uw.xfchu.sunspotter.model.Weather;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ForcastAdapter forcastAdapter;

    String input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            input = savedInstanceState.getString("input");
            sunSpotter(input);
        }

        final EditText searchField = (EditText)findViewById(R.id.search_txt);
        final Button button = (Button)findViewById(R.id.btn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = searchField.getText().toString();

                //Hide keyboard
                InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                sunSpotter(input);
            }

        });

        // Construct the data source
        ArrayList<Weather> weather = new ArrayList<>();
        // Create the adapter to convert the array to views
        forcastAdapter = new ForcastAdapter(this, weather);
        // Attach the adapter to a ListView

        //create a DisplayMetrics metrics objec
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //get the information required to size the display
        int widthPixels = metrics.widthPixels;

        // get the density of the screen using metrics
        float scaleFactor = metrics.density;
        //calculate the amount of density independent pixels
        float widthDp = widthPixels / scaleFactor;

        if (widthDp < 600) {
            ListView listView= (ListView) findViewById(R.id.list_view);
            listView.setAdapter(forcastAdapter);
        } else {
            GridView gridview = (GridView) findViewById(R.id.grid_view);
            gridview.setAdapter(forcastAdapter);
        }

    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        String search = ((EditText)findViewById(R.id.search_txt)).
                getText().toString();
        savedInstanceState.putString("input", search);
        super.onSaveInstanceState(savedInstanceState);
    }

    //Create custom adapter
    public class ForcastAdapter extends ArrayAdapter<Weather> {
        private  class ViewHolder {
            ImageView icon;
            TextView info;
            TextView date;
            TextView temp;
        }

        public ForcastAdapter(Context context, ArrayList<Weather> weathers) {
            super(context, R.layout.list_item, weathers);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            //Get the data item for this position
            Weather weather = getItem(position);
            ViewHolder viewHolder;
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item, parent, false);
                // Lookup view for data population
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.tvIcon);
                viewHolder.info = (TextView) convertView.findViewById(R.id.tvInfo);
                viewHolder.date = (TextView) convertView.findViewById(R.id.tvDate);
                viewHolder.temp = (TextView) convertView.findViewById(R.id.tvTemp);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            //lookup icon id
            int id = getResources().getIdentifier("icon" + weather.icon, "drawable", getPackageName());

            //get data string
            String dateString = weather.date;
            SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
            SimpleDateFormat fFormat = new SimpleDateFormat("E HH:mm a");
            Date d = new Date();
            try {
                d = dFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            // Populate the data into the template view using the data object
            viewHolder.icon.setImageDrawable(getDrawable(id));
            viewHolder.info.setText(weather.info);
            viewHolder.date.setText(fFormat.format(d) + " ");
            viewHolder.temp.setText("(" + weather.temp.toString() + "°)");

            // Return the completed view to render on screen
            return convertView;
        }
    }

    //download weather from openweathermap
    private void sunSpotter(String searchTerm) {
        String openKey = getString(R.string.OPEN_WEATHER_MAP_API_KEY);

        String urlString = "http://api.openweathermap.org/data/2.5/forecast?zip=" + searchTerm +
                "&format=json" + "&units=imperial" + "&appid=" + openKey;

        Request request = new JsonObjectRequest(Request.Method.GET, urlString, null,
                new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        ArrayList<Weather> weathers = new ArrayList<>();

                        try {
                            //parse the JSON results
                            JSONArray results = response.getJSONArray("list");

                            for(int i=0; i < results.length(); i++){
                                Weather weather = new Weather();
                                String icon = results.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("icon");
                                String info = results.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("main");
                                String date = results.getJSONObject(i).getString("dt_txt");
                                Double temp = results.getJSONObject(i).getJSONObject("main").getDouble("temp");

                                Weather w = new Weather(icon, info, date, temp);
                                weathers.add(w);
                            }
                        } catch (JSONException e) {
                            e.getStackTrace();
                        }

                        boolean sun = false;
                        String date = "There Will Be Sun Soon!";

                        forcastAdapter.clear();

                        // Add item to adapter
                        for(Weather weather : weathers) {
                            forcastAdapter.add(weather);
                            if(((weather.icon).equals("01d") || (weather.icon).equals("02d")) && sun == false) {
                                sun = true;
                                String dateString = weather.date;
                                SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
                                SimpleDateFormat fFormat = new SimpleDateFormat("E HH:mm a");
                                Date d = new Date();
                                try {
                                    d = dFormat.parse(dateString);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                date = "At " + fFormat.format(d);
                            }
                        }

                        TextView resultView = (TextView) findViewById(R.id.sunText);
                        TextView dateView = (TextView) findViewById(R.id.sunDate);
                        ImageView image = (ImageView) findViewById(R.id.sunImage);
                        String result = sun == true ? "There will be sun!" : "Sorry! No Sun.";
                        if (sun == true) {
                            image.setImageResource(R.drawable.ic_check_circle_black_24dp);
                        } else {
                            image.setImageResource(R.drawable.ic_highlight_off_black_24dp);
                        }

                        resultView.setText(result);
                        dateView.setText(date);

                    }

                },
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        String errMsg = new String(error.networkResponse.data);
                        Toast.makeText(MainActivity.this.getApplicationContext(), errMsg, Toast.LENGTH_LONG).show();
                    }
                });
        RequestSingleton.getInstance(this).add(request);

    }

    protected static class RequestSingleton {
        //the single instance of this singleton
        private static RequestSingleton instance;

        private RequestQueue requestQueue = null; //the singleton's RequestQueue
        private ImageLoader imageLoader = null;

        //private constructor; cannot instantiate directly
        private RequestSingleton(Context ctx){
            //create the requestQueue
            this.requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());

            //create the imageLoader
            imageLoader = new ImageLoader(requestQueue,
                    new ImageLoader.ImageCache() {  //define an anonymous Cache object
                        //the cache instance variable
                        private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

                        //method for accessing the cache
                        @Override
                        public Bitmap getBitmap(String url) {
                            return cache.get(url);
                        }

                        //method for storing to the cache
                        @Override
                        public void putBitmap(String url, Bitmap bitmap) {
                            cache.put(url, bitmap);
                        }
                    });
        }

        //call this "factory" method to access the Singleton
        public static RequestSingleton getInstance(Context ctx) {
            //only create the singleton if it doesn't exist yet
            if(instance == null){
                instance = new RequestSingleton(ctx);
            }

            return instance; //return the singleton object
        }

        //get queue from singleton for direct action
        public RequestQueue getRequestQueue() {
            return this.requestQueue;
        }

        //convenience wrapper method
        public <T> void add(Request<T> req) {
            requestQueue.add(req);
        }

        public ImageLoader getImageLoader() {
            return this.imageLoader;
        }
    }

}


