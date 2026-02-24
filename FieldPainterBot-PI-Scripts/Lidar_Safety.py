import time
import threading
import logging

from Conversion_Service import set_system_paused
from distance_utils import update_distance_traveled, distance_traveled
from rplidar import RPLidar


# For quick standalone test, add this at the top:
# def set_system_paused(paused: bool):
#     print(f"[TEST] System paused set to: {paused}")


# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

LIDAR_PORT = "COM4"  # Change as needed
DISTANCE_THRESHOLD = 350  # mm, adjust for your needs


# State variables
system_stopped = False
distance_traveled = 0  # mm, update this with your movement logic


# --- Distance Tracking ---
# Call this function from your movement code to update distance_traveled
def update_distance_traveled(duration_s, speed_cm_per_s=50.0):
    """
    Update the estimated distance traveled.
    duration_s: time spent moving (seconds)
    speed_cm_per_s: speed in cm/s (default matches Conversion_Service)
    Updates global distance_traveled in mm.
    """
    global distance_traveled
    distance_cm = duration_s * speed_cm_per_s
    distance_traveled += distance_cm * 10  # convert cm to mm
    return distance_traveled


# Functions to control the system
def stop_system():
    set_system_paused(True)
    logger.warning("STOP: Obstacle detected!")
    print("STOP: Obstacle detected!")


def resume_system():
    set_system_paused(False)
    logger.info("RESUME: Path is clear.")
    print("RESUME: Path is clear.")


# Set up RPLIDAR connection
lidar = RPLidar(LIDAR_PORT, baudrate=256000)


def get_lidar_distance():
    """
    Get the minimum distance in the front-facing sector (e.g., -30 to +30 degrees, or 330 to 30 degrees).
    Only considers data from the front of the rover as the LIDAR spins 360 degrees.
    """
    min_distance = None
    # Collect a full 360-degree scan, then filter for front sector
    for scan in lidar.iter_scans(max_buf_meas=500):
        front_distances = []
        for _, angle, distance in scan:
            # Normalize angle to [0, 360)
            angle = angle % 360
            # Front sector: 330 to 360 or 0 to 30 (i.e., -30 to +30 degrees)
            if (angle >= 330 or angle <= 30) and distance > 0:
                front_distances.append(distance)
        if front_distances:
            min_distance = min(front_distances)
            logger.debug(
                f"LIDAR scan: min front distance = {min_distance} mm, all front distances: {front_distances}"
            )
            return min_distance
        # If no valid reading, try again


# LIDAR safety loop to be run in a thread
def lidar_safety_loop():
    global system_stopped
    try:
        logger.info("LIDAR safety loop started.")
        while True:
            distance = get_lidar_distance()
            logger.info(f"LIDAR min distance (front): {distance} mm")
            print(f"LIDAR min distance (front): {distance} mm")
            if distance < DISTANCE_THRESHOLD:
                if not system_stopped:
                    stop_system()
                    system_stopped = True
            else:
                if system_stopped:
                    resume_system()
                    system_stopped = False
            time.sleep(0.1)
    except Exception as e:
        logger.error(f"LIDAR safety loop exited: {e}")
        print(f"LIDAR safety loop exited: {e}")
    finally:
        logger.info("LIDAR safety loop cleaning up (stopping/disconnecting LIDAR)")
        lidar.stop()
        lidar.disconnect()


# Function to start the LIDAR safety loop in a background thread
def start_lidar_safety():
    def lidar_thread_with_retry():
        global lidar
        while True:
            try:
                # Try to (re)initialize LIDAR if not already initialized
                if "lidar" not in globals() or lidar is None:
                    try:
                        lidar = RPLidar(LIDAR_PORT)
                        logger.info("LIDAR initialized successfully.")
                    except Exception as e:
                        logger.error(f"LIDAR not detected or failed to initialize: {e}")
                        print(f"LIDAR not detected or failed to initialize: {e}")
                        lidar = None
                        time.sleep(10)
                        continue
                lidar_safety_loop()
                break  # Exit if loop ends normally
            except Exception as e:
                logger.error(f"LIDAR safety loop crashed: {e}")
                print(f"LIDAR safety loop crashed: {e}")
                if lidar:
                    try:
                        lidar.stop()
                        lidar.disconnect()
                    except Exception:
                        pass
                    lidar = None
                time.sleep(10)  # Wait before retrying

    t = threading.Thread(target=lidar_thread_with_retry, daemon=True)
    t.start()
    return t


if __name__ == "__main__":
    logger.info("Starting LIDAR safety test. Press Ctrl+C to exit.")
    print("Starting LIDAR safety test. Press Ctrl+C to exit.")
    start_lidar_safety()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        logger.info("Exiting LIDAR safety test.")
        print("Exiting LIDAR safety test.")
