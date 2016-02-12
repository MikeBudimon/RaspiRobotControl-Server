
/**
 * Copyright (c) 2015 MikeBudimon (Michal Wagner) <mikebudimon@gmail.com>
 */

package de.raspirobotcontrol;

import com.pi4j.component.servo.ServoProvider;
import com.pi4j.component.servo.impl.RPIServoBlasterProvider;
import com.pi4j.io.gpio.*;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {

        System.out.println("RaspiRobotControl - Server started!");

        BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);

        // initialize gpios
        final GpioController gpio = GpioFactory.getInstance();
        Pin pinLedWarning = RaspiPin.GPIO_13;
        Pin pinBuzzer = RaspiPin.GPIO_05; // P1-18: audio-buzzer
        final GpioPinDigitalOutput pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);
        pinLed.setShutdownOptions(true, PinState.LOW);    // P1-13: progammstatus Led
        final ServoProvider servoProvider = new RPIServoBlasterProvider();

        pinLed.high(); // program started Led

        // starts web socket server
        Server server = new Server(9000, gpio, pinBuzzer, 0, 1, 3, 4, servoProvider, queue);
        Thread serverControl = new Thread(server);
        serverControl.start();


        // prints every seconds the calculated distance to the nearest object
        DistanceMonitor distanceMonitor = new DistanceMonitor(gpio, pinLedWarning, queue);
        Thread threadDistance = new Thread(distanceMonitor);
        threadDistance.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();

                System.out.println("Shutting down...");

                try {
                    server.stop();
                    serverControl.join(1200);
                    serverControl.interrupt();
                    System.out.println("Server down!");
                    distanceMonitor.stop();
                    threadDistance.join(1200);
                    threadDistance.interrupt();
                    System.out.println("DistanceMonitor down!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
