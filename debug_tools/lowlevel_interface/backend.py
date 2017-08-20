from communication import Communication, Message
from CommandList import *
import time, traceback
from PyQt5.QtCore import QTimer


class Backend:
    def __init__(self):
        self.communication = Communication()

        self.runInBackground = True
        self.backgroundTask = QTimer()
        self.backgroundTask.setInterval(10)
        self.backgroundTask.setSingleShot(False)
        self.backgroundTask.timeout.connect(self.main_loop)

        self.lastRefreshTime = 0.0
        self.connecting = False

        self.toolbar = None
        self.console = None
        self.graph = None

        self.consoleEntries = []
        self.curveGraphEntries = []
        self.scatterGraphEntries = []
        self.splitters = []
        self.needUpdateConsole = False
        self.needUpdateCurveGraph = False
        self.needUpdateScatterGraph = False

        self.currentTime = 0    # ms
        self.tMin = None        # ms
        self.tMax = 0           # ms
        self.zoom = 0           # ms

        self.command_from_id = {}
        for command in COMMAND_LIST:
            self.command_from_id[command.id] = command

    def setGraphicalInterface(self, toolbar, console, graph):
        self.toolbar = toolbar
        self.toolbar.setTimeBounds(0, 0)
        self.console = console
        self.graph = graph

    def startBackgroundTask(self):
        self.backgroundTask.start()

    def main_loop(self):
        self.communication.communicate()
        if self.connecting:
            if self.communication.connectionSuccess is not None:
                if self.communication.connectionSuccess:
                    self.toolbar.connexionSucceeded()
                else:
                    self.toolbar.connexionFailed()
                self.connecting = False

        t = time.time()
        while self.communication.available() > 0 and time.time() - t < 0.005:
            message = self.communication.getLastMessage()
            try:
                self.handleNewMessage(message)
                self.tMax = message.timestamp
                if self.tMin is None:
                    self.tMin = self.tMax
                self.updateTimeBounds()
            except (ValueError, IndexError) as e:
                print("Incorrect message received")
                traceback.print_tb(e.__traceback__)

        if time.time() - self.lastRefreshTime > 0.1 and (self.needUpdateConsole or self.needUpdateCurveGraph or self.needUpdateScatterGraph):
            if self.needUpdateConsole:
                self.updateConsole()
                self.needUpdateConsole = False
            if self.needUpdateCurveGraph:
                self.updateCurveGraph()
                self.needUpdateCurveGraph = False
            if self.needUpdateScatterGraph:
                self.updateScatterGraph()
                self.needUpdateScatterGraph = False
            self.lastRefreshTime = time.time()

    def handleNewMessage(self, message):
        command = self.command_from_id[message.id]
        if command.outputInfoFrame == message.standard:
            raise ValueError("Incoherent frame type received")
        try:
            if command.outputInfoFrame:
                args = self.interpretStrings(message.data, command.outputFormat)
            else:
                args = self.interpretBytes(message.data, command.outputFormat)
            if command.type == CommandType.SUBSCRIPTION_CURVE_DATA:
                i = 0
                for arg in args:
                    self.curveGraphEntries.append((message.timestamp, command.name, command.outputFormat[i].name, message.timestamp, float(arg)))
                    i += 1
                self.needUpdateCurveGraph = True
            elif command.type == CommandType.SUBSCRIPTION_SCATTER_DATA:
                # todo
                self.needUpdateScatterGraph = True
            else:
                string = str(message.timestamp) + "_"
                if command.type == CommandType.SUBSCRIPTION_TEXT:
                    if command.id == INFO_CHANNEL:
                        descriptor = INFO_CHANNEL_NAME
                    elif command.id == ERROR_CHANNEL:
                        descriptor = ERROR_CHANNEL_NAME
                    elif command.id == TRACE_CHANNEL:
                        descriptor = TRACE_CHANNEL_NAME
                    elif command.id == SPY_ORDER_CHANNEL:
                        descriptor = SPY_ORDER_CHANNEL_NAME
                    else:
                        descriptor = INFO_CHANNEL_NAME
                    string += descriptor + "_"
                    for arg in args:
                        string += str(arg) + " "
                else:
                    string += ANSWER_DESCRIPTOR + "_"
                    i = 0
                    for arg in args:
                        string += command.outputFormat[i].name + ": " + str(arg) + " "
                        i += 1
                string += '\n'
                self.consoleEntries.append((message.timestamp, string))
                self.needUpdateConsole = True
        except (IndexError, ValueError):
            print("Exception raised for command:", command.id)
            raise

    @staticmethod
    def interpretBytes(byteList, fieldList):
        output = []
        i = 0
        for field in fieldList:
            try:
                val = byteList[i]
                if field.type == NumberType.UINT8:
                    i += 1
                elif field.type == NumberType.INT8 :
                    if val > 127:
                        val -= 256
                    i+= 1
                elif field.type == NumberType.UINT16:
                    i += 1
                    val += byteList[i] * 256
                    i += 1
                elif field.type == NumberType.INT16:
                    i += 1
                    val += byteList[i] * 256
                    if val > 32767:
                        val -= 65536
                    i += 1
                elif field.type == NumberType.UINT32:
                    i += 1
                    val += byteList[i] * 256
                    i += 1
                    val += byteList[i] * 256 * 256
                    i += 1
                    val += byteList[i] * 256 * 256 * 256
                    i += 1
                elif field.type == NumberType.INT32:
                    i += 1
                    val += byteList[i] * 256
                    i += 1
                    val += byteList[i] * 256 * 256
                    i += 1
                    val += byteList[i] * 256 * 256 * 256
                    if val > 2147483647:
                        val -= 4294967296
                    i += 1
                output.append(val)
            except IndexError:
                print("Byte list:", byteList, "Field name:", field.name)
                raise
        return output

    @staticmethod
    def interpretStrings(byteList, fieldList):
        if len(byteList) > 1:
            string = byteList[0:len(byteList) - 2].decode(encoding='utf-8', errors='ignore')
            return string.split("_", maxsplit=len(fieldList) - 1)
        else:
            return []

    def updateConsole(self):
        self.console.setText("".join([x[1] for x in self.consoleEntries if self.tMin <= x[0] <= self.currentTime]))

    def updateCurveGraph(self):
        subData = {}
        for entry in self.curveGraphEntries:
            if self.tMin <= entry[0] <= self.currentTime:
                if entry[1] not in subData:
                    subData[entry[1]] = {}
                if entry[2] in subData[entry[1]]:
                    subData[entry[1]][entry[2]]["x"].append(entry[3])
                    subData[entry[1]][entry[2]]["y"].append(entry[4])
                else:
                    subData[entry[1]][entry[2]] = {"x": [entry[3]], "y": [entry[4]]}
        self.graph.update_data(curveData=subData, origin=self.tMax, xMin=self.tMin, xMax=self.tMax)

    def updateScatterGraph(self):
        #todo
        # self.graph.update_data(scatterData={})
        pass

    def updateTimeBounds(self):
        self.toolbar.setTimeBounds(self.tMin - self.tMax, 0)

    # Methods called in the Qt thread
    def connect(self, ip=None, com=None):
        self.connecting = True
        self.communication.connect(ip, com)

    def disconnect(self):
        self.communication.disconnect()

    def setCurrentTime(self, timestamp):
        self.currentTime = timestamp + self.tMax
        self.needUpdateConsole = True
        self.needUpdateCurveGraph = True
        self.needUpdateScatterGraph = True

    def setZoom(self, zoom):
        self.zoom = zoom
        self.needUpdateCurveGraph = True
        self.needUpdateScatterGraph = True

    def eraseBeforeCut(self):
        if len(self.splitters) > 1:
            self.consoleEntries[:] = [x for x in self.consoleEntries if x[0] > self.tMin]
            self.curveGraphEntries[:] = [x for x in self.curveGraphEntries if x[0] > self.tMin]
            self.scatterGraphEntries[:] = [x for x in self.scatterGraphEntries if x[0] > self.tMin]
            self.splitters = [self.tMin]
            self.toolbar.enableSecondaryIcons(False)

    def cancelLastCut(self):
        if len(self.splitters) > 1:
            self.tMin = self.splitters[-2]
            self.splitters = self.splitters[0:len(self.splitters) - 1]
            if len(self.splitters) <= 1:
                self.toolbar.enableSecondaryIcons(False)
            self.updateTimeBounds()
            self.needUpdateConsole = True
            self.needUpdateCurveGraph = True
            self.needUpdateScatterGraph = True

    def addCut(self):
        if self.tMin is None:
            return
        if len(self.splitters) == 0:
            self.splitters.append(self.tMin)
        if self.splitters[-1] == self.currentTime:
            return
        self.tMin = self.currentTime
        self.splitters.append(self.currentTime)
        self.toolbar.enableSecondaryIcons(True)
        self.updateTimeBounds()
        self.needUpdateConsole = True
        self.needUpdateCurveGraph = True
        self.needUpdateScatterGraph = True

    def sendOrder(self, command, args):
        messageData = bytearray()
        if len(command.inputFormat) != len(args):
            print("Invalid order to send")
        else:
            i = 0
            for field in command.inputFormat:
                v = args[i]
                if field.type == NumberType.UINT8:
                    messageData.append(v)
                elif field.type == NumberType.INT8 :
                    if v < 0:
                        v += 256
                    messageData.append(v)
                elif field.type == NumberType.UINT16:
                    messageData.append(int(v % 256))
                    messageData.append(int(v / 256))
                elif field.type == NumberType.INT16:
                    if v < 0:
                        v += (256*256)
                    messageData.append(int(v % 256))
                    messageData.append(int(v / 256))
                elif field.type == NumberType.UINT32:
                    o4 = int(v / (256*256*256))
                    r4 = int(v % (256*256*256))
                    o3 = int(r4 / (256*256))
                    r3 = int(r4 % (256*256))
                    o2 = int(r3 / 256)
                    o1 = int(r3 % 256)
                    messageData.append(o1)
                    messageData.append(o2)
                    messageData.append(o3)
                    messageData.append(o4)
                elif field.type == NumberType.INT32:
                    if v < 0:
                        v += (256*256*256*256)
                    o4 = int(v / (256*256*256))
                    r4 = int(v % (256*256*256))
                    o3 = int(r4 / (256*256))
                    r3 = int(r4 % (256*256))
                    o2 = int(r3 / 256)
                    o1 = int(r3 % 256)
                    messageData.append(o1)
                    messageData.append(o2)
                    messageData.append(o3)
                    messageData.append(o4)
                i += 1
            message = Message(command.id, bytes(messageData))
            self.communication.sendMessage(message)
