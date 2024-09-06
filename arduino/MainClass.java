import org.firmata4j.I2CDevice;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.IODevice;
import org.firmata4j.ssd1306.SSD1306;
import edu.princeton.cs.introcs.StdDraw;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class MainClass {

    public static void main(String[] args) throws IOException, InterruptedException {
        String myUSB = "COM4";
        IODevice myGroveBoard = new FirmataDevice(myUSB);
        int dryvalue = 700;
        int wetvalue = 550;

        try {
            myGroveBoard.start();
            System.out.println("Board started.");
            myGroveBoard.ensureInitializationIsDone();
        } catch (Exception ex) {
            System.out.println("Couldn't connect to board.");
        }

        Pin mybtn = myGroveBoard.getPin(6);
        mybtn.setMode(Pin.Mode.INPUT);
        Pin pump = myGroveBoard.getPin(7);
        pump.setMode(Pin.Mode.OUTPUT);
        Pin mySensor = myGroveBoard.getPin(15);
        mySensor.setMode(Pin.Mode.ANALOG);

        I2CDevice i2cObject = myGroveBoard.getI2CDevice((byte) 0x3C); // Use 0x3C for the Grove OLED
        SSD1306 theOledScrnObject = new SSD1306(i2cObject, SSD1306.Size.SSD1306_128_64); // 128x64 OLED SSD1515
        theOledScrnObject.init();

        ButtonListener buttonListener = new ButtonListener(pump, mybtn, theOledScrnObject);
        myGroveBoard.addEventListener(buttonListener);

        boolean systemRunning = false;
        long lastButtonPressTime = 0;
        double Gpoint;

        // ArrayList to store Gpoint values
        ArrayList<Double> gpointValues = new ArrayList<>();

        OLEDSCRN mytask = new OLEDSCRN(systemRunning,theOledScrnObject);
        Timer newtimer = new Timer();
        newtimer.schedule(mytask, 500, 1000);


        while (true) {
            long currentTime = System.currentTimeMillis();
            long buttonValue = mybtn.getValue();

            // Check if the button is pressed and it's been more than 500 milliseconds since the last press
            if (buttonValue == 1 && currentTime - lastButtonPressTime > 500) {
                lastButtonPressTime = currentTime;
                systemRunning = !systemRunning; // Toggle system running state
                System.out.println(systemRunning ? "System started." : "System stopped.");
                if (!systemRunning) {
                    pump.setValue(0); // Stop the pump when the system is stopped
                    System.out.println("Pump has been Stopped Manually!");
                    System.out.println("Want to Start the Pump again?");
                    System.out.println("Please press the button again");
                }
            }

            // If the system is running, read the moisture sensor and control the pump accordingly
            if (systemRunning) {
                long moistureValue = mySensor.getValue();

                // Adjust moisture value if it's out of bounds
                if (moistureValue < 550) {
                    moistureValue = wetvalue;
                } else if (moistureValue > 700) {
                    moistureValue = dryvalue;
                }

                // Calculate Gpoint value
                Gpoint = 1 - (moistureValue - wetvalue) / (double) (dryvalue - wetvalue);
                Gpoint = Gpoint * 100.0;
                gpointValues.add(Gpoint); // Store Gpoint value in the ArrayList

                // Draw Gpoint value on the graph
                plotData(gpointValues);
                Thread.sleep(500);

                // Control the pump based on moisture value
                if (moistureValue > wetvalue) {
                    pump.setValue(1);
                    mytask.setSystemRunning(true);
                    mytask.setValue((float) Gpoint);

                } else {
                    pump.setValue(0);
                    // Turn off the pump
                    mytask.setValue((float) Gpoint);
                    mytask.setSystemRunning(false);
                }

                // Print moisture value
                System.out.println("Moisture value: " + Gpoint);
                mytask.mositurevalue(Gpoint);

            }
        }
    }

    private static void plotData(ArrayList<Double> data){
        long currentTime = System.currentTimeMillis();
        // Set up graph parameters
        StdDraw.clear();
        StdDraw.line(0,0,data.size(),0);
        StdDraw.line(0,0,0,100);
        StdDraw.setXscale(-1, data.size() + 1); // Adjusted x-axis scale
        StdDraw.setYscale(-0.10, 100); // Adjusted y-axis scale
        StdDraw.text(data.size() / 2, -2, "Time"); // Adjusted x-axis label position
        StdDraw.setPenColor(Color.red);
        StdDraw.text(data.size() / 2, 101, "Time vs Moisture Graph"); // Adjusted graph title position
        StdDraw.setPenColor(StdDraw.BLUE);
        StdDraw.text(-0.5, 40, "Moisture", 90); // Adjusted y-axis label position

        // Plot data points
        StdDraw.setPenColor(StdDraw.BLUE);
        for (int i = 1; i < data.size(); i++) {
            double x1 = i - 1;
            double y1 = data.get(i - 1);
            double x2 = i;
            double y2 = data.get(i);
            StdDraw.line(x1, y1, x2, y2);

        }
    }
}