import logging

current_instruction_index = 0  # Tracks the current instruction being executed
total_instructions = 0  # Tracks the total number of instructions

import time
import math
from gpiozero import PWMOutputDevice, DigitalOutputDevice


# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Global pause flag for safety (set by LIDAR safety or other modules)
system_paused = False


def set_system_paused(paused: bool):
    global system_paused
    system_paused = paused
    if paused:
        logger.warning("System PAUSED by safety system.")
        stop_all()
    else:
        logger.info("System RESUMED by safety system.")


# =============================================================================
# CALIBRATION CONSTANTS - ADJUST THESE FOR YOUR ROBOT
# =============================================================================

# Wheel and chassis measurements
WHEEL_DIAMETER_CM = 16.5  # 6.5 inch wheels = 16.5cm (use 20.3 for 8 inch)
WHEEL_BASE_CM = 45.72  # Distance between wheel centers (18 inches = 45.72cm)

# Motor speed settings (hoverboard motors are fast, keep these low for accuracy)
DRIVE_SPEED = 0.3  # Speed for straight movement (0.0 - 1.0)
TURN_SPEED = 0.25  # Speed for turning in place

# Calibration values - TUNE THESE BY TESTING
# Run calibration_test() to measure actual values
CM_PER_SECOND = 50.0  # How far robot travels per second at DRIVE_SPEED
DEGREES_PER_SECOND = 90.0  # How fast robot rotates per second at TURN_SPEED

# Instruction type mappings - EASILY CHANGE THESE IF YOUR APP USES DIFFERENT NAMES
INSTRUCTION_TYPES = {
    # Maps your app's instruction names to internal handler names
    # Format: 'app_name': 'handler_name'
    "walk": "walk",  # Move forward (Paint field controls spray)
    "rotation": "rotation",  # Rotate in place (Quantity = degrees)
    "circle": "circle",  # Circle means half-circle (arc)
}

# Instruction data field mappings - CHANGE THESE TO MATCH YOUR APP'S JSON FORMAT
INSTRUCTION_FIELDS = {
    "order": "Instruction Order",  # Field name for instruction order
    "quantity": "Quantity",  # Field name for quantity (distance cm or degrees)
    "type": "Type of Movement",  # Field name for instruction type
    "paint": "Paint",  # Field name for spray on/off (boolean)
}


# =============================================================================


# --- PWM Pin Revision for ZS-X11H ---
# Use hardware PWM pins for motor PWM (GPIO 12, 13, 18, or 19)
# Example: GPIO 18 (physical pin 12) and GPIO 13 (physical pin 33)
MOTOR1_PWM_PIN = 18  # Hardware PWM0
MOTOR2_PWM_PIN = 13  # Hardware PWM1

# Motor 1 - BLDC Driver (ZS-X11H)
motor1_pwm = PWMOutputDevice(
    MOTOR1_PWM_PIN, frequency=1000
)  # P pin - Speed control (hardware PWM)
motor1_dir = DigitalOutputDevice(27)  # DIR pin - Direction
motor1_stop = DigitalOutputDevice(22, initial_value=False)  # STOP pin (LOW=run)

# Motor 2 - BLDC Driver (ZS-X11H)
motor2_pwm = PWMOutputDevice(
    MOTOR2_PWM_PIN, frequency=1000
)  # P pin - Speed control (hardware PWM)
motor2_dir = DigitalOutputDevice(24)  # DIR pin - Direction
motor2_stop = DigitalOutputDevice(5, initial_value=False)  # STOP pin (LOW=run)

# Spray actuator via L298N (software PWM is fine for spray)
spray_in1 = DigitalOutputDevice(20)  # IN1
spray_in2 = DigitalOutputDevice(21)  # IN2
spray_enable = PWMOutputDevice(25, frequency=50)  # ENA (PWM)


# Motor control helper functions
def sync_wheels_clockwise_low_pwm():
    """Set both wheels to rotate clockwise at low PWM."""
    low_pwm = 0.1
    motor1_backward(low_pwm)  # Left wheel clockwise
    motor2_forward(low_pwm)   # Right wheel counterclockwise


