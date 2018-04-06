/*
 Name:		lowlevel_master.ino
 Created:	04/03/2018 20:10:42
 Author:	Sylvain Gaultier
*/

#include "Config/pin_mapping.h"
#include "CommunicationServer/OrderMgr.h"
#include "Locomotion/MotionControlSystem.h"
#include "SlaveCommunication/SlaveSensorLed.h"
#include "CommunicationServer/Serializer.h"

#define ODOMETRY_REPORT_PERIOD  20  // ms


void setup()
{
    pinMode(PIN_DEL_STATUS_1, OUTPUT);
    pinMode(PIN_DEL_STATUS_2, OUTPUT);
    digitalWrite(PIN_DEL_STATUS_1, HIGH);

    if (Server.begin() != 0)
    {
        digitalWrite(PIN_DEL_STATUS_2, HIGH);
        delay(500);
    }
}


void loop()
{
    OrderMgr orderManager;
    MotionControlSystem &motionControlSystem = MotionControlSystem::Instance();
    DirectionController &directionController = DirectionController::Instance();
    SlaveSensorLed &slaveSensorLed = SlaveSensorLed::Instance();
    slaveSensorLed.setLightningMode(0); // Trigger init of slave Teensy

    IntervalTimer motionControlTimer;
    motionControlTimer.priority(253);
    motionControlTimer.begin(motionControlInterrupt, PERIOD_ASSERV);

    uint32_t odometryReportTimer = 0;
    std::vector<uint8_t> odometryReport;
    std::vector<uint8_t> shortRangeSensorsValues;
    std::vector<uint8_t> longRangeSensorsValues;
    while (!slaveSensorLed.available())
    {
        slaveSensorLed.listen();
    }
    slaveSensorLed.getSensorsValues(shortRangeSensorsValues);
    for (size_t i = 0; i < 5; i++)
    {
        Serializer::writeInt(0, longRangeSensorsValues); // provisoire, à termes il faudra attendre de recevoir la première trame pour initialiser avec
    }

    slaveSensorLed.setLightningMode(32);

    uint32_t delTimer = 0;
    bool delState = true;

    while (true)
    {
        orderManager.execute();
        directionController.control();

        slaveSensorLed.listen();
        if (slaveSensorLed.available())
        {
            slaveSensorLed.getSensorsValues(shortRangeSensorsValues);
        }

        if (millis() - odometryReportTimer > ODOMETRY_REPORT_PERIOD)
        {
            odometryReportTimer = millis();

            odometryReport.clear();
            Position p = motionControlSystem.getPosition();
            Serializer::writeInt(p.x, odometryReport);
            Serializer::writeInt(p.y, odometryReport);
            Serializer::writeFloat(p.orientation, odometryReport);
            Serializer::writeFloat(motionControlSystem.getCurvature(), odometryReport);
            Serializer::writeUInt(motionControlSystem.getTrajectoryIndex(), odometryReport);
            odometryReport.insert(odometryReport.end(), shortRangeSensorsValues.begin(), shortRangeSensorsValues.end());
            odometryReport.insert(odometryReport.end(), longRangeSensorsValues.begin(), longRangeSensorsValues.end());
            Server.sendData(ODOMETRY_AND_SENSORS, odometryReport);

            motionControlSystem.sendLogs();
        }

        if (millis() - delTimer > 500)
        {
            delState = !delState;
            digitalWrite(PIN_DEL_STATUS_1, delState);
            delTimer = millis();
        }
    }
}


void motionControlInterrupt()
{
    static MotionControlSystem &motionControlSystem = MotionControlSystem::Instance();
    motionControlSystem.control();
}


/* Ce bout de code permet de compiler avec std::vector */
namespace std {
    void __throw_bad_alloc()
    {
        while (true)
        {
            Server.printf_err("Unable to allocate memory\n");
            delay(500);
        }
    }

    void __throw_length_error(char const*e)
    {
        while (true)
        {
            Server.printf_err(e);
            Server.println_err();
            delay(500);
        }
    }

    void __throw_out_of_range(char const*e)
    {
        while (true)
        {
            Server.printf_err(e);
            Server.println_err();
            delay(500);
        }
    }

    void __throw_out_of_range_fmt(char const*e, ...)
    {
        while (true)
        {
            Server.printf_err(e);
            Server.println_err();
            delay(500);
        }
    }
}
