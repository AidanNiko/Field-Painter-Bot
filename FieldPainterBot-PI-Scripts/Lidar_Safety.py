import time
import threading
from Conversion_Service import set_system_paused
from rplidar import RPLidar

# --- CONFIGURATION ---

LIDAR_PORT = "/dev/ttyUSB0"  # Change as needed
DISTANCE_THRESHOLD = 1000  # mm, adjust for your needs


# State variables
system_stopped = False
distance_traveled = 0  # mm, update this with your movement logic


# Functions to control the system
def stop_system():
    set_system_paused(True)
    print("STOP: Obstacle detected!")


def resume_system():
    set_system_paused(False)
    print("RESUME: Path is clear.")


# Set up RPLIDAR connection
lidar = RPLidar(LIDAR_PORT)


# Function to get the minimum distance in a scan (front-facing obstacle)
def get_lidar_distance():
    # Get one scan, return the minimum distance (in mm) in the front 60-degree sector
    # Adjust angle range as needed for your robot's "front"
    min_distance = None
    for scan in lidar.iter_scans(max_buf_meas=500):
        for _, angle, distance in scan:
            # Consider only front sector (e.g., -30 to +30 degrees)
            if (angle >= 330 or angle <= 30) and distance > 0:
                if min_distance is None or distance < min_distance:
                    min_distance = distance
        if min_distance is not None:
            return min_distance
        # If no valid reading, try again


# LIDAR safety loop to be run in a thread
def lidar_safety_loop():
    global system_stopped, distance_traveled
    try:
        while True:
            distance = get_lidar_distance()
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
        print(f"LIDAR safety loop exited: {e}")
    finally:
        lidar.stop()
        lidar.disconnect()


# Function to start the LIDAR safety loop in a background thread
def start_lidar_safety():
    t = threading.Thread(target=lidar_safety_loop, daemon=True)
    t.start()
    return t
