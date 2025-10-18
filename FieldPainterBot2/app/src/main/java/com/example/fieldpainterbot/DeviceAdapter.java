package com.example.fieldpainterbot;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// Adapter for displaying a list of discovered Bluetooth devices
// each item shows device name and handles click events via a listener 
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final OnDeviceClickListener onClick;
    
    // Constructor takes a click listener to handle device selection
    // parameter onClick Callback triggered when adevice is clicked
    public DeviceAdapter(OnDeviceClickListener onClick) {
        this.onClick = onClick;
    }

    // Updates the adapter's device list and refreshes the UI
    // parameter newDevices list of newly discovered Bluetooth devices
    @SuppressLint("NotifyDataSetChanged")
    public void submitList(List<BluetoothDevice> newDevices) {
        devices.clear(); // clear old list
        devices.addAll(newDevices); // add new devices
        notifyDataSetChanged(); // Notify RecyclerView to redraw all items
    }
    // Inflate the layout for each item in the RecyclerView
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }
    // Binds data to each ViewHolder as it becomes visible
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position); // get device at current position
        String name = device.getName();  // get device name (can be null)
        holder.textView.setText(name != null ? name : "Unnamed Device"); // fallback if name is null
        // Set click listener to notify the ViewModel when a device is selected
        holder.itemView.setOnClickListener(v -> onClick.onDeviceClick(device));
    }
    // Returns the total number of items in the list
    @Override
    public int getItemCount() {
        return devices.size();
    }
    // ViewHolder class holds references to the views for each item
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ViewHolder(View itemView) {
            super(itemView);
            // Reference to the TextView that displays the device name
            textView = itemView.findViewById(R.id.deviceName);
        }
    }
}
