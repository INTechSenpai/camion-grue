#ifndef _CONFIG_h
#define _CONFIG_h

/* Report period */
#define REPORT_PERIOD   20  // ms

/* Master serial */
#define MASTER_SERIAL  Serial1
#define BAUDRATE_SERIAL 115200

/* Serial AX12 */
#define SERIAL_AX12             Serial2
#define SERIAL_AX12_BAUDRATE    1000000

/* Serial XL320 */
#define SERIAL_XL320            Serial3
#define SERIAL_XL320_BAUDRATE   1000000


/* DELs */
#define PIN_DEL_GYRO_G  6
#define PIN_DEL_GYRO_D  20


/* Capteurs ToF */
#define PIN_EN_TOF_G    2
#define PIN_EN_TOF_D    13
#define PIN_EN_TOF_INT  14
#define PIN_EN_TOF_EXT  17


/* Moteurs */
#define PIN_EN_MOT_H    3
#define PIN_A_MOT_H     4
#define PIN_B_MOT_H     5
#define PIN_EN_MOT_V    21
#define PIN_A_MOT_V     22
#define PIN_B_MOT_V     23


/* Encodeurs */
#define PIN_ENC_MOT_H_A 11
#define PIN_ENC_MOT_H_B 12
#define PIN_ENC_MOT_V_A 15
#define PIN_ENC_MOT_V_B 16


/* Boutons */
#define PIN_BUTTON_GROUP_1  A10
#define PIN_BUTTON_GROUP_2  A11


/* Config bras */
#define FREQ_ASSERV		1000					// Fréquence d'asservissement (Hz)
#define PERIOD_ASSERV	(1000000 / FREQ_ASSERV)	// Période d'asservissement (µs)
#define AVERAGE_SPEED_SIZE  50
#define AX12_CONTROL_PERIOD 5  // ms

#define ID_AX12_HEAD    0
#define ID_AX12_PLIER   1

#define HMOTOR_TICK_TO_RAD  0.000865892 // radian/tick
#define VMOTOR_TICK_TO_MM   0.00489995  // mm/tick

#define BLOCKING_DELAY          100 // ms
#define BLOCKING_SENSIBILITY    0

#define H_SPEED_KP    900
#define H_SPEED_KI    15000
#define H_SPEED_KD    30

#define V_SPEED_KP    90
#define V_SPEED_KI    1500
#define V_SPEED_KD    3

#define HMOTOR_KP   10
#define HMOTOR_KD   0.5

#define HMOTOR_MAX_SPEED    3   // rad/s
#define HMOTOR_MIN_SPEED    0.3 // rad/s
#define VMOTOR_MAX_SPEED    30  // mm/s

#define ARM_MOVE_TIMEOUT    6000    // ms


#endif
