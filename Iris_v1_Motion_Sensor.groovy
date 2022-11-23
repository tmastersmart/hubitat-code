/* Iris v1 Motion Detector driver for hubutat
   USA version only. 



Improved battery and temp reporting no more negatve battery reports.
Low bat value is now set by each device automaticaly. The way IRIS did it


======================================================
v2.4.4 11/23/2022 Maintance release
v2.4.2 11/12/2022 Another bug fix for presence
v2.4.0 11/11/2022 Added Retries to presence. Rewrote logging code.
                  cleaned up parsing code. New firmware detection
v2.3.2 11/06/2022 Trace log Zigbee send was displaying rec
v2.3.1 11/06/2022 Logos added
v2.3.0 10/30/2020 Bug fix in presence not giving warning before timeout
v2.2.5 10/26/2022 Reduced min voltage again to prevent -results
v2.2.4 10/19/2022 force motion ON/OFF from driver page
v2.2.3 10/16/2022 Reduced precision of bat voltage to reduce events .xxx to .xx
v2.2.2 10/10/2022 Changes in bat lower limit and config delay
v2.1  09/21/2022 Ranging adjustments
v2.0  09/19/2022 Rewrote logging routines.
v1.9  09/17/2022 Presence routine rewrote from scratch
v1.7  09/17/2022 New temp adjust code.
                 Randomised each device so they dont all run at the same
                 time on code change and reboot.
v1.6  09/06/2022 Init routine delayed. minor fixes
v1.5  09/05/2022 Temp check done before battery
v1.4  09/04/2022 Bad temp detection
v1.3  09/03/2022 Updated changes from contact code to motion. 
                 schedules are optional and better bat and temp code.
v1.2  08/19/2022 Rewrite of BAT and temp routines for IRIS
v1.0  09/10/2021 Update Inserted into get hub

=======================================================
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



May contain code from the following


http://www.apache.org/licenses/LICENSE-2.0
Iris v2 source code driver
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_AlertMe_MotionSensor_2_3.driver




code includes some routines based on alertme UK code from  

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
Forked from AlertMe Motion Sensor Driver v1.14 (24th January 2021)
https://github.com/birdslikewires/hubitat/blob/master/alertme/drivers/alertme_motion.groovy
*	
 */

def clientVersion() {
    TheVersion="2.4.4"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.helper.HexUtils
metadata {

	definition (name: "Iris v1 Motion Detector Custom", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/Iris_v1_Motion_Sensor.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
		capability "MotionSensor"
		capability "PresenceSensor"
		capability "Refresh"
		capability "Sensor"
		capability "SignalStrength"
		capability "TamperAlert"
		capability "TemperatureMeasurement"

		command "checkPresence"
		command "normalMode"
		command "rangeAndRefresh"
		//command "quietMode"
        command "unschedule"
        command "uninstall"
        command "ClearTamper"
        command "motionON"
        command "motionOFF"


		attribute "batteryVoltage", "string"
		attribute "mode", "string"


		fingerprint profileId: "C216", inClusters: "00F0,00F1,00F2", outClusters: "", manufacturer: "IRIS", model: "MT800", deviceJoinName: "Iris Motion Sensor"

    }

}
// fingerprint model:"MT800", manufacturer:"IRIS", profileId:"C216", endpointId:"02", inClusters:"00F0,00F1,00F2", outClusters:""
//endpointId: 02
//manufacturer: AlertMe
//model: PIR Device

preferences {
	
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true   
    
    input name: "tempAdj",type:"enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8",
    "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8",
    "-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",
    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",
   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: "0",required: true  

}

def installed() {
	// Runs after first pairing. this may never run internal drivers overide pairing.
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
 logging("Uninstalled - States removed you may now switch drivers", "info") , 
    ], 200)      
 
}


def initialize() {
    logging("Initialize", "info")
	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.
	// Reset states...
	state.presenceUpdated = 0
	state.rangingPulses = 0
	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("presenceUpdated")
	// Remove unnecessary device details.
	removeDataValue("application")
    if(!option1){sendEvent(name: "powerSource", value: "battery")}
    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
}


def configure() {
	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.
	// Remove state variables from old versions.
	
	// upgrade to new min values
	if (state.minVoltTest < 2.1 | state.minVoltTest > 2.25 ){ 
		state.minVoltTest= 2.25 
		logging("Min voltage set to ${state.minVoltTest}v Let bat run down to 0 for auto adj to work.", "info")
	}
	
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("batteryOkay")
    state.remove("Config")
    getIcons()
    
	unschedule()
	// Schedule randon ranging in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${6} * * ? *", rangeAndRefresh)	

    // Check presence in hrs
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
    logging("configure", "info")
	
}

def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh()
}




//	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"]),


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
    logging("StartMode : [Ranging]", "info")
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"]) // ranging
	state.rangingPulses = 0
	runIn(6, normalMode)
 
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

       



def ClearTamper (){
        logging("${device} : Tamper : Cleared FORCED", "info")
		sendEvent(name: "tamper", value: "clear")
}
    
    

// sends standard zigbee command
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() || status.isAlarm2Set()) {motionON()}
	else {motionOFF()}
}

private motionON(){
		logging("Motion : Active", "info")
		sendEvent(name: "motion", value: "active", isStateChange: true)
}

private motionOFF(){
		logging("Motion : Inactive", "info")
        sendEvent(name: "motion", value: "inactive", isStateChange: true)
}


def parse(String description) {
	logging("Parse : ${description}", "trace")
    clientVersion()
    loggingCheck()
    state.lastCheckin = now()
    checkPresence()
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
    	batteryVoltage = batteryVoltageRaw.setScale(2, BigDecimal.ROUND_HALF_UP)
        // Auto adjustment like iris hub did it  2.17 is 0 on the test device 
        // what is the lowest voltage this device can work on. 
        if (batteryVoltage < state.minVoltTest){
            if (state.minVoltTest > 2.17){ 
                state.minVoltTest = batteryVoltage
                logging("Min Voltage Lowered to ${state.minVoltTest}v", "info")  
            }                             
        } 
		BigDecimal batteryPercentage = 0
        BigDecimal batteryVoltageScaleMin = state.minVoltTest 
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

          if ( batteryVoltage < state.minVoltTest){state.minVoltTest = batteryVoltage}  // Record the min volts seen working      
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
      logging("${device} : Tamper : Cluster [${map.command} ${receivedData[0]}]", "debug")
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
			 logging("${device} : LQI: ${lqiRanging}", "info")
            }   
		if (receivedData[1] == "77" || receivedData[1] == "FF") { // Ranging running in a loop
			state.rangingPulses++
            logging("Ranging ${state.rangingPulses}", "debug")    
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
            

            String deviceFirmwareDate = versionInfoBlocks[versionInfoBlockCount - 1]
            firmwareVersion = "appV.appRel.hwV-" +appVer + "." + appRel + "." + hwVer+"-date-" + deviceFirmwareDate
            logging("Ident Block: ${versionInfoDump} ${firmwareVersion}", "trace")
    
    state.firmware =  appVer + "." + appRel + "." + hwVer 
			String deviceManufacturer = "IRIS"
			String deviceModel = ""

           reportFirm = "unknown"
          if(deviceFirmware == "2012-02-03" ){reportFirm = "v1 Ok"}
          if(deviceFirmware == "2012-09-20" ){reportFirm = "v2 Ok"}  
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
            updateDataValue("model", "MT800")    
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
    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-v1-motion.jpg' >"

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
