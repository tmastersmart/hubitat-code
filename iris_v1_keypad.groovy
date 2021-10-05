/* Iris v1 KeyPad Driver
Hubitat Iris v1 KeyPad driver 
Supports keypad disarm arm functions 
Works with Lock Code Manager 

  _____ _____  _____  _____        __    _  __                          _ 
 |_   _|  __ \|_   _|/ ____|      /_ |  | |/ /                         | |
   | | | |__) | | | | (___   __   _| |  | ' / ___ _   _ _ __   __ _  __| |
   | | |  _  /  | |  \___ \  \ \ / / |  |  < / _ \ | | | '_ \ / _` |/ _` |
  _| |_| | \ \ _| |_ ____) |  \ V /| |  | . \  __/ |_| | |_) | (_| | (_| |
 |_____|_|  \_\_____|_____/    \_/ |_|  |_|\_\___|\__, | .__/ \__,_|\__,_|
                                                   __/ | |                
                                                  |___/|_|   

=================================================================================================
*   v2.7 10/05/2021 Bat Bug fixed. Arm with Pin added. Unlock with pin and OFF added.
                    Panic sets custom panic flag. 
*   v2.6 10/02/2021 Added DisarmedBy command, Settings to remap Command Buttons
*   v2.5 10/02/2021 Config for tamper,Log debug cleanup,Remove alarm no sounds
*   v2.4 09/30/2021 Custom Panic command added. Added Config options for * # OFF
*   v2.3 09/30/2021 battery value changes
*   v2.2 09/29/2021 Version detection and auto upgrade/install. 
*   v2.1 09/29/2021 Tamper bugs fixed, Log fix,Old IRIS command found and Logged, Master pin added 
*   v2.0 09/28/2021 Keypad support debugged , Commands debounced, Logging cleaned up, 
                    Invalid pins trapped Star key sends a 6 digit PIN *#  * Now trapped.
*   v1.1 09/27/2021 Cleanup Button controler is working
*   v1.0 09/27/2021 Beta test version Buttons now reporting Bat working

* Minor versions are for internal testing and are not listed here.
=================================================================================================
Arming Buttons setup on driver page

Disarming
Enter PIN and press OFF

Arming
Can be set to require PIN or not
ActionButtons can be remaped.


Button Support
If a key is pressed once it acts like a button not a PIN
All keypad number buttons mapped to 10 push buttons.


Tamper
Invalid PIN will press tamper

Passcodes
Lock Manager can store monitor and delete passcode but not recall
MASTER 7 digit pin 

Chimes Lights not working. Help is needed on this. 





Total worktime to build up to v2 2 days. Have fun.
 
FCC ID:FU5TSA04 https://fccid.io/FU5TSA04
Built by Everspring Industry Co Ltd Smart Keypad TSA04            

I would use the sample keypad code hubitat posted 
but its not open source so I wrote my own.
I wrote this for my keyboards you are welcome to use it.
================================================================================================
To Reset for paring:
Remove batteries (if already powered up.)

Insert two batteries side-by-side at one end or the other

then press "ON" button device 5 times within the first 10 seconds.

the On button will begin to blink twice periodically.

Tested on 
2013-06-28
2012-12-11

https://github.com/tmastersmart/hubitat-code/blob/main/iris_v1_keypad.groovy
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_v1_keypad.groovy

Post your comments here. 
http://www.winnfreenet.com/wp/2021/09/iris-v1-keyboard-driver-for-hubitat/


* See opensource IRIS code at. The orginal v1 code is in a zigbee driver but they
have no source code for it and are going to replace it with a new zigbee driver.
If anyone can decompile the bin let me know.

"keypad:enabledSounds": ["ARMED", "ARMING", "SOAKING", "ALERTING", "DISARMED", "BUTTONS"],

* To Reset Device:
 *    Insert battery and then press "ON" button device 5 times within the first 10 seconds.
 * 
 * Keypad is device type 28 (0x1C)
 * Most messages are sent and received on the Attribute Cluster (0x00C0).
 * The standard device messages (Hello and Lifesign) are sent on the Join and General Clusters, as usual.
 * The lifesign will be sent every 2 minutes, in common with other AlertMe sleepy end devices.
 * 
 * The keypad is responsible for;
 *   1. Driving its LEDs according to its state (see ATTRID_KEYPADSTATE attribute below),
 *   2. Accumulating a PIN
 *   3. Sending an action key and/or PIN when appropriate
 *   4. Making sound sequences on demand
 * 
 * The keypad expects to be told its state, and may also send a triplet of attributes whenever an "action" key is used.
 * The triplet is ATTRID_PIN (if there is one), ATTRID_ACTIONKEY_ID and ATTRID_ACTIONKEY_TIME.
 * 
 * While an actionKey is held down, the keypad will send ATTRID_ACTIONKEY_ID and ATTRID_ACTIONKEY_TIME once per second.
 * It’ll also send an ATTRID_PIN (if available) with the first ATTRID_ACTIONKEY_ID.
 * 
 * If a PIN has been typed in, but no action key pressed within 2 seconds of the last digit, then a single ATTRID_PIN
 * will be sent to the hub.
 * 

https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_AlertMe_KeyPad_2_4.driver
https://github.com/arcus-smart-home/arcusweb/blob/c8f30cef8d59c94a3be83fe3d3c7bfa5c151a091/src/models/capability/KeyPad.js
https://github.com/arcus-smart-home
https://github.com/arcus-smart-home/arcushubos/tree/master/meta-iris
https://github.com/arcus-smart-home/arcushubos/tree/master/meta-iris/recipes-core/iris-utils/files

I have been unable to find any iris v1 code in it but it does have some drivers. 
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
    TheVersion="2.7"
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
capability "PushableButton"
capability "TamperAlert"
capability "Switch"

//capability "Chime"
//capability "Alarm"


command "checkPresence"
command "normalMode"
command "rangingMode"
//command "sendHex"
//command "quietMode"

attribute "batteryState", "string"
attribute "lastCodeName", "string"
attribute "batteryVoltage", "string"	
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

	input name: "tamperPIN",   type: "bool", title: "Press Tamper on BAD PIN", defaultValue: true
  
    input name: "requirePIN",   type: "bool", title: "Require Valid PIN to ARM", defaultValue: false, required: true


    input name: "OnSet",   type: "enum", title: "ON Button", description: "Customize ON Button", options: ["Arm Home", "Arm Away"], defaultValue: "Arm Away",required: true 
    input name: "PartSet", type: "enum", title: "Partial Button", description: "Customize Partial Button",  options: ["Arm Night", "Arm Home"], defaultValue: "Arm Night",required: true 
    input name: "PoundSet",type: "enum", title: "# Button", description: "Customize Pound Button",  options: ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Home",required: true 
    input name: "StarSet" ,type: "enum", title: "* Button", description: "Customize Star Button",  options:  ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Disabled",required: true 

    input name: "BatType", type: "enum", title: "Battery Type", options: ["Lithium", "Alkaline", "NiMH", "NiCad"], defaultValue: "Alkaline" 


    input("secure",  "text", title: "7 digit password", description: "A Master 7 digit secure PIN. Seperate from Lock Code Manager 0=disable",defaultValue: 0,required: false)

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
state.validPIN = false
state.PinName = "none"
state.PIN = "none"
state.logo ="<img src='https://github.com/tmastersmart/hubitat-code/blob/main/images/iris-keypad.jpg?raw=true' >"
    
//sendEvent(name: "battery",value:100, unit: "%", isStateChange: false)
//sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
//sendEvent(name: "lqi", value: 0, isStateChange: false)
sendEvent(name: "operation", value: "normal", isStateChange: false)
sendEvent(name: "presence", value: "present", isStateChange: false)
sendEvent(name: "numberOfButtons", value: "10", isStateChange: false)
sendEvent(name: "panic", value: "off", isStateChange: false)    
sendEvent(name: "maxCodes", value:5)
sendEvent(name: "codeLength", value:4)
sendEvent(name: "securityKeypad", value: "Fetching")
sendEvent(name: "tamper", value: "clear")

state.remove("switch")	
state.remove("uptime")
state.remove("uptimeReceived")
state.remove("iriscmd")
state.remove("rssi")
state.remove("pushed")
state.remove("state.reportToDev")
state.remove("message")
    
  	
device.deleteCurrentState("alarm")    
device.deleteCurrentState("pushed") 
device.deleteCurrentState("pin")     
device.deleteCurrentState("lockCodes") 
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
def getCodes(){  
   logging("${device} : getCodes  ", "info")
    
}
def setEntryDelay(code){logging("${device} : setEntryDelay ${code}  unsupported", "info")}
def setExitDelay(code){	logging("${device} : setExitDelay  ${code}  unsupported", "info")}
def setCodeLength(code){logging("${device} : setCodeLength 4", "info")                   }



def armAway() {
	logging ("${device} : Sending armAWAY by ${state.PinName}","info")
    sendEvent(name: "securityKeypad",value: "armed away",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "armAway")
    state.Command = "away"
//    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {22 00 09 18 48 23 00 09 1A 48} {0xC216}"])

}
def armHome() {
	logging ("${device} : Sending armHome by ${state.PinName}","info")
	sendEvent(name: "securityKeypad",value: "armed home",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "armHome")
    state.Command = "home"
///sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {22 00 09 18 48 23 00 09 1A 48} {0xC216}"])   

}
def armNight() {
	logging ("${device} : Sending armNight by ${state.PinName}","info")
	sendEvent(name: "securityKeypad",value: "armed night",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "armNight")
    state.Command = "night"
//sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {22 00 09 18 48 23 00 09 1A 48} {0xC216}"]) 

}

def panic() {
	logging ("${device} : Panic Sent","warn")
    sendEvent(name: "panic", value: "on", displayed: true, isStateChange: true, isPhysical: true)
//  siren()
    state.Panic = true 
//  sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {22 00 09 18 48 23 00 09 1A 48} {0xC216}"])  
}

//  You only get here by authorized PIN
def disarm() {
    if (state.validPIN == false){logging ("${device} : PIN ERROR Bug detected. Report to DEV", "warn")}    
	sendEvent(name: "securityKeypad", value: "disarmed", descriptionText: "Disarmed by ${state.PinName}", displayed: true,data: /{"-1":{"name":"${state.PinName}","code":"${state.PIN}","isInitiator":true}}/)
	sendEvent(name: "lastCodeName", value: "${state.PinName}", descriptionText: "Disarmed by ${state.PinName}")
 
    logging ("${device} : Sent Disarmed by ${state.PinName}", "info")

    if (state.Panic){
        sendEvent(name: "panic",  value: "off", descriptionText: "cancled by ${state.PinName} PIN", displayed: true)
        state.Panic = false
    }
//    sendEvent(name: "strobe", value: "off", displayed: true)
//    sendEvent(name: "alarm",  value: "off", displayed: true) 
	sendLocationEvent (name: "hsmSetArm", value: "disarm")
    state.Command = "off"
  
}

def purgePIN(){
if (state.validPIN){logging ("${device} : PIN Removed from state memory", "info")}
state.validPIN = false
state.PinName = "none"
state.PIN = "NA"    
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
state.switch = true

}



def off(cmd){
 logging ("${device} : Switch OFF","info")
 sendEvent(name: "switch", value: "off")   
state.switch = false
}

def tamper(){
sendEvent(name: "tamper", value: "detected")
logging ("${device} : Tamper Detected","info")
 pauseExecution(6000)
logging ("${device} : Tamper Clear","info")    
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

// Get HSM status And update our state if its changed
def getStatus(status) {
    status = location.hsmStatus
    logging ("${device} : Received HSM ${status} Our state:${state.Command}","trace")
// HUB armedAway, armingAway, armedHome, armingHome, armedNight, armingNight, disarmed, allDisarmed
    if (status == "armedAway"){  
        if (state.Command != "away"){
            sendEvent(name: "securityKeypad", value: "armed away")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "away"
        }
     return
    }
    if (status == "armingAway"){ 
        if (state.Command != "away"){
          sendEvent(name: "securityKeypad", value: "armed away")
            logging ("${device} : Received HSM ${status}","info")
          state.Command = "away"
        }
      return 
    }
    
    if (status == "armedHome"){
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armed home")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "home"
        }
        return
       }
    if (status == "armingHome"){ 
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armed home")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "home"
        }
        return
       }  
    
    if (status == "armedNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad", value: "armed night")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "night"
        }
        return
       }

    if (status == "armingNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad", value: "armed night")
            logging ("${device} : Received HSM  ${status}","info")
            state.Command = "night"
        }
        return
       } 
    
    if (status == "disarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad", value: "disarmed")
            state.Command = "off"
            state.Panic = false
            logging ("${device} : Received HSM ${status}","info")
        }
        return
    }
    if (status == "allDisarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad", value: "all disarmed")
            logging ("${device} : Received HSM ${status}","info")
            state.Panic = false
            state.Command = "off"
        }
        return
    } 
    logging ("${device} : Received HSM ${status} INVALID Unable to decode. Our state:${state.Command}","warn")
}

void refresh() {
    logging ("${device} : Refresh. Sending Hello to Device","info")
// send a "Hello" message, to get version, etc.   
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])	
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
	 logging ( "${device} :Present: ${secondsElapsed} seconds ago.","info")
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
	String[] receivedData = map.data	
    logging("${device} : Cluster:${map.clusterId} State:${map.command} MAP:${map.data}","trace")
    logging("${device} : Cluster:${map.clusterId} ${map.command} ","debug")
/*
Internal notes: Building Cluster map 
* = likely done by HUB in Join.
0000 Network (16-bit) Address Request *
0004 Simple Descriptor Request *
0005 Active Endpoint Request *
0006 Match Descriptor Request (Light Flashes)
0013 Device announce message (Light Flashes)
00C0 Button report (button on repeator)
     00 = Unknown (Lifeline report)
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
 
//   if (map.command != "00"){logging ("${device} : key Cluster CMD:${map.command} MAP:${map.data}","trace") }
     
     if (map.command == "01") {
         // Reply to our sending (undocumented in iris code)
         you  = receivedData[1]
         me   = receivedData[3]
         st = "-${reply}-"  
         if (me == "30") {st="ok"}
         if (me == "86") {st="error"}
    
         logging ("${device} : Received: ${you} My Reply ${st} data:${map.data}","trace")
     }
     
                               
     if (map.command == "00" ) {
         // Lifeline status report
         logging ("${device} : Lifeline:  raw data:${map.data}","trace") 
     }  
        
     if (map.command == "0A") {  
      PinEnclosed = receivedData[0]// The command being passed to us
      pinSize     = receivedData[3]// The PIN size     
      keyRec      = receivedData[4]// The Key pressed    
	  buttonNumber = 0 
      status = "Unknown"   
	  size = receivedData.size()// size of data field
         // Action matrix based in iris source code.
         // Create text for logging
         if (PinEnclosed  =="20" ){   status = "STATE" }
         if (PinEnclosed  =="21" ){   status = "PIN" }
         if (PinEnclosed  =="22" ){   status = "Pressed" }
         if (PinEnclosed  =="23" ){   status = "Released" }
         if (PinEnclosed  =="24" ){   status = "Poll Rate" }
         if (PinEnclosed  =="25" ){   status = "sound mask" }
         if (PinEnclosed  =="26" ){   status = "Sound ID" }
         if (PinEnclosed  =="28" ){   status = "Error" } 
         if (keyRec == "48" ){keyRecA ="OFF"}
         if (keyRec == "41" ){keyRecA ="ON"}
         if (keyRec == "4E" ){keyRecA ="PARTIAL"}          
         if (keyRec == "50" ){keyRecA ="PANIC"}            
         if (keyRec == "2A" ){keyRecA ="STAR"}
         if (keyRec == "23" ){keyRecA ="POUND"}
    
         
         
         if(debugLogging){logging ("${device} : Keypad #${keyRec} Action:${status} State:${state.Command} Panic:${state.Panic} PIN Valid:${state.validPIN} State:${state.Command}","debug")}
         else {           logging ("${device} : Action :[${status}  ${keyRecA}]  Panic:${state.Panic} PIN Valid:${state.validPIN} State:${state.Command}","info")}
      if (size == 10){ // IRIS MODE commands show up here
       hexcmd = receivedData[4..9]  
       rawCMD = receivedData[4..9].collect{ (char)Integer.parseInt(it, 16) }.join()
       irsCMD = receivedData[4..4].collect{ (char)Integer.parseInt(it, 16) }.join() 
   nextirsCMD = receivedData[9..9].collect{ (char)Integer.parseInt(it, 16) }.join() 
          // Iris had only 2 armed modes night and away. * and # had no function
          if (irsCMD == "H") {
              irsCMD1= "HOME"
              keyRec = "48"// Press OFF
          }
          if (irsCMD == "A") {
              irsCMD1= "AWAY"
              keyRec = "41"// Press ON
          }
          if (irsCMD == "N") {
              irsCMD1= "NIGHT"
              keyRec = "4E"// Press Part
          }
          if (irsCMD == "P") {
              irsCMD1= "PANIC"
              keyRec = "50"// Press Panic
          }
          if (irsCMD == "*") {
              irsCMD1= "_*_"
              keyRec = "2A"// Press *
          }
          if (irsCMD == "#") {
              irsCMD1= "_#_"
              keyRec = "23"// Press #
          }

          
          if ( irsCMD == nextirsCMD){logging("${device} : Received: Iris command:${irsCMD1} Valid PIN ${state.validPIN}", "info")}
          else{ logging("${device} : Received: Iris command:${irsCMD1}. Next command in qwery ${nextirsCMD}.   Valid PIN ${state.validPIN}", "info")}
          logging("${device} : #${keyRec} Action:${status} Iris cmd:${rawCMD} :${hexcmd}", "trace")
          state.iriscmd = irsCMD1 // store for later use
	      
	 }    

// Now check for our command buttons          
//    "OnSet"   ["Arm Home",  "Arm Away"], defaultValue: "Arm Away"
      if (keyRec == "41"){
          if (OnSet == "Arm Away"){
           if (state.Command =="away"){ 
             logging("${device} : Button ON ${OnSet} (But state already sent)","debug")
             return }
             logging("${device} : Button ON ${OnSet} Valid PIN ${state.validPIN}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armAway()
               return
               }
              }
              else{
              state.PinName = "Not Required"  
              armAway()
              return
              }
              
          }
          
           if (OnSet == "Arm Home"){
           if (state.Command =="home"){ 
             logging("${device} : Button ON ${OnSet} (But state already sent)","debug")
             return }
             logging("${device} : Button ON ${OnSet} Valid PIN ${state.validPIN}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armHome()
               return
               }
              }
              else
              state.PinName = "Not Required"    
              armHome()
              return
              } 
          }
	 
//    "PartSet" ["Arm Night", "Arm Home"], defaultValue: "Arm Night"         

     if (keyRec == "4E"){
         if (PartSet =="Arm Night"){          
		  if (state.Command =="night"){
          logging("${device} : Button PARTIAL ${PartSet} (But state already sent)","debug")  
          return }
          logging("${device} : Button PARTIAL ${PartSet} Valid PIN ${state.validPIN}","info")  
              if (requirePIN){
               if (state.validPIN == true){           
               armNight()
               return
               }
              }
              else{
              state.PinName = "Not Required"
              armNight()
              return
              } 
         }
          if (PartSet =="Arm Home"){          
		  if (state.Command =="home"){
          logging("${device} : Button PARTIAL ${PartSet} (But state already sent)","debug")  
          return }
          logging("${device} : Button PARTIAL ${PartSet} Valid PIN ${state.validPIN}","info")  
              if (requirePIN){
               if (state.validPIN == true){           
               armAway()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armAway()
              return
              } 
         }   
         
         
	 }          
         
// OFF Disarm Command Valid PIN required        
       if (keyRec == "48"){
           if (state.Command == "off"){
             logging("${device} : Button OFF (But state already sent) state${state.Command}","debug")
             return
             }
         if (state.validPIN == true){  
            logging("${device} : Button OFF Valid PIN: ${state.validPIN} State: ${state.Command}","info")
            disarm()
         return
         }
         logging("${device} : Button OFF Valid PIN: ${state.validPIN} State: ${state.Command}","info")
         return  
	 }         
         
         


 //     "StarSet" ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Home"
         
     if (keyRec == "2A"){
      if (StarSet == "Arm Home"){
		 if (state.Command =="home"){
         logging("${device} : Button # ${StarSet} (But state already sent) state${state.Command}","debug")
         return }
         logging("${device} : Button # ${StarSet}  ${state.validPIN}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armHome()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armHome()
              return
              }    
         }
       if (StarSet == "Arm Night"){
		 if (state.Command =="night"){
         logging("${device} : Button # ${StarSet} (But state already sent) state${state.Command}","debug")
         return }
         logging("${device} : Button # ${StarSet}  ${state.validPIN}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armNight()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armNight()
              return
              }    
         } 
        if (StarSet == "Arm Away"){
		 if (state.Command =="away"){
         logging("${device} : Button # ${StarSet} (But state already sent) state${state.Command}","debug")
         return }
         logging("${device} : Button # ${StarSet} ${state.validPIN}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armAway()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armAway()
              return
              }    
         }     
     
     logging("${device} : Button # Star ${StarSet} ERROR NOT SETUP","debug")
     return
     }        
        
        
        
        
        
        
        
        
        
        
        
//     "PoundSet" ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Home"
         
     if (keyRec == "23"){
      if (PoundSet == "Arm Home"){
		 if (state.Command =="home"){
             logging("${device} : Button # ${PoundSet}","debug")
         return }
         logging("${device} : Button # ${PoundSet}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armHome()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armHome()
              return
              }    
         }
       if (PoundSet == "Arm Night"){
		 if (state.Command =="night"){
         logging("${device} : Button # ${PoundSet}","debug")
         return }
         logging("${device} : Button # ${PoundSet}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armNight()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armNight()
              return
              }   
         } 
        if (PoundSet == "Arm Away"){
		 if (state.Command =="away"){
         logging("${device} : Button # ${PoundSet}","debug")
         return }
         logging("${device} : Button # ${PoundSet}","info")
              if (requirePIN){
               if (state.validPIN == true){           
               armAway()
               return
               }
              }
              else{
              state.PinName = "Not Required"    
              armAway()
              return
              }   
         }     
     
     logging("${device} : Button # ${PoundSet}","debug")
     return
     } 
   
     if (keyRec == "50"){
         if (state.Panic){ 
             logging("${device} : Button PANIC but already sent","debug") 
             return }    
		 panic()
         logging("${device} : Button *** PANIC ***","info") 
         return
   	  }
          

//      PinEnclosed = receivedData[0]// 21 = pin
//      pinSize     = receivedData[3]// The PIN size + 4 = size   
    if (PinEnclosed == "21" ){ 
        state.validPIN = false
        state.PinName = "NA"
        state.PIN     = "NA"
        asciiPin = "NA"
        logging("${device} : Data Size:${size} pinSize${pinSize}" , "debug")
        if (size == 8) {asciiPin = receivedData[4..7].collect{ (char)Integer.parseInt(it, 16) }.join()}
        if (size == 9) {asciiPin = receivedData[4..8].collect{ (char)Integer.parseInt(it, 16) }.join()}
        if (size == 10){asciiPin = receivedData[4..9].collect{ (char)Integer.parseInt(it, 16) }.join()}
        if (size == 11){asciiPin = receivedData[4..10].collect{ (char)Integer.parseInt(it, 16) }.join()}
//      sendEvent(name: "PIN", value: asciiPin)
 
      if (device.currentValue("code1") == asciiPin){
          name = device.currentValue("code1n")
          state.validPIN = true    
	   }	     
      if (device.currentValue("code2") == asciiPin){
          name = device.currentValue("code2n")
      	  state.validPIN = true
      }
      if (device.currentValue("code3") == asciiPin){
          name = device.currentValue("code3n")
          state.validPIN = true
      }
	  if (device.currentValue("code4") == asciiPin){
          name = device.currentValue("code4n")
          state.validPIN = true
      }
      if (device.currentValue("code5") == asciiPin){
          name = device.currentValue("code5n")
          state.validPIN = true
      }  
      if (secure == asciiPin){
          name ="master"
          state.validPIN = true
      }  

        if (state.validPIN == true){
          state.PinName = name
          state.PIN     = asciiPin  
          logging("${device} : Valid Pin Detected ${name} ${asciiPin}","info")
          runIn(60, "purgePIN")

     	  return  
        }   
         
      logging("${device} : PIN ${asciiPin} Invalid PIN HACKING" , "warn")
      if(tamperPIN){tamper()}
      state.validPIN = false
      state.PinName = TAMPER
      state.PIN     = asciiPin   
      return	 
	  
    }// end pin check       

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
//         if (keyRec == "48"){press(11)}// OFF button mapped to 11
 }// end of 0A

    
        
    } else if (map.clusterId == "00F0") {
      // AlertMe General Cluster 
      if (map.command == "FB") { 
    // if bit 0 battery voltage // bit 5 and 6 reversed
    // if bit 1 temp // bit 7 and 8 reversed
    // if bit 8 lqi // LQI = 10 (lqi * 100.0) / 255.0
    
       batRec = receivedData[0]// [19] This sould be set with bat data but doesnt follow standard
      tempRec = receivedData[1]// does not folow alertme standard 1 2 3 are actualy running a timmer up and down
    switchRec = receivedData[4]
       lqiRec = receivedData[8]// 
      //if (lqiRec){ lqi = receivedData[10]}
      def batteryVoltageHex = "undefined"
      BigDecimal batteryVoltage = 0
      inspect = receivedData[1..3].reverse().join()
      inspect2 = zigbee.convertHexToInt(inspect) // Unknown Counter Counts up or down
      batteryVoltageHex = receivedData[5..6].reverse().join()
//      if (tempRec){
//          temp  = receivedData[7..8].reverse().join()
//          temp = zigbee.convertHexToInt(temp)
//      }
      if (batteryVoltageHex == "FFFF") {return}
//      if (batRec){ 
     batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
     batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)
     logging("${device} Raw Battery  Bat:${batRec} ${batteryVoltage}", "debug")    
 
// I base this on Battery discharge curves.
// Iris source code says 2.1 is min voltage   
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

        
        if (state.lastBattery != batteryVoltage){
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
  
	 if (batteryPercentage < 5) {
            logging("${device} : Battery BAD: $batteryPercentage%", "debug") 
	    state.batteryOkay = false
	    sendEvent(name: "batteryState", value: "exhausted")
	}
        state.lastBattery = batteryVoltage     
    }
   //}// end valid bat report
  }// end FB
        else {
        // There are other known commands
        // F0 FD FA 80 83 00 01 02    
        reportToDev(map)
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
        reportFirm = "unknown"
      if(deviceFirmware == "2012-12-11" ){reportFirm = "Ok"}
      if(deviceFirmware == "2013-06-28" ){reportFirm = "Ok"}
	if(reportFirm == "unknown"){state.reportToDev="Report Unknown version [${deviceModel}] [${deviceFirmware}] " }
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