def motor1_forward(speed: float):
    motor1_dir.off()
    motor1_stop.off()  # Enable motor
    motor1_pwm.value = speed


def motor1_backward(speed: float):
    motor1_dir.on()
    motor1_stop.off()  # Enable motor
    motor1_pwm.value = speed


def motor1_halt():
    motor1_pwm.value = 0
    motor1_stop.on()  # Disable motor


def motor2_forward(speed: float):
    motor2_dir.off()
    motor2_stop.off()  # Enable motor
    motor2_pwm.value = speed


def motor2_backward(speed: float):
    motor2_dir.on()
    motor2_stop.off()  # Enable motor
    motor2_pwm.value = speed


def motor2_halt():
    motor2_pwm.value = 0
    motor2_stop.on()  # Disable motor


def stop_all():
    """Emergency stop - halt all motors and spray."""
    motor1_halt()
    motor2_halt()
    spray_enable.value = 0


# =============================================================================
# AUTOMATED INSTRUCTION HANDLERS - ADD/MODIFY HANDLERS HERE
# =============================================================================


def handle_walk(quantity: float, paint: bool = True, **kwargs) -> bool:
    """Move forward. Quantity = distance in cm. Paint = spray on/off. Supports pause/resume."""
    global system_paused
    total_duration = quantity / CM_PER_SECOND
    logger.info(
        f"Walking {quantity}cm (paint={paint}) for {total_duration:.2f}s (pause/resume enabled)"
    )

    if paint:
        spray_in1.on()
        spray_in2.off()
        spray_enable.value = 1.0

    from mpu6050_gyro import get_yaw

    moved_time = 0.0
    step = 0.05  # seconds per increment
    initial_yaw = get_yaw()
    Kp = 0.05  # Proportional gain, tune for your robot
    motor1_backward(DRIVE_SPEED)  # Left wheel clockwise
    motor2_forward(DRIVE_SPEED)   # Right wheel counterclockwise
    try:
        while moved_time < total_duration:
            if system_paused:
                logger.warning(
                    f"Paused at {moved_time * CM_PER_SECOND:.1f}cm/{quantity}cm"
                )
                motor1_halt()
                motor2_halt()
                if paint:
                    spray_enable.value = 0
                # Wait until unpaused
                while system_paused:
                    time.sleep(0.1)
                logger.info("Resuming walk...")
                if paint:
                    spray_in1.on()
                    spray_in2.off()
                    spray_enable.value = 1.0
                motor1_forward(DRIVE_SPEED)
                motor2_forward(DRIVE_SPEED)
            # --- Yaw correction ---
            current_yaw = get_yaw()
            error = current_yaw - initial_yaw
            correction = Kp * error
            left_speed = max(0.0, min(1.0, DRIVE_SPEED - correction))
            right_speed = max(0.0, min(1.0, DRIVE_SPEED + correction))
            motor1_backward(left_speed)  # Left wheel clockwise
            motor2_forward(right_speed)  # Right wheel counterclockwise
            # --- End yaw correction ---
            time.sleep(step)
            moved_time += step
        motor1_halt()
        motor2_halt()
        if paint:
            spray_enable.value = 0
        return True
    except Exception as e:
        logger.error(f"Error in handle_walk: {e}")
        motor1_halt()
        motor2_halt()
        if paint:
            spray_enable.value = 0
        return False


