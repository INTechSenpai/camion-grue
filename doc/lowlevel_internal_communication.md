# Low level: internal communication protocol

## Frame structure

| Header | Instruction | Length | Payload | Checksum |
|--------|-------------|--------|---------|----------|
| 0xFFFF |   uint8_t   | uint8_t| uint8_t*|  uint8_t |
0        2             3        4        5+N        6+N    Bytes

* Length: number of bytes in the payload

## Instruction set and associated payloads

### [0x00] Sensors board report
uint8_t[9] = [front_left, front, front_right, side_front_left, side_front_right, side_back_left, side_back_right, back_left, back_right]
Values:

[4, MAX_INT]: Distance to the obstacle (mm)
3: No obstacle in front of the sensor
2: obstacle too close to measure (you can consider the distance being the minimum applicable, but not use it for repositionning)
1: Value was not updated yet (use last value instead)
0: Sensor is not working

### [0x01] Set LED lightning mode
uint8_t[1]
Values:
value & b00000001: left_turn_signal
value & b00000010: right_turn_signal
value & b00000100: flashing_lights
value & b00001000: stop_light
value & b00010000: reverse_light
value & b00100000: night_lights_low
value & b01000000: night_lights_high

### [0x..] Actuators board report

