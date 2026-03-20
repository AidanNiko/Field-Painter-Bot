# mpu6050_gyro.py
# Reads data from the GY-521 MPU6050 module and prints gyro/accel data
# Requires: pip install mpu6050-raspberrypi

from mpu6050 import mpu6050
import time
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

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
YAW_AXIS = "z"
YAW_SIGN = -1

# =============================================================================
# CALIBRATION - computed at startup by averaging readings while stationary.
# Keep the robot still during calibration.
# Accel offsets bring x/y to 0 and z to 9.81 m/s² (gravity).
# Gyro offsets bring all axes to 0 deg/s.
# =============================================================================
ACCEL_OFFSET = {"x": 0.0, "y": 0.0, "z": 0.0}
GYRO_OFFSET  = {"x": 0.0, "y": 0.0, "z": 0.0}


def calibrate(samples: int = 200, delay: float = 0.005) -> None:
    """Sample the sensor while stationary and compute bias offsets."""
    global ACCEL_OFFSET, GYRO_OFFSET
    logger.info(f"Calibrating MPU6050 — keep the robot still ({samples} samples)...")
    accel_sum = {"x": 0.0, "y": 0.0, "z": 0.0}
    gyro_sum  = {"x": 0.0, "y": 0.0, "z": 0.0}
    for _ in range(samples):
        a = sensor.get_accel_data()
        g = sensor.get_gyro_data()
        for axis in ("x", "y", "z"):
            accel_sum[axis] += a[axis]
            gyro_sum[axis]  += g[axis]
        time.sleep(delay)
    ACCEL_OFFSET = {axis: accel_sum[axis] / samples for axis in ("x", "y", "z")}
    GYRO_OFFSET  = {axis: gyro_sum[axis]  / samples for axis in ("x", "y", "z")}
    # Leave gravity on the z-axis so z reads ~9.81 m/s² when flat
    ACCEL_OFFSET["z"] -= 9.81
    logger.info(f"Accel offsets: {ACCEL_OFFSET}")
    logger.info(f"Gyro  offsets: {GYRO_OFFSET}")


# Run calibration automatically when the module is imported / started
calibrate()


def read_gyro_accel():
    accel_data = sensor.get_accel_data()
    gyro_data  = sensor.get_gyro_data()
    temp       = sensor.get_temp()
    accel_cal  = {k: accel_data[k] - ACCEL_OFFSET[k] for k in accel_data}
    gyro_cal   = {k: gyro_data[k]  - GYRO_OFFSET[k]  for k in gyro_data}
    logger.info(f"Accelerometer (calibrated): {accel_cal}")
    logger.info(f"Gyroscope     (calibrated): {gyro_cal}")
    logger.info(f"Temperature: {temp} C")


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
    # Apply calibration offset then use configured axis and sign
    yaw += (gyro_data[YAW_AXIS] - GYRO_OFFSET[YAW_AXIS]) * YAW_SIGN * dt
    return yaw


if __name__ == "__main__":
    try:
        while True:
            read_gyro_accel()
            time.sleep(0.5)
    except KeyboardInterrupt:
        logger.info("Exiting...")
