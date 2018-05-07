#ifndef XL320_H_
#define XL320_H_

/*EEPROM Area*/
#define XL_MODEL_NUMBER_L           0
#define XL_MODEL_NUMBER_H           1
#define XL_VERSION                  2
#define XL_ID                       3
#define XL_BAUD_RATE                4
#define XL_RETURN_DELAY_TIME        5
#define XL_CW_ANGLE_LIMIT_L         6
#define XL_CW_ANGLE_LIMIT_H         7
#define XL_CCW_ANGLE_LIMIT_L        8
#define XL_CCW_ANGLE_LIMIT_H        9
#define XL_CONTROL_MODE             11
#define XL_LIMIT_TEMPERATURE        12
#define XL_DOWN_LIMIT_VOLTAGE       13
#define XL_UP_LIMIT_VOLTAGE         14
#define XL_MAX_TORQUE_L             15
#define XL_MAX_TORQUE_H             16
#define XL_RETURN_LEVEL             17
#define XL_ALARM_SHUTDOWN           18
/*RAM Area*/
#define XL_TORQUE_ENABLE            24
#define XL_LED                      25
#define XL_D_GAIN    				27
#define XL_I_GAIN      				28
#define XL_P_GAIN    				29
#define XL_GOAL_POSITION_L          30
#define XL_GOAL_SPEED_L             32
#define XL_GOAL_TORQUE 		        35
#define XL_PRESENT_POSITION         37
#define XL_PRESENT_SPEED            39
#define XL_PRESENT_LOAD             41
#define XL_PRESENT_VOLTAGE          45
#define XL_PRESENT_TEMPERATURE      46
#define XL_REGISTERED_INSTRUCTION   47
#define XL_MOVING                   49
#define XL_HARDWARE_ERROR           50
#define XL_PUNCH                    51

#include <Printable.h>
#include <vector>
#include <Arduino.h>


class XL320
{
public:
	XL320(HardwareSerial & stream);

    void begin(uint32_t baudrate, uint32_t timeout);
	
	void setPosition(uint8_t id, uint16_t value);
	void setSpeed(uint8_t id, uint16_t value);
	void setLED(uint8_t id, char led_color);
	void setTorque(uint8_t id, uint16_t value);
	void torqueOn(uint8_t id);
	void torqueOff(uint8_t id);

	uint16_t getPresentPosition(uint8_t id);
    uint16_t getPresentSpeed(uint8_t id);
    uint16_t getPresentTorque(uint8_t id);

    size_t write(uint8_t id, uint16_t address, uint16_t value, uint8_t size);
    uint16_t read(uint8_t id, uint16_t address, uint16_t size);

private:
	class Packet : public Printable
    {
    public:
        Packet();
        Packet(uint8_t id, uint8_t instruction, const std::vector<uint8_t> & parameters);

        void reset();
	    uint8_t getId() const;
	    uint16_t getParameterCount() const;
	    uint8_t getInstruction() const;
        uint8_t getParameter(uint16_t n) const;
	    bool isValid() const;
        size_t printTo(Print& p) const;
        size_t writeOnStream(HardwareSerial & stream) const;

        void addByte(uint8_t b);
        bool isReading() const;
    private:
        uint16_t computeChecksum() const;
        bool valid;
        bool reading;
        std::vector<uint8_t> data;
	};
    
    size_t requestPacket(uint8_t id, uint16_t address, uint16_t size);
    Packet readPacket();
    HardwareSerial & stream;


