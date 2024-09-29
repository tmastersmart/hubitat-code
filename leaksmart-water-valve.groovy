/**
   Hubitat LeakSmart Water Valve driver for hubitat
   Hubitat Iris Water Valve driver
   with mains detection,Appliance Alerts,Test system,
   Test schedule.

   leaksmart driver hubitat 

 _                _    _____                      _     _   _       _           
| |              | |  /  ___|                    | |   | | | |     | |          
| |     ___  __ _| | _\ `--. _ __ ___   __ _ _ __| |_  | | | | __ _| |_   _____ 
| |    / _ \/ _` | |/ /`--. \ '_ ` _ \ / _` | '__| __| | | | |/ _` | \ \ / / _ \
| |___|  __/ (_| |   </\__/ / | | | | | (_| | |  | |_  \ \_/ / (_| | |\ V /  __/
\_____/\___|\__,_|_|\_\____/|_| |_| |_|\__,_|_|   \__|  \___/ \__,_|_| \_/ \___|



LeakSmart Valve FCC ID: W7Z-ZICM357SP2

tested on firmware 
113B-03E8-0000001D (01-1D-01-0A) false mains flag sent
113B-03E8-00000019 valid mains flag 




https://leaksmart.com/storage/2020/01/Protect-by-LeakSmart-Manual.pdf


web   >   https://github.com/tmastersmart/hubitat-code/blob/main/leaksmart-water-valve.groovy
import>   https://github.com/tmastersmart/hubitat-code/raw/main/leaksmart-water-valve.groovy


  Changelog:
    3.4.0 09/29/2024   Battery adjustments added
    3.3.3 09/29/2024   Convert bat voltage to number not string
    3.3.2 09/15/2024   Fix for 'Device XXX generates excessive hub load' 
    3.3.1 12/08/2022   rewrites of parsing code Presence added. Bat% changed to automatic
    3.3.0 10/28/2022   Removed code to detect false mains flag. It was not working on all valves
                       If your valve sends false mains flags you will just have to ignore them.
    3.2.3 10/18/2022   Test attribute added for routines to monitor. true when running
    3.2.2 09/27/2022   bug in bat logging. Added test function, Added test schedule
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
False mains flags seen on v1 valves but v2.1 works. 

Warning:
Valve has internal testing routines Appliance Alerts not implimented in any other driver.
Code has been added to read detect falures but is still being debugged. Device will send failed test messages
and should detect stuck valves. No documentation exist for this I am creating my own.



Valves need to be manualy tested to make sure the water does shut off. Dont rely on the CLOSED report.
If the valve sticks remove the 4 screws on the motor. Take photos of what it looks like.
Remove the gear then remove the 3 screws and turn the valve around several times with a wrench to free the valve.
You may have to rotate it back and forth several times to get it free. 
Afterward test often making sure the water goes off.

Update its possible for the valve to stick and the motor to still function. Testing is importiant!


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


def final short CLUSTER_BASIC_CNFG                  = 0x0000
def final short CLUSTER_PWR_CNFG                    = 0x0001
def final short CLUSTER_ON_OFF                      = 0x0006
def final short CLUSTER_POLL_CONTROL                = 0x0020
def final short CLUSTER_APPLIANCE_ALERTS            = 0x0B02



Much of this code is my own but parts will contain code from 

    Forked fromIris source code
 *  https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_Waxman_SmartValve.driver
 *  https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_Waxman_SmartValveHA.driver
 *
 *  forked from https://github.com/krlaframboise/SmartThings/tree/master/devicetypes/krlaframboise/leaksmart-water-valve.src
 *  Author:Kevin LaFramboise (krlaframboise)(from 1.3 orginal)    (Mode: 8830000L)
 *
 *  Above looks to be forked from Orignal SmartThings code at
 *  https://raw.githubusercontent.com/mleibman/SmartThingsPublic/a5bc475cc1b2edc77e4649609db5833421ad7f48/devicetypes/smartthings/zigbee-valve.src/zigbee-valve.groovy
 *
 */
