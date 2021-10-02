/* Iris v1 KeyPad Driver
Hubitat Iris v1 KeyPad driver 
Supports keypad disarm arm functions (no chimes)
Works with Lock Code Manager 

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

Disarming
enter PIN  (dont use OFF or ON buttons)

Panic
Panic = Panic ON
PIN = Panic off

Buttion Support
If a key is pressed once it acts like a button not a PIN
All keypad number buttons mapped to 10 push buttons.

Tamper
Invalid PIN will press

Passcodes
MASTER 7 digit pin

Optional
* switch on
OFF switch OFF
# armHome

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
Press the On key 8 times(Or is it Hold down ON for 8 seconds)

Insert two batteries side-by-side at one end or the other
Press the On key 8 times
You should see the keypad light up, and the On button will begin to blink twice periodically.

*   v2.5 10/02/2021 Config for tamper,Log debug cleanup,Remove alarm no sounds
*   v2.4 09/30/2021 Custom Panic command added. Added Config options for * # OFF
*   v2.3 09/30/2021 battery value changes
*   v2.2 09/29/2021 Version detection and auto upgrade/install. 
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

 * based on alertme UK code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.

 */
def clientVersion() {
    TheVersion="2.5.3"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

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
//        capability "Alarm"
		capability "PushableButton"
        capability "TamperAlert"
		capability "Switch"



		command "checkPresence"
		command "normalMode"
		command "rangingMode"
		//command "quietMode"

		attribute "batteryState", "string"
		attribute "panic", "string"
        attribute "code1", "string"
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
	
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false

	input name: "switchByOFF", type: "bool", title: "OFF/* control a Switch", description: "If disabled STAR OFF buttons are ignored ",defaultValue: false
	input name: "poundActive", type: "bool", title: "# sets ArmHome", description: "If disabled POUND button is ignored ",defaultValue: false
	input name: "tamperPIN",   type: "bool", title: "Press Tamper on BAD PIN", defaultValue: true
  
  
    input("secure",  "text", title: "7 digit password", description: "A Master 7 digit secure PIN. Seperate from Lock Code Manager 0=disable",defaultValue: 0,required: false)

    input name: "BatType", type: "enum", title: "Battery Type", options: ["Lithium", "Alkaline", "NiMH", "NiCad"], defaultValue: "Alkaline" 

}

// So far this doesnt work because hub detects it has a care fob and you have to manualy install
def installed(){logging("${device} : Paired!", "info")}

 
def initialize() {
state.batteryOkay = true
state.operatingMode = "normal"
state.presenceUpdated = 0
state.rangingPulses = 0
state.Command = "unknown"
state.Panic = false
    
sendEvent(name: "battery",value:100, unit: "%", isStateChange: false)
sendEvent(name: "batteryVoltage", value: 3, unit: "V", isStateChange: false)
sendEvent(name: "lqi", value: 0, isStateChange: false)
sendEvent(name: "operation", value: "normal", isStateChange: false)
sendEvent(name: "presence", value: "present", isStateChange: false)
sendEvent(name: "numberOfButtons", value: "10", isStateChange: false)
sendEvent(name: "panic", value: "off", isStateChange: false)    
sendEvent(name: "maxCodes", value:5)
sendEvent(name: "codeLength", value:4)
//sendEvent(name: "alarm", value: "off")
sendEvent(name: "securityKeypad", value: "Fetching")
sendEvent(name: "tamper", value: "clear")

state.remove("switch")	
state.remove("uptime")
state.remove("uptimeReceived")
state.remove("iriscmd")
state.remove("rssi")
state.remove("pushed")
    
device.deleteCurrentState("alarm")    
device.deleteCurrentState("pushed") 
device.deleteCurrentState("batteryVoltageTest")     

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
	int checkEveryHours = 10 // Request a ranging report and refresh every x hours.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)
    // At X seconds past X minute, every checkEveryHours hours, starting at Y hour.

	// Schedule the presence check.
	int checkEveryMinutes = 50 // Check presence timestamp every 6 minutes.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)// At X seconds past the minute, every checkEveryMinutes minutes.

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
//	sendEvent(name: "${save}", value: "")
//	sendEvent(name: "${save}n",value: "")
        
//    removeDataValue("${save}")
//    removeDataValue("${save}n")  
// This is the worst documented language I have ever seen.
// Had to find this in a post.        
    device.deleteCurrentState("${save}")    
    device.deleteCurrentState("${save}n")
        
//    device.removeSetting("${save}")   
//	device.removeSetting("${save}n")  
	}	
	
}

    
    
    

