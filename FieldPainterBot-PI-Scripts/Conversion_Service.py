import logging
import time
import math
from gpiozero import PWMOutputDevice, DigitalOutputDevice


# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# =============================================================================
# CALIBRATION CONSTANTS - ADJUST THESE FOR YOUR ROBOT
# =============================================================================

# Wheel and chassis measurements
WHEEL_DIAMETER_CM = 16.5      # 6.5 inch wheels = 16.5cm (use 20.3 for 8 inch)
WHEEL_BASE_CM = 50.0          # Distance between wheel centers - MEASURE THIS

# Motor speed settings (hoverboard motors are fast, keep these low for accuracy)
DRIVE_SPEED = 0.3             # Speed for straight movement (0.0 - 1.0)
TURN_SPEED = 0.25             # Speed for turning in place

# Calibration values - TUNE THESE BY TESTING
# Run calibration_test() to measure actual values
CM_PER_SECOND = 50.0          # How far robot travels per second at DRIVE_SPEED
DEGREES_PER_SECOND = 90.0     # How fast robot rotates per second at TURN_SPEED

# Instruction type mappings - EASILY CHANGE THESE IF YOUR APP USES DIFFERENT NAMES
INSTRUCTION_TYPES = {
    # Maps your app's instruction names to internal handler names
    # Format: 'app_name': 'handler_name'
    'walk': 'walk',           # Move forward (Paint field controls spray)
    'rotation': 'rotation',   # Rotate in place (Quantity = degrees)
}

# Instruction data field mappings - CHANGE THESE TO MATCH YOUR APP'S JSON FORMAT
INSTRUCTION_FIELDS = {
    'order': 'Instruction Order',     # Field name for instruction order
    'quantity': 'Quantity',           # Field name for quantity (distance cm or degrees)
    'type': 'Type of Movement',       # Field name for instruction type
    'paint': 'Paint',                 # Field name for spray on/off (boolean)
}


# =============================================================================


# Motor 1 - BLDC Driver (ZS-X11H)
motor1_pwm = PWMOutputDevice(17, frequency=1000)    # P pin - Speed control
motor1_dir = DigitalOutputDevice(27)                 # DIR pin - Direction
motor1_stop = DigitalOutputDevice(22, initial_value=False)  # STOP pin (LOW=run)

# Motor 2 - BLDC Driver (ZS-X11H)
motor2_pwm = PWMOutputDevice(23, frequency=1000)    # P pin - Speed control
motor2_dir = DigitalOutputDevice(24)                 # DIR pin - Direction
motor2_stop = DigitalOutputDevice(5, initial_value=False)   # STOP pin (LOW=run)

# Spray
spray = PWMOutputDevice(25, frequency=50)


# Motor control helper functions
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
    spray.value = 0


# =============================================================================
# AUTOMATED INSTRUCTION HANDLERS - ADD/MODIFY HANDLERS HERE
# =============================================================================

def handle_walk(quantity: float, paint: bool = True, **kwargs) -> bool:
    """Move forward. Quantity = distance in cm. Paint = spray on/off."""
    duration = quantity / CM_PER_SECOND
    logger.info(f"Walking {quantity}cm (paint={paint}) for {duration:.2f}s")
    
    if paint:
        spray.value = 1.0
    
    motor1_forward(DRIVE_SPEED)
    motor2_forward(DRIVE_SPEED)
    time.sleep(duration)
    motor1_halt()
    motor2_halt()
    
    if paint:
        spray.value = 0
    return True


def handle_move(quantity: float, **kwargs) -> bool:
    """Move forward without spraying. Quantity = distance in cm."""
    duration = quantity / CM_PER_SECOND
    logger.info(f"Moving {quantity}cm (no spray) for {duration:.2f}s")
    
    motor1_forward(DRIVE_SPEED)
    motor2_forward(DRIVE_SPEED)
    time.sleep(duration)
    motor1_halt()
    motor2_halt()
    return True