def clientVersion() {
    TheVersion="3.4.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     logging("Upgrading Driver v${state.version}", "warn") 
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
        capability "Health Check"
        capability "Initialize"
	    capability "PresenceSensor"
        
     
        command "test"
        command "checkPresence"
        command "uninstall"

//    attribute "lastPollD", "string"
	attribute "batteryVoltage", "number"
    attribute "Alert", "string"
    attribute "Test", "bool"    

        
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
        

	input name: "reportWD",     type: "bool", title: "Report WET/DRY", description: "Send the wet dry signal", defaultValue: false,required: false
    input(  "testMonths", "enum", title: "Test every x months", description: "set Chron for a valve test every x months. Events are hidden during test to stop scripts from running. Valve may still run its own internal 1 year test. Press save then config", options: ["1","2","3","4","5","6","7","8","9","10"],defaultValue: 6,required: true)
    input(  "testHr", "enum", title: "Test Hour", description: "The hr of the day to run the test", options: ["0","1","2","3","4","5","6","7","8","9","10","11","12","14","15","16","17","18","19","20","21","22","23"],defaultValue: 0,required: true)
   
    input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
    input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 10 ,required: true 
    input name: "maxV",   type: "enum", title: "Max Voltage",description: "set the max batt voltage ", options: ["6.0","6.1","6.2","6.3","6.4","6.5","6.6","6.7"], defaultValue: "6.0" ,required: true  
  
        
    }
//testMonths	

}
private getCLUSTER_BASIC() { 0x0000 }
private getCLUSTER_POWER() { 0x0001 }
private getBASIC_ATTR_POWER_SOURCE() { 0x0007 }
private getPOWER_ATTR_BATTERY_PERCENTAGE_REMAINING() { 0x0021 }
private getTYPE_U8() { 0x20 }
private getTYPE_ENUM8() { 0x30 }


def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
pollHR = 10
pingIt = 30    
configure()   
updated()
    
}
def initialize(){
    pollHR = 10
    pingIt = 30
    installed()
}




def uninstall() {// need to clear everything before manual driver change. 
  delayBetween([
    unschedule(),
    state.icon = "",
    state.donate = "",
    state.remove("minVoltTest"), 
    state.remove("presenceUpdated"),    
	state.remove("version"),
    state.remove("checkPhase"),
    state.remove("lastCheckInMin"),
    state.remove("icon"),
    state.remove("logo"),  
    state.remove("DataUpdate"),
    state.remove("lastCheckin"),
    state.remove("lastBatteryVoltage"),  
    state.remove("lastPoll"),
    state.remove("donate"),
    state.remove("model"),
    state.remove("MFR"),
    state.remove("poll"),
    state.remove("ping"),
    state.remove("tempAdj"),
    logging("Uninstalled - States removed you may now switch drivers", "info") , 
    ], 200)  
}

def updated(){
    logging("Updated ", "info")
    loggingUpdate()
    clientVersion()
    if (!state.configured) {	return response(configure())}
}



