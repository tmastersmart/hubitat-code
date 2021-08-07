/*Iris v1 AlertMe Repeator Zigbe
https://fcc.report/FCC-ID/WJHRP11/

// Item #388560 Model #REP901 REP800 Iris Range Extender FCC ID WJHRP11 Zigbee/Zwave

    08/06/2021 v1.5 Remove power and uptime
    05/16/2021 v1.4  
    05/11/2021 v1.3  Power stats testing
    05/08/2021 v1.2
    04/04/2021 v1.1  
    04/11/2021 v1.0
 https://github.com/tmastersmart/hubitat-code/blob/main/iris_alertme_repeator_zigbe.groovy
 https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy



You have to pair this as zigbee and then zwave.
Hold down button while plugging in then press about 10 times to reset. Once flashing pair zigbee
Then pair zwave exact zwave process is not clear try pairing before zigbee or after.
Zwave driver is required for zwave part.


fingerprint model:"RepeaterPlug", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00F3", outClusters:""

Tested on  REP800 uses Firmware : 2013-09-26 

REP901 is the new version Need firmware versions.







 * using modified UK plug code
 * Forked from https://raw.githubusercontent.com/birdslikewires/hubitat/master/alertme/drivers/alertme_smartplug.groovy
 * name: "AlertMe Smart Plug" 
 * namespace: "BirdsLikeWires", 
 * author: "Andrew Davison", 
 */


metadata {

	definition (name: "Iris AlertMe Repeator Zigbe", namespace: "tmastersmart", author: "tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
//		capability "PowerSource"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"

		command "normalMode"
		command "rangingMode"
		command "quietMode"

		attribute "batteryState", "string"
		attribute "batteryVoltage", "string"
//attribute "batteryVoltageWithUnit", "string"
//		attribute "batteryWithUnit", "string"
		attribute "mode", "string"
//		attribute "stateMismatch", "boolean"
//		attribute "temperature", "string"
//		attribute "uptime", "string"
//		attribute "uptimeReadable", "string"

		fingerprint profileId: "C216", inClusters: "00F0,00F3", outClusters: "", manufacturer: "Iris/AlertMe", model: "RepeaterPlug", deviceJoinName: "Iris AlertMe Repeater Plug"
		
	}

}
// Item #388560 Model #REP901
//manufacturer: AlertMe
//model: RepeaterPlug
//profileId: C216
//inClusters: 00F0,00F3
//firmware: 2013-09-26

preferences {
	
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
	
}


def installed() {
	// Runs after first pairing.
	logging("${device} : Paired!", "info")
}


def initialize() {

	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.

	// Reset states...

	state.batteryOkay = true
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0

	// ...but don't arbitrarily reset the state of the device's main functions or tamper status.

	sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryState", value: "unknown", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
// ndEvent(name: "batteryVoltageWithUnit", value: "unknown", isStateChange: false)
//	sendEvent(name: "batteryWithUnit", value: "unknown", isStateChange: false)

	sendEvent(name: "lqi", value: 0)
	sendEvent(name: "operation", value: "unknown", isStateChange: false)

//	sendEvent(name: "powerSource", value: "unknown", isStateChange: false)

	sendEvent(name: "presence", value: "not present")
//	sendEvent(name: "stateMismatch", value: true, isStateChange: false)



	// Remove old settings. Upgrade for debugging
	state.remove("switch")
    state.remove("tamper")
	state.remove("temperature")	
	state.remove("temperatureWithUnit")
	state.remove("uptimeReceived")
	state.remove("presentAt")
	state.remove("relayClosed")
	state.remove("batteryVoltageWithUnit")
	state.remove("supplyPresent")
    state.remove("batteryWithUnit")
   

	// Remove unnecessary device details.
	removeDataValue("application")

	// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!
	randomSixty = Math.abs(new Random().nextInt() % 60)
	runIn(randomSixty,refresh)

	// Initialisation complete.
	logging("${device} : Initialised", "info")

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

	// Schedule our ranging report.
	int checkEveryHours = 1																						// Request a ranging report and refresh every 6 hours or every 1 hour for outlets.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)	// At X seconds past X minute, every checkEveryHours hours, starting at Y hour.

	// Schedule the presence check.
	int checkEveryMinutes = 6																					// Check presence timestamp every 6 minutes or every 1 minute for key fobs.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)									// At X seconds past the minute, every checkEveryMinutes minutes.

	// Configuration complete.
	logging("${device} : Configured", "info")

	// Run a ranging report and then switch to normal operating mode.
	rangingMode()
	runIn(12,normalMode)
	
}


