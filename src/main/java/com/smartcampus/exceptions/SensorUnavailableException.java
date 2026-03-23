package com.smartcampus.exceptions;

public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String sensorId) {
        super("Sensor " + sensorId + " is currently under MAINTENANCE and cannot accept new readings.");
    }
}