from PyQt5.QtWidgets import (QWidget, QGridLayout, QSplitter, QApplication, QFrame, QPushButton, QTabWidget, QSlider,
                             QLabel, QLineEdit, QComboBox, QScrollArea, QCheckBox, QTextEdit, QHBoxLayout, QVBoxLayout, QLayoutItem)
from PyQt5.QtGui import (QIcon, QPixmap, QFont)
from PyQt5.QtCore import Qt
import sys, random
import pyqtgraph
from CommandList import *
from backend import Backend
from communication import DEFAULT_ROBOT_IP


class MainWindow(QWidget):
    def __init__(self, toolbarConnect, toolbarDisconnect, toolbarSetTime, toolbarSetZoom, toolbarErase, toolbarCancelCut, toolbarCut, panelSend):
        super().__init__()
        grid = QGridLayout()

        self.toolBar = ToolBar(self, toolbarConnect, toolbarDisconnect, toolbarSetTime, toolbarSetZoom, toolbarErase, toolbarCancelCut, toolbarCut)
        self.commandPanelScrollArea = QScrollArea(self)
        self.commandPanelScrollArea.setFrameShape(QFrame.StyledPanel)
        self.commandPanel = CommandPanel(self.commandPanelScrollArea, panelSend)
        self.commandPanelScrollArea.setWidgetResizable(True)
        self.commandPanelScrollArea.setWidget(self.commandPanel)
        self.consolePanel = ConsolePanel(self)
        self.consolePanel.setFrameShape(QFrame.StyledPanel)
        self.graphPanel = GraphPanel(self)

        self.splitter = QSplitter(Qt.Horizontal)
        self.splitter.addWidget(self.commandPanelScrollArea)
        self.splitter.addWidget(self.consolePanel)
        self.splitter.addWidget(self.graphPanel)

        self.splitter.setStretchFactor(0, 0)
        self.splitter.setStretchFactor(1, 0)
        self.splitter.setStretchFactor(2, 1)

        grid.addWidget(self.toolBar, 0, 0)
        grid.addWidget(self.splitter, 1, 0)
        grid.setRowStretch(0, 0)
        grid.setRowStretch(1, 1)

        self.setLayout(grid)
        self.setWindowTitle('Lowlevel Interface')
        self.setWindowIcon(QIcon('img/intech.ico'))
        self.resize(1024, 512)
        self.show()


