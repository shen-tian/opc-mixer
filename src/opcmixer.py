import SocketServer
import socket
import threading
import time
import math
import struct


class OpcRequestHandler(SocketServer.BaseRequestHandler):

    def __init__(self, callback, source_id, *args, **keys):
        self.callback = callback
        self.id = source_id
        SocketServer.BaseRequestHandler.__init__(self, *args, **keys)

    def handle(self):
        print "Connected from", self.client_address
        print "Source", self.id

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


                self.callback(self.id, header, frame)

        finally:
            print "Fin"
            return


class Server(object):

    def process_frame(self, source, header, frame):

        if source == 1:
            self.a_frame = frame
        else:
            self.b_frame = frame

        (channel, cmd, n) = struct.unpack('!BBH', header)

        if cmd == 0:

            new_frame = []

            if (self.a_frame is not None) & (self.b_frame is not None):
                for i in range(0, n/3):
                    r_a = self.a_frame[i * 3]
                    g_a = self.a_frame[i * 3 + 1]
                    b_a = self.a_frame[i * 3 + 2]

                    r_b = self.b_frame[i * 3]
                    g_b = self.b_frame[i * 3 + 1]
                    b_b = self.b_frame[i * 3 + 2]

                    r = int(r_a + r_b)/2
                    g = int(g_a + g_b)/2
                    b = int(b_a + b_b)/2

                    new_frame.append(chr(r) + chr(g) + chr(b))
            else:
                for i in range(0, n/3):
                    r = frame[i * 3]
                    g = frame[i * 3 + 1]
                    b = frame[i * 3 + 2]

                    new_frame.append(chr(r) + chr(g) + chr(b))

            self.lock.acquire()
            self._outsocket.send(header)
            self._outsocket.send(''.join(new_frame))
            self.lock.release()

    def __init__(self, port):

        self._source = 0

        self.lock = threading.Lock()

        self.a_frame = None
        self.b_frame = None

        self._outsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._outsocket.settimeout(1)
        self._outsocket.connect(('127.0.0.1', port))

        def handler_factory(callback):
            def createHandler(*args, **keys):
                source_id = self._source
                self._source += 1
                return OpcRequestHandler(callback, source_id, *args, **keys)

            return createHandler

        address = ('0.0.0.0', 7890)
        server = SocketServer.ThreadingTCPServer(address, handler_factory(self.process_frame))

        server.serve_forever()

Server(7891)
