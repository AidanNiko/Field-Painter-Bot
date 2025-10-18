package com.example.fieldpainterbot;
// Represent the possible state of a Bluetooth connection.
// Used to track and communicate connection status throughout the app
public enum ConnectionStatus {
    // Indicates that the device is successfully connected
    CONNECTED,
    // Indicates that a connection attempt is in progress
    CONNECTING,
    // Indicates that the device is not connected
    DISCONNECTED
}
