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
  v3.3   10/12/2021 Bug fix * and # reversed in the logs. More debugging in logs
  v3.2   10/09/2021 Added cancel cmd to stop Smoke/Water alarms by OFF. 
                    Added lights flash after action. 
  v3.1   10/08/2021 Lock Manager dupe checking.Bug on PIN code 5 fixed.It was broken from the begining.
  v3.0   10/07/2021 Lock Code manager can now read PINs from Keypad. Max PIN legenth increased to 7
  v2.9   10/07/2021 Reduced dec to 2 in bat voltage to many reports.
  v2.8.3 10/06/2021 Only the pad that sets PANIC can remove it so it now times out
  v2.8.2  "         HSM status was reporting disarmed when sending arm 
                    added delay to allow it to take action before testing state
  v2.8.1  "         OFF was not clearing PANIC if alarm was OFF Fixed
  v2.8    "         Last PIN code rewrite broke button actions  Fixed
  v2.7.1 10/05/2021 Last update slowed down driver screen fixed.
  v2.7    "         Bat Bug fixed. Arm with Pin added. Unlock with pin and OFF added.
                    Panic sets custom panic flag. 
  v2.6   10/02/2021 Added DisarmedBy command, Settings to remap Command Buttons
  v2.5      "       Config for tamper,Log debug cleanup,Remove alarm no sounds
  v2.4   09/30/2021 Custom Panic command added. Added Config options for * # OFF
  v2.3   09/30/2021 battery value changes
  v2.2   09/29/2021 Version detection and auto upgrade/install. 
  v2.1   09/29/2021 Tamper bugs fixed, Log fix,Old IRIS command found and Logged, Master pin added 
  v2.0   09/28/2021 Keypad support debugged , Commands debounced, Logging cleaned up, 
                    Invalid pins trapped Star key sends a 6 digit PIN *#  * Now trapped.
  v1.1   09/27/2021 Cleanup Button controler is working
  v1.0   09/27/2021 Beta test version Buttons now reporting Bat working
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

Panic
Panic sets a panic on or off 
This must be disarmed on the keyboard thats in Panic.

Lock Code Manager
Manager can add delete and verify.

Please Note:
Lock Code Manager has bugs with no error checking it will dupe and corrup PINS 
I have also caught it adding line feeds to pins on my locks corrupting them.
When it sees its duped pin it will crash or create a new user sometimes causing it to crash.
Basicaly anything it sees thats odd will crash it.

I now dupe check what the lock manger sends so no more dupes. But if you have problems with the lock
manager crashing I suggest you manualy delete all codes and then run it again..



As a safety enter a master PIN that the lock manager cant touch.





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
    TheVersion="3.3"
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

//command "quietMode"

attribute "batteryState", "string"
attribute "lastCodeName", "string"
attribute "batteryVoltage", "string"	
attribute "panic", "string"
attribute "code1", "string"
attribute "code1n", "string"
attribute "code2", "string"
attribute "code2n", "string"
attribute "code3", "string"
attribute "code3n", "string"
attribute "code4", "string"
attribute "code4n", "string"
attribute "code5", "string"
attribute "code5n", "string"		
attribute "lockCodes", "string"		

fingerprint profileId: "C216", inClusters: "00F0,00C0,00F3,00F5", endpointId:"02",outClusters: "00C0", manufacturer: "Iris/AlertMe", model: "KeyPad Device", deviceJoinName: "Iris V1 Keypad"
	}
}



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


    input("secure",  "text", title: "7 digit password", description: "A Master 7 digit Overide PIN. Not stored in Lock Code Manager Database 0=disable",defaultValue: 0,required: false)
	input name: "flashError",   type: "bool", title: "Flash the lights after action", description: "Expermental. I think its actualy flashing a error code.",defaultValue: false

    
hack = false
cmdtest = false    
   
    input name: "cmdtest",   type: "bool", title: "Send arming commands to KeyPad", description: "Testing only this does not work yet",defaultValue: false
     
    if (cmdtest == true){flashError = false}// Overide we cant do both

}


