#!/usr/bin/env python3
"""PyBluez simple example rfcomm-server.py

Simple demonstration of a server application that uses RFCOMM sockets.

Author: Albert Huang <albert@csail.mit.edu>
$Id: rfcomm-server.py 518 2007-08-10 07:20:07Z albert $
"""

import threading
import json
import time
import bluetooth
import subprocess
from Lidar_Safety import start_lidar_safety
from Conversion_Service import (
    Convert_To_Array,
    execute_field_pattern,
    translate_manual_instruction,
)
from status_checks import battery_percent, read_battery_voltage, progress_check


def setup_bluetooth_server():
    subprocess.call(["sudo", "hciconfig", "hci0", "piscan"])
    server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    server_sock.bind(("", bluetooth.PORT_ANY))
    server_sock.listen(1)
    port = server_sock.getsockname()[1]
    uuid = "00001101-0000-1000-8000-00805F9B34FB"
    bluetooth.advertise_service(
        server_sock,
        "SampleServer",
        service_id=uuid,
        service_classes=[uuid, bluetooth.SERIAL_PORT_CLASS],
        profiles=[bluetooth.SERIAL_PORT_PROFILE],
        # protocols=[bluetooth.OBEX_UUID]
    )
    print("Waiting for connection on RFCOMM channel", port)
    return server_sock


def handle_client(client_sock):

    bat = "BATTERY:66"
    paint = "SPRAY:33"
    progress = "PROGRESS:60"
    # Send initial status
    time.sleep(1)
    client_sock.send(bat.encode("utf-8"))
    client_sock.send(paint.encode("utf-8"))
    client_sock.send(progress.encode("utf-8"))

    stop_battery_thread = threading.Event()

    def battery_update_loop():
        while not stop_battery_thread.is_set():
            try:
                try:
                    bat_msg = f"BATTERY:{battery_percent(read_battery_voltage()):.1f}"
                    client_sock.send(bat_msg.encode("utf-8"))
                except Exception as e:
                    print("Battery status error:", e)

                try:
                    progress_msg = f"PROGRESS:{progress_check():.1f}"
                    client_sock.send(progress_msg.encode("utf-8"))
                except Exception as e:
                    print("Progress status error:", e)

            except Exception as e:
                print("Unknown battery update error:", e)
                break
            time.sleep(10)  # Send every 10 seconds

    battery_thread = threading.Thread(target=battery_update_loop, daemon=True)
    battery_thread.start()

    try:
        while True:
            # receive commands
            data = client_sock.recv(1024)

            if not data:
                break
            print("Received", data)
            try:
                msg = json.loads(data.decode("utf-8"))
                if isinstance(msg, dict) and "items" in msg:
                    instructions = Convert_To_Array(msg)
                    execute_field_pattern(instructions)
                elif isinstance(msg, dict):
                    translate_manual_instruction(msg)
                else:
                    print("Unknown command format")
            except Exception as e:
                print("Error parsing command:", e)
    except OSError:
        raise
    finally:
        stop_battery_thread.set()
        battery_thread.join(timeout=2)


def main():
    # Start LIDAR safety thread
    start_lidar_safety()
    server_sock = setup_bluetooth_server()
    try:
        client_sock, client_info = server_sock.accept()
        print("Accepted connection from", client_info)
        handle_client(client_sock)
        print("Disconnected.")
        client_sock.close()
    except Exception as e:
        print("Error:", e)
    finally:
        server_sock.close()
        print("All done.")


if __name__ == "__main__":
    main()
