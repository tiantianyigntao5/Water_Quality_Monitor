package com.waterquality.monitor;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.textfield.TextInputEditText;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TextView tvMcuStatus;
    private TextView tvOneNetStatus;
    private LineChart lineChart;
    private Map<String, TextView> sensorValues;
    private Map<String, TextInputEditText> thresholds;
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeViews();
        setupLineChart();
        connectToOneNET();
    }

    private void initializeViews() {
        tvMcuStatus = findViewById(R.id.tvMcuStatus);
        tvOneNetStatus = findViewById(R.id.tvOneNetStatus);
        lineChart = findViewById(R.id.lineChart);

        sensorValues = new HashMap<>();
        thresholds = new HashMap<>();

        // 初始化传感器卡片
        setupSensorCard("TDS", "ppm", R.id.cardTds);
        setupSensorCard("温度", "°C", R.id.cardTemperature);
        setupSensorCard("浊度", "NTU", R.id.cardTurbidity);
        setupSensorCard("pH值", "", R.id.cardPh);
    }

    private void setupSensorCard(String name, String unit, int cardId) {
        View card = findViewById(cardId);
        TextView tvName = card.findViewById(R.id.tvSensorName);
        TextView tvValue = card.findViewById(R.id.tvSensorValue);
        TextView tvUnit = card.findViewById(R.id.tvSensorUnit);
        TextInputEditText etThreshold = card.findViewById(R.id.etThreshold);

        tvName.setText(name);
        tvUnit.setText(unit);
        sensorValues.put(name, tvValue);
        thresholds.put(name, etThreshold);

        etThreshold.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    publishThreshold(name, Float.parseFloat(s.toString()));
                }
            }
        });
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // 初始化空数据
        List<Entry> entries = new ArrayList<>();
        LineDataSet dataSet = new LineDataSet(entries, "传感器数据");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
    }

    private void connectToOneNET() {
        try {
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttClient(OneNetConfig.MQTT_SERVER, clientId, null);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(OneNetConfig.PRODUCT_ID);
            options.setPassword(OneNetConfig.ACCESS_KEY.toCharArray());

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    runOnUiThread(() -> {
                        tvOneNetStatus.setText("已断开");
                        tvOneNetStatus.setTextColor(Color.RED);
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    updateSensorData(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(options);
            mqttClient.subscribe(OneNetConfig.getDataTopic());

            runOnUiThread(() -> {
                tvOneNetStatus.setText("已连接");
                tvOneNetStatus.setTextColor(Color.GREEN);
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                tvOneNetStatus.setText("连接失败");
                tvOneNetStatus.setTextColor(Color.RED);
            });
        }
    }

    private void updateSensorData(String payload) {
        Map<String, Float> data = MqttMessageParser.parseSensorData(payload);
        runOnUiThread(() -> {
            for (Map.Entry<String, Float> entry : data.entrySet()) {
                TextView valueView = sensorValues.get(entry.getKey());
                if (valueView != null) {
                    valueView.setText(String.format("%.2f", entry.getValue()));
                }

                // 更新图表
                LineData lineData = lineChart.getData();
                LineDataSet dataSet = (LineDataSet) lineData.getDataSetByIndex(0);
                if (dataSet == null) {
                    dataSet = new LineDataSet(new ArrayList<>(), entry.getKey());
                    dataSet.setColor(Color.BLUE);
                    dataSet.setCircleColor(Color.BLUE);
                    lineData.addDataSet(dataSet);
                }

                lineData.addEntry(new Entry(dataSet.getEntryCount(), entry.getValue()), 0);
                lineData.notifyDataChanged();
                lineChart.notifyDataSetChanged();
                lineChart.setVisibleXRangeMaximum(10);
                lineChart.moveViewToX(lineData.getEntryCount());
            }
        });
    }

    private void publishThreshold(String sensorName, float threshold) {
        try {
            String topic = OneNetConfig.getThresholdTopic();
            String payload = String.format("{\"%s\": %.2f}", sensorName, threshold);
            mqttClient.publish(topic, new MqttMessage(payload.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 