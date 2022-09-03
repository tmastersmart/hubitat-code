/* Iris v1 motion sensor driver for hubitat
   USA version only. 



Improved battery and temp reporting that default drivers dont understand.
No more negatve battery reports. 

Tested on Firmware [2012-09-20]


v1.3  09/03/2022 Updated changes from contact code to motion. 
                 schedules are optional and better bat and temp code.
v1.2  08/19/2022 Rewrite of BAT and temp routines for IRIS
v1.0  09/10/2021 Update Inserted into get hub

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






code includes some routines based on alertme UK code from  
https://github.com/birdslikewires/hubitat
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
    TheVersion="1.3"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.helper.HexUtils
metadata {

	definition (name: "Iris v1 Motion Sensor Custom", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/Iris_v1_Motion_Sensor.groovy") {

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
		command "rangingMode"
		//command "quietMode"
        command "unschedule"
        command "uninstall"
        command "ClearTamper"


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
	
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
    input name: "optionPresent",type: "bool", title: "Presence Detection", description: "Run a presence schedule (save then config)",defaultValue: false,required: true
    input name: "optionRange",  type: "bool", title: "Ranging Report", description: "Run a ranging schedule (save then config)",defaultValue: true,required: true

    input("tempAdj", "number", title: "Temperature Offset:", description: "Adjust the temp by adding or subtraction this amount",defaultValue: 0,required: false)
	input("batAdj",  "number", title: "Battery Offset:", description: "Adjust the Bat% by adding or subtracting this amount in % ",defaultValue: 0,required: false)

}


def installed() {
	// Runs after first pairing. this may never run internal drivers overide pairing.
	logging("${device} : Paired!", "info")
}

def uninstall() {
	unschedule()
	state.remove("rangingPulses")
	state.remove("operatingMode")
	state.remove("batteryOkay")
	state.remove("presenceUpdated")    
	state.remove("version")
	state.remove("battery")


    
removeDataValue("battery")
removeDataValue("battertState")
removeDataValue("batteryVoltage")
removeDataValue("batteryVoltageWithUnit")
removeDataValue("batteryWithUnit")     
removeDataValue("contact")
removeDataValue("lqi")
removeDataValue("operation")
removeDataValue("presence")
removeDataValue("tamper")    
removeDataValue("temperature")
removeDataValue("temperatureWithUnit")    
logging("${device} : Uninstalled", "info")   
}

def initialize() {
logging("${device} : Initialised", "info")

	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.

	// Reset states...

	state.batteryOkay = true
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0


	// Remove disused state variables from earlier versions.
	state.remove("batteryInstalled")
	state.remove("firmwareVersion")	
	state.remove("uptime")
	state.remove("uptimeReceived")
	state.remove("presentAt")
	state.remove("relayClosed")
	state.remove("rssi")
	state.remove("supplyPresent")

	// Remove unnecessary device details.
	removeDataValue("application")

	
	// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!

    clientVersion()
    randomSixty = Math.abs(new Random().nextInt() % 60)
    runIn(randomSixty,refresh) // Refresh in random time
}


def configure() {

	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.

	initialize()
	unschedule()

	// Default logging preferences.
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])

	// Schedule our ranging report.6 hours or every 1 hour for outlets.
    if(optionRange){
	int checkEveryHours = 12				
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)	// At X seconds past X minute, every checkEveryHours hours, starting at Y hour.
    }
	// Schedule the presence check. 6 minutes or every 1 minute for key fobs.
    if (optionPresent){
	int checkEveryMinutes = 20							
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)									// At X seconds past the minute, every checkEveryMinutes minutes.
    }
	// Run a ranging report and then switch to normal operating mode.
	rangingMode()
	runIn(12,normalMode)
	
}


def updated() {

	// Runs whenever preferences are saved.
clientVersion()
	loggingStatus()
	runIn(3600,debugLogOff)
	runIn(1800,traceLogOff)
	refresh()

}


void loggingStatus() {

	log.info "${device} : Logging : ${infoLogging == true}"
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

// Expermental  will likely retamper..
def ClearTamper (){
        logging("${device} : Tamper : Cleared FORCED", "info")
		sendEvent(name: "tamper", value: "clear")
}


void reportToDev(map) {
	String[] receivedData = map.data
	def receivedDataCount = ""
	if (receivedData != null) {
		receivedDataCount = "${receivedData.length} bits of "
	}
	logging("${device} : New unknown Cluster Detected: Report to DEV clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command} with value:${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
}



def normalMode() {

	// This is the standard running mode.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"])
	state.operatingMode = "normal"
	refresh()
	sendEvent(name: "operation", value: "normal")
	logging("${device} : Mode : Normal", "info")

}


def rangingMode() {

	// Ranging mode double-flashes (good signal) or triple-flashes (poor signal) the indicator
	// while reporting LQI values. It's also a handy means of identifying or pinging a device.

	// Don't set state.operatingMode here! Ranging is a temporary state only.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"])
	sendEvent(name: "operation", value: "ranging")
	logging("${device} : Mode : Ranging", "info")

	// Ranging will be disabled after a maximum of 30 pulses.
	state.rangingPulses = 0

}


def quietMode() {

	// Turns off all reporting except for a ranging message every 2 minutes.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])
	state.operatingMode = "quiet"

	// We don't receive any of these in quiet mode, so reset them.
	sendEvent(name: "battery",value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")
	sendEvent(name: "temperature", value: 0, unit: "F", isStateChange: false)


	logging("${device} : Mode : Quiet", "info")

	refresh()

}


void refresh() {
	logging("${device} : Refreshing", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])	   // version information request
}


def rangeAndRefresh() {
	// This toggles ranging mode to update the device's LQI value.
   int returnToModeSeconds = 6			// We use 3 seconds for outlets, 6 seconds for battery devices, which respond a little more slowly.
	rangingMode()
	runIn(returnToModeSeconds, "${state.operatingMode}Mode")
}


def updatePresence() {
	long millisNow = new Date().time
	state.presenceUpdated = millisNow
}


def checkPresence() {

	// Check how long ago the presence state was updated.

	// AlertMe devices check in with some sort of report at least every 2 minutes (every minute for outlets).

	// It would be suspicious if nothing was received after 4 minutes, but this check runs every 6 minutes
	// by default (every minute for key fobs) so we don't exaggerate a wayward transmission or two.

	presenceTimeoutMinutes = 4
	uptimeAllowanceMinutes = 5

	if (state.presenceUpdated > 0 && state.batteryOkay == true) {

		long millisNow = new Date().time
		long millisElapsed = millisNow - state.presenceUpdated
		long presenceTimeoutMillis = presenceTimeoutMinutes * 60000
		BigInteger secondsElapsed = BigDecimal.valueOf(millisElapsed / 1000)
		BigInteger hubUptime = location.hub.uptime

		if (millisElapsed > presenceTimeoutMillis) {

			if (hubUptime > uptimeAllowanceMinutes * 60) {

				sendEvent(name: "presence", value: "not present")
				logging("${device} : Presence : Not Present! Last report received ${secondsElapsed} seconds ago.", "warn")

			} else {

				logging("${device} : Presence : Ignoring overdue presence reports for ${uptimeAllowanceMinutes} minutes. The hub was rebooted ${hubUptime} seconds ago.", "info")

			}

		} else {

			sendEvent(name: "presence", value: "present")
			logging("${device} : Presence : Last presence report ${secondsElapsed} seconds ago.", "debug")

		}

		logging("${device} : checkPresence() : ${millisNow} - ${state.presenceUpdated} = ${millisElapsed} (Threshold: ${presenceTimeoutMillis} ms)", "trace")

	} else if (state.presenceUpdated > 0 && state.batteryOkay == false) {

		sendEvent(name: "presence", value: "not present")
		logging("${device} : Presence : Battery too low! Reporting not present as this device will no longer be reliable.", "warn")

	} else {

		logging("${device} : Presence : Waiting for first presence report.", "warn")

	}

}


def parse(String description) {
	logging("${device} : Parse : ${description}", "trace")
	state.batteryOkay == true ?	sendEvent(name: "presence", value: "present") : sendEvent(name: "presence", value: "not present")
	updatePresence()
    // Device contacts are zigbee cluster compatable
	if (description.startsWith("zone status")) {
		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
		processStatus(zoneStatus)
	}  else if (description?.startsWith('enroll request')) {
			logging("${device} : Responding to Enroll Request. Likely Battery Change", "info")
			sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x0500 {11 80 00 00 05} {0xC216}"])    
    }else {
		Map descriptionMap = zigbee.parseDescriptionAsMap(description)
	    if (descriptionMap) {processMap(descriptionMap)}
        else{
        // we should never get here reportToDev is in processMap above
            logging("${device} : Error ${description} ${descriptionMap}", "debug")    
        }
	}
}

// sends standard zigbee command
def processStatus(ZoneStatus status) {
    logging("${device} : ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() || status.isAlarm2Set()) {
		logging("${device} : Motion : Active", "info")
		sendEvent(name: "motion", value: "active", isStateChange: true)
	} else {
		logging("${device} : Motion : Inactive", "info")
        sendEvent(name: "motion", value: "inactive", isStateChange: true)
	}
}


def processMap(Map map) {
	// AlertMe values are always sent in a data element.
	String[] receivedData = map.data
    logging("${device} : processMap clusterId:${map.clusterId} command:${map.command} ${receivedData} ", "debug")

	if (map.clusterId == "00F0") {
		// Device status cluster.
		// Report the battery voltage and calculated percentage.
		def batteryVoltageHex = "undefined"
		BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
		if (!batteryVoltageHex == "FFFF") {
     		batteryVoltageRaw = zigbee.convertHexToInt(batteryVoltageHex) / 1000
    		batteryVoltage = batteryVoltageRaw.setScale(3, BigDecimal.ROUND_HALF_UP)

        if (state.minVoltTest < 2.00){ 
            state.minVoltTest= 2.70// 
            logging("${device} : Min Voltage Reset to ${state.minVoltTest}v", "info") 
        }
        if (batteryVoltage < state.minVoltTest){
            state.minVoltTest = batteryVoltage
            logging("${device} : Min Voltage Lowered to ${state.minVoltTest}v", "info")  
        } 
 
		BigDecimal batteryPercentage = 0
        BigDecimal batteryVoltageScaleMin = state.minVoltTest // Auto adjustment
		BigDecimal batteryVoltageScaleMax = 3.00 // can be 3.048 

      
    	state.batteryOkay = true
			batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
            

            powerLast = device.currentValue("battery")
            logging("${device} : battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V", "debug")
            if (powerLast != batteryPercentage){
             sendEvent(name: "battery", value:batteryPercentage, unit: "%")
             sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V", descriptionText: "Volts:${batteryVoltage}V MinVolts:${batteryVoltageScaleMin} v${state.version}")    
             if (batteryPercentage > 10) {logging("${device} : Battery:${batteryPercentage}% ${batteryVoltage}V", "info")}
             else { logging("${device} : Battery :LOW ${batteryPercentage}% ${batteryVoltage}V", "info")}
		 
        // Record the min volts seen working 
        if ( batteryVoltage < state.minVoltTest){state.minVoltTest = batteryVoltage}       

      } // end if changes %
   }// end of FFFF detection

// Report the temperature 
		def temperatureValue = "undefined"
		temperatureValue = receivedData[7..8].reverse().join()
		BigDecimal temperatureCelsius = hexToBigDecimal(temperatureValue) / 16
        temperatureF = (temperatureCelsius * 9/5) + 32//      fixed from UK code use F
        if (!tempAdj){tempAdj = 0}
        if (tempAdj > 0){temperatureF= temperatureF + tempAdj }
        if (tempAdj < 0){temperatureF= temperatureF - tempAdj }    
        logging("${device} : Temp:${temperatureF}F ${temperatureCelsius}C ${temperatureValue}", "debug")
        if(temperatureValue == "0000"){logging("${device} : Temp: 0000 Bad sensor?", "debug")}
      
       // bad sensor may report 0000 32f It will get auto ignored because all events will be the same.
        tempLast = device.currentValue("temperature")
        if (tempLast != temperatureF){
        logging("${device} : temperature: now:${temperatureF} Last:${tempLast}", "info")
		sendEvent(name: "temperature", value: temperatureF, unit: "F")
        }  


// Tamper cluster.
	} else if (map.clusterId == "00F2") {
        if(tamperIgnore != "true"){
		if (map.command == "00") {
			if (receivedData[0] == "02") {
                
				logging("${device} : Tamper : Detected", "warn")
				sendEvent(name: "tamper", value: "detected")
                }    
			
		} else if (map.command == "01") {
			if (receivedData[0] == "01") {
				logging("${device} : Tamper : Cleared", "info")
				sendEvent(name: "tamper", value: "clear")
			} 
        }
    }

	} else if (map.clusterId == "00F6") {

		// Discovery cluster. 

		if (map.command == "FD") {

			// Ranging is our jam, Hubitat deals with joining on our behalf.

			def lqiRangingHex = "undefined"
			int lqiRanging = 0
			lqiRangingHex = receivedData[0]
			lqiRanging = zigbee.convertHexToInt(lqiRangingHex)
			sendEvent(name: "lqi", value: lqiRanging)
			logging("${device} : lqiRanging : ${lqiRanging}", "debug")

			if (receivedData[1] == "77") {

				// This is ranging mode, which must be temporary. Make sure we come out of it.
				state.rangingPulses++
				if (state.rangingPulses > 30) {
					"${state.operatingMode}Mode"()
				}

			} else if (receivedData[1] == "FF") {

				// This is the ranging report received every 30 seconds while in quiet mode.
				logging("${device} : quiet ranging report received", "debug")

			} else if (receivedData[1] == "00") {

				// This is the ranging report received when the device reboots.
				// After rebooting a refresh is required to bring back remote control.
				logging("${device} : reboot ranging report received", "debug")
				refresh()

			} else {

				// Something to do with ranging we don't know about!
				reportToDev(map)

			} 

		} else if (map.command == "FE") {

			// Device version response.

			def versionInfoHex = receivedData[31..receivedData.size() - 1].join()

			StringBuilder str = new StringBuilder()
			for (int i = 0; i < versionInfoHex.length(); i+=2) {
				str.append((char) Integer.parseInt(versionInfoHex.substring(i, i + 2), 16))
			} 

			String versionInfo = str.toString()
			String[] versionInfoBlocks = versionInfo.split("\\s")
			int versionInfoBlockCount = versionInfoBlocks.size()
			String versionInfoDump = versionInfoBlocks[0..versionInfoBlockCount - 1].toString()

			logging("${device} : ident: ${versionInfoDump}", "trace")

			String deviceManufacturer = "IRIS"
			String deviceModel = ""
            String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
           reportFirm = "unknown"
          if(deviceFirmware == "2012-09-20" ){reportFirm = "Ok"}

          if(reportFirm == "unknown"){state.reportToDev="Report Unknown firmware [${deviceFirmware}] " }
          else{state.remove("reportToDev")}
			// Sometimes the model name contains spaces.
	if (versionInfoBlockCount == 2) {
	deviceModel = versionInfoBlocks[0]
	} else {
	deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
	}
            logging("${device} : ${deviceModel} Ident: Firm:[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "debug")
			updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
            updateDataValue("model", "MT800")
            updateDataValue("firmware", deviceFirmware)
		} else {reportToDev(map)}
        

	} else if (map.clusterId == "8001" || map.clusterId == "8038") {

		// These clusters are sometimes received from the SPG100 and I have no idea why.
		//   8001 arrives with 12 bytes of data
		//   8038 arrives with 27 bytes of data
		logging("${device} : Skipping data received on cluserId ${map.clusterId}.", "debug")

	} else if (map.clusterId == "8032" ) {
		//   8032 arrives with 80 bytes of data, probably routing and neighbour information.
		logging("${device} : New join has triggered a routing table reshuffle.", "info")
} else if (map.clusterId == "0013" ) {
        // clusterId: 0013, command: 00 [8E, D1, 9F, 2D, 40, BC, 03, 00, 6F, 0D, 00, 80]        
        // NEW seen 2022 during power falure we see this as repeators go down. 
		logging("${device} : Re-routeing around dead devices. (power Falure?)", "warn")
    
    
	} else {

		// Not a clue what we've received.
		reportToDev(map)

	}

	return null

}


void sendZigbeeCommands(List<String> cmds) {

	// All hub commands go through here for immediate transmission and to avoid some method() weirdness.

    logging("${device} : sendZigbeeCommands received : ${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))

}


private String[] millisToDhms(int millisToParse) {

	long secondsToParse = millisToParse / 1000

	def dhms = []
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 60)
	secondsToParse = secondsToParse / 60
	dhms.add(secondsToParse % 24)
	secondsToParse = secondsToParse / 24
	dhms.add(secondsToParse % 365)
	return dhms

}


private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
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
