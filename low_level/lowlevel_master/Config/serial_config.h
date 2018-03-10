#ifndef _SERIAL_CONFIG_h
#define _SERIAL_CONFIG_h


/* Liaisons série */
#define SERIAL_ACTUATOR		Serial1		// Pins 0 1
#define SERIAL_SENSORS		Serial5		// Pins 33 34
#define SERIAL_AX12			Serial3		// Pins 7 8
#define SERIAL_XL320		Serial4	    // Pins 31 32

/* Débits */
#define SERIAL_ACTUATOR_BAUDRATE    115200
#define SERIAL_SENSORS_BAUDRATE     115200
#define SERIAL_AX12_BAUDRATE        1000000
#define SERIAL_XL320_BAUDRATE       1000000

/* Timeout */
#define SERIAL_AX12_TIMEOUT         50  // ms


#endif
