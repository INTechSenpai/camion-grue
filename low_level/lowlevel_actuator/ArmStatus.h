#ifndef _ARM_STATUS_h
#define _ARM_STATUS_h

typedef int32_t ArmStatus;

enum AStatus
{
    ARM_STATUS_OK           = 0,
    ARM_STATUS_HBLOCKED     = 1,
    ARM_STATUS_VBLOCKED     = 2,
    ARM_STATUS_AXBLOCKED    = 4,
    ARM_STATUS_AXERR        = 8,
    ARM_STATUS_MANUAL_STOP  = 16,
    ARM_STATUS_UNREACHABLE  = 32,
    ARM_STATUS_SENSOR_ERR   = 64,
    ARM_STATUS_NO_DETECTION = 128,
    ARM_STATUS_COM_ERR      = 256,
    ARM_STATUS_CUBE_MISSED  = 512,
    ARM_STATUS_TIMEOUT      = 1024
};


#endif