def updated() {

	// Runs whenever preferences are saved.

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


void reportToDev(map) {

	String[] receivedData = map.data

	def receivedDataCount = ""
	if (receivedData != null) {
		receivedDataCount = "${receivedData.length} bits of "
	}

//	logging("${device} : UNKNOWN DATA!", "warn")
	logging("${device} : Received Unknown: cluster: ${map.cluster}, clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
//	logging("${device} : Splurge! : ${map}", "trace")

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


def lockedMode() {

	// Locked mode is not as useful as it might first appear. This disables the local power button on
	// the outlet. However, this can be reset by rebooting the outlet by holding that same power
	// button for ten seconds. Or you could just turn off the supply, of course.

	// To complicate matters this mode cannot be disabled remotely, so far as I can tell.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 02 01} {0xC216}"])
	refresh()
	state.operatingMode = "locked"
	sendEvent(name: "operation", value: "locked")

	logging("${device} : Mode : Locked", "info")

}


def quietMode() {

	// Turns off all reporting except for a ranging message every 2 minutes.

	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])
	state.operatingMode = "quiet"

	// We don't receive any of these in quiet mode, so reset them.
	sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
//	sendEvent(name: "batteryVoltageWithUnit", value: ".", isStateChange: false)
//	sendEvent(name: "batteryWithUnit", value: ".", isStateChange: false)
//	sendEvent(name: "energy", value: 0, unit: "kWh", isStateChange: false)
//	sendEvent(name: "energyWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")
//	sendEvent(name: "power", value: 0, unit: "W", isStateChange: false)
//	sendEvent(name: "powerWithUnit", value: "unknown", isStateChange: false)
//	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
//	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)
//	sendEvent(name: "temperature", value: 0, unit: "C", isStateChange: false)
//	sendEvent(name: "temperatureWithUnit", value: "unknown", isStateChange: false)

	logging("${device} : Mode : Quiet", "info")

	refresh()

}





void refresh() {

	// The Smart Plug becomes remote controllable after joining once it has received confirmation of the power control operating mode.
	// It also expects the Hub to check in with this occasionally, otherwise remote control is eventually dropped.
	// Whenever a refresh happens (which is at least hourly using rangeAndRefresh() for outlets) we send the nudge.

	logging("${device} : Refreshing", "info")

	def cmds = new ArrayList<String>()
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}")    // version information request
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 01 01} {0xC216}")    // power control operating mode nudge
	sendZigbeeCommands(cmds)

}


def rangeAndRefresh() {

	// This toggles ranging mode to update the device's LQI value.

	int returnToModeSeconds = 3			// We use 3 seconds for outlets, 6 seconds for battery devices, which respond a little more slowly.

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

	if (state.presenceUpdated > 0) {

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

	} else {

		logging("${device} : Presence : Waiting for first presence report.", "warn")

	}

}


def parse(String description) {

	// Primary parse routine.

	logging("${device} : Parse : $description", "debug")

	sendEvent(name: "presence", value: "present")
	updatePresence()

	Map descriptionMap = zigbee.parseDescriptionAsMap(description)

	if (descriptionMap) {

		processMap(descriptionMap)

	} else {
		
		logging("${device} : Parse : Failed to parse received data. Please report these messages to the developer.", "warn")
		logging("${device} : Splurge! : ${description}", "warn")

	}

}