def installed(){logging("${device} : Paired!", "info")}

 
def initialize() {
    
// Testing is this needed? Because its not set right by default   
updateDataValue("inClusters", "00F0,00C0,00F3,00F5")
updateDataValue("outClusters", "00C0")
    
state.batteryOkay = true
state.operatingMode = "normal"
state.presenceUpdated = 0
state.rangingPulses = 0
state.Command = "unknown"
state.Panic = false
state.validPIN = false
state.PinName = "none"
state.PIN = "none"

state.icon ="<img src='data:image/jpeg;base64,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'>"
   
sendEvent(name: "operation", value: "normal", isStateChange: false)
sendEvent(name: "presence", value: "present", isStateChange: false)
sendEvent(name: "numberOfButtons", value: "10", isStateChange: false)
sendEvent(name: "panic", value: "off", isStateChange: false)    
sendEvent(name: "maxCodes", value:5)
sendEvent(name: "codeLength", value:7)
sendEvent(name: "securityKeypad", value: "Fetching")
sendEvent(name: "tamper", value: "clear")

state.remove("switch")	
state.remove("uptime")
state.remove("logo")
state.remove("irisKeyPad")
state.remove("rssi")
state.remove("pushed")
state.remove("state.reportToDev")
state.remove("message")
    
removeDataValue("image")
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
	runIn(3500,traceLogOff)
	refresh()

}
/*

from iris source code 
constants alertme.KeyPad {
  DEVICE_TYPE = 0x1C;

  ATTR_STATE = 0x20;
  ATTR_PIN = 0x21;
  ATTR_ACTION_KEY_PRESS = 0x22;
  ATTR_ACTION_KEY_RELEASE = 0x23;
  ATTR_HUB_POLL_RATE = 0x24;
  ATTR_SOUNDS_MASK = 0x25;
  ATTR_SOUND_ID = 0x26;
  ATTR_CUSTOM_SOUND = 0x27;
  ATTR_UNSUCCESSFUL_STATE_CHANGE = 0x27;

  KEYPAD_STATE_UNKNOWN = 0x00;
  KEYPAD_STATE_HOME = 0x01;
  KEYPAD_STATE_ARMED = 0x02;
  KEYPAD_STATE_NIGHT = 0x03;
  KEYPAD_STATE_PANIC = 0x04;
  KEYPAD_STATE_ARMING = 0x05;
  KEYPAD_STATE_ALARMING = 0x06;
  KEYPAD_STATE_NIGHT_ARMING = 0x07;
  KEYPAD_STATE_NIGHT_ALARMING = 0x08;

  KEYPAD_STATE_LOCKED_MASK = 0x80;

  ACTION_KEY_POUND = 0x23; // '#'
  ACTION_KEY_HOME = 0x48; // 'H'
  ACTION_KEY_AWAY = 0x41; // 'A'
  ACTION_KEY_NIGHT = 0x4E; // 'N'
  ACTION_KEY_PANIC = 0x50; // 'P'

  SOUND_CUSTOM = 0x00;
  SOUND_KEYCLICK = 0x01;
  SOUND_LOSTHUB = 0x02;
  SOUND_ARMING = 0x03;
  SOUND_ARMED = 0x04;
  SOUND_HOME = 0x05;
  SOUND_NIGHT = 0x06;
  SOUND_ALARM = 0x07;
  SOUND_PANIC = 0x08;
  SOUND_BADPIN = 0x09;
  SOUND_OPENDOOR = 0x0A;
  SOUND_LOCKED = 0x0B;
}
*/



// Sample Hubitat pin store code is copyrighted
// Legaly we cant use it without written permission
// replacement code
def setCode(code,pinCode,userCode){

	if (code == 1){ save= "code1";}
	if (code == 2){ save= "code2";}
	if (code == 3){ save= "code3";}
	if (code == 4){ save= "code4";}	
    if (code == 5){ save= "code5";}	
	if (code < 6){
        saveit = true
        // Check for dupes due to broken lock manager
        if (device.currentValue("code1") == pinCode){saveit = false} 
        if (device.currentValue("code2") == pinCode){saveit = false}
        if (device.currentValue("code3") == pinCode){saveit = false} 
        if (device.currentValue("code4") == pinCode){saveit = false}        
        if (device.currentValue("code5") == pinCode){saveit = false}        
            logging( "${device} : ADD code#${code} PIN:${pinCode} User:${userCode} [OK to save:${saveit}]","info")        
        if (saveit){    
          logging( "${device} : Saving ...${save}...","info")        
   
	      sendEvent(name: "${save}", value: pinCode)
	      sendEvent(name: "${save}n",value: userCode)
    
        }
	}
     pauseExecution(1200) 
     getCodes()  
}

private upgradeCodes(code){
    
    store1 = device.currentValue("code1")
    store1n= device.currentValue("code1n")
    store2 = device.currentValue("code2")
    store2n= device.currentValue("code2n")    
    store3 = device.currentValue("code3")
    store3n= device.currentValue("code3n")    
    store4 = device.currentValue("code4")
    store4n= device.currentValue("code4n")    
    store5 = device.currentValue("code5")
    store5n= device.currentValue("code5n")

}