class ToolBar(QWidget):
    def __init__(self, master, mConnectTo, mDisconnect, mSetTime, mSetZoom, mEraseBeforeCut, mCancelCut, mCut):
        super().__init__(master)

        # Callbacks
        self.connectTo = mConnectTo
        self.closeConnection = mDisconnect
        self.setTimeValue = mSetTime

        # Widgets
        self.protocolTabs = QTabWidget(self)
        self.ipChooser = IpChooser(self)
        self.serialChooser = SerialChooser(self)

        self.protocolTabs.addTab(self.ipChooser, "TCP/IP")
        self.protocolTabs.addTab(self.serialChooser, "COM")

        self.iconConnect = QIcon("img/connect.png")
        self.iconDisconnect = QIcon("img/disconnect.png")
        self.iconPlay = QIcon("img/play.png")
        self.iconPause = QIcon("img/pause.png")

        self.bConnect = QPushButton(self.iconConnect, "", self)
        self.bConnect.clicked.connect(self.connexionMgr)
        self.connected = False
        self.connecting = False
        self.bErase = QPushButton(QIcon("img/erase.png"), "", self)
        self.bErase.setEnabled(False)
        self.bErase.clicked.connect(mEraseBeforeCut)
        self.bUncut = QPushButton(QIcon("img/uncut.png"), "", self)
        self.bUncut.setEnabled(False)
        self.bUncut.clicked.connect(mCancelCut)
        self.bCut = QPushButton(QIcon("img/cut.png"), "", self)
        self.bCut.clicked.connect(mCut)
        self.bPlayPause = QPushButton(self.iconPlay, "", self)
        self.bPlayPause.clicked.connect(self.playPauseToggle)
        self.paused = True

        self.timeSlider = AnnotatedSlider(self, 0, 10, unit="ms", icon=None, callback=self.timeSliderMoved)
        self.zoomSlider = AnnotatedSlider(self, 100, 10000, unit="ms", icon=None, callback=mSetZoom)
        self.zoomSlider.setValue(1000)
        mSetZoom(1000)

        # Layout
        grid = QGridLayout()
        grid.addWidget(self.protocolTabs, 0, 0)
        grid.addWidget(self.bConnect, 0, 1)
        grid.addWidget(self.bErase, 0, 2)
        grid.addWidget(self.bUncut, 0, 3)
        grid.addWidget(self.bCut, 0, 4)
        grid.addWidget(self.bPlayPause, 0, 5)
        grid.addWidget(self.timeSlider, 0, 6)
        grid.addWidget(self.zoomSlider, 0, 7)

        grid.setColumnStretch(6, 2)
        grid.setColumnStretch(7, 1)
        grid.setSpacing(5)
        grid.setContentsMargins(0,0,0,0)
        self.setLayout(grid)

    def connexionMgr(self):
        if not self.connecting:
            if self.connected:
                self.closeConnection()
                self.ipChooser.markBlank()
                self.serialChooser.markBlank()
                self.protocolTabs.setEnabled(True)
                self.bConnect.setIcon(self.iconConnect)
                self.connected = False
            else:
                self.connecting = True
                self.ipChooser.markBlank()
                self.serialChooser.markBlank()
                self.protocolTabs.setEnabled(False)
                self.bConnect.setEnabled(False)
                if self.isIpMode():
                    self.connectTo(ip=self.ipChooser.getIp())
                else:
                    self.connectTo(com=self.serialChooser.getEntry())

    def isIpMode(self):
        return self.protocolTabs.currentIndex() == 0

    def playPauseToggle(self):
        if self.paused:
            tMin, tMax = self.timeSlider.getBounds()
            self.timeSlider.setValue(tMax)
            self.paused = False
            self.bPlayPause.setIcon(self.iconPause)
        else:
            self.paused = True
            self.bPlayPause.setIcon(self.iconPlay)

    def timeSliderMoved(self, value):
        if not self.paused:
            self.paused = True
            self.bPlayPause.setIcon(self.iconPlay)
        self.setTimeValue(value)

    # Methods available for the backend
    def setTimeBounds(self, tMin, tMax):
        self.timeSlider.setBounds(tMin, tMax)
        if not self.paused:
            self.timeSlider.setValue(tMax)

    def enableSecondaryIcons(self, enable):
        self.bErase.setEnabled(enable)
        self.bUncut.setEnabled(enable)

    def connexionFailed(self):
        self.connected = False
        self.connecting = False
        self.protocolTabs.setEnabled(True)
        self.bConnect.setEnabled(True)
        if self.isIpMode():
            self.ipChooser.markWrong()
            self.serialChooser.markBlank()
        else:
            self.ipChooser.markBlank()
            self.serialChooser.markWrong()

    def connexionSucceeded(self):
        self.connected = True
        self.connecting = False
        self.bConnect.setIcon(self.iconDisconnect)
        self.bConnect.setEnabled(True)
        if self.isIpMode():
            self.ipChooser.markConnected()
            self.serialChooser.markBlank()
        else:
            self.ipChooser.markBlank()
            self.serialChooser.markConnected()


class IpChooser(QWidget):
    def __init__(self, master):
        super().__init__(master)
        grid = QGridLayout()
        self.ipField = QLineEdit(DEFAULT_ROBOT_IP, self)
        grid.addWidget(self.ipField, 0, 0)
        self.setLayout(grid)

    def getIp(self):
        return self.ipField.text()

    def markBlank(self):
        self.ipField.setStyleSheet("")

    def markWrong(self):
        self.ipField.setStyleSheet("""QLineEdit { background-color: rgb(249, 83, 83) }""")

    def markConnected(self):
        self.ipField.setStyleSheet("""QLineEdit { background-color: rgb(203, 230, 163) }""")


class SerialChooser(QWidget):
    def __init__(self, master):
        super().__init__(master)
        grid = QGridLayout()
        self.comboBox = QComboBox(self)
        self.comboBox.setEditable(True)
        grid.addWidget(self.comboBox, 0, 0)
        self.setLayout(grid)

    def setEntries(self, entries):
        for i in range(self.comboBox.count()):
            self.comboBox.removeItem(i)
        self.comboBox.addItems(entries)

    def getEntry(self):
        strList = self.comboBox.currentText().split()
        if len(strList) > 0:
            return strList[0]
        else:
            return ""

    def markBlank(self):
        self.comboBox.setStyleSheet("")

    def markWrong(self):
        self.comboBox.setStyleSheet("""QComboBox { background-color: rgb(249, 83, 83) }""")

    def markConnected(self):
        self.comboBox.setStyleSheet("""QComboBox { background-color: rgb(203, 230, 163) }""")


