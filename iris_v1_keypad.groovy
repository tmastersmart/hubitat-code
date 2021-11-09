/* Iris v1 KeyPad Driver
Hubitat Iris v1 KeyPad driver 
Supports keypad disarm arm functions 
Works with Lock Code Manager (4-15 digit Pin size supported)
Works with HSM 
 


  _____ _____  _____  _____        __    _  __                          _ 
 |_   _|  __ \|_   _|/ ____|      /_ |  | |/ /                         | |
   | | | |__) | | | | (___   __   _| |  | ' / ___ _   _ _ __   __ _  __| |
   | | |  _  /  | |  \___ \  \ \ / / |  |  < / _ \ | | | '_ \ / _` |/ _` |
  _| |_| | \ \ _| |_ ____) |  \ V /| |  | . \  __/ |_| | |_) | (_| | (_| |
 |_____|_|  \_\_____|_____/    \_/ |_|  |_|\_\___|\__, | .__/ \__,_|\__,_|
                                                   __/ | |                
                                                  |___/|_|   

=================================================================================================
  v4.9   11/09/2021 Silent arming options.  min bat voltage adjustments.
                    Door chime still walking over entry chime. New fix.
                    Upgrade lockdata fileds in hsm arming to match keypad arming.
  v4.8   10/29/2021 No longer sending HSM arming commands. HSM now reads the keyboard state.
                    error in panic cancel fixed
  v4.7   10/28/2021 Logging for HSM SYNC. Verifies proper operation in log.
                    Disarmed during Entry did not always stop alarm fixed
                    Syntax error causing arm away to arm 2 times.
                    ON is now AWAY only.  Part can be customized
                    Door chime during entry is a problem. Fixed
  v4.6   10/27/2021 added armingIn event, 
                    Logging changed to match standard digital/physical
                    Arming routine changed, No longer waits for HSM to start.
                    Rewrite of arming/entry/disarm routine. Less log clutter
                    Direct HSM arming can be disabled.
                    Fingerprint updated. Testing to see if detection will work
                    Would arm if you cancled arming. fixed
                    Arming delays now set direct by HSM.
  v4.5   10/26/2021 Error in cron on driver change.
  v4.4   10/22/2021 Added Beep cmd. Tamper or shock alarm for bad pin.
                    Arming overides chime rewritten. Automatic dead bat detection
  v4.3   10/21/2021 Arming and Entry have priority over chime. Timeout timmer for chime
  v4.2   10/20/2021 Code cleanup UI text changes Longer PIN'S added. 
                    More Undocumented chimes found. Door chime is sound 10. Bad PIN sound added.
                    Entry Alarm fixed. Uncluttering the logs.
  v4.1   10/19/2021 Rewrite Entry code.
  v4.0   10/19/2021 Bugs in arming disarming and entry Fixed    
                    Alarm tones setup. Chimes now working using play command.
  v3.9   10/18/2021 Bug in the entry delay chimes.
  v3.8   10/18/2021 Bug in Pin fixed.  Arming sounds added.  Alarm is working Enabled.
                    Chimes 1 2 3 4 added but not fully working. 
                    Added alarm arming delay
  v3.7   10/17/2021 bug in reboot logging throwing a error fixed
  v3.6   10/17/2021 Master PIN any legenth all PINS legenth increased to 10
  v3.5   10/16/2021 No longer polling HSM sate. This requires that Keyboard be enabled in HSM
                    Last pin stats bug, was only reporting disarm
                    Added codeChanged Lock manager may look for this.
                    Added entry HSM sends this command with a delay. Future use.
  v3.4   10/14/2021 HSM was throwing errors if keyboard was enable in HSM. Fixed
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
4-15 digit Pin size supported

Arming
Can be set to require PIN or not
ActionButtons can be remaped.
HSM is armed direct can be disabled in setup.


Alarm
Siren = plays tone set
strobe= strobes panic
both  = plays tone set
Note: Key pad has its own HSM alarm mode.
sending alarm while in HSM alarming will change the tones.


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

Play chimes select 1 to 13
1 KEYCLICK
2 LOSTHUB
3 ARMING
4 ARMED
5 HOME
6 NIGHT
7 ALARM
8 PANIC
9 BADPIN 
10 Clasic Iris Door Chime 
11 GAME
12 CPU
13 Real Bad PIN

Chimes repeat to stop them I have added a countdown timmer. 
Beware sometimes the keypad is slow and you may not get the same tone legenth every time..



Please Note:
Lock Code Manager has bugs with no error checking it will dupe and corrup PINS 
I have also caught it adding line feeds to pins on my locks corrupting them.
When it sees its duped pin it will crash or create a new user sometimes causing it to crash.
If you have any problems erase all your codes.Do not reinstall lock manager


As a safety enter a master PIN that the lock manager cant touch.



Total worktime to build up to v2 2 days. Have fun.
 
FCC ID:FU5TSA04 https://fccid.io/FU5TSA04
Built by Everspring Industry Co Ltd Smart Keypad TSA04            


I wrote this for my keyboards you are welcome to use it.
================================================================================================
Tested on Firmware 2013-06-28 and 2012-12-11 Only known versions

https://github.com/tmastersmart/hubitat-code


Post your comments here. 
http://www.winnfreenet.com/wp/2021/09/iris-v1-keyboard-driver-for-hubitat/

* To Reset Device:
 *    Insert battery and then press "ON" button device 5 times within the first 10 seconds.
 * 
 * The keypad is responsible for;
 *   1. Driving its LEDs according to its state
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



 * ranging code based on alertme UK code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.

 */
def clientVersion() {
    TheVersion="4.9"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

import hubitat.zigbee.clusters.iaszone.ZoneStatus
//import hubitat.zigbee
import hubitat.helper.HexUtils
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

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
capability "Tone"        
capability "ShockSensor"
capability "Chime"
capability "Alarm"


command "checkPresence"
command "normalMode"
command "rangingMode"
command "entry" 
command "quietMode"
command "SendState"
command "getStatus"
        

attribute "armingIn", "NUMBER"
attribute "batteryState", "string"
attribute "lastCodeName", "string"
attribute "lastCodePIN", "string"        
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

fingerprint profileId:"C216", inClusters: "00F0,00C0,00F3,00F5", endpointId:"02",outClusters: "00C0", manufacturer: "Iris/AlertMe", model: "KeyPad Device", deviceJoinName: "Iris V1 Keypad"
	}
}



