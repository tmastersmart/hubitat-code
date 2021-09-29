/* Iris v1 KeyPad Driver
Hubitat Iris v1 KeyPad driver 
v2 now supports keypad functions

Please press initialize after updating


  _____ _____  _____  _____        __    _  __                          _ 
 |_   _|  __ \|_   _|/ ____|      /_ |  | |/ /                         | |
   | | | |__) | | | | (___   __   _| |  | ' / ___ _   _ _ __   __ _  __| |
   | | |  _  /  | |  \___ \  \ \ / / |  |  < / _ \ | | | '_ \ / _` |/ _` |
  _| |_| | \ \ _| |_ ____) |  \ V /| |  | . \  __/ |_| | |_) | (_| | (_| |
 |_____|_|  \_\_____|_____/    \_/ |_|  |_|\_\___|\__, | .__/ \__,_|\__,_|
                                                   __/ | |                
                                                  |___/|_|   


Arming
ON   = Arm Away
Part = Arm Night
#    = Arm Home
*    = NA

Disarming
enter PIN  (dont use OFF or ON buttons)

Panic
Pacic = Sirene ON Alarm = on
OFF = Sirene OFF Alarm = off

Buttion Support
All keypad number buttons mapped to 10 push buttons. 

Tamper
Invalid PIN will press

Passcodes
MASTER 7 digit pin

Chimes Lights not yet working.

Total worktime to build 2 days. Have fun.
 
FCC ID:FU5TSA04 https://fccid.io/FU5TSA04
Built by Everspring Industry Co Ltd Smart Keypad TSA04            

I would use the sample keypad code hubitat posted 
but its not open source so I wrote my own.
Thats caused a delay in finishing the driver.
I wrote this for my keyboards you are welcome to use it.
If anyone knows the commands to activate the chimes let 
me know....



To Reset for paring:
Remove batteries (if already powered up.)
Press the On key 8 times(Or Hold down ON for 8 seconds)

Insert two batteries side-by-side at one end or the other
Press the On key 8 times
You should see the keypad light up, and the On button will begin to blink twice periodically.



*   v2.1 09/29/2021 Tamper bugs fixed, Log fix,Old IRIS command trapped Master pin added 
*   v2.0 09/28/2021 Keypad support debugged , Commands debounced, Logging cleaned up, Invalid wrong size pins trapped
                    Star key sends a 6 digit PIN *#  * Now trapped. Perhaps a debug master PIN.
*   v1.1 09/27/2021 Cleanup Button controler is working
*   v1.0 09/27/2021 Beta test version Buttons now reporting Bat working

Tested on 
2013-06-28
2012-12-11

https://github.com/tmastersmart/hubitat-code/blob/main/iris_v1_keypad.groovy
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_v1_keypad.groovy

Post your comments here. 
http://www.winnfreenet.com/wp/2021/09/iris-v1-keyboard-driver-for-hubitat/



 * See opensource IRIS code at  https://github.com/arcus-smart-home I have been unable to find any iris v1 code in it

  
==========================================================================================================================

 * based on iris code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.

 */


import hubitat.zigbee.clusters.iaszone.ZoneStatus


metadata {

	definition (name: "Iris v1 Keypad", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_v1_keypad.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
		capability "PresenceSensor"
		capability "Refresh"
		capability "Sensor"
		capability "SignalStrength"
		capability "Security Keypad"
//              capability "Chime"
        capability "Alarm"
		capability "PushableButton"
        capability "TamperAlert"
//		capability "Switch"



		command "checkPresence"
		command "normalMode"
		command "rangingMode"
		//command "quietMode"

		attribute "batteryState", "string"
		attribute "batteryVoltage", "string"
		
	
		attribute "code1", "string"
		attribute "code2", "string"
		attribute "code3", "string"
		attribute "code4", "string"
		
	
		attribute "code1n", "string"
		attribute "code2n", "string"
		attribute "code3n", "string"
		attribute "code4n", "string"
		

		fingerprint profileId: "C216", inClusters: "00F0,00C0", outClusters: "", manufacturer: "AlertMe", model: "KeyPad Device", deviceJoinName: "Iris V1 Keypad"

	}

}
// fingerprint model:"KeyPad Device", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00C0", outClusters:""


