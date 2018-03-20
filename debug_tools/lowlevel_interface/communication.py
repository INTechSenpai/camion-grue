import socket, threading, serial, time

DEFAULT_ROBOT_IP = "172.16.0.2"
ROBOT_TCP_PORT = 80
CONNEXION_TIMEOUT = 5  # seconds
ORIGIN_TIMESTAMP = int(time.time() * 1000)


class Communication:
    def __init__(self):
        self.abstract_interface = None
        self.messageBuffer = []
        self.rBuffer = bytearray()
        self.readingMsg = False
        self.currentMsgId = None
        self.currentMsgLength = None
        self.currentMgsData = []
        self.connectionSuccess = None

    def connect(self, ip=None, com=None):
        if ip is not None and com is None:
            self.abstract_interface = TCPIP_interface()
            port = ip
        elif ip is None and com is not None:
            self.abstract_interface = Serial_interface()
            port = com
        else:
            self.connectionSuccess = False
            return
        self.connectionSuccess = None
        connectThread = threading.Thread(target=self.bg_connect, args=(port,))
        connectThread.start()

    def bg_connect(self, port):
        self.connectionSuccess = self.abstract_interface.open(port)
        if not self.connectionSuccess:
            self.abstract_interface = None

    def disconnect(self):
        if self.abstract_interface is not None:
            self.abstract_interface.close()
            self.abstract_interface = None
            self.connectionSuccess = None

    def sendMessage(self, message):
        if self.abstract_interface is not None:
            b = bytearray([0xFF, message.id])
            if message.standard:
                b += bytearray([len(message.data)])
            else:
                b += bytearray([0xFF])
            b += bytearray(message.data)
            self.abstract_interface.sendBytes(b)
            print("send: ", b)

    def available(self):
        return len(self.messageBuffer)

    def getLastMessage(self):
        message = self.messageBuffer[0]
        self.messageBuffer = self.messageBuffer[1:]
        return message

    def communicate(self):
        if self.abstract_interface is not None:
            try:
                avail = self.abstract_interface.available()
                if avail > 0:
                    self.rBuffer += bytearray(self.abstract_interface.read(avail))
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
            self.timestamp = int(1000*time.time() - ORIGIN_TIMESTAMP)
            self.standard = standard
            if not standard and data[len(data) - 1] != 0:
                raise ValueError
        else:
            print("ERR - id=", ID, "data=", data, "std=", standard)
            raise ValueError


class Serial_interface:
    def __init__(self):
        self.serial = serial.Serial()
        self.serial.timeout = CONNEXION_TIMEOUT

    def open(self, port):
        self.serial.port = port
        try:
            self.serial.open()
            return True
        except IOError:
            return False

    def close(self):
        self.serial.close()

    def sendBytes(self, b):
        self.serial.write(b)

    def available(self):
        if self.serial.is_open:
            return self.serial.in_waiting
        else:
            return 0

    def read(self, nbBytes):
        return self.serial.read(nbBytes)


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
            except (socket.timeout, ConnectionRefusedError) as e:
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
            except ConnectionAbortedError:
                pass
        print("TCP/IP _backgroundReception end")