def handle_turn(quantity: float, paint: bool = False, **kwargs) -> bool:
    """Rotate in place. Positive = counter-clockwise (left), Negative = clockwise (right). Paint = spray on/off. Supports pause/resume."""
    global system_paused
    total_duration = abs(quantity) / DEGREES_PER_SECOND
    logger.info(
        f"Turning {quantity}° (paint={paint}) for {total_duration:.2f}s (pause/resume enabled)"
    )

    if paint:
        spray_in1.on()
        spray_in2.off()
        spray_enable.value = 1.0

    turned_time = 0.0
    step = 0.05
    if quantity >= 0:  # Turn left (counter-clockwise)
        motor1_forward(TURN_SPEED)   # Left wheel counterclockwise
        motor2_forward(TURN_SPEED)   # Right wheel counterclockwise
    else:  # Turn right (clockwise)
        motor1_backward(TURN_SPEED)  # Left wheel clockwise
        motor2_backward(TURN_SPEED)  # Right wheel clockwise
    try:
        while turned_time < total_duration:
            if system_paused:
                logger.warning(
                    f"Paused at {turned_time * DEGREES_PER_SECOND:.1f}°/{abs(quantity)}°"
                )
                motor1_halt()
                motor2_halt()
                if paint:
                    spray_enable.value = 0
                while system_paused:
                    time.sleep(0.1)
                logger.info("Resuming turn...")
                if paint:
                    spray_in1.on()
                    spray_in2.off()
                    spray_enable.value = 1.0
                if quantity >= 0:
                    motor1_forward(TURN_SPEED)
                    motor2_forward(TURN_SPEED)
                else:
                    motor1_backward(TURN_SPEED)
                    motor2_backward(TURN_SPEED)
            time.sleep(step)
            turned_time += step
        motor1_halt()
        motor2_halt()
        if paint:
            spray_enable.value = 0
        return True
    except Exception as e:
        logger.error(f"Error in handle_turn: {e}")
        motor1_halt()
        motor2_halt()
        if paint:
            spray_enable.value = 0
        return False


# Remove handle_turn_left, handle_turn_right, and handle_circle (now handled by lambda in INSTRUCTION_HANDLERS)


def handle_arc(quantity: float, angle: float = 90, **kwargs) -> bool:
    """Draw an arc while spraying. Quantity = radius in cm, angle = degrees. Supports pause/resume."""
    global system_paused
    from mpu6050_gyro import get_yaw

    radius = quantity
    # If angle is not provided, default to 90 degrees arc
    if "angle" in kwargs:
        angle = kwargs["angle"]
    logger.info(
        f"Drawing arc with radius {radius}cm, angle {angle}° (pause/resume enabled, gyro correction)"
    )

    outer_radius = radius + WHEEL_BASE_CM / 2
    inner_radius = radius - WHEEL_BASE_CM / 2

    if inner_radius <= 0:
        inner_speed = 0
        outer_speed = DRIVE_SPEED
    else:
        outer_speed = DRIVE_SPEED
        inner_speed = DRIVE_SPEED * (inner_radius / outer_radius)

    # Calculate arc length and expected yaw change
    arc_length = 2 * math.pi * radius * (angle / 360)
    expected_yaw_change = angle  # degrees
    # Use outer wheel for timing
    circumference = 2 * math.pi * outer_radius
    total_duration = (circumference / CM_PER_SECOND) * (angle / 360)

    spray_in1.on()
    spray_in2.off()
    spray_enable.value = 1.0
    moved_time = 0.0
    step = 0.05
    initial_yaw = get_yaw()
    Kp = 0.05  # Tune for your robot
    motor1_backward(inner_speed)  # Left wheel clockwise
    motor2_forward(outer_speed)   # Right wheel counterclockwise
    try:
        while moved_time < total_duration:
            if system_paused:
                logger.warning(
                    f"Paused at {moved_time / total_duration * 100:.1f}% of arc"
                )
                motor1_halt()
                motor2_halt()
                spray_enable.value = 0
                while system_paused:
                    time.sleep(0.1)
                logger.info("Resuming arc...")
                spray_in1.on()
                spray_in2.off()
                spray_enable.value = 1.0
                motor1_forward(inner_speed)
                motor2_forward(outer_speed)
            # --- Yaw correction ---
            current_yaw = get_yaw()
            yaw_error = (current_yaw - initial_yaw) - (
                moved_time / total_duration
            ) * expected_yaw_change
            correction = Kp * yaw_error
            left_speed = max(0.0, min(1.0, inner_speed - correction))
            right_speed = max(0.0, min(1.0, outer_speed + correction))
            motor1_backward(left_speed)  # Left wheel clockwise
            motor2_forward(right_speed)  # Right wheel counterclockwise
            # --- End yaw correction ---
            time.sleep(step)
            moved_time += step
        motor1_halt()
        motor2_halt()
        spray_enable.value = 0
        return True
    except Exception as e:
        logger.error(f"Error in handle_arc: {e}")
        motor1_halt()
        motor2_halt()
        spray_enable.value = 0
        return False


