
/**
 * Copyright (c) 2015 MikeBudimon (Michal Wagner) <mikebudimon@gmail.com>
 */

package de.raspirobotcontrol;

import com.pi4j.io.gpio.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {

        System.out.println("RaspiRobotControl - Server started!");

        // initialize gpios
        final GpioController gpio = GpioFactory.getInstance();
        Pin pinTrigger = RaspiPin.GPIO_07;
        Pin pinEcho = RaspiPin.GPIO_06;
        Pin pinLedWarning = RaspiPin.GPIO_21;
        final GpioPinDigitalOutput pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);
        Pin pinBuzzer = RaspiPin.GPIO_05;

        pinLed.setShutdownOptions(true, PinState.LOW);    // program status Led


        pinLed.high(); // program started Led
        System.out.println("Enter from 2,4,5,6,8 to control,\t" + "Enter 0 to buzz,\t" + "Enter q to quit!");

        // starts web socket server
        Server server = new Server(9000, gpio, pinBuzzer, 0, 1, 3, 4);
        Thread serverControl = new Thread(server);
        serverControl.start();


        // prints every seconds the calculated distance to the nearest object
        DistanceMonitor distanceMonitor = new DistanceMonitor(gpio, pinTrigger, pinEcho, pinLedWarning);
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