preferences {
	
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false

	input name: "tamperPIN",   type: "bool", title: "Press Tamper on BAD PIN (off use shock)", defaultValue: true
    input name: "requirePIN",  type: "bool", title: "Require Valid PIN to ARM", defaultValue: false, required: true
    input name: "detectBadBat",  type: "bool", title: "Automatic dead battery detection", defaultValue: true, required: true

	input name: "SilentArmHome", type: "bool", title: "Silent Arming Home", description: "No beep while arming", defaultValue: false
	input name: "SilentArmAway", type: "bool", title: "Silent Arming Away", description: "No beep while arming", defaultValue: false
	input name: "SilentArmNight",type: "bool", title: "Silent Arming Night",description: "No beep while arming", defaultValue: false
    
    
    
    input name: "PartSet", type: "enum", title: "Partial Button", description: "Customize Partial Button",  options: ["Arm Night", "Arm Home"], defaultValue: "Arm Night",required: true 
    input name: "PoundSet",type: "enum", title: "# Button", description: "Customize Pound Button",  options: ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Home",required: true 
    input name: "StarSet" ,type: "enum", title: "* Button", description: "Customize Star Button",  options:  ["Disabled","Arm Night", "Arm Home", "Arm Away"], defaultValue: "Disabled",required: true 

    input name: "BatType", type: "enum", title: "Battery Type", options: ["Lithium", "Alkaline", "NiMH", "NiCad"], defaultValue: "Alkaline",required: true  
    input name: "AlarmTone",type:"enum", title: "Alarm Tone",description: "Customize Alarm Tone", options: ["KEYCLICK","LOSTHUB","ARMING","ARMED","HOME","NIGHT","ALARM","PANIC","BADPIN","GAME","CPU"], defaultValue: "ALARM",required: true  
    input("chimeTime",  "number", title: "Chime Timeout", description: "Chime Timeout timer. Sends stop in ms 0=disable",defaultValue: 5000,required: true)

    input("secure",  "text", title: "Master password", description: "4 to 11 digit Overide PIN. Not stored in Lock Code Manager Database 0=disable",defaultValue: 0,required: false)

//    input name: "DirectHSMcmd",  type: "bool", title: "Send direct HSM cmd", description:"Send the local hub HSM arm cmd", defaultValue: true, required: true

}


def installed(){
logging("${device} : Paired!", "info")
configure()
}

 
def initialize() {
    
// Testing is this needed? Because its not set right by default   
updateDataValue("inClusters", "00F0,00C0,00F3,00F5")
updateDataValue("outClusters", "00C0")
    
state.delayExit = 30
state.armNightDelay = 30
state.armHomeDelay = 30
state.delayEntry = 30  
    
state.message = "Enable [${device}] in HSM"
state.waiting = 0 // Out of state timer
state.delay = 10  // hub sends this on every arm

state.batteryOkay = true
state.Panic = false
state.validPIN = false
    
state.operatingMode = "normal"
state.presenceUpdated = 0
state.rangingPulses = 0
state.Command = "unknown"

state.PinName = "none"
state.PIN = "none"
state.icon ="<img src='data:image/jpeg;base64,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'>"
   
sendEvent(name: "operation", value: "normal", isStateChange: false)
sendEvent(name: "presence", value: "present", isStateChange: false)
sendEvent(name: "numberOfButtons", value: "10", isStateChange: false)
sendEvent(name: "maxCodes", value:5)
sendEvent(name: "codeLength", value:15)
sendEvent(name: "securityKeypad", value: "Fetching")
sendEvent(name: "tamper", value: "clear")
//sendEvent(name: "switch", value: "off")
sendEvent(name: "status", value: "ok")    
sendEvent(name: "lastCodeName", value: "none", descriptionText: "Initialised")
sendEvent(name: "lastCodePIN",  value: "0000", descriptionText: "Initialised")    
sendEvent(name: "siren", value: "off")  
sendEvent(name: "strobe", value: "off") 
sendEvent(name: "alarm", value: "off")
sendEvent(name: "panic",  value: "off")    
    
state.remove("switch")	
state.remove("uptime")
state.remove("logo")
state.remove("irisKeyPad")
state.remove("iriscmd")
state.remove("alertST")
state.remove("reportToDev")
state.remove("iriscmd")
state.remove("alertST")
state.remove("armingMode")
state.remove("waitForGetInfo")
    
removeDataValue("image")
device.deleteCurrentState("alarm")    
device.deleteCurrentState("pushed") 
device.deleteCurrentState("pin")     
device.deleteCurrentState("lockCodes")
device.deleteCurrentState("HSMAlert")    

//operation
// What is operation for carryover from alert me drivers
    
// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!
randomSixty = Math.abs(new Random().nextInt() % 60)
runIn(randomSixty,refresh)

getStatus()
//SendState()    
getCodes()   
logging("${device} : Initialised", "info")
  
}


