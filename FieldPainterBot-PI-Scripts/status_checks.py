import board
import busio
from adafruit_ads1x15.ads1115 import ADS1115
from adafruit_ads1x15.analog_in import AnalogIn
from Conversion_Service import get_instruction_progress

# Initialize I2C and ADC
i2c = busio.I2C(board.SCL, board.SDA)
ads = ADS1115(i2c)
chan = AnalogIn(ads, ADS1115.P0)  # Use P0 (A0) for battery voltage


def read_battery_voltage():
    # If using a voltage divider, multiply by the divider ratio
    voltage = chan.voltage * 3.7  # 10K and 27K ohm resistors
    return voltage


def battery_percent(voltage, min_v=3.0, max_v=4.2):
    # Adjust min_v and max_v for your battery chemistry
    percent = (voltage - min_v) / (max_v - min_v) * 100
    percent = max(0, min(100, percent))
    return percent


def progress_check():
    current, total = get_instruction_progress()
    return current, total
