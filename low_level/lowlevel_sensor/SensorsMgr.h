#ifndef _SENSORS_MGR_h
#define _SENSORS_MGR_h

#include <Printable.h>
#include "ToF_shortRange.h"
#include "config.h"

#define STD_MIN_RANGE   18
#define STD_MAX_RANGE   200
#define MAX_POWERON_ATTEMPTS    10


class SensorsMgr : public Printable
{
public:
    SensorsMgr()
    {
        ToF_shortRange tofAVG("AVG", 42, PIN_EN_TOF_AVG, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofAV("AV", 43, PIN_EN_TOF_AV, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofAVD("AVD", 44, PIN_EN_TOF_AVD, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofFlanAVG("FlanAVG", 45, PIN_EN_TOF_FLAN_AVG, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofFlanAVD("FlanAVD", 46, PIN_EN_TOF_FLAN_AVD, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofFlanARG("FlanARG", 47, PIN_EN_TOF_FLAN_ARG, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofFlanARD("FlanARD", 48, PIN_EN_TOF_FLAN_ARD, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofARG("ARG", 49, PIN_EN_TOF_ARG, STD_MIN_RANGE, STD_MAX_RANGE);
        ToF_shortRange tofARD("ARD", 50, PIN_EN_TOF_ARD, STD_MIN_RANGE, STD_MAX_RANGE);

        sensors[0] = tofAV;
        sensors[1] = tofAVG;
        sensors[2] = tofAVD;
        sensors[3] = tofFlanAVG;
        sensors[4] = tofFlanAVD;
        sensors[5] = tofFlanARG;
        sensors[6] = tofFlanARD;
        sensors[7] = tofARG;
        sensors[8] = tofARD;

        for (size_t i = 0; i < NB_SENSORS; i++)
        {
            sensorsValues[i] = (SensorValue)SENSOR_DEAD;
        }
    }

    int init()
    {
        int ret = 0;
        for (size_t i = 0; i < NB_SENSORS; i++)
        {
            int nbAttempts = 0;
            while (sensors[i].powerON() != 0)
            {
                nbAttempts++;
                if (nbAttempts > MAX_POWERON_ATTEMPTS)
                {
                    ret = -1;
                    break;
                }
            }
        }
        return ret;
    }

    void update()
    {
        for (size_t i = 0; i < NB_SENSORS; i++)
        {
            sensorsValues[i] = sensors[i].getMesure();
        }
    }

    void getValues(SensorValue values[NB_SENSORS])
    {
        for (size_t i = 0; i < NB_SENSORS; i++)
        {
            values[i] = sensorsValues[i];
            sensorsValues[i] = (SensorValue)SENSOR_NOT_UPDATED;
        }
    }

    size_t printTo(Print& p) const
    {
        size_t ret = 0;
        for (size_t i = 0; i < NB_SENSORS; i++)
        {
            ret += p.print(sensors[i].name);
            ret += p.print("=");
            SensorValue val = sensorsValues[i];
            if (val == (SensorValue)SENSOR_DEAD || val == (SensorValue)SENSOR_NOT_UPDATED)
            {
                ret += p.print("HS ");
            }
            else if (val == (SensorValue)NO_OBSTACLE)
            {
                ret += p.print("inf ");
            }
            else if (val == (SensorValue)OBSTACLE_TOO_CLOSE)
            {
                ret += p.print("0 ");
            }
            else
            {
                ret += p.printf("%u ", val);
            }
        }
        return ret;
    }

private:
    ToF_shortRange sensors[NB_SENSORS];
    SensorValue sensorsValues[NB_SENSORS];
};


#endif
