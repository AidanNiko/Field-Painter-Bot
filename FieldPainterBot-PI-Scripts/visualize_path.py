import matplotlib.pyplot as plt
import math
import json

# Paste your instruction list here or load from a file
instructions = [
    {"Instruction Order": 1, "Type of Movement": "Walk", "Quantity": 13, "Paint": True},
    {
        "Instruction Order": 2,
        "Type of Movement": "Rotation",
        "Quantity": 180,
        "Paint": False,
    },
    {
        "Instruction Order": 3,
        "Type of Movement": "Walk",
        "Quantity": 13,
        "Paint": False,
    },
    {
        "Instruction Order": 4,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {
        "Instruction Order": 5,
        "Type of Movement": "Walk",
        "Quantity": 1.5,
        "Paint": True,
    },
    {
        "Instruction Order": 6,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {"Instruction Order": 7, "Type of Movement": "Walk", "Quantity": 13, "Paint": True},
    {
        "Instruction Order": 8,
        "Type of Movement": "Rotation",
        "Quantity": 180,
        "Paint": False,
    },
    {"Instruction Order": 9, "Type of Movement": "Walk", "Quantity": 7, "Paint": False},
    {
        "Instruction Order": 10,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {
        "Instruction Order": 11,
        "Type of Movement": "Walk",
        "Quantity": 4.5,
        "Paint": True,
    },
    {
        "Instruction Order": 12,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {"Instruction Order": 13, "Type of Movement": "Walk", "Quantity": 7, "Paint": True},
    {
        "Instruction Order": 14,
        "Type of Movement": "Rotation",
        "Quantity": 180,
        "Paint": False,
    },
    {
        "Instruction Order": 15,
        "Type of Movement": "Walk",
        "Quantity": 7,
        "Paint": False,
    },
    {
        "Instruction Order": 16,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {
        "Instruction Order": 17,
        "Type of Movement": "Walk",
        "Quantity": 4.5,
        "Paint": True,
    },
    {
        "Instruction Order": 18,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {"Instruction Order": 19, "Type of Movement": "Walk", "Quantity": 7, "Paint": True},
    {
        "Instruction Order": 20,
        "Type of Movement": "Rotation",
        "Quantity": 180,
        "Paint": False,
    },
    {
        "Instruction Order": 21,
        "Type of Movement": "Walk",
        "Quantity": 7,
        "Paint": False,
    },
    {"Instruction Order": 22, "Type of Movement": "Walk", "Quantity": 6, "Paint": True},
    {
        "Instruction Order": 23,
        "Type of Movement": "Rotation",
        "Quantity": -90,
        "Paint": False,
    },
    {
        "Instruction Order": 24,
        "Type of Movement": "Walk",
        "Quantity": 9,
        "Paint": False,
    },
    {
        "Instruction Order": 25,
        "Type of Movement": "Rotation",
        "Quantity": 180,
        "Paint": False,
    },
    {
        "Instruction Order": 26,
        "Type of Movement": "Walk",
        "Quantity": 10.5,
        "Paint": True,
    },
    {
        "Instruction Order": 27,
        "Type of Movement": "Rotation",
        "Quantity": 90,
        "Paint": False,
    },
    {
        "Instruction Order": 28,
        "Type of Movement": "Walk",
        "Quantity": 13,
        "Paint": True,
    },
]


# Initial position and heading (facing right, 0 degrees)
x, y, heading = 0, 0, 0
xs, ys = [x], [y]
labels = []  # To store (x, y, label) for annotation

for idx, instr in enumerate(instructions):
    try:
        move = instr["Type of Movement"].lower()
        qty = instr["Quantity"]
        order = instr.get("Instruction Order", idx + 1)
    except Exception as e:
        print(f"Skipping instruction at index {idx} due to missing fields: {e}")
        continue

    try:
        if move == "walk":
            # Move forward by qty units
            x += qty * math.cos(math.radians(heading))
            y += qty * math.sin(math.radians(heading))
            xs.append(x)
            ys.append(y)
            # Annotate with instruction number at the start of each segment
            labels.append((x, y, str(order)))
        elif move == "rotation":
            # Positive = clockwise (right), negative = counterclockwise (left)
            heading -= qty  # Subtract for positive (CW), add for negative (CCW)
        elif move == "circle":
            # Draw a half-circle (arc) with given radius
            circle_radius = abs(qty)  # Quantity is radius
            arc_points = 30
            direction = -1 if qty > 0 else 1  # CW if positive, CCW if negative
            for i in range(arc_points + 1):
                angle = heading + direction * (i * 180 / arc_points)
                arc_x = x + circle_radius * math.cos(math.radians(angle))
                arc_y = y + circle_radius * math.sin(math.radians(angle))
                xs.append(arc_x)
                ys.append(arc_y)
            # Update heading after arc
            heading += direction * 180
        else:
            print(f"Unknown movement type '{move}' at instruction {order}, skipping.")
    except Exception as e:
        print(f"Error processing instruction {order}: {e}")
        continue

plt.plot(xs, ys, marker="o")
plt.axis("equal")
plt.title("Robot Path Simulation")
plt.xlabel("X")
plt.ylabel("Y")
for xlbl, ylbl, lbl in labels:
    plt.text(xlbl, ylbl, lbl, fontsize=8, color="red", ha="center", va="center")
plt.show()
