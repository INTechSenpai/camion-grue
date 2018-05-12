/*
 Name:		lowlevel_master.ino
 Created:	04/03/2018 20:10:42
 Author:	Sylvain Gaultier
*/

#include "Config/pin_mapping.h"
#include "CommunicationServer/OrderMgr.h"
#include "Locomotion/MotionControlSystem.h"
#include "Locomotion/ContextualLightning.h"
#include "SlaveCommunication/SlaveSensorLed.h"
#include "SlaveCommunication/SlaveActuator.h"
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
    SlaveActuator &slaveActuator = SlaveActuator::Instance();
    ContextualLightning contextualLightning;
    
    IntervalTimer motionControlTimer;
    motionControlTimer.priority(253);
    motionControlTimer.begin(motionControlInterrupt, PERIOD_ASSERV);

    uint32_t odometryReportTimer = 0;
    std::vector<uint8_t> odometryReport;
    std::vector<uint8_t> shortRangeSensorsValues;
    std::vector<uint8_t> longRangeSensorsValues;

    // Attente du démarrage de la grue
    digitalWrite(PIN_DEL_STATUS_2, HIGH);
    while (!slaveActuator.sensorDataAvailable())
    {
        slaveActuator.listen();
    }
    slaveActuator.getSensorsValues(longRangeSensorsValues);
    digitalWrite(PIN_DEL_STATUS_2, LOW);

    // Lancement de la carte capteurs
    slaveSensorLed.setLightningMode((uint8_t)SlaveSensorLed::ALL_OFF);

    // Attente du démarrage de la carte capteurs
    while (!slaveSensorLed.available())
    {
        slaveSensorLed.listen();
    }
    slaveSensorLed.getSensorsValues(shortRangeSensorsValues);

    // Affichage du succès du démarrage
    slaveSensorLed.setLightningMode((uint8_t)SlaveSensorLed::NIGHT_LIGHT_HIGH);

    uint32_t delTimer = 0;
    bool delState = true;

    while (true)
    {
        orderManager.execute();
        directionController.control();

        slaveActuator.listen();
        if (slaveActuator.sensorDataAvailable())
        {
            slaveActuator.getSensorsValues(longRangeSensorsValues);
        }

        slaveSensorLed.listen();
        if (slaveSensorLed.available())
        {
            slaveSensorLed.getSensorsValues(shortRangeSensorsValues);
        }

        contextualLightning.update();

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
            Serializer::writeBool(motionControlSystem.isMovingForward(), odometryReport);
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
