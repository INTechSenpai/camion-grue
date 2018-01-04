from PyQt5.Qt import *
import sys
import Pyro4
import Pyro4.util
import os
from collections import OrderedDict


def main(uri):
    settingsProxy = Pyro4.Proxy(uri)

    root = QApplication(sys.argv)

    window = Settings_GUI(master=None, settingsProxy=settingsProxy)
    window.setWindowTitle('Settings')

    update_timer = QTimer()
    update_timer.timeout.connect(window.check_for_updates)
    update_timer.start(1000) # delay between two calls of 'window.check_for_updates' (in ms)

    window.show()
    sys.exit(root.exec_())


class Settings_GUI(QWidget):
    def __init__(self, master, settingsProxy):
        super().__init__(parent=master)
        self.master = master
        self.settingsProxy = settingsProxy
        descriptor = self.settingsProxy.get_descriptor()
        self.scrollArea = QScrollArea(self)
        self.gui = ParameterContainer_GUI(self.scrollArea, "", descriptor, self.settingsProxy)
        grid = QGridLayout()
        grid.setContentsMargins(0, 0, 0, 0)
        grid.addWidget(self.scrollArea, 1, 0)
        self.setLayout(grid)
        self.scrollArea.setWidgetResizable(True)
        self.scrollArea.setWidget(self.gui)
        menuBar = QMenuBar(self)
        fileMenu = menuBar.addMenu('File')
        saveAction = QAction('Save', self)
        saveAction.setShortcut('Ctrl+S')
        saveAction.triggered.connect(self.save_to_file)
        loadAction = QAction('Load', self)
        loadAction.setShortcut('Ctrl+O')
        loadAction.triggered.connect(self.load_from_file)
        fileMenu.addAction(saveAction)
        fileMenu.addAction(loadAction)
        grid.addWidget(menuBar, 0, 0)

    def update_GUI(self):
        descriptor = self.settingsProxy.get_descriptor()
        self.gui.update_GUI("", descriptor)
        self.settingsProxy.GUI_structure_just_updated()
        self.settingsProxy.GUI_values_just_updated()

    def update_GUI_values(self, param_to_update):
        self.gui.update_GUI_values(param_to_update)

    def check_for_updates(self):
        if self.settingsProxy.is_GUI_structure_to_up():
            self.update_GUI()
        else:
            param_to_update = self.settingsProxy.get_GUI_values_to_up()
            self.update_GUI_values(param_to_update)

    def save_to_file(self):
        userPath, _ = QFileDialog.getSaveFileName(parent=self, caption="Save", directory=os.getcwd())
        if len(userPath) > 0:
            self.settingsProxy.serialize_to_file(userPath)

    def load_from_file(self):
        userPath, _ = QFileDialog.getOpenFileName(parent=self, caption="Load from file", directory=os.getcwd())
        if len(userPath) > 0:
            errorLine = self.settingsProxy.load_from_file(userPath)
            if errorLine == 0: # file read successfully
                self.check_for_updates()
            else:
                errMsg = QMessageBox(self)
                errMsg.setIcon(QMessageBox.Critical)
                errMsg.setWindowTitle("File format incorrect")
                errMsg.setText("The loading stopped at line " + str(errorLine))
                errMsg.setStandardButtons(QMessageBox.Ok)
                errMsg.setDetailedText("Filename: " + userPath)
                errMsg.exec_()




