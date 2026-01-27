import bluetooth
import time
n = bluetooth.discover_devices(lookup_names=True)
print("Found {} devices".format(len(n)))
for addr, name in n:
    print("{}-{}".format(addr,name))
