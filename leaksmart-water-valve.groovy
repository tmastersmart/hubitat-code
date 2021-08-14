/**
   Hubitat LeakSmart Water Valve driver
   Hubitat Iris Water Valve driver
   with mains detection

   leaksmart driver hubitat 

LeakSmart Valve FCC ID: W7Z-ZICM357SP2
https://leaksmart.com/storage/2020/01/Protect-by-LeakSmart-Manual.pdf


   https://github.com/tmastersmart/hubitat-code/blob/main/leaksmart-water-valve.groovy
   https://github.com/tmastersmart/hubitat-code/raw/main/leaksmart-water-valve.groovy

  Changelog:
    2.4.1 08/13/2021   force battery report / cleanup   
    2.3   08/10/2021   New mains and battery detection added. Old battery detection is now EST
    2.2.2 08/10/2021  
    2.2.1 08/08/2021 Changed logging on battery routines
    2.1 05/03/2012   
    2.0 04/12/2021   Ported to Hubitat


To reset the valve controller, rapidly press the center button 5 times. The Blue LED light will begin to flash
indicating it is reset and ready to join the system.
Note:
The device may require a power cycled before a reset. Removing AC adapter and bat. Then power back up.



Aditional code merged into orginal from fork at

 *  Copyright 2016 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.

 * https://raw.githubusercontent.com/mleibman/SmartThingsPublic/a5bc475cc1b2edc77e4649609db5833421ad7f48/devicetypes/smartthings/zigbee-valve.src/zigbee-valve.groovy



 *  orginal forked from https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/krlaframboise/leaksmart-water-valve.src
 
 *  Author:Kevin LaFramboise (krlaframboise)(from 1.3 orginal)    (Mode: 8830000L)
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
	definition (name: "LeakSmart Water Valve", namespace: "tmastersmart", author: "Tmaster", importUrl:"https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/leaksmart-water-valve.groovy" ) {
		capability "Actuator"
		capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
        capability "Contact Sensor"
		capability "Valve"
		capability "Polling"
        capability "Power Source"
        
		attribute "lastPoll", "number"
        attribute "lastPollD", "number"
        attribute "batteryEST", "number"
		attribute "batteryVoltage", "string"

        
        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02, FC02", outClusters: "0019", manufacturer: "WAXMAN", model: "leakSMART Water Valve v2.10", deviceJoinName: "leakSMART Valve v2.10" //leakSMART Valve
        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0008, 000F, 0020, 0B02", outClusters: "0003, 0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Waxman Valve" //Waxman House Water Valve
        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0020, 0B02, FC02", outClusters: "0003,0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Leaksmart Water Valve - MDL-TBD"
		fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02", outClusters: "0019"
	}
// need fingerprints for other valves
// fingerprint model:"House Water Valve - MDL-TBD", manufacturer:"WAXMAN", profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0004,0005,0006,0020,0B02,FC02", outClusters:"0003,0019", application:"1D"

	preferences {
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: false, 
			required: false
	}
	

}
private getCLUSTER_BASIC() { 0x0000 }
private getCLUSTER_POWER() { 0x0001 }
private getBASIC_ATTR_POWER_SOURCE() { 0x0007 }
private getPOWER_ATTR_BATTERY_PERCENTAGE_REMAINING() { 0x0021 }
private getTYPE_U8() { 0x20 }
private getTYPE_ENUM8() { 0x30 }


def updated() {
    
    if (!state.configured) {	return response(configure())}
}

def parse(String description) {
    // log what we received . Looking for events the hub doesnt know about
//        
//      logDebug("${device} : Parse : ${description}") 
//
//  This looks at all events and decodes them CATCHALL:       
    Map descriptionMap = zigbee.parseDescriptionAsMap(description)
     if (descriptionMap) {
      String[] receivedData = descriptionMap.data
         if ( descriptionMap.profileId){ 
//             logDebug("${device} : ${descriptionMap}")
             logDebug("${device} : clusterId: ${descriptionMap.clusterId} profileId: ${descriptionMap.profileId} command: ${descriptionMap.command} data: ${descriptionMap.data}")  
         }
        }

         
         
//    }
// org routine this lets the hub decode known standard events  
	def result = []
	def evt = zigbee.getEvent(description) // test for known events by the hub drivers
    if (evt) {
        logDebug "${device} :Received Event: ${evt.name} ${evt}"
        result << createEvent(name: "lastPoll",value: new Date().time, isStateChange: true, displayed: false)
     	result << createEvent(name: "lastPollD", value: new Date().format("MMM dd yyyy hh:mm", location.timeZone))
// valve status        
        if (evt.name == "switch") {
			def val2 = (evt.value == "on") ? "open" : "closed"
			result << createEvent(name: "contact", value: val2)
			result << createEvent(name: "valve", value: val2)
			result << createEvent(name: "switch", value: evt.value, displayed:false)
            log.info "${device}: Valve ${val2}"
		//	result << createEvent(name: "lastPoll",value: new Date().time, isStateChange: true, displayed: false)
		}
        if (evt.name == "battery") {
            def val3 = evt.value
            result << createEvent(name: "battery", value: val3,isStateChange: true)
            log.info "${device}: battery ${val3}"
        }
        
//  voltage status      
        if (evt.name == "batteryVoltage") {
//            result << createEvent(name: "batteryVoltage", value: ${evt.value}, unit:"V")
            def battest = evt.value 
//	        def maxVolts = 6.1
//	        def minVolts = 3.5 
	        def volts = (battest)
//	        def batteryPercentages = (volts - minVolts ) / (maxVolts - minVolts)	
//            def batteryLevel = (int) batteryPercentages * 100
//            if (batteryLevel > 100) {batteryLevel​ = 100}
// this routine was causing a flakey result using manual            
            
            def batteryLevel = 100
            if (volts < 6)   {batteryLevel = 90}
            if (volts < 5.9) {batteryLevel = 80}
            if (volts < 5.85) {batteryLevel = 70}
            if (volts < 5.8) {batteryLevel = 60}
            if (volts < 5.65) {batteryLevel = 50}           
            if (volts < 5.6) {batteryLevel = 40}
            if (volts < 5.55) {batteryLevel = 30}
            if (volts < 5.5) {batteryLevel = 10}
            if (volts <= 5.4) {batteryLevel = 0}
 
            if (batteryLevel < 80 ){
            result << createEvent(name: "powerSource", value: battery)
            log.info "${device}: powerSource: battery"
            }
            log.info "${device}: Battery ${volts} v EST${batteryLevel}%"
            result << createEvent(name: "batteryEST", value: batteryLevel, unit:"%")
            result << createEvent(name: "batteryVoltage", value: volts, unit:"V")
        }

        // This is the mains (not working on my valve?)
        if (evt.name == "powerSource"){
            // results should be (Mains,Battery,DC,Unknown)
            def val4 = evt.value
            result << createEvent(name: "powerSource", value: val4)
                 log.info "${device}: powerSource: ${val4}"
       }
        
        
	//	result << createEvent(evt)    left over this I think was to create events for unknown items
 }
	return result
}



def on()   {	open()  } 
def off()  {	close() }
def open() {
	log.info "${device} Opening the valve"
	zigbee.on()
}
def close() {
	log.info "${device} Closing the valve"
	zigbee.off()
}


def poll() {
    
	def minimumPollMinutes = (3 * 60) // 3 Hours
	def lastPoll = device.currentValue("lastPoll")
	if ((new Date().time - lastPoll) > (minimumPollMinutes * 60 * 1000)) {
		logDebug "${device}: Polling: Sending refresh cmd"
        
		return refresh()
	}
	else {
//        logDebug "${device}: Skipping Poll: must be > ${minimumPollMinutes} minutes"
        log.info "${device}: Skipping Poll: must be > ${minimumPollMinutes} minutes"
	}
}


def refresh() {
//    logDebug "${device}: Refreshing"
    log.info "${device}: Refreshing"
    return zigbee.onOffRefresh() +
    getBatteryReport() +
    zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE) +
    zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING) +
    zigbee.onOffConfig() +
    zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING, TYPE_U8, 600, 21600, 1) +
    zigbee.configureReporting(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE, TYPE_ENUM8, 5, 21600, 1)+
    configureBatteryReporting()    
}

def configure() {

    logDebug "${device}: Configuring"
	state.configured = true
    

return   zigbee.onOffConfig() +configureBatteryReporting() +
    zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING, TYPE_U8, 600, 21600, 1) +
    zigbee.configureReporting(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE, TYPE_ENUM8, 5, 21600, 1) +
    zigbee.onOffRefresh() +
    zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE) +
    zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING) +
    getBatteryReport() 
}

// see this driver the above from this driver doesnt work
//https://raw.githubusercontent.com/mleibman/SmartThingsPublic/a5bc475cc1b2edc77e4649609db5833421ad7f48/devicetypes/smartthings/zigbee-valve.src/zigbee-valve.groovy


private configureBatteryReporting() {
	def minSeconds = (30 * 60) // 30 Minutes
	def maxSeconds = (5* 60 * 60) // 5 Hours	
	zigbee.configureReporting(0x0001, 0x0020, 0x20, minSeconds, maxSeconds, 0x01)
    logDebug "${device}: Set bat reports to 30 min min 5hr max"
}

private getSwitchReport() {
	return readAttribute(0x0006, 0x0000)
}

private getBatteryReport() {
	zigbee.readAttribute(0x0001, 0x0020)
}

private logDebug(msg) {
	if (settings.debugOutput != false) {
		log.debug "$msg"
	}
}

/**
Hubitat LeakSmart Water Valve driver
Hubitat Iris Water Valve driver
mains detection
leaksmart driver hubitat
iris water valve driver
*/