def configure() {
	initialize()
	unschedule()
	// Default logging preferences.
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"true",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	// Schedule our ranging report.
	int checkEveryHours = 12 
    // Request a ranging report and refresh every x hours.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)
    // At X seconds past X minute, every checkEveryHours hours, starting at Y hour.
	// Schedule the presence check.
	int checkEveryMinutes = 6																					
    // Check presence timestamp every 6 minutes or every 1 minute for key fobs.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)	
    // At X seconds past the minute, every checkEveryMinutes minutes.
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
     pauseExecution(3000) // We have to wait for database update
     getCodes() 
     sendEvent(name:"codeChanged",value:"added",data:"${codeStore}", isStateChange: true)
     pauseExecution(3000) // We have to wait for database update
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
     pauseExecution(3000) // We have to wait for database update
     getCodes() 
     sendEvent(name:"codeChanged",value:"deleted",data:"${codeStore}", isStateChange: true)
     pauseExecution(3000) // We have to wait for database update
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

// using state.delayExit 
void setExitDelay(Map delays){
    logging("${device} : setExitDelay ${delays}", "debug")
    state.delayExit = (delays?.awayDelay ?: 0).toInteger()
    state.armNightDelay = (delays?.nightDelay ?: 0).toInteger()
    state.armHomeDelay = (delays?.homeDelay ?: 0).toInteger()
    state.delay = state.delayExit 
}
def setEntryDelay(cmd){
    state.delayEntry =cmd
    state.delay = cmd  
    logging("${device} : setEntryDelay ${cmd}", "debug")
    }
// reserved later use
def setExitDelay(cmd){return}

def setCodeLength(cmd){
    logging("${device} : setCodeLength ${cmd} ignored set to 15", "info")
}

// Arming commands
//hsmSetArm = armAway ,armHome,armNight,disarm,disarmAll,armAll,CancelAlerts
//subscribe (location, "hsmStatus", statusHandler)
//subscribe (location, "hsmAlerts", alertHandler)



// For calling delayed arming.
def setAway(){
if (state.Command == "off"){
    logging ("${device} : Disarmed while arming. Abort","warn")
    return}    
state.Command = "away"
SendState()
 runIn(20,getSoftStatus) 
}
def setHome(){
if (state.Command == "off"){
    logging ("${device} : Disarmed while arming. Abort","warn")
    return}     
state.Command = "home"
SendState()
 runIn(20,getSoftStatus)   
}
def setNight(){
if (state.Command == "off"){
    logging ("${device} : Disarmed while arming. Abort","warn")
    return}     
state.Command = "night"
SendState()
 runIn(20,getSoftStatus)    
}

// not sure what this log it
void setPartialFunction(cmd){
logging ("${device} : HSM sent >> ${cmd}","warn")
}


// ===========================================HSM RECEIVED =======================================
// Incomming command from HSM Including a delay


def armAway(cmd){
// state.delayExit  < we use preset delay not cmd  
    state.received = true
    if (state.Command == "armingAway" | state.Command == "away" ){
    logging ("${device} : Ignored HSM CMD [ArmAWAY] State:${state.Command}","info")
    return
    }
 
    if (cmd >5){ state.delay =cmd} 

    MakeLockCodeIN()
    data = [armMode:"armed away",armCmd:"armAway"]
    sendEvent(name: "securityKeypad",value: "armed away",data: lockCode, type: "digital",descriptionText: "${device} was armed away [digital]")
    sendEvent(name:"armingIn", value: state.delayExit,data: data, isStateChange:true,descriptionText: data ) // The actual armming cmd    
    logging ("${device} : Received >> [ArmAWAY] Delay:${state.delayExit}  Our State:${state.Command}  [digital] SilentArm${SilentArmAway}", "info")
    state.Command = "armingAway"
    if (SilentArmAway == false){SendState()}
    runIn(state.delayExit,setAway)
}

def armHome(cmd){
//    state.armNightDelay    
//    state.armHomeDelay
//    
    state.received = true
    if (state.Command == "armingHome" | state.Command == "home"){
    logging ("${device} : Ignored HSM CMD [ArmHOME] State:${state.Command}","info")
    return
    }

    if (cmd >5){ state.delay =cmd} 
    MakeLockCodeIN()
    data = [armMode:"armed home",armCmd:"armHome"]
    sendEvent(name: "securityKeypad",value: "armed home",data: lockCode, type: "digital",descriptionText: "${device} was armed home [digital]")
    sendEvent(name:"armingIn", value: state.armHomeDelay,data: data, isStateChange:true,descriptionText: data ) // The actual armming cmd    
    logging ("${device} : Received >> [ArmHome] Delay:${state.armHomeDelay}  Our State:${state.Command}  [digital] SilentArm${SilentArmHome}", "info")
    state.Command = "armingHome"
    if (SilentArmHome == false){SendState()}
    runIn(state.armHomeDelay,setHome)
    return
}
def armNight(cmd){

    state.received = true
    if (state.Command == "armingNight" | state.Command == "night"){
        logging ("${device} : Ignored HSM CMD [ArmNight] State:${state.Command}","info")
        return
    }

    if (cmd >5){ state.delay =cmd}
    MakeLockCodeIN()
    data = [armMode:"armed night",armCmd:"armNight"]
    sendEvent(name: "securityKeypad",value: "armed night",data: lockCode, type: "digital",descriptionText: "${device} was armed night [digital]")
    sendEvent(name:"armingIn", value: state.armNightDelay,data: data, isStateChange:true,descriptionText: data ) // The actual armming cmd    
    logging ("${device} : Received >> [ArmNight] Delay:${state.armNightDelay}  Our State:${state.Command}  [digital] SilentArm${SilentArmNight}", "info")
    state.Command = "armingNight"
    if (SilentArmNight == false){SendState()}
    runIn(state.armNightDelay,setNight)
}

// HUB says DISARM
def disarm(cmd) { 
    cmd = 0
    state.received = true
    sendEvent(name: "alarm", value: "off") 
    sendEvent(name: "siren", value: "off")  
    sendEvent(name: "strobe", value: "off") 
    sendEvent(name: "panic",  value: "off") 
    
    
    if (state.Command == "off" ){ // stop repeated off commands
        logging ("${device} : Ignored HSM CMD [disarm] State:${state.Command} [digital]","info")
        return
    }    
    MakeLockCodeIN()
      data = [armMode:"disarmed",armCmd:"disarm"]
    sendEvent(name: "securityKeypad",value: "disarmed",data: lockCode, type: "digital",descriptionText: "${device} was disarmed [digital]")
    sendEvent(name:"armingIn", value: cmd,data: data, isStateChange:true,descriptionText: data ) // The actual armming cmd    
    logging ("${device} : Received >> [disarmed]  Our State:${state.Command}  [digital] ", "info")
    state.Command = "off"
    SendState()
    runIn(15,getSoftStatus)
}



def softOFF(){
  logging ("${device} : Alarm OFF ","info")  
  sendEvent(name: "alarm", value: "off") 
  getStatus() // Are we armed or not Reset state
}

def setNA(){// Alarm Part
    if (state.Command == "off"){
    logging ("${device} : Disarmed during Entry. Abort","warn")
    return} 
    
    state.Command ="alarmingNight"
    SendState() 
    sendEvent(name: "alarm", value: "on")
    logging ("${device} : Alarm on ","info")
    runIn(60,softOFF) // Times out the alarm to save bat. 1 min  
}

def setAA(){// Alarm ON 
    if (state.Command == "off"){
    logging ("${device} : Disarmed during Entry. Abort","warn")
    return} 
    
    state.Command ="alarmingAway"    
    SendState()
    sendEvent(name: "alarm", value: "on")
    logging ("${device} : Alarm on ","info")
    runIn(60,softOFF) // Times out the alarm to save bat. 1 min  
}
//=========================================ENTRY====================================================
def entry(cmd){ 
    if (cmd >5){ state.delay =cmd} 
// no longer using the input here using the set delay
    if (state.delayEntry < 3){
     instantEntry()
     return
    }

    if (state.Command == "off"){ 
    logging ("${device} : Entry while in OFF (We are out of Sync forcing Resync ","warn")
    getStatus()     
    } 
    
    if (state.Command == "night"){
        state.Command = "armingNight"
        SendState()
        runIn(state.delayEntry+1,setNA) // Night ALARM
    }
    if (state.Command == "home"){ 
        state.Command = "armingHome"
        SendState()
        runIn(state.delayEntry+1,setAA)// ALARM
    }
    if (state.Command == "away"){ 
        state.Command = "armingAway"
        SendState()
        runIn(state.delayEntry+1,setAA)// ALARM
    }
logging ("${device} : ENTRY in progress delay:${state.delayEntry} state:${state.Command} ","warn")
   
   

}    
        
def instantEntry(){    
    logging ("${device} : Entry in Progress INSTANT  ","warn")    
    if (state.Command == "night"){
        state.Command = "armingNight"
        setNA()
    }
    else {
       state.Command = "armingHome"
        setAA()
    }
    // how long the alarm lasts. 10Polls ~ 40sec app
    state.delay = 10
    runIn(60,softOFF) 
 }
/*=============================================================================================

 sendEvent(name:"armingIn", value: value,data:[armMode:getArmText(armRequest),armCmd:getArmCmd(armRequest)], isStateChange:true)
        case "00": return "disarm"
        case "01": return "armHome"
        case "02": return "armNight" 
        case "03": return "armAway"

 input name: "DirectHSMcmd",  type: "bool", title: "Send direct HSM cmd", defaultValue: true, required: true

, type: "physical", type: "digital"
*/
// My arming commands======================================================================

private MakeLockCode(cmd){
// more compatibility code
def isInitiator = true    
def lockCode = JsonOutput.toJson(["${state.pinN}":["name":"${state.PinName}", "code":"${state.PIN}", "isInitiator":"${isInitiator}"]] )
logging ("${device} : lockCode:${lockCode}]","trace")
}

private defaultLockCode(){
// more compatibility code NO PIN
        state.PIN = "0000"         
        state.PinName = "not required"
        state.pinN = -1
}

private MakeLockCodeIN(cmd){
defaultLockCode()
def isInitiator = false    
def lockCode = JsonOutput.toJson(["${state.pinN}":["name":"${state.PinName}", "code":"${state.PIN}", "isInitiator":"${isInitiator}"]] )
logging ("${device} : lockCode:${lockCode}]","trace")
}
//defaultLockCode()
//MakeLockCode()

private MyarmAway() {
    if (!state.delayExit){state.delayExit =30 }
    cmd = state.delayExit 
    MakeLockCode()
    data = [armMode:"armed away",armCmd:"armAway"]
    sendEvent(name: "securityKeypad",value: "armed away",data: lockCode, type: "physical",descriptionText: "[physical] away")
    sendEvent(name:"armingIn", value: cmd,data: data, isStateChange:true,descriptionText: data ) // The actual armming cmd
    state.received = false
    logging ("${device} : was armed away [physical] by [${state.PinName}] Delay:${cmd} SilentArm:${SilentArmAway}","info")
    state.Command = "armingAway"
    if (SilentArmAway == false){SendState()}
    runIn(cmd+2,setAway)
   
}
private MyarmHome() {
    if (!state.armHomeDelay){state.armHomeDelay =30 }
    cmd = state.armHomeDelay 
    MakeLockCode()
    data = [armMode:"armed home",armCmd:"armHome"]
	sendEvent(name: "securityKeypad",value: "armed home",data: lockCode, type: "physical",descriptionText: "[physical] home")
    sendEvent(name:"armingIn", value: cmd,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
    state.received = false
    logging ("${device} : was armed home [physical] by [${state.PinName}] Delay:${cmd} SilentArm:${SilentArmHome}","info")
    state.Command = "armingHome"
    if (SilentArmHome == false){SendState()}
    runIn(cmd+2,setHome)
  
    
}
private MyarmNight() {
    if (!state.armNightDelay){state.armNightDelay =30 }
    cmd = state.armNightDelay  
    MakeLockCode()
    data = [armMode:"armed night",armCmd:"armNight"]
	sendEvent(name: "securityKeypad",value: "armed night",data: lockCode, type: "physical",descriptionText: "[physical] night")
    sendEvent(name:"armingIn", value: cmd,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
    state.received =false
    logging ("${device} : was armed night [physical] by [${state.PinName}]  Delay:${cmd} SilentArm:${SilentArmNight}","info")
    state.Command = "armingNight"
    if (SilentArmNight == false){SendState()}
    runIn(cmd+2,setNight)
    
}

def panic() {
	logging ("${device} : Panic Pressed","warn")
    sendEvent(name: "panic", value: "on", displayed: true, isStateChange: true, isPhysical: true)
    state.Command = "panic"
    SendState()         
    runIn(70, "off") 
    // has to be longer than timeout on PANIC triplits
    // has to be long or it will restart.
}

private cancelAlert(){
	logging ("${device} : Sending CancelAlerts by [${state.PinName}]","info")
    data = [armMode:"cancel alerts",armCmd:"CancelAlerts"]
	sendEvent(name: "securityKeypad",value: "cancel alerts",data: lockCode, type: "physical",descriptionText: "[physical] ${lockCode}")
    sendEvent(name:"armingIn", value: 0,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
}

private MyDisarm(cmd) {
    if (state.validPIN == false){
        logging ("${device} : Untrapped ERROR Disarm with NO PIN.", "warn")
        return
    } 

    if (state.Command == "panic") {
        logging ("${device} : Panic cancled by [${state.PinName}]","info")
        sendEvent(name: "panic",  value: "off", descriptionText: "cancled by ${state.PinName} PIN", isStateChange: true,displayed: true)
     }
    
    MakeLockCode()

    if (state.Command == "off") {cancelAlert()}// if OFF may be trying to cancel a alert
    logging ("${device} : was disarmed [physical] by [${state.PinName}] state:${state.Command}","info")
    data = [armMode:"disarmed",armCmd:"disarm"]
	sendEvent(name: "securityKeypad", value: "disarmed", descriptionText: "[physical] disarmed", data: lockCode, type: "physical")
    sendEvent(name:"armingIn", value: 0,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
    sendEvent(name: "alarm", value: "off") 
    state.Command = "off"
    SendState()
    runIn(15,getSoftStatus)
    

}
def SendState(cmd){
// Iris KeyPad states not HSM
// Iris only has 2 armed states  
if (state.Command == "off")  {        sendIrisCmd (0x01)}// OFF
if (state.Command == "panic"){        sendIrisCmd (0x04)}// Panic 
// Away uses ON   
if (state.Command == "away") {        sendIrisCmd (0x02)}// ON 
if (state.Command == "armingAway"){   sendIrisCmd (0x05)}// Arming ON 
if (state.Command == "alarmingAway"){ sendIrisCmd (0x06)}// alarming ON
// Home uses Part  
if (state.Command == "home") {        sendIrisCmd (0x03)}// P 
if (state.Command == "armingHome"){   sendIrisCmd (0x07)}// Arming P
if (state.Command == "alarmingHome"){ sendIrisCmd (0x08)}// alarming P
// Night uses Part   
if (state.Command == "night"){        sendIrisCmd (0x03)}// P      
if (state.Command == "armingNight"){  sendIrisCmd (0x07)}// Arming P    
if (state.Command == "alarmingNight"){sendIrisCmd (0x08)}// alarming P
}


//==================================================== POLL for HSM STATUS =================================================
// Polls HSM and sets state even if keyboard is not setup in HSM

def getSoftStatus(status){
// logging routine for debuging HSM status
//  HSM Sync status HSM:home <> state:armingHome  
   status = location.hsmStatus
    if (status == "armedAway" ) {test = "away"}    
    if (status == "armedHome" ) {test = "home"}
    if (status == "armedNight") {test = "night"}
    if (status == "disarmed"  | status == "allDisarmed"){test = "off"}
    if (test == state.Command){logging ("${device} : Verified in state with HSM No problems","info") }
    else {logging ("${device} : HSM Sync status HSM:${test} <> state:${state.Command}","info")}          
}

def getStatus(status) {
   status = location.hsmStatus
   MakeLockCodeIN() 
   logging ("${device} : Polling HSMStatus:${status} Ourstate:${state.Command}","debug") 
    
    if (status == "armedAway" | status == "armingAway" ){  
        if (state.Command != "away"){
            sendEvent(name: "securityKeypad",value: "armed away",data: lockCode, type: "digital",descriptionText: "${device} was armed away [digital]")
            logging ("${device} : Polled HSM ${status} switching now","info")
            state.Command = "away"
            SendState()
        }
     return
    }
    
    if (status == "armedHome" | status == "armingHome"){
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armed home")
            logging ("${device} : Polled HSM ${status} switching now","info")
            state.Command = "home"
            SendState()
        }
        return
       }
    
    if (status == "armedNight" | status == "armingNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad",value: "armed night",data: lockCode, type: "digital",descriptionText: "${device} was armed night [digital]")
            logging ("${device} : Polled HSM ${status} switching now","info")
            state.Command = "night"
            SendState()
        }
        return
       }

    
    if (status == "disarmed" | status == "allDisarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad",value: "disarmed",data: lockCode, type: "digital",descriptionText: "${device} was disarmed [digital]")
            logging ("${device} : Polled HSM ${status} switching now","info")
            state.Command = "off"
            SendState()
        }
        return
    }
}

