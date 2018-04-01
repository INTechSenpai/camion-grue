#ifndef _TOF_SHORT_RANGE_H
#define _TOF_SHORT_RANGE_H

#if defined(ARDUINO) && ARDUINO >= 100
	#include "Arduino.h"
#else
	#include "WProgram.h"
#endif

#include "VL6180X.h"

class ToF_shortRange
{
public:
	ToF_shortRange(uint8_t id, uint8_t pinStandby)
	{
        distance = 0;
		i2cAddress = id;
		this->pinStandby = pinStandby;
		standby();
		vlSensor.setTimeout(500);
	}

	uint32_t getMesure()
	{
		if (isON)
		{
			distance = vlSensor.readRangeContinuousMillimeters();
			if (vlSensor.timeoutOccurred() || vlSensor.last_status != 0)
			{
				distance = 0;
                standby();
			}
		}
		else
		{
            distance = 0;
		}
		return distance;
	}

	void standby()
	{
		pinMode(pinStandby, OUTPUT);
		digitalWrite(pinStandby, LOW);
		isON = false;
	}

	void powerON(const char* name = "")
	{
		Serial.print("PowerOn ToF ");
		Serial.print(name);
		Serial.print("...");
		pinMode(pinStandby, INPUT);
		delay(50);
        if (vlSensor.init())
        {
            vlSensor.configureDefault();
            vlSensor.setAddress(i2cAddress);

            vlSensor.writeReg(VL6180X::SYSRANGE__MAX_CONVERGENCE_TIME, 12);

            vlSensor.stopContinuous();
            delay(100);
            vlSensor.startRangeContinuous(20);
            isON = true;
            Serial.println("OK");
        }
        else
        {
            standby();
            Serial.println("NOT OK");
        }
	}

private:
	uint8_t i2cAddress, pinStandby;
	uint32_t distance;
	bool isON;
	VL6180X vlSensor;
};

#endif