def handle_turn(quantity: float, paint: bool = False, **kwargs) -> bool:
    """Rotate in place. Positive = counter-clockwise (left), Negative = clockwise (right). Paint = spray on/off."""
    duration = abs(quantity) / DEGREES_PER_SECOND
    logger.info(f"Turning {quantity}° (paint={paint}) for {duration:.2f}s")
    
    if paint:
        spray.value = 1.0
    
    if quantity >= 0:  # Turn left (counter-clockwise)
        motor1_backward(TURN_SPEED)
        motor2_forward(TURN_SPEED)
    else:  # Turn right (clockwise)
        motor1_forward(TURN_SPEED)
        motor2_backward(TURN_SPEED)
    
    time.sleep(duration)
    motor1_halt()
    motor2_halt()
    
    if paint:
        spray.value = 0
    return True


def handle_turn_left(quantity: float = 90, **kwargs) -> bool:
    """Turn left. Quantity = degrees (default 90)."""
    return handle_turn(-abs(quantity), **kwargs)


def handle_turn_right(quantity: float = 90, **kwargs) -> bool:
    """Turn right. Quantity = degrees (default 90)."""
    return handle_turn(abs(quantity), **kwargs)


def handle_circle(quantity: float, **kwargs) -> bool:
    """Draw a circle while spraying. Quantity = radius in cm."""
    radius = quantity
    logger.info(f"Drawing circle with radius {radius}cm")
    
    # Calculate differential speeds for circle
    # Outer wheel travels further than inner wheel
    outer_radius = radius + WHEEL_BASE_CM / 2
    inner_radius = radius - WHEEL_BASE_CM / 2
    
    if inner_radius <= 0:
        # Radius too small, pivot around inner wheel
        inner_speed = 0
        outer_speed = DRIVE_SPEED
    else:
        outer_speed = DRIVE_SPEED
        inner_speed = DRIVE_SPEED * (inner_radius / outer_radius)
    
    # Time for full circle based on outer wheel
    circumference = 2 * math.pi * outer_radius
    duration = circumference / CM_PER_SECOND
    
    spray.value = 1.0
    motor1_forward(inner_speed)   # Left/inner wheel
    motor2_forward(outer_speed)   # Right/outer wheel
    time.sleep(duration)
    motor1_halt()
    motor2_halt()
    spray.value = 0
    return True


def handle_arc(quantity: float, angle: float = 90, **kwargs) -> bool:
    """Draw an arc while spraying. Quantity = radius in cm, angle = degrees."""
    radius = quantity
    logger.info(f"Drawing arc with radius {radius}cm, angle {angle}°")
    
    # Same as circle but for partial rotation
    outer_radius = radius + WHEEL_BASE_CM / 2
    inner_radius = radius - WHEEL_BASE_CM / 2
    
    if inner_radius <= 0:
        inner_speed = 0
        outer_speed = DRIVE_SPEED
    else:
        outer_speed = DRIVE_SPEED
        inner_speed = DRIVE_SPEED * (inner_radius / outer_radius)
    
    # Time for arc (fraction of full circle)
    circumference = 2 * math.pi * outer_radius
    duration = (circumference / CM_PER_SECOND) * (angle / 360)
    
    spray.value = 1.0
    motor1_forward(inner_speed)
    motor2_forward(outer_speed)
    time.sleep(duration)
    motor1_halt()
    motor2_halt()
    spray.value = 0
    return True


def handle_spray_on(**kwargs) -> bool:
    """Turn spray on."""
    logger.info("Spray ON")
    spray.value = 1.0
    return True


def handle_spray_off(**kwargs) -> bool:
    """Turn spray off."""
    logger.info("Spray OFF")
    spray.value = 0
    return True


def handle_wait(quantity: float, **kwargs) -> bool:
    """Pause execution. Quantity = seconds."""
    logger.info(f"Waiting {quantity}s")
    time.sleep(quantity)
    return True