class AnnotatedSlider(QWidget):
    def __init__(self, master, sMin, sMax, unit, icon, callback):
        super().__init__(master)
        self.callback = callback
        if sMax < sMin:
            raise ValueError
        grid = QGridLayout()
        self.sMin = sMin
        self.sMax = sMax
        self.slider = QSlider(Qt.Horizontal, self)
        self.slider.setRange(sMin, sMax)
        self.slider.valueChanged.connect(self._slider_moved_by_user)
        if icon is not None:
            label = QLabel(self)
            label.setPixmap(QPixmap(icon))
            grid.addWidget(label, 0, 0)
        if unit is not None:
            label = QLabel(self)
            label.setText(unit)
            grid.addWidget(label, 0, 3)
        self.lineEdit = QLineEdit(self)
        self.lineEdit.setText(str(self.slider.value()))
        self.lineEdit.editingFinished.connect(self._value_typed_by_user)
        self._update_lineEdit_size()

        grid.addWidget(self.slider, 0, 1)
        grid.addWidget(self.lineEdit, 0, 2)
        grid.setColumnStretch(0, 0)
        grid.setColumnStretch(1, 1)
        grid.setColumnStretch(2, 0)
        grid.setColumnStretch(3, 0)
        self.setLayout(grid)

    def _update_lineEdit_size(self):
        nbDigit = max(len(str(self.sMin)), len(str(self.sMax)))
        size_px = (nbDigit + 1) * 7
        self.lineEdit.setMinimumWidth(size_px)
        self.lineEdit.setMaximumWidth(size_px)

    def setBounds(self, sMin, sMax):
        if sMax < sMin:
            return
        self.slider.setRange(sMin, sMax)
        self.lineEdit.setText(str(self.slider.value()))
        self.sMin = sMin
        self.sMax = sMax
        self._update_lineEdit_size()

    def getBounds(self):
        return self.sMin, self.sMax

    def getValue(self):
        return self.slider.value()

    def setValue(self, value):
        value = int(value)
        if value < self.sMin:
            value = self.sMin
        elif value > self.sMax:
            value = self.sMax
        self.slider.setValue(value)
        self.lineEdit.setText(str(value))

    def _value_typed_by_user(self):
        try:
            newValue = self._string_to_int(self.lineEdit.text())
            self.slider.setValue(newValue)
            self.callback(newValue)
        except ValueError:
            self.lineEdit.setText(str(self.slider.value()))

    def _slider_moved_by_user(self, value):
        self.lineEdit.setText(str(value))
        self.callback(value)

    def _string_to_int(self, string):
        val = int(string)
        if val < self.sMin or val > self.sMax:
            raise ValueError
        return val


class CommandPanel(QWidget):
    def __init__(self, master, sendMethod):
        super().__init__(master)
        grid = QGridLayout()
        row = 0
        for command in COMMAND_LIST:
            if command.type == CommandType.SHORT_ORDER or command.type == CommandType.LONG_ORDER:
                cb = CommandBox(self, command, sendMethod)
                cb.setFrameShape(QFrame.StyledPanel)
                grid.addWidget(cb, row, 0)
                row += 1
        grid.setRowStretch(row, 1)
        grid.setContentsMargins(5,5,5,5)
        self.setLayout(grid)