def purgePIN(){
    if (state.validPIN){logging ("${device} : PIN [${state.PinName}] Removed from memory", "info")}
state.validPIN = false
state.PinName = "none"
state.PIN = "NA"    
}


def siren(cmd){
    
if (state.Command == "armingNight" | state.Command == "armingHome"| state.Command == "armingAway" | state.Command == "alarmingNight"| state.Command == "alarmingAway" | state.Command == "alarmingHome"){
    logging ("${device} : Alarm in use Restarting in 10","warn")
        runIn(10,siren(cmd))
        return
        }

  sendEvent(name: "siren", value: "on", displayed: true) 
  sendEvent(name: "alarm", value: "on", displayed: true) 
  if (AlarmTone == "KEYCLICK"){soundCode(1)}
  if (AlarmTone == "LOSTHUB") {soundCode(2)}
  if (AlarmTone == "ARMING")  {soundCode(3)}
  if (AlarmTone == "ARMED")   {soundCode(4)}
  if (AlarmTone == "HOME")    {soundCode(5)}
  if (AlarmTone == "NIGHT")   {soundCode(6)}
  if (AlarmTone == "ALARM")   {soundCode(7)}
  if (AlarmTone == "PANIC")   {soundCode(8)}
  if (AlarmTone == "BADPIN")  {soundCode(9)}
  if (AlarmTone == "GAME")    {soundCode(11)}
  if (AlarmTone == "CPU")     {soundCode(12)}  
  logging ("${device} : Siren ${AlarmTone} ON", "warn")  
}
def strobe(cmd){
    
    if (state.Command == "armingNight" | state.Command == "armingHome" | state.Command == "alarmingNight" | state.Command == "alarmingHome"){
        logging ("${device} : Unable to Play Chimes. ${state.Command} overides chime.","warn")
        sendEvent(name: "status", value: "Inuse")
        return
        }  
    
  logging ("${device} : Panic Strobe ON","info")  
  sendEvent(name: "strobe", value: "on", displayed: true) 
  sendEvent(name: "alarm", value: "on")
  state.Command = "panic"  
  SendState()  
}
def both(cmd){siren(cmd)}

