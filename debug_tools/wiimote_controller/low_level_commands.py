from low_level_com import LowLevelCom, Message


class LowLevelCommands:
    def __init__(self):
        self.low_level_com = LowLevelCom()

    def connect(self):
        self.low_level_com.connect()
        while self.low_level_com.connectionSuccess is None:
            pass
        if not self.low_level_com.connectionSuccess:
            print "Failed to connect to low-level"
            raise OSError

        # Unsubscribe from all channels
        for i in range(12):
            message = Message(i, bytes([0]))
            self.low_level_com.sendMessage(message)

        self.receptionThread = threading.Thread(target=self._backgroundReception)

    def disconnect(self):
        self.low_level_com.disconnect()

    def communicate(self):
        self.low_level_com.communicate()
        while self.low_level_com.available() > 0:
            message = self.low_level_com.getLastMessage()
            print "Received: " + str(message)

    def _backgroundReception(self):
        print("TCP/IP _backgroundReception start")
        while self.isOpen:
            try:
                b = bytearray(self.socket.recv(4096))
                self.rBuffer += b
                print b # TODO à faire ?
            except ConnectionAbortedError:
                pass
        print("TCP/IP _backgroundReception end")


    def pull_down_net(self):
        pass

    def pull_up_net(self):
        pass

    def close_net(self):
        pass

    def open_net(self):
        pass

    def eject_right_side(self):
        pass

    def eject_left_side(self):
        pass

    def rearm_right_side(self):
        pass

    def rearm_left_side(self):
        pass

    def robot_stop(self):
        sendOrder(0x21, [])

    def start_manual_mode(self):
        sendOrder(0x97, [])

    def set_speed(self, speed):
        sendOrder(0x21, ["<f"], speed)

    def set_direction(self, direction):
        sendOrder(0x9B, ["<f"], direction)

    def sendOrder(self, id, command, args):
        try:
           if len(command) != len(args):
                raise ValueError("Invalid order to send")
            messageData = bytearray()
            i = 0
            for field in command:
                if field.type == int:
                    fmt = "<i"
                    val = int(args[i])
                elif field.type == float:
                    fmt = "<f"
                    val = float(args[i])
                elif field.type == bool:
                    fmt = "<?"
                    val = bool(args[i])
                elif field.type == Enum:
                    fmt = "<B"
                    val = field.legend.index(args[i])
                else:
                    raise ValueError("Invalid type in order to send")
                messageData += struct.pack(fmt, val)
                i += 1
            message = Message(command, bytes(messageData))
            sendMessage(message)
        except (ValueError, struct.error) as e:
            print(e)