class CommandBox(QFrame):
    def __init__(self, master, command, sendCallback):
        super().__init__(master)
        self.command = command
        self.sendCallback = sendCallback
        grid = QGridLayout()
        self.id = QLabel('0x%02x' % command.id, self)
        if len(command.inputFormat) > 0:
            self.nameButton = QPushButton(command.name, self)
            self.nameButton.setCheckable(True)
            self.nameButton.clicked.connect(self.click_event)
        else:
            self.nameButton = QLabel(command.name, self)
            self.nameButton.setAlignment(Qt.AlignCenter)
        self.sendButton = QPushButton(self)
        self.sendButton.setIcon(QIcon('img/send.png'))
        self.sendButton.clicked.connect(self.send)

        grid.addWidget(self.id, 0, 0)
        grid.addWidget(self.nameButton, 0, 1)
        grid.addWidget(self.sendButton, 0, 2)
        grid.setContentsMargins(5,3,5,3)
        grid.setColumnStretch(0, 0)
        grid.setColumnStretch(1, 1)
        grid.setColumnStretch(2, 0)

        self.label_widget_field = []
        row = 1
        for field in self.command.inputFormat:
            label = QLabel(field.name, self)
            label.setHidden(True)
            grid.addWidget(label, row, 0)
            if len(field.legend) == 0:
                widget = QLineEdit(self)
                widget.textChanged.connect(self.checkTextFields)
            else:
                widget = QComboBox(self)
                for value, name in field.legend.items():
                    widget.addItem(name)
            widget.setHidden(True)
            grid.addWidget(widget, row, 1)
            self.label_widget_field.append((label, widget, field))
            row += 1
        self.bReset = QPushButton(self)
        self.bReset.setIcon(QIcon('img/reset.png'))
        self.bReset.clicked.connect(self.reset_values)
        self.bReset.setHidden(True)
        grid.addWidget(self.bReset, 1, 2)
        self.setLayout(grid)
        self.reset_values()

    def click_event(self):
        if self.nameButton.isChecked():
            self.expand()
        else:
            self.collapse()

    def expand(self):
        self.bReset.setHidden(False)
        for label, widget, field in self.label_widget_field:
            label.setHidden(False)
            widget.setHidden(False)

    def collapse(self):
        self.bReset.setHidden(True)
        for label, widget, field in self.label_widget_field:
            label.setHidden(True)
            widget.setHidden(True)

    def reset_values(self):
        for label, widget, field in self.label_widget_field:
            if isinstance(widget, QLineEdit):
                widget.setText(str(field.default))
            elif isinstance(widget, QComboBox):
                i = 0
                for value, name in field.legend.items():
                    if value == field.default:
                        widget.setCurrentIndex(i)
                        break
                    i += 1
            else:
                raise Exception("Unknown widget type")

    def send(self):
        args = []
        for label, widget, field in self.label_widget_field:
            value = None
            if isinstance(widget, QLineEdit):
                if self.checkTextFields():
                    value = int(widget.text())
            elif isinstance(widget, QComboBox):
                for v, name in field.legend.items():
                    if name == widget.currentText():
                        value = v
            else:
                raise Exception("Unknown widget type")
            if value is None:
                return
            else:
                args.append(value)
        self.sendCallback(self.command, args)

    def checkTextFields(self):
        allOk = True
        for label, widget, field in self.label_widget_field:
            if isinstance(widget, QLineEdit):
                try:
                    value = int(widget.text())
                    if field.type == NumberType.UINT8 and (value < 0 or value > 255):
                        raise ValueError
                    elif field.type == NumberType.INT8 and (value < -128 or value > 127):
                        raise ValueError
                    elif field.type == NumberType.UINT16 and (value < 0 or value > 65535):
                        raise ValueError
                    elif field.type == NumberType.INT16 and (value < -32768 or value > 32767):
                        raise ValueError
                    elif field.type == NumberType.UINT32 and (value < 0 or value > 4294967295):
                        raise ValueError
                    elif field.type == NumberType.INT32 and (value < -2147483648 or value > 2147483647):
                        raise ValueError
                    widget.setStyleSheet("")
                    self.sendButton.setEnabled(True)
                except ValueError:
                    widget.setStyleSheet("background-color: rgb(249, 83, 83)")
                    self.sendButton.setEnabled(False)
                    allOk = False
        return allOk


