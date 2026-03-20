import time
import threading
import logging

from Conversion_Service import set_system_paused
from rplidar import RPLidar

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

LIDAR_PORT = "/dev/ttyUSB0"
DISTANCE_THRESHOLD = 475  # mm

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
    Keeps the scan iterator open continuously instead of
    re-opening it on every call — fixes the byte-count errors.
    """
    global system_stopped

    while True:
        lidar = None
        try:
            lidar = RPLidar(LIDAR_PORT, baudrate=115200)
            logger.info("LIDAR connected. Starting scan loop.")

            # ✅ One persistent iterator for the lifetime of the connection
            for scan in lidar.iter_scans(max_buf_meas=500):
                front_distances = []
                for _, angle, distance in scan:
                    angle = angle % 360
                    if (angle >= 330 or angle <= 30) and distance > 0:
                        front_distances.append(distance)

                if not front_distances:
                    continue

                min_distance = min(front_distances)
                #logger.info(f"LIDAR min front distance: {min_distance:.1f} mm")
                #print(f"LIDAR min front distance: {min_distance:.1f} mm")

                if min_distance < DISTANCE_THRESHOLD:
                    if not system_stopped:
                        stop_system()
                        system_stopped = True
                else:
                    if system_stopped:
                        resume_system()
                        system_stopped = False

                time.sleep(0.05)

        except Exception as e:
            logger.error(f"LIDAR error: {e} — retrying in 5s...")
            print(f"LIDAR error: {e} - retrying in 5s...")

        finally:
            if lidar:
                try:
                    lidar.stop()
                    lidar.disconnect()
                    logger.info("LIDAR disconnected cleanly.")
                except Exception:
                    pass

        time.sleep(5)  # Wait before reconnecting


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
