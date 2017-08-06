#ifndef _MOVE_STATE_h
#define _MOVE_STATE_h


typedef uint8_t MoveStatus;
typedef uint8_t MovePhase;


enum MStatus
{
	MOVE_OK = 0,

	// Erreur de suivi de trajectoire
	EMERGENCY_BREAK = 1,
	EXT_BLOCKED = 2,
	INT_BLOCKED = 4,
	FAR_AWAY = 8,

	// Trajectoire erronée
	EMPTY_TRAJ = 16
};


enum MPhase
{
	MOVE_INIT,
	MOVING,
	BREAKING,
	MOVE_ENDED
};


#endif