def handle_spray_on(**kwargs) -> bool:
    """Turn spray on (forward direction)."""
    logger.info("Spray ON")
    spray_in1.on()
    spray_in2.off()
    spray_enable.value = 1.0
    return True


def handle_spray_off(**kwargs) -> bool:
    """Turn spray off."""
    logger.info("Spray OFF")
    spray_enable.value = 0
    return True


def handle_wait(quantity: float, **kwargs) -> bool:
    """Pause execution. Quantity = seconds."""
    logger.info(f"Waiting {quantity}s")
    time.sleep(quantity)
    return True


# Handler lookup - maps handler names to functions
# Add new handlers here when you create them
INSTRUCTION_HANDLERS = {
    "walk": handle_walk,
    "rotation": handle_turn,
    "circle": lambda quantity, paint=True, **kwargs: handle_arc(
        quantity, angle=180, paint=paint
    ),
}


# =============================================================================
# INSTRUCTION PARSING AND EXECUTION
# =============================================================================


def Convert_To_Array(data: dict) -> list:
    """
    Convert mobile app data to instruction list.
    Adjust INSTRUCTION_FIELDS mapping at top of file to match your app's format.

    Expected format: {'items': [{...}, {...}]}
    """
    instructions = []

    for item in data.get("items", []):
        if not item:  # Skip empty dicts
            continue

        # Extract fields using configurable field names
        order = item.get(INSTRUCTION_FIELDS["Instruction Order"], 0)
        quantity = item.get(INSTRUCTION_FIELDS["Quantity"], 0)
        move_type = item.get(INSTRUCTION_FIELDS["Type of Movement"], "").lower()
        paint = item.get(INSTRUCTION_FIELDS["Paint"], False)

        if move_type:
            instructions.append(
                {
                    "order": int(order) if order else 0,
                    "quantity": float(quantity) if quantity else 0,
                    "type": move_type,
                    "paint": bool(paint),
                }
            )

    # Sort by order
    instructions.sort(key=lambda x: x["order"])
    return instructions


def execute_instruction(instruction: dict) -> bool:
    """
    Execute a single automated instruction.
    Maps instruction type through INSTRUCTION_TYPES to find the handler.
    """
    raw_type = instruction.get("type", "").lower()
    quantity = instruction.get("quantity", 0)

    # Map the app's instruction type to internal handler name
    handler_name = INSTRUCTION_TYPES.get(raw_type, raw_type)

    # Get the handler function
    handler = INSTRUCTION_HANDLERS.get(handler_name)

    if not handler:
        logger.warning(
            f"Unknown instruction type: {raw_type} (mapped to: {handler_name})"
        )
        return False

    logger.info(f"Executing: {raw_type} -> {handler_name} (quantity={quantity})")

    try:
        return handler(
            quantity=quantity,
            paint=instruction.get("paint", False),
            angle=instruction.get("angle", 90),
            radius=instruction.get("radius", quantity),
        )
    except Exception as e:
        logger.error(f"Error executing instruction: {e}")
        stop_all()  # Safety stop on error
        return False


