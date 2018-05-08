from enum import Enum
from PyQt5.QtGui import QColor

INFO_CHANNEL        = 0x01
ERROR_CHANNEL       = 0x02
TRACE_CHANNEL       = 0x03
SPY_ORDER_CHANNEL   = 0x04

INFO_CHANNEL_NAME       = "info"
ERROR_CHANNEL_NAME      = "error"
TRACE_CHANNEL_NAME      = "trace"
SPY_ORDER_CHANNEL_NAME  = "spyOrder"
ANSWER_DESCRIPTOR       = "answer"

TIMESTAMP_INFO_FIELD = "timestamp"

CONSOLE_HISTORY_SIZE = 4000


class Command:
    def __init__(self, ID, name, commandType, inputFormat, outputFormat, outputInfoFrame=False):
        assert isinstance(ID, int) and 0 <= ID < 256
        assert isinstance(name, str)
        assert isinstance(commandType, CommandType) and isinstance(inputFormat, list)
        assert isinstance(outputInfoFrame, bool) and isinstance(outputFormat, list)
        if outputInfoFrame:
            for entry in outputFormat:
                assert isinstance(entry, InfoField)
        else:
            for entry in outputFormat:
                assert isinstance(entry, Field)
        for entry in inputFormat:
            assert isinstance(entry, Field)
        self.id = ID
        self.name = name
        self.type = commandType
        self.inputFormat = inputFormat
        self.outputFormat = outputFormat
        self.outputInfoFrame = outputInfoFrame


class Field:
    def __init__(self, name, dataType, legend=None, default=0, repeatable=False, description=""):
        assert isinstance(name, str) and isinstance(repeatable, bool) and isinstance(description, str)
        if dataType == Enum:
            assert isinstance(legend, list)
            assert len(legend) > 0
            assert isinstance(default, int)
            assert 0 <= default < len(legend)
            for entry in legend:
                assert isinstance(entry, str)
            self.legend = legend
        else:
            assert dataType == int or dataType == float or dataType == bool
            assert legend is None
            assert isinstance(default, int) or isinstance(default, float)
            self.legend = []
        self.name = name
        self.type = dataType
        self.default = default
        self.repeatable = repeatable
        self.description = description


class InfoField:
    def __init__(self, name, color=QColor(197, 200, 198), description=""):
        assert isinstance(name, str) and isinstance(color, QColor) and isinstance(description, str)
        self.name = name
        self.color = color
        self.description = description


class CommandType(Enum):
    SUBSCRIPTION_TEXT           = 0
    SUBSCRIPTION_CURVE_DATA     = 1
    SUBSCRIPTION_SCATTER_DATA   = 2
    LONG_ORDER                  = 3
    SHORT_ORDER                 = 4



COMMAND_LIST = [

# Channels
Command(0x00, "Odometry and Sensors", CommandType.SUBSCRIPTION_SCATTER_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [Field("x", int, description="mm"),
         Field("y", int, description="mm"),
         Field("Angle", float, description="radians"),
         Field("Curvature", float, description="m^-1"),
         Field("Trajectory index", int),
         Field("ToF Front", int, description="mm"),
         Field("ToF FrontLeft", int, description="mm"),
         Field("ToF FrontRight", int, description="mm"),
         Field("ToF SideFrontLeft", int, description="mm"),
         Field("ToF SideFrontRight", int, description="mm"),
         Field("ToF SideBackLeft", int, description="mm"),
         Field("ToF SideBackRight", int, description="mm"),
         Field("ToF BackLeft", int, description="mm"),
         Field("ToF BackRight", int, description="mm"),
         Field("ToF LongRangeLeft", int, description="mm"),
         Field("ToF LongRangeRight", int, description="mm"),
         Field("Angle LongRangeLeft", float, description="radians"),
         Field("Angle LongRangeRight", float, description="radians"),
         Field("Angle Grue", float, description="radians")]),

Command(0x01, "Info", CommandType.SUBSCRIPTION_TEXT, [Field("Subscribe", Enum, ["No", "Yes"])], [InfoField("Info")], outputInfoFrame=True),
Command(0x02, "Error", CommandType.SUBSCRIPTION_TEXT, [Field("Subscribe", Enum, ["No", "Yes"])], [InfoField("Error")], outputInfoFrame=True),
Command(0x03, "Trace", CommandType.SUBSCRIPTION_TEXT, [Field("Subscribe", Enum, ["No", "Yes"])], [InfoField("Trace")], outputInfoFrame=True),
Command(0x04, "Spy orders", CommandType.SUBSCRIPTION_TEXT, [Field("Subscribe", Enum, ["No", "Yes"])], [InfoField("Spy orders")], outputInfoFrame=True),

Command(0x05, "Direction", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("Aim direction", QColor(0, 0, 255), description="m^-1"),
         InfoField("Real direction", QColor(0, 255, 0), description="m^-1")], outputInfoFrame=True),
