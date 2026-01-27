import RPi.GPIO as GPIO
import time

#PINOUTS FOR THE MOTORS
L_Motor = 12
R_Motor = 13
L_Dir = 16
R_Dir = 26

GPIO.setwarnings(False)
GPIO.setmode(GPIO.BOARD)
GPIO.setup(L_Motor, GPIO.OUT)
GPIO.setup(R_Motor, GPIO.OUT)
GPIO.setup(L_Dir, GPIO.OUT)
GPIO.setup(R_Dir, GPIO.OUT)

pwm_L = GPIO.PWM(L_Motor, 100) #set both as PWM pins
pwm_R = GPIO.PWM(R_Motor, 100)
pwm_L.start(0) #start both, no speed
pwm_R.start(0)

GPIO.output(L_Dir, GPIO.HIGH)
GPIO.output(R_Dir, GPIO.HIGH)

print("L Motor High\tR Motor Low\n")
pwm_L.ChangeDutyCycle(100)
time.sleep(5)


print("L Motor Low\tR Motor High\n")
pwm_L.ChangeDutyCycle(0)
pwm_R.ChangeDutyCycle(100)
time.sleep(5)

print("L Motor High\tR Motor High\n")
pwm_L.ChangeDutyCycle(100)
time.sleep(5)

print("L Motor Low\tR Motor Low\n")
pwm_L.ChangeDutyCycle(0)
pwm_L.ChangeDutyCycle(0)
time.sleep(5)

print("L Motor Accelerate\tR Motor Low\n")
for i in range(0, 100, 1):
    pwm_L.ChangeDutyCycle(i/100)
    time.sleep(0.05)

print("L Motor Decelerate\tR Motor Accelerate\n")
for i in range(0,100,1):
    pwm_R.ChangeDutyCycle(i/100)
    pwm_L.ChangeDutyCycle((100-i)/100)
    time.sleep(0.05)

print("L Motor Low\tR Motor Decelerate\n")
for i in range(0,100,1):
    pwm_R.ChangeDutyCycle((100-i)/100)
    time.sleep(0.05)

GPIO.output(L_Dir,GPIO.LOW)
print("L Motor Reverse\tR Motor High\n")
pwm_R.ChangeDutyCycle(33)
pwm_L.ChangeDutyCycle(33)
time.sleep(5)

pwm_R.ChangeDutyCycle(0)
time.sleep(1)
GPIO.output(R_Dir,GPIO.LOW)
print("L Motor Reverse\t R Motor Reverse\n")
pwm_R.ChangeDutyCycle(33)
time.sleep(5)

pwm_R.ChangeDutyCycle(0)
pwm_L.ChangeDutyCycle(0)
