
/**
First Alert Smoke and CO alarm driver for hubitat

THIS IS A ALPHA TEST DO NOT USE IN PRODUCTION...........
MANY problems wit this need detection for bad unit


08/27/2021 v2.0  Bad unit detection added. smartthings code removed. Proper copyright added.


Split into 2 forks one for smoke only one for smoke CO
https://github.com/tmastersmart/hubitat-code/blob/main/First_Alert_Smoke_CO_Detector.groovy
https://github.com/tmastersmart/hubitat-code/raw/main/First_Alert_Smoke_CO_Detector.groovy





Fingerprints needed

 To pair hold down button while inserting batteries. Green light will flash and long flash when paired. Repeat to exclude
 
*
* v1 Code changes from author: "keltymd"
https://community.hubitat.com/t/first-alert-zcombo-generation-2/53339


Forked from orginal  author: "SmartThings"
https://raw.githubusercontent.com/SmartThingsCommunity/SmartThingsPublic/master/devicetypes/smartthings/zwave-smoke-alarm.src/zwave-smoke-alarm.groovy 

 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "First Alert Smoke CO Detector", namespace: "tmastersmart", author: "Tmaster", importUrl:"https://github.com/tmastersmart/hubitat-code/raw/main/First_Alert_Smoke_CO_Detector.groovy") {
		capability "Smoke Detector"
		capability "Carbon Monoxide Detector"
		capability "Sensor"
		capability "Battery"
		capability "Health Check"

		attribute "alarmState", "string"
        fingerprint mfr:"0138", prod:"0001", model:"0002", deviceJoinName: "First Alert Smoke Detector and CO Alarm (ZCOMBO)" 
        fingerprint mfr:"0138", prod:"0001", model:"0003", deviceJoinName: "First Alert Smoke Detector and CO Alarm (ZCOMBO)" 
        fingerprint mfr:"0312", prod:"0001", model:"0002", deviceJoinName: "First Alert Smoke Detector and CO Alarm IRIS (ZCOMBO)"


	}
// lowels smoke co model
//inClusters: 0x20,0x80,0x70,0x85,0x71,0x72,0x86
//MSR: 0138-0001-0002
//manufacturer: 312 IRIS lowels  
//deviceType: 1
//deviceId: 2    
    
//inClusters: 0x20,0x80,0x70,0x85,0x71,0x72,0x86
	
// lowels smoke only model	
//deviceType: 1
//inClusters: 0x20,0x80,0x70,0x85,0x71,0x72,0x86
//deviceId: 1
//MSR: 0138-0001-0001
//manufacturer: 312	
	
	
}

def installed() {
// Device checks in every hour, this interval allows us to miss one check-in notification before marking offline
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])

	def cmds = []
	createSmokeOrCOEvents("allClear", cmds) // allClear to set inital states for smoke and CO
	cmds.each { cmd -> sendEvent(cmd) }
}

def updated() {
// Device checks in every hour, this interval allows us to miss one check-in notification before marking offline
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def parse(String description) {
	def results = []
	if (description.startsWith("Err")) {
	    results << createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
		if (cmd) {
			zwaveEvent(cmd, results)
		}
	}
	log.debug "'$description' parsed to ${results.inspect()}"
	return results
}

def createSmokeOrCOEvents(name, results) {
	def text = null
	switch (name) {
		case "smoke":
			text = "$device.displayName smoke was detected!"
			// these are displayed:false because the composite event is the one we want to see in the app
		        // this is wrong for hubitat changed to true
			results << createEvent(name: "smoke",          value: "detected", descriptionText: text, displayed: true)
			break
		case "carbonMonoxide":
			text = "$device.displayName carbon monoxide was detected!"
			results << createEvent(name: "carbonMonoxide", value: "detected", descriptionText: text, displayed: true)
			break
		case "tested":
			text = "$device.displayName was tested"
			results << createEvent(name: "smoke",          value: "tested", descriptionText: text, displayed: true)
			results << createEvent(name: "carbonMonoxide", value: "tested", descriptionText: text, displayed: true)
			break
		case "smokeClear":
			text = "$device.displayName smoke is clear"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text, displayed: true)
			name = "clear"
			break
		case "carbonMonoxideClear":
			text = "$device.displayName carbon monoxide is clear"
			results << createEvent(name: "carbonMonoxide", value: "clear", descriptionText: text, displayed: true)
			name = "clear"
			break
		case "allClear":
			text = "$device.displayName all clear"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text, displayed: true)
			results << createEvent(name: "carbonMonoxide", value: "clear", displayed: true)
			name = "clear"
			break
		case "testClear":
			text = "$device.displayName test cleared"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text, displayed: true)
			results << createEvent(name: "carbonMonoxide", value: "clear", displayed: true)
			name = "clear"
			break
	}
	// This composite event is used for updating the tile --- no tiles on hubitat
	results << createEvent(name: "alarmState", value: name, descriptionText: text)
}

//  when alarm beeps 5 times. alarm is bad and sends this  alarmLevel:255, alarmType:9 
//  when alarm beeps 4 times   ?? still dont know command sent
def zwaveEvent(hubitat.zwave.commands.alarmv2.AlarmReport cmd, results) {
	if (cmd.zwaveAlarmType == hubitat.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_SMOKE) {
		if (cmd.zwaveAlarmEvent == 3) {
			createSmokeOrCOEvents("tested", results)
		} else {
			createSmokeOrCOEvents((cmd.zwaveAlarmEvent == 1 || cmd.zwaveAlarmEvent == 2) ? "smoke" : "smokeClear", results)
		}
	} else if (cmd.zwaveAlarmType == hubitat.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_CO) {
		createSmokeOrCOEvents((cmd.zwaveAlarmEvent == 1 || cmd.zwaveAlarmEvent == 2) ? "carbonMonoxide" : "carbonMonoxideClear", results)
	} else switch(cmd.alarmType) {
		case 1:
			createSmokeOrCOEvents(cmd.alarmLevel ? "smoke" : "smokeClear", results)
			break
		case 2:
			createSmokeOrCOEvents(cmd.alarmLevel ? "carbonMonoxide" : "carbonMonoxideClear", results)
			break
		case 9:
            createSmokeOrCOEvents(cmd.alarmLevel ? "tested" : "Failed", results)
            results << createEvent(descriptionText: "$device.displayName Reports unit failed", isStateChange: true)
			break
        
        case 12:  // test button pressed
			createSmokeOrCOEvents(cmd.alarmLevel ? "tested" : "testClear", results)
			break
		

		
		case 13:  // sent every hour -- not sure what this means, just a wake up notification?
			if (cmd.alarmLevel == 255) {
				results << createEvent(descriptionText: "$device.displayName checked in", isStateChange: false)
			} else {
				results << createEvent(descriptionText: "$device.displayName code 13 is $cmd.alarmLevel", isStateChange:true, displayed:false)
			}
			
			// Clear smoke in case they pulled batteries and we missed the clear msg
			if(device.currentValue("smoke") != "clear") {
				createSmokeOrCOEvents("smokeClear", results)
			}
			
			// Check battery if we don't have a recent battery event
			if (!state.lastbatt || (now() - state.lastbatt) >= 48*60*60*1000) {
				results << response(zwave.batteryV1.batteryGet())
			}
			break
             

		
		default:
			
			results << createEvent(displayed: true, descriptionText: "Alarm $cmd.alarmType ${cmd.alarmLevel == 255 ? 'activated' : cmd.alarmLevel ?: 'deactivated'}".toString())
			break

 	if (cmd.alarmType == 14) {
	    createSmokeOrCOEvents(cmd.alarmLevel ? "tested" : "Failed", results)
            results << createEvent(descriptionText: "$device.displayName Reports unit failed", isStateChange: true)
	}
	}
}

// SensorBinary and SensorAlarm aren't tested, but included to preemptively support future smoke alarms
//
def zwaveEvent(hubitat.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, results) {
	if (cmd.sensorType == hubitat.zwave.commandclasses.SensorBinaryV2.SENSOR_TYPE_SMOKE) {
		createSmokeOrCOEvents(cmd.sensorValue ? "smoke" : "smokeClear", results)
	} else if (cmd.sensorType == hubitat.zwave.commandclasses.SensorBinaryV2.SENSOR_TYPE_CO) {
		createSmokeOrCOEvents(cmd.sensorValue ? "carbonMonoxide" : "carbonMonoxideClear", results)
	}
}

def zwaveEvent(hubitat.zwave.commands.sensoralarmv1.SensorAlarmReport cmd, results) {
	if (cmd.sensorType == 1) {
		createSmokeOrCOEvents(cmd.sensorState ? "smoke" : "smokeClear", results)
	} else if (cmd.sensorType == 2) {
		createSmokeOrCOEvents(cmd.sensorState ? "carbonMonoxide" : "carbonMonoxideClear", results)
	}
	
}

def zwaveEvent(hubitat.zwave.commands.wakeupv1.WakeUpNotification cmd, results) {
	results << createEvent(descriptionText: "$device.displayName woke up", isStateChange: false)
	if (!state.lastbatt || (now() - state.lastbatt) >= 56*60*60*1000) {
		results << response([
				zwave.batteryV1.batteryGet().format(),
				"delay 2000",
				zwave.wakeUpV1.wakeUpNoMoreInformation().format()
			])
	} else {
		results << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
	}
}

def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd, results) {
	def map = [ name: "battery", unit: "%", isStateChange: true ]
	state.lastbatt = now()
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName battery is low!"
	} else {
		map.value = cmd.batteryLevel
	}
	results << createEvent(map)
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, results) {
	def encapsulatedCommand = cmd.encapsulatedCommand([ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
	state.sec = 1
	log.debug "encapsulated: ${encapsulatedCommand}"
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand, results)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		results << createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(hubitat.zwave.Command cmd, results) {
	def event = [ displayed: false ]
	event.linkText = device.label ?: device.name
	event.descriptionText = "$event.linkText: $cmd"
	results << createEvent(event)
}
