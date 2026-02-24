# distance_utils.py
# Utility for tracking distance traveled, shared by Lidar_Safety and Conversion_Service

distance_traveled = 0  # mm, update this with your movement logic

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
