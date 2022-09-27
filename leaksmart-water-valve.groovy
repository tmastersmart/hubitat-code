/**
   Hubitat LeakSmart Water Valve driver for hubitat
   Hubitat Iris Water Valve driver
   with mains detection,Appliance Alerts

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
    3.2.1 09/27/2022   Wet/Dry optional
    3.2   09/05/2022   Decoding events rewritten. New alert added from iris source code.
    3.1   09/01/2022   Stoped repeated open close events.
    3.0   08/31/2022   Upgraded Logs and Events to current standards. Reduced event traffic
                       Removed unneeded polling dates. Code Rewrites, Polling updated
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


To reset the valve for repairing, rapidly press the center button 5 times. The Blue LED light will begin to flash
indicating it is reset and ready to join.
The device may require a power cycled before a reset. Removing AC adapter and bat. Then power back up.


Notes:
False mains flags seen on v1 valves but v2.1 works. If the bat starts discharging then the mains report
will be considered false and ignored until it reports battery or you change the batteries. 

Warning:
Valve has internal testing routines Appliance Alerts not implimented in any other driver.
Code has been added to read detect falures but is still being debugged. Device will send failed test messages
and should detect stuck valves. More work is needed. No documentation exist for this I am creating my own.


Valves need to be manualy tested to make sure the water does shut off. Dont rely on the CLOSED report.
If the valve sticks remove the 4 screws on the motor. Take photos of what it looks like.
Remove the gear then remove the 3 screws and turn the valve around several times with a wrench to free the valve.
You may have to rotate it back and forth several times to get it free. 
Afterward test often making sure the water goes off.


Post comments here
http://www.winnfreenet.com/wp/2021/09/leaksmart-water-valve-driver-for-hubitat/

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
=======================================================================================================

Iris source code
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_Waxman_SmartValve.driver
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_Waxman_SmartValveHA.driver
def final short CLUSTER_BASIC_CNFG                  = 0x0000
def final short CLUSTER_PWR_CNFG                    = 0x0001
def final short CLUSTER_ON_OFF                      = 0x0006
def final short CLUSTER_POLL_CONTROL                = 0x0020
def final short CLUSTER_APPLIANCE_ALERTS            = 0x0B02


 *  forked from https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/krlaframboise/leaksmart-water-valve.src
 *  Author:Kevin LaFramboise (krlaframboise)(from 1.3 orginal)    (Mode: 8830000L)
 *
 *  Above looks to be forked from Orignal SmartThings code at
 *  https://raw.githubusercontent.com/mleibman/SmartThingsPublic/a5bc475cc1b2edc77e4649609db5833421ad7f48/devicetypes/smartthings/zigbee-valve.src/zigbee-valve.groovy
 *
 */
def clientVersion() {
    TheVersion="3.2.1"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType

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
        

//    attribute "lastPollD", "string"
	attribute "batteryVoltage", "string"
    attribute "Alert", "string"

        
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
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
        

	input name: "calBat",       type: "bool", title: "Calculate Bat%", description: "If you do not receive bat% reports create them", defaultValue: true
	input name: "reportWD",     type: "bool", title: "Report WET/DRY", description: "Send the wet dry signal", defaultValue: false,required: true
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
	loggingUpdate()

}

