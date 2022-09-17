/* Everspring/Utilitech Water Sensor Driver for hubitat
   Everspring Flood Sensor       :ST812-2
   Utilitech Water Leak Detector :TST01-1 (Lowe's Iris)


  To reset device press 10 times. remove batteries. reinstall and exclude. 
  Include pressing the button several times slowely or it will reset..
  Wait and Wait.

  Its sometimes hard to get it to pair. Remove batteries let it set and try again later.
  If include fails check zwave log and if the device is in the log you must remove before retying


  This driver atempts to get correct battery results and fix the wake up times
  This code has been rewritten with proper delays to fix some problems with the
  sensor ignoring commands sent to fast in the orginal driver.

====================================================================
v2.2  09/17/2022 Logging and Init code added
v2.1  08/04/2022 Total rewrite of code and cleanup for hubitat.
                 Logging and events added unneeded event calls removed
v2.0  05/08/2021 Init porting to hubitat and removing smartthings routines


https://github.com/tmastersmart/hubitat-code/edit/main/Everspring_Utilitech_Water_Sensor.groovy


 
  Copyright 2022 Winnfreenet.com

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
  in compliance with the License. You may obtain a copy of the License at:

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
  for the specific language governing permissions and limitations under the License.




>> Forked from https://github.com/tosa68/tosaSmartThings/blob/master/devicetypes/tosa68/utilitech-water-sensor.src/utilitech-water-sensor.groovy
Author: tosa68 
Date:   2014-07-17
Version 0.8 (2016-11-02)

>>> Forked from   https://community.hubitat.com/t/utilitech-water-leak-sensor/3696 by cuboy29 

 *  
 */
def clientVersion() {
    TheVersion="2.2"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


metadata {
	definition (name: "Utilitech Water Sensor", namespace: "tmastersmart", author: "tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/Everspring_Utilitech_Water_Sensor.groovy") {
		capability "Water Sensor"
		capability "Battery"
		capability "Configuration"
        capability "Refresh"
        capability "Polling"
        
        
     
        fingerprint deviceId: "0xA102", inClusters: "0x86,0x72,0x85,0x84,0x80,0x70,0x9C,0x20,0x71",  deviceJoinName: "Everspring Flood Sensor"
        fingerprint mfr: "96", inClusters: "0x86,0x72,0x85,0x84,0x80,0x70,0x9C,0x20,0x71",  deviceJoinName: "Utilitech Water Leak Detector"
        
	}
}
preferences {
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false

}

def installed() {
	logging("${device} : Paired!", "info")
    configure()
    runIn(20,refresh)
}

def initialize() {
    logging("${device} : Initialize", "info")
	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.

	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
    logging("${device} : Initialised Refreash in ${randomSixty}sec", "info")

}
def configure() {
        // manufacturer default wake up is every hour. Some of mine never wake up until an event.
    logging("${device} : configure", "info")
    state.WakeUpInterval = 3600 // 1 hr
    state.remove("seconds") 
    state.remove("defaultWakeUpInterval")
    state.remove("wakeUpInterval")
    
    delayBetween([
        zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format(),
        zwave.wakeUpV2.wakeUpIntervalSet(seconds:state.WakeUpInterval, nodeid:zwaveHubNodeId).format(),
        zwave.wakeUpV2.wakeUpIntervalGet().format(),
        zwave.batteryV1.batteryGet().format()
    ], 2300)
}

def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingStatus()
	runIn(3600,debugLogOff)
	runIn(1800,traceLogOff)
    randomSixty = Math.abs(new Random().nextInt() % 60)
	runIn(randomSixty,refresh) // Refresh in random time
}


def parse(String description) {
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MM/dd/YYYY", location.timeZone)}"
    state.LastCheckin = Timecheck
    logging("${device} : Raw [${description}]", "trace")
    CommandClassCapabilities = [0x9C: 1, 0x71: 1, 0x84: 2, 0x30: 1, 0x70: 1]   
    hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
    if (map == null) {return null}
    def result = [map]
    if (!result) {return null}
    logging("${device} : Parse ${result}", "debug")
        if (map) { 
        zwaveEvent(map)
        return
    }

}

