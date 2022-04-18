/*
 Iris v2 - v3 keypad driver
==================================================================================================
Driver supports model# 1112-S and model# 3405-L iris keypads

supports both the v2 and v2 keypads in one driver.

<<<<<<<<<<<<<<<<
not for release This is a beta test BUGS exist
I have decided not to have v2 and v3 in the same driver many problems.
This is delayed until later.....




Improvments over default drivers.

v3 fix for being unable to cancel alarms
chimes added
Panic support added for button controler. silent alarm

note:
The Iris v3 keypad is hardwired to not send disarm commands when unarmed.
In order to disable a safety alert you must set keypad to Alarm on water/smoke alerts.
When the driver sees the alarm and water/smoke it will arm the keypad. 
You will then be able to disarm Alerts..

The v3 keypad can only send disarm once then it has to be rearmed. Remember you
cant send disarm over and over it will be ignored by the hardware. This is the
cause of runaway alarms.The driver will try to rearm the keypad on most events.




  v3 softwareBuild: 10036230 firmwareMT: 123B-0012-10036230  tested working 
  v2 softwareBuild: 10025310 firmwareMT: 104E-0021-10025310  tested working
  v2 softwareBuild: 140B5310 has a mute volume problem (alt firmware)
holding 2 raises volume but it goes back to 0.
Have been unable to fix volume on alt v2 firmware. 



 v1.6 03/01/2022 more debuging. Alt v2 firmware detection
 V1.2  2/25/2022 V2 working. v3 Working.
 v1.1  2/23/2022 Tested on v2 and v3 kaypads. 
 v1.0  2/22/2022 Beta test copy 



Iris v3 fccid:2AMI2IL02 model:3405-L marked model:IL02_01
Iris v2 fccid: model:3405-L



================================================================================================= 
--Forked from example drivers at 
https://github.com/hubitat/HubitatPublic/tree/master/examples/drivers
https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/irisKeypadV3.groovy
2019-12-13 2.1.8 maxwell
Iris V3 Keypad
Copyright 2016 -> 2020 Hubitat Inc.  All Rights Reserved

*/
def clientVersion() {
    TheVersion="1.6"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

metadata {
    definition (name: "Iris v2 - v3 Keypad", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_v2-v3_keypad.groovy") {


        capability "Battery"
        capability "Configuration"
        capability "Motion Sensor"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Refresh"
        capability "Security Keypad"
        capability "Tamper Alert"
        capability "Alarm"
        capability "Tone"
        capability "Initialize"
        capability "Chime"
        capability "PushableButton"
        capability "ContactSensor"
        

        command "armNight"
        command "setArmNightDelay", ["number"]
        command "setArmHomeDelay", ["number"]
        command "entry" //fired from HSM on system entry
        command "setPartialFunction"

        attribute "armingIn", "NUMBER"
        attribute "lastCodeName", "STRING"

        fingerprint model:"1112-S", manufacturer:"iMagic by GreatStar",profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0405,0500,0501,0B05,FC01,FC02,FC04", outClusters:"0003,0019,0501", deviceJoinName:"Iris V3 Keypad"
        fingerprint model:"1112-S", manufacturer:"iMagic by GreatStar",profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0405,0500,0501,0B05,FC01,FC02",      outClusters:"0003,0019,0501", deviceJoinName:"Iris V3 Keypad old firmware"
        fingerprint model:"3405-L", manufacturer:"CentraLite",         profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0500,0501,0B05,FC04",                outClusters:"0019,0501",      deviceJoinName:"Iris V2 Keypad"
        fingerprint model:"3405-L", manufacturer:"CentraLite",         profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0500,0501,0B05,FC04,FC05",           outClusters:"0019,0501",      deviceJoinName:"Iris V2 Keypad"
//        fingerprint model:"1112-S", manufacturer:"iMagic by GreatStar",profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0405,0500,0501,0B05,FC01,FC02",      outClusters:"0003,0019,0501", deviceJoinName:"Iris V3 Keypad"
    
    }

    preferences{
        input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true
	    input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false
	    input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false

// not yet implimented        
//      input name: "SilentArmHome", type: "bool", title: "Silent Arming Home", description: "No beep while arming", defaultValue: false
//	    input name: "SilentArmAway", type: "bool", title: "Silent Arming Away", description: "No beep while arming", defaultValue: false
//	    input name: "SilentArmNight",type: "bool", title: "Silent Arming Night",description: "No beep while arming", defaultValue: false
    
        input name: "PartSet", type: "enum", title: "Partial Button", description: "Customize Partial Button",  options: ["Arm Night", "Arm Home"], defaultValue: "Arm Night",required: true 
        input name: "OnSet",   type: "enum", title: "On Button", description: "Customize ON Button",  options: ["Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Away",required: true 

        input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: "Hides code from log"
        input "refTemp", "decimal", title: "Reference temperature", description: "Adjust the temp", range: "*..*"

    }
}



void installed(){
    log.warn "${device.displayName} installed..."
    initialize()
}

def initialize() {
    state.exitDelay = 0
    state.entryDelay = 0
    state.armNightDelay = 0
    state.armHomeDelay = 0
    state.armMode = "00"
    state.fnPartial = "01"
    state.v2alt = false
    sendEvent(name:"maxCodes", value:20)
    sendEvent(name:"codeLength", value:4)
    sendEvent(name:"alarm", value: "off")
    sendEvent(name:"tamper", value: "clear")
    sendEvent(name:"securityKeypad", value: "disarmed")  
    sendEvent(name: "numberOfButtons", value: "1", isStateChange: false)
    
   	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"true",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
}

	


def uninstalled(){
    return zigbee.command(0x0000,0x00)
}
// Statuses:
// 00 - Command: setDisarmed   Centralite all icons off / Iris Off button on, Ready to Arm
// 01 - Command: setArmedStay  lights Centralite Stay button / Iris Partial
// 02 - Command: setArmedNight lights Centralite Night button / Iris V2 does nothing / Iris V3 lights Partial
// 03 - Command: setArmedAway  lights Centralite Away button / Iris ON
// 04 - Panic Sound, uses seconds for duration (siren on everything but 3400 use beep instead, max 255)
// 05 - Command: Beep and SetEntryDelay Fast beep (1 per second, uses seconds for duration, max 255) Appears to keep the status lights as it was, used for entry delay command
// 06 - Not ready to Arm Centralite - Amber status blink (Runs forever until Off or some command issued on Centralite, Iris V3 wont Arm)
// 07 - Zigbee In Alarm: sounds Siren on Iris V2/V3
// 08 - Command: setExitStay  Blink Stay Icon/Partial light all devices, Slow beep on Iris only (1 per second, accelerating to 2 beep per second for the last 10 seconds) - With red flashing status - lights Stay icon/Iris Partial Uses seconds
// 09 - Command: setExitNight Blink Night Icon on Centralite and UEI devices no beeps, with red flashing status - lights Night icon/ Uses seconds  (does nothing on Iris)
// 10 - Command: setExitAway  Blink Away Icon / ON light on all devices (1 per second, accelerating to 2 beep per second for the last 10 seconds) - With red flashing status - lights Away Uses/Iris ON seconds


def parse(String description) {

    if (description.startsWith("zone status")) {
        def zoneStatus = zigbee.parseZoneStatus(description)
        getTamperResult(zoneStatus.tamper)
    } else if (description.startsWith("enroll request")) {
        return
    } else {
        def descMap = zigbee.parseDescriptionAsMap(description)

        def resp = []
        def clusterId = descMap.clusterId ?: descMap.cluster
        def cmd = descMap.command
        def des = cmd
        if (cmd == "00") {des ="keypad action"}
        if (cmd == "01") {des ="battery/temp"}
        if (cmd == "04") {des ="panic"}
        if (cmd == "07"){ des ="motion"}
        if (cmd == "0B"){ des ="AltFirm"}
        
            logging ("${device} : parse >> cluster:${clusterId} cmd:${des}  state${state.bin}","trace")     
        

   

        switch (clusterId) {
            case "0001":
                if (descMap.value) {
                    value = hexStrToUnsignedInt(descMap.value)
                    getBatteryResult(value)
                }
                break
            case "0501":
                if (cmd == "07" && descMap.data.size() == 0) { //get panel status client -> server
                    if (state.bin == -1) getMotionResult()// send motion event
                    resp.addAll(sendPanelResponse(false))
                } else if (cmd == "00") {
                    state.bin = -1
                    def armRequest = descMap.data[0] // will be 00 disarm or 01 part  03 on  (no 2 key)
                    def asciiPin = "0000"
                    logging ("${device} : Keypad requesting ${getArmText(armRequest)}","info")
//                    if (armRequest == 1) {countdown(state.armNightDelay)}// start the countdown
//                    if (armRequest == 3) {countdown(state.armAwayDelay)}

                    if (armRequest == "00") { asciiPin = descMap.data[2..5].collect{ (char)Integer.parseInt(it, 16) }.join()} // if disarm need a pin
                    
                    resp.addAll(sendArmResponse(armRequest,isValidPin(asciiPin, armRequest)))
                } else if (cmd =="0B") {
                // we get this with alt firmware. We respond to it
                // This firmware has a mute problem     
                state.v2alt = true 
                logging ("${device} : 0501 0B :${descMap.data} Alt firmware detected","trace") 
                resp.addAll(sendPanelResponse(false))   
                
                } else if (cmd == "04") { //panic button
                    logging ("${device} : Panic button pressed (pushed)","warn")
                    createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "panic button was pushed", isStateChange: true)
                  state.bin = 1
                  state.panic = "active"
                  sendEvent(name: "alarm",value: "alarm",descriptionText: "panic button was pushed")  
                  runIn(9,buttonRelease)
//                  modified for button support silent alarm                    
//                    resp.addAll(siren())
                } else {
                    logging ("${device} : cmd:${cmd} untrapped  ${descMap}","debug")
                    if (logEnable) log.warn "${device.displayName} 0501 skipped: cmd:${cmd}  ${descMap}"
                }
                break
            case "0402":
                if (descMap.value) {
                    def tempC = hexStrToSignedInt(descMap.value)
//                    logging ("${device} : Received temp ${tempC}","trace")
                    getTemperatureResult(tempC)
                }
                break
            case "FC04":
            logging ("${device} : ERROR FC04","warn")
                break
            case "0013":
            logging ("${device} : 0013 (Sends this while arming) Idevent:${description}","trace")
            break
            
            default :
           if (cmd == 0x0B) { 
            if (descMap.data[1] == 0x81) {logging ("${device} : Unknown cmd","warn")}
            else if (descMap.data[1] == 0x80)  {logging ("${device} : Malformed cmd","warn")}
         }
            logging ("${device} : Untrapped cluster ${clusterId} Idevent:${description}","trace")
        }
        if (resp){
            sendHubCommand(new hubitat.device.HubMultiAction(resp, hubitat.device.Protocol.ZIGBEE))
        }
    }
}

def beep(){
    state.model = getDataValue("model")

    // v2 alt doesnt support beeps at all
//    if (state.v2alt == true){BeepCode = "2"} 
    if (state.model == "1112-S"){
        if (BeepCode == "2"){BeepCode = "3"} // v3 doesnt support beep 2   
    }
    cmd = BeepCode
    logging ("${device} : beep ","info") 
    playSound(cmd)
}

void beepBad(){
//playSound(3) 

}
def stop(){
stopBee()

}
void stopBeep(){
    stopBee()
}
def stopBee(){
    logging ("${device} : beep stop","info") 

     cmds = [
        			"raw 0x0501 {09 01 04 05 00 01}", // 
        			"delay 200",
			        "send 0x${device.deviceNetworkId} ${device.endpointId as int} 1",
			        "delay 500"
    			] 
    logging ("${device} : ${cmds}","trace")
    return cmds   
}

def countdown(delay){
    logging ("${device} : countdown ${delay}","info")
    def cmds = [
        "raw 0x0501 {09 01 04 05 ${delay} 01}", // Fast beep (1 per second)
        			"delay 200",
			        "send 0x${device.deviceNetworkId} ${device.endpointId as int} 1",
			        "delay 500"
    			] 
    logging ("${device} : ${cmds}","trace")
    return cmds
}
    

def playSound(cmd){
  
    if (cmd == null){cmd=1}
    if (cmd >= 6){cmd=1}
//    if (state.v2alt == true){ cmd=2}
    if (state.model == "1112-S"){

    if (cmd == 2){
     cmd = 3
        logging ("${device} : chime 2 not supported on ${state.model}","warn") // v3 doesnt support beep 2   
    }
    }
    logging ("${device} : playing chime ${cmd}","info") 
    runIn(9,stopBeep) // stops the countdown timmer or you get 2nd beep in 10 sec
      /*
    	09 - Frame Ctl
		01 - Transaction
        00 - Cmd Arm response
        0x - Arm Notification  (armMode)  (5=entry delay + a delay code fast beep)
    */
  if (cmd==1) { len ="01"}
  if (cmd==3) { len ="03"}
  if (cmd==4) { len ="05"}
  if (cmd==5) { len ="07"}  
    
    def cmds = [
        "raw 0x0501 {09 01 04 05 ${len} 01}", // Fast beep (1 per second)
        			"delay 200",
			        "send 0x${device.deviceNetworkId} ${device.endpointId as int} 1",
			        "delay 500"
    			]    
 //      0x0501 {09 01 04 05 01 01 01}"  
//  [raw 0x0501 {09 01 04 05 1}, delay 200, send 0xB7AA 1 1, delay 500]   

    /*
    	09 - Frame Ctl
		01 - Transaction
		04 - Cmd Panel Status Changed Cmd
		05 - Entry Delay
        01 - Seconds
	*/     
  

   
    if (cmd==2) {
        
    cmds = [
        			"raw 0xFC04 {15 4E 10 00 00 00}", // 
        			"delay 200",
			        "send 0x${device.deviceNetworkId} ${device.endpointId as int} 1",
			        "delay 500"
    			] 
   }
    
logging ("${device} : ${cmds}","trace")     
return cmds
 

}




void setEntryDelay(delay){
    state.entryDelay = delay != null ? delay.toInteger() : 0
    logging ("${device} : set Entry delay${state.entryDelay}","info") 

}

void setExitDelay(Map delays){
    state.exitDelay = (delays?.awayDelay ?: 0).toInteger()
    state.armNightDelay = (delays?.nightDelay ?: 0).toInteger()
    state.armHomeDelay = (delays?.homeDelay ?: 0).toInteger()
    logging ("${device} : set delay exit${state.exitDelay} Night${state.armNightDelay} Home${state.armHomeDelay}","info") 
}

void setExitDelay(delay){
    state.exitDelay = delay != null ? delay.toInteger() : 0
    logging ("${device} : set delay exit${state.exitDelay}","info") 
}

void setArmNightDelay(delay){
    state.armNightDelay = delay != null ? delay.toInteger() : 0
    logging ("${device} : set delay Night${state.armNightDelay}","info") 

}

void setArmHomeDelay(delay){
    state.armHomeDelay = delay != null ? delay.toInteger() : 0
    logging ("${device} : set delay Home${state.armHomeDelay}","info") 

}

// whats this for?
void setPartialFunction(mode = null) {
    logging ("${device} : set Partial ${mode}","trace") 
    if ( !(mode in ["armHome","armNight"]) ) {
    logging ("${device} : custom command used by HSM","trace")
    } else if (mode in ["armHome","armNight"]) {
        state.fnPartial = mode == "armHome" ? "01" : "02"
    }
}

void setCodeLength(length){
    String descriptionText = "${device.displayName} codeLength set to 4"
    logging ("${device} : ${descriptionText}","trace")  
    sendEvent(name:"codeLength",value:"${4}",descriptionText:descriptionText)
}

void setCode(codeNumber, code, name = null) {
    if (!name) name = "code #${codeNumber}"

    def lockCodes = getLockCodes()
    def codeMap = getCodeMap(lockCodes,codeNumber)
    def data = [:]
    def value
    //verify proposed changes
    if (!changeIsValid(codeMap,codeNumber,code,name)) return

    if (codeMap) {
        if (codeMap.name != name || codeMap.code != code) {
            codeMap = ["name":"${name}", "code":"${code}"]
            lockCodes."${codeNumber}" = codeMap
            data = ["${codeNumber}":codeMap]
            if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
            value = "changed"
        }
    } else {
        codeMap = ["name":"${name}", "code":"${code}"]
        data = ["${codeNumber}":codeMap]
        lockCodes << data
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        value = "added"
    }
    updateLockCodes(lockCodes)
    sendEvent(name:"codeChanged",value:value,data:data, isStateChange: true)
    logging ("${device} : setting #:${codeNumber} code:${code} name:${name}","info")
}

def deleteCode(codeNumber) {
    def codeMap = getCodeMap(lockCodes,"${codeNumber}")
    def result = [:]
    if (codeMap) {
        lockCodes.each{
            if (it.key != "${codeNumber}"){
                result << it
            }
        }
        updateLockCodes(result)
        def data =  ["${codeNumber}":codeMap]
        if (optEncrypt) data = encrypt(JsonOutput.toJson(data))
        sendEvent(name:"codeChanged",value:"deleted",data:data, isStateChange: true)
        logging ("${device} : deleting #${codeNumber} code:${code} name:${name}","info")
    }
}

def getCodes(){
    updateEncryption()
    logging ("${device} : get codes","info")
}



// Hub says entry in process (start a countdown)
def entry(entranceDelay){
    if (state.entryDelay  == 0 ){state.entryDelay  = 30}
    if (entranceDelay == NULL ){entranceDelay = state.entryDelay}
    logging ("${device} : >> HUB Entry in progress delay:${entranceDelay}","info")

    if (entranceDelay) {
        def ed = entranceDelay.toInteger()
        state.entryDelay  = ed
        state.bin = 1
        state.delayExpire = now() + (ed * 1000)
        state.armingMode = "05" //entry delay
        def hexVal = intToHexStr(ed)
        runIn(ed + 5 ,clearPending)
        return [
                "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 04 05 ${hexVal} 01 01}"
        ]
    }
}
    /*

    	09 - Frame Ctl
		01 - Transaction
        00 - Cmd Arm response
        0x - Arm Notification  (armMode)  (5=entry delay + a delay code fast beep)
    */



// HUB says DISARM do only once
def disarm(exitDelay = null) {
        if (state.armMode == "00") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub send Disarm (already disarmed)","info")
        return
    }

    logging ("${device} : >> Hub sent disarm","info")
    state.armPending = false
    state.bin = 1
    sendArmResponse("00",getDefaultLCdata())// disarm should not have a delay
}

