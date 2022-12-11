      /* Iris v1 contact sensor-custom

iris v1 contact sensor for hubitat

Totaly rewritten battery support.
No more negative battery readings. Min voltage will auto adjust to sensor.
This is new and will be added to all my scripts.
The default settings supplied by iris are wrong and vary from device to device.


Adjustment for temp sensor

Tested on Firmware : 2012-09-20 The last iris pushed update.
New out of the box devices may have older firmware.


Added option to force events on off. Can be used in scripts. 
Added option to force clear tamper. Can be used in scripts.
Since the tamper and contact only reports on events It will stay as you force it
until the next event.  Great if using on a custom operation.
added option to ignore tamper on broken cases.


=================
v3.2.9 12/10/2022 Auto min adj started throwing errors. Rewritten
                  firmware verson to hard to read fixed
v3.2.8 12/05/2022 was not ref after ranging
v3.2.7 11/29/2022 Ranging schedule adj
v3.2.6 11/29/2022 more bat fixes
v3.2.5 11/25/2022 bat min fix
v3.2.2 11/12/2022 Another bug fix for presence
v3.2.0 11/11/2022 Added retry and recovery mode.New firmware detection
v3.1.3 11/07/2022 compacted logging code.Rewriting sections of code
v3.1.2 11/06/2022 Trace looking bug zigbee rec should be send
v3.1.1 11/06/2022 Logos added
v3.1.0 10/30/2022 Bug fix in presence routine. not sending warning before timeout
v3.0.5 10/16/2022 Reduced precision of bat voltage to reduce events .xxx to .xx
v3.0.4 10/10/2022 Min voltage reset, Config delays changed
v3.0.0 09/21/2022 Ranging adjustments
v2.9   09/19/2022 Rewrote logging routines.
v2.8.0 09/17/2022 Presence routine rewrote from scratch
v2.7.3 09/17/2022 New temp adjust code.
                 Randomised each device so they dont all run at the same
                 time on code change and reboot.
v2.7.1           Minor code change
v2.7  09/14/2022 Temp/Battery report reverse enegered only one sent at a time
                 never together. Code adjusted. Presence changed to 1 hr
v2.6  09/13/2022 Min bat voltage tested down to 2.19 Adjustments in code
v2.5  09/08/2022 Rewritten ranging code.
v2.4  09/07/2022 adjusting bad temp/bat events
v2.3  09/07/2022 Operation event removed to reduce events
v2.2  09/06/2022 Init routine delayed. minor fixes
v2.1  09/04/2022 Updating same format routines on all my iris drivers.
                 Schedules made optional.
v2.0             Better bat routine. Detection of bad temp sensor
v1.9             Respond to enrole request
v1.8             Mains detection option added to use with a relay
v1.7  09/02/2022 Fix null in log. Refresh now random times.
v1.6  09/01/2022 Battery voltage rewriten. LQI events reduced
v1.5  09/01/2022 New release. More fixes
v1.4  08/17/2022 Bug fix on bat and temp adj
      07-27-2022 Detect dead batt. new force options. Uninstall option
      05/29/2022 Removed init routine was causing problems.
      04/11/2021 First release
=================================================================================================
https://fccid.io/WJHWD11


Before going back to internal drivers you must use uninstall to stop chron
=================================================================================================== 
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



May contain code from the following


Iris v2 source code driver here 2.3 driver
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_AlertMe_ContactSensor_2_3.driver
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/common/arcus-protocol/src/main/irp/ame-general.irp





code includes some routines based on alertme UK code from  

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
Uk Iris code 
   https://github.com/birdslikewires/hubitat/blob/master/alertme/drivers/alertme_contact.groovy
 *	
 */