// CLUSTER_APPLIANCE_ALERTS            = 0x0B02
def parse(String description) {
    logging("Parse: [${description}]", "trace")
    state.lastCheckin = now()
    checkPresence()
    clientVersion()
    
    if (description?.startsWith('enroll request')) { 
     zigbee.enrollResponse()
     return  
    }  
	
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
  
    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("MAP: ${descMap}", "trace")    
    if (!descMap) {return}
    
     // fix parse Geting 2 formats so merge them
    if (descMap.clusterId) {descMap.cluster = descMap.clusterId}

 if (descMap.cluster == "0000" ) {
      if (descMap.attrId== "0000" && descMap.attrInt ==0){
          state.v1 = descMap.value
          logging("Firmware ZCL v${descMap.value}", "debug") 
       }      
      if (descMap.attrId== "0001" && descMap.attrInt ==1){
          state.v2 = descMap.value
          logging("Firmware APP v${descMap.value}", "debug")
      }
      if (descMap.attrId== "0002" && descMap.attrInt ==2){
          state.v3 = descMap.value
          logging("Firmware STACK v${descMap.value}", "debug")
      }
      if (descMap.attrId== "0003" && descMap.attrInt ==3){
          logging("Firmware Hardware v${descMap.value}", "debug")
          state.v4 = descMap.value
      }

     if(state.v1 && state.v2 && state.v3 && state.v4){
         state.firmware = "${state.v1}-${state.v2}-${state.v3}-${state.v4}"
         logging("Firmware  v${state.firmware}", "debug")
         state.remove("v1") 
         state.remove("v2")
         state.remove("v3")
         state.remove("v4")
     } 
             
     
        if (descMap.attrId== "0004" && descMap.attrInt ==4){
        state.MFR = descMap.value     
        logging("Manufacturer :${state.MFR}", "debug") 
        updateDataValue("manufacturer", state.MFR)
        state.DataUpdate = true 
        return    
        } 
        if (descMap.attrId== "0005" && descMap.attrInt ==5){
        state.model = descMap.value 
        logging("Model :${state.model}", "debug")   
        updateDataValue("model", state.model)
        state.DataUpdate = true    
        return    
        } 
        if (descMap.attrId== "0007" && descMap.attrInt ==7){
        logging("Mains :${descMap.value}", "debug")   
        }
     
     // Cluster 0000 attrID:0007 attrInt7 value01 (power source mains)
     logging("Cluster 0000  attrID:${descMap.attrId} attrInt${descMap.attrInt} value${descMap.value}", "trace")

} 

def evt = zigbee.getEvent(description)
           if (evt){
               logging("Event: ${evt}", "debug")
               processEvt(evt)
               return// we stop here if valid event
           } 



// only undetected events here

    
    
// reverse engenered from iris source code (no one else has this working) We have only seen yearly test not monthly
// clusterId:0B02 clusterInt:2818 options:0040 command:01 data:[00, 00, 86]     
 if (descMap.clusterId == "0B02"){
        logging ("Alerts clusterInt:${descMap.clusterInt} options:${descMap.options} command:${descMap.command} data:${descMap.data}","debug")  
        msgId = descMap.command // We dont have a msgID value use command? Why does byte2 have a 86 in it
        dataByte0 = descMap.data[0]
        dataByte1 = descMap.data[1]
        dataByte2 = descMap.data[2]
        state.alarms = descMap.data // For testing
        logging ("Alerts msgId:${msgId} data:${descMap.data}","debug")  
//      sendEvent(name: "Alert",value: map.data,descriptionText: "command:${map.command} data:${map.data}", isStateChange: true, displayed: true) 
//        if (msgId == "00" || msgId == "01"){
          if (dataByte1 == "00") {
              logging ("Status: Clear","info")
              sendEvent(name: "Alert",value: "clear" ,descriptionText: "clear command:${descMap.command} data:${descMap.data}", isStateChange: true, displayed: true)
          
//		 } else {
//         logging ("Valve Operation Failure Alert","warn") 
//          sendEvent(name: "Alert",value: "fail",descriptionText: "Valve Operation Failure Alert", isStateChange: true, displayed: true)

//        if (msgId == "02") {// Events Notification
           }else if (dataByte1 == "80" ) {// 0x80 (-128)
					logging ("Starting Monthly Test","info") 
					state.lastTest = new Date().format("MM/dd/yyyy", location.timeZone)
                    sendEvent(name: "Alert",value: "test",descriptionText: "Starting Monthly Test", isStateChange: true, displayed: true)
            }else if (dataByte1 == "81") {// 0x81 (-127)
                	logging ("Valve Operation Failure Event","warn") 
                    sendEvent(name: "Alert",value: "fail",descriptionText: "Valve Operation Failure Event", isStateChange: true, displayed: true)
            }else if (dataByte1 == "82") {// 0x82 (-126)
                    logging ("Monthly Test NOT performed","warn") 
                    sendEvent(name: "Alert",value: "none",descriptionText: "Monthly Test NOT performed", isStateChange: true, displayed: true)
            }else if (dataByte1 == "83") {// 0x83 (-125)
                	logging ("Monthly Test Completed Successfully","info") 
                     sendEvent(name: "Alert",value: "ok",descriptionText: "Monthly Test Completed Successfully", isStateChange: true, displayed: true)
					state.lastTest = new Date().format("MM/dd/yyyy", location.timeZone)
            }else{
                logging ("Unexpected Event Data:${dataByte1}","info" )
                sendEvent(name: "Alert",value: "unknown",descriptionText: "Unexpected Event Data:${dataByte1}", isStateChange: true, displayed: true)       
            }
     
 

// just ignore these unknown clusters for now
}else if (descMap.cluster == "0500" ||descMap.cluster == "0006" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" || descMap.cluster == "8013") {
   text= ""
      if (descMap.cluster =="8001"){text="GENERAL"}
 else if (descMap.cluster =="8021"){text="BIND RESPONSE"}
 else if (descMap.cluster =="8031"){text="Link Quality"}
 else if (descMap.cluster =="8032"){text="Routing Table"}
 else if (descMap.cluster =="8013"){text="Multistate event"} 
 else if (descMap.cluster =="0001"){text="Powercluster with no event"}
   
   if (descMap.data){text ="${text} clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Cluster${descMap.cluster} Ignoring ${descMap.data} ${text}", "debug") 


 }  else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

    }    
    
    