class ConsolePanel(QFrame):
    def __init__(self, master):
        super().__init__(master)

        self.cbTimestamp = QCheckBox("Timestamp", self)
        self.cbErrors = QCheckBox("Errors", self)
        self.cbInfo = QCheckBox("Info", self)
        self.cbOrders = QCheckBox("Orders", self)
        self.cbAnswers = QCheckBox("Answers", self)

        self.cbList = [self.cbTimestamp, self.cbErrors, self.cbInfo, self.cbOrders, self.cbAnswers]
        for cb in self.cbList:
            cb.setChecked(True)
            cb.stateChanged.connect(self.settings_changed)

        self.console = QTextEdit(self)
        self.console.setReadOnly(True)
        self.font = "Consolas"
        self.fontSize = 9
        self.fontSize_small = 7
        self.timestampColor = QColor(119, 180, 177)
        self.textColor = QColor(197, 200, 198)
        self.errorColor = QColor(195, 48, 39)
        self.orderColor = QColor(1, 160, 228)
        self.answerColor = QColor(1, 162, 82)

        self.console.setStyleSheet("""
            background-color: rgb(29, 31, 33);
            color: rgb(197, 200, 198);
        """)

        self.consoleText = ""

        self.margin = 31
        self.width_needed = None

        grid = QGridLayout()
        self.cbArea = QVBoxLayout()
        grid.addLayout(self.cbArea, 0, 0)
        grid.addWidget(self.console, 1, 0, 1, 2)
        grid.setRowStretch(1, 1)
        grid.setColumnStretch(1, 1)
        self.setLayout(grid)
        self.organizeWidgets(init=True)

    def resizeEvent(self, event):
        w = self.cbArea.minimumSize().width()
        availableWidth = event.size().width() - self.margin
        if availableWidth > 30:
            if availableWidth <= w or (self.width_needed is not None and availableWidth > w + self.width_needed):
                self.organizeWidgets(False, availableWidth - 30)

    def organizeWidgets(self, init, availableWidth=None):
        if not init:
            for i in reversed(range(self.cbArea.count())):
                item = self.cbArea.itemAt(i)
                if isinstance(item, QLayoutItem):
                    layout = item.layout()
                    for j in reversed(range(layout.count())):
                        widget = layout.itemAt(j).widget()
                        if widget is not None:
                            layout.removeWidget(widget)
                        else:
                            print("ERR")
                else:
                    print("Err")
                self.cbArea.removeItem(item)
                item.deleteLater()

        self.width_needed = None
        i = 0
        while i < len(self.cbList):
            line = QHBoxLayout()
            columnCount = 0
            while True:
                if availableWidth is not None and line.minimumSize().width() >= availableWidth:
                    if columnCount > 1:
                        i -= 1
                        line.removeWidget(self.cbList[i])
                    overlap = line.minimumSize().width() - availableWidth
                    if self.width_needed is None:
                        self.width_needed = overlap
                    else:
                        self.width_needed = min(self.width_needed, overlap)
                    break
                elif i >= len(self.cbList):
                    break
                else:
                    line.addWidget(self.cbList[i])
                    i +=1
                    columnCount += 1
            self.cbArea.addLayout(line)

    def formatTextToConsole(self, text): # todo add support for spyOrder channel
        textLines = text.splitlines(keepends=True)
        lineNb = 0
        for line in textLines:
            lineNb += 1
            sLine = line.split('_', maxsplit=2)
            if len(sLine) != 3:
                print("Incorrect line nb", lineNb, ":", line)
                continue
            try:
                int(sLine[0])
            except ValueError:
                print("Incorrect timestamp :", sLine[0])
                continue
            if sLine[1] == INFO_CHANNEL_NAME:
                if not self.cbInfo.isChecked():
                    continue
                color = self.textColor
            elif sLine[1] == ERROR_CHANNEL_NAME:
                if not self.cbErrors.isChecked():
                    continue
                color = self.errorColor
            elif sLine[1] == ORDER_DESCRIPTOR:
                if not self.cbOrders.isChecked():
                    continue
                color = self.orderColor
            elif sLine[1] == ANSWER_DESCRIPTOR:
                if not self.cbAnswers.isChecked():
                    continue
                color = self.answerColor
            else:
                print("Incorrect line type :", sLine[1])
                continue
            if self.cbTimestamp.isChecked():
                self.console.setTextColor(self.timestampColor)
                self.console.setCurrentFont(QFont(self.font, self.fontSize_small))
                self.console.insertPlainText(sLine[0] + " ")
            self.console.setTextColor(color)
            self.console.setCurrentFont(QFont(self.font, self.fontSize))
            self.console.insertPlainText(sLine[2])

    def setText(self, newText):
        self.consoleText = newText
        self.console.clear()
        self.formatTextToConsole(newText)

    def appendText(self, text):
        self.consoleText += text
        self.formatTextToConsole(text)

    def settings_changed(self):
        self.console.setPlainText("")
        self.formatTextToConsole(self.consoleText)