def clientVersion() {
    TheVersion="3.2.9"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.helper.HexUtils


metadata {

	definition (name: "Iris v1 Contact Sensor-custom", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris%20v1%20sensor-custom.groovy") {

	capability "Battery"
	capability "Configuration"
	capability "Contact Sensor"
	capability "Initialize"
	capability "MotionSensor"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
	capability "SignalStrength"
	capability "TamperAlert"
	capability "TemperatureMeasurement"
    capability "Power Source"    


	command "checkPresence"
	command "normalMode"
    command "rangeAndRefresh"
    command "ForceClosed"
    command "ForceOpen"
    command "ClearTamper"
    command "unschedule"
    command "uninstall"


	attribute "batteryVoltage", "string"
	attribute "mode", "string"



		fingerprint profileId: "C216", inClusters: "00F0,00F1,0500,00F2", outClusters: "", manufacturer: "AlertMe", model: "Contact Sensor Device", deviceJoinName: "Iris v1 Contact Sensor"
      
	}

}


preferences {
	
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    
    input name: "tamperIgnore", type: "bool", title: "Ignore the Tamper alarm", defaultValue: false
	input name: "option1",      type: "bool", title: "Trigger Mains", description: "Use as a mains detection switch ",defaultValue: false


    input name: "tempAdj",type:"enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8",
    "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8",
    "-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",
    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",
   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: "0",required: true  


}


def installed() {

	logging("Paired!", "info")
    initialize()
    configure()
    
}

def uninstall() {
 
  delayBetween([
    unschedule(),
    state.icon = "",
    state.donate = "",
    state.remove("presenceUpdated"),    
	state.remove("version"),
    state.remove("checkPhase"),
    state.remove("lastCheckInMin"),
    state.remove("icon"),
    state.remove("logo"),  
    state.remove("DataUpdate"),
    state.remove("lastCheckin"),
    state.remove("lastPoll"),
    state.remove("donate"),
    state.remove("model"),
    state.remove("MFR"),
    state.remove("poll"),
    state.remove("ping"),
    state.remove("tempAdj"),
	state.remove("rangingPulses"),
	state.remove("operatingMode"),
	state.remove("batteryOkay"),
	state.remove("battery"),
    state.remove("LQI"),
    state.remove("batteryOkay"),
    state.remove("Config"),
    state.remove("batteryState"), 
    state.remove("reportToDev"),
    state.remove("tries"),
removeDataValue("battery"),
removeDataValue("battertState"),
removeDataValue("batteryVoltage"),
removeDataValue("contact"),
removeDataValue("lqi"),
removeDataValue("operation"),
removeDataValue("presence"),
removeDataValue("tamper")  ,  
removeDataValue("temperature"),
removeDataValue("minVoltTest"),      
 logging("Uninstalled - States removed you may now switch drivers", "info") , 
    ], 200)  
      
// Works better with a delay. Some were not getting removed      
      
}



def initialize() {

	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.
	// Reset states...

	state.presenceUpdated = 0
	state.rangingPulses = 0

	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    
	// Remove unnecessary device details.
	removeDataValue("application")


    if(!option1){sendEvent(name: "powerSource", value: "battery")}
    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 3500)
	runIn(randomSixty,refresh)
    logging("Initialised", "info")

}


def configure() {
   logging("configure", "info")
	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.
    state.Config = false // force update

    getIcons()
	unschedule()


	// Schedule randon ranging in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${5} * * ? *", rangeAndRefresh)	

    // Check presence every hr
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *", checkPresence)	

    
	// Schedule presence check in mins
//	int checkEveryMinutes = 20							
//	randomSixty = Math.abs(new Random().nextInt() % 60)
//	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)	

// Run a ranging report and then switch to normal operating mode.
// Randomise so we dont get several running at the same time
    runIn(randomSixty,rangeAndRefresh)
   
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
//    runIn(3000,loggingUpdate)
}


// useless mode shuts down the device? Unknown 
// ranging message every 2 minutes.
//void offMode() {
//	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])// shut off device
//	logging ("Mode: Quiet  [FA:03.01]","info")
//}

def normalMode() { // v2.0
        logging("Sending: [Normal Mode]  Pulses:${state.rangingPulses}", "info")                              
	    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"])// normal
   if (state.rangingPulses >15){ 
        logging("Not responding! adding extra kick", "warn")
        delayBetween([ // Once is not enough
//	    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"]),// version information request
	    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
        sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
        sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
        sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal    
	    ], 6000)
       
   }
}

