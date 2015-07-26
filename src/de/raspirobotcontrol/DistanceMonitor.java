package de.raspirobotcontrol;

import com.pi4j.io.gpio.*;

/**
 * Created by MikeBudimon on 23.07.2015.
 */
public class DistanceMonitor implements Runnable {


    private final GpioPinDigitalInput pinEcho;  // receives impulses
    private final GpioPinDigitalOutput pinTrigger; // sends impulses
    private final GpioPinDigitalOutput pinLedWarning; // distance Led status(red if too near)


    protected DistanceMonitor(GpioController gpio, Pin pinTrigger, Pin pinEcho, Pin pinLedWarning) {
        this.pinEcho = gpio.provisionDigitalInputPin(pinEcho);
        this.pinTrigger = gpio.provisionDigitalOutputPin(pinTrigger);
        this.pinLedWarning = gpio.provisionDigitalOutputPin(pinLedWarning);

        this.pinTrigger.low();
        this.pinLedWarning.low();

        this.pinTrigger.setShutdownOptions(true, PinState.LOW);
        this.pinLedWarning.setShutdownOptions(true, PinState.LOW);

    }


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

    private void triggerSensor() throws InterruptedException {

        pinTrigger.high();
        Thread.sleep(0, 10000); // trigger duration of 10 micro s
        pinTrigger.low();
    }


    @Override
    public void run() {

        while (true){
            try {
                System.out.printf("Object is %.1f cm away.\n", measureDistance());

                // blink red led when distance is smaller than 25 cm
                if (measureDistance() < 25.0){
                    pinLedWarning.blink(200, 1000);
                } else {
                    pinLedWarning.low();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