class ParameterContainer_GUI(QWidget):
    def __init__(self, master, path, descriptor, settingsProxy, hideTitle=False):
        super().__init__(master)
        self.settingsProxy = settingsProxy
        self.parametersWidget = OrderedDict()
        self.subContainersWidget = OrderedDict()
        self.grid = QGridLayout()
        if self.settingsProxy.is_visible(path):
            self.update_GUI(path, descriptor, hideTitle)
            self.setLayout(self.grid)

    def update_GUI(self, path, descriptor, hideTitle=False):
        for i in reversed(range(self.grid.count())):
            self.grid.itemAt(i).widget().setParent(None)
        if hideTitle:
            labeled_frame = QFrame(self)
        else:
            labeled_frame = QGroupBox(self)
            sPath = path.split('.')
            if len(sPath) > 1:
                labeled_frame.setTitle(sPath[len(sPath) - 2])
        box_grid = QGridLayout()

        description = self.settingsProxy.get_description(path)
        if description != 'None':
            w_description = QLabel(labeled_frame)
            w_description.setWordWrap(True)
            w_description.setText("<i>" + description + "</i>")
            box_grid.addWidget(w_description, 0, 0)

        self.parametersWidget = OrderedDict()
        self.subContainersWidget = OrderedDict()
        row = 1
        for param in descriptor['parameters']:
            newLabel = QLabel(self)
            description = self.settingsProxy.get_description(path + param)
            if description != 'None' or True:
                newLabel.setToolTip(description)
            newLabel.setText(param)
            box_grid.addWidget(newLabel, row, 0)

            newWidget = Parameter_GUI(labeled_frame, path + param, self.settingsProxy)
            self.parametersWidget[param] = newWidget
            box_grid.addWidget(newWidget, row, 1)
            row += 1

        displayMode = self.settingsProxy.get_display_mode(path)
        if displayMode == 'panel':
            for subContainer in descriptor['subContainers']:
                widgetPath = path + subContainer[0] + "."
                newWidget = ParameterContainer_GUI(labeled_frame, widgetPath, subContainer[1], self.settingsProxy)
                box_grid.addWidget(newWidget, row, 0, 1, 2)
                row += 1
                self.subContainersWidget[subContainer[0]] = newWidget
        elif displayMode == 'tab':
            tabs = QTabWidget(labeled_frame)
            for subContainer in descriptor['subContainers']:
                widgetPath = path + subContainer[0] + "."
                if self.settingsProxy.is_visible(widgetPath):
                    newWidget = ParameterContainer_GUI(tabs, widgetPath, subContainer[1], self.settingsProxy, True)
                    tabs.addTab(newWidget, subContainer[0])
                    self.subContainersWidget[subContainer[0]] = newWidget
            box_grid.addWidget(tabs, row, 0, 1, 2)
            row += 1
        else:
            raise Exception("wrong display mode")

        labeled_frame.setLayout(box_grid)
        self.grid.addWidget(labeled_frame)

    def update_GUI_values(self, param_to_update):
        for name, paramWidget in self.parametersWidget.items():
            if paramWidget.path in param_to_update:
                paramWidget.update_value()
        for name, subContainerWidget in self.subContainersWidget.items():
            subContainerWidget.update_GUI_values(param_to_update)


class Parameter_GUI(QWidget):
    def __init__(self, master, path, settingsProxy):
        super().__init__(master)
        self.path = path
        if settingsProxy.is_visible(path):
            self.grid = QGridLayout()
            self.grid.setContentsMargins(0, 0, 0, 0)

            displayMode = settingsProxy.get_display_mode(path)
            if displayMode == 'default':
                self.input_field = Input_default(self, path, settingsProxy)
            elif displayMode == 'const':
                self.input_field = Input_default(self, path, settingsProxy, const=True)
            elif 'stepper' in displayMode:
                if 'integer' in displayMode:
                    self.input_field = Input_stepper(self, path, settingsProxy, isInteger=True)
                else:
                    self.input_field = Input_stepper(self, path, settingsProxy, isInteger=False)
            elif displayMode == 'checkbox':
                self.input_field = Input_checkbox(self, path, settingsProxy)
            elif displayMode == 'file':
                self.input_field = Input_path(self, path, settingsProxy, isFile=True)
            elif displayMode == 'folder':
                self.input_field = Input_path(self, path, settingsProxy, isFile=False)
            elif displayMode == 'timestamp':
                self.input_field = Input_timestamp(self, path, settingsProxy)
            elif displayMode == 'combobox':
                self.input_field = Input_enum(self, path, settingsProxy)
            elif displayMode == 'str-list':
                self.input_field = Input_list(self, path, settingsProxy)
            elif displayMode == 'float-list':
                self.input_field = Input_list(self, path, settingsProxy, useFloat=True)
            elif displayMode == 'file-list':
                self.input_field = Input_list(self, path, settingsProxy, usePath=True, modeFile=True)
            elif displayMode == 'folder-list':
                self.input_field = Input_list(self, path, settingsProxy, usePath=True, modeFile=False)
            else:
                print("WARNING unknown display mode: ", displayMode)
                self.input_field = Input_default(self, path, settingsProxy)

            self.grid.addWidget(self.input_field)
            self.setLayout(self.grid)
            self.update_value()

    def update_value(self): # update coming from the backend
        self.input_field.update_value()