def armHome(exitDelay = null) {
    if (state.armMode == "01") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub sent ArmHome (already armed)","info")
        return
    }
    logging ("${device} : >> Hub sent armHome delay:${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
    state.bin = 1
    if (exitDelay == null) sendArmResponse("01",getDefaultLCdata())
    else sendArmResponse("01",getDefaultLCdata(),exitDelay.toInteger())
}

def armNight(exitDelay = null) {
    if (state.armMode == "01") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub sent armNight (already armed)","info")
 
        return
    } 
    logging ("${device} : >> Hub sent armNight delay:${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
 
    state.bin = 1
    if (exitDelay == null) sendArmResponse("01",getDefaultLCdata())
    else sendArmResponse("01",getDefaultLCdata(),exitDelay.toInteger())
}

def armAway(exitDelay = null) {
    
    if (state.armMode == "03") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub sent armAway (already armed)","info")
 
        return
    }

    logging ("${device} : >> Hub sent armAway delay:${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
 
    state.bin = 1
    if (exitDelay == null) sendArmResponse("03",getDefaultLCdata())
    else sendArmResponse("03",getDefaultLCdata(),exitDelay.toInteger())
}

//alarm commands
void buttonRelease(){
def descriptionText = "${device.displayName} panic button was released"   
createEvent(name: "button", value: "released", data: [buttonNumber: 1], descriptionText: "${descriptionText}", isStateChange: true)
logging ("${device} : Panic button (released)","info")
state.bin = -1
state.panic = "inactive" 
sendEvent(name: "alarm",value: "off",descriptionText: "${descriptionText}")    
}



