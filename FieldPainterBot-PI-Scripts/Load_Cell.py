import time
import statistics
from hx711 import HX711

# -----------------------------
# USER SETTINGS (To edit)
# -----------------------------
DOUT_PIN = 7   # HX711 DT
SCK_PIN = 8    # HX711 SCK

CALIBRATION_FACTOR = -300  # To calibrate, If weight increases → value decreases → factor is negative
EMPTY_WEIGHT = 90          # grams (empty can weight)
FULL_WEIGHT = 400          # grams (full can weight)

READINGS = 5               # number of samples for filtering, Noise filtering to remove vibration/movements/pump

# -----------------------------
# INIT HX711
# -----------------------------
hx = HX711(dout_pin=DOUT_PIN, pd_sck_pin=SCK_PIN)
hx.set_reading_format("MSB", "MSB")
hx.set_reference_unit(CALIBRATION_FACTOR)

hx.reset()
hx.tare()

print("System ready. Tare complete.")
time.sleep(2)

# -----------------------------
# FUNCTION: GET STABLE WEIGHT
# -----------------------------
def get_weight_filtered():
    values = []
    for _ in range(READINGS):
        val = hx.get_weight(3) #number of internal readings
        values.append(val)

    # Median filter (better than average for noise)
    return statistics.median(values)

# -----------------------------
# MAIN LOOP
# -----------------------------
while True:
    try:
        weight = get_weight_filtered()

        # Remove negative noise
        weight = max(0, weight)

        # Calculate paint weight only
        paint_weight = max(0, weight - EMPTY_WEIGHT)

        # Calculate percentage
        total_paint_capacity = FULL_WEIGHT - EMPTY_WEIGHT
        if total_paint_capacity <= 0:
            print("Error: Check EMPTY_WEIGHT and FULL_WEIGHT")
            break

        percentage = (paint_weight / total_paint_capacity) * 100

        # Clamp values
        percentage = max(0, min(100, percentage))

        # Display
        print(f"Total Weight: {weight:.2f} g")
        print(f"Paint Only: {paint_weight:.2f} g")
        print(f"Paint Level: {percentage:.1f} %")
        if percentage < 10:
            print("⚠️ LOW PAINT LEVEL!")
        print("-" * 30)

        # Power cycle HX711 (reduces noise)
        hx.power_down()
        hx.power_up()

        time.sleep(1)

    except KeyboardInterrupt:
        print("Stopping...")
        break