// CLUSTER_APPLIANCE_ALERTS            = 0x0B02
def parse(String description) {
    logging ("${device} : ${description}","trace") 
	clientVersion()
    state.lastPoll = new Date().format("MM/dd/yyyy", location.timeZone)
    def evt = zigbee.getEvent(description)
    if (evt) {
        processEvt(evt)// if its a known event get out
        return
        }
    
    Map map = zigbee.parseDescriptionAsMap(description)
    if (map) {   
        logging ("${device} : MAP: clusterId:${map.clusterId} clusterInt:${map.clusterInt} options:${map.options} command:${map.command} data:${map.data}","debug")  
 

// reverse engenered from iris source code (no one else has this working)
// clusterId:0B02 clusterInt:2818 options:0040 command:01 data:[00, 00, 86]
       
if (map.clusterId == "0001"){
    if(map.command == "07"){
     logging ("${device} : Replying to Set bat reporting times. command:${map.command} Int:${clusterInt} data:${map.data}","debug")
     }
        else {logging ("${device} : Battery% report with no EVENT. command:${map.command} Int:${clusterInt} data:${map.data}","debug")}
   
}
if (map.clusterId == "0000"){ logging ("${device} : Replying to Set PowerSource reporting times. command:${map.command} data:${map.data}","debug")}
        
 //MAP: clusterId:8021 clusterInt:32801 options:0040 command:00 data:[AA, 00]        
if (map.clusterId == "8021"){ logging ("${device} : Replying to Set reporting times. command:${map.command} Int:${clusterInt} data:${map.data}","debug")}



        
if (map.clusterId == "0B02"){
        logging ("${device} : Alerts clusterInt:${map.clusterInt} options:${map.options} command:${map.command} data:${map.data}","debug")  
        msgId = map.command // We dont have a msgID value use command?
//        msgId = map.data[0] 
        dataByte = map.data[1]
        logging ("${device} : Alerts msgId:${msgId} dataByte:${dataByte} data:${map.data}","info")  
        sendEvent(name: "Alert",value: map.data,descriptionText: "command:${map.command} data:${map.data}", isStateChange: true, displayed: true) 
        if (msgId == "00" || msgId == "01"){
          if (dataByte == "00") {
              logging ("${device} : Status: Clear","info")				
		 } else {
         logging ("${device} : Valve Operation Failure Alert","warn") 
          sendEvent(name: "Alert",value: "fail",descriptionText: "Valve Operation Failure Alert", isStateChange: true, displayed: true)
			}   
        }
        if (msgId == "02") {// Events Notification
			if (dataByte == "80") {// 0x80 (-128)
					logging ("${device} : Starting Monthly Test","info") 
					state.lastTest = new Date().format("MM/dd/yyyy", location.timeZone)
                    sendEvent(name: "Alert",value: "test",descriptionText: "Starting Monthly Test", isStateChange: true, displayed: true)
            }else if (dataByte == "81") {// 0x81 (-127)
                	logging ("${device} : Valve Operation Failure Event","warn") 
                    sendEvent(name: "Alert",value: "fail",descriptionText: "Valve Operation Failure Event", isStateChange: true, displayed: true)
            }else if (dataByte == "82") {// 0x82 (-126)
                    logging ("${device} : Monthly Test NOT performed","warn") 
                    sendEvent(name: "Alert",value: "none",descriptionText: "Monthly Test NOT performed", isStateChange: true, displayed: true)
            }else if (dataByte == "83") {// 0x83 (-125)
                	logging ("${device} : Monthly Test Completed Successfully","info") 
                     sendEvent(name: "Alert",value: "ok",descriptionText: "Monthly Test Completed Successfully", isStateChange: true, displayed: true)
					state.lastTest = new Date().format("MM/dd/yyyy", location.timeZone)
            }else{
                logging ("${device} : Unexpected Event Data:${dataByte}","info" )
                sendEvent(name: "Alert",value: "unknown",descriptionText: "Unexpected Event Data:${dataByte}", isStateChange: true, displayed: true)       
                       
            
            }
        }// end msg2
  }//end 0B02 
 }// end map    
}

