/*
 Iris v2 - v3 keypad driver
==================================================================================================

  fix for being unable to cancel alarms
  Panic support added for button controler. silent alarm
  

  

  v3 softwareBuild: 10036230 firmwareMT: 123B-0012-10036230  tested working 
  v2 softwareBuild: 10025310 firmwareMT: 104E-0021-10025310  tested working
  v2 softwareBuild: 140B5310 has a volume problem holding 2 raises volume but it goes back to 0
 


 V1.2 2/25/2022 V2 working. v3 Working.
 v1.1 2/23/2022 Tested on v2 and v3 kaypads. 
 v1.0 2/22/2022 Beta test copy 








================================================================================================= 
--Forked from example drivers at 
https://github.com/hubitat/HubitatPublic/tree/master/examples/drivers
https://github.com/hubitat/HubitatPublic/blob/master/examples/drivers/irisKeypadV3.groovy
2019-12-13 2.1.8 maxwell
Iris V3 Keypad
Copyright 2016 -> 2020 Hubitat Inc.  All Rights Reserved

*/
def clientVersion() {
    TheVersion="1.2"
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
        
        input name: "BeepCode", type: "enum", title: "Beep code", description: "Which beep to use",  options: ["1 Standard", "2 Alt (not for v3)","3","4","5","6"], defaultValue: "1 Standard",required: true 


        input name: "optEncrypt", type: "bool", title: "Enable lockCode encryption", defaultValue: false, description: ""
        input "refTemp", "decimal", title: "Reference temperature", description: "Enter current reference temperature reading", range: "*..*"

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
    sendEvent(name:"maxCodes", value:20)
    sendEvent(name:"codeLength", value:4)
    sendEvent(name:"alarm", value: "off")
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
 //       if (logEnable) log.debug "${device.displayName} descMap: ${descMap}"// extra debugging code 
        def resp = []
        def clusterId = descMap.clusterId ?: descMap.cluster
        def cmd = descMap.command

        switch (clusterId) {
            case "0001":
                if (descMap.value) {
                    value = hexStrToUnsignedInt(descMap.value)
                    getBatteryResult(value)
                }
                break
            case "0501":
                if (cmd == "07" && descMap.data.size() == 0) { //get panel status client -> server
                    if (state.bin == -1) getMotionResult("active")
                    resp.addAll(sendPanelResponse(false))
                } else if (cmd == "00") {
                    state.bin = -1
                    def armRequest = descMap.data[0]
                    def asciiPin = "0000"
                    logging ("${device} : cmd:${cmd} armRequest${armRequest} ${descMap.data}","info")
                    // 01 away 03 away
                    if (armRequest == "00") { asciiPin = descMap.data[2..5].collect{ (char)Integer.parseInt(it, 16) }.join()}
                    
                    resp.addAll(sendArmResponse(armRequest,isValidPin(asciiPin, armRequest)))

                } else if (cmd == "04") { //panic button
                    logging ("${device} : Panic button pressed","warn")
                    createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName panic button was pushed", isStateChange: true)
//                  state.bin = 1
//                  state.panic = "active"
                    runIn(10, off())
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
            logging ("${device} : ERROR unsupported command :${description}","warn")
                break
            default :
            logging ("${device} : Untrapped cluster ${clusterId} Idevent:${description}","trace")
        }
        if (resp){
            sendHubCommand(new hubitat.device.HubMultiAction(resp, hubitat.device.Protocol.ZIGBEE))
        }
    }
}

def beep(){
    state.model = getDataValue("model")
    logging ("${device} : beep ","info")
    if (state.model == "1112-S"){ 
        playSound(1)// v3 doesnt support beep 2
        return
    } 
    playSound(2) 
}

void beepBad(){
playSound(3) 

}


//capability "Chime"
def playSound(cmd){
  logging ("${device} : playing chime ${cmd}","info") 

  if (cmd==1) return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 05 01 01 01}"] // beep one time 4=status change 5=entry 
  if (cmd==2) return ["he raw 0x${device.deviceNetworkId} 1 1 0xFC04 {15 4E 10 00 00 00}"]  // is a ok beep (does not work on v3)
//  if (cmd==3) return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 00 04}"] // invalid pin (not working on v2 or v3)
  if (cmd==3) return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 05 04}"]  //beep 4 times
  if (cmd==4) return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 05 05}"]  //beep 5 times 2 timeson v3
  if (cmd==5) return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 04 05 09}"] 
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

