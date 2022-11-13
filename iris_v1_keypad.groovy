/* Iris v1 KeyPad Driver
Hubitat Iris v1 KeyPad driver 
Supports keypad disarm arm functions (all modes)
Works with Lock Code Manager (4-15 digit Pin size supported)
Works with Hubitat® Safety Monitor
Plays chime codes and alarm strobe
Button Controller support to map coomands to buttons 1-0

  _____ _____  _____  _____        __    _  __                          _ 
 |_   _|  __ \|_   _|/ ____|      /_ |  | |/ /                         | |
   | | | |__) | | | | (___   __   _| |  | ' / ___ _   _ _ __   __ _  __| |
   | | |  _  /  | |  \___ \  \ \ / / |  |  < / _ \ | | | '_ \ / _` |/ _` |
  _| |_| | \ \ _| |_ ____) |  \ V /| |  | . \  __/ |_| | |_) | (_| | (_| |
 |_____|_|  \_\_____|_____/    \_/ |_|  |_|\_\___|\__, | .__/ \__,_|\__,_|
                                                   __/ | |                
                                                  |___/|_|   

Must set keypad in (Hubitat® Safety Monitor) and (Lock Code manager) for it to work 

=================================================================================================
  v7.0.2 11/12/2022 Another bug fix for presence
  v7.0.0 11/11/2022 Rewrote logging code (smaller code size) Added improvements from my iris drivers
  v6.9.0 10/30/2022 Bug fix in presence routine was not giving warning before timing out.
  v6.8.5 10/10/2022 Bat 2 detection removed more work needed.
  v6.8.4 10/10/2022 Cancel/Disarm code rewritten. Switch capability added. Routines can now turn off/on
   alarm by switch. 2 new tones added depends on your firmware if they work. 
   Rewrote HSM send cmd code. Rewrote HSM delay code.
   Changes in arming from keypad now sends arm to HSM and waits for HSM to send ARM. 
   Was arming first. Should help keep the countdown timer in sync with slugish hubs.
   BadPin tone setting added. 

  v6.8.2 09/30/2022 STROBE added to alarm setting to fix alt firmware problems. 
  v6.8.1 09/30/2022 Fingerprint adjusted Finaly have autopair working 
   Firmware 2012-06-11 does not autorepeate the alarm tone fixed. Redirected to strobe.
   if your firmware doesnt repeat alarm tones I need to know. 
  v6.8   09/24/2022 Pin code logging bug fixed. Reduce bat events
  v6.7.1 09/22/2022 New presence routine
  v6.7   09/21/2022 Ranging Adjustments
  v6.6   09/19/2022 Rewrote logging routines.
  v6.5   09/17/2022 Increased database to 10 PINS
  v6.4   09/16/2022 Allow HSM to send more than 1 off cmd. Bug in panic timeout fixed.
  Rewrite of ranging code. Rewrite of battery logging code. Updating iris block code on all my iris drivers.
  Pin logging rewritten. Hide pin from log.
  v6.3   09/10/2022 Reversing v.61 update
  v6.2   09/04/2022 Updating iris block code on all my iris drivers.
  v6.1   09/03/2022 ---
  v6.0   09/02/2022 Changed Init boot up routines. Trying to prevent Beep on reboot.
                    Enrole Request detect added.
  v5.9   08/26/2022 New iris cluster seen on the mesh, added detection for it.
  v5.8   08/09/2022 Fixed looping if 2 alarm commands sent.
  v5.7   06/28/2022 Stop runaway countdown if disarmed by app during entry.
                    Added more events to logs
  v5.6   04/28/2022 Added command to unschedule pausing cron for debuging.
  v5.5   04/18/2022 Fine Tunning disarm during entry. Chimes countdown sometimes didnt stop.
                    Hide PINS from the info log for security
  v5.4   04/12/2022 More adj to alkaline batt lasting longer than expected.
  v5.3   03/12/2022 Battery routine and log adjustments.
  v5.2   02/22/2022 Added new firmware detect
  v5.1   01/18/2022 Minor cosmetic changes
  v5.0   11/25/2021 Bug in arm home was armingaway
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

Eratic Operation:
It is possible for the cpu to become corupted and cause problems such as 
countdown timmer not stopping not taking commands and missing sounds.
I saw this in testing when sending lots of diffrent tones confusing the unit. 
The solution is to reboot the pad by removing batteries. 


Please Note:
If you have problems with the lock code manager erase all your pins manualy using the driver. Do not reinstall LCM
There is no error checking in LCM it will dupe and corrup PINS. Causing it to crash.
I have added Error corection to prevent corrupting my pin storage. 
It is possible to use this driver without LCM just add pins using the driver.



Total worktime to build up to v2 2 days. Have fun.
 
FCC ID:FU5TSA04 https://fccid.io/FU5TSA04
Built by Everspring Industry Co Ltd Smart Keypad TSA04            


I wrote this for my keyboards you are welcome to use it.
================================================================================================
Tested on Firmware 2013-06-28 and 2012-12-11 Only 3 known firmware versions exist

https://github.com/tmastersmart/hubitat-code


Post your comments here. 
http://www.winnfreenet.com/wp/2021/09/iris-v1-keyboard-driver-for-hubitat/

* To Reset Device:
 *    Insert two batteries side-by-side at one end or the other and then press "ON" button device 5 times (8 times?) within the first 10 seconds.
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
Used forked code bellow from a contact/motion switch. This included
ranging code and some detection code and zigbee sending commans.

All added code is orginal code written by me. Keypad routines were back engerned by me
and commands to control keypad were created by trial and error. Lock code storage code
was created to replace the copyrighted hubitat code. I created my own button code to use
with the button manager. HSM connection code created from scratch since no docs exist.
Lock code manager reverse engenered since no docs exist for it and it crashes easialy.
This code recreates how the keypad functioned on Iris,
I beleive it even works better because of the extra custom options I have added.

This code looks diffrent because I learned to program on a Timex Sinclair ZX81 and a Commorore C64
READY.
poke 53280,0 
poke 53281,0


 * Includes some opensource code from https://github.com/birdslikewires/hubitat
   On how to control Iris devices. Much has been rewritten but some orginal
   code is still contained in these routines. This would have not been possible
   without this code.

*  Used info from IRIS V2 source code in another language. Now called Arcus
   This code would not work here and was very hard to follow through many subroutines. 
   Much of it was never decyphered. But gained sound control cmds.
   https://github.com/arcus-smart-home/arcusplatform/
   This code helped with controling tones setting OFF on buttons.
  


GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.

 */
def clientVersion() {
    TheVersion="7.0.2"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
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
capability "Switch"


command "checkPresence"
command "normalMode"
command "rangeAndRefresh"
command "entry" 

command "SendState"
command "getStatus"
command "unschedule"        
command "installed"        

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
attribute "code6", "string"
attribute "code6n", "string"
attribute "code7", "string"
attribute "code7n", "string"
attribute "code8", "string"
attribute "code8n", "string"
attribute "code9", "string"
attribute "code9n", "string"
attribute "code10", "string"
attribute "code10n", "string"        
        
    
attribute "lockCodes", "string"	

// HUB has hardwired any iris device with 00C0 as a care fob so we have to fake carefob fingerprint.
fingerprint profileId: "C216", endpointId:"02", inClusters:"00F0,00C0",           outClusters: "00C0", manufacturer: "Iris/AlertMe", model: "KeyPad Device", deviceJoinName: "Iris V1 Keypad"
fingerprint profileId: "C216", endpointId:"02", inClusters:"00F0,00C0,00F3,00F5", outClusters: "00C0", manufacturer: "Iris/AlertMe", model: "KeyPad Device", deviceJoinName: "Iris V1 Keypad"
	}
}