// unsupported error matrix
def getCodes(){     logging("${device} : getCodes  unsupported", "info")             }
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

def panic() {
	logging ("${device} : Panic Sent","warn")
    sendEvent(name: "panic", value: "on", displayed: true, isStateChange: true, isPhysical: true)
//  siren()
    state.Panic = true 
}

//  You only get here by authorized PIN
def disarm() {
	logging ("${device} : Sending disarm (OFF: Panic/alarm/siren)", "info")
	sendEvent(name: "securityKeypad", value: "disarmed", descriptionText: "cancled by PIN", displayed: true)
    sendEvent(name: "panic",  value: "off", descriptionText: "cancled by PIN", displayed: true)
//    sendEvent(name: "strobe", value: "off", displayed: true)
//    sendEvent(name: "alarm",  value: "off", displayed: true) 
	sendLocationEvent (name: "hsmSetArm", value: "disarm")
    state.Command = "off"
    state.Panic = false
}

def siren(cmd){
  logging ("${device} : Siren ON", "info")
  sendEvent(name: "siren", value: "on", displayed: true) 
  sendEvent(name: "alarm", value: "on", displayed: true) 
  
}
def strobe(cmd){
  logging ("${device} : Strobe ON","info")  
  sendEvent(name: "strobe", value: "on", displayed: true)  
// This does nothing but set flag    
}
def both(cmd){
  logging ("${device} : both ON siren/strobe","info")
  sendEvent(name: "siren", value: "on")  
  sendEvent(name: "strobe", value: "on", descriptionText: "not supported yet", displayed: true) 
  sendEvent(name: "alarm", value: "on") 
}

def on(cmd) {
 logging ("${device} :Switch ON","info")   
 sendEvent(name: "switch", value: "on") 
state.switch = on
}
def off(cmd){
 logging ("${device} : Switch OFF","info")
 sendEvent(name: "switch", value: "off")   
state.switch = off
    
//sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0 ${device.endpointId} 0xFC04 {15 4E 10 00 00 00}"])

sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 0 ${device.endpointId} 0x0501 {09 01 04 05 09 01 01}"])
 
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
    lqi = device.currentValue(lqi)
    logging ("${device} : Mode: Ranging LQI:${lqi}","info")
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
            state.Command = "night"
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
            state.Panic = false
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
		logging("${device} : Presence : Battery too low!", "warn")
	} else {
		logging("${device} : Presence : Not yet received.", "warn")
	}
}


