
/**
 * Copyright (c) 2015 MikeBudimon (Michal Wagner) <mikebudimon@gmail.com>
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package de.raspirobotcontrol;


import com.pi4j.component.servo.ServoDriver;
import com.pi4j.component.servo.ServoProvider;
import com.pi4j.component.servo.impl.RPIServoBlasterProvider;
import com.pi4j.io.gpio.*;

import java.io.IOException;
import java.util.Scanner;

public class RaspiRobotControl {

    public static void main(String[] args) throws InterruptedException, IOException {

        /**
         * Servo cycle time:                20000us
         * Pulse increment step size:       10us
         * Minimum width value:             50 (500us) 2,5%
         * Maximum width value:             250 (2500us) 12,5%
         */

        System.out.println("RubyRobot Control started.");

        final GpioController gpio = GpioFactory.getInstance();

        final GpioPinDigitalOutput pinLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW);
        final GpioPinDigitalOutput pinBuzzer = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, PinState.LOW);

        pinLed.setShutdownOptions(true, PinState.LOW);    // P1-13: progammstatus Led
        pinBuzzer.setShutdownOptions(true, PinState.LOW); // P1-18: audio-buzzer


        final ServoProvider servoProvider = new RPIServoBlasterProvider();

        final ServoDriver motorL_forw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(1)); // P1-11: left forward
        final ServoDriver motorL_bckw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(2)); // P1-12: left backward
        final ServoDriver motorR_bckw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(4)); // P1-15: right backward
        final ServoDriver motorR_forw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(5)); // P1-16: right forward


        Scanner scouter = new Scanner(System.in);

        Integer speed = 1500; // 75%, Set raw value for this servo driver: 0 - 2000

        System.out.println("Enter from 1-8 to control,\t" + "Enter 0 to buzz,\t" + "Enter q to quit!");
        pinLed.high();

        while (true) {

            String input = scouter.next();

            if (input.equals("8")) { // forward
                motorL_forw.setServoPulseWidth(speed);
                motorL_bckw.setServoPulseWidth(0);
                motorR_bckw.setServoPulseWidth(0);
                motorR_forw.setServoPulseWidth(speed);

            } else if (input.equals("2")) { // backward
                motorL_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(speed);
                motorR_bckw.setServoPulseWidth(speed);
                motorR_forw.setServoPulseWidth(0);

            } else if (input.equals("5")) { // stop
                motorL_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(0);
                motorR_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(0);

            } else if (input.equals("4")) { // left
                motorL_forw.setServoPulseWidth(speed);
                motorL_bckw.setServoPulseWidth(0);
                motorR_bckw.setServoPulseWidth(speed);
                motorR_forw.setServoPulseWidth(0);

            } else if (input.equals("6")) { // right
                motorL_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(speed);
                motorR_bckw.setServoPulseWidth(0);
                motorR_forw.setServoPulseWidth(speed);

            } else if (input.equals("1")) { // back left
                motorL_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(speed);
                motorR_bckw.setServoPulseWidth(0);
                motorR_forw.setServoPulseWidth(0);

            } else if (input.equals("3")) { // back right
                motorL_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(0);
                motorR_bckw.setServoPulseWidth(speed);
                motorR_forw.setServoPulseWidth(0);

            } else if (input.equals("7")) { // forw left
                motorL_forw.setServoPulseWidth(speed);
                motorL_bckw.setServoPulseWidth(0);
                motorR_bckw.setServoPulseWidth(0);
                motorR_forw.setServoPulseWidth(0);

            } else if (input.equals("9")) { // forw right
                motorL_forw.setServoPulseWidth(0);
                motorL_bckw.setServoPulseWidth(0);
                motorR_bckw.setServoPulseWidth(0);
                motorR_forw.setServoPulseWidth(speed);

            } else if (input.equals("0")) { // audio-buzzer ON
                pinBuzzer.pulse(1000, PinState.HIGH);

            } else if (input.equals("+")) { // increase speed to 1500
                speed = 1500;
                System.out.println("Speed increased!");

            } else if (input.equals("-")) { // decrease speed to 1000
                speed = 1000;
                System.out.println("Speed decreased!");

            } else if (input.equals("q")) { // quit
                break;

            } else {
                System.out.println("Invalid enter, You suck!");
            }


        }

        // shutdown all gpios
        motorL_forw.setServoPulseWidth(0);
        motorL_bckw.setServoPulseWidth(0);
        motorR_forw.setServoPulseWidth(0);
        motorL_bckw.setServoPulseWidth(0);

        gpio.shutdown();


    }
}
