#ifndef _ARM_POSITION_h
#define _ARM_POSITION_h

#include <Printable.h>


/* Constantes g�om�triques de conversion [mm] */
#define ARM_DIM_A   (76.5)
#define ARM_DIM_B   (103)
#define ARM_K1  (ARM_DIM_A*ARM_DIM_A + ARM_DIM_B*ARM_DIM_B)
#define ARM_K2  (2*ARM_DIM_A*ARM_DIM_B)
#define ARM_K3  (ARM_K1 / ARM_K2)
#define ARM_PLIER_AMPLITUDE (25)

/* Angles limite [rad] */
#define ARM_MAX_H_ANGLE     HALF_PI
#define ARM_MIN_V_ANGLE     -0.26   // TBD
#define ARM_MAX_V_ANGLE     0.35    // TBD
#define ARM_POS_VMOT_ORIGIN 128.3   // [mm]
#define ARM_MIN_HEAD_ANGLE  0.436   // min(headLocalAngle)
#define ARM_MAX_HEAD_ANGLE  4.974   // max(headLocalAngle)
#define ARM_MIN_PLIER_POS   0       // [mm]
#define ARM_MAX_PLIER_POS   49      // [mm]

#define ARM_H_ANGLE_CABIN           0.8     // [rad] Angle horizontal en dessous duquel on peut se manger la cabine
#define ARM_HEAD_SCAN_ANGLE         -1.3    // [rad] Angle global permettant de voir le cube � prendre
#define ARM_HEAD_L_ANGLE_TRANSPORT  0.698   // [rad] Angle local de la t�te de bras pour transporter un cube
#define ARM_HEAD_G_ANGLE_STORAGE    -0.785  // [rad] Angle de premettant la prise et la d�pose dans la zone de stockage interne
#define ARM_V_ANGLE_STORAGE         0.157   // [rad] (TBD) Angle vertical permettant la prise et la d�pose dans la zone de stockage interne
#define ARM_V_ANGLE_STAGE_0         -0.157  // [rad] (TBD) Angle vertical de prise/d�pose de cube sur le sol
#define ARM_V_ANGLE_STAGE_1         -0.157  // [rad] (TBD) Angle vertical de prise/d�pose de cube sur l'�tage 1
#define ARM_V_ANGLE_STAGE_2         -0.157  // [rad] (TBD) Angle vertical de prise/d�pose de cube sur l'�tage 2
#define ARM_V_ANGLE_STAGE_3         -0.157  // [rad] (TBD) Angle vertical de prise/d�pose de cube sur l'�tage 3
#define ARM_V_ANGLE_STAGE_4         -0.157  // [rad] (TBD) Angle vertical de prise/d�pose de cube sur l'�tage 4

/* Tol�rances */
#define ARM_H_TOLERANCE     0.035   // [rad]
#define ARM_V_TOLERANCE     1       // [mm]
#define ARM_AX12_TOLERANCE  0.10    // [rad]


class ArmPosition : public Printable
{
public:
    ArmPosition()
    {
        hAngle = 0;
        vAngle = 0;
        posMotV = 0;
        headGlobalAngle = 0;
        headLocalAngle = 0;
        plierAngle = 0;
        plierPos = 0;
    }

    void resetCCToOrigin()
    {
        setHAngle(0);
        setVAngle(0);
    }

    void resetAXToOrigin()
    {
        setHeadLocalAngle(ARM_MAX_HEAD_ANGLE);
        setPlierPos(ARM_MIN_PLIER_POS);
    }

    float getHAngle() const { return hAngle; }
    float getVAngle() const { return vAngle; }
    float getPosMotV() const { return posMotV; }
    float getHeadGlobalAngle() const { return headGlobalAngle; }
    float getHeadLocalAngle() const { return headLocalAngle; }
    float getHeadLocalAngleDeg() const { return headLocalAngle * 180 / M_PI; }
    float getPlierAngle() const { return plierAngle; }
    float getPlierAngleDeg() const { return plierAngle * 180 / M_PI; }
    float getPlierPos() const { return plierPos; }

    void setHAngle(float angle)
    {
        hAngle = constrain(angle, -ARM_MAX_H_ANGLE, ARM_MAX_H_ANGLE);
    }