// test for known events
def processEvt(evt) { 
        if (evt.name == "switch") {// valve status 
          if ( state.valve != device.currentState("switch")?.value){state.value= device.currentState("switch")?.value}  // try to stop exesive hub events
		 def val2 = (evt.value == "on") ? "open" : "closed"
		 def val3 = (evt.value == "on") ? "wet" : "dry"
         logging ("Valve: ${val2} Water:${val3} Switch:${evt.value} Contact:${val2}","debug")
           // Prevent repeaded events
           if(state.valve != val2 && !state.test ){
            sendEvent(name: "contact",value: val2, isStateChange: true, displayed: true)
            sendEvent(name: "valve", value: val2, isStateChange: true, displayed: true,descriptionText: "${val2} last state:${state.valve} v${state.version}")
            sendEvent(name: "switch", value: evt.value, isStateChange: true, displayed: true) 
            if(reportWD){sendEvent(name: "water", value: val3, isStateChange: true, displayed: true)}
            logging ("${device} : Event Valve: ${val2}","info")
            state.valve = val2
            return   
            } 
            else{
                logging ("Received Valve:${val2} Our State:${state.valve}","info")
            return
            }
		}
// This should be the battery % 
  if (evt.name == "battery") {
              
        def val3 = evt.value
        state.CalcBat = false 
        if ( evt.value != device.currentState("battery")?.value){ sendEvent(name: "battery", value: val3,unit:"%",isStateChange: true,displayed: true) }// stop overload
        logging ("Rec Battery:${val3}%   ${evt}","info")
  }
//  We will get 2 bat% readings (some devices dont report % so we do it also)        
//  voltage status      
  if (evt.name == "batteryVoltage") {
		def batteryVoltage = evt.value
        BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 3.50
		BigDecimal batteryVoltageScaleMax = 6
      
    if (maxV == "6.1"){batteryVoltageScaleMax = 6.10 }
    if (maxV == "6.2"){batteryVoltageScaleMax = 6.20 }
    if (maxV == "6.3"){batteryVoltageScaleMax = 6.30 }
    if (maxV == "6.4"){batteryVoltageScaleMax = 6.40 }
    if (maxV == "6.5"){batteryVoltageScaleMax = 6.50 }
    if (maxV == "6.6"){batteryVoltageScaleMax = 6.60 }
    if (maxV == "6.7"){batteryVoltageScaleMax = 6.70 }
    if (maxV == "6.8"){batteryVoltageScaleMax = 6.80 }      
      
		
		batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
      if(state.lastBatteryVoltage != batteryVoltage){
        if(state.CalcBat == true){

            
        if ( batteryPercentage != device.currentState("battery")?.value){ sendEvent(name: "battery",value: batteryPercentage ,unit:"%",isStateChange: true,displayed: true)}
        }
        if ( batteryVoltage  != device.currentState("batteryVoltage")?.value){ sendEvent(name: "batteryVoltage", value: batteryVoltage    ,unit:"V",descriptionText: "Last${state.lastBatteryVoltage}v  v${state.version}",isStateChange: true,displayed: true) }
        logging ("Battery Voltage ${batteryVoltage}v Calc ${batteryPercentage}%","info")    
      }
      else{logging ("Received Voltage:${batteryVoltage}v same as Last:${state.lastBatteryVoltage}v","info")}

   state.lastBatteryVoltage = batteryVoltage
}// end bat voltage	

    
// This is the mains detection mains,batterty,dc,unknown
// Some models give false reports so we have create our own mains flags based on voltage   
   if (evt.name == "powerSource"){
        def val4 = evt.value
       logging ("Received powerSource: ${val4}","info")
		if (val4=="mains"){
		  if (device.data.firmwareMT == "113B-03E8-0000001D" || state.firmware == "01-1D-01-0A"){logging ("${device} : This model reports false mains flag..","debug")}
              state.supplyPresent = true
            
       if ( val4 != device.currentState("powerSource")?.value){sendEvent(name: "powerSource",value: "mains",descriptionText: "mains ${state.version}", isStateChange: true)}
		}	
		else {
		  state.supplyPresent = false
          if ( val4 != device.currentState("powerSource")?.value){sendEvent(name: "powerSource",value: "battery",descriptionText: "${val4} battery ${state.version}", isStateChange: true)}
		  }
         }// end powersource
  }// end evt



