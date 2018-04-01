from communication import Communication, Message
from CommandList import *
from enum import Enum
import struct
import time
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
        self.consoleEntriesIndex = 0
        self.curveGraphEntries = []
        self.scatterGraphEntries = []
        self.splitters = []
        self.needFullUpdateConsole = False
        self.needSmallUpdateConsole = False
        self.needUpdateCurveGraph = False
        self.needUpdateScatterGraph = False

        self.currentTime = 0    # ms
        self.tMin = None        # ms
        self.tMax = 0           # ms
        self.zoom = 0           # ms

        subscriptionTypes = [CommandType.SUBSCRIPTION_CURVE_DATA,
                             CommandType.SUBSCRIPTION_SCATTER_DATA,
                             CommandType.SUBSCRIPTION_TEXT]
        self.subscriptions = []
        for c in COMMAND_LIST:
            if c.type in subscriptionTypes:
                self.subscriptions.append(False)
        self.subscriptions[INFO_CHANNEL] = True
        self.subscriptions[ERROR_CHANNEL] = True
        self.subscriptions[TRACE_CHANNEL] = True
        self.subscriptions[SPY_ORDER_CHANNEL] = True

        self.command_from_id = {}
        for command in COMMAND_LIST:
            self.command_from_id[command.id] = command

    def setGraphicalInterface(self, toolbar, console, graph):
        self.toolbar = toolbar
        self.toolbar.setTimeBounds(0)
        self.console = console
        self.graph = graph

    def startBackgroundTask(self):
        self.backgroundTask.start()

    def main_loop(self):
        try:
            self.communication.communicate()
        except IOError:
            self.toolbar.connexionFailed()
        if self.connecting:
            if self.communication.connectionSuccess is not None:
                if self.communication.connectionSuccess:
                    self.sendSubscriptions()
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
                print("[Incorrect message received]", e)

        if time.time() - self.lastRefreshTime > 0.3 and \
                (self.needFullUpdateConsole or self.needSmallUpdateConsole or
                 self.needUpdateCurveGraph or self.needUpdateScatterGraph):
            if self.needFullUpdateConsole:
                self.updateFullConsole()
                self.needFullUpdateConsole = False
                self.needSmallUpdateConsole = False
            elif self.needSmallUpdateConsole:
                self.updateConsole()
                self.needSmallUpdateConsole = False
            if self.needUpdateCurveGraph:
                self.updateCurveGraph()
                self.needUpdateCurveGraph = False
            if self.needUpdateScatterGraph:
                self.updateScatterGraph()
                self.needUpdateScatterGraph = False
            self.lastRefreshTime = time.time()

    def handleNewMessage(self, message):
        try:
            command = self.command_from_id[message.id]
        except KeyError:
            print("Unknown message ID:", message.id)
            return
        if command.outputInfoFrame == message.standard:
            raise ValueError("Incoherent frame type received")
        try:
            if command.outputInfoFrame:
                args = self.interpretStrings(message.data, command.outputFormat)
            else:
                args = self.interpretBytes(message.data, command.outputFormat)
            if command.type == CommandType.SUBSCRIPTION_CURVE_DATA:
                if len(command.outputFormat) != len(args):
                    raise IndexError("Wrong number of arguments received")
                nbArgs = len(args)
                timestamp = message.timestamp
                for i in range(nbArgs):
                    if command.outputFormat[i].name == TIMESTAMP_INFO_FIELD:
                        timestamp = float(args[i])
                        break
                for i in range(nbArgs):
                    if command.outputFormat[i].name != TIMESTAMP_INFO_FIELD:
                        self.curveGraphEntries.append((message.timestamp, command.name, command.outputFormat[i].name, timestamp, float(args[i])))
                self.needUpdateCurveGraph = True
            elif command.type == CommandType.SUBSCRIPTION_SCATTER_DATA:
                # todo
                self.needUpdateScatterGraph = True
            else:
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
                    fullArgs = ""
                    for arg in args:
                        fullArgs += str(arg) + " "
                    sArgs = fullArgs.split('\n')
                    for argLine in sArgs:
                        if len(argLine) > 1:
                            string = str(message.timestamp) + "_"
                            string += descriptor + "_" + argLine
                            string += '\n'
                            self.consoleEntries.append((message.timestamp, string))
                else:
                    string = str(message.timestamp) + "_"
                    string += ANSWER_DESCRIPTOR + "_"
                    if len(args) == 0:
                        string += "Order " + command.name + " terminated"
                    i = 0
                    for arg in args:
                        string += command.outputFormat[i].name + ": " + str(arg) + " "
                        i += 1
                    string += '\n'
                    self.consoleEntries.append((message.timestamp, string))
                if len(self.consoleEntries) > CONSOLE_HISTORY_SIZE:
                    self.consoleEntries.pop(0)
                    self.consoleEntriesIndex -= 1
                if not self.toolbar.paused:
                    self.needSmallUpdateConsole = True
        except (IndexError, ValueError):
            print("Exception raised for command:", command.id)
            raise

    @staticmethod
    def interpretBytes(byteList, fieldList):
        output = []
        i = 0
        for field in fieldList:
            try:
                if field.type == int:
                    val, = struct.unpack_from("i", byteList, i)
                    i += struct.calcsize("i")
                elif field.type == float:
                    val, = struct.unpack_from("f", byteList, i)
                    i += struct.calcsize("f")
                elif field.type == bool:
                    val, = struct.unpack_from("?", byteList, i)
                    i += struct.calcsize("?")
                elif field.type == Enum:
                    index, = struct.unpack_from("B", byteList, i)
                    i += struct.calcsize("B")
                    val = field.legend[index]
                else:
                    raise ValueError
                output.append(val)
            except (IndexError, struct.error, ValueError):
                print("Byte list:", byteList, "Field name:", field.name)
                raise
        return output

    @staticmethod
    def interpretStrings(byteList, fieldList):
        if len(byteList) > 1:
            string = byteList[0:-1].decode(encoding='utf-8', errors='ignore')
            return string.split("_", maxsplit=len(fieldList) - 1)
        else:
            return []

    def updateFullConsole(self):
        self.console.setText("".join([x[1] for x in self.consoleEntries if self.tMin <= x[0] <= self.currentTime]))
        self.consoleEntriesIndex = len(self.consoleEntries)

    def updateConsole(self):
        lines = [x[1] for x in self.consoleEntries[self.consoleEntriesIndex:] if self.tMin <= x[0] <= self.currentTime]
        self.console.appendText("".join(lines))
        self.consoleEntriesIndex = len(self.consoleEntries)

    def updateCurveGraph(self):
        #todo : optimize this
        subData = {}
        tMin = None
        tMax = None
        for entry in self.curveGraphEntries:
            if self.currentTime - self.zoom <= entry[0] <= self.currentTime:
                t = entry[3]
                if tMin is None or t < tMin:
                    tMin = t
                if tMax is None or t > tMax:
                    tMax = t
                if entry[1] not in subData:
                    subData[entry[1]] = {}
                if entry[2] in subData[entry[1]]:
                    subData[entry[1]][entry[2]]["x"].append(t)
                    subData[entry[1]][entry[2]]["y"].append(entry[4])
                else:
                    subData[entry[1]][entry[2]] = {"x": [t], "y": [entry[4]]}
        # print(subData)
        if tMin is None:
            tMin = self.currentTime - self.zoom
        if tMax is None:
            tMax = self.currentTime
        if tMin > tMax:
            tMin = tMax
        self.graph.update_data(curveData=subData, origin=tMax, xMin=tMin, xMax=tMax)

    def updateScatterGraph(self):
        #todo
        # self.graph.update_data(scatterData={})
        pass

    def updateTimeBounds(self):
        # print("Set bounds")
        self.toolbar.setTimeBounds(self.tMax - self.tMin)

    # Methods called in the Qt thread
    def connect(self, ip=None, com=None):
        self.connecting = True
        self.communication.connect(ip, com)

    def disconnect(self):
        self.communication.disconnect()

    def setCurrentTime(self, timestamp):
        # print("Set current time")
        needUpdate = self.currentTime != timestamp + self.tMax
        self.currentTime = timestamp + self.tMax
        if self.toolbar.paused and needUpdate:
            self.needFullUpdateConsole = True
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
            self.needFullUpdateConsole = True
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
        self.needFullUpdateConsole = True
        self.needUpdateCurveGraph = True
        self.needUpdateScatterGraph = True

    def sendOrder(self, command, args):
        try:
            if len(command.inputFormat) != len(args):
                raise ValueError("Invalid order to send")
            messageData = bytearray()
            i = 0
            for field in command.inputFormat:
                if field.type == int:
                    fmt = "i"
                    val = int(args[i])
                elif field.type == float:
                    fmt = "f"
                    val = float(args[i])
                elif field.type == bool:
                    fmt = "?"
                    val = bool(args[i])
                elif field.type == Enum:
                    fmt = "B"
                    val = field.legend.index(args[i])
                else:
                    raise ValueError("Invalid type in order to send")
                messageData += struct.pack(fmt, val)
                i += 1
            message = Message(command.id, bytes(messageData))
            self.communication.sendMessage(message)
        except (ValueError, struct.error) as e:
            print(e)

    def sendSubscriptions(self):
        for i in range(len(self.subscriptions)):
            message = Message(i, bytes([self.subscriptions[i]]))
            self.communication.sendMessage(message)
        # print("Send subscriptions:", self.subscriptions)

    def updateSubscriptions(self, updateList):
        changed = False
        for channel, enabled in updateList:
            if self.subscriptions[channel] != enabled:
                changed = True
                self.subscriptions[channel] = enabled
        if changed:
            self.sendSubscriptions()
