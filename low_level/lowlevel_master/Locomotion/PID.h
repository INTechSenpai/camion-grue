#ifndef _PID_h
#define _PID_h

#include <Printable.h>

class PID : public Printable
{
public:

	PID(volatile float const & input, volatile float & output, volatile float const & setPoint, const float freqAsserv) :
		input(input),
		output(output),
		setPoint(setPoint),
        callFreq(freqAsserv)
	{
		setOutputLimits(INT32_MIN, INT32_MAX);
		setTunings(0, 0, 0);
		pre_error = 0;
		derivative = 0;
		integral = 0;
		resetDerivativeError();
	}

	inline void compute()
	{
		float error = setPoint - input;
		derivative = (error - pre_error) * callFreq;
		integral += error / callFreq;
		pre_error = error;

		float result = kp * error + ki * integral + kd * derivative;
		if (result > outMax)
		{
			result = outMax;
		}
		else if (result < outMin)
		{
			result = outMin;
		}
		output = result;
	}

	void setTunings(float kp, float ki, float kd) 
	{
		if (kp < 0 || ki < 0 || kd < 0)
		{
			this->kp = 0;
			this->ki = 0;
			this->kd = 0;
		}
		this->kp = kp;
		this->ki = ki;
		this->kd = kd;
	}

	void setOutputLimits(float min, float max) {
		if (min >= max)
		{
			return;
		}

		outMin = min;
		outMax = max;

		if (output > outMax)
		{
			output = outMax;
		}
		else if (output < outMin)
		{
			output = outMin;
		}
	}

	void resetDerivativeError() 
	{
		pre_error = setPoint - input; // pre_error = error
		derivative = 0;
	}
	void resetIntegralError() 
	{
		integral = 0;
	}
	float getKp() const 
	{
		return kp;
	}
	float getKi() const 
	{
		return ki;
	}
	float getKd() const 
	{
		return kd;
	}
	float getError() const 
	{
		return pre_error;
	}
	float getDerivativeError() const
	{
		return derivative;
	}
	float getIntegralErrol() const 
	{
		return integral;
	}

	size_t printTo(Print& p) const
	{
		return p.printf("%u_%g_%g_%g", millis(), input, output, setPoint);
	}

private:
	float kp;
	float ki;
	float kd;

	volatile float const & input;
	volatile float & output;
	volatile float const & setPoint;

	float outMin, outMax;

	float pre_error;
	float derivative;
	float integral;

    const float callFreq;   // s^-1
};


#endif