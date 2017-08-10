from enum import Enum


class Command:
    def __init__(self, ID, name, t, inputFormat, outputFormat, outputInfoFrame=False):
        if isinstance(ID, int) and 0 <= ID < 256 and \
                isinstance(name, str) and isinstance(t, CommandType) and \
                isinstance(inputFormat, list) and isinstance(outputFormat, list) and isinstance(outputInfoFrame, bool):
            for entry in inputFormat:
                if not isinstance(entry, Field):
                    raise ValueError
            for entry in outputFormat:
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


class CommandType(Enum):
    SUBSCRIPTION    = 0
    LONG_ORDER      = 1
    SHORT_ORDER     = 2


class NumberType(Enum):
    UINT8   = 0
    INT8    = 1
    UINT16  = 2
    INT16   = 3
    UINT32  = 4
    INT32   = 5


COMMAND_LIST = [
    Command(0x59, "Get color", CommandType.SHORT_ORDER, [], [Field("Color", NumberType.UINT8, {0x00: "Blue", 0x01: "Yellow", 0x02: "Unknown"})]),
    Command(0x5A, "Ping", CommandType.SHORT_ORDER, [], []),
    Command(0x5B, "Set position", CommandType.SHORT_ORDER, [Field("x", NumberType.INT16, description="mm"), Field("y", NumberType.INT16, description="mm"), Field("Angle", NumberType.INT16, description="milli-radians")], [])
]
