# mpu6050_gyro.py
# Reads data from the GY-521 MPU6050 module and prints gyro/accel data
# Requires: pip install mpu6050-raspberrypi

from mpu6050 import mpu6050
import time

import logging


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Default I2C address for MPU6050 is 0x68
sensor = mpu6050(0x68)

# =============================================================================
# MOUNTING CORRECTION - adjust these if your sensor is not oriented correctly
# YAW_AXIS: which gyro axis represents your robot's rotation ('x', 'y', or 'z')
# YAW_SIGN: 1 for normal, -1 to flip the direction
#
# Current mounting: X → left of rover, Y → back of rover
# Z (= X × Y = left × back) therefore points UP — yaw axis is correct.
# However, positive Z rotation in this frame goes from left toward back,
# which is CLOCKWISE viewed from above (rover turning right).
# Negate so that a left/CCW turn gives a positive yaw increment.
# =============================================================================
YAW_AXIS = 'z'
YAW_SIGN = -1

def read_gyro_accel():
    accel_data = sensor.get_accel_data()
    gyro_data = sensor.get_gyro_data()
    temp = sensor.get_temp()
    print(f"Accelerometer: {accel_data}")
    print(f"Gyroscope: {gyro_data}")
    print(f"Temperature: {temp} C\n")


# Simple yaw estimation by integrating gyro z-axis (degrees/sec)
yaw = 0.0
last_time = None

def get_yaw():
    global yaw, last_time
    gyro_data = sensor.get_gyro_data()
    current_time = time.time()
    if last_time is None:
        last_time = current_time
        return yaw
    dt = current_time - last_time
    last_time = current_time
    # Use configured axis and sign to handle any mounting orientation
    yaw += gyro_data[YAW_AXIS] * YAW_SIGN * dt
    return yaw

if __name__ == "__main__":
    try:
        while True:
            read_gyro_accel()
            time.sleep(0.5)
    except KeyboardInterrupt:
        print("Exiting...")