preferences {
	
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
	
    input("secure",  "text", title: "7 digit password", description: "A Master 7 digit secure password",defaultValue: 0,required: false)

}

// So far this doesnt work because hub detects it has a care fob and you have to manualy install
def installed(){logging("${device} : Paired!", "info")}

// You have to manualy do this 
def initialize() {
state.batteryOkay = true
state.operatingMode = "normal"
state.presenceUpdated = 0
state.rangingPulses = 0
state.Command = "off"
state.Panic = "off"
    
sendEvent(name: "battery",value:0, unit: "%", isStateChange: false)
sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
sendEvent(name: "lqi", value: 0, isStateChange: false)
sendEvent(name: "operation", value: "unknown", isStateChange: false)
sendEvent(name: "presence", value: "not present", isStateChange: false)
sendEvent(name: "numberOfButtons", value: "10", isStateChange: false)
sendEvent(name: "alarm", value: "off", isStateChange: false)    
sendEvent(name: "maxCodes", value:5)
sendEvent(name: "codeLength", value:4)
sendEvent(name: "alarm", value: "off")
sendEvent(name: "securityKeypad", value: "disarmed")
sendEvent(name: "tamper", value: "clear")

state.remove("firmwareVersion")	
state.remove("uptime")
state.remove("uptimeReceived")
state.remove("relayClosed")
state.remove("rssi")
state.remove("pushed")

removeDataValue("pushed")

operation

// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!
randomSixty = Math.abs(new Random().nextInt() % 60)
runIn(randomSixty,refresh)
// Initialisation complete.
logging("${device} : Initialised", "info")
}


def configure() {

	initialize()
	unschedule()

	// Default logging preferences.
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])

	// Schedule our ranging report.
	int checkEveryHours = 6																						// Request a ranging report and refresh every 6 hours or every 1 hour for outlets.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)	// At X seconds past X minute, every checkEveryHours hours, starting at Y hour.

	// Schedule the presence check.
	int checkEveryMinutes = 6																					// Check presence timestamp every 6 minutes or every 1 minute for key fobs.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)									// At X seconds past the minute, every checkEveryMinutes minutes.

	// Configuration complete.
	logging("${device} : Configured", "info")

	// Run a ranging report and then switch to normal operating mode.
	rangingMode()
	runIn(12,normalMode)
	
}


def updated() {
	loggingStatus()
	runIn(3600,debugLogOff)
	runIn(1800,traceLogOff)
	refresh()

}


def setCode(code,pinCode,userCode){
	logging( "${device} : setCode#${code} PIN:${pinCode} User:${userCode} ","info")
	if (code == 1){ save= "code1";}
	if (code == 2){ save= "code2";}
	if (code == 3){ save= "code3";}
	if (code == 4){ save= "code4";}	
    if (code == 5){ save= "code5";}	
	if (code < 6){
	sendEvent(name: "${save}", value: pinCode)
	sendEvent(name: "${save}n",value: userCode)
	}	
}

def deleteCode(code) {
	logging ("${device} : deleteCode  #${code}","info")
	if (code == 1){ save= "code1";}
	if (code == 2){ save= "code2";}
	if (code == 3){ save= "code3";}
	if (code == 4){ save= "code4";}	
	if (code == 5){ save= "code5";}	   
	if (code < 6){
	sendEvent(name: "${save}", value: " ")
	sendEvent(name: "${save}n",value: " ")
        
    device.removeSetting("${save}")   
	device.removeSetting("${save}n")  
	}	
	
}	
// unsupported error matrix
def getCodes(code){     logging("${device} : getCodes  unsupported", "info")             }
def setEntryDelay(code){logging("${device} : setEntryDelay ${code}  unsupported", "info")}
def setExitDelay(code){	logging("${device} : setExitDelay  ${code}  unsupported", "info")}
def setCodeLength(code){logging("${device} : setCodeLength 4", "info")                   }
	

