# mpu6050_gyro.py
# Reads data from the GY-521 MPU6050 module and prints gyro/accel data
# Requires: pip install mpu6050-raspberrypi

from mpu6050 import mpu6050
import time

# Default I2C address for MPU6050 is 0x68
sensor = mpu6050(0x68)

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
    # Gyro z is in deg/sec, integrate to get angle
    yaw += gyro_data['z'] * dt
    return yaw

if __name__ == "__main__":
    try:
        while True:
            read_gyro_accel()
            time.sleep(0.5)
    except KeyboardInterrupt:
        print("Exiting...")