def off(cmd){
  logging ("${device} : OFF siren/strobe","info")
  sendEvent(name: "siren", value: "off")  
  sendEvent(name: "strobe", value: "off") 
  sendEvent(name: "alarm", value: "off")
  sendEvent(name: "panic",  value: "off") 
  state.Command = "off"
  SendState()  
  runIn(2, getStatus) // Reset the state to HSM 
}

//if (state.Command == "armingHome"){   sendIrisCmd (0x05)}// Arming ON
//if (state.Command == "armingNight"){  sendIrisCmd (0x07)}// Arming Part
//if (state.Command == "alarmingHome"){ sendIrisCmd (0x06)}// alarming ON
//if (state.Command == "alarmingNight"){sendIrisCmd (0x08)}// alarming Part


def playSound(cmd){
//   status = location.hsmStatus    
    if (state.Command == "armingNight" | state.Command == "armingHome"| state.Command == "armingAway" | state.Command == "alarmingNight"| state.Command == "alarmingAway" | state.Command == "alarmingHome"){
        logging ("${device} : Unable to Play Chimes. ${state.Command} overides chime.","warn")
        sendEvent(name: "status", value: "Inuse")
        return
        }

                 
  sound = "none" 
  if (cmd ==1){ sound = "KEYCLICK"}
  if (cmd ==2){ sound = "LOSTHUB"}
  if (cmd ==3){ sound = "ARMING"}
  if (cmd ==4){ sound = "ARMED"}
  if (cmd ==5){ sound = "HOME"}
  if (cmd ==6){ sound = "NIGHT"}
  if (cmd ==7){ sound = "ALARM"}
  if (cmd ==8){ sound = "PANIC"}
  if (cmd ==9){ sound = "BADPIN"}
  if (cmd ==10){ sound = "Door Chime"}  
  if (cmd ==11){ sound = "GAME"}
  if (cmd ==12){ sound = "CPU"}
  if (cmd ==13){ sound = "Error Tone"}  

    
  sendEvent(name: "soundName", value: sound)   
  sendEvent(name: "status", value: "playing")  
    
    if (cmd == 10){ 
         beep()
         pauseExecution(2000)
         sendEvent(name: "status", value: "stopped")
         device.deleteCurrentState("soundName") 
         return    
    }
 
soundCode(cmd)
    
    if (chimeTime > 10){ 
        logging ("${device} : Chime Delay ${chimeTime}","info")    
    pauseExecution(chimeTime)
    soundCode(0)
    }    
   
sendEvent(name: "status", value: "stopped") 
device.deleteCurrentState("soundName")     
}    

   