Command(0x06, "Aim trajectory", CommandType.SUBSCRIPTION_SCATTER_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("x", QColor(255, 255, 255), description="mm"),
         InfoField("y", QColor(255, 255, 255), description="mm")], outputInfoFrame=True),
Command(0x07, "Speed PID", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("Real speed", QColor(0, 255, 0), description="mm/s"),
         InfoField("PWM output", QColor(255, 0, 0), description="PWM"),
         InfoField("Aim speed", QColor(0, 0, 255), description="mm/s")], outputInfoFrame=True),
Command(0x08, "Translation PID", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("Current translation", QColor(0, 255, 0), description="mm"),
         InfoField("Output speed", QColor(255, 0, 0), description="mm/s"),
         InfoField("Aim translation", QColor(0, 0, 255), description="mm")], outputInfoFrame=True),
Command(0x09, "Trajectory PID", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("Translation err", QColor(255, 0, 255), description="mm"),
         InfoField("Angular err", QColor(255, 255, 0), description="radians"),
         InfoField("Curvature order", QColor(0, 255, 255), description="m^-1")], outputInfoFrame=True),
Command(0x0A, "Blocking mgr", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("Aim speed", QColor(0, 0, 255), description="mm/s"),
         InfoField("Real speed", QColor(0, 255, 0), description="mm/s"),
         InfoField("Motor blocked", QColor(255, 0, 0), description="boolean")], outputInfoFrame=True),
Command(0x0B, "Stopping mgr", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", Enum, ["No", "Yes"])],
        [InfoField(TIMESTAMP_INFO_FIELD),
         InfoField("Current speed", QColor(0, 255, 0), description="mm/s"),
         InfoField("Robot stopped", QColor(255, 0, 0), description="boolean")], outputInfoFrame=True),


# Long orders
Command(0x20, "Follow trajectory",  CommandType.LONG_ORDER, [], [Field("Move state", int)]),
Command(0x21, "Stop",               CommandType.LONG_ORDER, [], []),
Command(0x22, "Wait for jumper",    CommandType.LONG_ORDER, [], []),
Command(0x23, "Start match chrono", CommandType.LONG_ORDER, [], []),
Command(0x24, "Arm go home",        CommandType.LONG_ORDER, [Field("side", float)], [Field("Arm status", int)]),
Command(0x25, "Take cube w/sensor", CommandType.LONG_ORDER, [Field("angle", float)], [Field("Arm status", int)]),
Command(0x26, "Take cube fixed",    CommandType.LONG_ORDER, [Field("angle", float)], [Field("Arm status", int)]),
Command(0x27, "Store cube inside",  CommandType.LONG_ORDER, [], [Field("Arm status", int)]),
Command(0x28, "Store cube on top",  CommandType.LONG_ORDER, [Field("side", float)], [Field("Arm status", int)]),
Command(0x29, "Take from storage",  CommandType.LONG_ORDER, [Field("side", float)], [Field("Arm status", int)]),
Command(0x2A, "Put on pile",        CommandType.LONG_ORDER, [Field("angle", float), Field("floor", int)], [Field("Arm status", int)]),
Command(0x2B, "Put on pile fixed",  CommandType.LONG_ORDER, [Field("angle", float), Field("floor", int)], [Field("Arm status", int)]),
Command(0x2C, "Arm go to",          CommandType.LONG_ORDER,
        [Field("angle H", float),
         Field("angle V", float),
         Field("angle Head", float, default=4.974),
         Field("plier pos", float, default=25)],
        [Field("Arm status", int)]),
Command(0x2D, "Arm stop",           CommandType.LONG_ORDER, [], []),
Command(0x2E, "Arm push button",    CommandType.LONG_ORDER, [Field("angle", float, default=0.1), Field("side", int)], [Field("Arm status", int)]),
Command(0x2F, "Arm push bee",       CommandType.LONG_ORDER, [Field("angle", float)], [Field("Arm status", int)]),
Command(0x30, "Take from human",    CommandType.LONG_ORDER, [Field("side", float)], [Field("Arm status", int)]),


