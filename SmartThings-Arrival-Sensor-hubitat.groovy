/** SmartThings Arrival Sensor driver for hubitat

or  SmartThings Presence Sensor driver for hubitat



Beep Beep for 5 seconds.

Chime for x seconds Play 6 chimes for 6 seconds.

This is my improved smartthings arrival sensor driver for hubitat.
One of the improvements is you can play longer beeps. Another is less cluter in the log
As only changes are recorded. Im also hoping for better reliabilaty and less dropouts.




====================================================================================================
v1.2  08/30/2022 Refresh added
v1.1  08/17/2022 Reworked all the code
v1.0  08/16/2022 Forked and modified or hubitat


======================================================================================================
Copyright [2022] [tmaster winnfreenet.com]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.



forked from here ( with many rewrites)

Which a newer version is here 2016 +
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/arrival-sensor-ha.src/arrival-sensor-ha.groovy
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/arrival-sensor-ha.src/README.md

 *
 */
def clientVersion() {
    TheVersion="1.2"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() // Forces config on updates
 }
}



metadata {
    
    definition (name: "SmartThings Arrival Sensor Chime", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/SmartThings-Arrival-Sensor-hubitat.groovy") {


		capability "Tone"
		capability "Actuator"
		capability "Refresh"
		capability "Presence Sensor"
		capability "Sensor"
		capability "Battery"
        capability "Configuration"
        capability "Chime"
//        capability "Health Check"

        attribute "batteryVoltage", "string"

        command "stopTimer"
        
        

// device Join name ignored in hubitat used for ref
        
        fingerprint profileId: "FC01", deviceId: "019A", manufacturer: "SmartThings", deviceJoinName: "SmartThings Presence Sensor"
		fingerprint profileId: "FC01", deviceId: "0131", manufacturer: "SmartThings", inClusters: "0000,0003", outClusters: "0003", deviceJoinName: "SmartThings Presence Sensor"
		fingerprint profileId: "FC01", deviceId: "0131", manufacturer: "SmartThings", inClusters: "0000",      outClusters: "0006", deviceJoinName: "SmartThings Presence Sensor"
        fingerprint inClusters: "0000,0001,0003,000F,0020", outClusters: "0003,0019", manufacturer: "SmartThings", model: "tagv4", deviceJoinName: "SmartThings Presence Sensor"

	}
	
}
preferences {
	
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
	input("Interval",  "number", title: "Presence Timeout", description: "Time in mins driver sets not present",defaultValue: 15,required: true)

}

def installed(){
logging("${device} : Paired!", "info")
configure()
  
}

def configure() {
    logging("${device} : Configure Driver v${state.version}", "info")
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
    updated() 
}

def updated() {
    clientVersion()
    loggingStatus()
	runIn(3600,debugLogOff)
	runIn(3500,traceLogOff)
}

def stop(){
logging("${device} : Stop Ignored ", "info")
}

def beep() {
    logging("${device} : Beep for 5 seconds ", "info")
    return zigbee.command(0x0003, 0x00, "0500")
}

def playSound(cmd){
    if(cmd <2){return}
    logging("${device} : Chime for ${cmd} seconds ", "info")
    cmd = cmd * 100
    return zigbee.command(0x0003, 0x00, "${cmd}")
    
}    
def refresh() {
    def cmds =
        zigbee.readAttribute(0x001, 0x0020) //Battery percentage
    logging("${device} : refresh ", "info")
    return cmds
}

// [raw:E131010001082000201A, dni:E131, endpoint:01, cluster:0001, size:08, attrId:0020, encoding:20, command:0A, value:1A, clusterInt:1, attrInt:32]

// [raw:catchall: 0104 0003 01 01 0040 00 E131 00 00 0000 0B 01 0000, profileId:0104, clusterId:0003, clusterInt:3, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:E131, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]] 

// [raw:E1310100010A2000201A, dni:E131, endpoint:01, cluster:0001, size:0A, attrId:0020, encoding:20, command:01, value:1A, clusterInt:1, attrInt:32] 
def parse(String description) {
    state.lastCheckin = now()
    def descMap = zigbee.parseDescriptionAsMap(description)
    logging("${device} : Parse :${descMap} ", "trace")
    handlePresenceEvent(true)
    if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0020) { 
        handleBatteryEvent(Integer.parseInt(descMap.value, 16)) 
    }
    if (descMap.command == "01") {
    logging("${device} : Reply to refresh ", "info")
    return
    }
    if (descMap.command == "0B") {
        logging("${device} : Beep Processed ", "info")
        return
    }
    if (descMap.command == "0A") {
        logging("${device} : Ping  Driver V${state.version}", "debug")
        return
     }
    else{
    logging("${device} : Unknown command:${descMap.command} Options:${descMap.options} Value:${descMap.value}", "warn")
    }    
    
    
    

}

// Only log battery if its changed
private handleBatteryEvent(batteryVoltage) {
  
    if (batteryVoltage == 0 || batteryVoltage == 255) {
        return
    }
    else {
        BigDecimal batteryVoltageScaleMin = 1.5
		BigDecimal batteryVoltageScaleMax = 3.0
        batteryVoltage = batteryVoltage/10
        BigDecimal batteryPercentage = 0
        batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
        logging("${device} : Battery ${batteryVoltage}V: ${batteryPercentage}%  Last:${state.lastBattery}v","debug")
        
        if (state.lastBattery != batteryVoltage){
         logging("${device} : Battery ${batteryVoltage}V ${batteryPercentage}%","info")   
    	 sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V",descriptionText:"${value}V ${state.version}", isStateChange: true)
     	 sendEvent(name: "battery", value: batteryPercentage, unit: "%",descriptionText:"${value}% ${state.version}", isStateChange: true)
         state.lastBattery = batteryVoltage
        }  
        
        
        }

}

private handlePresenceEvent(present) {
    logging("${device} : presence event: ${present}","trace")
    if(present){value="present"}
    else{value="not present"}

    def wasPresent = device.currentState("presence")?.value == "present"
    if (!wasPresent && present) {
        logging("${device} : Creating presence event: ${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        startTimer()
    } else if (!present) {
        logging("${device} : Creating presence event: ${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        stopTimer()
    }
}

private startTimer() {
    logging("${device} : Scheduling periodic timer","debug")
    runEvery1Minute("checkPresenceCallback")
}

private stopTimer() {
    logging("${device} : unschedule()","debug")
    unschedule()
}


def checkPresenceCallback() {
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
//  Interval set by driver
    min = timeSinceLastCheckin/60
//  sensor checks in every  0.13 seconds. We have to mimimize log entry.
    logging("${device} : Sensor Check in its ben ${min} mins","debug")
    if (min >= Interval-5){
        logging("${device} : Sensor timing out ${min} min ago","info")
        refresh()// Ping Perhaps we can wake it up...
    }
    if (min >= Interval) { handlePresenceEvent(false)}
}



void loggingStatus() {
	log.info  "${device} : Info  Logging : ${infoLogging == true}"
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