void timeoutalarm(){
    logging ("${device} : Timeout alarm resending OFF","info")
    off()
}

def off(){
    logging ("${device} : OFF alarm/strobe","info")
    def value = "off"
    def descriptionText = "${device.displayName} alarm was turned ${value}"

    state.bin = -1
//    state.panic = "inactive"
    sendEvent(name: "alarm",value: value,descriptionText: "${descriptionText}")
    return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 04 00 00 01 04}"] //clear
    
 
    
}

def siren(){
//    if (state.panic == "inactive") {
        state.bin = 1
//        state.panic = "active"

// v3 rearm fix to allow canceling alarms when keypad is disarmed    
  if (state.model == "1112-S"){  
//===============================SMOKE detection=============================================    
//    hsmAlert intrusion intrusion-home intrusion-night smoke water
    if (location.hsmAlert == "smoke" | location.hsmAlert == "water"){
        if(state.armMode == "00"){
        armAway(0) // make sure keypad is armed so it can disarm smoke alarm
        logging ("${device} : ALARM${location.hsmAlert}. Arming keypad so it can disable!","warn")
     }
    }
// ==============================fix being unable to disarm keypad already off========================    
    if (location.hsmAlert == "intrusion" | location.hsmAlert == "intrusion-home"| location.hsmAlert == "intrusion-night"){

        if(state.armMode == "00"){
        armAway(0) // make sure keypad is armed 
        logging ("${device} : ALARM${location.hsmAlert}. Keypad was disarmed out of sync rearming","warn")
       }
    }    
// ==========================================================================================  
  } 
    def value = "siren"
    def descriptionText = "${device.displayName} alarm set to ${value}"
    logging ("${device} : ON alarm","warn")
    sendEvent(name: "alarm",value: value,descriptionText: "${descriptionText}")
    runIn(40,timeoutalarm) // limit allarm run time
    return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 04 07 00 01 01}"]
    
    
}