    void setVAngle(float angle)
    {
        angle = constrain(angle, ARM_MIN_V_ANGLE, ARM_MAX_V_ANGLE);
        vAngle = angle;
        posMotV = sqrtf(ARM_K1 - ARM_K2*cosf(angle + HALF_PI)) - ARM_POS_VMOT_ORIGIN;
        updateHeadGlobalAngle();
    }

    void setPosMotV(float pos)
    {
        posMotV = pos;
        pos += ARM_POS_VMOT_ORIGIN;
        vAngle = acosf(constrain(ARM_K3 - (pos * pos / ARM_K2), -1, 1)) - HALF_PI;
        updateHeadGlobalAngle();
    }

    void setHeadGlobalAngle(float angle)
    {
        headGlobalAngle = angle;
        updateHeadLocalAngle();
    }

    void setHeadLocalAngle(float angle)
    {
        headLocalAngle = constrain(angle, ARM_MIN_HEAD_ANGLE, ARM_MAX_HEAD_ANGLE);
        updateHeadGlobalAngle();
    }

    void setHeadLocalAngleDeg(float angleDeg)
    {
        setHeadLocalAngle(angleDeg * M_PI / 180);
    }

    void setPlierAngle(float angle)
    {
        plierAngle = angle;
        plierPos = ARM_PLIER_AMPLITUDE * (1 - cosf(plierAngle - M_PI / 3)) - 0.38;
    }

    void setPlierAngleDeg(float angleDeg)
    {
        setPlierAngle(angleDeg * M_PI / 180);
    }

    void setPlierPos(float pos)
    {
        plierPos = constrain(pos, ARM_MIN_PLIER_POS, ARM_MAX_PLIER_POS);
        plierAngle = M_PI / 3 + acosf(constrain(1 - (plierPos + 0.38) / ARM_PLIER_AMPLITUDE, -1, 1));
    }

    bool closeEnoughTo(ArmPosition position)
    {
        return 
            (abs(hAngle - position.getHAngle()) < ARM_H_TOLERANCE &&
             abs(posMotV - position.getPosMotV()) < ARM_V_TOLERANCE) && 
            (abs(headLocalAngle - position.getHeadLocalAngle()) < ARM_AX12_TOLERANCE &&
             abs(plierAngle - position.getPlierAngle()) < ARM_AX12_TOLERANCE);
    }

    size_t printTo(Print& p) const
    {
        return p.printf("%g\t%g\t%g\t%g\t%g\t%g\t%g",
            hAngle,
            vAngle,
            posMotV,
            headGlobalAngle,
            headLocalAngle,
            plierAngle,
            plierPos);
    }

private:
    void updateHeadGlobalAngle()
    {
        headGlobalAngle = ARM_MAX_HEAD_ANGLE - headLocalAngle - HALF_PI + vAngle;
    }

    void updateHeadLocalAngle()
    {
        headLocalAngle = constrain(
            ARM_MAX_HEAD_ANGLE - headGlobalAngle - HALF_PI + vAngle,
            ARM_MIN_HEAD_ANGLE, 
            ARM_MAX_HEAD_ANGLE);
    }

    /* Angles exprim�s en radians */
    float hAngle;           // Angle de la tourelle par rapport � l'axe du robot (>0 <=> vers le flan gauche)
    float vAngle;           // Angle du bras de la tourelle par rapport au plan horizontal (>0 <=> vers le haut)
    float posMotV;          // [mm] Position de l'�crou de la vis sans fin selon l'axe de la vis (posMotV=0 <=> vAngle=0)
    float headGlobalAngle;  // Angle de l'AX12 de la t�te du bras dans le r�f�rentiel du robot (=0 <=> pince pointant vers le sol ; >0 <=> d�ploiement)
    float headLocalAngle;   // Angle de l'AX12 de la t�te du bras, dans le r�f�rentiel de l'AX12 (0 <-> 300� ou 0 <-> 5.24 rad) 
    float plierAngle;       // Angle de l'AX12 de la pince, dans le r�f�rentiel de l'AX12
    float plierPos;         // [mm] Position de la pince (=0 <=> serrage maximum : >0 <=> ouverture)
};


#endif
