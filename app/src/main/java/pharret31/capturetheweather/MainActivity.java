package pharret31.capturetheweather;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    EditText m_editText;
    Button m_findButton;
    ListView m_listView;

    List<Map<String, Object>> m_cityWeathers;
    SimpleAdapter m_adapter;

    Context m_context;

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_main);

        m_context = MainActivity.this;

        m_cityWeathers = new ArrayList<>();

        m_editText = (EditText)findViewById(R.id.editText);

        m_listView = (ListView)findViewById(R.id.listView);

        m_adapter = new SimpleAdapter(MainActivity.this, m_cityWeathers,
                R.layout.list_item,
                new String[] {"City", "Weather", "Temperature", "Date", "Image"},
                new int[] {R.id.cityText, R.id.weatherText, R.id.tempText, R.id.timeText, R.id.weatherImage});

        m_adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                boolean binded = false;
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageBitmap((Bitmap)data);
                    binded = true;
                } else if (view instanceof TextView) {
                    if (view.getId() == R.id.tempText) {
                        ((TextView) view).setText(String.format(Locale.getDefault(), "%.2f°C", (Double)data));
                        binded = true;
                    }
                }
                return binded;
            }
        });

        m_listView.setAdapter(m_adapter);

        m_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String cityName = (String) m_cityWeathers.get(position).get("City");
                m_cityWeathers.remove(position);
                ParseTask pt = new ParseTask();
                pt.execute(cityName);
                m_adapter.notifyDataSetChanged();
                return true;
            }
        });

        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent detailsIntent = new Intent(MainActivity.this, DetailsActivity.class);
                startActivity(detailsIntent);
            }
        });

        m_findButton = (Button)findViewById(R.id.findButton);
        m_findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!m_editText.getText().toString().isEmpty()) {
                    ParseTask pt = new ParseTask();
                    pt.execute(m_editText.getText().toString());
                }
                else {
                    Toast toast = Toast.makeText(MainActivity.this,
                            "City name is empty", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    class ParseTask extends AsyncTask<String, Integer, Void> {
        String m_cityName = null;

        @Override
        protected Void doInBackground(String... params) {
            m_cityName = params[0];
            JSONObject content = null;
            int cod = 0;
            try {
                content = getCityWeather(m_cityName);
                cod = content.getInt("cod");
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            if (content == null) {
                publishProgress(0);
                return null;
            } else if (cod == 404) {
                publishProgress(404);
                return null;
            }

            JSONArray weather;
            JSONObject mainPart;
            try {
                weather = content.getJSONArray("weather");
                mainPart = content.getJSONObject("main");

                String weatherDescription = weather.getJSONObject(0).getString("description");
                Double temperature = (Double) mainPart.get("temp");

                Map<String, Object> tempMap = new HashMap<>(4);
                tempMap.put("City", m_cityName);
                tempMap.put("Weather", weatherDescription);
                Date time = new Date();
                tempMap.put("Date", time.toString().substring(10, 16));
                tempMap.put("Temperature", temperature);

                String iconName = weather.getJSONObject(0).getString("icon");
                byte[] imageByte = getIcon(iconName);
                Bitmap bm = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
                tempMap.put("Image", bm);

                m_cityWeathers.add(tempMap);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }

//            String cityName = m_cityName;
//            Weather weather = new Weather();
//            weather.catchTheWeather(cityName);
//            Map<String, Object> cityWTHR = new HashMap<String, Object>(4);
//            cityWTHR.put("City", weather.getCityName());
//            cityWTHR.put("Weather", weather.getWeatherDescription());
//            cityWTHR.put("Date", weather.getTime());
//            cityWTHR.put("Image", weather.getIcon());
//            m_cityWeathers.add(cityWTHR);

            return null;
        }

        private void showErrorToast(String message) {
            Toast.makeText(m_context, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            switch (progress[0]) {
                case 0:
                    showErrorToast("Check your internet connection");
                    break;
                case 404:
                    showErrorToast("City has not found");
                    break;
                default:
                    showErrorToast("Unknown error");
                    break;
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            m_adapter.notifyDataSetChanged();
        }

        private JSONObject getCityWeather(String cityName) throws IOException, JSONException {
            URL cityWeatherURL = new URL("http://api.openweathermap.org/data/2.5/weather?q=" +
                    cityName + "&lang=gb&units=metric&appid=8e96f740a4459fd1c46f92c82e4cb31d");
            BufferedReader bufferReader = null;
            JSONObject city = null;
            try {
                HttpURLConnection cityWeatherConnection = (HttpURLConnection)cityWeatherURL.openConnection();
                cityWeatherConnection.connect();

                bufferReader = new BufferedReader(new InputStreamReader(cityWeatherConnection.getInputStream()));
                StringBuilder buf = new StringBuilder();
                String line;
                while ((line = bufferReader.readLine()) != null) {
                    buf.append(line).append("\n");
                }

                city = new JSONObject(buf.toString());
            } finally {
                if (bufferReader != null) {
                    try {
                        bufferReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return city;
        }

        private byte[] getIcon(String iconName) throws IOException, JSONException {
            URL iconURL = new URL("http://openweathermap.org/img/w/"+iconName+".png");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            HttpURLConnection iconHttp = (HttpURLConnection)iconURL.openConnection();
            iconHttp.connect();

            InputStream inputStream = iconHttp.getInputStream();
            byte[] buffer = new byte[1024];
            while (inputStream.read(buffer) != -1) {
                baos.write(buffer);
            }

            return baos.toByteArray();
        }
    }
}
