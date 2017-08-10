from PyQt5.QtWidgets import (QWidget, QGridLayout, QSplitter, QApplication, QFrame, QPushButton, QTabWidget, QSlider,
                             QLabel, QLineEdit, QComboBox, QScrollArea, QCheckBox, QTextEdit, QHBoxLayout, QVBoxLayout, QLayoutItem)
from PyQt5.QtGui import (QIcon, QPixmap, QFont, QColor)
from PyQt5.QtCore import Qt
import sys
from CommandList import *


DEFAULT_ROBOT_IP = "172.16.0.2"


class MainWindow(QWidget):
    def __init__(self):
        super().__init__()
        grid = QGridLayout()

        self.toolBar = ToolBar(self)
        self.commandPanelScrollArea = QScrollArea(self)
        self.commandPanelScrollArea.setFrameShape(QFrame.StyledPanel)
        self.commandPanel = CommandPanel(self.commandPanelScrollArea)
        self.commandPanelScrollArea.setWidgetResizable(True)
        self.commandPanelScrollArea.setWidget(self.commandPanel)
        self.consolePanel = ConsolePanel(self)
        self.consolePanel.setFrameShape(QFrame.StyledPanel)
        self.graphPanel = GraphPanel(self)
        self.graphPanel.setFrameShape(QFrame.StyledPanel)

        self.splitter = QSplitter(Qt.Horizontal)
        self.splitter.addWidget(self.commandPanelScrollArea)
        self.splitter.addWidget(self.consolePanel)
        self.splitter.addWidget(self.graphPanel)

        self.splitter.setStretchFactor(0, 1)
        self.splitter.setStretchFactor(1, 2)
        self.splitter.setStretchFactor(2, 2)

        grid.addWidget(self.toolBar, 0, 0)
        grid.addWidget(self.splitter, 1, 0)
        grid.setRowStretch(0, 0)
        grid.setRowStretch(1, 1)

        self.setLayout(grid)
        self.setWindowTitle('Lowlevel Interface')
        self.setWindowIcon(QIcon('intech.ico'))
        self.show()


class ToolBar(QWidget):
    def __init__(self, master):
        super().__init__(master)
        grid = QGridLayout()

        self.protocolTabs = QTabWidget(self)
        self.ipChooser = IpChooser(self)
        self.serialChooser = SerialChooser(self)

        self.protocolTabs.addTab(self.ipChooser, "TCP/IP")
        self.protocolTabs.addTab(self.serialChooser, "COM")

        self.bConnect = QPushButton('Connect', self)
        self.bDefineStartTime = QPushButton("Define start time")
        self.bCancelDefinition = QPushButton("Cancel definition")
        self.bEraseBeforeStartTime = QPushButton("Erase before start time")

        self.timeSlider = AnnotatedSlider(self, 0, 10, unit="ms", icon=None)
        self.zoomSlider = AnnotatedSlider(self, 0, 5, unit="ms", icon=None)

        grid.addWidget(self.protocolTabs, 0, 0, 2, 1)
        grid.addWidget(self.bConnect, 0, 1)
        grid.addWidget(self.bDefineStartTime, 0, 2)
        grid.addWidget(self.bCancelDefinition, 1, 2)
        grid.addWidget(self.bEraseBeforeStartTime, 1, 1)
        grid.addWidget(self.timeSlider, 0, 3, 2, 1)
        grid.addWidget(self.zoomSlider, 0, 4, 2, 1)

        grid.setColumnStretch(0, 0)
        grid.setColumnStretch(1, 0)
        grid.setColumnStretch(2, 0)
        grid.setColumnStretch(3, 2)
        grid.setColumnStretch(4, 1)
        grid.setSpacing(5)
        grid.setContentsMargins(0,0,0,0)
        self.setLayout(grid)


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
        return self.comboBox.currentText()

    def markBlank(self):
        self.comboBox.setStyleSheet("")

    def markWrong(self):
        self.comboBox.setStyleSheet("""QComboBox { background-color: rgb(249, 83, 83) }""")

    def markConnected(self):
        self.comboBox.setStyleSheet("""QComboBox { background-color: rgb(203, 230, 163) }""")


