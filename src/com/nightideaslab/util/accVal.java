package com.nightideaslab.util;

/**
 * Sensor data.
 * 
 * Store data from accelerometer sensor
 * 
 */
public class accVal
{
	public float accx;
	public float accy;
	public float accz;

	public float roll;
	public float pitch;

	public long ms;								/** ms time stamp */
	public boolean bbRad;						/** TRUE if the value are in Radiant */

	float alpha = (float) 0.8;
	float gravity[];

	/**
	 * default constructor.
	 */
	public accVal()
	{
		ms = 0;
		gravity = new float[3];
		gravity[0] = gravity[1] = gravity[2] = 0;
	}

	/**
	 * Update sensor values subtracting gravity force
	 */
	public void SubtractGravity()
	{
		// alpha is calculated as t / (t + dT)
		// with t, the low-pass filter's time-constant
		// and dT, the event delivery rate

		gravity[0] = (float) (alpha * gravity[0] + (1.0 - alpha) * accx);
		gravity[1] = (float) (alpha * gravity[1] + (1.0 - alpha) * accy);
		gravity[2] = (float) (alpha * gravity[2] + (1.0 - alpha) * accz);

		accx = accx - gravity[0];
		accy = accy - gravity[1];
		accz = accz - gravity[2];
	}
}