preferences {
	
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true, required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false, required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false, required: true

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

    input name: "AlarmTone",type:"enum", title: "Alarm Tone",description: "Customize Alarm Tone. Some firmware may only work with STROBE", options: ["STROBE","KEYCLICK","LOSTHUB","ARMING","ARMED","HOME","NIGHT","ALARM","PANIC","OPENDOOR","LOCKED","BADPIN","GAME","CPU"], defaultValue: "STROBE",required: true
    input name: "BadPinTone",type:"enum",title: "Bad Pin Tone",description: "Customize Bad Pin Tone. some firmware may not play all tones", options: ["ERROR","BADPIN","GAME","CPU","LOCKED","LOSTHUB"], defaultValue: "ERROR",required: true

    input("chimeTime",  "number", title: "Chime Timeout", description: "Chime Timeout timer. Sends stop in ms 0=disable",defaultValue: 5000,required: true)
    
    input("secure",  "text", title: "Master password", description: "4 to 11 digit Overide PIN. Not stored in Lock Code Manager Database 0=disable",defaultValue: 0,required: false)


}


def installed() {	// Runs after first pairing. 
    infoLogging=true
    debugLogging=false
    traceLogging=false
	logging("Paired!", "info")
    loggingUpdate()
    initialize()
    getStatus()
}

 
def initialize() {
/// Runs on reboot also    
logging("initialize", "info")
// Set defaults they will be updated by HSM
// Survive a reboot    
if (!state.armNightDelay){state.armNightDelay = 30}
if (!state.armHomeDelay){state.armHomeDelay  = 30}
if (!state.delayEntry){state.delayEntry    = 30}
if (!state.delayExit){state.delayExit     = 30}
if (!state.Command){state.Command = "unknown"} 
state.delay = 40 
state.waiting = 0    
state.message = "Enable [${device}] in HSM"
state.batteryOkay = true
state.Panic = false
state.presenceUpdated = 0
state.rangingPulses = 0
state.validPIN = false  
state.PinName = "NA"
state.PIN = "NA"
    
state.message = "Keypad supports 14 diffrent chimes under play command. Not supported on all Firmware"
    
sendEvent(name: "operation", value: "normal", isStateChange: false)
sendEvent(name: "presence", value: "present", isStateChange: false)
sendEvent(name: "numberOfButtons", value: "12", isStateChange: false)
sendEvent(name: "maxCodes", value:10)
sendEvent(name: "codeLength", value:15)

sendEvent(name: "tamper", value: "clear")
sendEvent(name: "status", value: "ok")    
offEvents()    
  
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
state.remove("AltTones")
    
removeDataValue("image")
device.deleteCurrentState("alarm")    
device.deleteCurrentState("pushed") 
device.deleteCurrentState("pin")     
device.deleteCurrentState("lockCodes")
device.deleteCurrentState("HSMAlert")    
  
// Stagger init refreshes On reboot
randomSixty = Math.abs(new Random().nextInt() % 60)
logging("refresh in ${randomSixty}sec", "info")
runIn(randomSixty,refresh)    
randomSixty = Math.abs(new Random().nextInt() % 60)
logging("getStatus in ${randomSixty}sec", "info")    
runIn(randomSixty,getStatus)
randomSixty = Math.abs(new Random().nextInt() % 60)
logging("getCodes in ${randomSixty}sec", "info")    
runIn(randomSixty,getCodes)    
//clientVersion()
}


