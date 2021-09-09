/**
   Hubitat LeakSmart Water Valve driver
   Hubitat Iris Water Valve driver
   with mains detection

   leaksmart driver hubitat 

LeakSmart Valve FCC ID: W7Z-ZICM357SP2

tested on firmware 
113B-03E8-0000001D Tested
113B-03E8-00000019  


https://leaksmart.com/storage/2020/01/Protect-by-LeakSmart-Manual.pdf


web   >   https://github.com/tmastersmart/hubitat-code/blob/main/leaksmart-water-valve.groovy
import>   https://github.com/tmastersmart/hubitat-code/raw/main/leaksmart-water-valve.groovy

  Changelog:
    2.5.2 09/09/2021   Mains detection now estimated from last batt reading            
    2.5.0 08/14/2021   update 
    2.4.1 08/13/2021   force battery report / cleanup   
    2.3   08/10/2021   New mains and battery detection added. Old battery detection is now EST
    2.2.2 08/10/2021  
    2.2.1 08/08/2021 Changed logging on battery routines
    2.1   05/03/2021   
    2.0   04/12/2021   Ported to Hubitat


To reset the valve controller, rapidly press the center button 5 times. The Blue LED light will begin to flash
indicating it is reset and ready to join the system.
Note:
The device may require a power cycled before a reset. Removing AC adapter and bat. Then power back up.


Pust comments here
http://www.winnfreenet.com/wp/2021/09/leaksmart-water-valve-driver-for-hubitat/



 *  orginal forked from https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/krlaframboise/leaksmart-water-valve.src
 *  Author:Kevin LaFramboise (krlaframboise)(from 1.3 orginal)    (Mode: 8830000L)
 *
 *  Aditional code from fork at
 *  https://raw.githubusercontent.com/mleibman/SmartThingsPublic/a5bc475cc1b2edc77e4649609db5833421ad7f48/devicetypes/smartthings/zigbee-valve.src/zigbee-valve.groovy
 *

 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
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
        

        attribute "lastPollD", "number"
        attribute "batteryEST", "number"
	attribute "batteryVoltage", "string"

        
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02, FC02", outClusters: "0019", manufacturer: "WAXMAN", model: "leakSMART Water Valve v2.10", deviceJoinName: "leakSMART Valve v2.10" //leakSMART Valve
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0008, 000F, 0020, 0B02", outClusters: "0003, 0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Waxman Valve" //Waxman House Water Valve
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0020, 0B02, FC02", outClusters: "0003,0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Leaksmart Water Valve"// Lowels version
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02", outClusters: "0019"
	}
// need fingerprints  (TBD = to be determined)
// fingerprint model:"House Water Valve - MDL-TBD", manufacturer:"WAXMAN", profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0004,0005,0006,0020,0B02,FC02", outClusters:"0003,0019", application:"1D"

//manufacturer :WAXMAN Model: leakSMART Water Valve v2.10  Firmware: 113B-03E8-00000019 softwareBuild: 00000019
//manufacturer :WAXMAN Model: House Water Valve - MDL-TBD  Firmware: 113B-03E8-0000001D softwareBuild: 0000001D
	
	
    
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
//        result << createEvent(name: "lastPoll",value: new Date().time, isStateChange: true, displayed: false)
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
            log.info "${device}: battery ${val3}%"
        }
        
//  voltage status      
        if (evt.name == "batteryVoltage") {
//            result << createEvent(name: "batteryVoltage", value: ${evt.value}, unit:"V")

		def batteryVoltage = evt.value

                BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 3.50
		BigDecimal batteryVoltageScaleMax = 6
		
		batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
		
		
            // We will get 2 bat % readings one calc one reported not using calc 
		log.info "${device}: Battery ${batteryVoltage} v ${batteryPercentage}% "
            result << createEvent(name: "batteryEST", value: batteryPercentage, unit:"%")
            result << createEvent(name: "batteryVoltage", value: batteryVoltage, unit:"V")
		
            // watch for battery discharging to detect mains off
	    // Mains,Battery,DC,Unknown

		def testVoltage = (state.lastBatteryVoltage - 0.2)
            if (batteryVoltage < testVoltage){
                if (state.supplyPresent){
                    log.info "${device} : discharging detected Last:${state.lastBatteryVoltage}v > Current:${batteryVoltage}v" 
                    state.supplyPresent = false
		    result << createEvent(name: "PowerSource", value: "battery", isStateChange: true)
                  }
            }
            // this valve does not charge if it goes up it should be dc
            if (batteryVoltage > testVoltage){
                if(!state.supplyPresent){
                    log.info "${device} : Mains detected Last:${state.lastBatteryVoltage}v < Current:${batteryVoltage}v" 
                    state.supplyPresent = true
		    result << createEvent(name: "PowerSource", value: "mains", isStateChange: true)
                }
            }
            state.lastBatteryVoltage = batteryVoltage
	   // end new mains detect		
        }

        // This is the mains detection - this doesnt work so using above estimate
        if (evt.name == "powerSource"){
            // results should be (Mains,Battery,DC,Unknown)
            // name:powerSource, value:mains	
            def val4 = evt.value
	    state.supplyPresent = true
            result << createEvent(name: "powerSource", value: val4)
                 log.info "${device}: Received proper flag powerSource: ${val4}"
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
    
//	def minimumPollMinutes = (3 * 60) // 3 Hours
//	def lastPoll = device.currentValue("lastPoll")
//	if ((new Date().time - lastPoll) > (minimumPollMinutes * 60 * 1000)) {
//		logDebug "${device}: Polling: Sending refresh cmd"
//        
//		return refresh()
//	}
//	else {
//        logDebug "${device}: Skipping Poll: must be > ${minimumPollMinutes} minutes"
//        log.info "${device}: Skipping Poll: must be > ${minimumPollMinutes} minutes"
//	}
// last pool time format is wrong not working
    
    return refresh()
}



def refresh() {
    //    logDebug "${device}: Refreshing"
    log.info "${device}: manufacturer :${device.data.manufacturer} Model: ${device.data.model}  Firmware: ${device.data.firmwareMT} softwareBuild: ${device.data.softwareBuild}"
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
    log.info "${device}: manufacturer :${device.data.manufacturer} Model: ${device.data.model}  Firmware: ${device.data.firmwareMT} softwareBuild: ${device.data.softwareBuild}"
    logDebug "${device}: Configuring"
	state.configured = true
	state.lastBatteryVoltage = 6
    state.remove("lastPoll")
    state.remove("waitForGetInfo")
    removeDataValue("lastPoll")
    if (device.data.firmwareMT == "113B-03E8-0000001D"){ state.firm = "113B-03E8-0000001D Tested"}
    if (device.data.firmwareMT == "113B-03E8-00000019"){ state.firm = "113B-03E8-00000019 Tested"}

    else {state.firm = "${device.data.firmwareMT} Unknown Submit yours"}
    

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