# Short orders
Command(0x80, "Ping",           CommandType.SHORT_ORDER, [], [Field("Zero", int)]),
Command(0x81, "Get color",      CommandType.SHORT_ORDER, [], [Field("Color", Enum, ["Orange", "Green", "Unknown"])]),
Command(0x82, "Edit position",  CommandType.SHORT_ORDER, [Field("x", int), Field("y", int), Field("angle", float)], []),
Command(0x83, "Set position",   CommandType.SHORT_ORDER, [Field("x", int), Field("y", int), Field("angle", float)], []),
Command(0x84, "Append traj pt", CommandType.SHORT_ORDER,
        [Field("x", int, repeatable=True),
         Field("y", int, repeatable=True),
         Field("angle", float, repeatable=True),
         Field("curvature", float, repeatable=True),
         Field("speed", float, repeatable=True),
         Field("stop point", bool, repeatable=True),
         Field("end of traj", bool, repeatable=True)],
        [Field("Ret code", Enum, ["Success", "Failure"])]),
Command(0x85, "Edit traj pt", CommandType.SHORT_ORDER,
        [Field("index", int),
         Field("x", int, repeatable=True),
         Field("y", int, repeatable=True),
         Field("angle", float, repeatable=True),
         Field("curvature", float, repeatable=True),
         Field("speed", float, repeatable=True),
         Field("stop point", bool, repeatable=True),
         Field("end of traj", bool, repeatable=True)],
        [Field("Ret code", Enum, ["Success", "Failure"])]),
Command(0x86, "Delete traj pt", CommandType.SHORT_ORDER,
        [Field("index", int)], [Field("Ret code", Enum, ["Success", "Failure"])]),
Command(0x87, "Set sensors angles", CommandType.SHORT_ORDER,
        [Field("left angle", float), Field("right angle", float)], []),
Command(0x88, "Set score", CommandType.SHORT_ORDER,
        [Field("score", int)], []),
Command(0x89, "Get arm position", CommandType.SHORT_ORDER, [],
		[Field("Angle H", float),
		 Field("Angle V", float),
		 Field("Angle Head", float),
		 Field("Pos Plier", float)]),

Command(0x90, "Display",        CommandType.SHORT_ORDER, [], []),
Command(0x91, "Save",           CommandType.SHORT_ORDER, [], []),
Command(0x92, "Load defaults",  CommandType.SHORT_ORDER, [], []),
Command(0x93, "Get position",   CommandType.SHORT_ORDER, [],
        [Field("x", int, description="mm"),
         Field("y", int, description="mm"),
         Field("Angle", float, description="radians")]),
Command(0x94, "Get battery",    CommandType.SHORT_ORDER, [], [Field("Battery level", int)]),
Command(0x95, "Control level",  CommandType.SHORT_ORDER,
        [Field("Control level", Enum, ["None", "PWM", "Speed", "Translation", "Trajectory"])], []),
Command(0x96, "Set monitored motor", CommandType.SHORT_ORDER,
        [Field("Motor", Enum, ["None", "Front left", "Front right", "Back left", "Back right"])], []),
Command(0x97, "Start manual move",      CommandType.SHORT_ORDER, [], []),
Command(0x98, "Set PWM",                CommandType.SHORT_ORDER, [Field("PWM", int)], []),
Command(0x99, "Set max speed",          CommandType.SHORT_ORDER, [Field("Speed", float)], []),
Command(0x9A, "Set aim distance",       CommandType.SHORT_ORDER, [Field("Distance", float)], []),
Command(0x9B, "Set curvature",          CommandType.SHORT_ORDER, [Field("Curvature", float)], []),
Command(0x9C, "Speed tunings",          CommandType.SHORT_ORDER, [Field("Kp", float), Field("Ki", float), Field("Kd", float)], []),
Command(0x9D, "Translation tunings",    CommandType.SHORT_ORDER, [Field("Kp", float), Field("Kd", float), Field("minSpeed", float)], []),
Command(0x9E, "Trajectory tunings",     CommandType.SHORT_ORDER, [Field("K1", float), Field("K2", float)], []),
Command(0x9F, "Blocking tunings",       CommandType.SHORT_ORDER, [Field("Sensibility", float), Field("Response-time", int)], []),
Command(0xA0, "Stopping tunings",       CommandType.SHORT_ORDER, [Field("Epsilon", float), Field("Response-time", int)], []),
Command(0xA1, "Set max acceleration",   CommandType.SHORT_ORDER, [Field("Acceleration", float)], []),
Command(0xA2, "Set max deceleration",   CommandType.SHORT_ORDER, [Field("Deceleration", float)], []),
Command(0xA3, "Set max curvature",      CommandType.SHORT_ORDER, [Field("Curvature", float)], []),

]