// from old smartthings contact/switch code 
def on()   {	open()  } 
def off()  {	close() }


def open() {
    logging ("Opening the valve","info") 
	zigbee.on()
}
def close() {
    logging ("Closing the valve","info") 
	zigbee.off()
}


def poll() {
    logging ("Polling:","info") 
    return refresh()
}

def ping() {
    logging ("Polling:","info") 
    return refresh()
}

def testF(){
state.lastTest =  new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
state.test = false
logging ("Test finished.","info") 
    sendEvent(name: "Test",value: state.test ,descriptionText: "The Test has finished", isStateChange: true, displayed: true)  
}

def test(){
   state.test = true
   sendEvent(name: "Test",value: state.test ,descriptionText: "A Test is in process LAST:${state.lastTest}", isStateChange: true, displayed: true) 
   logging ("Testing the valve. Supressing events. LAST:${state.lastTest}","info")  
   runIn(1,close)
   runIn(32,open)  
   runIn(45,testF)
   runIn(60,getApplianceAlerts) 
 
}


def refresh() {
    if(state.DataUpdate){ logging("Refreshing ${state.MFR} Model:${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}
// Fixed This is the only way polling will work.
// None of the other drivers work
// device needs lots of time to process cmds
runIn(5,getSwitchReport)    // valve
runIn(10,getBatteryReport)  // v 
runIn(15,getBatteryReport2) // mains
runIn(30,getBatteryReport3) // %   
runIn(35,getApplianceAlerts)  
    
 

}

def configure() {

    logging ("Configuring","info") 
    // Poll the device every x min    0 0 23 ? 1/6 * *   0 0 1 */6 *
  //0 24 1 1-7/6 *
    unschedule()
	randomSixty = Math.abs(new Random().nextInt() % 60)
    schedule("0 ${randomSixty} ${testHr} 1 1/${testMonths} ?", test)

    logging("Setting Chron Test: ${testHr}:${randomSixty}am every ${testMonths} months.", "info")
    getIcons()
    
    if(reportWD){removeDataValue("water")}
 	state.configured = true
	state.supplyPresent = true
    state.CalcBat = true // will go false if we get % report
    // upgrade from old versions
    state.remove("badSupplyFlag")
    state.remove("paypal")
    state.remove("BatCal")
    state.remove("BatRec")
    state.remove("firm")
    state.remove("firmware1")

    
    
    state.test = false
	state.lastBatteryVoltage = 0 // force a new event

    state.logo ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/leaksmart.jpg' >"
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
 
    version = "?"
	if(state.model=="House Water Valve - MDL-TBD"){state.model = "House Water Valve - MDL-TBD v1a"}// fix (TBD=To Be Determed) they forgot put the version no in the firmware
//  if(state.model=="leakSMART Water Valve v2.10")
    logging ("${state.MFR} Mdl: ${state.model} Firmware: ${state.firm} softwareBuild: ${device.data.softwareBuild}","info") 
    
    
runIn(1,configurePowerSourceReporting)
runIn(10,configureBatteryReporting) 
runIn(15,getSwitchReport)    // valve
runIn(20,getBatteryReport)  // v 
runIn(25,getBatteryReport2) // mains
runIn(40,getBatteryReport3) // %   
runIn(45,getApplianceAlerts)
runIn(55,configurePowerSourceReporting)
runIn(60,getFirmware)
     
 
    if (pollYes){ 
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)    
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
    logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", "debug") 
    schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", checkPresence)	
    logging("Presence Check Every ${pollHR}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
    }     
    
    
delayBetween([
    sendZigbeeCommands(zigbee.onOffConfig()), 
    sendZigbeeCommands(zigbee.configureReporting(0x0001, 0x0021, 0x20, 600, 21600, 1)), // power bat% //U8=20
    sendZigbeeCommands(zigbee.configureReporting(0x0000, 0x0007, 0x30, 5, 21600, 1)),// power source enum8 = 30
    sendZigbeeCommands(zigbee.onOffRefresh()),
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0007)),// read power source
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0021)),// Read battery %
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// ATTR_MANUFACTURER_NAME
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// ATTR_MODEL_IDENTIFIER
	], 35000)
}