class Abstract_input(QWidget):
    def __init__(self, master, path, settingsProxy):
        super().__init__(master)
        self.path = path
        self.settingsProxy = settingsProxy
        self.grid = QGridLayout()
        self.grid.setContentsMargins(0, 0, 0, 0)
        self.setLayout(self.grid)

    def update_value(self):
        pass


class Colored_QLineEdit(QWidget):
    def __init__(self, master, update_finished_callback):
        super().__init__(master)
        self.update_finished_callback = update_finished_callback
        self.editColor = QColor(255, 255, 0)
        self.errorColor = QColor(255, 0, 0)
        self.textField = QLineEdit(self)
        self.textField_palette = self.textField.palette()
        self.textField_defaultPalette = self.textField.palette()
        self.textField.editingFinished.connect(self.user_update_finished)
        self.textField.textEdited.connect(self.user_update_started)
        self.grid = QGridLayout()
        self.grid.setContentsMargins(0, 0, 0, 0)
        self.setLayout(self.grid)
        self.grid.addWidget(self.textField)

    def user_update_started(self):
        self.textField_palette.setColor(self.textField.backgroundRole(), self.editColor)
        self.textField.setPalette(self.textField_palette)

    def user_update_finished(self):
        self.textField.setPalette(self.textField_defaultPalette)
        self.update_finished_callback()

    def set_error_color(self):
        self.textField_palette.setColor(self.textField.backgroundRole(), self.errorColor)
        self.textField.setPalette(self.textField_palette)

    def set_default_color(self):
        self.textField.setPalette(self.textField_defaultPalette)

    def setText(self, text):
        self.textField.setText(text)

    def text(self):
        return self.textField.text()


class Input_default(Abstract_input):
    def __init__(self, master, path, settingsProxy, const=False):
        super().__init__(master, path, settingsProxy)
        self.lastUserValue = None
        self.textField = Colored_QLineEdit(self, self.user_update_finished)
        self.textField.textField.setEnabled(not const)
        self.grid.addWidget(self.textField)

    def update_value(self): # update coming from the backend
        self.textField.setText(str(self.settingsProxy.get(self.path)))
        self.textField.set_default_color()

    def user_update_finished(self):
        newValue = self.textField.text()
        if self.lastUserValue is None or self.lastUserValue != newValue:
            self.lastUserValue = newValue
            try:
                self.settingsProxy.set(self.path, newValue)
                self.textField.set_default_color()
            except ValueError:
                self.textField.set_error_color()


class Input_stepper(Abstract_input):
    def __init__(self, master, path, settingsProxy, isInteger):
        super().__init__(master, path, settingsProxy)
        self.min, self.max, self.step = self.settingsProxy.get_bounds(self.path)
        if isInteger:
            self.spinBox = QSpinBox(self)
        else:
            self.spinBox = QDoubleSpinBox(self)
        if self.min is not None:
            self.spinBox.setMinimum(self.min)
        if self.max is not None:
            self.spinBox.setMaximum(self.max)
        if self.step is not None:
            self.spinBox.setSingleStep(self.step)
        self.spinBox.valueChanged.connect(self.user_update_finished)
        size_policy = QSizePolicy()
        size_policy.setHorizontalPolicy(QSizePolicy.Expanding)
        self.spinBox.setSizePolicy(size_policy)
        self.grid.addWidget(self.spinBox, 0, 0)

    def update_value(self):
        self.spinBox.setValue(self.settingsProxy.get(self.path))

    def user_update_finished(self):
        self.settingsProxy.set(self.path, self.spinBox.value())


