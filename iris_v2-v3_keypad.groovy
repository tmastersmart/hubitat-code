  /*
 Iris v2 - v3 keypad driver
=============================================================
Driver supports model# 1112-S and model# 3405-L iris keypads
Maker iMagic by GreatStar

 v1.6 09/13/2026 Debuging and rewriting parts of the code.
 V1.2 02/25/2022 V2 working. v3 Working.
 v1.1 02/23/2022 Tested on v2 and v3 kaypads. 
 v1.0 02/22/2022 Beta test copy 

   ========================================================================
   Driver Improvements Over Stock Keypad Drivers
   ========================================================================

   • Fixes inability to cancel alarms on Iris V3.
   • Restores proper beep behavior on models that normally stay silent.
   • Adds Iris-style door chime support (legacy Iris behavior).
   • Adds Panic support for button controllers (silent alarm mode).
   • Improves entry countdown timing and LED behavior.
   • Filters out bad temperature and battery garbage values
     produced by some Iris keypads.

   ========================================================================
   Iris V3 Keypad Behavior Notes
   ========================================================================

   • Iris V3 does NOT support true Night mode.
     - ACE cmd 02 (setArmedNight) → treated as Stay/Partial.
     - ACE cmd 09 (Exit Night) → ignored entirely.

   • Iris V3 uses ACE cmd 07 (with empty data) as a motion wake‑up ping.
     - This is NOT an alarm.
     - Driver must respond with a panel ACK.

   • Exit delay beeps accelerate during the final 10 seconds.
     - Applies to Stay (cmd 08) and Away (cmd 10).
     - Iris V3 LED behavior differs from Centralite.

   • Panic (cmd 04) and Alarm (cmd 07) behave differently on V2 vs V3.
     - Iris V3 uses a beep‑based alarm tone instead of full siren.

   • LED behavior differs significantly between keypad families.
     - Centralite: Stay/Night/Away icons.
     - Iris V3: Partial/On/Off LEDs only.

   • ACE commands are server → client (hub → keypad).
     - Keypad → hub uses IAS Zone, IAS ACE, and manufacturer clusters.

   ========================================================================
   Iris V3 PIN / Arming Quirk
   ========================================================================

   • Iris V3 is hardwired to reject PIN entry while unarmed.
     - A valid PIN immediately disarms the keypad.
     - To stop an Alert, the driver must temporarily arm the keypad
       and wait for a PIN.
     - The keypad can only perform one disarm action per cycle.

   ========================================================================



  v3 softwareBuild: 10036230 firmwareMT: 123B-0012-10036230  tested working 
  v2 softwareBuild: 10025310 firmwareMT: 104E-0021-10025310  tested working
  v2 softwareBuild: 140B5310 has a volume problem holding 2 raises volume but it goes back to 0

Iris v3 fccid:2AMI2IL02 model:3405-L marked model:IL02_01
Iris v2 fccid:_________ model:3405-L marked model:_______


   ========================================================================
   Iris / Centralite / UEI Keypad – ACE (0x0501) Command Reference
   ========================================================================

   These notes document how different keypads respond to ACE commands
   sent from the hub (server → client). Behavior varies between models.

   ------------------------------------------------------------------------
   00 – setDisarmed
   ------------------------------------------------------------------------
   Centralite: All icons off
   Iris V2/V3: “Off” LED on
   Meaning: Ready to arm

   ------------------------------------------------------------------------
   01 – setArmedStay
   ------------------------------------------------------------------------
   Centralite: Stay icon lit
   Iris V2/V3: Partial LED lit
   Meaning: Arm Stay

   ------------------------------------------------------------------------
   02 – setArmedNight
   ------------------------------------------------------------------------
   Centralite: Night icon lit
   Iris V2: No response
   Iris V3: Lights Partial (same as Stay)
   Meaning: Arm Night (Iris V3 does NOT support true Night mode)

   ------------------------------------------------------------------------
   03 – setArmedAway
   ------------------------------------------------------------------------
   Centralite: Away icon lit
   Iris V2/V3: ON LED lit
   Meaning: Arm Away

   ------------------------------------------------------------------------
   04 – Panic Sound
   ------------------------------------------------------------------------
   Duration: seconds (0–255)
   Iris V2/V3: Siren tone (V3 may use beep instead of full siren)
   Centralite: Siren tone
   Used for panic or alarm confirmation

   ------------------------------------------------------------------------
   05 – Entry Delay Beep
   ------------------------------------------------------------------------
   Fast beep (1/sec), duration in seconds
   Iris V2/V3: Keeps status LEDs unchanged
   Used when system is armed and a door opens

   ------------------------------------------------------------------------
   06 – Not Ready to Arm
   ------------------------------------------------------------------------
   Centralite: Amber blink
   Iris V3: Refuses to arm
   Meaning: Open sensor or fault condition

   ------------------------------------------------------------------------
   07 – Zigbee In Alarm
   ------------------------------------------------------------------------
   Iris V2/V3: Full siren/beep alarm
   Centralite: Siren
   NOTE: Iris V3 also sends cmd:07 with empty data as a motion wake‑up ping

   ------------------------------------------------------------------------
   08 – setExitStay (Exit Delay – Stay)
   ------------------------------------------------------------------------
   Slow beep → fast beep last 10 seconds
   Iris V3: Partial LED blinks, red status flash
   Duration: seconds

   ------------------------------------------------------------------------
   09 – setExitNight (Exit Delay – Night)
   ------------------------------------------------------------------------
   Centralite: Night icon blink
   Iris V3: No response
   Duration: seconds

   ------------------------------------------------------------------------
   10 – setExitAway (Exit Delay – Away)
   ------------------------------------------------------------------------
   Slow beep → fast beep last 10 seconds
   Iris V3: ON LED blinks, red status flash
   Duration: seconds



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
        command "entry" 
        command "setPartialFunction"

        attribute "armingIn", "NUMBER"
        attribute "lastCodeName", "STRING"

        fingerprint model:"1112-S", manufacturer:"iMagic by GreatStar",profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0405,0500,0501,0B05,FC01,FC02,FC04", outClusters:"0003,0019,0501", deviceJoinName:"Iris V3 Keypad"
        fingerprint model:"1112-S", manufacturer:"iMagic by GreatStar",profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0405,0500,0501,0B05,FC01,FC02",      outClusters:"0003,0019,0501", deviceJoinName:"Iris V3 Keypad old firmware"
        fingerprint model:"3405-L", manufacturer:"CentraLite",         profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0500,0501,0B05,FC04",                outClusters:"0019,0501",      deviceJoinName:"Iris V2 Keypad"
        fingerprint model:"3405-L", manufacturer:"CentraLite",         profileId:"0104", inClusters:"0000,0001,0003,0020,0402,0500,0501,0B05,FC04,FC05",           outClusters:"0019,0501",      deviceJoinName:"Iris V2 Keypad"

    
    }

    preferences{
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: true,required: true
    input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true


        input name: "SilentArmHome", type: "bool", title: "Silent Arming Home", description: "No beep while arming", defaultValue: false
	    input name: "SilentArmAway", type: "bool", title: "Silent Arming Away", description: "No beep while arming", defaultValue: false
	    input name: "SilentArmNight",type: "bool", title: "Silent Arming Night",description: "No beep while arming", defaultValue: false
    
        input name: "PartSet", type: "enum", title: "Partial Button", description: "Customize Partial Button",  options: ["Arm Night", "Arm Home"], defaultValue: "Arm Night",required: true 
        input name: "OnSet",   type: "enum", title: "On Button", description: "Customize ON Button",  options: ["Arm Night", "Arm Home", "Arm Away"], defaultValue: "Arm Away",required: true 

        input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: "Hides code from log"
        input "refTemp", "decimal", title: "Reference temperature", defaultValue: "0" , description: "Adjust the temp", range: "*..*"

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
    state.bin = -1
    state.armMode = "00"
    state.fnPartial = "01"
    
    sendEvent(name:"maxCodes", value:20)
    sendEvent(name:"codeLength", value:4)
    sendEvent(name:"alarm", value: "off")
    sendEvent(name:"securityKeypad", value: "disarmed")  
    sendEvent(name:"numberOfButtons", value: "1", isStateChange: false)
    
   	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"true",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
}

	


def uninstalled(){
    return zigbee.command(0x0000,0x00)
}

def parse(String description) {

    if (description.startsWith("zone status")) {
        logging ("${device} : description: ${description}","trace")  
        def zoneStatus = zigbee.parseZoneStatus(description)
        if (state.model == "1112-S"){
        logging ("${device} : Ignoring tamper on 1112-S ","debug") 
        return
        }// just ignore its always inactive
        
        
        getTamperResult(zoneStatus.tamper)
        return
    } else if (description.startsWith("enroll request")) {
        logging ("${device} : description: ${description}","trace")  
        return
    } else {
        def descMap = zigbee.parseDescriptionAsMap(description)
        logging ("${device} : descMap: ${descMap}","trace")
       
def clusterId = descMap.clusterId ?: descMap.cluster

// Normalize clusterId BEFORE any logging or lookup
clusterId = clusterId.toString().toUpperCase().replace("0X", "").padLeft(4, '0')

def cmd = descMap.command
def resp = []
def clusterName = [
    "0501": "Keypad (ACE)",
    "0500": "IAS Zone",
    "0402": "Temperature",
    "0001": "Power Configuration",
    "0013": "Device Announce",
    "0006": "On/Off (Housekeeping)",
    "0020": "Poll Control",
    "8021": "ZDO Route Record / LQI",
    "0000": "ZDO (General Command)",
    "8020": "ZDO Bind/Unbind Response",
    "8038": "ZDO Leave/Join",
    "8035": "ZDO Mgmt Permit Join",
    "8034": "ZDO Mgmt LQI Response",
    "8032": "ZDO Mgmt Routing Response",
    "8031": "ZDO Mgmt Bind Response"
][clusterId] ?: "Unknown Cluster"

logging("${device} : Cluster ${clusterId} (${clusterName}) cmd:${cmd} value:${descMap.value} data:${descMap.data} state${state.bin}", "trace")


    switch (clusterId) {
        
case "0001":
    if (descMap.command == "01" &&
        descMap.attrId == "0020" &&
        descMap.value &&
        descMap.value.matches(/[0-9A-Fa-f]+/)
    ) {
        def raw = hexStrToUnsignedInt(descMap.value)
        if (raw == null) {
            logging("${device} : 0001 Unknown Power - frame value:${descMap.value} raw:${raw}", "debug")
            break
        }

        // sanity filter: ignore impossible battery voltages
        if (raw < 20 || raw > 65) {  
            logging("${device} : 0001 Bad battery - value value:${descMap.value} raw:${raw}", "debug")
            break
        }

        getBatteryResult(raw)
    } else {
        logging("${device} : 0001 Unknown Power - cmd:${descMap.command} value:${descMap.value}", "debug")
    }
    break

            
case "0402":
    if (descMap.command == "01" && descMap.value) {
        def raw = hexStrToUnsignedInt(descMap.value)
        if (raw == null) {
            logging("${device} : 0402 Bad Temp frame value:${descMap.value}", "debug")
            break
        }

        // sanity filter: ignore impossible temps
        if (raw < 500 || raw > 4000) {   // 5°C to 40°C
            logging("${device} : 0402 Bad temp raw:${raw} out of range", "debug")
            break
        }

        getTemperatureResult(raw)
    } else {
        logging("${device} : 0402 Unknown temp cmd:${descMap.command} value:${descMap.value}", "debug")
    }
    break

            
    case "0013":
    state.ieee = descMap.data[0..7].join()
    state.nwk = descMap.data[8..9].join()
    state.capabilities = descMap.data[11]
    logging("${device} : Device Announce IEEE:${state.ieee} NWK:${state.nwk}", "trace")
    break

    case "0006":
    state.parent = descMap.data[1..2].join()
    state.lqi = descMap.data[3]
    logging("${device} : On/Off housekeeping parent:${state.parent} LQI:${state.lqi}", "trace")
    logging("${device} : LQI:${state.lqi}", "info")
    break         

    // not all keypads even use this but they report all clear    
    case "0500":       
    def raw = zigbee.convertHexToInt(descMap.data[0])
    def tamperActive = (raw & 0x04) != 0
    logging("${device} : IAS Zone tamper raw:${raw} active:${tamperActive}", "trace")
    getTamperResult(tamperActive)
    break
        
      
   case "0501":
            // motion comes in we must respond back
                if (cmd == "07" && descMap.data.size() == 0) { 
                    if (state.bin == -1) getMotionResult()
                    resp.addAll(sendPanelResponse(false))
                    
                }   else if (cmd == "00") {
                    state.bin = -1
                    def armRequest = descMap.data[0] 
                    def asciiPin = "0000"
                    def modeName = ["00": "Disarm","01": "Arm Partial","03": "Arm Away"][armRequest] ?: "Unknown (${armRequest})"// Beter log support
                    logging("${device} : Received ${modeName} (${armRequest})", "info")

                    if (armRequest == "01") { countdown(state.armNightDelay) }
                    if (armRequest == "03") { countdown(state.armAwayDelay) }
                    if (armRequest == "00") { asciiPin = descMap.data[2..5].collect{ (char)Integer.parseInt(it, 16) }.join()} // if disarm need a pin
                    
                    resp.addAll(sendArmResponse(armRequest,isValidPin(asciiPin, armRequest)))

                } else if (cmd == "04") { //panic button
                    logging ("${device} : Panic button pressed (pushed)","warn")
                    createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "panic button was pushed", isStateChange: true)
                  state.bin = 1
                  state.panic = "active"
                  sendEvent(name: "alarm",value: "alarm",descriptionText: "panic button was pushed")  
                  runIn(9,buttonRelease)
//                modified for silent alarm                    
//                resp.addAll(siren())
                }  
                    
                else {logging ("${device} : 0501 cmd:${cmd} untrapped  ${descMap}","debug")}
                break



            
            default :
             logging("${device} : Untrapped Cluster ${clusterId} (${clusterName}) cmd:${cmd} value:${descMap.value} data:${descMap.data} state${state.bin}", "trace")
        }
        
        if (resp){ 
           sendHubCommand(new hubitat.device.HubMultiAction(resp, hubitat.device.Protocol.ZIGBEE)) 
            logging ("${device} : send Response ${resp}","trace")
           }
    }
}





def beep(){
    state.model = getDataValue("model")
    if (state.model == "1112-S"){
        if (BeepCode == "2"){BeepCode = "3"} // v3 doesnt support beep 2   
    }
  
    cmd = BeepCode
    playSound(cmd)
    logging ("${device} : beep cmd ${cmd}","info") 
}


void beepBad(){
playSound(3) 
}

def stop(){
stopBeep()
}

void stopBeep(){
    stopBee()
}
def stopBee(){
    logging ("${device} : Beep Stop","info") 
    cmds = ["raw 0x0501 {09 01 04 05 00 01}","delay 200","send 0x${device.deviceNetworkId} ${device.endpointId as int} 1","delay 500"] 
    logging ("${device} :Send ${cmds}","trace")
    return cmds   
}

def countdown(delay){
    logging ("${device} : countdown ${delay}","info")
    def cmds = ["raw 0x0501 {09 01 04 05 ${delay} 01}", "delay 200","send 0x${device.deviceNetworkId} ${device.endpointId as int} 1","delay 500"] 
    logging ("${device} :Send ${cmds}","trace")
    return cmds
}
    

def playSound(cmd){
    if (cmd == null){cmd=1}
    if (cmd >= 6){cmd=1}
    
    state.model = getDataValue("model")
    if (state.model == "1112-S"){
        if (BeepCode == "2"){BeepCode = "3"} // v3 doesnt support beep 2   
    }
    
    runIn(9,stopBeep) // stops the countdown timmer or you get 2nd beep in 10 sec
    // Length mapping
    def lenMap = [1: "01",2: "02",3: "03",4: "05",5: "07"]
    def len = lenMap[cmd]
    logging ("${device} : Playing chime ${cmd} Len ${len}","info") 

// Fast beep (1 per second)  
 def cmds
    if (cmd == 2 && state.model != "1112-S") {
        // V2 special beep
        cmds = ["raw 0xFC04 {15 4E 10 00 00 00}","delay 200","send 0x${device.deviceNetworkId} ${device.endpointId as int} 1","delay 500"]
    } else {
        // Standard chime
        cmds = ["raw 0x0501 {09 01 04 05 ${len} 01}","delay 200","send 0x${device.deviceNetworkId} ${device.endpointId as int} 1","delay 500"]
    }
    
// notes:
//  [raw 0x0501 {09 01 04 05 01}, delay 200, send 0xB7AA 1 1, delay 500]   
//    	09 - Frame Ctl
//		01 - Transaction
//		04 - Cmd Panel Status Changed Cmd
//		05 - Entry Delay
//      01 - Seconds
  

logging ("${device} :Send ${cmds}","trace") 
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


void setPartialFunction(mode = null) {
    logging ("${device} : set Partial ${mode}","trace") 
    if ( !(mode in ["armHome","armNight"]) ) {logging ("${device} : custom command used by HSM","trace")}
    else if (mode in ["armHome","armNight"]) { state.fnPartial = mode == "armHome" ? "01" : "02" }
    
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
    logging ("${device} : HSM Entry in progress delay:${entranceDelay}","info")

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




def disarm(exitDelay = null) {
        if (state.armMode == "00") {
        sendPanelResponse(false)
        logging ("${device} : HSM Disarm (ignored)","info")
        return
    }

    logging ("${device} : HSM disarm armMode:${state.armMode})","info")
    state.armPending = false
    state.bin = 1
    sendArmResponse("00",getDefaultLCdata())// disarm should not have a delay
}

def armHome(exitDelay = null) {
    if (state.armMode == "01") {
        sendPanelResponse(false)
        logging ("${device} : HSM armHome (ignored)","info")
        return
    }
    logging ("${device} : HSM armHome delay:${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")

    state.bin = 1
    if (exitDelay == null) sendArmResponse("01",getDefaultLCdata())
    else sendArmResponse("01",getDefaultLCdata(),exitDelay.toInteger())
}

def armNight(exitDelay = null) {
    if (state.armMode == "01") {
        sendPanelResponse(false)
        logging ("${device} : HSM armNight (ignored)","info")
        return
    } 
    logging ("${device} : HSM armNight delay:${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
 
    state.bin = 1
    if (exitDelay == null) sendArmResponse("01",getDefaultLCdata())
    else sendArmResponse("01",getDefaultLCdata(),exitDelay.toInteger())
}

def armAway(exitDelay = null) {
    if (state.armMode == "03") {
        sendPanelResponse(false)
        logging ("${device} : HSM armAway (ignored)","info")
        return
    }

    logging ("${device} : HSM armAway delay:${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
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


  
  if (state.model == "1112-S"){  
//===============================SMOKE detection=============================================    
//    hsmAlert intrusion intrusion-home intrusion-night smoke water
    if (location.hsmAlert == "smoke" | location.hsmAlert == "water"){
        if(state.armMode == "00"){
        armAway(0) // make sure keypad is armed so it can disarm a smoke alarm
        logging ("${device} : HSM Alert${location.hsmAlert}. Arming keypad so it can disable!","warn")
     }
    }
// ==============================fix being unable to disarm with keypad out of sync===========    
    if (location.hsmAlert == "intrusion" | location.hsmAlert == "intrusion-home"| location.hsmAlert == "intrusion-night"){
        if(state.armMode == "00"){
        armAway(0) // make sure keypad is armed 
        logging ("${device} : HSM Alert${location.hsmAlert}. Keypad was out of sync rearming","warn")
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
//    strobe() // strobe doesnt work
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
    if (lockCode) { codeMap = ["name":"${lockCode.name}", "code":"${lockCode.code}"] }
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
        if (optEncrypt && lockCodes[0] == "{") {      sendEvent(name:"lockCodes",value: encrypt(lockCodes), isStateChange:true)}
        else if (!optEncrypt && lockCodes[0] != "{") {sendEvent(name:"lockCodes",value: decrypt(lockCodes), isStateChange:true)}
        else {sendEvent(name:"lockCodes",value: lockCodes, isStateChange:true)}
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
            descriptionText = "${device.displayName} was disarmed by ${data.name}"
            sendEvent(name: "lastCodeName", value: data.name, descriptionText: descriptionText, isStateChange: true)
            logging ("${device} : was disarmed by ${data.name}","info")
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
    if (remaining < 0) { remaining = 0} 

    if (remaining > 3) {
        runIn(2,"sendPanelResponse")
        resp.add("he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${state.armingMode} ${intToHexStr(remaining)} 01 01}")
        logging ("${device} : Send panel response remaining:${remaining} alert:${alert}","trace")
    } else {
        if (alert) {resp.addAll(["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 05 01 01 01}","delay 400"])}
        resp.add("he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${state.armMode ?: "00"} 00 00 00}")
        logging ("${device} : send Panel response remaining:${remaining} alert:${alert}","trace")
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
    return [isValid:true,isInitiator:false,code:"0000",name:"not required",codeNumber: -1]
}

private sendArmResponse(armRequest,lcData, exitDelay = null) {
   def isInitiator = false
  if (exitDelay == null) {
        isInitiator = true
        switch (armRequest) {
            case "01": //armNight
                exitDelay = (state.armNightDelay ?: 0).toInteger()
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
    logging ("${device} : sendArmResponse Password:${changeText} bin:${state.bin} armMode:${state.armMode}  armRequest:${armRequest} exitDelay:${exitDelay}","debug")

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
              logging ("${device} : send HSM event armingIn [${data}] sendArmResponce","info")

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

    loggingUpdate()
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
    if (armMode =="02"){return "armHome"} 
}


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
    if (armMode == "02"){return "armed home"}
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
// remove the duplication of events
//    sendEvent(name:"armingIn", value: 0,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
//    logging ("${device} : send HSM event armingIn [${data}] getArmResult","info")
    
    
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


// v3 tamper doesnt actualy work but it will send the clear message
// v2 only
private getTamperResult(rawValue){
    def value = rawValue ? "detected" : "clear"
    def descriptionText = "${device.displayName} tamper is ${value}"
    sendEvent(name: "tamper",value: value,descriptionText: "${descriptionText}")
    logging ("${device} : tamper:${value}","info")
}

private getTemperatureResult(valueRaw){
    valueRaw = valueRaw / 100
    def value = convertTemperatureIfNeeded(valueRaw.toFloat(),"c",2)
    state.sensorTemp = value
    if (state.tempOffset) {
        value =  (value.toFloat() + state.tempOffset.toFloat()).round(2).toString()
    }
    def name = "temperature"
    def descriptionText = "${device.displayName} ${name} is ${value}°${location.temperatureScale}"
    sendEvent(name: name,value: value,descriptionText: descriptionText, unit: "°${location.temperatureScale}")
    logging ("${device} : temp:${value}°${location.temperatureScale}","info")
}


private getBatteryResult(rawValue) {
    if (!rawValue || rawValue == "null") {
    logging("Battery report ignored: rawValue was null","warn")
    return
}
    

    def descriptionText
    def value
    def minVolts = 20
    def maxVolts = 30

    if (state.model == "1112-S"){  // this model uses AA batteries and 64 = 6.4 volts
    minVolts = 43
    maxVolts = 64
    } 
    
    logging ("${device} : battery tracing  ${rawValue} - ${minVolts}) / (${maxVolts} - ${minVolts} ","trace")
    def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
    value = Math.min(100, pct)
    descriptionText = "${device.displayName} battery is ${value}%"
    sendEvent(name:"battery", value:value, descriptionText:descriptionText, unit: "%", isStateChange: true)
    logging ("${device} : battery is ${value}% ","info")// volts removed ${rawValue}volts
}

// changed old code generated false results
// Call now creates motion and then times out with inactive.
private getMotionResult() {
   runIn(5,motionOff)// safety always make sure off runs
   def value = "active"
   def descriptionText = "${device.displayName}  Motion is active"
   sendEvent(name: "motion",value: value,descriptionText: "${descriptionText}")
   //       sendPanelResponse()		//Iris V3 needs a response (sent elsewhere?)
   logging ("${device} : motion active","info") 
}







def motionOff(){
    def value = "inactive"
    def descriptionText = "${device.displayName} motion is inactive"
    sendEvent(name: "motion",value: value,descriptionText: "${descriptionText}")
    logging ("${device} : motion inactive","info")
    
}






def configure() {
    initialize()
    logging("${device} : configure","info")

    def cmds = []

    // IAS enroll
    cmds += zigbee.enrollResponse(1500)

    // Required binds
    cmds += [
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0001 {${device.zigbeeId}} {}", "delay 200",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0402 {${device.zigbeeId}} {}", "delay 200",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0500 {${device.zigbeeId}} {}", "delay 200",
        "zdo bind 0x${device.deviceNetworkId} 1 1 0x0501 {${device.zigbeeId}} {}", "delay 200"
    ]

    // Reporting
    cmds += [
        "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0020 0x20 1 86400 {01}", "delay 200",
        "he cr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0x0000 0x29 60 0xFFFE {3200}", "delay 200"
    ]

    // Optional: refresh
    cmds += refresh()

    return cmds
}





def refresh() {
    logging ("${device} : refresh","info")
    return [
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0001 0x0020 {}","delay 200",  //battery
            "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0402 0 {}","delay 200",  //temp
    ] + sendPanelResponse(false)
}

// Logging block  v4

void loggingUpdate() {
    logging("${device} : Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("${device} : Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("${device} : Trace log: off in 1800s", "warn")
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
    if (level == "infoBypass"){log.info  "$message"}
	if (level == "error"){     log.error "$message"}
	if (level == "warn") {     log.warn  "$message"}
	if (level == "trace" && traceLogging) {log.trace "$message"}
	if (level == "debug" && debugLogging) {log.debug "$message"}
    if (level == "info"  && infoLogging)  {log.info  "$message"}
}

