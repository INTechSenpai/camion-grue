# Low level: internal communication protocol

## Frame structure

| Header | Instruction | Length |  Payload  | Checksum |
|--------|-------------|--------|-----------|----------|
| 0xFFFF |   uint8_t   | uint8_t| uint8_t[] |  uint8_t |
0        2             3        4          5+N        6+N    Bytes

* Length: number of bytes in the payload

## Instruction set and associated payloads

### [0x00] Sensors board report
uint8_t[9] = [front, front_left, front_right, side_front_left, side_front_right, side_back_left, side_back_right, back_left, back_right]
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
value & b10000000: alarm_signal

### [0x02] Actuators board move ACK
uint8_t[0]
Indique que "isMoving" vient de passer à "true"

### [0x03] Actuators board report
uint8_t[38]
(bool)moving
(int)status
(bool)cube_in_plier
(int)tof_g
(int)tof_d
(float)angle_t_g
(float)angle_t_d
(float)angle_h_grue
(float)angle_v_grue
(float)angle_head_grue_local
(float)pos_pince_grue

### [0x04] Go to home position
uint8_t[0]
Retour à la position initiale du bras (avec pince entre-ouverte)

### [0x05] Take cube using sensor
uint8_t[4]
(float) angle (h) de prise (approx)

### [0x06] Take cube at fixed position
uint8_t[4]
(float) angle (h) de prise (exact)

### [0x07] Store cube inside robot
uint8_t[0]

### [0x08] Store cube on top
uint8_t[0]

### [0x09] Take cube from storage
uint8_t[0]

### [0x0A] Put cube on pile using sensor 
uint8_t[8]
(float) angle (h) de dépose (approx)
(int)floor (0=sur le sol; étage max: 4)

### [0x0B] Put cube on pile at fixed position
uint8_t[8]
(float) angle (h) de dépose (exact)
(int)floor (0=sur le sol; étage max: 4)

### [0x0C] Put arm at given position
uint8_t[16]
(float)angle_h_grue
(float)angle_v_grue
(float)angle_head_grue
(float)pos_pince_grue

### [0x0D] Set sensors angles
uint8_t[8]
(float)angle_t_g
(float)angle_t_d

### [0x0E] Set displayed score
uint8_t[4]
(int)score