def strobe(){
//    not working on v3
   
   state.bin = 1 
   def value = "strobe" 
   logging ("${device} : ON strobe","info")
   sendEvent(name: "alarm",value: value,descriptionText: "${descriptionText}")

 return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 04 ${zigbee.convertToHexString(6,2)}${zigbee.convertToHexString(1,2)}}"]
}

def both(){
    siren()
//    strobe()
}

//private
private changeIsValid(codeMap,codeNumber,code,name){
    def result = true
    def codeLength = device.currentValue("codeLength")?.toInteger() ?: 4
    def maxCodes = device.currentValue("maxCodes")?.toInteger() ?: 20
    def isBadLength = codeLength != code.size()
    def isBadCodeNum = maxCodes < codeNumber
    if (lockCodes) {
        def nameSet = lockCodes.collect{ it.value.name }
        def codeSet = lockCodes.collect{ it.value.code }
        if (codeMap) {
            nameSet = nameSet.findAll{ it != codeMap.name }
            codeSet = codeSet.findAll{ it != codeMap.code }
        }
        def nameInUse = name in nameSet
        def codeInUse = code in codeSet
        if (nameInUse || codeInUse) {
            if (nameInUse) { logging ("${device} : Change failed, name:${name} is in use:${ lockCodes.find{ it.value.name == "${name}" } }","warn")      }
            if (codeInUse) { logging ("${device} : Change failed, code:${code} is in use:${ lockCodes.find{ it.value.code == "${code}" } }","warn")      }
            result = false
        }
    }
    if (isBadLength || isBadCodeNum) {
        if (isBadLength) { logging ("${device} : length of code ${code} <> ${codeLength}","warn")}
        if (isBadCodeNum){ logging ("${device} : To many codes! maxCodes=${maxCodes}","warn") }
        result = false
    }
    return result
}