//   const u16 ATTR_ZCL_VERSION = 0x0000;
//   const u16 ATTR_APPLICATION_VERSION = 0x0001;
//   const u16 ATTR_STACK_VERSION = 0x0002;
//   const u16 ATTR_HARDWARE_VERSION = 0x0003; 

def getFirmware(){
    logging ("Requesting Firmware ver","debug")
    delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0000)),// ATTR_ZCL_VERSION 
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0001)),// 
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0002)),// 
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0003)),// 
	], 35000)


}


//status alearts pulled from iris source code
def getApplianceAlerts() {
    logging ("getApplianceAlerts","debug")
	sendZigbeeCommands(zigbee.readAttribute(0x0B02, 0x0000))
}

def configurePowerSourceReporting(){
    logging ("configurePowerSourceReporting","debug")
// configure power source reporting interval from iris
sendZigbeeCommands(zigbee.configureReporting(0x0000, 0x0007, 0x30, 5, 21600, 1))//basic-attr power source-ENUM8
}


def configureBatteryReporting() {
    logging ("configure Battery Reporting max: 5hrs min:30sec","debug") 
	def minSeconds = (30 * 60) // 30 Minutes
	def maxSeconds = (5* 60 * 60) // 5 Hours	
	sendZigbeeCommands(zigbee.configureReporting(0x0001, 0x0020, 0x20, minSeconds, maxSeconds, 0x01))
}

def getSwitchReport() {
    logging ("get Switch Report","debug")
	sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000))
}

def getBatteryReport() {
    logging ("get BatteryVoltage","debug")
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)) //Read BatteryVoltage
}
def getBatteryReport2() {
    logging ("get Power Source","debug")
    sendZigbeeCommands(zigbee.readAttribute(0x000, 0x0007))  //Read PowerSource
}
def getBatteryReport3() { 
    logging ("get Battery %","debug")
    sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0033))  //Read BatteryQuantity
}
def checkPresence() {
    // presence routine. v5.1 11-12-22
    // simulated 0% battery detection
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 20
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins Timeout:${checkMin} Tries:${state.tries}","debug")
    if (state.lastCheckInMin <= checkMin){ 
        state.tries = 0
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
            logging("Creating presence event: ${value}  ","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin) { 
      state.tries = state.tries + 1
      if (state.tries >=5){
        test = device.currentValue("presence")
        if (test != "not present" ){
         value = "not present"
         logging("Creating presence event: ${value}","warn")
         sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
         sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)    
         return // we dont want a ping after this or it could toggle
         }
         
     } 
       
     runIn(2,ping)
     if (state.tries <4){
         logging("Recovery in process Last checkin ${state.lastCheckInMin} min ago ","warn") 
         runIn(50,checkPresence)
     }
    }
}
void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}


void getIcons(){
    state.logo ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/leaksmart.jpg' >"
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
 }


// Logging block  v4

void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
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
    if (level == "infoBypass"){log.info  "${device} : $message"}
	if (level == "error"){     log.error "${device} : $message"}
	if (level == "warn") {     log.warn  "${device} : $message"}
	if (level == "trace" && traceLogging) {log.trace "${device} : $message"}
	if (level == "debug" && debugLogging) {log.debug "${device} : $message"}
    if (level == "info"  && infoLogging)  {log.info  "${device} : $message"}
}




/**
Hubitat LeakSmart Water Valve driver
Hubitat Iris Water Valve driver
mains detection
leaksmart driver hubitat
iris water valve driver


*/