//Received Wakeup cmd:WakeUpNotification() Int:3600
def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpNotification cmd) {
    logging("${device} : Device Woke Up. Waiting to poll", "info")
    runIn(12,refresh)// polling wont work unless delayed
}

def zwaveEvent(hubitat.zwave.commands.sensoralarmv1.SensorAlarmReport cmd) {
	def map = [:]
	if (cmd.sensorType == 0x05) {
	 map.value = cmd.sensorState ? "wet" : "dry"
     logging("${device} : Water: ${map.value} alarm1", "info")
     sendEvent(name: "water", value: map.value, descriptionText: "${map.value} ${state.version}", isStateChange:true)
     runIn(10,refresh)// polling wont work unless delayed
     runIn(12,configure)// Be sure our wake up is stored.
    }    
}

def zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
	def map = [:]
	map.value = cmd.sensorValue ? "wet" : "dry"
    logging("${device} : Water: ${map.value} binary1", "info")
    sendEvent(name: "water", value: map.value, descriptionText: "${map.value} ${state.version}", isStateChange:true)
    runIn(20,refresh)// polling wont work unless delayed
}

def zwaveEvent(hubitat.zwave.commands.alarmv1.AlarmReport cmd) {
    logging("${device} : received alarmv1 ${cmd}", "debug")
    def map = [:]
    def result
	if (cmd.alarmType == 1) {
        if (cmd.alarmLevel == 0xFF || cmd.alarmLevel == 1) {
            logging("${device} : Alarm: Low Battery ${cmd.alarmLevel}", "warn")
        } 
    } else if (cmd.alarmType == 2 && cmd.alarmLevel == 1) {
        map.descriptionText = "${device.displayName} powered up"
		logging("${device} : Powered Up ${cmd.alarmLevel}", "warn")
    } 
    
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
      logging("${device} : received batteryv1 ${cmd}", "debug")
    if (cmd.batteryLevel == 0xFF) { 
        logging("${device} : Low battery", "debug")
     } 
    else {
        logging("${device} : battery: ${cmd.batteryLevel} ", "info")
        sendEvent(name: "battery", value: cmd.batteryLevel,unit: "%", descriptionText: "${cmd.batteryLevel} ${state.LastCheckin} v${state.version}", isStateChange:true)
	}
}

def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalCapabilitiesReport cmd) {
    logging("${device} : WakeUpIntervalCapabilitiesReport: ${cmd} ", "trace")
    logging("${device} : Wake Up Seconds1: ${cmd.defaultWakeUpIntervalSeconds} ", "info")
	state.WakeUpInterval = cmd.defaultWakeUpIntervalSeconds
}
// I receive this on not the one above
def zwaveEvent(hubitat.zwave.commands.wakeupv2.WakeUpIntervalReport cmd) {
    logging("${device} : WakeUpIntervalCapabilitiesReport: ${cmd} ", "trace")
    logging("${device} : Wake Up Seconds: ${cmd.seconds} ", "info")
    state.WakeUpInterval = cmd.seconds
}

// these are untrapped log them...
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("${device} : Received basic1 ${cmd}", "debug")
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    def map = [:]
	logging("${device} : COMMAND CLASS: ${cmd} ", "trace")
    logging("${device} : ${map} ", "debug")

//    map.name = "ManufacturerSpecificReport"
//    map.mfr   = hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)
//    map.model = hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)
//    map.type  = hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)
//    logging("${device} : E11 fingerprint mfr:${map.mfr} prod:${map.type} model:${map.model}", "debug")
}

private getBattery() {	
        logging("${device} : Requesting Battery", "debug")
		zwave.batteryV1.batteryGet().format()
  
}
def pollDevice() {
    poll()
}
def refresh() {
    poll()
}
def poll() {
    clientVersion()
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MM/dd/YYYY", location.timeZone)}"
    logging("${device} : Poll  ${Timecheck} v${state.version}", "info") 
// When its sleeping you basicaly cant wake it up.
	delayBetween([
        zwave.wakeUpV2.wakeUpIntervalGet().format(),
		zwave.sensorMultilevelV3.sensorMultilevelGet().format(),// current temperature
		zwave.batteryV1.batteryGet().format(),
	], 2300)
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