void EnrollRequest(){
    logging("Responding to Enroll Request. Likely Battery Change", "warn")
    delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x0500 {11 80 00 00 05} {0xC216}"]),//enrole 
 	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x0500 {11 80 00 00 05} {0xC216}"]),//enrole 
    ], 3000)    
}    

void MatchDescriptorRequest (){
	logging("Match Descriptor Request. Sending Reply","info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])//match des    
}

def ping(){
    logging("Ping", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}

void refresh() {
    logging("Refreshing ${state.model} v${state.version}", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}

// 3 seconds mains 6 battery  2 flash good 3 bad
def rangeAndRefresh() {
    
        // we dont want to range if its not pressent we want to recover first
    test = device.currentValue("presence")
    if (test == "not present" ){
        ping()
        return
    }
    
    
    
    logging("StartMode : [Ranging]", "info")
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"]) // ranging
	state.rangingPulses = 0
	runIn(6, normalMode)
    runIn(10,refresh)
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
         state.minVoltTest = device.currentValue("batteryVoltage")   
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




  
    

def ClearTamper (){
        logging("Tamper : Cleared FORCED", "info")
		sendEvent(name: "tamper", value: "clear", isStateChange: true, descriptionText: "force cleared v${state.version}")
}

def ForceOpen (){
 contactOpen()
} 
void contactOpen(){
logging("Contact: Open", "info")
    sendEvent(name: "contact", value: "open", isStateChange: true, descriptionText: "open v${state.version}")
if(option1){
    logging("powerSource: battery", "info")
    sendEvent(name: "powerSource", value: "battery", isStateChange: true, descriptionText: "battery v${state.version}")
}    
}

def ForceClosed (){
contactClosed()
}
void contactClosed(){
logging("Contact: Closed", "info")
sendEvent(name: "contact", value: "closed", isStateChange: true, descriptionText: "closed v${state.version}")
if(option1){
    logging("powerSource: mains", "info")
    sendEvent(name: "powerSource", value: "mains", isStateChange: true, descriptionText: "mains v${state.version}")
 }
}

// sends standard zigbee command
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() || status.isAlarm2Set()) {// 2 does not look to be used on irs
        contactOpen()
    }else {contactClosed()}
}




def parse(String description) {
	logging("Parse : ${description}", "trace")
    clientVersion()
    state.lastCheckin = now()
    checkPresence()
    loggingCheck()
    // Device contacts are zigbee cluster compatable
	if (description.startsWith("zone status")) {
		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
		processStatus(zoneStatus)
        return
   }else if (description?.startsWith('enroll request')) {
        EnrollRequest()
        return
    }
 
//  processMap clusterId:00F0 command:FB [1F, 7C, 63, 16, 00, 3C, 0C, 90, 01, CF, FF, 03, 00] 13   
//  new processing routine    
    Map map = zigbee.parseDescriptionAsMap(description)
    if (!map){
        logging("Failed to parse", "debug")
         return
    }
        
    
	String[] receivedData = map.data
    size = receivedData.size()// size of data field
    logging("${map}", "trace")// full parsed data
    logging("Map clusterId:${map.clusterId} command:${map.command} map:${receivedData}", "debug")// all iris valid data

	if (map.clusterId == "00F0") {
      if (map.command == "FB") {
	   // Device status cluster.
       // if 0 bat/if 1 temp/ if 3 lqi
       // bat = 5 and 6 reversed 
       // temp =7 and 8 reversed
       // LQI = 10 (lqi * 100.0) / 255.0 
		def temperatureValue  = "NA"
        def batteryVoltageHex = "NA"
        BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
        temperatureValue = receivedData[7..8].reverse().join()

     // some sensors report bat and temp at diffrent times some both at once?
     if (batteryVoltageHex != "FFFF") {
     	batteryVoltageRaw = zigbee.convertHexToInt(batteryVoltageHex) / 1000
    	batteryVoltage = batteryVoltageRaw.setScale(3, BigDecimal.ROUND_HALF_UP)
        // Auto adjustment like iris hub did it  2.17 is 0 on the test device 
        // what is the lowest voltage this device can work on.
       if(state.minVoltTest){state.remove("minVoltTest")}   
       if(!state.minVolt){
       state.minVolt= 2.21
       logging("Min voltage set to ${state.minVolt}v Let bat run down to 0 for auto adj to work.", "info")
       }  
       if (batteryVoltageRaw < state.minVolt){
          if (state.minVolt > 2.17){ 
                state.minVolt = batteryVoltageRaw
                logging("Min Voltage Lowered to ${state.minVolt}v", "info")  
           }                             
       } 
		BigDecimal batteryPercentage = 0
        BigDecimal batteryVoltageScaleMin = state.minVolt 
		BigDecimal batteryVoltageScaleMax = 3.00  // 3.2 new battery
		batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
        powerLast = device.currentValue("battery")
        logging("battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V", "debug")
        if (powerLast != batteryPercentage){
           sendEvent(name: "battery", value:batteryPercentage, unit: "%")
           sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V", descriptionText: "Volts:${batteryVoltage}V MinVolts:${batteryVoltageScaleMin} v${state.version}")    
          logging("Battery:${batteryPercentage}% ${batteryVoltage}V", "info")

          if (batteryVoltageRaw < state.minVolt){state.minVolt = batteryVoltageRaw}  // Record the min volts seen working      
         } // end dupe events detection

        }// end battery report  
         
         
     if (temperatureValue != "0000") {
        // We get false temp readings of 0000 so ignore for now.
        // When getting a false reading bat is not FFFF 
        // This is not true for all sensors. So more work needed.Cannot cast object '38.9840' with class 'java.lang.String' to class 'float' on line 522 (method parse)

        if(!tempAdj){tempAdj = 0}   

        BigDecimal temperatureCelsius = hexToBigDecimal(temperatureValue) / 16
        temperatureF = (temperatureCelsius * 9/5) + 32//      fixed from UK code use F
        temperatureU = temperatureF
        def correctNum = (tempAdj ?: 0.0).toBigDecimal() 
        if (correctNum != 0){ temperatureF = temperatureF + correctNum }
        tempLast = device.currentValue("temperature")
         logging("temperature: Now:${temperatureF}°F Last:${tempLast}°F adjust:${correctNum} [Sensor:${temperatureU}°F ${temperatureCelsius}°C]", "debug")
        if (tempLast != temperatureF){
         logging("temperature: Now:${temperatureF}°F Last:${tempLast}°F adjust:${correctNum} [Sensor:${temperatureU}°F ${temperatureCelsius}°C]", "info")
		 sendEvent(name: "temperature", value: temperatureF, unit: "F", isStateChange: true, descriptionText: "Sensor:${temperatureU} adjust:${correctNum} v${state.version}")
         }// end dupe events detection
        }// end temp   
    }// end FB
 } // end cluster 00F0

 else if (map.clusterId == "00F2") {// Tamper cluster.
      logging("Tamper : Cluster [${map.command} ${receivedData[0]}]", "debug")
      if (map.command == "00" || receivedData[0] == "02") {
           if(tamperIgnore){logging("Tamper : ignored", "debug")}
           else{
            logging("Tamper : Detected", "warn")
			sendEvent(name: "tamper", value: "detected", isStateChange: true, descriptionText: "tamper detected v${state.version}")
            }
        }
		if (map.command == "01" || receivedData[0] == "01") {
			logging("Tamper : Cleared", "info")
			sendEvent(name: "tamper", value: "clear",    isStateChange: true, descriptionText: "tamper clear v${state.version}")
		 }
        }

 else if (map.clusterId == "00F6") {// Join Cluster 0xF6

		
       // Ranging
		if (map.command == "FD") { // LQI
			def lqiHex = "na"
			lqiHex = receivedData[0]
            int lqiRanging = 0
			lqiRanging = zigbee.convertHexToInt(lqiHex)
            lqiLast = device.currentValue("lqi")
            if(lqiRanging != lqiLast){
			 sendEvent(name: "lqi", value: lqiRanging)
			 logging("LQI: ${lqiRanging}", "info")
            }   
		if (receivedData[1] == "77" || receivedData[1] == "FF") { // Ranging running in a loop
			state.rangingPulses++
            logging("Ranging ${state.rangingPulses} ", "debug")    
 			 if (state.rangingPulses > 6) {
             normalMode()
             return   
             }  
        } else if (receivedData[1] == "00") { // Ranging during a reboot
				// when the device reboots.(keypad) Must answer
				logging("reboot ranging report received", "info")
				refresh()
                return
			} 
// End ranging block 
            
            
		} else if (map.command == "FE") {// Hello Response 0xF6 0xFE
          

			def versionInfoHex = receivedData[31..receivedData.size() - 1].join()
			StringBuilder str = new StringBuilder()
			for (int i = 0; i < versionInfoHex.length(); i+=2) {
				str.append((char) Integer.parseInt(versionInfoHex.substring(i, i + 2), 16))
			} 
			String versionInfo = str.toString()
			String[] versionInfoBlocks = versionInfo.split("\\s")
			int versionInfoBlockCount = versionInfoBlocks.size()
			String versionInfoDump = versionInfoBlocks[0..versionInfoBlockCount - 1].toString()

            // taken from iris source code
            mfgIdhex = receivedData[11] + receivedData[10]  
            def mfgIddec = Integer.parseInt(mfgIdhex,16)
            dvcType = receivedData[13] + receivedData[12]
            def appRel = Integer.parseInt(receivedData[14],16)
            def appVer = Integer.parseInt(receivedData[15],16)
            hwVerHex = receivedData[17] + receivedData[16]
            def hwVer = Integer.parseInt(hwVerHex,16)
            
//            appVerHex = receivedData[15]
//            def appVerDec = Integer.parseInt(appVerHex,16)
//            appVer = new Double(appVerDec) /10
//            hwVer = new Double(receivedData[17]) + (new Double(receivedData[16]) / 10)
            String deviceFirmwareDate = versionInfoBlocks[versionInfoBlockCount - 1]
            firmwareVersion = appVer + "." + appRel + "." + hwVer+" " + deviceFirmwareDate//appV.appRel.hwV
            logging("Ident Block: ${versionInfoDump} ${firmwareVersion}", "trace")
    
    state.firmware =  appVer + "." + appRel + "." + hwVer 
			String deviceManufacturer = "IRIS"
			String deviceModel = ""
//known firmware :appV.appRel.hwV- 40.16.256 2012-09-20
           reportFirm = "unknown"
          if(deviceFirmwareDate == "2012-09-20" ){reportFirm = "Ok"}
         if(reportFirm == "unknown"){state.reportToDev="Unknown firmware [${firmwareVersion}] ${deviceFirmwareDate}" }
          else{state.remove("reportToDev")}
			// Sometimes the model name contains spaces.
	if (versionInfoBlockCount == 2) {
	deviceModel = versionInfoBlocks[0]
	} else {
	deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
	}
   state.model = deviceModel
    // Moved to debug because of to many events
            logging("Ident:${deviceModel} Firm:[${firmwareVersion}] ${reportFirm} Driver v${state.version}", "debug")
            if(!state.Config){
            state.Config = true    
			updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
            updateDataValue("model", "DWS800")// DWS901 ?
            updateDataValue("firmware", firmwareVersion)
            updateDataValue("fcc", "WJHWD11")
                }   
		} 

// Standard IRIS USA Cluster detection block v4

   } else if (map.clusterId == "8038") {
    logging("${map.clusterId} Seen before but unknown", "debug")
	} else if (map.clusterId == "8001" ) {
	logging("${map.clusterId} Routing and Neighbour Information", "info")    
	} else if (map.clusterId == "8032" ) {
	logging("${map.clusterId} New join has triggered a routing table reshuffle.", "info")
    } else if (map.clusterId == "0006") {MatchDescriptorRequest()
	} else if (map.clusterId == "0013" ) {
	logging("${map.clusterId} Re-routeing", "warn")

 } else {logging("New unknown Cluster Detected: ${map}", "warn")}// report to dev improved
}
	



void sendZigbeeCommands(List<String> cmds) {
logging("send Zigbee :${cmds}", "trace")
sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}





private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}


void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-v1-contact.jpg' >"

 }

// Logging block  v5
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
        if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
}

void loggingCheck(){ 
// not working fix later
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