// The keypad needs to be notified of its state.
// Send the command
def sendIrisCmd (cmdI){
state.waiting = 0 // reset the poll timmer
cluster = 0x00C0
attributeId = 0x020
dataType = DataType.ENUM8
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, cmdI, [destEndpoint :0x02]))    
logging ("${device} : KeyPad set to [${state.Command}]","info")
} 

def stop(){
    soundCode(0x00)
    off()
}

def beep(){

   status = location.hsmStatus
   if (status != "disarmed" ){
        logging ("${device} : Unable to Play door Chimes. ${status} overides entry chime.","warn")
        sendEvent(name: "status", value: "Inuse")
        return
        }
    
    logging ("${device} : Door Chime/Beep >> Send KeyPad CMD:${state.Command}","info")
    SendState()
    
    
    
}    

def soundCodeOff(cmd){soundCode(0x00)}
def soundCode(cmd){
value = 0x00
    if (cmd == 1){value = 0x01}
    if (cmd == 2){value = 0x02}
    if (cmd == 3){value = 0x03}
    if (cmd == 4){value = 0x04}
    if (cmd == 5){value = 0x05}
    if (cmd == 6){value = 0x06}
    if (cmd == 7){value = 0x07}
    if (cmd == 8){value = 0x08}
    if (cmd == 9){value = 0x09}
    if (cmd == 11){value = 0x0C}// Undocumented GAME
    if (cmd == 12){value = 0x0D}// CPU
    if (cmd == 13){value = 0x0E}// Bad Pin
cluster = 0x00C0
attributeId = 0x26
dataType = DataType.ENUM8
 
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, value, [destEndpoint :0x02]))    
    logging ("${device} : Playing Sound code ${value} ","info")
}
//def on() {
//cluster = 0x00C0
//attributeId = 0x20
//dataType = DataType.ENUM8
////value = 0x01

//sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, value, [destEndpoint :0x02]))    
//   
//return
//}

def shock(){
sendEvent(name: "shock", value: "detected")    
logging ("${device} : Shock Detected","info")
 pauseExecution(6000)
logging ("${device} : Shock Clear","info")    
sendEvent(name: "shock", value: "clear")    
}


def tamper(){
sendEvent(name: "tamper", value: "detected")
logging ("${device} : Tamper Detected","info")
 pauseExecution(6000)
logging ("${device} : Tamper Clear","info")    
sendEvent(name: "tamper", value: "clear")
}

def push (buttonNumber){
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
    lqi = device.currentValue("lqi")
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






void refresh() {
    logging ("${device} : Refresh. Sending Hello to Device","info")

    
	def cmds = new ArrayList<String>()
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}")    // version information request
//	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 01 01} {0xC216}")    // power control operating mode nudge ( I dont think we need this)
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
                if(detectBadBat){
                 if(state.batteryOkay == false){
                  logging( "${device} : Auto Bat detection. was low now not present. set 0% ")
	              sendEvent(name: "batteryVoltage", value: 0, unit: "V")
     	          sendEvent(name: "battery", value:0, unit: "%")   
                 }
                }    
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
    clientVersion()
    updatePresence()
//    getSoftStatus(status)// for debuging
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
  
   
    if (map.clusterId == "0013"){
	logging("${device} : Device announce message (new device?)","warn")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Anouncing New Device?","debug")

    } else if (map.clusterId == "0006") {
      logging("${device} : Sending Match Descriptor Response","debug")
	  sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])