class GraphPanel(QWidget):
    def __init__(self, master):
        super().__init__(master)
        grid = QGridLayout()
        grid.setContentsMargins(0, 0, 0, 0)

        self.curveCommandList = []
        self.scatterCommandList = []
        for command in COMMAND_LIST:
            if command.type == CommandType.SUBSCRIPTION_CURVE_DATA:
                self.curveCommandList.append(command)
            elif command.type == CommandType.SUBSCRIPTION_SCATTER_DATA:
                self.scatterCommandList.append(command)

        scrollArea = QScrollArea(self)
        scrollArea.setFrameShape(QFrame.StyledPanel)
        scrollArea.setVerticalScrollBarPolicy(Qt.ScrollBarAlwaysOff)
        self.graphSettings = GraphSettings(scrollArea, self.update_displayed_data, self.curveCommandList, self.scatterCommandList)
        scrollArea.setWidgetResizable(True)
        scrollArea.setWidget(self.graphSettings)

        self.graphCurveArea = GraphCurveArea(self, self.curveCommandList)
        self.graphCurveArea.setFrameShape(QFrame.StyledPanel)

        self.graphScatterArea = GraphScatterArea(self, self.scatterCommandList)
        self.graphScatterArea.setFrameShape(QFrame.StyledPanel)

        subSplitter = QSplitter(Qt.Horizontal)
        subSplitter.addWidget(self.graphCurveArea)
        subSplitter.addWidget(self.graphScatterArea)

        splitter = QSplitter(Qt.Vertical)
        splitter.addWidget(scrollArea)
        splitter.addWidget(subSplitter)
        splitter.setCollapsible(1, False)
        splitter.setStretchFactor(1, 1)

        grid.addWidget(splitter)
        self.setLayout(grid)
        self.update_displayed_data()

    def update_displayed_data(self):
        checkboxes = self.graphSettings.getCheckboxes()
        atLeastOneCurve = False
        for command in self.curveCommandList:
            for checkbox in checkboxes[command.name]:
                if checkbox.isChecked():
                    atLeastOneCurve = True
                    break
            if atLeastOneCurve:
                break
        atLeastOneScatter = False
        for command in self.scatterCommandList:
            for checkbox in checkboxes[command.name]:
                if checkbox.isChecked():
                    atLeastOneScatter = True
                    break
            if atLeastOneScatter:
                break
        if atLeastOneCurve and atLeastOneScatter:
            self.graphCurveArea.show()
            self.graphScatterArea.show()
        elif atLeastOneScatter:
            self.graphCurveArea.hide()
            self.graphScatterArea.show()
        else:
            self.graphCurveArea.show()
            self.graphScatterArea.hide()
        self.graphCurveArea.update_graph(checkboxes)
        self.graphScatterArea.update_graph(checkboxes)

    # curve data format : {command.name: {field.name: {"x": [], "y": []}, ...}, ...}
    # scatter data format : {command.name: {"x": [], "y": [], "color": []}, ...}
    def update_data(self, curveData=None, scatterData=None, origin=None, xMin = None, xMax=None):
        if xMin is not None and xMax is not None:
            self.graphCurveArea.setRange(xMin, xMax)
        if origin is not None:
            self.graphCurveArea.setOrigin(origin)
        if curveData is not None:
            for commandName, fieldDict in curveData.items():
                for fieldName, points in fieldDict.items():
                    self.graphCurveArea.setData(commandName, fieldName, points["x"], points["y"])
        if scatterData is not None:
            for commandName, pointsDict in scatterData.items():
                self.graphScatterArea.setData(commandName, pointsDict["x"], pointsDict["y"], pointsDict["color"])
        self.update_displayed_data()


class GraphSettings(QWidget):
    def __init__(self, master, update_callback, commandList_curves, commandList_scatter):
        super().__init__(master)
        grid = QGridLayout()
        grid.setContentsMargins(5,5,5,5)
        self.checkboxes = {}
        column = 0
        for command in commandList_curves:
            checkboxList = []
            cbGroup = GraphSettingGroup(self, update_callback, command.name, command.outputFormat, checkboxList)
            grid.addWidget(cbGroup, 0, column)
            self.checkboxes[command.name] = checkboxList
            column += 1
        checkboxList = []
        cbGroup = GraphSettingGroup(self, update_callback, "Scatter graphs", commandList_scatter, checkboxList)
        grid.addWidget(cbGroup, 0, column)
        for checkbox in checkboxList:
            self.checkboxes[checkbox.text()] = [checkbox]
        grid.setRowStretch(1, 1)
        grid.setColumnStretch(column + 1, 1)
        self.setLayout(grid)

    def getCheckboxes(self):
        return self.checkboxes


