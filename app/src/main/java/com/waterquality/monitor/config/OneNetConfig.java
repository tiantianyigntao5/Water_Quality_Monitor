package com.waterquality.monitor.config;

public class OneNetConfig {
    public static final String MQTT_SERVER = "tcp://183.230.40.39:6002";
    public static final String PRODUCT_ID = "YOUR_PRODUCT_ID"; // TODO: 替换为实际的产品ID
    public static final String ACCESS_KEY = "YOUR_ACCESS_KEY"; // TODO: 替换为实际的访问密钥
    public static final String DEVICE_ID = "YOUR_DEVICE_ID"; // TODO: 替换为实际的设备ID

    public static String getDataTopic() {
        return String.format("$sys/%s/%s/dp/post/json", PRODUCT_ID, DEVICE_ID);
    }

    public static String getThresholdTopic() {
        return String.format("$sys/%s/%s/dp/post/json/threshold", PRODUCT_ID, DEVICE_ID);
    }
} 