// We dont need this( to be removed)  
//    if ( !(mode in ["armHome","armNight"]) ) {
//        if (txtEnable) log.warn "${device.displayName} custom command used by HSM"
//    } else if (mode in ["armHome","armNight"]) {
//        state.fnPartial = mode == "armHome" ? "01" : "02"
//    }
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

def entry(){
    logging ("${device} : >> Entry in progress ","info")
    def intDelay = state.entryDelay ? state.entryDelay.toInteger() : 0
    if (intDelay) return entry(intDelay)
}

// Hub says entry in process
def entry(entranceDelay){
    logging ("${device} : >> Entry in progress delay:${entranceDelay}","info")
    if (entranceDelay) {
        def ed = entranceDelay.toInteger()
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
// HUB says DISARM
def disarm(exitDelay = null) {
    logging ("${device} : >> Hub sent disarm","info")
    state.armPending = false
    state.bin = 1
    sendArmResponse("00",getDefaultLCdata())// disarm should not have a delay
}

def armHome(exitDelay = null) {
    if (state.armMode == "01") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub sent ArmHome (already armed) armMode:${state.armMode} armingMode:${state.armingMode})","warn")
        return
    }
    logging ("${device} : >> armHome delay ${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
    state.bin = 1
    if (exitDelay == null) sendArmResponse("01",getDefaultLCdata())
    else sendArmResponse("01",getDefaultLCdata(),exitDelay.toInteger())
}

def armNight(exitDelay = null) {
    if (state.armMode == "01") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub armNight already armed) armMode:${state.armMode} armingMode:${state.armingMode})","warn")
 
        return
    } 
    logging ("${device} : >> armNight delay ${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
 
    state.bin = 1
    if (exitDelay == null) sendArmResponse("01",getDefaultLCdata())
    else sendArmResponse("01",getDefaultLCdata(),exitDelay.toInteger())
}

def armAway(exitDelay = null) {
    
    if (state.armMode == "03") {
        sendPanelResponse(false)
        logging ("${device} : >> Hub armAway already armed) armMode:${state.armMode} armingMode:${state.armingMode})","warn")
 
        return
    }

    logging ("${device} : >> armAway delay ${exitDelay} armMode:${state.armMode} armingMode:${state.armingMode})","info")
 
    state.bin = 1
    if (exitDelay == null) sendArmResponse("03",getDefaultLCdata())
    else sendArmResponse("03",getDefaultLCdata(),exitDelay.toInteger())
}

//alarm commands

def off(){
    def value = "off"
    def descriptionText = "${device.displayName} alarm was turned ${value}"
    if (txtEnable) log.info "${descriptionText}"
    state.bin = -1
    state.panic = "inactive"
    sendEvent(name: "alarm",value: value,descriptionText: "${descriptionText}")
    return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 04 00 00 01 04}"] //clear
    logging ("${device} : Off alarm/strobe","info")
 
    
}

def siren(){
    if (state.panic == "inactive") {
        state.bin = 1
        state.panic = "active"
        def value = "siren"
        def descriptionText = "${device.displayName} alarm set to ${value}"
        logging ("${device} : ON alarm","info")
        sendEvent(name: "alarm",value: value,descriptionText: "${descriptionText}")
    }
    return ["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 04 07 00 01 01}"]
}

def strobe(){
//    siren()
   logging ("${device} : ON strobe","info")
   List cmds = ["raw 0x501 {09 01 04 ${zigbee.convertToHexString(6,2)}${zigbee.convertToHexString(1,2)}}",
  			 "send 0x${device.deviceNetworkId} 1 1", 'delay 100']

 cmds
}

def both(){
    siren()
    strobe()
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
            if (logEnable && nameInUse) { log.warn "${device.displayName} changeIsValid:false, name:${name} is in use:${ lockCodes.find{ it.value.name == "${name}" } }" }
            if (logEnable && codeInUse) { log.warn "${device.displayName} changeIsValid:false, code:${code} is in use:${ lockCodes.find{ it.value.code == "${code}" } }" }
            result = false
        }
    }
    if (isBadLength || isBadCodeNum) {
        if (logEnable && isBadLength) { logging ("${device} : length of code ${code} <> ${codeLength}","warn")}
        if (logEnable && isBadCodeNum) {logging ("${device} : To many codes! maxCodes=${maxCodes}","warn") }
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
            descriptionText = "${device.displayName} was disarmed by ${data.name}"
            sendEvent(name: "lastCodeName", value: data.name, descriptionText: descriptionText, isStateChange: true)
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
    logging ("${device} : Send panel response ${remaining}","trace")
    if (remaining > 3) {
        runIn(2,"sendPanelResponse")
        resp.add("he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${state.armingMode} ${intToHexStr(remaining)} 01 01}")
    } else {
        if (alert) {
            resp.addAll(["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 05 01 01 01}","delay 400"])
        }
        resp.add("he raw 0x${device.deviceNetworkId} 1 1 0x0501 {19 01 05 ${state.armMode ?: "00"} 00 00 00}")
    }
    return resp
}

