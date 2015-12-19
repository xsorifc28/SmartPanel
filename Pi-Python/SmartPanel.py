from pubnub import Pubnub
from time import sleep
from sys import argv
import RPi.GPIO as GPIO
import os.path

# See http://pi4j.com/images/j8header-2b.png for pin out
# This sets the GPIO mode to GPIO.BOARD. 
# The pins used should be declared from 1-40,
# rather than the GPIOXX seen in the image referenced above.
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BOARD)

# Declare constants
smartPanelCh = "SmartPanelCh"
SEP = "_"
statusStr = "status"
wrongCodeStr = "wrongPin"

ON = 0
OFF = 1
STATUS = 2


# Instantiate PubNub instance
pubnub = Pubnub(publish_key = "pub-c-11122c1e-4f32-4222-a8d8-cdacb80b2ba0",
		subscribe_key = "sub-c-d3143fa0-9c46-11e5-b44a-02ee2ddab7fe",
		uuid = "Matt-RaspberryPi")

# Function to toggle a GPIO on and off 
def toggleGPIO(on,gpio):
    print('Toggle gpio: ' + str(on))
    if on == 1:
       	GPIO.output(gpio,True)

    elif on == 0:
       	GPIO.output(gpio,False)

# Function to send the status of the GPIO to the PubNub Channel		
def sendStatus(breakerName,gpio):
    status =  str(GPIO.input(gpio))
    print('Sending status of breaker \'' + breakerName +'\':' + status)
    pubnub.publish(smartPanelCh,statusStr + SEP + breakerName + SEP + status)

# Function to notify the channel that the wrong PIN was submitted
def sendWrongCodeMsg():
    pubnub.publish(smartPanelCh,wrongCodeStr)

# Function to store the PIN for a breaker to a file	
def writePin(breakername,code):
    target = open(breakername, 'w')
    target.truncate()
    target.write(code)
    target.close()

# Function to read the PIN for a breaker from a file
def readPin(breakername,code):
    target = open(breakername, 'r')
    a = target.read()
    return a
    
# Main function which will process messages from the channel
# Messages will be received in the following format
# BREAKERNAME_GPIO_ACTION_CODE
# For example, to KITCHEN_5_1_1234
	
def processMessage(message, smartPanelCh):
    # The message structure that is received:
    # If action is on or off, code will be available
                print("Message received: " + message)

                # Prevent local message being processed by local node
                if(message == wrongCodeStr):
                    return
                # Exit if shutdown message was received
				elif(message == 'shutdown'):
                    exit()
					
        # Split and process the message        
		splitMessage = message.split(SEP)
            
                if(splitMessage[0] != statusStr):
                    breakerName = splitMessage[0]
                    gpio = int(splitMessage[1])
                    GPIO.setup(gpio,GPIO.OUT)
                    action = int(splitMessage[2])

                    if(action == ON or action == OFF):
                        print("Action is on or off")
                        code = splitMessage[3]
                        print("Received code is: " + code)
                        storedCode = readPin(breakerName,code)
                        print("Stored code is: " + storedCode)

                        if (action == OFF):
                            writePin(breakerName,code)
                            readPin(breakerName,code)
                            toggleGPIO(action,gpio)
                        elif(action == ON and storedCode == code):
                            toggleGPIO(action,gpio)
                        elif(storedCode != code):
                            sendWrongCodeMsg()
                            
                        sendStatus(breakerName,gpio)

                    elif (action == STATUS):
                        if (os.path.isfile(breakerName) != True):
                            writePin(breakerName,'0000')
                        sendStatus(breakerName,gpio)
		

# Subscribe to PubNub and set the callback to the processMessage function		
pubnub.subscribe(smartPanelCh, callback = processMessage)