    void setHalfDuplex(Stream & mStream)
    {
#if defined(__MK20DX128__) || defined(__MK20DX256__) || defined(__MK64FX512__) || defined(__MK66FX1M0__) // Teensy 3.0 3.1 3.2 3.5 3.6
        if (&mStream == &Serial1)
        {
            UART0_C1 |= UART_C1_LOOPS | UART_C1_RSRC; // Connect internally RX and TX for half duplex
            CORE_PIN1_CONFIG |= PORT_PCR_PE | PORT_PCR_PS; // pullup on output pin
        }
        else if (&mStream == &Serial2)
        {
            UART1_C1 |= UART_C1_LOOPS | UART_C1_RSRC; // Connect internally RX and TX for half duplex
            CORE_PIN10_CONFIG |= PORT_PCR_PE | PORT_PCR_PS; // pullup on output pin
        }
        else if (&mStream == &Serial3)
        {
            UART2_C1 |= UART_C1_LOOPS | UART_C1_RSRC; // Connect internally RX and TX for half duplex
            CORE_PIN8_CONFIG |= PORT_PCR_PE | PORT_PCR_PS; // pullup on output pin
        }
#if defined(__MK64FX512__) || defined(__MK66FX1M0__) // Teensy 3.5 or 3.6
        else if (&mStream == &Serial4)
        {
            UART3_C1 |= UART_C1_LOOPS | UART_C1_RSRC; // Connect internally RX and TX for half duplex
            CORE_PIN32_CONFIG |= PORT_PCR_PE | PORT_PCR_PS; // pullup on output pin
        }
        else if (&mStream == &Serial5)
        {
            UART4_C1 |= UART_C1_LOOPS | UART_C1_RSRC; // Connect internally RX and TX for half duplex
            CORE_PIN33_CONFIG |= PORT_PCR_PE | PORT_PCR_PS; // pullup on output pin
        }
        else if (&mStream == &Serial6)
        {
            UART5_C1 |= UART_C1_LOOPS | UART_C1_RSRC; // Connect internally RX and TX for half duplex
            CORE_PIN48_CONFIG |= PORT_PCR_PE | PORT_PCR_PS; // pullup on output pin
        }
#endif
#else
#error Dynamixel lib : unsupported hardware
#endif
    }


    void setReadMode(Stream & mStream)
    {
#if defined(__MK20DX128__) || defined(__MK20DX256__) || defined(__MK64FX512__) || defined(__MK66FX1M0__) // Teensy 3.0 3.1 3.2 3.5 3.6
        if (&mStream == &Serial1)
        {
            UART0_C3 &= ~UART_C3_TXDIR;
        }
        else if (&mStream == &Serial2)
        {
            UART1_C3 &= ~UART_C3_TXDIR;
        }
        else if (&mStream == &Serial3)
        {
            UART2_C3 &= ~UART_C3_TXDIR;
        }
#if defined(__MK64FX512__) || defined(__MK66FX1M0__) // Teensy 3.5 or 3.6
        else if (&mStream == &Serial4)
        {
            UART3_C3 &= ~UART_C3_TXDIR;
        }
        else if (&mStream == &Serial5)
        {
            UART4_C3 &= ~UART_C3_TXDIR;
        }
        else if (&mStream == &Serial6)
        {
            UART5_C3 &= ~UART_C3_TXDIR;
        }
#endif
#else
#error Dynamixel lib : unsupported hardware
#endif
    }


    void setWriteMode(Stream & mStream)
    {
#if defined(__MK20DX128__) || defined(__MK20DX256__) || defined(__MK64FX512__) || defined(__MK66FX1M0__) // Teensy 3.0 3.1 3.2 3.5 3.6
        if (&mStream == &Serial1)
        {
            UART0_C3 |= UART_C3_TXDIR;
        }
        else if (&mStream == &Serial2)
        {
            UART1_C3 |= UART_C3_TXDIR;
        }
        else if (&mStream == &Serial3)
        {
            UART2_C3 |= UART_C3_TXDIR;
        }
#if defined(__MK64FX512__) || defined(__MK66FX1M0__) // Teensy 3.5 ou 3.6
        else if (&mStream == &Serial4)
        {
            UART3_C3 |= UART_C3_TXDIR;
        }
        else if (&mStream == &Serial5)
        {
            UART4_C3 |= UART_C3_TXDIR;
        }
        else if (&mStream == &Serial6)
        {
            UART5_C3 |= UART_C3_TXDIR;
        }
#endif
#else
#error Dynamixel lib : unsupported hardware
#endif
    }

};

#endif
