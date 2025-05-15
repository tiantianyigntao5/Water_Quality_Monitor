package com.waterquality.monitor.utils;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class MqttMessageParser {
    public static Map<String, Float> parseSensorData(String jsonPayload) {
        Map<String, Float> result = new HashMap<>();
        try {
            JSONObject json = new JSONObject(jsonPayload);
            if (json.has("TDS")) {
                result.put("TDS", (float) json.getDouble("TDS"));
            }
            if (json.has("temperature")) {
                result.put("温度", (float) json.getDouble("temperature"));
            }
            if (json.has("turbidity")) {
                result.put("浊度", (float) json.getDouble("turbidity"));
            }
            if (json.has("pH")) {
                result.put("pH值", (float) json.getDouble("pH"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String createThresholdMessage(Map<String, Float> thresholds) {
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Float> entry : thresholds.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
} 