// test for known events
def processEvt(evt) { 
    
        if (evt.name == "switch") {// valve status 
		 def val2 = (evt.value == "on") ? "open" : "closed"
		 def val3 = (evt.value == "on") ? "wet" : "dry"
         logging ("${device} : Valve: ${val2} Water:${val3} Switch:${evt.value} Contact:${val2}","debug")
           // Prevent repeaded events
           if(state.valve != val2){
            sendEvent(name: "contact",value: val2, isStateChange: true, displayed: true)
            sendEvent(name: "valve", value: val2, isStateChange: true, displayed: true,descriptionText: "${val2} last state:${state.valve} v${state.version}")
            sendEvent(name: "switch", value: evt.value, isStateChange: true, displayed: true) 
            if(reportWD){sendEvent(name: "water", value: val3, isStateChange: true, displayed: true)}
            logging ("${device} : Event Valve: ${val2}","info")
            state.valve = val2
            return   
            } 
            else{
                logging ("${device} : Received Valve:${val2} Our State:${state.valve}","info")
            return
            }
		}
// This should be the battery % 
  if (evt.name == "battery") {
            def val3 = evt.value
            if(!calBat){
            sendEvent(name: "battery", value: val3,unit:"%",isStateChange: true,displayed: true) 
            }
            logging ("${device} : Rec Battery:${value}%","info")
        }
//  We will get 2 bat% readings (some devices dont report % so we do it also)        
//  voltage status      
  if (evt.name == "batteryVoltage") {
		def batteryVoltage = evt.value
        BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 3.50
		BigDecimal batteryVoltageScaleMax = 6
		
		batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
      
      if(state.lastBatteryVoltage != batteryVoltage){
        if(calBat){sendEvent(name: "battery",        value: batteryPercentage ,unit:"%",isStateChange: true,displayed: true)}
        sendEvent(name: "batteryVoltage", value: batteryVoltage    ,unit:"V",descriptionText: "Last${state.lastBatteryVoltage}v  v${state.version}",isStateChange: true,displayed: true) 
        logging ("${device} : Battery Voltage ${batteryVoltage}v Calc ${batteryPercentage}%","info")    
      }
      else{logging ("${device} : Received Voltage:${batteryVoltage}v same as Last:${state.lastBatteryVoltage}v","info")}
// Mains,Battery,DC,Unknown

	def testVoltage = (state.lastBatteryVoltage - 0.2)
// watch for battery discharging to detect mains error(bug in firmware on some models)
// should never be under 6 if mains    
		if (batteryVoltage < 6 ){
         if (state.supplyPresent){
           state.supplyPresent = false
	       state.badSupplyFlag = true
           sendEvent(name: "powerSource",value: "battery",descriptionText: "discharging detected Last:${state.lastBatteryVoltage}v > Current:${batteryVoltage}v ${state.version}", isStateChange: true)   
           logging ("${device} : discharging detected Last:${state.lastBatteryVoltage}v > Current:${batteryVoltage}v","debug")    
            }
          }
// if 6v it is on mains or full battery		
        if (batteryVoltage > 5.99){
         if(!state.supplyPresent){
           state.supplyPresent = true
           state.badSupplyFlag = false
           sendEvent(name: "powerSource",value: "mains",descriptionText: "Battery change detected Last:${state.lastBatteryVoltage}v > Current:${batteryVoltage}v ${state.version}", isStateChange: true)   
           logging ("${device} : Battery change detected Last:${state.lastBatteryVoltage}v > Current:${batteryVoltage}v","debug")    
            }
           }

         state.lastBatteryVoltage = batteryVoltage
	   	
        }// end bat voltage	

// This is the mains detection mains,batterty,dc,unknown
// Some models give false reports so we have create our own mains flags based on voltage   
   if (evt.name == "powerSource"){
        def val4 = evt.value
		if (val4=="mains"){
		  if (device.data.firmwareMT == "113B-03E8-0000001D"){logging ("${device} : This model reports false mains flag..","debug")}
          if (state.badSupplyFlag){logging ("${device} : Bat discharging/False mains report","info")}
		  if (!state.badSupplyFlag){
              state.supplyPresent = true
              sendEvent(name: "powerSource",value: "mains",descriptionText: "mains ${state.version}", isStateChange: true)
              logging ("${device} : Received powerSource: mains","info")
		  }
		}	
		else {
		  state.supplyPresent = false
		  state.badSupplyFlag= false
          sendEvent(name: "powerSource",value: "battery",descriptionText: "${val4} battery ${state.version}", isStateChange: true)  
          logging ("${device} : Received powerSource: ${val4}/battery","info")
		  }
         }// end powersource
    
  logging ("${device} : Event: ${evt}","debug")  
 }// end evt



// from old smartthings contact/switch code 
def on()   {	open()  } 
def off()  {	close() }


def open() {
    logging ("${device} : Opening the valve","info") 
	zigbee.on()
}
def close() {
    logging ("${device} : Closing the valve","info") 
	zigbee.off()
}