def armAway() {
	logging ("${device} : Sending armAWAY","info")
	sendEvent(name: "securityKeypad",value: "armedAway")
	sendLocationEvent (name: "hsmSetArm", value: "armAway")
    state.Command = "away"
}
def armHome() {
	logging ("${device} : Sending armHome","info")
	sendEvent(name: "securityKeypad",value: "armedHome")
	sendLocationEvent (name: "hsmSetArm", value: "armHome")
    state.Command = "home"
}
def armNight() {
	logging ("${device} : Sending armNight","info")
	sendEvent(name: "securityKeypad",value: "armedNight")
	sendLocationEvent (name: "hsmSetArm", value: "armNight")
    state.Command = "night"
}
def disarm() {
	logging ("${device} : Sending disarm", "info")
	sendEvent(name: "securityKeypad", value: "disarmed")
	sendLocationEvent (name: "hsmSetArm", value: "disarm")
    state.Command = "off"
}
def off(cmd){
    sendEvent(name: "siren",  value: "off", descriptionText: "Its OFF", displayed: true)
    sendEvent(name: "strobe", value: "off", descriptionText: "not supported", displayed: true)
    sendEvent(name: "alarm",  value: "off", descriptionText: "Its OFF", displayed: true) 
    logging ("${device} : OFF Strobe/Siren/Panic/Alarm","info")
    state.Panic = "off"    
}

def siren(cmd){
  logging ("${device} : Siren ON", "info")
  sendEvent(name: "siren", value: "on")
  sendEvent(name: "alarm", value: "on")
  state.Panic = "alarm"    
}
def strobe(cmd){
  logging ("${device} : Strobe ON","info")  
  sendEvent(name: "siren", value: "on", descriptionText: "not supported yet", displayed: true)
  sendEvent(name: "alarm", value: "on")
  state.Panic = "alarm"    
}
def both(cmd){
  logging ("${device} : both ON siren/strobe","info")
  sendEvent(name: "siren", value: "on")  
  sendEvent(name: "strobe", value: "on", descriptionText: "not supported yet", displayed: true) 
  sendEvent(name: "alarm", value: "on") 
  state.Panic = "alarm"   
}

def on() {
   siren(1)
}


def tamper(){
sendEvent(name: "tamper", value: "detected")
logging ("${device} : Tamper Pressed and Released","info")
sendEvent(name: "tamper", value: "clear")
}

def press(buttonNumber){
   logging("${device} : Button ${buttonNumber}","info")
   sendEvent(name: "pushed", value: buttonNumber, isStateChange: true) 
}

void reportToDev(map) {

	String[] receivedData = map.data

	def receivedDataCount = ""
	if (receivedData != null) {
		receivedDataCount = "${receivedData.length} bits of "
	}
	logging("${device} : Report to DEV cluster:${map.cluster}, clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command} with value:${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
}
// clusters so far
//0013, command:00 12 bits of data: [82, 00, 1E, 28, 7E, 6C, 03, 00, 6F, 0D, 00, 80]

def normalMode() {

	// This is the standard running mode.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"])
	state.operatingMode = "normal"
	refresh()
	sendEvent(name: "operation", value: "normal")
	logging ( "${device} : Mode : Normal","info")

}


def rangingMode() {

	// Ranging mode double-flashes (good signal) or triple-flashes (poor signal) the ON button
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"])
	sendEvent(name: "operation", value: "ranging")
    logging ("${device} : Mode: Ranging ${state.rangingPulses}","info")
	// Ranging will be disabled after a maximum of 30 pulses.
	state.rangingPulses = 0

}


def quietMode() {
	// Turns off all reporting except for a ranging message every 2 minutes.
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])
	state.operatingMode = "quiet"
	// We don't receive any of these in quiet mode, so reset them.
	sendEvent(name: "battery",value:0, unit: "%", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")
	logging ("${device} : Mode : Quiet","info")
    refresh()
}