private getCodeMap(lockCodes,codeNumber){
  
    def codeMap = [:]
    def lockCode = lockCodes?."${codeNumber}"
    if (lockCode) {
        codeMap = ["name":"${lockCode.name}", "code":"${lockCode.code}"]
    }
    logging ("${device} : Get code map ${codemap}","trace")
    return codeMap
}

private getLockCodes() {
    def lockCodes = device.currentValue("lockCodes")
    def result = [:]
    if (lockCodes) {
        if (lockCodes[0] == "{") result = new JsonSlurper().parseText(lockCodes)
        else result = new JsonSlurper().parseText(decrypt(lockCodes))
    }
    logging ("${device} : Get code ${result}","trace")
    return result
}

private updateLockCodes(lockCodes){
    def data = new groovy.json.JsonBuilder(lockCodes)
    if (optEncrypt) data = encrypt(data.toString())
    sendEvent(name:"lockCodes",value:data,isStateChange:true)
    logging ("${device} : updateLockCodes: ${lockCodes}","trace")
}

private updateEncryption(){
    def lockCodes = device.currentValue("lockCodes") //encrypted or decrypted
    if (lockCodes){
        if (optEncrypt && lockCodes[0] == "{") {	//resend encrypted
            sendEvent(name:"lockCodes",value: encrypt(lockCodes), isStateChange:true)
        } else if (!optEncrypt && lockCodes[0] != "{") {	//resend decrypted
            sendEvent(name:"lockCodes",value: decrypt(lockCodes), isStateChange:true)
        } else {
            sendEvent(name:"lockCodes",value: lockCodes, isStateChange:true)
        }
    }
}