class Input_checkbox(Abstract_input):
    def __init__(self, master, path, settingsProxy):
        super().__init__(master, path, settingsProxy)
        self.checkbox = QCheckBox('', self)
        self.checkbox.stateChanged.connect(self.user_changed_state)
        self.grid.addWidget(self.checkbox)

    def update_value(self):
        self.checkbox.setChecked(self.settingsProxy.get(self.path))

    def user_changed_state(self):
        self.settingsProxy.set(self.path, self.checkbox.isChecked())


class Input_path(Abstract_input):
    def __init__(self, master, path, settingsProxy, isFile):
        super().__init__(master, path, settingsProxy)
        self.text_field = Input_default(self, path, settingsProxy)
        self.explore_button = ExploreButton(self, '...', self.new_path_chosen, isFile)
        self.grid.addWidget(self.text_field, 0, 0)
        self.grid.addWidget(self.explore_button, 0, 1)

    def update_value(self):
        self.text_field.update_value()

    def new_path_chosen(self, userPath):
        if len(userPath) > 0:
            self.text_field.textField.setText(userPath)
            self.text_field.user_update_finished()


class ExploreButton(QWidget):
    def __init__(self, master, label, callback, modeFile=True):
        super().__init__(master)
        self.callback = callback
        self.modeFile = modeFile
        self.grid = QGridLayout()
        self.grid.setContentsMargins(0, 0, 0, 0)
        self.setLayout(self.grid)
        self.button = QPushButton(label, self)
        self.button.clicked.connect(self.choose_path)
        self.grid.addWidget(self.button)

    def choose_path(self):
        if self.modeFile:
            userPath, _ = QFileDialog.getOpenFileName(parent=self, caption="Open file", directory=os.getcwd())
        else:
            userPath = QFileDialog.getExistingDirectory(parent=self, caption="Open file", directory=os.getcwd())
        self.callback(userPath)


class Input_timestamp(Abstract_input):
    def __init__(self, master, path, settingsProxy):
        super().__init__(master, path, settingsProxy)
        self.text_field = Input_default(self, path, settingsProxy)
        self.text_field.textField.textField.editingFinished.connect(self.user_update_text)
        self.date_editor = QDateTimeEdit(self)
        self.date_editor.dateTimeChanged.connect(self.user_update_date)
        self.current_time = QDateTime()
        self.day_name = QLabel(self)
        self.grid.addWidget(self.text_field, 0, 0)
        self.grid.addWidget(self.day_name, 0, 1)
        self.grid.addWidget(self.date_editor, 0, 2)

    def update_value(self):
        self.text_field.update_value()
        self.user_update_text()

    def user_update_date(self):
        try:
            self.current_time = self.date_editor.dateTime()
            timestamp = float(self.current_time.toMSecsSinceEpoch()) / 1000
            self.text_field.textField.setText(str(timestamp))
            self.text_field.user_update_finished()
            self.update_day_name()
            self.text_field.textField.set_default_color()
        except ValueError:
            self.text_field.textField.set_error_color()

    def user_update_text(self):
        try:
            timestamp = int(float(self.text_field.textField.text()) * 1000)
            self.current_time.setMSecsSinceEpoch(timestamp)
            self.date_editor.setDateTime(self.current_time)
            self.update_day_name()
            self.text_field.textField.set_default_color()
        except ValueError:
            self.text_field.textField.set_error_color()

    def update_day_name(self):
        self.day_name.setText(self.current_time.toString('ddd'))


