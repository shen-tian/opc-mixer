import SocketServer
import socket
import time
import math
import struct


class OpcRequestHandler(SocketServer.BaseRequestHandler):

    def __init__(self, callback, *args, **keys):
        self.callback = callback
        SocketServer.BaseRequestHandler.__init__(self, *args, **keys)

    def handle(self):
        print "Connected from", self.client_address

        try:
            new_conn = True

            while True:
                header = self.request.recv(4)
                if header == '':
                    break;
                (channel, cmd, n) = struct.unpack('!BBH', header)

                frame = bytearray(self.request.recv(n))

                if new_conn & (cmd == 0):
                    print n
                    new_conn = False

                self.callback(header, frame)

        finally:
            print "Fin"
            return


class Server(object):

    def process_frame(self, header, frame):
        (channel, cmd, n) = struct.unpack('!BBH', header)

        if cmd == 0:

            millis = int(round(time.time() * 1000))

            scale = (math.sin(millis * .001) + 1) * .5

            new_frame = []

            for i in range(0, n/3):
                r = frame[i * 3]
                g = frame[i * 3 + 1]
                b = frame[i * 3 + 2]

                r = int(r * scale)
                g = int(g * scale)
                # b = int(b * scale)

                new_frame.append(chr(r) + chr(g) + chr(b))

            self._outsocket.send(header)
            self._outsocket.send(''.join(new_frame))

    def __init__(self, port):

        self._outsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._outsocket.settimeout(1)
        self._outsocket.connect(('127.0.0.1', port))

        def handler_factory(callback):
            def createHandler(*args, **keys):
                return OpcRequestHandler(callback, *args, **keys)

            return createHandler

        address = ('0.0.0.0', 7890)
        server = SocketServer.ThreadingTCPServer(address, handler_factory(self.process_frame))

        server.serve_forever()

Server(7891)
