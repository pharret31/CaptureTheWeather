package pharret31.capturetheweather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class Weather {
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
    }

    public int catchTheWeather(String cityName) {
        ParseURLTask pt = new ParseURLTask();
        pt.execute(cityName);
        try {
            m_fullWeather = pt.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (m_fullWeather == null) {
            return -1;
        }

        try {
            parseWeatherJSON(m_fullWeather);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

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

    private class ParseURLTask extends AsyncTask<String, Integer, JSONObject> {
        String m_cityName = null;

        @Override
        protected JSONObject doInBackground(String... params) {
            m_cityName = params[0];
            JSONObject weather = null;
            try {
                weather = getWeatherJSON(m_cityName);
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
            if (weather == null) {
                return null;
            }
            byte [] iconByte = new byte[0];
            try {
                iconByte = getIcon(weather.getString("icon"));
            } catch (NullPointerException | JSONException | IOException e) {
                e.printStackTrace();
            }
            m_icon = BitmapFactory.decodeByteArray(iconByte, 0, iconByte.length);
            return weather;
        }

        private JSONObject getWeatherJSON(String cityName) throws IOException, JSONException {
            URL cityWeatherURL = new URL("http://api.openweathermap.org/data/2.5/weather?q=" +
                    cityName + "&lang=ru&units=metric&appid=8e96f740a4459fd1c46f92c82e4cb31d");
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

        m_dt = new Date((Integer) fullWeather.get("dt") * 1000);

        JSONObject sys = fullWeather.getJSONObject("sys");
        m_countryName = sys.getString("country");
        m_sunrise = new Date((Integer) sys.get("sunrise") * 1000);
        m_sunset = new Date((Integer) sys.get("sunset") * 1000);

        m_id = fullWeather.getInt("id");
        m_name = fullWeather.getString("name");
        m_cod = fullWeather.getInt("cod");

        return 0;
    }
}
