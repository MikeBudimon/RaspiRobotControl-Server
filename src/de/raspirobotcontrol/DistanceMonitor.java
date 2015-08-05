package de.raspirobotcontrol;

import com.pi4j.io.gpio.*;

/**
 * Measuring the distance to an object with a ultrasonic sensor
 */
public class DistanceMonitor implements Runnable {


    private final GpioPinDigitalInput pinEcho;  // receives impulses
    private final GpioPinDigitalOutput pinTrigger; // sends impulses
    private final GpioPinDigitalOutput pinLedWarning; // distance Led status(red if too near)
    private boolean flag = false;


    // Constructor
    protected DistanceMonitor(GpioController gpio, Pin pinTrigger, Pin pinEcho, Pin pinLedWarning) {
        this.pinEcho = gpio.provisionDigitalInputPin(pinEcho);
        this.pinTrigger = gpio.provisionDigitalOutputPin(pinTrigger, PinState.LOW);
        this.pinLedWarning = gpio.provisionDigitalOutputPin(pinLedWarning, PinState.LOW);

        this.pinTrigger.setShutdownOptions(true, PinState.LOW);
        this.pinLedWarning.setShutdownOptions(true, PinState.LOW);
    }


    /**
     * Measures distance to an object
     *
     * @return Distance to an object in cm
     * @throws InterruptedException
     */
    public double measureDistance() throws InterruptedException {

        this.triggerSensor();
        long starttime = System.nanoTime();
        long endtime = System.nanoTime();

        while (pinEcho.isLow()) {
            starttime = System.nanoTime();
        }

        while (pinEcho.isHigh()) {
            endtime = System.nanoTime();
        }

        // time-difference in seconds
        double timeDifference = (endtime - starttime);

        // time-difference multiply with sonic speed 34300 cm/s and divide with 2 because forward and back
        return (timeDifference / 1000000000 * 34300) / 2;

    }

    /**
     * Sends one impulse for 10 microseconds
     *
     * @throws InterruptedException
     */
    private void triggerSensor() throws InterruptedException {

        pinTrigger.high();
        Thread.sleep(0, 10000); // trigger duration of 10 micro s
        pinTrigger.low();
    }


    @Override
    public void run() {

        flag = true;

        while (flag) {
            try {
                System.out.printf("Object is %.1f cm away.\n", measureDistance());

                // Sends the measured distance to the client when successfully connected.
                if (Server.started) {
                    Main.queue.put(String.valueOf(measureDistance()));
                }

                // blink red led when distance is smaller than 25 cm
                if (measureDistance() < 25.0) {
                    pinLedWarning.blink(200, 1000);
                } else {
                    pinLedWarning.low();
                }

                Thread.sleep(1100);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the while loop.
     */
    public void stop() {
        flag = false;
    }
}