class GraphSettingGroup(QFrame):
    def __init__(self, master, update_callback, title, fieldList, checkboxList):
        super().__init__(master)
        self.update_callback = update_callback
        self.setFrameShape(QFrame.Panel)
        self.setFrameShadow(QFrame.Sunken)
        self.setStyleSheet("background-color: rgb(29, 31, 33)")
        grid = QVBoxLayout()
        grid.setContentsMargins(5,0,5,5)
        grid.setSpacing(0)
        buttonTitle = QPushButton(title, self)
        buttonTitle.clicked.connect(self.globalToggle)
        buttonTitle.setFlat(True)
        buttonTitle.setStyleSheet("color: rgb(197, 200, 198)")
        grid.addWidget(buttonTitle, stretch=0, alignment=Qt.AlignTop)
        self.cbList = []
        for field in fieldList:
            cb = QCheckBox(field.name, self)
            if isinstance(field, InfoField):
                r, g, b, a = field.color.getRgb()
            else:
                r, g, b = (197, 200, 198)
            cb.setStyleSheet("color: rgb("+ str(r) + "," + str(g) + "," + str(b) + ")")
            cb.clicked.connect(update_callback)
            grid.addWidget(cb, stretch=0, alignment=Qt.AlignTop)
            checkboxList.append(cb)
            self.cbList.append(cb)
        self.setLayout(grid)

    def globalToggle(self):
        turnOn = False
        for cb in self.cbList:
            if not cb.isChecked():
                turnOn = True
                break
        for cb in self.cbList:
            cb.setChecked(turnOn)
        self.update_callback()


class GraphCurveArea(QFrame):
    def __init__(self, master, commandList):
        super().__init__(master)
        grid = QGridLayout()
        grid.setContentsMargins(0,0,0,0)
        pyqtgraph.setConfigOption('background', QColor(29, 31, 33))
        self.plotWidget = pyqtgraph.PlotWidget(self)
        self.plotWidget.setEnabled(False)
        self.plotWidget.setLabel('bottom', 't', units='ms')
        self.plotItem = self.plotWidget.getPlotItem()
        self.plotItem.showAxis('left', False)
        self.plot = self.plotItem.plot(pen=QColor(255, 255, 255, 0))
        grid.addWidget(self.plotWidget, 0, 0)
        self.setLayout(grid)
        self.origin = 0
        self.xMin = 0
        self.xMax = 0
        self.data = {}
        axisColumn = 3
        for command in commandList:
            viewBox = pyqtgraph.ViewBox()
            axis = pyqtgraph.AxisItem('right')
            self.plotItem.layout.addItem(axis, 2, axisColumn)
            axisColumn += 1
            self.plotItem.scene().addItem(viewBox)
            axis.linkToView(viewBox)
            viewBox.setXLink(self.plotItem)
            axis.setLabel(command.name)
            self.data[command.name] = {"viewBox": viewBox, "axis": axis, "data": {}}
            for field in command.outputFormat:
                self.data[command.name]["data"][field.name] = {"x": [], "y": []}
                plot = pyqtgraph.PlotCurveItem()
                plot.setPen(field.color)
                self.data[command.name]["viewBox"].addItem(plot)
                self.data[command.name]["data"][field.name]["plot"] = plot
        self.updateViews()
        self.plotItem.vb.sigResized.connect(self.updateViews)

    # self.data format: {channelName: {"viewBox": viewBox, "axis": axis, "data": {fieldName: {"x": [], "y": []}, ...}}, ...}
    def setData(self, commandName, fieldName, x, y):
        self.data[commandName]["data"][fieldName]["x"] = x
        self.data[commandName]["data"][fieldName]["y"] = y

    def setOrigin(self, origin):
        self.origin = origin

    def setRange(self, xMin, xMax):
        if xMin <= xMax:
            self.xMin = xMin
            self.xMax = xMax

    def update_graph(self, checkboxes):
        self.plot.setData(x=[self.xMin - self.origin, self.xMax - self.origin], y=[1, 1])
        for name, cbList in checkboxes.items():
            if name in self.data:
                atLeastOneCbChecked = False
                for checkbox in cbList:
                    data = self.data[name]["data"][checkbox.text()]
                    if checkbox.isChecked():
                        atLeastOneCbChecked = True
                        data["plot"].setData(x=[x - self.origin for x in data["x"]], y=data["y"])
                    else:
                        data["plot"].setData(x=[], y=[])
                self.data[name]["axis"].setVisible(atLeastOneCbChecked)

    def updateViews(self):
        for name, dic in self.data.items():
            dic["viewBox"].setGeometry(self.plotItem.vb.sceneBoundingRect())
            dic["viewBox"].linkedViewChanged(self.plotItem.vb, dic["viewBox"].XAxis)