def poll() {
    logging ("${device} : Polling:","info") 
    return refresh()
}



def refresh() {
    logging ("${device} : Refreshing","info") 
// Fixed This is the only way polling will work.
// None of the other drivers work
// Otherwise only the last command is honored.   
runIn(5,getSwitchReport)    // valve
runIn(10,getBatteryReport)  // v 
runIn(15,getBatteryReport2) // mains
runIn(30,getBatteryReport3) // %   
runIn(35,getApplianceAlerts)  
    
 

}

def configure() {

    logging ("${device} : Configuring","info") 

//    unschedule()

    if(reportWD){removeDataValue("water")}
 	state.configured = true
	state.supplyPresent = true
    state.badSupplyFlag = false
	state.lastBatteryVoltage = 0 // force a new event

	state.logo ="<img src='data:image/jpeg;base64,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'>"
    state.firm = device.data.firmwareMT
	if(device.data.model=="House Water Valve - MDL-TBD"){state.model = "v1a"}
    if(device.data.model=="leakSMART Water Valve v2.10"){state.model = "v2.1"}
    logging ("${device} : ${device.data.manufacturer} Mdl: ${device.data.model} ${state.model}  Firmware: ${device.data.firmwareMT} softwareBuild: ${device.data.softwareBuild}","info") 
runIn(1,configurePowerSourceReporting)
runIn(10,configureBatteryReporting) 
runIn(15,getSwitchReport)    // valve
runIn(20,getBatteryReport)  // v 
runIn(25,getBatteryReport2) // mains
runIn(40,getBatteryReport3) // %   
runIn(45,getApplianceAlerts)
runIn(55,configurePowerSourceReporting)
    
delayBetween([
    zigbee.onOffConfig(), 
    zigbee.configureReporting(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING, TYPE_U8, 600, 21600, 1),
    zigbee.configureReporting(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE, TYPE_ENUM8, 5, 21600, 1),
    zigbee.onOffRefresh(),
    zigbee.readAttribute(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE),
    zigbee.readAttribute(CLUSTER_POWER, POWER_ATTR_BATTERY_PERCENTAGE_REMAINING),
	], 30000)
}

//status alearts pulled from iris source code
def getApplianceAlerts() {
    logging ("${device} : getApplianceAlerts","debug")
	zigbee.readAttribute(0x0B02, 0x0000)
}

def configurePowerSourceReporting(){
    logging ("${device} : configurePowerSourceReporting","debug")
// configure power source reporting interval from iris
zigbee.configureReporting(CLUSTER_BASIC, BASIC_ATTR_POWER_SOURCE, TYPE_ENUM8, 5, 21600, 1)
}


def configureBatteryReporting() {
    logging ("${device} : configure Battery Reporting max: 5hrs min:30sec","debug") 
	def minSeconds = (30 * 60) // 30 Minutes
	def maxSeconds = (5* 60 * 60) // 5 Hours	
	zigbee.configureReporting(0x0001, 0x0020, 0x20, minSeconds, maxSeconds, 0x01)
}

def getSwitchReport() {
    logging ("${device} : get Switch Report","debug")
	zigbee.readAttribute(0x0006, 0x0000)
}

def getBatteryReport() {
    logging ("${device} : get BatteryVoltage","debug")
    zigbee.readAttribute(0x0001, 0x0020) //Read BatteryVoltage
}
def getBatteryReport2() {
    logging ("${device} : get Power Source","debug")
    zigbee.readAttribute(0x000, 0x0007)  //Read PowerSource
}
def getBatteryReport3() { 
    logging ("${device} : get Battery %","debug")
    zigbee.readAttribute(0x0001, 0x0033)  //Read BatteryQuantity
}

// Logging block 
//	device.updateSetting("infoLogging",[value:"true",type:"bool"])
void loggingUpdate() {
    logging("${device} : Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){runIn(3600,debugLogOff)}
    if (traceLogging){runIn(1800,traceLogOff)}
}
void loggingStatus() {logging("${device} : Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")}
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


/**
Hubitat LeakSmart Water Valve driver
Hubitat Iris Water Valve driver
mains detection
leaksmart driver hubitat
iris water valve driver


*/