def configure() {
	initialize()
    state.Config = false
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("batteryOkay")
    state.remove("batteryState") 
    state.remove("armMode")
    
//    updateDataValue("inClusters", "00F0,00C0,00F3,00F5")
//    updateDataValue("outClusters", "00C0")
    
	unschedule()

	// Schedule random ranging in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${8} * * ? *", rangeAndRefresh)	
    logging("Configure - Ranging Every 8hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty} ", "info") 
 
    // Check presence in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *", checkPresence)	
    logging("Configure - checkPresence Every hr starting at ${randomTwentyFour}:${randomSixty}:${randomSixty} ", "info")    
    runIn(randomSixty,rangeAndRefresh)
//	runIn(12,normalMode)
}
def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
    loggingUpdate()
    getIcons()
    refresh()
}
// Sample Hubitat pin store code is not opensource
// My custom pin store code
def setCode(code,pinCode,userCode){
    size = pinCode.size() 
    if (size < 4 || size >15){
        logging( "Invalid PIN size :${size} Rejected","warn")
        return
    }
	if (code == 1){ save= "code1";}
	if (code == 2){ save= "code2";}
	if (code == 3){ save= "code3";}
	if (code == 4){ save= "code4";}	
    if (code == 5){ save= "code5";}
    if (code == 6){ save= "code6";}
	if (code == 7){ save= "code7";}
	if (code == 8){ save= "code8";}
	if (code == 9){ save= "code9";}	
    if (code == 10){save= "code10";}
	if (code < 11){
        saveit = true
        // stop dupes from lock manager
        if (device.currentValue("code1") == pinCode){saveit = false} 
        if (device.currentValue("code2") == pinCode){saveit = false}
        if (device.currentValue("code3") == pinCode){saveit = false} 
        if (device.currentValue("code4") == pinCode){saveit = false}        
        if (device.currentValue("code5") == pinCode){saveit = false}
        if (device.currentValue("code6") == pinCode){saveit = false} 
        if (device.currentValue("code7") == pinCode){saveit = false}
        if (device.currentValue("code8") == pinCode){saveit = false} 
        if (device.currentValue("code9") == pinCode){saveit = false}        
        if (device.currentValue("code10") == pinCode){saveit = false} 
          logging( "ADD code#${code} PIN:${pinCode} User:${userCode} [OK to save:${saveit}]","debug")
          logging( "ADD code#${code} PIN:XXXX User:${userCode}","info") 
        if (saveit){    
          logging( "Saving ...${save}...","debug")        
          logging( "Saving User:${userCode}","info")
	      sendEvent(name: "${save}", value: pinCode)
	      sendEvent(name: "${save}n",value: userCode)
    
        }
	}
     pauseExecution(3000) // wait for database
     getCodes() 
     sendEvent(name:"codeChanged",value:"added",data:"${codeStore}", isStateChange: true, descriptionText: "code:${pinCode} name:${userCode}" )
     pauseExecution(3000) // wait for database
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
    store6 = device.currentValue("code6")
    store6n= device.currentValue("code6n")
    store7 = device.currentValue("code7")
    store7n= device.currentValue("code7n")    
    store8 = device.currentValue("code8")
    store8n= device.currentValue("code8n")    
    store9 = device.currentValue("code9")
    store9n= device.currentValue("code9n")    
    store10 = device.currentValue("code10")
    store10n= device.currentValue("code10n")
}
def deleteCode(code) { 
    if (code == 1){ save= "code1"}
	if (code == 2){ save= "code2"}
	if (code == 3){ save= "code3"}
	if (code == 4){ save= "code4"}	
	if (code == 5){ save= "code5"}	  
    if (code == 6){ save= "code6"}
	if (code == 7){ save= "code7"}
	if (code == 8){ save= "code8"}
	if (code == 9){ save= "code9"}	
	if (code == 10){ save= "code10"}    
    thecode = device.currentValue("${save}")
    thename = device.currentValue("${save}n")
    logging ("deleteCode  #${code}   code:${thecode} name:${thename}","debug")    
    logging ("deleteCode  #${code}   code:XXXX name:${thename}","info") 
	if (code < 11) {
     device.deleteCurrentState("${save}")    
     device.deleteCurrentState("${save}n")
     pauseExecution(3000) // wait for database
     getCodes() 
     sendEvent(name:"codeChanged",value:"deleted",data:"${codeStore}", isStateChange: true, descriptionText: "code:${thecode} name:${thename} ")
     pauseExecution(3000) // wait for database
     getCodes()    
}
}
// Scan all the pins and return stat
void checkThePin(code)
{
        state.validPIN = false
        state.PinName = "none"
        state.pinN    = 0 
        
        
      if (device.currentValue("code1") == state.PIN){
          state.PinName = device.currentValue("code1n")
          state.validPIN = true 
          state.pinN = 1
	   }	     
      if (device.currentValue("code2") == state.PIN){
          state.PinName = device.currentValue("code2n")
      	  state.validPIN = true
          state.pinN = 2
      }
      if (device.currentValue("code3") == state.PIN){
          state.PinName = device.currentValue("code3n")
          state.validPIN = true
          state.pinN = 3
      }
	  if (device.currentValue("code4") == state.PIN){
          state.PinName = device.currentValue("code4n")
          state.validPIN = true
          state.pinN = 4
      }
      if (device.currentValue("code5") == state.PIN){
          state.name = device.currentValue("code5n")
          state.validPIN = true
          state.pinN = 5
      } 
         
      if (device.currentValue("code6") == state.PIN){
          state.PinName= device.currentValue("code6n")
          state.validPIN = true 
          state.pinN = 6
	   }	     
      if (device.currentValue("code7") == state.PIN){
          state.PinName = device.currentValue("code7n")
      	  state.validPIN = true
          state.pinN = 7
      }
      if (device.currentValue("code8") == state.PIN){
          state.PinName = device.currentValue("code8n")
          state.validPIN = true
          state.pinN = 8
      }
	  if (device.currentValue("code9") == state.PIN){
          state.PinName = device.currentValue("code9n")
          state.validPIN = true
          state.pinN = 9
      }
      if (device.currentValue("code10") == state.PIN){
          state.PinName = device.currentValue("code10n")
          state.validPIN = true
          state.pinN = 10
      }  
          if (secure == state.PIN){
          state.PinName ="master"
          state.validPIN = true
          state.pinN = 100
      }  
  
}
// Create jason code for lock manager
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
    if (device.currentValue("code5")) {
        code = device.currentValue("code5")
        name = device.currentValue("code5n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode5 = "${qt}5${end}"
        if (needsComma == true){ setCode5 = ",${setCode5}"} 
        needsComma = true 
    }
         if (device.currentValue("code6")) {
        code = device.currentValue("code6")
        name = device.currentValue("code6n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode6 = "${qt}6${end}"
        if (needsComma == true){ setCode6 = ",${setCode6}"}      
        needsComma = true 
    }
    if (device.currentValue("code7")) {
        code = device.currentValue("code7")
        name = device.currentValue("code7n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode7 = "${qt}7${end}"
        if (needsComma == true){ setCode7 = ",${setCode7}"}
        needsComma = true                
    }    
    if (device.currentValue("code8")) {
        code = device.currentValue("code8")
        name = device.currentValue("code8n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode8 = "${qt}8${end}"
        if (needsComma == true){ setCode8 = ",${setCode8}"}
        needsComma = true 
    }
    if (device.currentValue("code9")) {
        code = device.currentValue("code9")
        name = device.currentValue("code9n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode9 = "${qt}9${end}"
        if (needsComma== true){ setCode9 = ",${setCode9}"} 
        needsComma = true 
    }
    if (device.currentValue("code10")) {
        code = device.currentValue("code10")
        name = device.currentValue("code10n")
        end = "${qt}:{${qt}name${qt}:${qt}${name}${qt},${qt}code${qt}:${qt}${code}${qt}}"
        setCode10 = "${qt}10${end}"
        if (needsComma == true){ setCode10 = ",${setCode10}"} 
    }
    codeStore= "{"
    if (setCode1) { codeStore = "${codeStore}${setCode1}" }
    if (setCode2) { codeStore = "${codeStore}${setCode2}" }
    if (setCode3) { codeStore = "${codeStore}${setCode3}" }
    if (setCode4) { codeStore = "${codeStore}${setCode4}" }              
    if (setCode5) { codeStore = "${codeStore}${setCode5}" }
    if (setCode6) { codeStore = "${codeStore}${setCode6}" }
    if (setCode7) { codeStore = "${codeStore}${setCode7}" }
    if (setCode8) { codeStore = "${codeStore}${setCode8}" }
    if (setCode9) { codeStore = "${codeStore}${setCode9}" }              
    if (setCode10){ codeStore = "${codeStore}${setCode10}"}
    codeStore = "${codeStore}}"
    sendEvent(name: "lockCodes",value: codeStore)
    // Dont show codes in normal log for security
    logging("lockCode Database ${codeStore}", "trace")          
    logging("getCodes (Lockcode Database rebuilt)", "info")
 }

// using state.delayExit 
void setExitDelay(Map delays){
    state.delayExit = (delays?.awayDelay ?: 0).toInteger()
    state.armNightDelay = (delays?.nightDelay ?: 0).toInteger()
    state.armHomeDelay = (delays?.homeDelay ?: 0).toInteger()
    state.delay = state.delayExit
    logging("setExitDelay ${delays}", "info")
}
def setEntryDelay(cmd){
    state.delayEntry = (cmd ?: 0).toInteger()
    logging("setEntryDelay ${cmd}", "info")
    }

def setExitDelay(cmd){
    logging("setExitDelay ${cmd} Not used", "debug")
}

def setCodeLength(cmd){
    logging("setCodeLength ${cmd} ignored we us 15", "debug")
}

// Arming commands
//hsmSetArm = armAway ,armHome,armNight,disarm,disarmAll,armAll,CancelAlerts
//subscribe (location, "hsmStatus", statusHandler)
//subscribe (location, "hsmAlerts", alertHandler)



// For calling delayed arming.
// if disarmed during countdown dont arm. 
def setAway(){
if (state.Command == "off"){
    logging ("Disarmed while arming. Abort","warn")
    SendState()
    runIn(1,getSoftStatus)
    return}    
state.Command = "away"
SendState()
 runIn(40,getSoftStatus) 
}
def setHome(){
if (state.Command == "off"){
    logging ("Disarmed while arming. Abort","warn")
    SendState()
    runIn(1,getSoftStatus)
    return}     
state.Command = "home"
SendState()
 runIn(40,getSoftStatus)   
}
def setNight(){
if (state.Command == "off"){
    logging ("Disarmed while arming. Abort","warn")
    SendState()
    runIn(1,getSoftStatus)
    return}     
state.Command = "night"
SendState()
 runIn(40,getSoftStatus)    
}

void setPartialFunction(cmd){
logging ("HSM sent >> ${cmd}","warn")
}
// =====================Incomming command==HSM RECEIVED =======================================
def armAway(cmd){
    delay = (cmd ?: 0).toInteger()
    if (delay != state.delayExit){
      state.delayExit = delay 
      logging ("Received >> new Delay:${state.delayExit} for delayExit", "info")
    }
    state.received = true
    if (state.Command == "armingAway" | state.Command == "away" ){
    logging ("Ignored HSM CMD [ArmAWAY] State:${state.Command}","info")
    return
    }
    MakeLockCodeIN()
    state.delay = state.delayExit // state.delay used by the timeout loop also
    sendEvent(name: "securityKeypad",value: "armed away",data: lockCode, type: "digital",descriptionText: "armed away [digital] Delay:${state.delayExit}")
    logging ("Received >> [ArmAWAY] Delay:${state.delayExit}  Our State:${state.Command}  [digital] SilentArm${SilentArmAway}", "info")
    state.Command = "armingAway"
    if (SilentArmAway == false){SendState()}
    runIn(state.delay,setAway)
}

def armHome(cmd){
    delay = (cmd ?: 0).toInteger()
    if (delay != state.armHomeDelay){
    state.armHomeDelay = delay    
    logging ("Received >> new Delay:${state.armHomeDelay} for armHomeDelay", "info")
    }
    state.received = true
    if (state.Command == "armingHome" | state.Command == "home"){
    logging ("Ignored HSM CMD [ArmHOME] State:${state.Command}","info")
    return
    }
    MakeLockCodeIN()
    state.delay = state.armHomeDelay 
    sendEvent(name: "securityKeypad",value: "armed home",data: lockCode, type: "digital",descriptionText: "armed home [digital] Delay:${state.armHomeDelay}")
    logging ("Received >> [ArmHome] Delay:${state.armHomeDelay}  Our State:${state.Command}  [digital] SilentArm${SilentArmHome}", "info")
    state.Command = "armingHome"
    if (SilentArmHome == false){SendState()}
    runIn(state.delay,setHome)
    return
}
def armNight(cmd){
    delay = (cmd ?: 0).toInteger()
    if (delay != state.armNightDelay){
    state.armNightDelay = delay    
    logging ("Received >> new Delay:${state.armNightDelay} for armNightDelay", "info")
    }
    state.received = true
    if (state.Command == "armingNight" | state.Command == "night"){
        logging ("Ignored HSM CMD [ArmNight] State:${state.Command}","info")
        return
    }
    MakeLockCodeIN()
    state.delay = state.armNightDelay // state.delay used by the timeout loop also
    sendEvent(name: "securityKeypad",value: "armed night",data: lockCode, type: "digital",descriptionText: "armed night [digital] Delay:${state.armNightDelay}")
    logging ("Received >> [ArmNight] Delay:${state.armNightDelay}  Our State:${state.Command}  [digital] SilentArm${SilentArmNight}", "info")
    state.Command = "armingNight"
    if (SilentArmNight == false){SendState()}
    runIn(state.delay,setNight)
}

// HUB says DISARM
def disarm(cmd) { 
    cmd = 0
    state.received = true
    offEvents()
   if (state.Command == "off" ){ // Ignore dupes cause a beep
        logging ("HSM CMD [disarm] received but we are State:${state.Command}","info")
        return
     }    
    MakeLockCodeIN()
    sendEvent(name: "securityKeypad",value: "disarmed",data: lockCode, type: "digital",descriptionText: "disarmed [digital]")
    logging ("Received >> [disarm]  Our State:${state.Command}  [digital] ", "info")
    state.Command = "off"
    SendState()
    runIn(15,getSoftStatus)
}
def offEvents(){
    alarmTest = device.currentValue("alarm")   
    if(alarmTest != "off"){sendEvent(name: "alarm",  value: "off")}
    alarmTest = device.currentValue("siren") 
    if(alarmTest != "off"){sendEvent(name: "siren",  value: "off")} 
    alarmTest = device.currentValue("strobe") 
    if(alarmTest != "off"){sendEvent(name: "strobe", value: "off")}
    alarmTest = device.currentValue("panic") 
    if(alarmTest != "off"){sendEvent(name: "panic",  value: "off")} 
    alarmTest = device.currentValue("switch")
    if(alarmTest != "off"){sendEvent(name: "switch", value: "off")} 
    sendEvent(name: "status", value: "off")  
}
def softOFF(){
  logging ("Alarm OFF ","info")  
  offEvents() 
  getStatus() // Are we armed or not Reset state
}

def setNA(){// Alarm Part button
    if (state.Command == "off"){
    logging ("Disarmed during Entry. Abort","warn")
    SendState()
    runIn(1,getSoftStatus)    
    return
    } 
    state.Command ="alarmingNight"
    SendState() 
    logging ("Alarm on ","info")
    sendEvent(name: "strobe",value: "on", displayed: true) 
    sendEvent(name: "alarm", value: "on", displayed: true)
    sendEvent(name: "switch",value: "on", displayed: true)
    runIn(60,softOFF) // Times out the alarm to save bat. 1 min  
}

def setAA(){// Alarm ON button
    if (state.Command == "off"){
    logging ("Disarmed during Entry. Abort","warn")
    SendState()
    runIn(1,getSoftStatus)   
    return} 
    
    state.Command ="alarmingAway"    
    SendState()
    logging ("Alarm on ","info")
    sendEvent(name: "strobe",value: "on", displayed: true) 
    sendEvent(name: "alarm", value: "on", displayed: true)
    sendEvent(name: "switch",value: "on", displayed: true)
    runIn(60,softOFF) // Times out the alarm to save bat. 1 min  
}
//=========================================ENTRY====================================================
def entry(cmd){
    delay = (cmd ?: 0).toInteger()
    if (delay != state.delayEntry){
    state.delayEntry = delay    
    logging ("Received >> new Delay:${state.delayEntry} for delayEntry", "info")
    }
    sendEvent(name: "securityKeypad",value: "Entry",data: lockCode, type: "digital",descriptionText: "Entry delay:${state.delayEntry} state:${state.Command}")

    if (state.Command == "night"){
        state.Command = "armingNight"
        if (state.delayEntry >5){SendState()}//dont do a short countdown
        runIn(state.delayEntry+1,setNA) // Night ALARM
    }
    if (state.Command == "home"){ 
        state.Command = "armingHome"
        if (state.delayEntry >5){SendState()}//dont do a short countdown
        runIn(state.delayEntry+1,setAA)// ALARM
    }
    if (state.Command == "away" | state.Command == "off"){//assume away even if we are off 
        state.Command = "armingAway"
        if (state.delayEntry >5){SendState()}
        runIn(state.delayEntry+1,setAA)// ALARM
    }
logging ("ENTRY in progress delay:${state.delayEntry} state:${state.Command} ","warn")
}    
        
/* Old direct HSM cmds no longer needed

 sendEvent(name:"armingIn", value: value,data:[armMode:getArmText(armRequest),armCmd:getArmCmd(armRequest)], isStateChange:true)
        case "00": return "disarm"
        case "01": return "armHome"
        case "02": return "armNight" 
        case "03": return "armAway"

 input name: "DirectHSMcmd",  type: "bool", title: "Send direct HSM cmd", defaultValue: true, required: true

, type: "physical", type: "digital"
*/

// My arming commands

private MakeLockCode(cmd){
// more compatibility code
def isInitiator = true    
def lockCode = JsonOutput.toJson(["${state.pinN}":["name":"${state.PinName}", "code":"${state.PIN}", "isInitiator":"${isInitiator}"]] )
logging ("lockCode:${lockCode}]","trace")
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
logging ("lockCode:${lockCode}]","trace")
}


private MyarmAway() {
    logging ("arming away in ${state.delayExit}","info")
    HSMarmingIn("armed away")
    state.received = false
}
private MyarmHome() {
    logging ("arming home in ${state.armHomeDelay}","info")
    HSMarmingIn("armed home")
    state.received = false
}
private MyarmNight() {
    logging ("arming night in ${state.armNightDelay}","info")
    HSMarmingIn("armed night")
    state.received =false
}
private MyDisarm(cmd) {
    if (state.validPIN == false){return}//safety it should never get this far
    if (state.Command == "panic") {
        logging ("Panic cancled by [${state.PinName}]","info")
        sendEvent(name: "panic",  value: "off", descriptionText: "cancled by ${state.PinName} PIN", isStateChange: true,displayed: true)
     }
    if (state.Command == "off") {HSMarmingIn("cancel alerts")}
    HSMarmingIn("disarmed")
    offEvents() 
    state.Command = "off"
    SendState()
    runIn(15,getSoftStatus)
}



def panic() {
	logging ("Panic Pressed","warn")
    sendEvent(name: "panic", value: "on", displayed: true, isStateChange: true, isPhysical: true)
    state.Command = "panic"
    SendState()         
    state.delay = 10
    runIn(70,softOFF)
    // has to be longer than timeout on PANIC triplits or it will restart
}

// ============================================================================================
// Driver sends HSM commands 
private HSMarmingIn(cmd){
    MakeLockCode()
    delay = 0
    if (cmd=="disarmed"){data = [armMode:"disarmed",armCmd:"disarm"]}
    else if (cmd=="cancel alerts"){data = [armMode:"cancel alerts",armCmd:"CancelAlerts"]}
    else if (cmd=="armed night"){
        data = [armMode:"armed night",armCmd:"armNight"]
        delay = state.armNightDelay
    }
    else if (cmd=="armed home"){
        data = [armMode:"armed home",armCmd:"armHome"]
        delay = state.armHomeDelay 
    }
    else if (cmd=="armed away"){
        data = [armMode:"armed away",armCmd:"armAway"]
        delay = state.delayExit
    }
                               
    if(data){                           
    logging ("${cmd} [physical] by [${state.PinName}] state:${state.Command} Delay:${delay}","info")
	sendEvent(name: "securityKeypad", value: cmd, descriptionText: "[physical] ${cmd} by [${state.PinName}] Delay:${delay}", data: lockCode, type: "physical")
    sendEvent(name:"armingIn", value: delay, data:data, isStateChange:true,descriptionText: data) // The actual armming cmd                             
    }
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


// POLL for HSM STATUS =================================================
// HSM Sync status HSM:night <> state:armingNight
def getSoftStatus(status){
if (state.Command =="armingAway" |state.Command =="armingHome"|state.Command =="armingNight"){return}// we got here to early
   status = location.hsmStatus
   test = "NA" 
    if (status == "armedAway" | status == "armingAway" ) {test = "away"}// this is the state   
    if (status == "armedHome" | status == "armingHome")  {test = "home"}
    if (status == "armedNight"| status == "armingNight") {test = "night"}
    
    if (status == "disarmed"  | status == "allDisarmed") {test = "off"}
    if (test == state.Command){logging ("Verified in state with HSM No problems","info") }
    else {
        logging ("Problem found HSM:${test} is <> State:${state.Command}","error")
        logging ("Waiting for any events to finish Polling HSM in 50","info")
        runIn(50, getStatus) 
    }          
}

def getStatus(status) {
   status = location.hsmStatus // Get status then match it. Polled HSM:armingNight state:armingNight
   logging ("Polled HSM:${status} state:${state.Command}","debug") 
   MakeLockCodeIN() 
    if (status == "armedAway" | status == "armingAway" ){  
        if (state.Command != "away"){
            sendEvent(name: "securityKeypad",value: "armed away",data: lockCode, type: "digital",descriptionText: "${device} was armed away [digital]")
            logging ("Polled HSM ${status} switching now","info")
            state.Command = "away"
            SendState()
        }
     return
    }
    
    if (status == "armedHome" | status == "armingHome"){
        if (state.Command != "home"){
            sendEvent(name: "securityKeypad", value: "armed home",data: lockCode, type: "digital",descriptionText: "${device} was armed home [digital]")
            logging ("Polled HSM ${status} switching now","info")
            state.Command = "home"
            SendState()
        }
        return
       }
    
    if (status == "armedNight" | status == "armingNight"){
        if (state.Command != "night"){
            sendEvent(name: "securityKeypad",value: "armed night",data: lockCode, type: "digital",descriptionText: "${device} was armed night [digital]")
            logging ("Polled HSM ${status} switching now","info")
            state.Command = "night"
            SendState()
        }
        return
       }

    
    if (status == "disarmed" | status == "allDisarmed"){
        if (state.Command != "off"){
            sendEvent(name: "securityKeypad",value: "disarmed",data: lockCode, type: "digital",descriptionText: "${device} was disarmed [digital]")
            logging ("Polled HSM ${status} switching now","info")
            state.Command = "off"
            SendState()
        }
        return
    }
  logging ("Polling HSMStatus:${status} Ourstate:${state.Command}","debug") 
    
}

def purgePIN(){
if (state.validPIN){logging ("PIN [${state.PinName}] Removed from memory", "info")}
state.validPIN = false
state.PinName = "none"
state.PIN = "NA"    
}


def siren(cmd){
    if (state.Command == "armingNight" | state.Command == "armingHome"| state.Command == "armingAway" | state.Command == "alarmingNight"| state.Command == "alarmingAway" | state.Command == "alarmingHome"){
    logging ("Unable to Play siren. ${state.Command} overides siren.","warn")
    sendEvent(name: "status", value: "Inuse")
    return
    }
    
  sendEvent(name: "securityKeypad",value: "siren ON",data: lockCode, type: "digital",descriptionText: "${device} alarm siren ON ${status}")
  sendEvent(name: "siren", value: "on", displayed: true) 
  sendEvent(name: "alarm", value: "on", displayed: true)
  sendEvent(name: "switch",value: "on", displayed: true)
  
  if (AlarmTone == "STROBE")  {strobe()}
  if (AlarmTone == "KEYCLICK"){soundCode(1)}
  if (AlarmTone == "LOSTHUB") {soundCode(2)}
  if (AlarmTone == "ARMING")  {soundCode(3)}
  if (AlarmTone == "ARMED")   {soundCode(4)}// alarm
  if (AlarmTone == "HOME")    {soundCode(5)}// entry
  if (AlarmTone == "NIGHT")   {soundCode(6)}// alarm
  if (AlarmTone == "ALARM")   {soundCode(7)}// entry
  if (AlarmTone == "PANIC")   {soundCode(8)}// alarm
  if (AlarmTone == "BADPIN")  {soundCode(9)}
  if (AlarmTone == "OPENDOOR"){soundCode(11)}
  if (AlarmTone == "LOCKED")  {soundCode(12)}   
  if (AlarmTone == "GAME")    {soundCode(13)}
  if (AlarmTone == "CPU")     {soundCode(14)}  
  
    
  logging ("Siren ${AlarmTone} ON", "warn")  
}
def strobe(cmd){
    
    if (state.Command == "armingNight" | state.Command == "armingHome" | state.Command == "alarmingNight" | state.Command == "alarmingHome"){
        logging ("Unable to Play strobe. ${state.Command} overides strobe.","warn")
        sendEvent(name: "status", value: "Inuse")
        return
        }  
    
  logging ("Panic Strobe ON","info")  
  sendEvent(name: "strobe",value: "on", displayed: true) 
  sendEvent(name: "alarm", value: "on", displayed: true)
  sendEvent(name: "switch",value: "on", displayed: true)
  sendEvent(name: "soundName", value: "strobe")   
  sendEvent(name: "status", value: "playing")    
    
  state.Command = "panic"  
  SendState()  
}
def both(cmd){strobe(cmd)}

def on(cmd){
siren()
}

def off(cmd){
  sendEvent(name: "status", value: "off")   
  sendEvent(name: "securityKeypad",value: "siren OFF",data: lockCode, type: "digital",descriptionText: "${device} alarm siren OFF ${status}")  
  logging ("OFF siren/strobe","info")
  offEvents()  
  state.Command = "off"
  SendState()  
  runIn(2, getStatus) // Reset the state to HSM 
}

//if (state.Command == "armingHome"){   sendIrisCmd (0x05)}// Arming ON
//if (state.Command == "armingNight"){  sendIrisCmd (0x07)}// Arming Part
//if (state.Command == "alarmingHome"){ sendIrisCmd (0x06)}// alarming ON
//if (state.Command == "alarmingNight"){sendIrisCmd (0x08)}// alarming Part

//input name: "BadPinTone",type:"enum",title: "Bad Pin Tone",description: "Customize Bad Pin Tone.", options: ["ERROR","BADPIN"], defaultValue: "ERROR",required: true

private BadPinTone(){
    if (BadPinTone == "BADPIN"){ playSound(9)}
    if (BadPinTone == "GAME")  { playSound(13)}
    if (BadPinTone == "LOCKED"){ playSound(12)}
    if (BadPinTone == "LOSTHUB"){playSound(2)} 
    if (BadPinTone == "CPU"){    playSound(14)}
    if (BadPinTone == "ERROR") { playSound(15)}
        pauseExecution(1000)
        soundCodeOff(0)
}


def playSound(cmd){
//   status = location.hsmStatus    
    if (state.Command == "armingNight" | state.Command == "armingHome"| state.Command == "armingAway" | state.Command == "alarmingNight"| state.Command == "alarmingAway" | state.Command == "alarmingHome"){
        logging ("Unable to Play Chimes. ${state.Command} overides chime.","warn")
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
  if (cmd ==11){ sound = "OPENDOOR"}
  if (cmd ==12){ sound = "LOCKED"}    
  if (cmd ==13){ sound = "GAME"}
  if (cmd ==14){ sound = "CPU"}
  if (cmd ==15){ sound = "Error Tone"}  

    
  sendEvent(name: "soundName", value: sound)   
  sendEvent(name: "status", value: "playing")  
    
    if (cmd == 10 ){ 
         beep()
         pauseExecution(2000)
         sendEvent(name: "status", value: "stopped")
         return    
    }
 
soundCode(cmd)
    
    if (chimeTime > 12){ 
        logging ("Chime Delay ${chimeTime}","info")    
        pauseExecution(chimeTime)
        soundCodeOff(0)
    }    
   
sendEvent(name: "status", value: "stopped") 
}    

   

// The keypad needs to be notified of its state.
// Send the command
def sendIrisCmd (cmdI){
state.waiting = 0 // reset the poll timmer
cluster = 0x00C0
attributeId = 0x020
dataType = DataType.ENUM8
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, cmdI, [destEndpoint :0x02]))    
logging ("KeyPad set to [${state.Command}]","info")
} 

def stop(){
    soundCodeOff(0)
    off()
}

def beep(){
   status = location.hsmStatus
   if (status != "disarmed" ){
        logging ("${status} overides entry chime. Ignored","warn")
        sendEvent(name: "status", value: "Inuse")
        return
        }
    logging ("Door Chime/Beep >> Send KeyPad CMD:${state.Command}","info")
    SendState()
}    

def soundCodeOff(cmd){soundCode(0)}
def soundCode(cmd){
value = 0x00
name = "STOP"    
    if (cmd == 1){
        value = 0x01
        name = "KEYCLICK"
    }
    if (cmd == 2){
        value = 0x02
        name = "LOSTHUB"
    }
    if (cmd == 3){
        value = 0x03
        name = "ARMING"
    }
    if (cmd == 4){
        value = 0x04
        name = "ARMED"
    }
    if (cmd == 5){
        value = 0x05
        name = "HOME"
    }
    if (cmd == 6){
        value = 0x06
        name = "NIGHT"
    }
    if (cmd == 7){
        value = 0x07
        name = "ALARM"
    }
    if (cmd == 8){
        value = 0x08
        name = "PANIC"
    }
    if (cmd == 9){
        value = 0x09
        name = "BADPIN"
    }
    if (cmd == 11){
        value = 0x0A
        name = "OPENDOOR"
    }
    if (cmd == 12){
        value = 0x0B
        name = "LOCKED"
    }
    if (cmd == 13){
        value = 0x0C
        name = "GAME"
    }
    if (cmd == 14){
        value = 0x0D
        name = "CPU"
    }
    if (cmd == 15){
        value = 0x0E
        name = "Error Tone"
    }
cluster = 0x00C0
attributeId = 0x26
dataType = DataType.ENUM8
 
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, value, [destEndpoint :0x02]))    
    logging ("Playing Sound code ${name} - ${value} ","info")
}

def shock(){
sendEvent(name: "shock", value: "detected")    
logging ("Shock Detected","info")
 pauseExecution(6000)
logging ("Shock Clear","info")    
sendEvent(name: "shock", value: "clear")    
}


def tamper(){
sendEvent(name: "tamper", value: "detected")
logging ("Tamper Detected","info")
 pauseExecution(6000)
logging ("Tamper Clear","info")    
sendEvent(name: "tamper", value: "clear")
}

def push (buttonNumber){
   logging("Button ${buttonNumber}","info")
   sendEvent(name: "pushed", value: buttonNumber, isStateChange: true) 
}




def normalMode() {
    // This is the standard running mode.
   delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	], 3000)
    logging("SendMode: [Normal]  Pulses:${state.rangingPulses}", "info")
}