class Input_enum(Abstract_input):
    def __init__(self, master, path, settingsProxy):
        super().__init__(master, path, settingsProxy)
        self.combobox = QComboBox(self)
        self.available_values = settingsProxy.get_available_values(path)
        self.combobox.setEditable(False)
        self.combobox.addItems(self.available_values)
        self.combobox.activated[str].connect(self.on_selection)
        size_policy = QSizePolicy()
        size_policy.setHorizontalPolicy(QSizePolicy.Expanding)
        self.combobox.setSizePolicy(size_policy)
        self.grid.addWidget(self.combobox, 0,0)

    def on_selection(self, item):
        self.settingsProxy.set(self.path, item)

    def update_value(self):
        newValue = self.settingsProxy.get(self.path)
        if newValue in self.available_values:
            self.combobox.setEditText(str(newValue))
        else:
            raise ValueError


class Input_list(Abstract_input):
    def __init__(self, master, path, settingsProxy, useFloat=False, usePath=False, modeFile=True):
        super().__init__(master, path, settingsProxy)
        self.useFloat = useFloat
        self.usePath = usePath
        self.modeFile = modeFile
        self.current_list = []
        self.widget_list = []
        self.add_button = QPushButton('add', self)
        size_policy = QSizePolicy()
        size_policy.setHorizontalPolicy(QSizePolicy.Expanding)
        self.add_button.setSizePolicy(size_policy)
        self.add_button.clicked.connect(self.add_element)
        self.grid.addWidget(self.add_button)

    def update_display(self):
        for i in reversed(range(self.grid.count())):
            self.grid.itemAt(i).widget().setParent(None)
        self.widget_list = []
        for index in range(len(self.current_list)):
            newWidget = Input_list_element(self, index, self.user_edit_element, self.del_element, self.usePath, self.modeFile)
            self.widget_list.append(newWidget)
            self.grid.addWidget(newWidget, index, 0)
        self.grid.addWidget(self.add_button, len(self.current_list), 0)
        self.update_fields()

    def update_fields(self):
        index = 0
        for item in self.current_list:
            self.widget_list[index].update_value(item)
            index += 1

    def add_element(self):
        if self.useFloat:
            self.current_list.append(0.0)
        elif self.usePath:
            self.current_list.append('/')
        else:
            self.current_list.append('')
        self.update_display()
        self.settingsProxy.set(self.path, self.current_list)

    def del_element(self, index):
        del self.current_list[index]
        self.update_display()
        self.settingsProxy.set(self.path, self.current_list)

    def user_edit_element(self, index):
        content = self.widget_list[index].text()
        if self.useFloat:
            try:
                self.current_list[index] = float(content)
                self.widget_list[index].set_default_color()
            except ValueError:
                self.widget_list[index].set_error_color()
        else:
            self.current_list[index] = content
        self.settingsProxy.set(self.path, self.current_list)

    def update_value(self):
        self.current_list = self.settingsProxy.get(self.path)
        if len(self.current_list) == len(self.widget_list):
            self.update_fields()
        else:
            self.update_display()


class Input_list_element(QWidget):
    def __init__(self, master, index, callback_user_edit, callback_delete, usePath=False, modeFile=True):
        super().__init__(master)
        self.id = index
        self.callback_user_edit = callback_user_edit
        self.callback_delete = callback_delete
        self.grid = QGridLayout()
        self.grid.setContentsMargins(0, 0, 0, 0)
        self.setLayout(self.grid)
        self.text_field = Colored_QLineEdit(self, self.user_edit)
        self.delete_button = QPushButton('del', self)
        self.delete_button.clicked.connect(self.delete_entry)
        self.grid.addWidget(self.text_field, 0, 0)
        self.grid.addWidget(self.delete_button, 0, 2)
        if usePath:
            explore_button = ExploreButton(self, "...", self.new_path_chosen, modeFile)
            self.grid.addWidget(explore_button, 0, 1)

    def user_edit(self):
        self.callback_user_edit(self.id)

    def delete_entry(self):
        self.callback_delete(self.id)

    def update_value(self, value):
        self.text_field.setText(str(value))

    def text(self):
        return self.text_field.text()

    def set_error_color(self):
        self.text_field.set_error_color()

    def set_default_color(self):
        self.text_field.set_default_color()

    def new_path_chosen(self, userPath):
        if len(userPath) > 0:
            self.update_value(userPath)
            self.callback_user_edit(self.id)