private isValidPin(code, armRequest){
    def data = getDefaultLCdata()
    if (armRequest == "00") {
        //verify pin
        def lockCode = lockCodes.find{ it.value.code == "${code}" }
        if (lockCode) {
            data.codeNumber = lockCode.key
            data.name = lockCode.value.name
            data.code = code
            descriptionText = "${device.displayName} disarmed by ${data.name}"
            sendEvent(name: "lastCodeName", value: data.name, descriptionText: descriptionText, isStateChange: true)
            logging ("${device} : -Disarmed- by [${data.name}]","info")
        } else {
            data.isValid = false
            logging ("${device} : Invalid pin entered [${code}]","warn")
            runIn(5, beepBad) // send tone
        }
    }
    return data
}



private sendPanelResponse(alert = false){
    def resp = []
    def remaining = (state.delayExpire ?: now()) - now()
    remaining = Math.ceil(remaining /= 1000).toInteger()
    if (remaining < 0) { remaining = 0} // get rid of - nos in the log
//    logging ("${device} : Send panel response ${alert}","trace")
    if (remaining > 3) {
        runIn(2,"sendPanelResponse")
        resp.add("he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${state.armingMode} ${intToHexStr(remaining)} 01 01}")
        logging ("${device} : Send panel response ${remaining} alert:${alert}","info")
    } else {
        if (alert) {
            resp.addAll(["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 05 01 01 01}","delay 400"])
        }
        resp.add("he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${state.armMode ?: "00"} 00 00 00}")
        logging ("${device} : Send panel response ${remaining} alert:${alert}","trace")
    }
    return resp
}

def clearPending(){
    if (state.armPending == false) return
    def resp = []
    state.armPending = false
    logging ("${device} :  clearPending","info")
    resp.addAll(["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 00 ${state.armMode}}"]) //arm response
    if (state.bin == 1 && state.armMode == "01") {
    logging ("${device} : clearPending- armPending:${state.armPending}, armMode:${state.armMode}, bin:${state.bin}","info")

     resp.addAll([
                "delay 200","he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 05 01 01 01}","delay 1000",
                "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 ${state.armMode} 00 00 01}"
        ])
    }
    getArmResult()
    sendHubCommand(new hubitat.device.HubMultiAction(resp, hubitat.device.Protocol.ZIGBEE))
}

private getDefaultLCdata(){
    return [
            isValid:true
            ,isInitiator:false
            ,code:"0000"
            ,name:"not required"
            ,codeNumber: -1
    ]
}