void EnrollRequest(){
    logging("Responding to Enroll Request. Likely Battery Change", "warn")
    delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x0500 {11 80 00 00 05} {0xC216}"]),//enrole 
 	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x0500 {11 80 00 00 05} {0xC216}"]),//enrole 
    ], 3000)    
}    

void MatchDescriptorRequest (){
	logging("Match Descriptor Request. Sending Reply","info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])//match des    
}
def ping(){
    logging("Ping", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}


void refresh() {
    logging("Refreshing v${state.version}", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}

// 3 seconds mains 6 battery  2 flash good 3 bad
def rangeAndRefresh() {
    logging("StartMode : [Ranging]", "info")
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"]) // ranging
	state.rangingPulses = 0
	runIn(6, normalMode)
 
}



def checkPresence() {
    // presence routine. v5.1 11-12-22
    // simulated 0% battery detection
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 20
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins Timeout:${checkMin} Tries:${state.tries}","debug")
    if (state.lastCheckInMin <= checkMin){ 
        state.tries = 0
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
            logging("Creating presence event: ${value}  ","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin) { 
      state.tries = state.tries + 1
      if (state.tries >=5){
        test = device.currentValue("presence")
        if (test != "not present" ){
         value = "not present"
         logging("Creating presence event: ${value}","warn")
         sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
         sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)    
         return // we dont want a ping after this or it could toggle
         }
         
     } 
       
     runIn(2,ping)
     if (state.tries <4){
         logging("Recovery in process Last checkin ${state.lastCheckInMin} min ago ","warn") 
         runIn(50,checkPresence)
     }
    }
}


def parse(String description) {
	logging("Parse : ${description}", "trace")
    clientVersion()
    state.lastCheckin = now()
    checkPresence()
    
   if (description?.startsWith('enroll request')) {
        EnrollRequest()
        return
    }
 
//  processMap clusterId:00F0 command:FB [1F, 7C, 63, 16, 00, 3C, 0C, 90, 01, CF, FF, 03, 00] 13   
//  new processing routine    
    Map map = zigbee.parseDescriptionAsMap(description)
    if (!map){
        logging("Failed to parse", "debug")
         return
    }
        
    
	String[] receivedData = map.data
    size = receivedData.size()// size of data field
    logging("${map}", "trace")// full parsed data
    logging("Map clusterId:${map.clusterId} command:${map.command} map:${receivedData}", "debug")// all iris valid data


// Iris KeyPad report cluster 00C0  
   if (map.clusterId == "00C0") {
     if (map.command == "01") {
         // Sends a undocumented error code on bad data May also crash and reboot
         mode1 = receivedData[0]
         mode2 = receivedData[1]
         logging ("Error report: Cluster:${map.clusterId} CMD:${map.command} [${mode1} ${mode2}] ","error")
     }
       
     // KeyPad is polling asking for the state
     if (map.command == "00" ) {
         state.waiting ++
         state.waitingTo = state.delay
         if (state.delay > 22){ state.waitingTo = 22} // limit a runaway wait
         if (state.waiting == 3 | state.waiting==5 | state.waiting==10| state.waiting==15| state.waiting==22){logging ("${device} : KeyPad Polling for state Poll:${state.waiting} of ${state.waitingTo}","info")} 
//       Atempting to stop runaway countdown on entry abort.
         if (state.Command == "off"){
           if (state.waiting > 3){ // We should be off so why are we still pollin
           logging ("Polling while set to [OFF] Corecting state.","warn") 
           getStatus(status)// verify our state from HSM
           SendState()// Reset the state
           } 
         }    
         if (state.waiting > state.waitingTo){ // Correct lost state after a delay
          logging ("Requesting a Poll we lost state. waiting:${state.waiting}","warn")   
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
    if (PinEnclosed  =="28" ){ logging ("Mode Change Error","error")}// should auto correct
        
// action button Logging. 
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
           logging("Action :IRIS:[${irsCMD1}] [${nextirsCMD}]", "debug")} 
    }     
         

// Now check for our command buttons 
// ====================================PANIC ==================================  
     if (keyRec == "50"){
        if (PinEnclosed  =="22" ){logging ("Action :[Pressed PANIC] State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("Action :[Released PANIC] State:${state.Command}","debug")}
        
         if(state.Command != "panic"){
             logging("Action [Pressed PANIC]","info") 
             state.Command = "panic"
             state.delay = 50 // Time out
             panic()
             return
         }
       logging("Action [PANIC] I already sent cmd","debug") 
       return
   	  }    
// ====================================ON =====================================         
      if (keyRec == "41"){
        if (PinEnclosed  =="22" ){logging ("Action :[Pressed ON AWAY] Valid PIN:${state.validPIN} State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("Action :[Released ON AWAY] Valid PIN:${state.validPIN} State:${state.Command}","debug")}

        if (state.Command =="away" | state.Command =="armingAway" ){
          if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }
        logging("Action [ON] already sent:${OnSet} state:${state.Command}","debug")    
        return
        }
        
         if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmAway()
         return   

        if (state.validPIN == false){logging("Invalid PIN Cant Arm","warn")}
        if (PinEnclosed  =="22" ){BadPinTone()}
        return   
        }
//=============================================PARTIAL================================	 
//    PartSet =Arm Night,Arm Home         
     if (keyRec == "4E"){
        if (PinEnclosed  =="22" ){logging ("Action :[Pressed PARTIAL] ${PartSet} Valid PIN:${state.validPIN} State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("Action :[Released PARTIAL] ${PartSet} Valid PIN:${state.validPIN} State:${state.Command}","debug")}

          if (PartSet =="Arm Night"){          
		  if (state.Command =="night" | state.Command =="armingNight" ){
               if (state.received == false){
            MyarmNight() // send it again We got no reply
            return
            }
         
          logging("Action [PARTIAL] Ignored already sent:${PartSet} state${state.Command}","debug")  
          return 
          }
           if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmNight()
         return 

         }
          if (PartSet =="Arm Home"){          
		  if (state.Command =="home" | state.Command =="armingHome" ){
            if (state.received == false ){
            MyarmHome() // send it again We got no reply
            return
            }  
          logging("Action [PARTIAL] Ignored already sent:${PartSet} state${state.Command}","debug")  
          return }
          
          if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmHome()
         return 

         }   
      logging("Invalid PIN Ignoring","warn")   
      if (PinEnclosed  =="22" ){BadPinTone()}
      return   
	 }          

//=============================================OFF==============================        
         if (keyRec == "48"){
          if (PinEnclosed  =="22" ){logging ("Action :[Pressed OFF] Valid PIN:${state.validPIN} State:${state.Command}","info")}
          if (PinEnclosed  =="23" ){logging ("Action :[Released OFF] Valid PIN:${state.validPIN} State:${state.Command}","debug")}
          if (device.currentValue("panic")== "on"){state.Command == "panic"}// Fix being out of sync 
            if (state.validPIN == true){              
             if (state.Command == "off" && PinEnclosed  =="23"){
             logging("Action [OFF] Ignored already sent state${state.Command}","debug")  
             return
             }
            MyDisarm()
            return
         }
             
         logging("Invalid PIN Ignoring","warn")
         if (PinEnclosed  =="22" ){BadPinTone()}  
         return  
	 }         
         
         


 //     StarSet =Disabled,Arm Night,Arm Home,Arm Away
         
     if (keyRec == "2A"){
      if (PinEnclosed  =="22" ){logging ("Action :[Pressed * STAR] ${StarSet} Valid PIN:${state.validPIN} State:${state.Command}","info")}
      if (PinEnclosed  =="23" ){logging ("Action :[Released * STAR] ${StarSet} Valid PIN:${state.validPIN} State:${state.Command}","debug")}

      if (StarSet == "Arm Home"){
		 if (state.Command =="home" | state.Command =="armingHome" ){
          if (state.received == false ){
          MyarmHome() 
          return
          }
         logging("Action [STAR] Ignored already sent:${StarSet} state${state.Command}","debug")
         return }

         if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmHome()
         return 
          
  
         }
       if (StarSet == "Arm Night"){
		 if (state.Command =="night"| state.Command =="armingNight" ){
             if (state.received == false ){
            MyarmNight() // send it again We got no reply
            return
            } 
             
         logging("Action [STAR] Ignored already sent:${StarSet} state${state.Command}","debug")
         return }
 
          if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmNight()
         return 
  
         } 
        if (StarSet == "Arm Away"){
		 if (state.Command =="away" | state.Command =="armingAway" ){
              if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }
         logging("Action [STAR] Ignored already sent:${StarSet} state${state.Command}","debug")
         return }
 
          if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmAway()
         return 
         }     
     
     // disabled
     logging("${StarSet} Valid PIN:${state.validPIN} Ignoring","info")   
     if (PinEnclosed  =="22" ){BadPinTone()}
     return
     }        
        
      
        
        
//     PoundSet =Disabled,Arm Night,Arm Home,Arm Away
         
     if (keyRec == "23"){
         
        if (PinEnclosed  =="22" ){logging ("Action :[Pressed # POUND] ${PoundSet} Valid PIN:${state.validPIN} State:${state.Command}","info")}
        if (PinEnclosed  =="23" ){logging ("Action :[Released # POUND] ${PoundSet} Valid PIN:${state.validPIN} State:${state.Command}","debug")}
    
      if (PoundSet == "Arm Home"){
		 if (state.Command =="home" | state.Command =="armingHome" ){
              if (state.received == false ){
            MyarmHome() // send it again We got no reply
            return
            }
         logging("Action [POUND] Ignored already sent:${PoundSet} state:${state.Command}","debug")
         return 
         }

         if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
         MyarmHome()
         return
                 
         }
       if (PoundSet == "Arm Night"){
		 if (state.Command =="night" | state.Command =="armingNight" ){
              if (state.received == false ){
            MyarmNight() // send it again We got no reply
            return
            }
         logging("Action [POUND] Ignored already sent:${PoundSet} state:${state.Command}","debug")
         return
         }
 
         if (requirePIN){
            if (state.validPIN == false){return}
              }
           else{ defaultLockCode()}
           MyarmNight()
           return
         } 
        if (PoundSet == "Arm Away"){
		 if (state.Command =="away" | state.Command =="armingAway"  ){
              if (state.received == false ){
            MyarmAway() // send it again We got no reply
            return
            }
       logging("Action [POUND] Ignored already sent:${PoundSet} state:${state.Command}","debug")
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
     
     logging("${PoundSet} Valid PIN:${state.validPIN} Ignoring","info")   
     if (PinEnclosed  =="22" ){BadPinTone()}     
     return
     } 

          

//      PinEnclosed = receivedData[0]// 21 = pin
//      pinSize     = receivedData[3]// The PIN size + 4 = size   
    if (PinEnclosed == "21" ){
     
     if (pinSize != "01" ){// To small for a PIN skip to buttons
        pinSize = receivedData[3]// 4 - 15 digit Pin size supported 
        size = receivedData.size() 
        end = size -1 
        state.PIN  = receivedData[4..end].collect{ (char)Integer.parseInt(it, 16) }.join()
        checkThePin() // Scan the pins

      sendEvent(name: "lastCodeName", value: "${state.PinName}", descriptionText: "${state.PinName} [${state.PIN}] valid:${state.validPIN}")// May be needed by other aps.
      sendEvent(name: "lastCodePIN",  value: "${state.PIN}",     descriptionText: "${state.PinName} [${state.PIN}] valid:${state.validPIN}")         
      logging("valid:${state.validPIN} Pin:${state.PIN} Name:${state.PinName} Size:${pinSize} State:${state.Command}","debug")
      runIn(97, "purgePIN")// Purge time must allow repeating to finish
      if (state.validPIN == true){
          logging("[Valid PIN]:Name:${state.PinName} State:${state.Command}","info")
     	  return  
        }   
      // The pin was not valid         
      BadPinTone()
      logging("[Invalid PIN]:Pin:${state.PIN} State:${state.Command}","warn")
      if(tamperPIN){tamper()}
      else {shock()} 
      return	 
    }// Pin size check  
  }// end pin check       

// Keypad button matrix * # are in logic above
 
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

    
// General Cluster         
    } else if (map.clusterId == "00F0") {
       if (map.command == "FB") {
      // lqiRec = receivedData[8] 
      // if (lqiRec){ lqi = receivedData[10]}
        batRec = receivedData[0] // This sould be set to [19]
//        def temperatureValue = "NA"
        def batteryVoltageHex = "NA"
        BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
//        temperatureValue = receivedData[7..8].reverse().join()     
    if (batteryVoltageHex != "FFFF") {
        batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
        batteryVoltage2 = batteryVoltage.setScale(2, BigDecimal.ROUND_HALF_UP)
        batteryVoltage1 = batteryVoltage.setScale(1, BigDecimal.ROUND_HALF_UP)// base % on rounding
// I base this on Battery discharge curves(may need adjustments)
// Normal batteries slowely discharge others have a sudden drop          
// Iris source code says 2.1 is min voltage  

		BigDecimal batteryVoltageScaleMin = 2.10
		BigDecimal batteryVoltageScaleMax = (1.50 * 2)	    
	    
	    if (BatType == "NiCad"){ // < 1.2x2=2.2 drops out fast
		batteryVoltageScaleMin = 2.20
		batteryVoltageScaleMax = (1.50 * 2)	    
	    } 
        if (BatType == "NiMH"){ // < 1.2x2=2.2 drops out fast
		batteryVoltageScaleMin = 2.20
		batteryVoltageScaleMax = (1.35 * 2)	    
	    }    

	    if (BatType == "Lithium"){// < 1.25x2=2.5 drops out fast 
		batteryVoltageScaleMin = 2.50
		batteryVoltageScaleMax = (1.7 * 2)	    
	    } 	    
     	BigDecimal batteryPercentage = 0
            batteryPercentage = ((batteryVoltage1 - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
//        if (!state.DualBat){state.DualBat=0}
//        state.DualBat = state.DualBat + 1
//        if (state.DualBat < 1 | state.DualBat >2 ){stateDualBat = 1}
        
        evntBat = "battery"
        evntVol = "batteryVoltage"
        powerLast = device.currentValue(evntBat)
            
//        if (state.DualBat == 2){  not working
//        evntBat = "battery2"
//         evntVol = "batteryVoltage2" 
//         powerLast2 = device.currentValue(evntBat)   
//        }
        
        
        if ( batteryVoltage2 < state.minVoltTest){state.minVoltTest = batteryVoltage2}  // Record the min volts seen working 
       
        logging("${evntBat}: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage2}V", "debug")
        if (powerLast == batteryPercentage){return}
           sendEvent(name: evntBat, value: batteryPercentage, unit: "%")
           sendEvent(name: evntVol, value: batteryVoltage2, unit: "V", descriptionText: "Volts:${batteryVoltage2}V MinVolts:${batteryVoltageScaleMin} v${state.version}")    
        logging("${evntBat}:${batteryPercentage}% ${batteryVoltage2}V", "info")
        
        }// end battery           
  }// FB
}// 00F0
    else if (map.clusterId == "00F6") {// Join Cluster 0xF6
       // Ranging
		if (map.command == "FD") { // LQI
			def lqiHex = "na"
			lqiHex = receivedData[0]
            int lqiRanging = 0
			lqiRanging = zigbee.convertHexToInt(lqiHex)
            lqiLast = device.currentValue("lqi")
            if(lqiRanging != lqiLast){
			 sendEvent(name: "lqi", value: lqiRanging)
			 logging("LQI: ${lqiRanging}", "info")
            }   
		if (receivedData[1] == "77" || receivedData[1] == "FF") { // Ranging running in a loop
			state.rangingPulses++
            logging("Ranging ${state.rangingPulses}", "debug")    
 			 if (state.rangingPulses > 8) {
              normalMode()
              return   
             }   
        } else if (receivedData[1] == "00") { // Ranging during a reboot
				// when the keypad reboots we must answer
				logging("reboot ranging report received", "info")
				refresh()
                return
			} 
// End ranging block 



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
            
             // taken from iris source code
            mfgIdhex = receivedData[11] + receivedData[10]  
            def mfgIddec = Integer.parseInt(mfgIdhex,16)
            dvcType = receivedData[13] + receivedData[12]
            def appRel = Integer.parseInt(receivedData[14],16)
            def appVer = Integer.parseInt(receivedData[15],16)
            hwVerHex = receivedData[17] + receivedData[16]
            def hwVer = Integer.parseInt(hwVerHex,16)        
            
            String deviceFirmwareDate = versionInfoBlocks[versionInfoBlockCount - 1]
            firmwareVersion = "appV.appRel.hwV-" +appVer + "." + appRel + "." + hwVer+"-date-" + deviceFirmwareDate
            logging("Ident Block: ${versionInfoDump} ${firmwareVersion}", "trace")
    
            state.firmware =  appVer + "." + appRel + "." + hwVer 

	String deviceManufacturer = "IRIS/Everspring"
	String deviceModel = ""
	String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
        reportFirm = "unknown"
      if(deviceFirmware == "2012-06-08" ){reportFirm = "v1 Ok"}
      if(deviceFirmware == "2012-06-11" ){reportFirm = "v1.1 Ok"} // Tones are diffrent    
      if(deviceFirmware == "2012-12-11" ){reportFirm = "v2 Ok"}
      if(deviceFirmware == "2013-06-28" ){reportFirm = "v3 Ok"}
  
      
	if(reportFirm == "unknown"){state.reportToDev="Unknown firmware [${firmwareVersion}] ${deviceFirmwareDate}" }
    else{state.remove("reportToDev")}
      
	// Sometimes the model name contains spaces.
	if (versionInfoBlockCount == 2) {
	deviceModel = versionInfoBlocks[0]
	} else {
	deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
	}
      logging("${deviceModel} Firmware :[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "debug")
        if(!state.Config){
        state.Config = true    
        updateDataValue("manufacturer", deviceManufacturer)
        updateDataValue("device", deviceModel)
        updateDataValue("model", "KPD800")
	    updateDataValue("firmware", deviceFirmware)
        updateDataValue("fcc", "FU5TSA04")
        updateDataValue("partno", "TSA04-0")
        }
     } 

// Standard IRIS USA Cluster detection block v4

    } else if (map.clusterId == "8038")  {logging("${map.clusterId} Seen before but unknown", "debug")
	} else if (map.clusterId == "8001" ) {logging("${map.clusterId} Routing and Neighbour Information", "info")    
	} else if (map.clusterId == "8032" ) {logging("${map.clusterId} New join has triggered a routing table reshuffle.", "info")
    } else if (map.clusterId == "8034" ) {logging("${map.clusterId} Seen during REMOVE. ", "warn")    
    } else if (map.clusterId == "0006")  {MatchDescriptorRequest()
	} else if (map.clusterId == "0013" ) {logging("${map.clusterId} Re-routeing", "warn")
    } else {logging("New unknown Cluster Detected: ${map}", "warn")}// report to dev improved
}
        
        

void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}



private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}

void getIcons(){
state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-keypad.jpg'>"
state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    
    
 }



// Logging block  v4

void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
}

void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}
void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}
private logging(String message, String level) {
    if (level == "infoBypass"){log.info  "${device} : $message"}
	if (level == "error"){     log.error "${device} : $message"}
	if (level == "warn") {     log.warn  "${device} : $message"}
	if (level == "trace" && traceLogging) {log.trace "${device} : $message"}
	if (level == "debug" && debugLogging) {log.debug "${device} : $message"}
    if (level == "info"  && infoLogging)  {log.info  "${device} : $message"}
}

