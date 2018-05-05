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

    def disconnect(self):
        self.low_level_com.disconnect()

    def communicate(self):
        self.low_level_com.communicate()
        while self.low_level_com.available() > 0:
            message = self.low_level_com.getLastMessage()
            print "Received: " + str(message)

