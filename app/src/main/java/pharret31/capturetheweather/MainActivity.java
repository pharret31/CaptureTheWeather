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
//                addWeather(cityName);
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
                    Weather weather = new Weather();
                    weather.execute(m_editText.getText().toString());
                    Map<String, Object> cityWTHR = new HashMap<>(1);
                    cityWTHR.put("City", weather.getCityName());
                    cityWTHR.put("Weather", weather.getWeatherDescription());
                    cityWTHR.put("Temperature", weather.getTemperature());
                    cityWTHR.put("Date", weather.getTime());
                    cityWTHR.put("Image", weather.getIcon());
                    m_cityWeathers.add(cityWTHR);
                    m_adapter.notifyDataSetChanged();
//                    addWeather(m_editText.getText().toString());
                }
                else {
                    Toast toast = Toast.makeText(MainActivity.this,
                            "City name is empty", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }
//
//    private int addWeather(String cityName) {
//        Weather weather = new Weather();
//        Thread weatherThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//                weather.execute(cityName);
//            }
//        })
//
//        Map<String, Object> cityWTHR = new HashMap<>(1);
//        cityWTHR.put("City", weather.getCityName());
//        cityWTHR.put("Weather", weather.getWeatherDescription());
//        cityWTHR.put("Temperature", weather.getTemperature());
//        cityWTHR.put("Date", weather.getTime());
//        cityWTHR.put("Image", weather.getIcon());
//        m_cityWeathers.add(cityWTHR);
//        m_adapter.notifyDataSetChanged();
//    }
}
