package de.raspirobotcontrol;

import com.pi4j.component.servo.ServoDriver;
import com.pi4j.component.servo.ServoProvider;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

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
    protected Thread runningThread = null;
    private ServoDriver motorL_forw, motorL_bckw, motorR_bckw, motorR_forw;
    private DataOutputStream out = null;
    private int serverPort = 8080;
    private ServerSocket serverSocket = null;
    private boolean isStopped = false;
    private DataInputStream in = null;

    /**
     * Servo cycle time:                20000us
     * Pulse increment step size:       10us
     * Minimum width value:             0 (0us) 0%
     * Maximum width value:             2000 (20000us) 100%
     */
    public Server(int port, GpioController gpio, Pin buzzer, int pin1, int pin2, int pin3, int pin4, ServoProvider servoProvider) throws IOException {
        this.serverPort = port;
        this.buzzer = gpio.provisionDigitalOutputPin(buzzer, PinState.LOW);
        this.buzzer.setShutdownOptions(true, PinState.LOW);
        this.motorL_forw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(pin1)); // P1-11: left forward
        this.motorL_bckw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(pin2)); // P1-12: left backward
        this.motorR_bckw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(pin3)); // P1-15: right backward
        this.motorR_forw = servoProvider.getServoDriver(servoProvider.getDefinedServoPins().get(pin4)); // P1-16: right forward
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
            } finally {
                if (started) {
                    started = false;
                    try {
                        this.out.close();
                        this.in.close();
                        Main.queue.clear();
                        stopPwms();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("Server stopped.");
    }

    private void processClientRequest(Socket clientSocket) throws IOException, InterruptedException {

        System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());

        Integer speed = 2000;   // Set raw value for servo drivers: 0 - 2000
        in = new DataInputStream(clientSocket.getInputStream());
        out = new DataOutputStream(clientSocket.getOutputStream());
        started = true;
        String command = "5";

        label:
        while (started) {
            if (Main.queue.size() > 0) out.writeUTF(Main.queue.take());
            if (in.available() > 0) command = in.readUTF();

            switch (command) {
                case "8":  // forward
                    motorL_forw.setServoPulseWidth(speed);
                    motorR_forw.setServoPulseWidth(speed);
                    motorL_bckw.setServoPulseWidth(0);
                    motorR_bckw.setServoPulseWidth(0);
                    //System.out.println("Forward!");
                    break;
                case "2":  // backward
                    motorL_forw.setServoPulseWidth(0);
                    motorR_forw.setServoPulseWidth(0);
                    motorL_bckw.setServoPulseWidth(speed);
                    motorR_bckw.setServoPulseWidth(speed);
                    //System.out.println("Backward!");
                    break;
                case "5":  // stop
                    stopPwms();
                    //System.out.println("Stop!");
                    break;
                case "4":  // left
                    motorL_forw.setServoPulseWidth(0);
                    motorR_forw.setServoPulseWidth(speed);
                    motorL_bckw.setServoPulseWidth(speed);
                    motorR_bckw.setServoPulseWidth(0);
                    //System.out.println("Left!");
                    break;
                case "6":  // right
                    motorL_forw.setServoPulseWidth(speed);
                    motorR_forw.setServoPulseWidth(0);
                    motorL_bckw.setServoPulseWidth(0);
                    motorR_bckw.setServoPulseWidth(speed);
                    //System.out.println("Right!");
                    break;
                case "0":  // audio-buzzer ON
                    buzzer.pulse(1000, PinState.HIGH);
                    //System.out.println("Buzzer!");
                    break;
                case "stop": // stop connection
                    started = false;
                    this.out.close();
                    this.in.close();
                    Main.queue.clear();
                    stopPwms();
                    break label;
                default:
                    stopPwms();
                    //System.out.println("Invalid enter!");
                    break;
            }
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
            stopPwms();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    /**
     * shutdown all software-PWMs
     */
    private void stopPwms() {
        motorL_forw.setServoPulseWidth(0);
        motorR_forw.setServoPulseWidth(0);
        motorL_bckw.setServoPulseWidth(0);
        motorR_bckw.setServoPulseWidth(0);
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port 9000", e);
        }
    }
}