def execute_field_pattern(instructions: list, pause_between: float = 0.5) -> bool:
    """
    Execute a full sequence of instructions.

    Args:
        instructions: List of instruction dicts (from Convert_To_Array)
        pause_between: Seconds to pause between instructions

    Returns:
        True if all instructions succeeded, False if any failed
    """

    global current_instruction_index, total_instructions
    total_instructions = len(instructions)
    logger.info(f"Starting field pattern with {total_instructions} instructions")

    try:
        for i, instruction in enumerate(instructions):
            current_instruction_index = (
                i + 1
            )  # 1-based index for user-friendly progress
            logger.info(
                f"--- Instruction {current_instruction_index}/{total_instructions} ---"
            )
            success = execute_instruction(instruction)

            if not success:
                logger.error(
                    f"Failed at instruction {instruction.get('order', current_instruction_index)}"
                )
                stop_all()
                return False

            time.sleep(pause_between)

        logger.info("Field pattern complete!")
        current_instruction_index = total_instructions
        return True

    except KeyboardInterrupt:
        logger.warning("Pattern interrupted by user")
        stop_all()
        return False
    except Exception as e:
        logger.error(f"Pattern failed: {e}")
        stop_all()
        return False


# Progress getter for external modules
def get_instruction_progress():
    """
    Returns (current_instruction_index, total_instructions)
    """
    global current_instruction_index, total_instructions
    return current_instruction_index, total_instructions


# =============================================================================
# CALIBRATION HELPERS
# =============================================================================


def calibration_test_distance(test_seconds: float = 3.0):
    """
    Run motors for a set time to measure distance traveled.
    Measure the distance, then: CM_PER_SECOND = distance_cm / test_seconds
    """
    logger.info(f"=== DISTANCE CALIBRATION TEST ===")
    logger.info(f"Running motors at {DRIVE_SPEED} speed for {test_seconds}s")
    logger.info("Measure the distance traveled, then update CM_PER_SECOND")

    motor1_backward(DRIVE_SPEED)  # Left wheel clockwise
    motor2_forward(DRIVE_SPEED)   # Right wheel counterclockwise
    time.sleep(test_seconds)
    motor1_halt()
    motor2_halt()

    logger.info(f"Done! CM_PER_SECOND = measured_distance_cm / {test_seconds}")


def calibration_test_rotation(rotations: int = 2):
    """
    Spin the robot to measure rotation speed.
    Count full rotations, then: DEGREES_PER_SECOND = (rotations * 360) / time
    """
    test_seconds = 5.0
    logger.info(f"=== ROTATION CALIBRATION TEST ===")
    logger.info(f"Spinning at {TURN_SPEED} speed for {test_seconds}s")
    logger.info("Count full rotations, then update DEGREES_PER_SECOND")

    motor1_forward(TURN_SPEED)
    motor2_backward(TURN_SPEED)
    time.sleep(test_seconds)
    motor1_halt()
    motor2_halt()

    logger.info(f"Done! DEGREES_PER_SECOND = (rotations * 360) / {test_seconds}")


# =============================================================================
# MANUAL CONTROL (unchanged)
# =============================================================================


def translate_manual_instruction(instruction: dict) -> bool:
    command = instruction.get("command")
    state = instruction.get("state")

    if command == "forward" and state == "pressed":
        logger.info("Sync Wheels Clockwise Low PWM (Both CW)")
        sync_wheels_clockwise_low_pwm()

    elif command == "backward" and state == "pressed":
        logger.info("Move Backward")
        motor1_backward(0.8)
        motor2_backward(0.8)

    elif command == "left" and state == "pressed":
        logger.info("Turn Left (Both CCW)")
        motor1_forward(0.8)  # Left wheel counterclockwise
        motor2_forward(0.8)  # Right wheel counterclockwise

    elif command == "right" and state == "pressed":
        logger.info("Turn Right (Both CW)")
        motor1_backward(0.8)  # Left wheel clockwise
        motor2_backward(0.8)  # Right wheel clockwise

    elif command == "spray" and state == "pressed":
        logger.info("Spray On")
        spray_in1.on()
        spray_in2.off()
        spray_enable.value = 1.0

    elif command == "spray" and state == "released":
        logger.info("Spray Off")
        spray_enable.value = 0

    elif state == "released":
        logger.info("Stop")
        motor1_halt()
        motor2_halt()
    else:
        logger.warning(f"Invalid command or state: command={command}, state={state}")
        return False

    return True


if __name__ == "__main__":
    calibration_test_distance()
    calibration_test_rotation()
