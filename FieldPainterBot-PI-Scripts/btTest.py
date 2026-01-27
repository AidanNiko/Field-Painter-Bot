from bluetooth import *

server_sock = BluetoothSocket(RFCOMM)
server_sock.bind(("", PORT_ANY))
server_sock.listen(1)

uuid = "00001101-0000-1000-8000-00805F9B34FB"

'''
advertise_service(
    server_sock,
    "PiSerialService",
    service_id=uuid,
    service_classes=[uuid, SERIAL_PORT_CLASS],
    profiles=[SERIAL_PORT_PROFILE]
)
'''

print("Waiting for connection on RFCOMM channel")
client_sock, client_info = server_sock.accept()
print("Accepted connection from", client_info)