class AnnotatedSlider(QWidget):
    def __init__(self, master, sMin, sMax, unit, icon):
        super().__init__(master)
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
        if self.slider.value() == self.sMax:
            self.slider.setValue(sMax)
        elif self.slider.value() == self.sMin:
            self.slider.setValue(sMin)
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
        except ValueError:
            self.lineEdit.setText(str(self.slider.value()))

    def _slider_moved_by_user(self, value):
        self.lineEdit.setText(str(value))

    def _string_to_int(self, string):
        val = int(string)
        if val < self.sMin or val > self.sMax:
            raise ValueError
        return val


class CommandPanel(QWidget):
    def __init__(self, master):
        super().__init__(master)
        grid = QGridLayout()
        row = 0
        for command in COMMAND_LIST:
            if command.type != CommandType.SUBSCRIPTION:
                cb = CommandBox(self, command)
                cb.setFrameShape(QFrame.StyledPanel)
                grid.addWidget(cb, row, 0)
                row += 1
        grid.setRowStretch(row, 1)
        grid.setContentsMargins(5,5,5,5)
        self.setLayout(grid)


class CommandBox(QFrame):
    def __init__(self, master, command):
        super().__init__(master)
        self.command = command
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
        self.sendButton.setIcon(QIcon('send.ico'))

        grid.addWidget(self.id, 0, 0)
        grid.addWidget(self.nameButton, 0, 1)
        grid.addWidget(self.sendButton, 0, 2)
        grid.setContentsMargins(5,3,5,3)
        grid.setColumnStretch(0, 0)
        grid.setColumnStretch(1, 1)
        grid.setColumnStretch(2, 0)

        self.label_lineEdit_field = []
        row = 1
        for field in self.command.inputFormat:
            label = QLabel(field.name, self)
            label.setHidden(True)
            grid.addWidget(label, row, 0)
            lineEdit = QLineEdit(str(field.default), self)
            lineEdit.setHidden(True)
            grid.addWidget(lineEdit, row, 1)
            self.label_lineEdit_field.append((label, lineEdit, field))
            row += 1
        self.bReset = QPushButton(self)
        self.bReset.setIcon(QIcon('reset.ico'))
        self.bReset.clicked.connect(self.reset_values)
        self.bReset.setHidden(True)
        grid.addWidget(self.bReset, 1, 2)
        self.setLayout(grid)

    def click_event(self):
        if self.nameButton.isChecked():
            self.expand()
        else:
            self.collapse()

    def expand(self):
        self.bReset.setHidden(False)
        for label, lineEdit, field in self.label_lineEdit_field:
            label.setHidden(False)
            lineEdit.setHidden(False)

    def collapse(self):
        self.bReset.setHidden(True)
        for label, lineEdit, field in self.label_lineEdit_field:
            label.setHidden(True)
            lineEdit.setHidden(True)

    def reset_values(self):
        for label, lineEdit, field in self.label_lineEdit_field:
            lineEdit.setText(str(field.default))


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

    def formatTextToConsole(self, text):
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
            if sLine[1] == "info":
                if not self.cbInfo.isChecked():
                    continue
                color = self.textColor
            elif sLine[1] == "error":
                if not self.cbErrors.isChecked():
                    continue
                color = self.errorColor
            elif sLine[1] == "order":
                if not self.cbOrders.isChecked():
                    continue
                color = self.orderColor
            elif sLine[1] == "answer":
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
        self.console.setPlainText("")
        self.formatTextToConsole(newText)

    def appendText(self, text):
        self.consoleText += text
        self.formatTextToConsole(text)

    def settings_changed(self):
        self.console.setPlainText("")
        self.formatTextToConsole(self.consoleText)


class GraphPanel(QFrame):
    def __init__(self, master):
        super().__init__(master)
        grid = QGridLayout()
        grid.addWidget(QPushButton('prout', self))
        self.setLayout(grid)


if __name__ == '__main__':
    app = QApplication(sys.argv)
    main = MainWindow()
    main.consolePanel.setText(
"""424242_info_loli is here !
424252_info_loli is not here !
424262_info_loli is there !
424272_error_This is normal !
424282_info_loli is very long like this line, which is especially long !
624272_order_Order launched
624273_answer_Answer of the order !
""")
    sys.exit(app.exec_())