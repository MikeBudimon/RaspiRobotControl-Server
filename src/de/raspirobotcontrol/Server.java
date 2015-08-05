package de.raspirobotcontrol;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Websocket server
 */
public class Server implements Runnable {

    static protected boolean started = false;
    private final GpioPinDigitalOutput buzzer;
    private final int pin1, pin2, pin3, pin4;
    protected Thread runningThread = null;
    private DataOutputStream out = null;
    private int serverPort = 8080;
    private ServerSocket serverSocket = null;
    private boolean isStopped = false;
    private DataInputStream in = null;


    public Server(int port, GpioController gpio, Pin buzzer, int pin1, int pin2, int pin3, int pin4) {
        this.serverPort = port;
        this.pin1 = pin1;
        this.pin2 = pin2;
        this.pin3 = pin3;
        this.pin4 = pin4;
        this.buzzer = gpio.provisionDigitalOutputPin(buzzer, PinState.LOW);
        this.buzzer.setShutdownOptions(true, PinState.LOW);
    }


    @Override
    public void run() {

        System.out.println("Server started!");

        synchronized (this) {
            this.runningThread = Thread.currentThread();
        }

        openServerSocket();

        while (!isStopped()) {
            Socket clientSocket;
            try {
                System.out.println("Waiting for client on port " + this.serverSocket.getLocalPort() + "...");
                clientSocket = this.serverSocket.accept();

            } catch (IOException e) {
                if (isStopped()) {
                    System.out.println("Server stopped.");
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }

            try {
                processClientRequest(clientSocket);
            } catch (Exception e) {
                e.printStackTrace();

            }

        }

        System.out.println("Server stopped.");

    }

    private void processClientRequest(Socket clientSocket) throws IOException, InterruptedException {

        System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());


        // initialize wiringPi library
        Gpio.wiringPiSetup();

        // create soft-pwm pins (min=0 ; max=100)
        SoftPwm.softPwmCreate(pin1, 0, 100); // P1-11: motor-left forward
        SoftPwm.softPwmCreate(pin2, 0, 100); // P1-12: motor-left backward
        SoftPwm.softPwmCreate(pin3, 0, 100); // P1-15: motor-right backward
        SoftPwm.softPwmCreate(pin4, 0, 100); // P1-16: motor-right forward

        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());
        started = true;

        String command;

        label:
        while (started) {

            command = in.readUTF();

            switch (command) {
                case "8":  // forward
                    SoftPwm.softPwmWrite(0, 100);
                    SoftPwm.softPwmWrite(1, 0);
                    SoftPwm.softPwmWrite(3, 0);
                    SoftPwm.softPwmWrite(4, 100);
                    System.out.println("Forward!");
                    break;

                case "2":  // backward
                    SoftPwm.softPwmWrite(0, 0);
                    SoftPwm.softPwmWrite(1, 100);
                    SoftPwm.softPwmWrite(3, 100);
                    SoftPwm.softPwmWrite(4, 0);
                    System.out.println("Backward!");
                    break;

                case "5":  // stop
                    SoftPwm.softPwmWrite(0, 0);
                    SoftPwm.softPwmWrite(1, 0);
                    SoftPwm.softPwmWrite(3, 0);
                    SoftPwm.softPwmWrite(4, 0);
                    System.out.println("Stop!");
                    break;

                case "4":  // left
                    SoftPwm.softPwmWrite(0, 0);
                    SoftPwm.softPwmWrite(1, 0);
                    SoftPwm.softPwmWrite(3, 0);
                    SoftPwm.softPwmWrite(4, 100);
                    System.out.println("Left!");
                    break;

                case "6":  // right
                    SoftPwm.softPwmWrite(0, 100);
                    SoftPwm.softPwmWrite(1, 0);
                    SoftPwm.softPwmWrite(3, 0);
                    SoftPwm.softPwmWrite(4, 0);
                    System.out.println("Right!");
                    break;

                case "0":  // audio-buzzer ON
                    buzzer.pulse(2000, PinState.HIGH);
                    System.out.println("Buzzer!");
                    break;

                case "stop": // stop connection
                    started = false;
                    this.out.close();
                    this.in.close();
                    Main.queue.clear();
                    break label;

                default:
                    System.out.println("Invalid enter!");
                    break;
            }

            out.writeUTF(Main.queue.take());
            Thread.sleep(100);
        }
    }


    private boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        started = false;
        try {
            this.in.close();
            out.close();
            this.serverSocket.close();

            // shutdown all software-PWMs
            SoftPwm.softPwmWrite(0, 0);
            SoftPwm.softPwmWrite(1, 0);
            SoftPwm.softPwmWrite(3, 0);
            SoftPwm.softPwmWrite(4, 0);


        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }
}