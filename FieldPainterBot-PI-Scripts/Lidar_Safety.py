import time
import threading
import logging
from Conversion_Service import set_system_paused
from rplidar import RPLidar


# --- CONFIGURATION ---

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

LIDAR_PORT = "/dev/ttyUSB0"  # Change as needed
DISTANCE_THRESHOLD = 1000  # mm, adjust for your needs


# State variables
system_stopped = False
distance_traveled = 0  # mm, update this with your movement logic


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
lidar = RPLidar(LIDAR_PORT)


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
            logger.debug(f"LIDAR scan: min front distance = {min_distance} mm, all front distances: {front_distances}")
            return min_distance
        # If no valid reading, try again


# LIDAR safety loop to be run in a thread
def lidar_safety_loop():
    global system_stopped, distance_traveled
    try:
        logger.info("LIDAR safety loop started.")
        while True:
            distance = get_lidar_distance()
            logger.info(f"LIDAR min distance (front): {distance} mm")
            print(f"LIDAR min distance (front): {distance} mm")
            if distance < DISTANCE_THRESHOLD:
                if not system_stopped:
                    stop_system()
                    # Save distance traveled so far (update as needed)
                    # distance_traveled = ...
                    system_stopped = True
            else:
                if system_stopped:
                    resume_system()
                    # Resume from distance_traveled (update as needed)
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
    t = threading.Thread(target=lidar_safety_loop, daemon=True)
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
