import board
import busio
from adafruit_ads1x15.ads1115 import ADS1115
from adafruit_ads1x15.analog_in import AnalogIn
from Conversion_Service import get_instruction_progress

# Initialize I2C and ADC
i2c = busio.I2C(board.SCL, board.SDA)
ads = ADS1115(i2c)
chan = AnalogIn(ads, 0)  # Use P0 (A0) for battery voltage


def read_battery_voltage():
    # If using a voltage divider, multiply by the divider ratio
    voltage = chan.voltage * 3.7  # 10K and 27K ohm resistors
    return voltage


def battery_percent(voltage, min_v=10.0, max_v=14.6):
    # Adjust min_v and max_v for your battery chemistry
    percent = (voltage - min_v) / (max_v - min_v) * 100
    percent = max(0, min(100, percent))
    return percent


def progress_check():
    current, total = get_instruction_progress()
    progress = (current / total) * 100 if total > 0 else 0
    return progress


# -----------------------------
# PAINT LEVEL (Load Cell)
# -----------------------------
import time
import statistics
try:
    from hx711 import HX711
    # USER SETTINGS
    DOUT_PIN = 7   # HX711 DT
    SCK_PIN = 8    # HX711 SCK
    CALIBRATION_FACTOR = -300
    EMPTY_WEIGHT = 90
    FULL_WEIGHT = 400
    READINGS = 5

    hx = HX711(dout_pin=DOUT_PIN, pd_sck_pin=SCK_PIN)
    hx.set_reading_format("MSB", "MSB")
    hx.set_reference_unit(CALIBRATION_FACTOR)
    hx.reset()
    hx.tare()
    time.sleep(2)
    _hx711_ready = True
except Exception as e:
    _hx711_ready = False
    hx = None

def get_paint_level():
    """
    Returns (percentage, paint_weight, total_weight) or (None, None, None) if error.
    """
    if not _hx711_ready or hx is None:
        return (None, None, None)
    try:
        values = [hx.get_weight(3) for _ in range(READINGS)]
        weight = statistics.median(values)
        weight = max(0, weight)
        paint_weight = max(0, weight - EMPTY_WEIGHT)
        total_paint_capacity = FULL_WEIGHT - EMPTY_WEIGHT
        if total_paint_capacity <= 0:
            return (None, None, None)
        percentage = (paint_weight / total_paint_capacity) * 100
        percentage = max(0, min(100, percentage))
        # Power cycle HX711 (reduces noise)
        hx.power_down()
        hx.power_up()
        return (percentage, paint_weight, weight)
    except Exception:
        return (None, None, None)
