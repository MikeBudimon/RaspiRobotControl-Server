package de.raspirobotcontrol;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Measuring the distance to an object with a ultrasonic sensor
 */
public class DistanceMonitor implements Runnable {

    private final GpioPinDigitalOutput pinLedWarning; // distance Led status(red if too near)
    private boolean flag = false;


    // Constructor
    protected DistanceMonitor(GpioController gpio, Pin pinLedWarning) throws InterruptedException {
        this.pinLedWarning = gpio.provisionDigitalOutputPin(pinLedWarning, PinState.LOW);
        this.pinLedWarning.setShutdownOptions(true, PinState.LOW);
    }


    /**
     * Measures distance to an object from a python script(usonic.py)
     *
     * @return Distance to an object in cm
     * @throws InterruptedException
     */
    public String measureDistance() throws InterruptedException {
        String s = null;

        try {
            Process process = Runtime.getRuntime().exec("python usonic.py");
            BufferedReader brI = new BufferedReader(new InputStreamReader(process.getInputStream()));
            s = brI.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return s;
    }

    @Override
    public void run() {

        flag = true;

        while (flag) {
            try {
                // Sends the measured distance to the client when successfully connected.
                if (Server.started) {
                    String distance = measureDistance();
                    Main.queue.put(distance);
                    //System.out.println("Object is " + measureDistance() + " cm away.");

                    // blink red led when distance is smaller than 10 cm
                    if (Double.valueOf(distance) < 10.0) {
                        pinLedWarning.blink(100, 1000);
                    } else {
                        pinLedWarning.low();
                    }
                    Thread.sleep(700);
                }
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