class GraphScatterArea(QFrame):
    def __init__(self, master, commandList):
        super().__init__(master)
        grid = QGridLayout()
        grid.setContentsMargins(0,0,0,0)
        pyqtgraph.setConfigOption('background', QColor(29, 31, 33))
        self.plotWidget = pyqtgraph.PlotWidget(self)
        self.plotWidget.setEnabled(False)
        self.plotWidget.setLabel('left', 'Y', units='mm')
        self.plotWidget.setLabel('bottom', 'X', units='mm')
        grid.addWidget(self.plotWidget, 0, 0)
        self.setLayout(grid)
        self.data = {}
        for command in commandList:
            plot = self.plotWidget.getPlotItem()
            scatterPlotItem = pyqtgraph.ScatterPlotItem(pxMode=False)
            plot.addItem(scatterPlotItem)
            self.data[command.name] = {"plot": scatterPlotItem, "data": []}

    def setData(self, commandName, x, y, color):
        self.data[commandName]["data"] = []
        if len(x) == len(y) == len(color):
            for i in range(len(x)):
                self.data[commandName]["data"].append({'pos': (x[i], y[i]), 'pen': {'color': color[i]}, 'brush': color[i]})
        else:
            raise Exception("Data length error")

    def update_graph(self, checkboxes):
        for name, cbList in checkboxes.items():
            if name in self.data:
                if len(cbList) == 1:
                    data = self.data[name]
                    if cbList[0].isChecked():
                        data["plot"].setData(spots=data["data"], pxMode=True, size=2)
                    else:
                        data["plot"].setData([])
                else:
                    raise Exception("Invalid checkboxes structure")


def randomDataDict():
    size = 20
    dic = {"x": list(range(size)), "y": []}
    y = 5
    for i in range(size):
        dic["y"].append(y)
        y += random.randint(-3, 3)
    return dic

def randomColor(size):
    output = []
    for i in range(size):
        output.append(QColor(random.randint(0, 16777215)))
    return output


if __name__ == '__main__':
    app = QApplication(sys.argv)
    backend = Backend()
    main = MainWindow(backend.connect, backend.disconnect, backend.setCurrentTime, backend.setZoom, backend.eraseBeforeCut, backend.cancelLastCut, backend.addCut, backend.sendOrder)
    backend.setGraphicalInterface(main.toolBar, main.consolePanel, main.graphPanel)
    backend.startBackgroundTask()
#     main.consolePanel.setText(
# """424242_info_loli is here !
# 424252_info_loli is not here !
# 424262_info_loli is there !
# 424272_error_This is normal !
# 424282_info_loli is very long like this line, which is especially long !
# 624272_order_Order launched
# 624273_answer_Answer of the order !
# """)
#     d = {
#         "Direction": {"Aim direction": randomDataDict(), "Real direction": randomDataDict()},
#         "Speed PID": {"P err": randomDataDict(), "I err": randomDataDict(), "D err": randomDataDict()},
#         "Translation PID": {"P err": randomDataDict(), "I err": randomDataDict(), "D err": randomDataDict()},
#         "Trajectory PID": {"Translation err": randomDataDict(), "Angular err": randomDataDict()}
#     }
#     s = {
#         "Odometry and Sensors" : {"x": [1,2,3,4,5], "y": [5,4,3,1,2], "color": randomColor(5)}
#     }
#     main.graphPanel.update_data(curveData=d, scatterData=s, origin=15, xMin=0, xMax=19)
    sys.exit(app.exec_())
