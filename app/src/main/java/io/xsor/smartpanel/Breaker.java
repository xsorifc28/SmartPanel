package io.xsor.smartpanel;

/**
 * Created by xSor.cr on 12/16/2015.
 *
 */
public class Breaker {

    private String name;
    private String gpioPin;

    public Breaker (String name, String gpioPin) {
        this.gpioPin = gpioPin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGpioPin() {
        return gpioPin;
    }

    public void setGpioPin(String gpioPin) {
        this.gpioPin = gpioPin;
    }

    @Override
    public String toString() {
        return "Name: " + getName() + ", GPIO Pin: " + getGpioPin();
    }
}