private sendArmResponse(armRequest,lcData, exitDelay = null) {
   def isInitiator = false
  if (exitDelay == null) {
        isInitiator = true
        switch (armRequest) {
            case "01": //armNight
                if (state.fnPartial == "02") {
                    exitDelay = (state.armNightDelay ?: 0).toInteger()
                } else {
                    exitDelay = (state.armHomeDelay ?: 0).toInteger()
                }
                break
            case "03": //armAway
                exitDelay = (state.exitDelay ?: 0).toInteger()
                break
            default :
                exitDelay = 0
                break
        }
    }

 
    lcData.isInitiator = isInitiator

    state.delayExpire = now()
    if (armRequest != "00") state.delayExpire += (exitDelay * 1000)

    def cmds = []

    //all digital arm changes are valid
    def changeIsValid = true
    def changeText = "sucess"
    if (state.bin == -1) {
        if (armRequest == "00" && lcData.isValid == false) {
            cmds.addAll(["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 00 04}"])
            changeIsValid = false
            changeText = "invalid pin code"
        }
    }
    logging ("${device} : Password:${changeText} bin:${state.bin} armMode:${state.armMode} armRequest:${armRequest} exitDelay:${exitDelay}","trace")

    if (changeIsValid) {
        state.armMode = armRequest
        def arming = (armRequest == "01") ? "08" : (armRequest == "02") ? "09" : (armRequest == "03") ? "0A" : "00"
        state.lcData = encrypt(JsonOutput.toJson(lcData))
        if (exitDelay && armRequest != "00") {
            def hexVal = intToHexStr(exitDelay)

            state.armingMode = arming
            runIn(exitDelay + 1, clearPending)
            state.armPending = true
            cmds.addAll([
                    "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 ${armRequest == "03" ? arming : armRequest} ${hexVal} ${armRequest == "03" ? "01" : "00"} 01}"  //works, missing conf
            ])
        } else {
            state.armPending = false
            if (state.bin != 1) { //kpd
                cmds.addAll([
                        "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 00 ${armRequest}}","delay 200",
                        "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 ${armRequest} 00 01 01}"
                ])
            } else {
                cmds.addAll([
                        "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 00 ${armRequest}}", "delay 200",
                        "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 ${arming != "00" ? arming : "05"} 01 01 01}", "delay 1000",
                        "he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${armRequest} 00 00 00}"
                ])
            }
            getArmResult()
            
                
  
            
        }
        if (isInitiator) {
            def value = armRequest == "00"  ? 0 : exitDelay
//            if (state.fnPartial == "02" && armRequest != "00") {
//             log.info "${device.displayName} sent Event armRequest2- ${armRequest} delay:${value}  mode:${getArmText("02")} cmd:${getArmCmd("02")}" // Simplified logging
//               sendEvent(name:"armingIn", value: value,data:[armMode:getArmText("02"),armCmd:getArmCmd("02")], isStateChange:true)
//            } else {
// data = [armMode:"armed away",armCmd:"armAway"]  
           
            
              data = [armMode:"${getArmText(armRequest)}",armCmd:"${getArmCmd(armRequest)}"]
              sendEvent(name:"armingIn", value: value,data: data, delay: value ,isStateChange:true,descriptionText: data ) // hubitat HSM control cmd
              logging ("${device} : << send HSM event[${data}] sendArmResponce","info")

        }
    }

    return cmds
}

def updated(){
    logging ("${device} : Updated","info")
    updateEncryption()
    def crntTemp = device?.currentValue("temperature")
    if (refTemp && crntTemp && state.sensorTemp) {
        def prevOffset = (state.tempOffset ?: 0).toFloat().round(2)
        def deviceTemp = state.sensorTemp.toFloat().round(2)
        def newOffset =  (refTemp.toFloat() - deviceTemp).round(2)
        def newTemp = (deviceTemp + newOffset).round(2)
        //send new event on offSet change
        if (newOffset.toString() != prevOffset.toString()){
            state.tempOffset = newOffset
            def map = [name: "temperature", value: "${newTemp}", descriptionText: "${device.displayName} temperature offset was set to ${newOffset}°${location.temperatureScale}"]
            logging ("${device} : temperature offset was set to ${newOffset}°${location.temperatureScale}","info")
            sendEvent(map)
        }
        //clear refTemp so it doesn't get changed later...
        device.removeSetting("refTemp")
    }
	loggingStatus()
	runIn(3600,debugLogOff)
	runIn(3500,traceLogOff)
	refresh()    
}




def getArmCmd(armMode){
   // partset ["Arm Night", "Arm Home"]  
// onset   ["Arm Night", "Arm Home", "Arm Away"]    
    if (armMode == "00"){ return "disarm"}  
    if (armMode == "01"){ 
        if (PartSet == "Arm Night"){return "armNight"}
        if (PartSet == "Arm Home") {return "armHome"}
    }
    if (armMode == "03"){ 
        if (OnSet == "Arm Night"){return "armNight"}
        if (OnSet == "Arm Home") {return "armHome"}
        if (OnSet == "Arm Away") {return "armAway"} 
    }
    if (armMode =="02"){ 
        logging ("${device} : Error getArmCmd(${ParmMode})","debug")// 
        return "armHome"
    } 
    
    
    
    
 
}
// modified to match v2 keyboard PART is 01 ON is 03   (02 is not used)
def getArmText(armMode){
    
// partset ["Arm Night", "Arm Home"]  
// onset   ["Arm Night", "Arm Home", "Arm Away"]    
    if (armMode == "00"){ return "disarm"}  
    if (armMode == "01"){ 
        if (PartSet == "Arm Night"){return "armed night"}
        if (PartSet == "Arm Home") {return "armed home"}
    }
    if (armMode == "03"){ 
        if (OnSet == "Arm Night"){return "armed night"}
        if (OnSet == "Arm Home") {return "armed home"}
        if (OnSet == "Arm Away") {return "armed away"} 
    }
    if (armMode == "02"){ 
        logging ("${device} : Error getArmText(${ParmMode})","debug")// 
        return "armed home"
    }
}