def clearPending(){
    if (state.armPending == false) return
    def resp = []
    state.armPending = false
    logging ("${device} : Clear Pending","info")
    resp.addAll(["he raw 0x${device.deviceNetworkId} 1 1 0x0501 {09 01 00 ${state.armMode}}"]) //arm response
    if (state.bin == 1 && state.armMode == "01") {
        log.warn "${device.displayName} clearPending- armPending:${state.armPending}, armMode:${state.armMode}, bin:${state.bin}"
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
   logging ("${device} : armRequest${armRequest} ${getArmText(armRequest)} ${getArmCmd(armRequest)}","info")
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
    logging ("${device} : sendArmResponse${changeText} bin:${state.bin} armMode:${state.armMode} -> armRequest:${armRequest} exitDelay:${exitDelay}","trace")

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
              logging ("${device} : sent Event armRequest${armRequest} delay:${value} mode:${getArmText(armRequest)} cmd:${getArmCmd(armRequest)}","info")

              sendEvent(name:"armingIn", value: value,data:[armMode:getArmText(armRequest),armCmd:getArmCmd(armRequest)], isStateChange:true)
//            }
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
            if (txtEnable) log.info "${map.descriptionText}"
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
    switch (armMode){
        case "00": return "disarm"
        case "01": return "armNight"
        case "02": return "armHome"
        case "03": return "armAway"
    }
}
// modified to match v2 keyboard PART is 01 ON is 03   (02 is not used)
def getArmText(armMode){
    switch (armMode){
        case "00": return "disarm"
        case "01": return "armed night"
        case "02": return "armed home"
        case "03": return "armed away"
    }
}

private getArmResult(){
    def value = getArmText(state.armMode)
    def type = state.bin == -1 ? "physical" : "digital"
    state.bin = -1
    state.armingMode = state.armMode

    def descriptionText = "${device.displayName} was ${value} [${type}]"
    def lcData = parseJson(decrypt(state.lcData))
    state.lcData = null

    //build lock code
    def lockCode = JsonOutput.toJson(["${lcData.codeNumber}":["name":"${lcData.name}", "code":"${lcData.code}", "isInitiator":lcData.isInitiator]] )
    if (txtEnable) log.info "${descriptionText}"
    if (optEncrypt) { lockCode = encrypt(lockCode)}

    sendEvent(name:"securityKeypad", value: value, data:lockCode, type: type, descriptionText: descriptionText)
    logging ("${device} : ${value} type${type}","info")
    
    
    if (value == "disarmed"){
        if (type == "physical"){cancelAlert()}
    }
    
}

// (fix for runaway water somoke alarms) only if physical
private cancelAlert(){
    logging ("${device} : cancel alerts","info")
    data = [armMode:"cancel alerts",armCmd:"CancelAlerts"]
	sendEvent(name: "securityKeypad",value: "cancel alerts", data:lockCode , type: "physical",descriptionText: "[physical] ")
    sendEvent(name:"armingIn", value: 0,data:data, isStateChange:true,descriptionText: data) // The actual armming cmd
}

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
    if (rawValue == null) return
    def descriptionText
    def value
    def minVolts = 20
    def maxVolts = 30
    def pct = (((rawValue - minVolts) / (maxVolts - minVolts)) * 100).toInteger()
    value = Math.min(100, pct)
    descriptionText = "${device.displayName} battery is ${value}%"
    sendEvent(name:"battery", value:value, descriptionText:descriptionText, unit: "%", isStateChange: true)
    logging ("${device} : battery is ${value}% ${rawValue}volts","info")
}

private getMotionResult(value) {
    if (device.currentValue("motion") != "active") {
        runIn(20,motionOff)
        def descriptionText = "${device.displayName} is ${value}"
        sendEvent(name: "motion",value: value,descriptionText: "${descriptionText}")
        logging ("${device} : motion ${value}","info")
        sendPanelResponse()		//Iris V3 needs a response
    }
}

def motionOff(){
    def value = "inactive"
    def descriptionText = "${device.displayName} motion is ${value}"
    sendEvent(name: "motion",value: value,descriptionText: "${descriptionText}")
    logging ("${device} : motion ${value}","info")
    
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


