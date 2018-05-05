import socket, threading

DEFAULT_ROBOT_IP = "172.16.0.2"
ROBOT_TCP_PORT = 80
CONNEXION_TIMEOUT = 2  # seconds

class LowLevelCom:
    def __init__(self):
        self.tcp_ip_interface = None
        self.messageBuffer = []
        self.rBuffer = bytearray()
        self.readingMsg = False
        self.currentMsgId = None
        self.currentMsgLength = None
        self.currentMgsData = []
        self.connectionSuccess = None

    def connect(self, ip=DEFAULT_ROBOT_IP):
        self.tcp_ip_interface = TCPIP_interface()
        port = ip
        self.connectionSuccess = None
        connectThread = threading.Thread(target=self.bg_connect, args=(port,))
        connectThread.start()

    def bg_connect(self, port):
        self.connectionSuccess = self.tcp_ip_interface.open(port)
        if not self.connectionSuccess:
            self.tcp_ip_interface = None

    def disconnect(self):
        if self.tcp_ip_interface is not None:
            self.tcp_ip_interface.close()
            self.tcp_ip_interface = None
            self.connectionSuccess = None

    def sendMessage(self, message):
        if self.tcp_ip_interface is not None:
            b = bytearray([0xFF, message.id])
            if message.standard:
                b += bytearray([len(message.data)])
            else:
                b += bytearray([0xFF])
            b += bytearray(message.data)
            self.tcp_ip_interface.sendBytes(b)

    def available(self):
        return len(self.messageBuffer)

    def getLastMessage(self):
        message = self.messageBuffer[0]
        self.messageBuffer = self.messageBuffer[1:]
        return message

    def communicate(self):
        if self.tcp_ip_interface is not None:
            try:
                avail = self.tcp_ip_interface.available()
                if avail > 0:
                    self.rBuffer += bytearray(self.tcp_ip_interface.read(avail))
                while len(self.rBuffer) > 0:
                    byte = self.rBuffer[0]
                    self.rBuffer = self.rBuffer[1:]
                    endReached = False
                    if not self.readingMsg:
                        if byte == 0xFF:
                            self.readingMsg = True
                        else:
                            print("Received incorrect byte:", byte)
                    elif self.currentMsgId is None:
                        self.currentMsgId = byte
                    elif self.currentMsgLength is None:
                        self.currentMsgLength = byte
                        if byte == 0:
                            endReached = True
                    else:
                        self.currentMgsData.append(byte)
                        if self.currentMsgLength == 0xFF and byte == 0x00:
                            endReached = True
                        elif self.currentMsgLength != 0xFF and len(self.currentMgsData) == self.currentMsgLength:
                            endReached = True
                    if endReached:
                        try:
                            message = Message(self.currentMsgId, bytes(self.currentMgsData), self.currentMsgLength != 0xFF)
                            self.messageBuffer.append(message)
                        except ValueError:
                            print("Incoherent frame received")
                        self.readingMsg = False
                        self.currentMsgId = None
                        self.currentMsgLength = None
                        self.currentMgsData = []
            except IOError:
                self.disconnect()
                raise


class Message:
    def __init__(self, ID, data=bytes(), standard=True):
        if isinstance(ID, int) and 0 <= ID < 256 and isinstance(data, bytes) and isinstance(standard, bool):
            self.id = ID
            self.data = data
            self.standard = standard
            if not standard and data[len(data) - 1] != 0:
                raise ValueError
        else:
            print("ERR - id=", ID, "data=", data, "std=", standard)
            raise ValueError

    def __str__(self):
        return "id=" + str(self.id) + " data=" + self.data.decode('utf-8', errors='ignore') + " std=" + str(self.standard)


class TCPIP_interface:
    def __init__(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.rBuffer = bytearray()
        self.receptionThread = threading.Thread(target=self._backgroundReception)
        self.isOpen = False

    def open(self, ip):
        if self.isOpen:
            return False
        else:
            try:
                self.socket.settimeout(CONNEXION_TIMEOUT)
                self.socket.connect((ip, ROBOT_TCP_PORT))
                self.isOpen = True
                self.socket.setblocking(True)
                self.receptionThread.start()
                return True
            except (socket.timeout, OSError) as e:
                print("[CONNEXION ERROR]", e)
                return False

    def close(self):
        if self.isOpen:
            self.isOpen = False
            self.socket.close()
            self.receptionThread.join()

    def sendBytes(self, b):
        if len(b) > 0:
            nbSent = self.socket.send(b)
            if nbSent == 0:
                raise OSError
            elif nbSent < len(b):
                self.sendBytes(b[nbSent:])

    def available(self):
        return len(self.rBuffer)

    def read(self, nbBytes):
        ret = bytes(self.rBuffer[0:nbBytes])
        self.rBuffer = self.rBuffer[nbBytes:]
        return ret

    def _backgroundReception(self):
        print("TCP/IP _backgroundReception start")
        while self.isOpen:
            try:
                b = bytearray(self.socket.recv(4096))
                self.rBuffer += b
            except OSError:
                pass
        print("TCP/IP _backgroundReception end")