def deleteCode(code) { 
    

    if (code == 1){ save= "code1"}
	if (code == 2){ save= "code2"}
	if (code == 3){ save= "code3"}
	if (code == 4){ save= "code4"}	
	if (code == 5){ save= "code5"}	  
    
    
    thecode = device.currentValue("${save}")
    thename = device.currentValue("${save}n")
    
    logging ("${device} : deleteCode  #${code}   code:${thecode} name:${thename}","info")    
    
    
	if (code < 6) {
     device.deleteCurrentState("${save}")    
     device.deleteCurrentState("${save}n")
     pauseExecution(1200) 
     getCodes()   

}
}
// the I hate jason format Routine
def getCodes(){
    qt= '"'
    needsComma = false 
     if (device.currentValue("code1")) {
        code = device.currentValue("code1")
        name = device.currentValue("code1n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode1 = "${qt}1${end}"
        needsComma = true 
    }
    if (device.currentValue("code2")) {
        code = device.currentValue("code2")
        name = device.currentValue("code2n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode2 = "${qt}2${end}"
        if (needsComma == true){ setCode2 = ",${setCode2}"}
        needsComma = true                
    }    
    if (device.currentValue("code3")) {
        code = device.currentValue("code3")
        name = device.currentValue("code3n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode3 = "${qt}3${end}"
        if (needsComma == true){ setCode3 = ",${setCode3}"}
        needsComma = true 
    }
    if (device.currentValue("code4")) {
        code = device.currentValue("code4")
        name = device.currentValue("code4n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode4 = "${qt}4${end}"
        if (needsComma== true){ setCode4 = ",${setCode4}"} 
        needsComma = true 
    }
//    logging("${device} : building ....${device.currentValue("code5")}", "info")
    if (device.currentValue("code5")) {
   
        code = device.currentValue("code5")
        name = device.currentValue("code5n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode5 = "${qt}5${end}"
        if (needsComma == true){ setCode5 = ",${setCode5}"} 
       
    }
  
    
    
    
    
    
    codeStore= "{"
    if (setCode1) { codeStore = "${codeStore}${setCode1}" }
    if (setCode2) { codeStore = "${codeStore}${setCode2}" }
    if (setCode3) { codeStore = "${codeStore}${setCode3}" }
    if (setCode4) { codeStore = "${codeStore}${setCode4}" }              
    if (setCode5) { codeStore = "${codeStore}${setCode5}" }
    

    codeStore = "${codeStore}}"
    sendEvent(name: "lockCodes",value: codeStore)
             
        logging("${device} : lockCode Database ${codeStore}", "info")          
 }

def setEntryDelay(code){logging("${device} : setEntryDelay ${code}  unsupported", "info")}
def setExitDelay(code){	logging("${device} : setExitDelay  ${code}  unsupported", "info")}
def setCodeLength(code){logging("${device} : setCodeLength 7", "info")                   }

// Arming commands
//hsmSetArm = armAway ,armHome,armNight,disarm,disarmAll,armAll,CancelAlerts
//subscribe (location, "hsmStatus", statusHandler)
//subscribe (location, "hsmAlerts", alertHandler)

def cancelAlert(){
    
	logging ("${device} : Sending CancelAlerts by [${state.PinName}]","info")
    sendEvent(name: "securityKeypad",value: "Cancel Alerts",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "CancelAlerts")
    state.Command = "cancel"
    pauseExecution(6000)

}

def armAway() {
	logging ("${device} : Sending armAWAY by [${state.PinName}]","info")
    sendEvent(name: "securityKeypad",value: "armed away",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "armAway")
    state.Command = "away"
    pauseExecution(6000)
}
def armHome() {
	logging ("${device} : Sending armHome by [${state.PinName}]","info")
	sendEvent(name: "securityKeypad",value: "armed home",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "armHome")
    state.Command = "home"
    pauseExecution(6000)
}
def armNight() {
	logging ("${device} : Sending armNight by [${state.PinName}]","info")
	sendEvent(name: "securityKeypad",value: "armed night",data: /{"-1":{"name":"not required","code":"0000","isInitiator":true}}/)
	sendLocationEvent (name: "hsmSetArm", value: "armNight")
    state.Command = "night"
    pauseExecution(6000)
}

def panic() {
	logging ("${device} : Panic Pressed","warn")
    sendEvent(name: "panic", value: "on", displayed: true, isStateChange: true, isPhysical: true)
    state.Panic = true
    sendIrisCmd (4)
    runIn(10000, "panicOff") // auto purge the panic because it cant be cleared from another pad
}
def panicOff() {
    if (state.Panic){
        logging ("${device} : Panic cancled by [${state.PinName}]","info")
        sendEvent(name: "panic",  value: "off", descriptionText: "cancled by ${state.PinName} PIN", isStateChange: true,displayed: true)
        state.Panic = false
        sendIrisCmd (1)
    }
}

def disarm() {
    // Prevent disarm from driver screen without a valid pin (security)
    if (state.validPIN == false){
        logging ("${device} : Must enter a PIN to Disarm.", "warn")
        tamper()
        return
    } 
	sendEvent(name: "securityKeypad", value: "disarmed", descriptionText: "Disarmed by ${state.PinName}", displayed: true,data: /{"-1":{"name":"${state.PinName}","code":"${state.PIN}","isInitiator":true}}/)
	sendEvent(name: "lastCodeName", value: "${state.PinName}", descriptionText: "Disarmed by ${state.PinName}")// May be needed by other aps.
    logging ("${device} : Sent Disarmed by [${state.PinName}]", "info")
    panicOff()
	sendLocationEvent (name: "hsmSetArm", value: "disarm")
    state.Command = "off"
    pauseExecution(6000)// Wait for hub to finish or we will loop.
}

def purgePIN(){
    if (state.validPIN){logging ("${device} : PIN [${state.PinName}] Removed from memory", "info")}
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
/*
  
  ACTION_KEY_POUND = 0x23; // '#'
  ACTION_KEY_HOME = 0x48; // 'H'
  ACTION_KEY_AWAY = 0x41; // 'A'
  ACTION_KEY_NIGHT = 0x4E; // 'N'
  ACTION_KEY_PANIC = 0x50; // 'P'
*/

def sendCommandToKeypad (cmd){
def hexOUT = hubitat.helper.HexUtils.integerToHexString(cmd, 1) 
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {11 00 FA ${hexOUT} 00} {0xC216}"])
    logging ("${device} : Sending CMD:${cmd} Cluster:0x00C0 {11 00 FA ${hexOUT} 00} ","warn")

}

def sendCommandToKeypad2(input) {
   // def hexOUT = hubitat.helper.HexUtils.integerToHexString(cmd, 2) 

	
sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 2 0x0000 0x04 "])

    
logging ("${device} : Sending ","info") 
}
// zigbee.swapOctets(device.deviceNetworkId)


// To be removed once the correct commands are found..
def flash(cmd){
    if (flashError == true){
    pauseExecution(50)// delay to make it look like its waiting for hub.
        
    // flashes the lights. 
    // This runs a cycle on the keybard making it unable to take anyinput till it finishes.
       
        if (state.Command =="off")  {cmd = "01"}
        if (state.Command =="away") {cmd = "02"}
        if (state.Command =="night"){cmd = "03"}
        if (state.Panic  == true  ) {cmd = "04"}
   
  // This is clearly a responce to me sending the wrong format But offers some feedback since cmds are not working 
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {11 ${cmd}} {0xC216}"])
    logging ("${device} : flash ~~ ~~~~ ~~~ ~~","trace")      
    return    
    }

}

// The keypad needs to be notified of its state.
// This must not be formatted right because it doesnt take the command.
// Anyone know whats missing please let me know
def sendIrisCmd (cmdI){
    def hexOUT = hubitat.helper.HexUtils.integerToHexString(cmdI, 2) 
    
    if (cmdtest == true){    
    sendZigbeeCommands(zigbee.readAttribute(0x00C0,cmdI))    
        logging ("${device} : Notifing KeyPad of state .${cmdI} #${hexOUT}","debug")
        return
    }
    logging ("${device} : Notifing KeyPad Disabled","debug") 
}    

def on(cmd) {

// sendEvent(name: "switch", value: "on") 
//state.switch = true
/* Internal notes: Building Cluster map 
* = likely done by HUB in Join.
0000 Network (16-bit) Address Request *
0004 Simple Descriptor Request *
0005 Active Endpoint Request *
0006 Match Descriptor Request (Light Flashes)
0013 Device announce message (Light Flashes)
00C0 Attribute Cluster 
     Button report (button on repeator)
     00 = Unknown (Lifeline report)
     0A = Button
00EE Power Control Cluster  Relay actuation (smartPlugs)
     80 = PowerState
00EF Power Monitor Cluster
     81 = Power Reading
     82 = Energy
00F0 General Cluster  Battery & Temp
     FB 
00F2 Tamper Cluster
     released 0
     pressed  1
     clear    2

00F3 Button Cluster
00F4 Key Fob Cluster
     ALARM_IN_HOUSE = 0x00
     ARM_HAPPY = 0x01
     ARM_UNHAPPY = 0x02
     DISARM_HAPPY = 0x03
     DISARM_UNHAPPY = 0x04
     FAILED_IN_HOME_CMD = 0x05
     HAPPY_IN_HOME_CMD = 0x06

00F6 Discovery Cluster
     FD = Ranging
     FE = Device version response.
0500 Security Cluster (Tamper & Reed)
8001 Routing Neighobor information
8004 simple descriptor response
8005 Active Endpoint Response (tells you what the device can do)
8032 Received when new devices join
8038 Management Network Update Request
0B7D Upgrade Cluster (dont use)
0B7E  "         " 
HE Raw Zigbee Frame for AlertMe
he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 01 01} {0xC216}

*/        
// sending 0x04 arm command 
send = "00 00 00 10" //Big Endian
send = "00 00 00 04" //Little Endian 
if (!state.test){state.test =1}  
if (state.test > 256 ){return}       
send = "00 04 00 00" //reverse

def hexStr = hubitat.helper.HexUtils.integerToHexString(state.test, 1) 
    send = "${hexStr} 23" 
    rev = send.reverse()
    send = "{11 00 00 ${send}}"
//def clust = hubitat.helper.HexUtils.integerToHexString(state.test, 2)  
    send = "FB ${hexStr}"
    send = "{11 00 20 04 00}" //   command 4 should work but it doesnt
//    {11 00 02 20 03}  20 is command 03 is data
   logging ("${device} :Probing >--- dec${state.test} ","info")   

   cmdI =state.test
    

//sendIrisCmd (cmdI)    
    
    
 sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 00 25 00} ",
                    "he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {11 00 00 03 00} "]
                   )
//he raw 0x2FAA 1 0x02 0x00C0 {10 00 00 25 00}
 //sendCommandToKeypad(2)  
// sendZigbeeCommands(zigbee.readAttribute(0x00C0,0x25))      
// sendZigbeeCommands(zigbee.readAttribute(0x00C0,0x01))  
    
    
//  zigbeeWriteAttribute(2, 0x00C0, 0,0 , 2 ) 
//    logging("zigbeeWriteAttribute()", 1)  
    
    
    
  // Send Zigbee Cmd :[he raw 0x2FAA 1 0x02 0x00F0 {10 00 00 C0 00}, delay 2000] 
  //             Cmd :[he raw 0x2FAA 1 0x02 0x00C0 {10 00 00 03 00}, delay 2000]
// sendCommandToKeypad2(2)   
    
 //   sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00C0 {11 05 } {0xC216}"])
       
state.test = state.test + 1
//    pauseExecution(2200)
// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x${clust} {11 02 FA 04 00} {0xC216}"])
    
//runIn(8, "on")    
// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 01 ${device.endpointId} 0x0006 {11 02 00 ${send}} "])
// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 01 ${device.endpointId} 0x00F6 {11 02 00 ${send}} "])

// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 01 ${device.endpointId} 0x00C0 ${send} "])
    
// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 01 ${device.endpointId} 0x00C0 ${send} "])

// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 01 ${device.endpointId} 0x00C0 ${send} "])

// sendZigbeeCommands(["he raw ${device.deviceNetworkId} 01 ${device.endpointId} 0x00C0 ${send} "])
   
    
    // zigbee.command(0x00C0, 0x02,20)
    
/*

Any help in trying to get the keypad to arm is welcome see docs and status here

https://github.com/tmastersmart/hubitat-code
*/
}



def off(cmd){
 logging ("${device} : OFF counter reset to 0","info")
// sendEvent(name: "switch", value: "off")   
state.switch = false
 state.test =0
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
	logging("${device} : New unknown Cluster Detected: Report to DEV clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command} with value:${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
}


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

/* Get HSM status And update our state if its changed
//====================================================STATUS    STATUS
//hsmStatus armedAway, armingAway, armedHome, armingHome, armedNight, armingNight, disarmed, allDisarmed
//hsmAlert  intrusion,intrusion-home,intrusion-night,smoke,water,rule,cancel,arming
  KEYPAD_STATE_UNKNOWN = 0x00;
  KEYPAD_STATE_HOME = 0x01;
  KEYPAD_STATE_ARMED = 0x02;
  KEYPAD_STATE_NIGHT = 0x03;
  KEYPAD_STATE_PANIC = 0x04;
  KEYPAD_STATE_ARMING = 0x05;
  KEYPAD_STATE_ALARMING = 0x06;
  KEYPAD_STATE_NIGHT_ARMING = 0x07;
  KEYPAD_STATE_NIGHT_ALARMING = 0x08;
  KEYPAD_STATE_LOCKED_MASK = 0x80;
*/
def getStatus(status) {
    status = location.hsmStatus
    state.alertST = location.hsmAlert
    if (!state.alertST){state.alertST = "none"}
    if (state.alertST != "none"){
        if(device.currentValue("HSMAlert")!= state.alertST){
        sendEvent(name: "HSMAlert", value: state.alertST)
        logging ("${device} : HSMAlert  Status:${state.alertST}","info")    
        }
    }
    
   logging ("${device} : HSMStatus:${status} HSMAlert:${state.alertST} Our state:${state.Command}","debug")
    if (status == "armedAway"){  
        if (state.Command != "away"){
            sendEvent(name: "securityKeypad", value: "armed away")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "away"
            sendIrisCmd (2)
            flash()
        }
     return
    }
    if (status == "armingAway"){ 
        if (state.Command != "away"){
          sendEvent(name: "securityKeypad", value: "armed away")
            logging ("${device} : Received HSM ${status}","info")
          state.Command = "away"
          sendIrisCmd (2) 
          flash() 
        }
      return 
    }
    
    if (status == "armedHome"){
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armed home")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "home"
            sendIrisCmd (2)
            flash()
        }
        return
       }
    if (status == "armingHome"){ 
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armed home")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "home"
            sendIrisCmd (2)
            flash()
        }
        return
       }  
    
    if (status == "armedNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad", value: "armed night")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "night"
            sendIrisCmd (3)
            flash()
        }
        return
       }

    if (status == "armingNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad", value: "armed night")
            logging ("${device} : Received HSM  ${status}","info")
            state.Command = "night"
            sendIrisCmd (3)
            flash()
        }
        return
       } 
    
    if (status == "disarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad", value: "disarmed")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "off"
            state.iriscmd = "Erased"
            sendIrisCmd (1)
            panicOff()
            flash()
        }
        return
    }
    if (status == "allDisarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad", value: "all disarmed")
            logging ("${device} : Received HSM ${status}","info")
            state.Command = "off"
            state.iriscmd = "Erased"
            sendIrisCmd (1)
            panicOff()
            flash()
        }
        return
    } 
    logging ("${device} : Received HSM ${status} <- (Unknown report to DEV for coding) What is this? Our state:${state.Command}","warn")
}