// Iris KeyPad report cluster 00C0  
    } else if (map.clusterId == "00C0") {
     if (map.command == "01") {
         // Reply to our sending commands in raw (undocumented) Wont be seen in normal use
         mode1 = receivedData[0]
         mode2 = receivedData[1]
         logging ("${device} : Error KeyPad Replied with CMD:[${mode1} ${mode2}] ","warn")
     }
     
// KeyPad does not know its state (running armming or alarming)                           
     if (map.command == "00" ) {
         state.waiting ++
         if (state.waiting == 5 | state.waiting==10 | state.waiting==15| state.waiting==20| state.waiting==25){logging ("${device} : KeyPad Polling for state Poll:${state.waiting} of ${state.delay}","info")} //Min logging
//         else {logging ("${device} : KeyPad Polling for state  Poll:${state.waiting} of ${state.delay}","debug")}
         if (state.waiting > state.delay){ // Correct lost state but wait out the delay
           getStatus(status) 
           SendState()
         }  
     }  
       
 // KeyPad return stats to us MAIN LOOP    
     if (map.command == "0A") {  
      PinEnclosed = receivedData[0]// The command being passed to us
      pinSize     = receivedData[3]// The PIN size     
      keyRec      = receivedData[4]// The Key pressed 
      keyRecAsc   = receivedData[4..4].collect{ (char)Integer.parseInt(it, 16) }.join()
	  buttonNumber = 0 
      status = "Unknown"   
	  size = receivedData.size()// size of data field
    if (PinEnclosed  =="28" ){ logging ("${device} : Action :[${status} MAP:${map.data}","error")}
// This indicates a error in mode change it should auto correct  
         
// action button pressed see if it has a IRIS command included-Logging Only
     if (PinEnclosed == "22"){ 
       if (size == 10){ 
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
           logging("${device} : Action :IRIS:[${irsCMD1}] [${nextirsCMD}]", "trace")} 

    }     
         

// Now check for our command buttons 
// ====================================PANIC ==================================  
     if (keyRec == "50"){
        if (PinEnclosed  =="22" ){logging ("${device} : Action :[Pressed PANIC] State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("${device} : Action :[Released PANIC] State:${state.Command}","debug")}
        
         if(state.Command != "panic"){
             logging("${device} : Action [Pressed PANIC]","info") 
             state.Command = "panic"
             state.delay = 50 // Time out
             panic()
             return
         }
       logging("${device} : Action [PANIC] I already sent cmd","debug") 
       return
   	  }    
// ====================================ON =====================================         
      if (keyRec == "41"){
        if (PinEnclosed  =="22" ){logging ("${device} : Action :[Pressed ON AWAY] Valid PIN:${state.validPIN} State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("${device} : Action :[Released ON AWAY] Valid PIN:${state.validPIN} State:${state.Command}","debug")}

        if (state.Command =="away" | state.Command =="armingAway" ){
          if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }
        logging("${device} : Action [ON] already sent:${OnSet} state:${state.Command}","debug")    
        return
        }
        
        if (requirePIN){ 
          if (state.validPIN == true){     
          MyarmAway() 
          return
          }
            
        }else{
        defaultLockCode()  
        MyarmAway()
        return
        }
        if (state.validPIN == false){logging("${device} : Invalid PIN Cant Arm","warn")}
        if (PinEnclosed  =="22" ){soundCode(13)} // beep once 
        return   
        }
//=============================================PARTIAL================================	 
//    PartSet =Arm Night,Arm Home         
     if (keyRec == "4E"){
        if (PinEnclosed  =="22" ){logging ("${device} : Action :[Pressed PARTIAL] ${PartSet} Valid PIN:${state.validPIN} State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("${device} : Action :[Released PARTIAL] ${PartSet} Valid PIN:${state.validPIN} State:${state.Command}","debug")}

          if (PartSet =="Arm Night"){          
		  if (state.Command =="night" | state.Command =="armingNight" ){
               if (state.received == false){
            MyarmNight() // send it again We got no reply
            return
            }
         
          logging("${device} : Action [PARTIAL] Ignored already sent:${PartSet} state${state.Command}","debug")  
          return 
          }
            
              if (requirePIN){
               if (state.validPIN == true){
               MyarmNight()
               return
               }
              }
              else{
              defaultLockCode()
              MyarmNight()
              return
              } 
         }
          if (PartSet =="Arm Home"){          
		  if (state.Command =="home" | state.Command =="armingHome" ){
            if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }  
          logging("${device} : Action [PARTIAL] Ignored already sent:${PartSet} state${state.Command}","debug")  
          return }
            
              if (requirePIN){
               if (state.validPIN == true){
               MyarmAway()
               return
               }
              }
              else{
              defaultLockCode()
              MyarmAway()
              return
              } 
         }   
      logging("${device} : Invalid PIN Ignoring","warn")   
      if (PinEnclosed  =="22" ){soundCode(13)} // beep once 
      return   
	 }          

//=============================================OFF==============================        
         if (keyRec == "48"){
          if (PinEnclosed  =="22" ){logging ("${device} : Action :[Pressed OFF] Valid PIN:${state.validPIN} State:${state.Command}","info")}
          if (PinEnclosed  =="23" ){logging ("${device} : Action :[Released OFF] Valid PIN:${state.validPIN} State:${state.Command}","debug")}
          if (device.currentValue("panic")== "on"){state.Command == "panic"}// Fix being out of sync 
            if (state.validPIN == true){              
             if (state.Command == "off" && PinEnclosed  =="23"){
             logging("${device} : Action [OFF] Ignored already sent state${state.Command}","debug")  
             return
             }
            MyDisarm()
            return
         }
             
         logging("${device} : Invalid PIN Ignoring","warn")
         if (PinEnclosed  =="22" ){soundCode(13)} // beep once     
         return  
	 }         
         
         


 //     StarSet =Disabled,Arm Night,Arm Home,Arm Away
         
     if (keyRec == "2A"){
      if (PinEnclosed  =="22" ){logging ("${device} : Action :[Pressed * STAR] ${StarSet} Valid PIN:${state.validPIN} State:${state.Command}","info")}
      if (PinEnclosed  =="23" ){logging ("${device} : Action :[Released * STAR] ${StarSet} Valid PIN:${state.validPIN} State:${state.Command}","debug")}

      if (StarSet == "Arm Home"){
		 if (state.Command =="home" | state.Command =="armingHome" ){
          if (state.received == false ){
          MyarmHome() 
          return
          }
         logging("${device} : Action [STAR] Ignored already sent:${StarSet} state${state.Command}","debug")
         return }
         
              if (requirePIN){
               if (state.validPIN == true){
               MyarmHome()
               return
               }
              }else{
              defaultLockCode()    
              MyarmHome()
              return
              }    
         }
       if (StarSet == "Arm Night"){
		 if (state.Command =="night"| state.Command =="armingNight" ){
             if (state.received == false ){
            MyarmNight() // send it again We got no reply
            return
            } 
             
         logging("${device} : Action [STAR] Ignored already sent:${StarSet} state${state.Command}","debug")
         return }
         
              if (requirePIN){
               if (state.validPIN == true){
               MyarmNight()
               return
               }
              }
              else{
              defaultLockCode()    
              MyarmNight()
              return
              }    
         } 
        if (StarSet == "Arm Away"){
		 if (state.Command =="away" | state.Command =="armingAway" ){
              if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }
         logging("${device} : Action [STAR] Ignored already sent:${StarSet} state${state.Command}","debug")
         return }
         
              if (requirePIN){
               if (state.validPIN == true){
               MyarmAway()
               return
               }
              }
              else{
              defaultLockCode()    
              MyarmAway()
              return
              }    
         }     
     
     // disabled
     logging("${device} :${StarSet} Valid PIN:${state.validPIN} Ignoring","info")   
     if (PinEnclosed  =="22" ){soundCode(13)} // beep once 
     return
     }        
        
      
        
        
//     PoundSet =Disabled,Arm Night,Arm Home,Arm Away
         
     if (keyRec == "23"){
         
        if (PinEnclosed  =="22" ){logging ("${device} : Action :[Pressed # POUND] ${PoundSet} Valid PIN:${state.validPIN} State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("${device} : Action :[Released # POUND] ${PoundSet} Valid PIN:${state.validPIN} State:${state.Command}","debug")}
    
      if (PoundSet == "Arm Home"){
		 if (state.Command =="home" | state.Command =="armingHome" ){
              if (state.received == false ){
            MyarmHome() // send it again We got no reply
            return
            }
         logging("${device} : Action [POUND] Ignored already sent:${PoundSet} state:${state.Command}","debug")
         return 
         }

         if (requirePIN){
           if (state.validPIN == true){
           MyarmHome()
           return
           }
         }
              else{
              defaultLockCode()
              MyarmHome()
              return
              }    
         }
       if (PoundSet == "Arm Night"){
		 if (state.Command =="night" | state.Command =="armingNight" ){
              if (state.received == false ){
            MyarmNight() // send it again We got no reply
            return
            }
         logging("${device} : Action [POUND] Ignored already sent:${PoundSet} state:${state.Command}","debug")
         return
         }
         
              if (requirePIN){
               if (state.validPIN == true){
               MyarmNight()
               return
               }
              }
              else{
              defaultLockCode()    
              MyarmNight()
              return
              }   
         } 
        if (PoundSet == "Arm Away"){
		 if (state.Command =="away" | state.Command =="armingAway"  ){
              if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }
       logging("${device} : Action [POUND] Ignored already sent:${PoundSet} state:${state.Command}","debug")
         return 
         }
              if (requirePIN){
               if (state.validPIN == true){
               MyarmAway()
               return
               }
              }
              else{
              defaultLockCode()   
              MyarmAway()
              return
              }   
         }     
     
     logging("${device} : ${PoundSet} Valid PIN:${state.validPIN} Ignoring","info")   
     if (PinEnclosed  =="22" ){   soundCode(13)}     
     return
     } 

          

//      PinEnclosed = receivedData[0]// 21 = pin
//      pinSize     = receivedData[3]// The PIN size + 4 = size   
    if (PinEnclosed == "21" ){
     
     if (pinSize != "01" ){// To small for a PIN go to buttons
        pinSize = receivedData[3]
        // 4 - 15 digit Pin size supported 
        size = receivedData.size() 
        state.validPIN = false
        state.PinName = "NA"
        state.PIN     = "NA"
        state.pinN    = 0 
        asciiPin = "NA"
        end = size -1 
         asciiPin = receivedData[4..end].collect{ (char)Integer.parseInt(it, 16) }.join()
      if (device.currentValue("code1") == asciiPin){
          name = device.currentValue("code1n")
          state.validPIN = true 
          state.pinN = 1
	   }	     
      if (device.currentValue("code2") == asciiPin){
          name = device.currentValue("code2n")
      	  state.validPIN = true
          state.pinN = 2
      }
      if (device.currentValue("code3") == asciiPin){
          name = device.currentValue("code3n")
          state.validPIN = true
          state.pinN = 3
      }
	  if (device.currentValue("code4") == asciiPin){
          name = device.currentValue("code4n")
          state.validPIN = true
          state.pinN = 4
      }
      if (device.currentValue("code5") == asciiPin){
          name = device.currentValue("code5n")
          state.validPIN = true
          state.pinN = 5
      }  
      if (secure == asciiPin){
          name ="master"
          state.validPIN = true
          state.pinN = 100
      }  

        if (state.validPIN == true){
          state.PinName = name
          state.PIN     = asciiPin 
          logging("${device} : Action :[PIN] Name:${name} Pin:${asciiPin} Size:${pinSize} [Waiting for Action CMD] State:${state.Command}","info")
          sendEvent(name: "lastCodeName", value: "${state.PinName}", descriptionText: "${state.PinName} [${state.PIN}]")// May be needed by other aps.
          sendEvent(name: "lastCodePIN",  value: "${state.PIN}",     descriptionText: "${state.PinName} [${state.PIN}]")
          runIn(90, "purgePIN")// Purge time increased to allow button repeating to finish
     	  return  
        }   
      soundCode(13)
      logging("${device} : Action :[PIN] Pin:${asciiPin} Size:${pinSize} [Invalid PIN] State:${state.Command}","warn")
      if(tamperPIN){
      tamper()
      state.PinName = "Tamper"
      }
      else {// if not using tamper use shock alarm
          shock()
      state.PinName = "Shock"
      } 
      state.validPIN = false
      state.PIN     = asciiPin 
      sendEvent(name: "lastCodeName", value: "${state.PinName}", descriptionText: "${state.PinName} [${state.PIN}]")// May be needed by other aps.
      sendEvent(name: "lastCodePIN",  value: "${state.PIN}",     descriptionText: "${state.PinName} [${state.PIN}]")
      return	 
    }// Pin size check  
    }// end pin check       

// Keypad button matrix 
     
if (keyRec == "31"){push(1)}
if (keyRec == "32"){push(2)}
if (keyRec == "33"){push(3)}
if (keyRec == "34"){push(4)}
if (keyRec == "35"){push(5)}
if (keyRec == "36"){push(6)}
if (keyRec == "37"){push(7)}
if (keyRec == "38"){push(8)}
if (keyRec == "39"){push(9)}
if (keyRec == "30"){push(10)}

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
     batteryVoltage = batteryVoltage.setScale(2, BigDecimal.ROUND_HALF_UP)
     logging("${device} Raw Battery  Bat:${batRec} ${batteryVoltage}", "trace")    
 
// I base this on Battery discharge curves(may need adjustments)
// Normal batteries slowely discharge others have a sudden drop          
// Iris source code says 2.1 is min voltage   
		BigDecimal batteryVoltageScaleMin = 2.10
		BigDecimal batteryVoltageScaleMax = 3.00	    
	    
	    if (BatType == "NiCad"){ // < 1.2x2=2.2 drops out fast
		batteryVoltageScaleMin = 2.19
		batteryVoltageScaleMax = 3.00	    
	    } 
        if (BatType == "NiMH"){ // < 1.2x2=2.2 drops out fast
		batteryVoltageScaleMin = 2.19
		batteryVoltageScaleMax = (1.35 * 2)	    
	    }    

	    if (BatType == "Lithium"){// < 1.25x2=2.5 drops out fast 
		batteryVoltageScaleMin = 2.50
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
            
         if (batteryPercentage < 20) {
             logging("${device} : Battery LOW : $batteryPercentage%", "debug")
             sendEvent(name: "batteryState", value: "low")
             state.batteryOkay = false
         }
  
	 if (batteryPercentage < 10) {
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
       logging("${device} : >>>--reboot--<<< Keypad is rebooting ","error")// We need to know its rebooted in case of crash.
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
	if(reportFirm == "unknown"){state.reportToDev="Report Unknown firmware [${deviceFirmware}] " }
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

// this did not work on the keypad.
void sendZigbeeCommands(List<String> cmds) {

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
