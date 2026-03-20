import time
import threading
import logging

from Conversion_Service import set_system_paused
from rplidar import RPLidar, RPLidarException

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

LIDAR_PORT = "/dev/ttyUSB0"
DISTANCE_THRESHOLD = 450  # mm

system_stopped = False


def stop_system():
    set_system_paused(True)
    logger.warning("STOP: Obstacle detected!")
    print("STOP: Obstacle detected!")


def resume_system():
    set_system_paused(False)
    logger.info("RESUME: Path is clear.")
    print("RESUME: Path is clear.")


def lidar_safety_loop():
    """
    Keeps the scan iterator open continuously.
    - RPLidarException (buffer/protocol errors): hardware reset, fast retry (1s).
    - Any other exception (USB loss, OS error): full reconnect after 5s.
    """
    global system_stopped

    while True:
        lidar = None
        retry_delay = 5
        full_disconnect = True
        try:
            lidar = RPLidar(LIDAR_PORT, baudrate=115200)
            # Flush stale bytes left in the serial buffer from any previous session.
            # Without this, the response parser reads mid-packet and raises
            # "wrong body size" / wrong response header errors.
            lidar._serial_port.reset_input_buffer()
            lidar.start_motor()
            time.sleep(1)  # allow motor to reach operating speed
            logger.info("LIDAR connected. Starting scan loop.")

            last_processed = 0
            # 3000 measurements ≈ 8 full scans of headroom — prevents buffer overflow
            for scan in lidar.iter_scans(max_buf_meas=3000):
                now = time.time()
                if now - last_processed < 0.05:
                    continue  # throttle processing; iterator still drains the buffer
                last_processed = now

                front_distances = [
                    distance
                    for _, angle, distance in scan
                    if distance > 0 and (angle % 360 >= 330 or angle % 360 <= 30)
                ]

                if not front_distances:
                    continue

                min_distance = min(front_distances)

                if min_distance < DISTANCE_THRESHOLD:
                    if not system_stopped:
                        stop_system()
                        system_stopped = True
                else:
                    if system_stopped:
                        resume_system()
                        system_stopped = False

        except RPLidarException as e:
            # Buffer overflow or protocol error — USB is still alive;
            # issue a hardware reset and re-enter the scan loop without disconnecting.
            retry_delay = 1
            full_disconnect = False
            logger.warning(f"LIDAR protocol error: {e} — resetting and retrying in {retry_delay}s...")
            if lidar:
                try:
                    lidar.stop()
                    lidar.reset()
                    time.sleep(0.5)  # wait for reset to complete
                    lidar._serial_port.reset_input_buffer()  # discard corrupt bytes
                except Exception:
                    pass

        except Exception as e:
            retry_delay = 5
            full_disconnect = True
            logger.error(f"LIDAR error: {e} — retrying in {retry_delay}s...")

        finally:
            if lidar and full_disconnect:
                try:
                    lidar.stop()
                    lidar.stop_motor()
                    lidar.disconnect()
                    logger.info("LIDAR disconnected cleanly.")
                except Exception:
                    pass

        time.sleep(retry_delay)


def start_lidar_safety():
    t = threading.Thread(target=lidar_safety_loop, daemon=True)
    t.start()
    return t


if __name__ == "__main__":
    logger.info("Starting LIDAR safety test. Press Ctrl+C to exit.")
    start_lidar_safety()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Exiting.")
