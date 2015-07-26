
/**
 * Copyright (c) 2015 MikeBudimon (Michal Wagner) <mikebudimon@gmail.com>
 */

package de.raspirobotcontrol;


import com.pi4j.io.gpio.*;
import com.pi4j.wiringpi.SoftPwm;

import java.io.IOException;
import java.util.Scanner;

public class RaspiRobotControl {

    public static void main(String[] args) throws InterruptedException, IOException {

        System.out.println("RaspiRobotControl started.");

        // initialize gpios
        final GpioController gpio = GpioFactory.getInstance();
        Pin pinTrigger = RaspiPin.GPIO_07;
        Pin pinEcho = RaspiPin.GPIO_06;
        Pin pinLedWarning = RaspiPin.GPIO_21;
        final GpioPinDigitalOutput pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);
        final GpioPinDigitalOutput pinBuzzer = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, PinState.LOW);

        pinLed.setShutdownOptions(true, PinState.LOW);    // programstatus Led
        pinBuzzer.setShutdownOptions(true, PinState.LOW); // audio-buzzer


        // initialize wiringPi library
        com.pi4j.wiringpi.Gpio.wiringPiSetup();

        // create soft-pwm pins (min=0 ; max=100)
        SoftPwm.softPwmCreate(0, 0, 100); // P1-11: motor-left forward
        SoftPwm.softPwmCreate(1, 0, 100); // P1-12: motor-left backward
        SoftPwm.softPwmCreate(3, 0, 100); // P1-15: motor-right backward
        SoftPwm.softPwmCreate(4, 0, 100); // P1-16: motor-right forward


        Scanner scouter = new Scanner(System.in);
        Integer speed = 100; // Set raw value for this servo driver: 0 - 100 %

        pinLed.high(); // program started LED
        System.out.println("Enter from 2,4,5,6,8 to control,\t" + "Enter 0 to buzz,\t" + "Enter q to quit!");

        // prints every seconds the calculated distance to the nearest object
        Thread threadDistance = new Thread(new DistanceMonitor(gpio, pinTrigger, pinEcho, pinLedWarning));
        threadDistance.start();

        while (true) {

            String input = scouter.next();

            if (input.equals("8")) { // forward
                SoftPwm.softPwmWrite(0, speed);
                SoftPwm.softPwmWrite(1, 0);
                SoftPwm.softPwmWrite(3, 0);
                SoftPwm.softPwmWrite(4, speed);
                Thread.sleep(100);

            } else if (input.equals("2")) { // backward
                SoftPwm.softPwmWrite(0, 0);
                SoftPwm.softPwmWrite(1, speed);
                SoftPwm.softPwmWrite(3, speed);
                SoftPwm.softPwmWrite(4, 0);
                Thread.sleep(100);

            } else if (input.equals("5")) { // stop
                SoftPwm.softPwmWrite(0, 0);
                SoftPwm.softPwmWrite(1, 0);
                SoftPwm.softPwmWrite(3, 0);
                SoftPwm.softPwmWrite(4, 0);
                Thread.sleep(100);

            } else if (input.equals("4")) { // left
                SoftPwm.softPwmWrite(0, 0);
                SoftPwm.softPwmWrite(1, 0);
                SoftPwm.softPwmWrite(3, 0);
                SoftPwm.softPwmWrite(4, speed);
                Thread.sleep(100);

            } else if (input.equals("6")) { // right
                SoftPwm.softPwmWrite(0, speed);
                SoftPwm.softPwmWrite(1, 0);
                SoftPwm.softPwmWrite(3, 0);
                SoftPwm.softPwmWrite(4, 0);
                Thread.sleep(100);

            } else if (input.equals("0")) { // audio-buzzer ON
                pinBuzzer.pulse(2000, PinState.HIGH);

            } else if (input.equals("+")) { // increase speed to 100
                speed = 100;
                System.out.println("Speed increased to 100%!");

            } else if (input.equals("-")) { // decrease speed to 75
                speed = 75;
                System.out.println("Speed decreased to 75%!");

            } else if (input.equals("q")) { // quit
                SoftPwm.softPwmWrite(0, 0);
                SoftPwm.softPwmWrite(1, 0);
                SoftPwm.softPwmWrite(3, 0);
                SoftPwm.softPwmWrite(4, 0);
                break;

            } else {
                System.out.println("Invalid enter, You suck!");
            }

        }

        // shutdown all gpios
        SoftPwm.softPwmWrite(0, 0);
        SoftPwm.softPwmWrite(1, 0);
        SoftPwm.softPwmWrite(3, 0);
        SoftPwm.softPwmWrite(4, 0);

        gpio.shutdown();

        System.exit(0);
    }

}
