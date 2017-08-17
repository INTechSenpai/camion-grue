from enum import Enum
from PyQt5.QtGui import QColor


class Command:
    def __init__(self, ID, name, t, inputFormat, outputFormat, outputInfoFrame=False):
        if isinstance(outputInfoFrame, bool) and isinstance(outputFormat, list):
            if outputInfoFrame:
                for entry in outputFormat:
                    if not isinstance(entry, InfoField):
                        raise ValueError
            else:
                for entry in outputFormat:
                    if not isinstance(entry, Field):
                        raise ValueError
        if isinstance(ID, int) and 0 <= ID < 256 and isinstance(name, str) and isinstance(t, CommandType) and isinstance(inputFormat, list):
            for entry in inputFormat:
                if not isinstance(entry, Field):
                    raise ValueError
            self.id = ID
            self.name = name
            self.type = t
            self.inputFormat = inputFormat
            self.outputFormat = outputFormat
            self.outputInfoFrame = outputInfoFrame
        else:
            raise ValueError


class Field:
    def __init__(self, name, t, legend=None, default=0, optional=False, description=""):
        if isinstance(name, str) and isinstance(t, NumberType) and (legend is None or isinstance(legend, dict)) and isinstance(default, int) and isinstance(optional, bool) and isinstance(description, str):
            self.name = name
            self.type = t
            if legend is None:
                self.legend = {}
            else:
                self.legend = legend
            self.default = default
            self.optional = optional
            self.description = description
        else:
            raise ValueError


class InfoField:
    def __init__(self, name, color=QColor(197, 200, 198), isBoolean=False, description=""):
        if isinstance(name, str) and isinstance(color, QColor) and isinstance(isBoolean, bool) and isinstance(description, str):
            self.name = name
            self.color = color
            self.isBoolean = isBoolean
            self.description = description
        else:
            raise ValueError


class CommandType(Enum):
    SUBSCRIPTION_TEXT           = 0
    SUBSCRIPTION_CURVE_DATA     = 1
    SUBSCRIPTION_SCATTER_DATA   = 2
    LONG_ORDER                  = 3
    SHORT_ORDER                 = 4


class NumberType(Enum):
    UINT8   = 0
    INT8    = 1
    UINT16  = 2
    INT16   = 3
    UINT32  = 4
    INT32   = 5


COMMAND_LIST = [
    # Channels
    Command(0x00, "Odometry and Sensors", CommandType.SUBSCRIPTION_SCATTER_DATA, [Field("Subscribe", NumberType.UINT8, {0x00: "No", 0x01: "Yes"})], [Field("x", NumberType.INT16, description="mm"), Field("y", NumberType.INT16, description="mm"), Field("Angle", NumberType.INT16, description="milli-radians")]),
    Command(0x04, "Direction", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", NumberType.UINT8, {0x00: "No", 0x01: "Yes"})], [InfoField("Aim direction", QColor(255,0,0)), InfoField("Real direction")], outputInfoFrame=True),
    Command(0x06, "Speed PID", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", NumberType.UINT8, {0x00: "No", 0x01: "Yes"})], [InfoField("P err", QColor(0,255,0)), InfoField("I err", QColor(0,0,255)), InfoField("D err")], outputInfoFrame=True),
    Command(0x07, "Translation PID", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", NumberType.UINT8, {0x00: "No", 0x01: "Yes"})], [InfoField("P err"), InfoField("I err"), InfoField("D err")], outputInfoFrame=True),
    Command(0x08, "Trajectory PID", CommandType.SUBSCRIPTION_CURVE_DATA, [Field("Subscribe", NumberType.UINT8, {0x00: "No", 0x01: "Yes"})], [InfoField("Translation err"), InfoField("Angular err")], outputInfoFrame=True),

    # Long orders

    # Short orders
    Command(0x59, "Get color", CommandType.SHORT_ORDER, [], [Field("Color", NumberType.UINT8, {0x00: "Blue", 0x01: "Yellow", 0x02: "Unknown"})]),
    Command(0x5C, "Set color", CommandType.SHORT_ORDER, [Field("Color", NumberType.UINT8, {0x00: "Blue", 0x01: "Yellow", 0x02: "Unknown"}, default=2)], []),

    Command(0x5A, "Ping", CommandType.SHORT_ORDER, [], []),
    Command(0x5B, "Set position", CommandType.SHORT_ORDER, [Field("x", NumberType.INT16, description="mm"), Field("y", NumberType.INT16, description="mm"), Field("Angle", NumberType.INT16, description="milli-radians")], [])
]
