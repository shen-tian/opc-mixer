import SocketServer
import struct

class OpcRequestHandler(SocketServer.BaseRequestHandler):

    def handle(self):
        print "Connected from", self.client_address

        try:
            new_conn = True

            while True:
                header = self.request.recv(4)
                if header == '':
                    break;
                (channel, cmd, n) = struct.unpack('>BBH', header)

                if cmd == 0:
                    if new_conn:
                        print n/3
                        new_conn = False

                data = self.request.recv(n)
        finally:
            print "Fin"
            return



class Server(object):

    def __init__(self, port):
        address = ('0.0.0.0', 7890)
        server = SocketServer.TCPServer(address, OpcRequestHandler)

        server.serve_forever()


print "Hello world"
Server(7890)