private getArmResult(){
    def value = getArmText(state.armMode)
    def type = state.bin == -1 ? "physical" : "digital"
    state.bin = -1
    state.armingMode = state.armMode

    def descriptionText = "${device.displayName}  ${value} [${type}]"
    def lcData = parseJson(decrypt(state.lcData))
    state.lcData = null

    //build lock code
    def lockCode = JsonOutput.toJson(["${lcData.codeNumber}":["name":"${lcData.name}", "code":"${lcData.code}", "isInitiator":lcData.isInitiator]] )

    if (optEncrypt) { lockCode = encrypt(lockCode)}

    sendEvent(name:"securityKeypad", value: value, data:lockCode, type: type, descriptionText: descriptionText)
    def arm1 =  getArmText(state.armMode)
    def arm2 =  getArmCmd(state.armMode)
//  data = [armMode:"armed away",armCmd:"armAway"]    
    data = [armMode:arm1,armCmd:arm2]
    
//    data = "[armMode:"getArmText(armRequest)",armCmd:"getArmCmd(armRequest)"]"
    sendEvent(name:"armingIn", value: 0,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
    logging ("${device} : << send HSM event [${data}] getArmResult","info")
    
    
    if (value == "disarmed"){
        if (type == "physical"){
            cancelAlert()
            clearPending()
            
        }       
    }
    
}
//----------------------------------------------------------------------disaRM ENDS ------------------------------
// (fix for runaway water somoke alarms) only if physical
private cancelAlert(){
    
    data = [armMode:"cancel alerts",armCmd:"CancelAlerts"]
	sendEvent(name: "securityKeypad",value: "cancel alerts", data:lockCode , type: "physical",descriptionText: "[physical] ")
    sendEvent(name:"armingIn", value: 0,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
    logging ("${device} : << send HSM event ${data}","info")
}

// v2 only v3 doesnt send that i can see
private getTamperResult(rawValue){
    def value = rawValue ? "detected" : "clear"
    def descriptionText = "${device.displayName} tamper is ${value}"
    sendEvent(name: "tamper",value: value,descriptionText: "${descriptionText}")
    if (value =="detected"){logging ("${device} : Tamper: [${value}]","warn")}
    else{logging ("${device} : Tamper: [${value}]","info")}
}

private getTemperatureResult(valueRaw){
    valueRaw = valueRaw / 100
    def value = convertTemperatureIfNeeded(valueRaw.toFloat(),"c",2)
    state.sensorTemp = value
    if (state.tempOffset) {
        value =  (value.toFloat() + state.tempOffset.toFloat()).round(2).toString()
    }
    def name = "temperature"
    def descriptionText = "${device.displayName} temperature is ${value}°${location.temperatureScale}"
    sendEvent(name: name,value: value,descriptionText: descriptionText, unit: "°${location.temperatureScale}")
    logging ("${device} : temperature: ${value}°${location.temperatureScale}","info")
}

private getBatteryResult(rawValue) {
    if (rawValue == null) return
    def descriptionText
    def value
    def minVolts = 20
    def maxVolts = 30
    def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
    value = Math.min(100, pct)
    def volts = (rawValue / 10) // voltage fix
    descriptionText = "${device.displayName} battery is ${value}% ${volts}volts"
    sendEvent(name:"battery", value:value, descriptionText:descriptionText, unit: "%", isStateChange: true)
    logging ("${device} : battery is [${value}%] ${volts}volts","info")
}

// changed old code generated false results
// Call now creates motion and then times out with inactive.
private getMotionResult() {
   runIn(20,motionOff)// safety always make sure off runs
   def value = "active"
   def descriptionText = "${device.displayName} is ${value}"
   sendEvent(name: "motion",value: value,descriptionText: "${descriptionText}")
   //       sendPanelResponse()		//Iris V3 needs a response (sent elsewhere?)
   logging ("${device} : motion ${value}","info") 
}

def motionOff(){
    def value = "inactive"
    def descriptionText = "${device.displayName} motion is ${value}"
    sendEvent(name: "motion",value: value,descriptionText: "${descriptionText}")
    logging ("${device} : motion inactive","info")
    
}






def configure() {
    initialize()
    logging ("${device} : configure","info")
    def cmd = zigbee.enrollResponse(1500) + [
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0001 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0402 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0500 {${device.zigbeeId}} {}", "delay 200",
            "zdo bind 0x${device.deviceNetworkId} 1 1 0x0501 {${device.zigbeeId}} {}", "delay 200",

            "he cmd 0x${device.deviceNetworkId} 1 0x0020 0x03 {04 00}","delay 200",  						//short poll interval
            "he cmd 0x${device.deviceNetworkId} 1 0x0020 0x02 {13 00 00 00}","delay 200", 					//long poll interval
            "he raw 0x${device.deviceNetworkId} 1 1 0x0020 {00 01 02 00 00 23 E0 01 00 00}","delay 200",	//check in interval

            //reporting
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0020 0x20 1 86400 {01}","delay 200",//battery
            "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0x0000 0x29 60 0xFFFE {3200}", "delay 500" //temp
    ] + refresh()
    return cmd
}





def refresh() {
    logging ("${device} : refresh","info")
    return [
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0020 {}","delay 200",  //battery
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0 {}","delay 200",  //temp
    ] + sendPanelResponse(false)
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


