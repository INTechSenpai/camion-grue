#ifndef _SENSORS_MGR_h
#define _SENSORS_MGR_h

#include "ToF_longRange.h"


class SensorsMgr
{
public:
    SensorsMgr()
    {

    }

    int init()
    {
        // todo
        return 0;
    }

    void updateServos()
    {
        // todo
    }

    void getSensorsData(int32_t & tof_g, int32_t & tof_d, float & angleTG, float & angleTD)
    {
        tof_g = 0;
        tof_d = 0;
        angleTG = 0;
        angleTD = 0;
        // todo
    }

    void setAimAngles(float angleG, float angleD)
    {
        // todo
    }

private:

};


#endif
