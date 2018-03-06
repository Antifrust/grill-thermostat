package com.thermosatat.marcel.thermostat;

/**
 * Created by Marcel on 21.02.2018.
 */

import java.util.UUID;

public class Constants {

    public static String SERVICE_STRING = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

    public static String CHARACTERISTIC_ECHO_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    public static UUID CHARACTERISTIC_ECHO_UUID = UUID.fromString(CHARACTERISTIC_ECHO_STRING);

    public static String CHARACTERISTIC_DEVICE_STRING = "00002902-0000-1000-8000-00805f9b34fb";
    public static UUID CHARACTERISTIC_DEVICE_UUID = UUID.fromString(CHARACTERISTIC_DEVICE_STRING);

}