def parse(String description) {
	// Primary parse routine.
	// catchall: C216 00C0 02 02 0040 00 1E00 00 00 0000 00 01 2000 <--- spams this 
//logging ("${device} : Parsing - - -","debug")
    // We check stat first and debounce ARM buttons if it took.
    // Keyboard spams cmd about 6 times. So we autodebounce and resend.
    clientVersion()
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
	logging ("${device} : ${map}","trace")
	String[] receivedData = map.data	// AlertMe values are always sent in a data element.
    logging("${device} : Cluster:${map.clusterId} State:${map.command} MAP:${map.data}","trace")
    logging("${device} : Cluster:${map.clusterId} - -${map.command} -","debug")
/*
Internal notes: Building Cluster map 
* = likely done by HUB in Join.
0000 Network (16-bit) Address Request *
0004 Simple Descriptor Request *
0005 Active Endpoint Request *
0006 Match Descriptor Request (Light Flashes)
0013 Device announce message (Light Flashes)
00C0 Button report (button on repeator)
     00 = Unknown (lots of reports)
     0A = Button
00EE Relay actuation (smartPlugs)
     80 = PowerState
00EF Power Energy messages
     81 = Power Reading
     82 = Energy
00F0 Battery & Temp
     FB 
00F3 Key Fob (button on Repeator pressed)
00F2 Tamper
00F6 Discovery Cluster
     FD = Ranging
     FE = Device version response.
0500 Security Cluster (Tamper & Reed)
8001 Routing Neighobor information
8004 simple descriptor response
8005 Active Endpoint Response (tells you what the device can do)
8032 Received when new devices join
8038 Management Network Update Request

*/        
   
    if (map.clusterID == "0013"){
	logging("${device} : Device announce message","warn")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Anouncing New Device","debug")
	

    } else if (map.clusterId == "0006") {
		logging("${device} : Sending Match Descriptor Response","debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])

    } else if (map.clusterId == "00C0") {
     // Iris Button report cluster 
     //  mapcommand 00 spams us so we ignore it even in trace . Unknown
     if (map.command != "00"){logging ("${device} : key Cluster CMD:${map.command} MAP:${map.data}","trace") }

     if (map.command == "0A") {  
      keyRec   = receivedData[4]    
	  buttonNumber = 0 
	  size = receivedData.size()     
      logging ("${device} : Keypad #${keyRec} size:${size} State:${state.Command} Panic:${state.Panic}","debug")

      if (size == 10){ // IRIS MODE commands show up here
	   rawCMD = receivedData[4..9].collect{ (char)Integer.parseInt(it, 16) }.join()
       irsCMD = receivedData[4..4].collect{ (char)Integer.parseInt(it, 16) }.join() 
       irsCMD = receivedData[6..6].collect{ (char)Integer.parseInt(it, 16) }.join() 
          //Iris had only 2 armed modes night and away HOME was disarmed
          //this is for logging only we are not folowing these
          if (irsCMD == "H") {irsCMD= "HOME"}
          if (irsCMD == "A") {irsCMD= "AWAY"}
          if (irsCMD == "N") {irsCMD= "NIGHT"}
          if (irsCMD == "P") {irsCMD= "PANIC"}
          // Sencond field. Unknown why 2 command are sent
           irsNCMD = receivedData[4..4].collect{ (char)Integer.parseInt(it, 16) }.join() 
          if (irsNCMD == "H") {irsNCMD= "HOME"}
          if (irsNCMD == "A") {irsNCMD= "AWAY"}
          if (irsNCMD == "N") {irsNCMD= "NIGHT"}
          if (irsNCMD == "P") {irsNCMD= "PANIC"}
          if (irsCMD != irsNCMD){ irsCMD= "${irsCMD} ${irsNCMD}"}// sometimes 2 commands are sent H A
          logging("${device} : Received: Iris command:${irsCMD}", "info")
          state.iriscmd = irsCMD // Store this its likely to be out of sync with actual command
	      return
	 }    
     
     // Now check for our command buttons  
      if (keyRec == "41"){
           if (state.Command =="away"){ 
             logging("${device} : Button ON","debug")
             return }
             logging("${device} : Button ON","info")
         armAway()
	 } 	     
      if (keyRec == "48"){
         if (state.switch =="off"){
             logging("${device} : Button OFF","debug")
             return
         }
         logging("${device} : Button OFF","debug")
         if (switchByOFF){off()}
         return
	 } 
      if (keyRec == "2A"){ 
		 if (state.switch =="on"){
         logging("${device} : Button *","debug")
         return }
         logging("${device} : Button *","debug")
         if (switchByOFF){ on()}
	 } 
  
         
     if (keyRec == "23"){
		 if (state.Command =="home"){
         logging("${device} : Button #","debug")
         return }
         logging("${device} : Button #","info")
         if (poundActive){armHome()}
	 } 
     if (keyRec == "4E"){
		 if (state.Command =="night"){
         logging("${device} : Button PARTIAL","debug")  
         return }
         logging("${device} : Button PARTIAL","info")  
		 armNight()
	 }       
     if (keyRec == "50"){
         if (state.Panic){ 
             logging("${device} : Button PANIC","debug") 
             return }    
		 panic()
         
   	  }
          
// Look for PINS stored in long fields
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

     if (size == 9){ 
	 asciiPin = receivedData[4..8].collect{ (char)Integer.parseInt(it, 16) }.join()
     logging("${device} : Received [${asciiPin}]", "warn")
     if(tamperPIN){tamper()}   
     return    
	 }         
	 if (size == 6){
	 asciiPin = receivedData[4..5].collect{ (char)Integer.parseInt(it, 16) }.join()
	 logging("${device} : Received [${asciiPin}]" , "warn")
     if(tamperPIN){tamper()}   
     return    
	 }
	 if (size == 7){
	 asciiPin = receivedData[4..6].collect{ (char)Integer.parseInt(it, 16) }.join()
     logging("${device} :Received [${asciiPin}]" , "warn")
     if(tamperPIN){tamper()}   
     return    
	 }	
         
// 4 digit PIN decoding         
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
         
      logging("${device} : PIN ${asciiPin} Invalid PIN HACKING" , "warn")
      if(tamperPIN){tamper()}   
      return	 
	 }// end pin code 
         

// Keypad button matrix 
// If a key is pressed once it acts like a button not a PIN
// Each key is mapped to a bitton you can use in a routine         
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
      inspect = receivedData[1..3].reverse().join()
      inspect2 = zigbee.convertHexToInt(inspect) // Unknown Counter Counts up or down
      batteryVoltageHex = receivedData[5..6].reverse().join()
//                  temp  = receivedData[7..8].reverse().join() 
//                  temp = zigbee.convertHexToInt(temp)
      if (batteryVoltageHex == "FFFF") {return}
      batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
      batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)
//          count           volt    temp
//     00, 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12
//MAP:[19, D9, F1, 3F, 02, 6C, 09, 00, 00, B2, FF, 00, 00]
//MAP:[19, B8, B1, 43, 02, 56, 09, 00, 00, BA, FF, 00, 00]        
        logging("${device} :count ${inspect2} Volts:${batteryVoltage}", "debug") 
// Being A Electronics technician I base this on Battery discharge curves.
// I also verified the sensor the voltage reported is close to my meter.	    
// Normal batteries slowely drop. Newer ones are steady and then drop dead.
// To detect them before they drop dead the lower voltage needs to be higher
// Problem is not all curve maps agree so this is a adverage	    
//	    if (BatType == "Alkaline"){// < slow discharge 
		BigDecimal batteryVoltageScaleMin = 2.10
		BigDecimal batteryVoltageScaleMax = 3.00	    
//	    } 	    
	    if (BatType == "NiCad"){ // <1.2 drops out fast
		batteryVoltageScaleMin = 2.25
		batteryVoltageScaleMax = 3.00	    
	    } 
            if (BatType == "NiMH"){ // <1.2 drops out fast
		batteryVoltageScaleMin = 2.25
		batteryVoltageScaleMax = 3.00	    
	    }    

	    if (BatType == "Lithium"){// <1.25 drops out fast 
		batteryVoltageScaleMin = 2.35
		batteryVoltageScaleMax = 3.00	    
	    } 	    
	    
//	 logging( "${device} : Battery : ${BatType} ${batteryVoltageScaleMin}% (${batteryVoltageScaleMax} )","info")	    
	    
//          batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
	    
     	BigDecimal batteryPercentage = 0
            batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage

        
        if (state.lastBattery != battertVoltage){
	 logging( "${device} : Battery : ${BatType} ${batteryPercentage}% (${batteryVoltage} V)","info")
	 sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
     	 sendEvent(name: "battery", value:batteryPercentage, unit: "%")
         
         
         if (batteryPercentage > 20) {  
             sendEvent(name: "batteryState", value: "ok")
             state.batteryOkay = true
             }
            
         if (batteryPercentage < 21) {
             logging("${device} : Battery LOW : $batteryPercentage%", "debug")
             sendEvent(name: "batteryState", value: "low")
             state.batteryOkay = true
         }
  
	 if (batteryPercentage < 19) {
            logging("${device} : Battery BAD: $batteryPercentage%", "debug") 
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
	String deviceManufacturer = "IRIS/Everspring"
	String deviceModel = ""
	String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
    reportFirm = "Report to DEV"
      if(deviceFirmware == "2012-12-11" ){reportFirm = "Ok"}
      if(deviceFirmware == "2013-06-28" ){reportFirm = "Ok"}
      
	// Sometimes the model name contains spaces.
	if (versionInfoBlockCount == 2) {
	deviceModel = versionInfoBlocks[0]
	} else {
	deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
	}
      logging("${device} : ${deviceModel} Firmware :[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "info")
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
} else if (map.clusterId == "8001") {
  // Never seen keyboard do this
  logging("${device} : Routing and Neighbour Information", "info")	     
     
        
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
