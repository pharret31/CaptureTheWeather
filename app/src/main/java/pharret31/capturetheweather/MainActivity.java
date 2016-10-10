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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    final private String[] WEATHER_WATCHING_PARAMETERS = {
            "City name",
            "Weather word",
            "Temperature",
            "Date and time",
            "Icon"};

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
                WEATHER_WATCHING_PARAMETERS,
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
                        ((TextView) view).setText(String.format(Locale.getDefault(), "%.1f°C", (Double)data));
                        binded = true;
                    } else if (view.getId() == R.id.timeText) {
                        SimpleDateFormat timeFormat = new SimpleDateFormat(
                                "HH:mm\ndd.MM.yy",
                                Locale.US);
                        ((TextView) view).setText(timeFormat.format((Date)data));
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
                String cityName = (String) m_cityWeathers.get(position).get("City name");
                m_cityWeathers.remove(position);
                Weather weather = new Weather();
                weather.execute(cityName);
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
                String cityName = m_editText.getText().toString();
                if (!cityName.isEmpty()) {
                    Weather weather = new Weather();
                    weather.execute(cityName);
                }
                else {
                    Toast toast = Toast.makeText(MainActivity.this,
                            R.string.citys_name_is_empty, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    public class Weather extends AsyncTask<String, Integer, JSONObject> {
        final private double HECTO_PASCAL = 0.75006375541921;

        private Double m_longitude;
        private Double m_latitude;
        private String m_weatherName;
        private String m_weatherDescription;
        private String m_base;
        private Double m_temperature;
        private Double m_pressure;
        private Integer m_humidity;
        private Double m_windSpeed;
        private Double m_windDegree;
        private Integer m_clouds;
        private Date m_dt;
        private String m_countryName;
        private Date m_sunrise;
        private Date m_sunset;
        private Integer m_id;
        private String m_name;
        private Integer m_cod;
        private Bitmap m_icon;

        private JSONObject m_fullWeather;

        Weather() {
            m_longitude = null;
            m_latitude = null;
            m_weatherName = null;
            m_weatherDescription = null;
            m_base = null;
            m_temperature = null;
            m_pressure = null;
            m_humidity = null;
            m_windSpeed = null;
            m_windDegree = null;
            m_clouds = null;
            m_dt = null;
            m_countryName = null;
            m_sunrise = null;
            m_sunset = null;
            m_id = null;
            m_name = null;
            m_cod = null;
            m_icon = null;

            m_fullWeather = null;
        }

        class IconThread extends Thread {
            private String iconName;
            private Bitmap icon;
            private int positionInMainList;

            {
                positionInMainList = -1;
            }

            public void run() {
                icon = null;
                int i = 1000; // Count of repeats
                while (i > 0) {
                    byte[] iconByte = new byte[0];
                    try {
                        iconByte = getIcon(iconName);
                    } catch (NullPointerException | JSONException | IOException e) {
                        e.printStackTrace();
                    }
                    icon = BitmapFactory.decodeByteArray(iconByte, 0, iconByte.length);

                    if (icon != null) {
                        putIconInMainList(icon);
                        break;
                    }

                    i--;
                }
            }

            private void putIconInMainList(Bitmap _icon) {
                if (positionInMainList == -1) { return; }

                Map<String, Object> tempMap = m_cityWeathers.get(positionInMainList);
                tempMap.put("Icon", _icon);
                m_cityWeathers.set(positionInMainList, tempMap);
            }

            synchronized void setIconName(String _iconName) {
                iconName = _iconName;
            }

            synchronized void setPositionInMainList(int _position) {
                positionInMainList = _position;
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

        @Override
        protected JSONObject doInBackground(String... params) {
            String cityName = params[0];
            JSONObject fullWeather = null;
            try {
                fullWeather = getWeatherJSON(cityName);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            if (fullWeather == null) {
                return null;
            }

            m_fullWeather = fullWeather;
            try {
                parseWeatherJSON(fullWeather);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            String [] LIST_OF_PARAMETERS = {
                    "Longtitude",
                    "Latitude",
                    "Weather word",
                    "Weather description",
                    "Base",
                    "Temperature",
                    "Pressure",
                    "Humidity",
                    "Wind speed",
                    "Wind degree",
                    "Clouds",
                    "Date and time",
                    "Country name",
                    "Sunrise time",
                    "Sunset time",
                    "ID",
                    "City name",
                    "Cod",
                    "Icon",
            };
            Object [] LIST_OF_VALUES = {
                    m_longitude,
                    m_latitude,
                    m_weatherName,
                    m_weatherDescription,
                    m_base,
                    m_temperature,
                    m_pressure,
                    m_humidity,
                    m_windSpeed,
                    m_windDegree,
                    m_clouds,
                    m_dt,
                    m_countryName,
                    m_sunrise,
                    m_sunset,
                    m_id,
                    m_name,
                    m_cod,
                    m_icon,
            };
            if (LIST_OF_VALUES.length != LIST_OF_PARAMETERS.length) {
                throw new IllegalStateException("Lists have no same length in class Weather");
            }
            int lengthOfLists = LIST_OF_PARAMETERS.length;

            Map<String, Object> weatherOrigin = new HashMap<>();
            for (int i = 0; i < lengthOfLists; i++) {
                weatherOrigin.put(LIST_OF_PARAMETERS[i], LIST_OF_VALUES[i]);
            }
            m_cityWeathers.add(weatherOrigin);

            String iconName = "";
            try {
                iconName = fullWeather.getJSONArray("weather").getJSONObject(0).getString("icon");
            } catch (NullPointerException | JSONException e) {
                e.printStackTrace();
            }
            if (iconName.length() != 0) {
                IconThread iconThread = new IconThread();
                iconThread.setIconName(iconName);
                iconThread.setPositionInMainList(m_cityWeathers.size() - 1);
                iconThread.run();
            }

            return fullWeather;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            m_adapter.notifyDataSetChanged();
        }

        private JSONObject getWeatherJSON(String cityName) throws IOException, JSONException {
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

        private int parseWeatherJSON(JSONObject fullWeather) throws JSONException, IOException {
            JSONObject coordinates = fullWeather.getJSONObject("coord");
            m_longitude = (Double) coordinates.get("lon");
            m_latitude = (Double) coordinates.get("lat");

            JSONObject weather = fullWeather.getJSONArray("weather").getJSONObject(0);
            m_weatherName = weather.getString("main");
            m_weatherDescription = weather.getString("description");

            m_base = fullWeather.getString("base");

            JSONObject mainPart = fullWeather.getJSONObject("main");
            m_temperature = (Double) mainPart.get("temp");
            m_pressure = mainPart.getInt("pressure") * HECTO_PASCAL;
            m_humidity = mainPart.getInt("humidity");

            JSONObject wind = fullWeather.getJSONObject("wind");
            m_windSpeed = (Double) wind.get("speed");
            m_windDegree = (Double) wind.get("deg");

            JSONObject clouds = fullWeather.getJSONObject("clouds");
            m_clouds = clouds.getInt("all");

            m_dt = new Date(Long.valueOf((Integer) fullWeather.get("dt")) * 1000);

            JSONObject sys = fullWeather.getJSONObject("sys");
            m_countryName = sys.getString("country");
            m_sunrise = new Date(Long.valueOf((Integer) sys.get("sunrise")) * 1000);
            m_sunset = new Date(Long.valueOf((Integer) sys.get("sunset")) * 1000);

            m_id = fullWeather.getInt("id");
            m_name = fullWeather.getString("name");
            m_cod = fullWeather.getInt("cod");

            return 0;
        }

        public Double getLongitude() {
            return m_longitude;
        }

        public Double getLatitude() {
            return m_latitude;
        }

        public String getWeatherName() {
            return m_weatherName;
        }

        public String getWeatherDescription() {
            return m_weatherDescription;
        }

        public String getBase() {
            return m_base;
        }

        public Double getTemperature() {
            return m_temperature;
        }

        public Double getPressure() {
            return m_pressure;
        }

        public Integer getHumidity() {
            return m_humidity;
        }

        public Double getWindSpeed() {
            return m_windSpeed;
        }

        public Double getWindDegree() {
            return m_windDegree;
        }

        public Integer getClouds() {
            return m_clouds;
        }

        public Date getTime() {
            return m_dt;
        }

        public String getCountryName() {
            return m_countryName;
        }

        public Date getSunriseTime() {
            return m_sunrise;
        }

        public Date getSunsetTime() {
            return m_sunset;
        }

        public Integer getCityId() {
            return m_id;
        }

        public String getCityName() {
            return m_name;
        }

        public Integer getCod() {
            return m_cod;
        }

        public Bitmap getIcon() {
            return m_icon;
        }
    }
}