void refresh() {
    logging ("${device} : Refresh. Sending Hello to Device","info")
// send a "Hello" message, to get version, etc.   
	def cmds = new ArrayList<String>()
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}")    // version information request
//	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 01 01} {0xC216}")    // power control operating mode nudge
	sendZigbeeCommands(cmds)
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
    logging("${device} : Cluster:${map.clusterId} CMD:${map.command} MAP:${map.data}","trace")
//    logging("${device} : Cluster:${map.clusterId} ${map.command} ","debug")
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
   
    if (map.clusterId == "0013"){
	logging("${device} : Device announce message (new device?)","warn")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Anouncing New Device?","debug")
//0013, command:00 [82, 00, 1E, 28, 7E, 6C, 03, 00, 6F, 0D, 00, 80]	<-- Earler
//0013, command:00 [81, AA, 2F, 6D, 07, 9C, 02, 00, 6F, 0D, 00, 80] <-- this is what we get

    } else if (map.clusterId == "0006") {
		logging("${device} : Sending Match Descriptor Response","debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])

    } else if (map.clusterId == "00C0") {
     // Iris Button report cluster 
 
//   if (map.command != "00"){logging ("${device} : key Cluster CMD:${map.command} MAP:${map.data}","trace") }
     
     if (map.command == "01") {
         // Reply to our sending (undocumented) 
         mode1 = receivedData[0]
         mode2 = receivedData[1]
         logging ("${device} : KeyPad Received CMD:[${mode1} ${mode2}]","warn")
     }
     
                               
     if (map.command == "00" ) {
         // I beleive this is theKeyPad asking for status reply. The care fobs do this same thing when asking for a reply.
         
         // Lifeline status report Its always [20, 00] waiting for it to change
         // We monitor it for change and Log what caused it to change. 
         // Still looking for proper command to change status. 
//  iris source recomends 
//  *  - We send a Stop Polling (0xFD) message after each Lifesign message received, to lengthen time between 
//  *    Lifesign messages to 2 minutes.  ( what cluster to send this on?)      
         
         mode1 = receivedData[0]
         mode2 = receivedData[1]
         if (mode1 != "20"){logging ("${device} : Notify DEV!!!! : Badly needed data: NEW MODE raw data:${map.data}","warn")}
         if (mode2 != "00"){logging ("${device} : Notify DEV!!!! : Badly needed data: NEW DATA raw data:${map.data}","warn")}
         
         testdata = "[${mode1},${mode2}]" 
         if (testdata != "[20,00]"){state.testing ="---[${mode1},${mode2}]--->>>>[${state.test}]<<<<}"}
         logging ("${device} : Test Data: STATE:${testdata}","debug") 

 //        if (hack){ on ()}
     }  
        
     if (map.command == "0A") {  
      PinEnclosed = receivedData[0]// The command being passed to us
      pinSize     = receivedData[3]// The PIN size     
      keyRec      = receivedData[4]// The Key pressed 
      keyRecAsc   = receivedData[4..4].collect{ (char)Integer.parseInt(it, 16) }.join()
	  buttonNumber = 0 
      status = "Unknown"   
	  size = receivedData.size()// size of data field
         // Action matrix based in iris source code.
         // Create text for logging
         logPIN  = "Valid PIN:${state.validPIN}"
         logPANIC= "Panic:${state.Panic}"
         if (PinEnclosed  =="20" ){   status = "STATE" }
         if (PinEnclosed  =="21" ){   
             status = "PIN"
             logPIN="Pin:Processing"
         }
         if (PinEnclosed  =="22" ){   status = "Pressed" }
         if (PinEnclosed  =="23" ){   status = "Released" }
         if (PinEnclosed  =="24" ){   status = "Poll Rate" }
         if (PinEnclosed  =="25" ){   status = "sound mask" }
         if (PinEnclosed  =="26" ){   status = "Sound ID" }
         if (PinEnclosed  =="28" ){   status = "Error" } 
         keyRecA ="Received"
         if (keyRec == "48" ){keyRecA ="OFF"}
         if (keyRec == "41" ){keyRecA ="ON"}
         if (keyRec == "4E" ){keyRecA ="PARTIAL"}          
         if (keyRec == "50" ){
             keyRecA ="PANIC"
             logPANIC= "-*-PANIC VALID -*-"
         }            
         if (keyRec == "2A" ){keyRecA ="STAR"}
         if (keyRec == "23" ){keyRecA ="POUND"}
         
       
      logging ("${device} : Action :[${status} ${keyRecA}] Key:${keyRecAsc} ${logPANIC} ${logPIN} State:${state.Command}","info")
       if (size == 10){ // IRIS MODE commands show up here
       hexcmd = receivedData[4..9]  
       rawCMD = receivedData[4..9].collect{ (char)Integer.parseInt(it, 16) }.join()
       irsCMD = keyRecAsc
   nextirsCMD = receivedData[9..9].collect{ (char)Integer.parseInt(it, 16) }.join() 
          // Iris had only 2 armed modes night and away. * and # had no function
          if (irsCMD == "H") {irsCMD1= "HOME"}
          if (irsCMD == "A") {irsCMD1= "AWAY"}
          if (irsCMD == "N") {irsCMD1= "NIGHT"}
          if (irsCMD == "P") {irsCMD1= "PANIC"}
          if (irsCMD == "#") {irsCMD1= "POUND"}
          if (irsCMD == "*") {irsCMD1= "STAR"}

          
          if ( irsCMD == nextirsCMD){logging("${device} : IRIS   :[${irsCMD1}] Valid PIN ${state.validPIN}", "info")}
          else{ logging("${device} : IRIS   :[${irsCMD1}] Next command in qwery [${nextirsCMD}].   Valid PIN ${state.validPIN}", "info")}
          logging("${device} : #${keyRec} Action:${status} Iris cmd:${rawCMD} :${hexcmd}", "trace")
          state.iriscmd = irsCMD1 // store for later use
	      
	 }    

// Now check for our command buttons 
         
     // ====================================PANIC ==================================  
     if (keyRec == "50"){
         if (state.Panic){ 
             if(device.currentValue("panic")!= "on"){ panic()}
             logging("${device} : Button PANIC but already sent","debug") 
             if(device.currentValue("panic")!= "on"){
                 logging("${device} : Panic out of Sync resending command","warn") 
                 panic()
             }
             sendIrisCmd (4)
             return
         }    
		 panic()
         
         logging("${device} : Button *** PANIC ***","info") 
         
         return
   	  }    
         
         
         
         
         
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
//=============================================OFF OFF OFF ==================================================================         
// OFF with produce triplits SO we will loop through here several times.
         if (keyRec == "48"){
           if (state.validPIN == true){ // Valid PIN in memory
            if (state.Command == "off"){// We have already sent disarm
                  if (state.Panic == true){// the PIN is valid we are disarmed and need to clear PANIC
                  logging("${device} : Button OFF (Clearing Panic) Valid PIN:${state.validPIN} State:${state.Command} Panic:${state.Panic}","info")
                  panicOff()
                  cancelAlert()
                  return  
                 }    
                  if (state.alertST != "none"){// the PIN is valid we are disarmed and something is alarming
                  logging("${device} : Button OFF (Clearing ${state.alertST}) Valid PIN:${state.validPIN} State:${state.Command} Panic:${state.Panic} ","info")
                  panicOff()
                  cancelAlert()
                  return  
                 }    
             logging("${device} : Button OFF (already disarmed Skipping) Alarm:${state.alertST} Valid PIN:${state.validPIN} State:${state.Command} Panic:${state.Panic}","debug")
             return
             }//End if already disarmed
               
            logging("${device} : Button OFF Valid PIN: ${state.validPIN} State:${state.Command}  Panic:${state.Panic}","info")
            disarm()
            return
         }// end valid PIN
         logging("${device} : Button OFF Valid PIN:false  State:${state.Command} Panic:${state.Panic}","info")
         return  
	 }         
         
         


 //     "StarSet" ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Home"
         
     if (keyRec == "2A"){
      if (StarSet == "Arm Home"){
		 if (state.Command =="home"){
         logging("${device} : Button * ${StarSet} (But state already sent) state${state.Command}","debug")
         return }
         logging("${device} : Button * ${StarSet}  ${state.validPIN}","info")
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
         logging("${device} : Button * ${StarSet} (But state already sent) state${state.Command}","debug")
         return }
         logging("${device} : Button * ${StarSet}  ${state.validPIN}","info")
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
         logging("${device} : Button * ${StarSet} (But state already sent) state${state.Command}","debug")
         return }
         logging("${device} : Button * ${StarSet} ${state.validPIN}","info")
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
     
     logging("${device} : Button Star ${StarSet} ERROR NOT SETUP","debug")
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
     
     logging("${device} : Button # ${PoundSet} Not setup","debug")
     return
     } 

          

//      PinEnclosed = receivedData[0]// 21 = pin
//      pinSize     = receivedData[3]// The PIN size + 4 = size   
    if (PinEnclosed == "21" ){ 
        if (pinSize != "01" ){// dont run this for button function
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
          logging("${device} : Valid Pin Entered:${name} Waiting for Action CMD" ,"info")
          runIn(60, "purgePIN")

     	  return  
        }   
         
      logging("${device} : PIN ${asciiPin} Invalid PIN HACKING" , "warn")
      if(tamperPIN){tamper()}
      state.validPIN = false
      state.PinName = TAMPER
      state.PIN     = asciiPin   
      return	 
    }// Pin size check  
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
      /*
StopPolling 0xFD
Lifesign    0xFB
   const u8 LIFESIGN_HAS_VOLTAGE = 0x01;
   const u8 LIFESIGN_HAS_TEMPERATURE = 0x02;
   const u8 LIFESIGN_HAS_SWITCH_STATUS = 0x04;
   const u8 LIFESIGN_HAS_LQI = 0x08;
   const u8 LIFESIGN_HAS_RSSI = 0x10;
   const u8 SWITCH_MASK_TAMPER_BUTTON = 0x02;
   const u8 SWITCH_MASK_MAIN_SENSOR = 0x01;
   const u8 SWITCH_STATE_TAMPER_BUTTON = 0x02;
   const u8 SWITCH_STATE_MAIN_SENSOR = 0x01;
   u8 statusFlags;
   u32 msTimer;
   u16 psuVoltage;
   u16 temperature;
   i8 rssi;
   u8 lqi;
   u8 switchMask;
   u8 switchState;

ModeChange  0xFA
   const u8 MODE_NORMAL = 0x00;
   const u8 MODE_RANGE_TEST = 0x01;
   const u8 MODE_TEST = 0x02;
   const u8 MODE_SEEKING = 0x03;
   const u8 MODE_IDLE = 0x04;
   const u8 MODE_QUIESCENT = 0x05;
   const u8 FLAG_CLEAR_HNF = 0x01;
   const u8 FLAG_SET_HNF = 0x01;
   u8 mode;
   u8 flags;

FaultReport 0x01
   const u16 FAULT_NOFAULT = 0;
   const u16 FAULT_EMBER_STACK_STARTUP = 1;
   const u16 FAULT_WRONG_HARDWARE = 2;
   const u16 FAULT_WRONG_HARDWARE_REVISION = 3;
   const u16 FAULT_TOKEN_AREA_INVALID = 4;
   const u16 FAULT_NO_BOOTLOADER = 5;
   const u16 FAULT_NO_SERIAL_OUTPUT = 6;
   const u16 FAULT_EMBER_MFGLIB_STARTUP = 7;
   const u16 FAULT_FLASH_FAILED = 8;
   const u16 FAULT_MCP23008_FAILED = 9;
   const u16 FAULT_VERY_LOW_BATTERY = 10;
   const u16 FAULT_FAILED_TO_FORM_NETWORK = 11;
   const u16 FAULT_CHILD_DEVICE_LOST = 12;

   u16 manufId;
   u16 modelId;
   u16 faultId;
}

      */ 
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
     batteryVoltage = batteryVoltage.setScale(2, BigDecimal.ROUND_HALF_UP)
     logging("${device} Raw Battery  Bat:${batRec} ${batteryVoltage}", "trace")    
 
// I base this on Battery discharge curves(may need adjustments)
// Normal batteries slowely discharge others have a sudden drop          
// Iris source code says 2.1 is min voltage   
      // iris source code recomends 2.1 as min voltage
      // however im still testing min voltage needed    
		BigDecimal batteryVoltageScaleMin = (0.8 * 2)
		BigDecimal batteryVoltageScaleMax = 3.00	    
	    
	    if (BatType == "NiCad"){ // < 1.2x2=2.2 drops out fast
		batteryVoltageScaleMin = (1.0 * 2)
		batteryVoltageScaleMax = 3.00	    
	    } 
        if (BatType == "NiMH"){ // < 1.2x2=2.2 drops out fast
		batteryVoltageScaleMin = (1.0 * 2)
		batteryVoltageScaleMax = (1.35 * 2)	    
	    }    

	    if (BatType == "Lithium"){// < 1.25x2=2.5 drops out fast 
		batteryVoltageScaleMin = (1.1 * 2)
		batteryVoltageScaleMax = (1.7 * 2)	    
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
	  logging ("${device} : quiet ranging","debug")

	} else if (receivedData[1] == "00") {
          // This is the ranging report received when the device reboots.
	  // After rebooting a refresh is required to bring back remote control.
            loging("${device} : --reboot-- ","warn")// We need to know its rebooted in case of crash.
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
	logging("${device} : Device version Size:${versionInfoBlockCount} blocks:${versionInfoDump}","trace")
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
