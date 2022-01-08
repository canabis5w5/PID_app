package com.example.pid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private static final String SAVED_TEXT = "ip";

    GraphView graphView;
    LineGraphSeries series = new LineGraphSeries();
    LineGraphSeries series1 = new LineGraphSeries();

    TextView temperature,
             temperature2,
             ipText,
             text_state1_hold,
             text_state2_hold,
             text_set_temp1,
             text_set_temp2;

    Button update,
            bSave,
            button_set1,
            button_set2;

    CheckBox checkBox;

    SharedPreferences spref;
    private Request request;
    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler();
    private byte set = 0;
    private byte check = 0;
    private int hash_cycl = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doTheAutoRefresh();

        temperature = (TextView) findViewById(R.id.temperature);
        temperature2 = (TextView) findViewById(R.id.temperature2);
        text_state1_hold = (TextView) findViewById(R.id.text_state1_hold);
        text_state2_hold = (TextView) findViewById(R.id.text_state2_hold);
        text_set_temp1 = (TextView) findViewById(R.id.text_set_temp1);
        text_set_temp2 = (TextView) findViewById(R.id.text_set_temp2);
        ipText = (TextView) findViewById(R.id.ipText);
        bSave = (Button) findViewById(R.id.bSave);
        button_set1 = (Button) findViewById(R.id.button_set1);
        button_set2 = (Button) findViewById(R.id.button_set2);
        graphView = (GraphView) findViewById(R.id.graf);
        checkBox = (CheckBox) findViewById(R.id.checkBox);

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveIp();
            }
        });

        button_set1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String set_temp1 = text_set_temp1.getText().toString().trim();
                String temp1 = "/console/send?text=State:";
                set_temp1 = temp1.concat(set_temp1);
                set = 1;

                    try {
                        post(set_temp1);
                        set = 0;
                    } catch (IOException e) {}
            }
        });

        button_set2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String set_temp2 = text_set_temp2.getText().toString().trim();
                String temp2 = "/console/send?text=State1:";
                set_temp2 = temp2.concat(set_temp2);
                set = 1;

                try {
                    post(set_temp2);
                    set = 0;
                } catch (IOException e) {}

            }
        });

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()){
                    check = 1;
                }
            }
        });

        LoadIp();
    }

    private void SaveIp(){
        spref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ip = spref.edit();
        ip.putString(SAVED_TEXT, ipText.getText().toString().trim());
        ip.commit();
    }

    private void LoadGraphics() {

        try {
            int temp_int1 = NumberFormat.getInstance().parse(temperature.getText().toString().trim()).intValue();
            int temp_int2 = NumberFormat.getInstance().parse(temperature2.getText().toString().trim()).intValue();

            series.appendData(new DataPoint(hash_cycl , temp_int1), true, 300);
            series.setTitle("Temp1");
            series.setColor(Color.BLUE);
            series.setThickness(8);

            series1.appendData(new DataPoint(hash_cycl, temp_int2), true, 300);
            series1.setTitle("Temp2");
            series1.setColor(Color.GREEN);
            series1.setThickness(8);

        graphView.addSeries(series1);
        graphView.addSeries(series);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(300);
        graphView.getViewport().setMinY(70);
        graphView.getViewport().setMaxY(130);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setScrollable(true);
        } catch (Exception e){}
    }

    private void LoadIp(){
        spref = getPreferences(MODE_PRIVATE);
        String savedtext = spref.getString(SAVED_TEXT, "");
        ipText.setText(savedtext);
    }

    private void post(String post) throws IOException {

        String ip = spref.getString(SAVED_TEXT, "");
        String http_str = "http://";
        ip = http_str.concat(ip);
        ip = ip.concat(post);

        request = new Request.Builder()
                .url(ip)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody responseBody = response.body();

                if (response.isSuccessful()){
                    if(set == 0){

                    String text = response.body().string();
                try {
                    getValue(text);
                } catch (Exception e){};}
                }
            }
        });
    }

    private void getValue(String string){
        int index_state = string.lastIndexOf("=========================");
        int index_hold = string.lastIndexOf("Hold:");
        if ((index_hold >= 0) & (index_state >= 0)) {

        String str_temp = string.substring(index_hold, index_state);
        str_temp = str_temp.replace("u000a","\\");
        str_temp = str_temp.replace("Hold: "," ");
        str_temp = str_temp.replace("State1:     Temp1: "," ");
        str_temp = str_temp.replace(" ","");
        String[] str_out = str_temp.split("\\\\");

        //str_temp = String.join(" ", str_out[0], str_out[2], str_out[4], str_out[6]);  //0,2,4,6

        temperature.post(new Runnable() {
            @Override
            public void run() {
                temperature.setText(str_out[0]);
            }
        });

        temperature2.post(new Runnable() {
            @Override
            public void run() {
                temperature2.setText(str_out[4]);
            }
        });

        text_state1_hold.post(new Runnable() {
            @Override
            public void run() {
                text_state1_hold.setText(str_out[2]);
            }
        });

        text_state2_hold.post(new Runnable() {
            @Override
            public void run() {
                text_state2_hold.setText(str_out[6]);
            }
        });
        }
    }

    private void doTheAutoRefresh(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doTheAutoRefresh();
                try {
                    post("/console/text");
                    if (check == 1){
                    LoadGraphics();}
                    hash_cycl ++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1500);
    }
}

