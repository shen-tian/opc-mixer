import SocketServer
import socket
import time
import math
import struct


class OpcRequestHandler(SocketServer.BaseRequestHandler):

    def setup(self):
        print "Setup"

    def handle(self):
        print "Connected from", self.client_address

        try:
            new_conn = True

            out_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            out_socket.settimeout(1)
            out_socket.connect(('127.0.0.1', 7891))

            while True:
                header = self.request.recv(4)
                if header == '':
                    break;
                (channel, cmd, n) = struct.unpack('!BBH', header)

                frame = bytearray(self.request.recv(n))

                if cmd == 0:
                    if new_conn:
                        print n/3
                        new_conn = False

                    millis = int(round(time.time() * 1000))

                    scale = (math.sin(millis * .001) + 1) * .5
                    #print scale
                    new_frame = []

                    for i in range(0, 1560):
                        r = frame[i * 3]
                        g = frame[i * 3 + 1]
                        b = frame[i * 3 + 2]

                        r = int(r * scale)
                        g = int(g * scale)
                        #b = int(b * scale)

                        new_frame.append(chr(r) + chr(g) + chr(b))

                    out_socket.send(header)
                    out_socket.send(''.join(new_frame))

        finally:
            print "Fin"
            return


class Server(object):

    def __init__(self, port):

        address = ('0.0.0.0', 7890)
        server = SocketServer.ThreadingTCPServer(address, OpcRequestHandler)

        server.serve_forever()

Server(7890)