def processMap(Map map) {
	// AlertMe values are always sent in a data element.
	String[] receivedData = map.data
 
//    logging("${device} : debug  Cluster:${map.clusterId}   State:${map.command}", "trace")
    
    
	if (map.clusterId == "00EE") {
// nothing ever gets sent from this cluster             
			reportToDev(map)
	} else if (map.clusterId == "00EF") {
// Power and energy messages none on this device.
			reportToDev(map)

    } else if (map.clusterId == "00F0") {

//    logging("${device} : debug  Cluster:${map.clusterId}   State:${map.command}", "trace")
//        if (map.command == "FB") {debug  Cluster:00F0   State:FB
        

//Parse : catchall: C216 00F0 02 02 0040 00 D75E 01 00 0000 FB 01 1D4EB10A16930C4277CEFF0100     

// Report the battery voltage and calculated percentage.
//        def mainsTest = "undefined"
//        mainsTest= receivedData[10]
//        logging("${device} : test is this mains bat : ${mainsTest}", "trace")
//        if (mainsTest == "FF"){
//       		logging("${device} : Power On Mains :${mainsTest}", "info")
//				sendEvent(name: "stateMismatch", value: false)
//				sendEvent(name: "powerSource", value: "mains")
//				state.supplyPresent = true
//       }

        
        def batteryVoltageHex = "undefined"
		BigDecimal batteryVoltage = 0

		batteryVoltageHex = receivedData[5..6].reverse().join()
		logging("${device} : batteryVoltageHex byte flipped : ${batteryVoltageHex}", "trace")

		if (batteryVoltageHex == "FFFF") {
			// Occasionally a weird battery reading can be received. Ignore it.
			logging("${device} : batteryVoltageHex skipping anomolous reading : ${batteryVoltageHex}", "debug")
			return
		}

		batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
		//logging("${device} : batteryVoltage sensor value : ${batteryVoltage}", "debug")


		batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)

		logging("${device} : batteryVoltage : ${batteryVoltage}", "debug")
		sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
//		sendEvent(name: "batteryVoltageWithUnit", value: "${batteryVoltage} V")

		BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 2.72// 3v would be 1 volt per cell
		BigDecimal batteryVoltageScaleMax = 4.15

		if (batteryVoltage >= batteryVoltageScaleMin && batteryVoltage <= 4.40) {

			// A good three-cell 3.6 V NiMH battery will sit between 4.10 V and 4.25 V. 
            // The above may be true but the repeator works on bat at 3v. USA batteries on these
            // units are old and no reasion to replace them. Lowering min state
            // More testing is need to see what state it stops working at.

			state.batteryOkay = true

			batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage 
            
			if (batteryPercentage > 10) {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "info")
			} else {
				logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			}

			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
//			sendEvent(name: "batteryWithUnit", value:"${batteryPercentage} %")

			if (batteryVoltage > batteryVoltageScaleMax) {
				!state.supplyPresent ?: sendEvent(name: "batteryState", value: "charged")
			} else {
				!state.supplyPresent ?: sendEvent(name: "batteryState", value: "charging")
			}

		} else if (batteryVoltage < batteryVoltageScaleMin) {

			// Very low voltages indicate an exhausted battery which requires replacement.

			state.batteryOkay = false

			batteryPercentage = 0

//			logging("${device} : Battery : Exhausted battery.", "warn")   To many warnings
			logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
//			sendEvent(name: "batteryWithUnit", value:"${batteryPercentage} %")
			sendEvent(name: "batteryState", value: "exhausted")

		} else {

			// If the charge circuitry is reporting greater than 4.5 V then the battery is either missing or faulty.

			state.batteryOkay = false

			batteryPercentage = 0

			logging("${device} : Battery : Exhausted battery requires replacement.", "warn")
			logging("${device} : Battery : $batteryPercentage% ($batteryVoltage V)", "warn")
			sendEvent(name: "battery", value:batteryPercentage, unit: "%")
//			sendEvent(name: "batteryWithUnit", value:"${batteryPercentage} %")
			sendEvent(name: "batteryState", value: "fault")

		}

		// Report the temperature in celsius. No sensor 
   
    
    
//Pressing button on ac gives
//00C0, 0A [21, 00, 30, 00]
//00C0, 0A [20, 00, 0B, 43, 46, 1A, EA]
//00C0, 0A [23, 00, 30, 02]
//00F3, 01 [00, 01, 4A, 72, 00, 00]
//00F3, 00 [00, 02, 7F, 73, 00, 00]

//pressing button on bat gives
//
// 00C0, 0A [21, 00, 30, 00]
// 00C0, 0A [20, 00, 0B, 43, 46, 1A, EA]
// 00C0, 0A [23, 00, 30, 02]
// 00F3, 01 [00, 01, 37, A6, 00, 00]
// 00F3, 00 [00, 02, ED, A6, 00, 00]    
 
      } else if (map.clusterId == "0006") {
        logging("${device} : Not joined - in process ${map.clusterId} MAP:${map.data}", "warn")

      } else if (map.clusterId == "0013") {
        logging("${device} : Not joined - in process ${map.clusterId} MAP:${map.data}", "warn")
    
        
    } else if (map.clusterId == "00F3") {
        logging("${device} : Button Pressed Cluster:${map.clusterId} MAP:${map.data}", "warn")

    } else if (map.clusterId == "00C0") {
   
        logging("${device} : Button Pressed Cluster:${map.clusterId} MAP:${map.data}", "warn")
  
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

			logging("${device} : device version received in ${versionInfoBlockCount} blocks : ${versionInfoDump}", "debug")

			String deviceManufacturer = "Iris/AlertMe"
			String deviceModel = "" // will say RepeaterPlug
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {
				deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
			}

// Firmware : 09-26-2013 Model  : RepeaterPlug manufacturer Iris/AlertMe
            deviceModel ="${deviceModel} REP901?"// Item #388560 Model #REP901 need firmware date on REP901
            
            if (deviceFirmware == "2013-09-26") {
                deviceFirmware = "09-26-2013"
                deviceModel = "REP800"
            }// REP800 uses Firmware : 2013-09-26  Old version
            
            
            logging("${device}: Firmware: ${deviceFirmware} Model: ${deviceModel} manufacturer: ${deviceManufacturer}", "info")

			updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("model", deviceModel)
			updateDataValue("firmware", deviceFirmware)

		} else {

			// Not a clue what we've received.
			reportToDev(map)

		}

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


private String[] millisToDhms(BigInteger millisToParse) {

	BigInteger secondsToParse = millisToParse / 1000

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
