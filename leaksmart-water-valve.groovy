/**
 * Hubitat LeakSmart/Iris Water Valve 
   ported to hubitat by tmastersmart 
   Correct a falure to detect battery 

LeakSmart Valve FCC ID: W7Z-ZICM357SP2
https://leaksmart.com/storage/2020/01/Protect-by-LeakSmart-Manual.pdf
tested on firmware:113B-03E8-0000001D 

   https://github.com/tmastersmart/hubitat-code/blob/main/leaksmart-water-valve.groovy
   https://github.com/tmastersmart/hubitat-code/raw/main/leaksmart-water-valve.groovy

  Changelog:

    2.2.1 08/08/2021   Changed logging on battery
    2.1 05/03/2012   Fixed log reports
    2.0 04/12/2021   Ported to Hubitat


To reset the valve controller, rapidly press the center button 5 times. The Blue LED light will begin to flash
indicating it is reset and ready to join the system.
Note:
The device may require a power cycled before a reset. Removing AC adapter and bat. Then power back up.


 *  forked from https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/krlaframboise/leaksmart-water-valve.src
 
 *  Author:Kevin LaFramboise (krlaframboise)(from 1.3 orginal)    (Mode: 8830000L)
 *    1.3 (10/23/2017) - Added support for valve attribute events.
 *    1.2.1 (08/12/2017) - Create switch events when the open/close state changes.
 *    1.2.1 (08/12/2017) - Create switch events when the open/close state changes.
 *    1.2 (08/20/2016)   - Changed lower battery limit to 5.0
 *    1.1.3 (05/23/2016) - Changed lower battery limit to 5.5
 *    1.1.2 (05/22/2016)
 *      - Added battery capability and tile
 *      - Added debug logging for battery map.
 *      - Changed poll method  read instead of configure.
 *      - Changed minimum batterr reporting interval to 10 minutes
 *        to avoid duplicates.  
 *      - Changed upper battery voltage to 6.0.
 *    1.0.3 (05/22/2016) - Initial Release Bug fixes
 *
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
		capability "Valve"
		capability "Polling"
        
		attribute "lastPoll", "number"	
		attribute "batteryVoltage", "string"
        
        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02, FC02", outClusters: "0019", manufacturer: "WAXMAN", model: "leakSMART Water Valve v2.10", deviceJoinName: "leakSMART Valve" //leakSMART Valve
        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0008, 000F, 0020, 0B02", outClusters: "0003, 0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Waxman Valve" //Waxman House Water Valve
        fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0020, 0B02, FC02", outClusters: "0003,0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Leaksmart Water Valve"
		fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02", outClusters: "0019"
	}
// need fingerprints for other valves

	preferences {
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: false, 
			required: false
	}
	

}

def updated() {
    
    if (!state.configured) {	return response(configure())}
}

def parse(String description) {
	def result = []
	def evt = zigbee.getEvent(description)
	if (evt) {
        logDebug "${device} :Received Event: ${evt.name} ${evt}"
		if (evt.name == "switch") {
			def val = (evt.value == "on") ? "open" : "closed"
//			logDebug "${device}: Valve:$val Contact:$val Switch:$evt.value"
			result << createEvent(name: "contact", value: val)
			result << createEvent(name: "valve", value: val)
            log.info "${device}: Valve ${val}"
			result << createEvent(name: "switch", value: evt.value, displayed:false)
			result << createEvent(name: "lastPoll",value: new Date().time, isStateChange: true, displayed: false)
		}
// 1.35 volts is a dead battery 1.35*4= 5.4
// 1.5 * 4 = 6 but 6.1 is the normal reading for a new battery
//        
        if (evt.name == "batteryVoltage") {
            def battest = evt.value 
//	        def maxVolts = 6.1
//	        def minVolts = 3.5 
	        def volts = (battest)
//	        def batteryPercentages = (volts - minVolts ) / (maxVolts - minVolts)	
//            def batteryLevel = (int) batteryPercentages * 100
//            if (batteryLevel > 100) {batteryLevel​ = 100}
// this routine was causing a flakey result using manual            
            
            batteryLevel = 100
            if (volts < 6)   {batteryLevel = 90}
            if (volts < 5.9) {batteryLevel = 80}
            if (volts < 5.85) {batteryLevel = 70}
            if (volts < 5.8) {batteryLevel = 60}
            if (volts < 5.65) {batteryLevel = 50}           
            if (volts < 5.6) {batteryLevel = 40}
            if (volts < 5.55) {batteryLevel = 30}
            if (volts < 5.5) {batteryLevel = 20}
            if (volts < 5.4) {batteryLevel = 10}
            if (volts <= 5.3){batteryLevel = 0}
  
            
            if (batteryLevel < 90) { 
               log.warn "${device}: Unplugged battery discharging ${volts} v"
               } 
            
//          logDebug "${device}: Battery ${batteryLevel}% $volts v"
            log.info "${device}: Battery ${batteryLevel}% ${volts} v"
//          result << createEvent(name: "batteryVoltage", value: volts, unit:"V")
            result << createEvent(name: "battery", value: batteryLevel, unit:"%")
        }
        
     
        

		result << createEvent(evt)
	}
//	else {
//		def map = zigbee.parseDescriptionAsMap(description)
//		if (map) {
//			if (map.clusterInt == 1) {
//                def battest = zigbee.convertHexToInt(map.value)
//				def batteryLevel = getBatteryLevel(battest)
//				logDebug "Battery Level is ${batteryLevel}%\nMap: $map"
//				result << createEvent(name: "battery", value: batteryLevel, unit:"%")
//			}
//		  	else {logDebug "Ignored Map: $map"}    
//		}
//		else {logDebug "Ignored Description: $description"}
//	}	
    
	return result
}



def on() {
	open()
}

def off() {
	close()
}

def open() {
	logDebug "${device} Opening"
	zigbee.on()
}

def close() {
	logDebug "${device} Closing"
	zigbee.off()
}

def poll() {
	def minimumPollMinutes = (3 * 60) // 3 Hours
	def lastPoll = device.currentValue("lastPoll")
	if ((new Date().time - lastPoll) > (minimumPollMinutes * 60 * 1000)) {
		logDebug "${device}: Poll: LastPoll was more than ${minimumPollMinutes} minutes ago."
		return refresh()
	}
	else {
		logDebug "${device}: Poll: Skipped to soon within ${minimumPollMinutes} minutes"
	}
}

def refresh() {
	logDebug "${device}: Refreshing"	
	return zigbee.onOffRefresh() + 
		getBatteryReport() +
		zigbee.onOffConfig() + 
		configureBatteryReporting()
}

def configure() {
    
	logDebug "${device}: Configuring Reporting and Bindings."
	state.configured = true
	return zigbee.onOffConfig() + 
		configureBatteryReporting() +
		zigbee.onOffRefresh() +
		getBatteryReport()
}

private configureBatteryReporting() {
	def minSeconds = (30 * 60) // 30 Minutes
	def maxSeconds = (4 * 60 * 60) // 4 Hours	
	zigbee.configureReporting(0x0001, 0x0020, 0x20, minSeconds, maxSeconds, 0x01)
    logDebug "${device}: Set bat reports to 30 min min 4hr max"
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
