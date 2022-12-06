

package com.example.android.soonami;

//What is the use of AsyncTask in Android:
//By default, our application code runs in our main thread and every statement is therefore execute
//in a sequence. If we need to perform long tasks/operations then our main thread is blocked until
//the corresponding operation has finished. For providing a good user experience in our application
//we need to use AsyncTasks class that runs in a separate thread. This class will executes everything
//in doInBackground() method inside of other thread which doesn’t have access to the GUI where all
//the views are present. The onPostExecute() method of this class synchronizes itself again with
//the main UI thread and allows it to make some updating. This method is called automatically after
//the doInBackground method finished its work.
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

/**
 * Displays information about a single earthquake.
 */
public class MainActivity extends AppCompatActivity {

    /** Tag for the log messages */
    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    /** URL to query the USGS dataset for earthquake information */
    //URL in String form to get the desired data if we just want to visit we can directly Uri parse it using intent
    //using ACTION_VIEW
    private static final String USGS_REQUEST_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=6";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Kick off an {@link AsyncTask} to perform the network request
        TsunamiAsyncTask task = new TsunamiAsyncTask();
        task.execute();//AsyncTask class is firstly executed using execute() method
        //execute(Params...) must be invoked on the UI thread==Main Thread.
    }

    /**
     * Update the screen to display information from the given {@link Event}.
     */
    private void updateUi(Event earthquake) {
        // Display the earthquake title in the UI
        TextView titleTextView = (TextView) findViewById(R.id.title);
        titleTextView.setText(earthquake.title);

        // Display the earthquake date in the UI
        TextView dateTextView = (TextView) findViewById(R.id.date);
        dateTextView.setText(getDateString(earthquake.time));//getDateString() is a method to format the string into
                                                             //formatted date

        // Display whether or not there was a tsunami alert in the UI
        TextView tsunamiTextView = (TextView) findViewById(R.id.tsunami_alert);
        tsunamiTextView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));//getTsunamiAlertString() is a method to get the
                                                                          // alert of the Tsunami on the basis of int value
    }

    /**
     * Returns a formatted date and time string for when the earthquake happened.
     */
    private String getDateString(long timeInMilliseconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(timeInMilliseconds);
    }

    /**
     * Return the display string for whether or not there was a tsunami alert for an earthquake.
     */
    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no); //getString is used to get a string for the given key element
                                                   // alert_no is a resource element of strings.xml with String value 'No'
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI with the first earthquake in the response.
     */
    // Creating New Class i.e a Private Class
    // public abstract class AsyncTask
    //AsyncTask class is used to do background operations that will update the UI(user interface).
    //Mainly we used it for short operations that will not effect on our main thread.
    //AsyncTask must be subclassed to be used. The subclass will override at least one method (doInBackground(Params...)),
    // Here subclassed as TsunamiAsyncTask class
    private class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {

        //AsyncTask<Params,Progress,Result>
        //Params, the type of the parameters sent to the task upon execution.
        //Progress, the type of the progress units published during the background computation.
        //Result, the type of the result of the background computation.
        //Not all types are always used by an asynchronous task. To mark a type as unused, simply use the type Void

        //Override this method to perform a computation on a background thread

        @Override
        protected Event doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(USGS_REQUEST_URL);//USGS_REQUEST_URL=> A Line Number 49
            // createUrl() function is at 181 line

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";// this is for JSON response
            try {
                jsonResponse = makeHttpRequest(url);//code at line number 195 for function makeHttpRequest
            } catch (IOException e) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object
            Event earthquake = extractFeatureFromJson(jsonResponse);

            // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}
            return earthquake;
        }
        //onProgressUpdate(Progress...), invoked on the UI thread
        //After a call to publishProgress(Progress...). The timing of the execution is undefined



        /**
         * Update the screen with the given earthquake (which was the result of the
         * {@link TsunamiAsyncTask}).
         */
        //Runs on the UI thread after doInBackground(Params...)
        //The result of the background operation is passed to this step as a parameter
        // and then we can easily update our UI to show the results.
        @Override
        protected void onPostExecute(Event earthquake) {
            if (earthquake == null) {
                return;
            }

            updateUi(earthquake);//Function Code at Line Number 76
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null; /// Way to create URl -> URL myURL = new URL("http://example.com/");
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();//Obtain a new HttpURLConnection by calling URL.openConnection()
                                                                         //and casting the result to HttpURLConnection
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);// The operation waits until it reads at least one data byte from the socket.
                // However, if the method doesn't return anything after an unspecified time,
                // it throws an InterrupedIOException with a “Read timed out” error message:

                urlConnection.setConnectTimeout(15000 /* milliseconds */);//The connection timeout is the timeout in making the initial connection;
                                                                          // i.e. completing the TCP connection handshake.
                urlConnection.connect();
                if (urlConnection.getResponseCode() == 200) {
                    inputStream = urlConnection.getInputStream();//The response body may be read from the stream returned by
                    // URLConnection.getInputStream(). If the response has no body, that method returns an empty stream.
                    //The getInputStream() returns an input stream for reading bytes from this socket.
                    jsonResponse = readFromStream(inputStream);//Function is at line number 216
                }
                else{
                    Log.e(LOG_TAG,"Error response code: "+urlConnection.getResponseCode());
                }
            } catch (IOException e) {
                // TODO: Handle the exception
                Log.e(LOG_TAG,"Problem retrieving the earthquake JSON results.",e);

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                // An InputStreamReader is a bridge from byte streams to character streams:
                // It reads bytes and decodes them into characters using a specified charset
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        /**
         * Return an {@link Event} object by parsing out information
         * about the first earthquake from the input earthquakeJSON string.
         */
        private Event extractFeatureFromJson(String earthquakeJSON) {

            if (TextUtils.isEmpty(earthquakeJSON)){
                return null;
            }

            try {
                JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
                JSONArray featureArray = baseJsonResponse.getJSONArray("features");

                // If there are results in the features array
                if (featureArray.length() > 0) {
                    // Extract out the first feature (which is an earthquake)
                    JSONObject firstFeature = featureArray.getJSONObject(0);
                    JSONObject properties = firstFeature.getJSONObject("properties");

                    // Extract out the title, time, and tsunami values
                    String title = properties.getString("title");
                    long time = properties.getLong("time");
                    int tsunamiAlert = properties.getInt("tsunami");

                    // Create a new {@link Event} object
                    return new Event(title, time, tsunamiAlert);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
            }
            return null;
        }
    }
}
