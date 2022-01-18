/**
   Hubitat LeakSmart Water Valve driver
   Hubitat Iris Water Valve driver
   with mains detection

   leaksmart driver hubitat 

 _                _    _____                      _     _   _       _           
| |              | |  /  ___|                    | |   | | | |     | |          
| |     ___  __ _| | _\ `--. _ __ ___   __ _ _ __| |_  | | | | __ _| |_   _____ 
| |    / _ \/ _` | |/ /`--. \ '_ ` _ \ / _` | '__| __| | | | |/ _` | \ \ / / _ \
| |___|  __/ (_| |   </\__/ / | | | | | (_| | |  | |_  \ \_/ / (_| | |\ V /  __/
\_____/\___|\__,_|_|\_\____/|_| |_| |_|\__,_|_|   \__|  \___/ \__,_|_| \_/ \___|



LeakSmart Valve FCC ID: W7Z-ZICM357SP2

tested on firmware 
113B-03E8-0000001D false mains flag sent (detect mains by voltage)
113B-03E8-00000019 valid mains flag 




https://leaksmart.com/storage/2020/01/Protect-by-LeakSmart-Manual.pdf


web   >   https://github.com/tmastersmart/hubitat-code/blob/main/leaksmart-water-valve.groovy
import>   https://github.com/tmastersmart/hubitat-code/raw/main/leaksmart-water-valve.groovy


  Changelog:
    2.9   01/17/2022   Slight changes to mains detect to fix false mains flags.
    2.8   10/14/2021   Added back switch/contact and Water sensor Wet= Open Dry=Closed
                       Notifier APP has no valve option this fixes it
    2.7   10/05/2021   Updates to match my others drivers version no system
    2.6   09/11/2021   Detection for false mains flag (firm bug fixed)
    2.5.1 09/10/2021   Mains is working on v2.1 but not v1 valve
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
The device may require a power cycled before a reset. Removing AC adapter and bat. Then power back up.


Notes:
False mains flags seen on v1 valves but v2.1 works. If the bat starts discharging then the mains report
will be considered false and ignored until it reports battery or you change the batteries. 




Post comments here
http://www.winnfreenet.com/wp/2021/09/leaksmart-water-valve-driver-for-hubitat/




 *  forked from https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/krlaframboise/leaksmart-water-valve.src
 *  Author:Kevin LaFramboise (krlaframboise)(from 1.3 orginal)    (Mode: 8830000L)
 *
 *  Above looks to be forked from Orignal SmartThings code at
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
def clientVersion() {
    TheVersion="2.8"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


metadata {
	definition (name: "LeakSmart Water Valve", namespace: "tmastersmart", author: "Tmaster", importUrl:"https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/leaksmart-water-valve.groovy" ) {
		capability "Actuator"
		capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
                capability "Contact Sensor"
		capability "Water Sensor"
		capability "Valve"
		capability "Polling"
                capability "Power Source"
        

        attribute "lastPollD", "number"
        attribute "batteryEST", "number"
	attribute "batteryVoltage", "string"

        
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02, FC02", outClusters: "0019", manufacturer: "WAXMAN", model: "leakSMART Water Valve v2.10", deviceJoinName: "leakSMART Valve v2.10" //leakSMART Valve
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0008, 000F, 0020, 0B02", outClusters: "0003, 0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Waxman Valve v1" //Waxman House Water Valve
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0004, 0005, 0006, 0020, 0B02, FC02", outClusters: "0003,0019", manufacturer: "WAXMAN", model: "House Water Valve - MDL-TBD", deviceJoinName: "Leaksmart Water Valve v1"// Lowels version
fingerprint profileId: "0104", inClusters: "0000, 0001, 0003, 0006, 0020, 0B02", outClusters: "0019"
	}
// need fingerprints  (TBD = to be determined) it looks like TBD has a mains reporting bug
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
    state.supplyPresent = true
    state.badSupplyFlag = false
    if (!state.configured) {	return response(configure())}
}

def parse(String description) {
	clientVersion()
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
		        def val3 = (evt.value == "on") ? "wet" : "dry"
			result << createEvent(name: "contact", value: val2)
			result << createEvent(name: "valve", value: val2)
		        result << createEvent(name: "water", value: val3)
			result << createEvent(name: "switch", value: evt.value, displayed:false)
            log.info "${device}: Valve ${val2}"
			result << createEvent(name: "lastPoll",value: new Date().time, isStateChange: true, displayed: false)
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
		
            // watch for battery discharging to detect mains error
	    // Mains,Battery,DC,Unknown
// Changed should never be under 6 if good ac power
		def testVoltage = (state.lastBatteryVoltage - 0.2)

		if (batteryVoltage < 6 ){
                if (state.supplyPresent){
                    log.info "${device} : discharging detected Last:${state.lastBatteryVoltage}v > Current:${batteryVoltage}v" 
                    state.supplyPresent = false
	            state.badSupplyFlag = true
		    result << createEvent(name: "PowerSource", value: "battery", isStateChange: true)
                  }
            }
            // this valve does not go up unless bat is changed
	    // we assume Mains and if it discharges its not. 
            // if 6v it is on mains or full battery		
            if (batteryVoltage > 5.99){
                if(!state.supplyPresent){
                    log.info "${device} : Battery change detected Last:${state.lastBatteryVoltage}v < Current:${batteryVoltage}v" 
                    state.supplyPresent = true
                    state.badSupplyFlag = false
		    result << createEvent(name: "PowerSource", value: "mains", isStateChange: true)
                }
            }
            state.lastBatteryVoltage = batteryVoltage
	   // end new mains detect		
        }

        // This is the mains detection mains,batterty,dc,unknown
	// Some valves give false reports so we have to detect them    
        if (evt.name == "powerSource"){
            def val4 = evt.value
		if (val4=="mains"){
		  if (device.data.firmwareMT == "113B-03E8-0000001D"){log.info "${device}: This model reports false mains flag.."}
		  if (state.badSupplyFlag){log.info "${device}: Bat discharging"}
		  if (!state.badSupplyFlag){
                  state.supplyPresent = true
                  result << createEvent(name: "powerSource", value: "mains")
                  log.info "${device}: Received powerSource: mains"
		  }
		}	
		else {
		  state.supplyPresent = false
		  state.badSupplyFlag= false	  
                  result << createEvent(name: "powerSource", value: "battery")
			  log.info "${device}: Received powerSource: ${val4}/battery"
		  }
                }
        
        
	//	result << createEvent(evt)    left over this I think was to create events for unknown items
 }
	return result
}


// from old smartthings contact/switch code 
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
	log.info "${device}: Polling:"
//                return refresh()
//	}
//	else {
//        logDebug "${device}: Skipping Poll: must be > ${minimumPollMinutes} minutes"
//        log.info "${device}: Skipping Poll: must be > ${minimumPollMinutes} minutes"
//	}

// the above throws this error
//	groovy.lang.GroovyRuntimeException: Ambiguous method overloading for method java.lang.Long#minus.
//Cannot resolve which method to invoke for [null] due to overlapping prototypes between:
//	[class java.lang.Character]
//	[class java.lang.Number] on line 261 (method poll)
    
    return refresh()
}



def refresh() {
    clientVersion() 
    log.info "${device}: Refreshing ${state.model}"
    state.supplyPresent = true
    state.badSupplyFlag = false
    log.info "${device}: ${device.data.manufacturer} Mdl: ${device.data.model}  Firmware: ${device.data.firmwareMT} softwareBuild: ${device.data.softwareBuild}"
	
    return zigbee.onOffRefresh() + getBatteryReport() +
    zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE) +
    zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING) +
    zigbee.onOffConfig() +
    zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING, TYPE_U8, 600, 21600, 1) +
    zigbee.configureReporting(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE, TYPE_ENUM8, 5, 21600, 1)+
    configureBatteryReporting()
}

def configure() {

	state.configured = true
	state.supplyPresent = true
        state.badSupplyFlag = false
	state.lastBatteryVoltage = 6
	
	state.logo ="<img src='data:image/jpeg;base64,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'>"

    state.remove("lastPoll")
    removeDataValue("lastPoll")
    state.firm = device.data.firmwareMT
	if(device.data.model=="House Water Valve - MDL-TBD"){state.model = "v1a"}
        if(device.data.model=="leakSMART Water Valve v2.10"){state.model = "v2.1"}
     log.info "${device}: Configuring ${state.model}"
     log.info "${device}: ${device.data.manufacturer} Mdl: ${device.data.model}  Firmware: ${device.data.firmwareMT} softwareBuild: ${device.data.softwareBuild}"


return   zigbee.onOffConfig() +configureBatteryReporting() +
    zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING, TYPE_U8, 600, 21600, 1) +
    zigbee.configureReporting(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE, TYPE_ENUM8, 5, 21600, 1) +
    zigbee.onOffRefresh() +
    zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE) +
    zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING) +
    getBatteryReport() 
}



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
