#ifndef _PIN_MAPPING_h
#define _PIN_MAPPING_h

/* Master serial */
#define SERIAL  Serial1
#define BAUDRATE_SERIAL 115200

/* Sensors config */
#define NB_SENSORS      9


/* ToF sensors */
#define PIN_EN_TOF_AVG      2
#define PIN_EN_TOF_AV       7
#define PIN_EN_TOF_AVD      8
#define PIN_EN_TOF_FLAN_AVG 17
#define PIN_EN_TOF_FLAN_AVD 11
#define PIN_EN_TOF_FLAN_ARG 16
#define PIN_EN_TOF_FLAN_ARD 12
#define PIN_EN_TOF_ARG      15
#define PIN_EN_TOF_ARD      14


/* LEDs */
#define PIN_DEL_ON_BOARD    13
#define PIN_DEL_CLIGNO_G    10
#define PIN_DEL_CLIGNO_D    20
#define PIN_DEL_NUIT_AV     9
#define PIN_DEL_NUIT_AR     21
#define PIN_DEL_STOP        22
#define PIN_DEL_RECUL       23
#define PIN_DEL_GYRO_1      6
#define PIN_DEL_GYRO_2      5
#define PIN_DEL_GYRO_3      4
#define PIN_DEL_GYRO_4      3

#endif
