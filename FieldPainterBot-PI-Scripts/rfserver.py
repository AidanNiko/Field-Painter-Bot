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
    set_system_paused,
    set_system_cancelled,
)
from status_checks import battery_percent, read_battery_voltage, progress_check
import logging


logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)


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
                    bat_msg = f"BATTERY:{int(battery_percent(read_battery_voltage()))}"
                    logger.info("Sending battery status: %s", bat_msg)
                    client_sock.send(bat_msg.encode("utf-8"))
                except Exception as e:
                    logger.error("Battery status error: %s", e)

                try:
                    progress_msg = f"PROGRESS:{int(progress_check())}"
                    logger.info("Sending progress status: %s", progress_msg)
                    client_sock.send(progress_msg.encode("utf-8"))
                except Exception as e:
                    logger.error("Progress status error: %s", e)

            except Exception as e:
                logger.error("Unknown battery update error: %s", e)
                break
            time.sleep(10)  # Send every 10 seconds

    battery_thread = threading.Thread(target=battery_update_loop, daemon=True)
    battery_thread.start()

    pattern_thread = None

    try:
        buffer = ""
        while True:
            # receive commands
            data = client_sock.recv(4096)

            if not data:
                break

            buffer += data.decode("utf-8")

            # Process all complete messages in the buffer (split by newline)
            while "\n" in buffer:
                decoded_data, buffer = buffer.split("\n", 1)
                decoded_data = decoded_data.strip()
                if not decoded_data:
                    continue
                if decoded_data == "HALT":
                    set_system_paused(True)
                    logger.warning("HALT command received: system paused.")
                    continue
                elif decoded_data == "RESUME":
                    set_system_paused(False)
                    logger.info("RESUME command received: system resumed.")
                    continue
                elif decoded_data == "QUIT":
                    set_system_cancelled(True)
                    set_system_paused(False)  # Unblock any paused wait loop
                    if pattern_thread and pattern_thread.is_alive():
                        pattern_thread.join(timeout=5)
                    logger.warning(
                        "QUIT command received: pattern execution cancelled."
                    )
                    continue
                logger.info("Received %s", decoded_data)
                try:
                    msg = json.loads(decoded_data)
                    if isinstance(msg, dict) and "items" in msg:
                        # Run field pattern in a thread so HALT can be received mid-execution
                        if pattern_thread and pattern_thread.is_alive():
                            logger.warning(
                                "Pattern already running, ignoring new request."
                            )
                            continue
                        instructions = Convert_To_Array(msg)
                        pattern_thread = threading.Thread(
                            target=execute_field_pattern,
                            args=(instructions,),
                            daemon=True,
                        )
                        pattern_thread.start()
                    elif isinstance(msg, dict):
                        translate_manual_instruction(msg)
                    else:
                        logger.warning("Unknown command format")
                except Exception as e:
                    logger.error("Error parsing command: %s", e)
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
        logger.info("Accepted connection from %s", client_info)
        handle_client(client_sock)
        logger.info("Disconnected.")
        client_sock.close()
    except Exception as e:
        logger.error("Error: %s", e)
    finally:
        server_sock.close()
        logger.info("All done.")


if __name__ == "__main__":
    main()