# Handler lookup - maps handler names to functions
# Add new handlers here when you create them
INSTRUCTION_HANDLERS = {
    'walk': handle_walk,
    'rotation': handle_turn,
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
    
    for item in data.get('items', []):
        if not item:  # Skip empty dicts
            continue
            
        # Extract fields using configurable field names
        order = item.get(INSTRUCTION_FIELDS['order'], 0)
        quantity = item.get(INSTRUCTION_FIELDS['quantity'], 0)
        move_type = item.get(INSTRUCTION_FIELDS['type'], '').lower()
        paint = item.get(INSTRUCTION_FIELDS['paint'], False)
        radius = item.get(INSTRUCTION_FIELDS['radius'], 0)
        angle = item.get(INSTRUCTION_FIELDS['angle'], 90)
        
        if move_type:
            instructions.append({
                'order': int(order) if order else 0,
                'quantity': float(quantity) if quantity else 0,
                'type': move_type,
                'paint': bool(paint),
                'radius': float(radius) if radius else 0,
                'angle': float(angle) if angle else 90,
            })
    
    # Sort by order
    instructions.sort(key=lambda x: x['order'])
    return instructions


def execute_instruction(instruction: dict) -> bool:
    """
    Execute a single automated instruction.
    Maps instruction type through INSTRUCTION_TYPES to find the handler.
    """
    raw_type = instruction.get('type', '').lower()
    quantity = instruction.get('quantity', 0)
    
    # Map the app's instruction type to internal handler name
    handler_name = INSTRUCTION_TYPES.get(raw_type, raw_type)
    
    # Get the handler function
    handler = INSTRUCTION_HANDLERS.get(handler_name)
    
    if not handler:
        logger.warning(f"Unknown instruction type: {raw_type} (mapped to: {handler_name})")
        return False
    
    logger.info(f"Executing: {raw_type} -> {handler_name} (quantity={quantity})")
    
    try:
        return handler(
            quantity=quantity,
            paint=instruction.get('paint', False),
            angle=instruction.get('angle', 90),
            radius=instruction.get('radius', quantity),
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
    logger.info(f"Starting field pattern with {len(instructions)} instructions")
    
    try:
        for i, instruction in enumerate(instructions):
            logger.info(f"--- Instruction {i+1}/{len(instructions)} ---")
            success = execute_instruction(instruction)
            
            if not success:
                logger.error(f"Failed at instruction {instruction.get('order', i+1)}")
                stop_all()
                return False
            
            time.sleep(pause_between)
        
        logger.info("Field pattern complete!")
        return True
        
    except KeyboardInterrupt:
        logger.warning("Pattern interrupted by user")
        stop_all()
        return False
    except Exception as e:
        logger.error(f"Pattern failed: {e}")
        stop_all()
        return False


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
    
    motor1_forward(DRIVE_SPEED)
    motor2_forward(DRIVE_SPEED)
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
        logger.info("Move Forward")
        motor1_forward(0.8)
        motor2_forward(0.8)

    elif command == "backward" and state == "pressed":
        logger.info("Move Backward")
        motor1_backward(0.8)
        motor2_backward(0.8)

    elif command == "left" and state == "pressed":
        logger.info("Turn Left")
        motor1_forward(0.3)  # Left motor slower
        motor2_forward(0.8)  # Right motor faster

    elif command == "right" and state == "pressed":
        logger.info("Turn Right")
        motor1_forward(0.8)  # Left motor faster
        motor2_forward(0.3)  # Right motor slower

    elif command == "spray" and state == "pressed":
        logger.info("Spray On")
        spray.value = 1.0

    elif command == "spray" and state == "released":
        logger.info("Spray Off")
        spray.value = 0.0

    elif state == "released":
        logger.info("Stop")
        motor1_halt()
        motor2_halt()
    else:
        logger.warning(f"Invalid command or state: command={command}, state={state}")
        return False

    return True


# For testing locally
""" if __name__ == "__main__":
    import uvicorn

    uvicorn.run("Conversion_Service:app", host="127.0.0.1", port=8000, reload=True) """

# For Reference
"""{'items': [{'Instruction Order': '1', 'Quantity of Movement': '50', 'Type of Movement': 'walk'}, 
{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, 
{'Instruction Order': '2', 'Quantity of Movement': '22', 'Type of Movement': 'Circle'}, 
{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}]}"""
