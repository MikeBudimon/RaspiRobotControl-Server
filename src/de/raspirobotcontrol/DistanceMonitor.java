package de.raspirobotcontrol;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

/**
 * Measuring the distance to an object with a ultrasonic sensor
 */
public class DistanceMonitor implements Runnable {

    private final GpioPinDigitalOutput pinLedWarning; // distance Led status(red if too near)
    private final BlockingQueue<String> queue;
    private boolean flag = false;


    // Constructor
    protected DistanceMonitor(GpioController gpio, Pin pinLedWarning, BlockingQueue<String> queue) throws InterruptedException {
        this.queue = queue;
        this.pinLedWarning = gpio.provisionDigitalOutputPin(pinLedWarning, PinState.LOW);
        this.pinLedWarning.setShutdownOptions(true, PinState.LOW);
    }


    /**
     * Measures distance to an object from a python script(usonic.py)
     *
     * @return Distance to an object in cm
     * @throws InterruptedException
     */
    private String measureDistance() throws InterruptedException {
        String s = "0";

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

        String distance;
        flag = true;

        while (flag) {
            try {
                // Sends the measured distance to the client when successfully connected.
                if (Server.started) {
                    distance = measureDistance();
                    if (distance != null) {
                        queue.put(distance);

                        // blink red led when distance is smaller than 10 cm
                        if (Double.parseDouble(distance) < 10.0) {
                            pinLedWarning.blink(100, 1000);
                        } else {
                            pinLedWarning.low();
                        }
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