def getStatus(status) {
    status = location.hsmStatus
    logging ("${device} : Received HSM ${status} Our state:${state.Command}","debug")
// HUB armedAway, armingAway, armedHome, armingHome, armedNight, armingNight, disarmed, allDisarmed
// Mine away home night panic off    
    if (status == "armedAway"){  
        if (state.Command != "away"){
            sendEvent(name: "securityKeypad", value: "armedAway")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "away" 
        }
    }
    if (status == "armingAway"){ 
        if (state.Command != "away"){
            sendEvent(name: "securityKeypad", value: "armedAway")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "away"
        }
    }
    
    if (status == "armedHome"){
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armedHome")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "home"
        }
       }
    if (status == "armingHome"){ 
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armedHome")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "home"
        }
       }  
    
    if (status == "armedNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad", value: "armedNight")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "hight"
        }
       }

    if (status == "armingNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad", value: "armedNight")
            logging ("${device} : Received HSM  ${status}","info")
            state.Command = "night"
        }
       } 
    
    if (status == "disarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad", value: "disarmed")
            state.Command = "off"
            logging ("${device} : Received HSM ${status}","info")
        }
    }
    if (status == "allDisarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad", value: "disarmed")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "off"
        }
    } 
    
}

void refresh() {
    getStatus(status)
    logging ("${device} : Refresh","info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])	   // version information request
}


def rangeAndRefresh() {
// This toggles ranging mode to update the device's LQI value.
	int returnToModeSeconds = 6			// We use 3 seconds for outlets, 6 seconds for battery devices, which respond a little more slowly.
	rangingMode()
	runIn(returnToModeSeconds, "${state.operatingMode}Mode")
}


def updatePresence() {
	long millisNow = new Date().time
	state.presenceUpdated = millisNow
	
	if (device.currentValue("presence") != "present"){
	 sendEvent(name: "presence", value: "present")
	 logging ( "${device} :Present Last ${secondsElapsed} seconds ago.","info")
	}	
}


def checkPresence() {
	presenceTimeoutMinutes = 4
	uptimeAllowanceMinutes = 5
	if (state.presenceUpdated > 0 && state.batteryOkay == true) {
		long millisNow = new Date().time
		long millisElapsed = millisNow - state.presenceUpdated
		long presenceTimeoutMillis = presenceTimeoutMinutes * 60000
		BigInteger secondsElapsed = BigDecimal.valueOf(millisElapsed / 1000)
		BigInteger hubUptime = location.hub.uptime

		if (millisElapsed > presenceTimeoutMillis) {

			if (hubUptime > uptimeAllowanceMinutes * 60) {

				sendEvent(name: "presence", value: "not present")
				logging("${device} : Presence : Not Present! Last report received ${secondsElapsed} seconds ago.", "warn")

			} else {
			logging("${device} : Presence : Ignoring overdue presence reports for ${uptimeAllowanceMinutes} minutes. The hub was rebooted ${hubUptime} seconds ago.","info")
			}

		} else {
     		sendEvent(name: "presence", value: "present")
			logging("${device} : Presence : Last presence report ${secondsElapsed} seconds ago.","debug")
		}
		logging("${device} : checkPresence() : ${millisNow} - ${state.presenceUpdated} = ${millisElapsed} (Threshold: ${presenceTimeoutMillis} ms)","trace")
	} else if (state.presenceUpdated > 0 && state.batteryOkay == false) {
		sendEvent(name: "presence", value: "not present")
		logging("${device} : Presence : Battery too low! Reporting not present as this device will no longer be reliable.", "warn")
	} else {
		logging("${device} : Presence : Not yet received. Your device may at max range if so you may have to use built in driver with no presence. ", "warn")
	}
}


def parse(String description) {
	// Primary parse routine.
	// catchall: C216 00C0 02 02 0040 00 1E00 00 00 0000 00 01 2000 <--- spamms this 
	logging ("${device} : $description","trace")
    // We check stat first and debounce ARM buttons if it took.
    // Keyboard spams cmd about 6 times. So we autodebounce and resend.
    getStatus(status)
    updatePresence()
	Map descriptionMap = zigbee.parseDescriptionAsMap(description)
	if (descriptionMap) {
		processMap(descriptionMap)
	} else {
		logging("${device} : Parse Failed ..${description}", "warn")
	}
}




def processMap(Map map) {
	logging ("${device} : processMap() : ${map}","trace")
	String[] receivedData = map.data	// AlertMe values are always sent in a data element.
    logging("${device} : debug  Cluster:${map.clusterId}   State:${map.command}","trace")

//0013, command:00 12 bits of data: [82, 00, 1E, 28, 7E, 6C, 03, 00, 6F, 0D, 00, 80]
    if (map.clusterID == "0013"){
	logging("${device} : Device moved remapping","warn")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Remapping","debug")
	
//  never seen this including just in case
    } else if (map.clusterId == "0006") {
		logging("${device} : Sending Match Descriptor Response","debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])

    } else if (map.clusterId == "00C0") {
     // Iris Button report cluster 
     if (map.command != "00"){logging ("${device} : key Cluster CMD:${map.command} MAP:${map.data}","trace") }

     if (map.command == "0A") {  
      keyRec   = receivedData[4]    
	  buttonNumber = 0 
	  size = receivedData.size()     
      logging ("${device} : Keypad #${keyRec} size:${size} State:${state.Command} Panic:${state.Panic}","debug")

      if (size == 10){ 
	 asciiPin = receivedData[4..9].collect{ (char)Integer.parseInt(it, 16) }.join()
     irsCMD = receivedData[4..4].collect{ (char)Integer.parseInt(it, 16) }.join() 
          //old iris commands (just logging)
          if (irsCMD == "H") {irsCMD= "HOME"}
          if (irsCMD == "A") {irsCMD= "AWAY"}
          if (irsCMD == "N") {irsCMD= "NIGHT"}
          if (irsCMD == "H") {irsCMD= "HOME"}
          if (irsCMD == "P") {irsCMD= "PANIC"}
          logging("${device} : Received: Iris command ${irsCMD} [${asciiPin}]", "info")
	 return
	 }    
     
       
      if (keyRec == "41"){
           if (state.Command =="away"){ 
             logging("${device} : Button ON","debug")
             return }
             logging("${device} : Button ON","info")
         armAway()
	 } 	     
      if (keyRec == "48"){
         if (state.Panic =="off"){
             logging("${device} : Button OFF","debug")
             return
         }
         logging("${device} : Button OFF","info")
		 off()
         return
	 } 
      if (keyRec == "2A"){
          logging("${device} : Button *","info")
     
	 }
  
         
     if (keyRec == "23"){
		 if (state.Command =="home"){
         logging("${device} : Button #","debug")
         return }
         logging("${device} : Button #","info")
 		 armHome()
	 } 
     if (keyRec == "4E"){
		 if (state.Command =="night"){
         logging("${device} : Button PARTIAL","debug")  
         return }
         logging("${device} : Button PARTIAL","info")  
		 armNight()
	 }       
     if (keyRec == "50"){
         if (state.Panic =="alarm"){ 
             logging("${device} : Button PANIC","debug") 
             return }    
		 siren()
         logging("${device} : Button PANIC", "warn")    
         state.Panic = "alarm"    
   	  }
     // star key sends *#	*  Or some other garbage
     
     if (size == 11){  
	 asciiPin = receivedData[4..10].collect{ (char)Integer.parseInt(it, 16) }.join()
     sendEvent(name: "PIN", value: "MASTER")
      if (secure == asciiPin){ 
          logging("${device} : Disarmed by MASTER PIN","info")
	      disarm()
	      return
	    }
      logging("${device} : PIN ${asciiPin} Invalid HACKING Master PIN" , "warn")
      tamper()
      return	 
     }
     if (size == 10){ // Trap all long passwords
	 asciiPin = receivedData[4..9].collect{ (char)Integer.parseInt(it, 16) }.join()
     logging("${device} : Received [${asciiPin}]", "warn")
     return
	 } 
     if (size == 9){ 
	 asciiPin = receivedData[4..8].collect{ (char)Integer.parseInt(it, 16) }.join()
         logging("${device} : Received [${asciiPin}]", "warn")

	 }         
	 if (size == 6){
	 asciiPin = receivedData[4..5].collect{ (char)Integer.parseInt(it, 16) }.join()
	 logging("${device} : Received [${asciiPin}]" , "warn")

	 }
	 if (size == 7){
	 asciiPin = receivedData[4..6].collect{ (char)Integer.parseInt(it, 16) }.join()
         logging("${device} :Received [${asciiPin}]" , "warn")

	 }	 
     if (size == 8) { 
      asciiPin = receivedData[4..7].collect{ (char)Integer.parseInt(it, 16) }.join()
      sendEvent(name: "PIN", value: asciiPin)

      if (device.currentValue("code1") == asciiPin){
          name = device.currentValue("code1n")
          logging("${device} : Disarmed by ${name}","info")
	  disarm()
	  return
	 }	     
         if (device.currentValue("code2") == asciiPin){
          name = device.currentValue("code2n")
          logging("${device} : Disarmed by ${name}","info")
	  disarm()
	  return
	}
      if (device.currentValue("code3") == asciiPin){
          name = device.currentValue("code3n")
          logging("${device} : Disarmed by ${name}","info")
 	  disarm()
	  return
	}
	  if (device.currentValue("code4") == asciiPin){
          name = device.currentValue("code4n")
          logging("${device} : Disarmed by ${name}","info")
	  disarm()
	  return
	}
      if (device.currentValue("code5") == asciiPin){
         name = device.currentValue("code5n")
         logging("${device} : Disarmed by ${name}","info")
	  disarm()
	  return
	}   
         
      logging("${device} : PIN ${asciiPin} Invalid  HACKING" , "warn")
      tamper()   
      return	 
	 }// end pin code 
         
         // Keypad button matrix         
         if (keyRec == "31"){press(1)}
         if (keyRec == "32"){press(2)}
         if (keyRec == "33"){press(3)}
         if (keyRec == "34"){press(4)}
         if (keyRec == "35"){press(5)}
         if (keyRec == "36"){press(6)}
         if (keyRec == "37"){press(7)}
         if (keyRec == "38"){press(8)}
         if (keyRec == "39"){press(9)}
         if (keyRec == "30"){press(10)}
        }// end of 0A
 
    
        
    } else if (map.clusterId == "00F0") {
      // Device status cluster. 
      def batteryVoltageHex = "undefined"
      BigDecimal batteryVoltage = 0
      batteryVoltageHex = receivedData[5..6].reverse().join()
        if (batteryVoltageHex == "FFFF") {return}
      batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
      logging("${device} : battery #${batteryVoltageHex}  ${batteryVoltage}volts","trace")
      batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)

      // battery doesnt report all batteries  I get 2.9 volts on 2 good batteries
      // more work in needed 6 AA batteries 
		BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 1.99
		BigDecimal batteryVoltageScaleMax = 2.90

            batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage

       // debounce batt spamming (ignore if same as last event)
        if (state.lastBattery != battertVoltage){
 	 logging( "${device} : Battery : $batteryPercentage% ($batteryVoltage V)","info")
	 sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
     	 sendEvent(name: "battery", value:batteryPercentage, unit: "%")
         
         
         if (batteryPercentage > 24) {  
             sendEvent(name: "batteryState", value: "ok")
             state.batteryOkay = true
             }
            
         if (batteryPercentage < 25) {
             logging("${device} : Battery LOW : $batteryPercentage%", "warn")
             sendEvent(name: "batteryState", value: "low")
             state.batteryOkay = true
         }
  
	 if (batteryPercentage < 19) {
            logging("${device} : Battery BAD: $batteryPercentage%", "warn") 
	    state.batteryOkay = false
	    sendEvent(name: "batteryState", value: "exhausted")
	}
        state.lastBattery = batteryVoltage     
    }

} else if (map.clusterId == "00F6") {
 // Discovery cluster. 
  if (map.command == "FD") {
   // Ranging is our jam, Hubitat deals with joining on our behalf.
   def lqiRangingHex = "undefined"
   int lqiRanging = 0
   lqiRangingHex = receivedData[0]
   lqiRanging = zigbee.convertHexToInt(lqiRangingHex)
   sendEvent(name: "lqi", value: lqiRanging)
   logging ("${device} : lqiRanging : ${lqiRanging}","debug")

        if (receivedData[1] == "77") {
           // This is ranging mode, which must be temporary. Make sure we come out of it.
           state.rangingPulses++
	   if (state.rangingPulses > 30) {"${state.operatingMode}Mode"()}

	} else if (receivedData[1] == "FF") {
          // This is the ranging report received every 30 seconds while in quiet mode.
	  logging ("${device} : quiet ranging report received","debug")

	} else if (receivedData[1] == "00") {
          // This is the ranging report received when the device reboots.
	  // After rebooting a refresh is required to bring back remote control.
          loging("${device} : reboot ranging report received","debug")
          refresh()

	} else {
          // Something to do with ranging we don't know about!
          reportToDev(map)
	} 

} else if (map.command == "FE") {
	// Device version response.
	def versionInfoHex = receivedData[31..receivedData.size() - 1].join()
	StringBuilder str = new StringBuilder()
	 for (int i = 0; i < versionInfoHex.length(); i+=2) {
	 str.append((char) Integer.parseInt(versionInfoHex.substring(i, i + 2), 16))
	 } 
	String versionInfo = str.toString()
	String[] versionInfoBlocks = versionInfo.split("\\s")
	int versionInfoBlockCount = versionInfoBlocks.size()
	String versionInfoDump = versionInfoBlocks[0..versionInfoBlockCount - 1].toString()
	logging("${device} : Device version Size:${versionInfoBlockCount} blocks:${versionInfoDump}","debug")
	String deviceManufacturer = "AlertMe"
	String deviceModel = ""
	String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
	// Sometimes the model name contains spaces.
	if (versionInfoBlockCount == 2) {
	deviceModel = versionInfoBlocks[0]
	} else {
	deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
	}
	logging("${device} : Device : ${deviceModel}", "info")// KeyPad Device
	logging("${device} : Firmware : ${deviceFirmware}", "info")//2013-06-28
	updateDataValue("manufacturer", deviceManufacturer)
        updateDataValue("device", deviceModel)
        updateDataValue("model", "KPD800")
	updateDataValue("firmware", deviceFirmware)
        updateDataValue("fcc", "FU5TSA04")
        updateDataValue("partno", "TSA04-0")
     } else {
	// Not a clue what we've received.
        reportToDev(map)
       }
} else if (map.clusterId == "8032" ) {
	// These clusters are sometimes received when joining new devices to the mesh.
        //   8032 arrives with 80 bytes of data, probably routing and neighbour information.
        // We don't do anything with this, the mesh re-jigs itself and is a known thing with AlertMe devices.
	logging( "${device} : New join has triggered a routing table reshuffle.","debug")
     } else {
	// Not a clue what we've received.
	reportToDev(map)
	}
	return null
}


void sendZigbeeCommands(List<String> cmds) {
    // All hub commands go through here for immediate transmission and to avoid some method() weirdness.
    logging( "${device} : Send Zigbee Cmd :${cmds}","trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}


private String[] millisToDhms(int millisToParse) {

	long secondsToParse = millisToParse / 1000

	def dhms = []
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 24)
	secondsToParse = secondsToParse / 24
	dhms.add(secondsToParse % 365)
	return dhms

}


private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}

void loggingStatus() {
	log.info "${device} : Logging : ${infoLogging == true}"
	log.debug "${device} : Debug Logging : ${debugLogging == true}"
	log.trace "${device} : Trace Logging : ${traceLogging == true}"
}


void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}


void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}



private boolean logging(String message, String level) {

	boolean didLog = false

	if (level == "error") {
		log.error "$message"
		didLog = true
	}

	if (level == "warn") {
		log.warn "$message"
		didLog = true
	}

	if (traceLogging && level == "trace") {
		log.trace "$message"
		didLog = true
	}

	if (debugLogging && level == "debug") {
		log.debug "$message"
		didLog = true
	}

	if (infoLogging && level == "info") {
		log.info "$message"
		didLog = true
	}

	return didLog

}
