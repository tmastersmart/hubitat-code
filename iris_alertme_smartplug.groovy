/* Iris AlertMe Smart Plug 
USA version  

https://github.com/tmastersmart/hubitat-code/edit/main/iris_alertme_smartplug.groovy
https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_smartplug.groovy

 * 05/05/2021 v1.3 Power recovered detection adjusted
 * 05/04/2021 v1.2   
 * 04/11/2021 v1
 *fingerprint model:"SmartPlug2.5", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00EF,00EE", outClusters:""

 tested on Iris Plug with Firmware : 2013-09-26


 * orginal by 
 * name: "AlertMe Smart Plug" 
 * namespace: "BirdsLikeWires", 
 * author: "Andrew Davison", 
 * importUrl: "https://raw.githubusercontent.com/birdslikewires/hubitat/master/alertme/drivers/alertme_smartplug.groovy"
 *
 * 
 *  AlertMe Smart Plug Driver v1.39 (24th January 2021)
 *	
 */


metadata {
	definition (name: "Iris AlertMe Smart Plug", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_smartplug.groovy") {


		capability "Actuator"
		capability "Configuration"
		capability "EnergyMeter"
		capability "Initialize"
		capability "Outlet"
		capability "PowerMeter"
		capability "PowerSource"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"
		capability "Switch"
//		capability "TamperAlert"
//		capability "TemperatureMeasurement"

		//command "lockedMode"
		command "normalMode"
		command "rangingMode"
		command "quietMode"

		//attribute "batteryState", "string"
		//attribute "batteryVoltage", "string"
		//attribute "batteryVoltageWithUnit", "string"
		//attribute "batteryWithUnit", "string"
		attribute "energyWithUnit", "string"
		attribute "mode", "string"
		attribute "powerWithUnit", "string"
		attribute "stateMismatch", "boolean"
//		attribute "temperatureWithUnit", "string"
		attribute "uptime", "string"
		attribute "uptimeReadable", "string"

		fingerprint profileId: "C216", inClusters: "00F0,00EF,00EE", outClusters: "", manufacturer: "AlertMe", model: "SmartPlug2.5", deviceJoinName: "Iris SmartPlug v2.5"
		
	}

}
//*fingerprint model:"SmartPlug2.5", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00EF,00EE", outClusters:""



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

	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0


	sendEvent(name: "energy", value: 0, unit: "kWh", isStateChange: false)
	sendEvent(name: "energyWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "lqi", value: 0)
	sendEvent(name: "operation", value: "unknown", isStateChange: false)
	sendEvent(name: "power", value: 0, unit: "W", isStateChange: false)
	sendEvent(name: "powerSource", value: "unknown", isStateChange: false)
	sendEvent(name: "powerWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "presence", value: "not present")
	sendEvent(name: "stateMismatch", value: true, isStateChange: false)
	sendEvent(name: "switch", value: "unknown")
//	sendEvent(name: "temperature", value: 0, unit: "F", isStateChange: false)
//	sendEvent(name: "temperatureWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)

	// Remove disused state variables from earlier versions.
	state.remove("temperature")
	state.remove("temperatureWithUnit")	
	state.remove("battery")
	state.remove("batteryState")
	state.remove("batteryVoltage")
	state.remove("batteryVoltageWithUnit")
	state.remove("batteryWithUnit")
	state.remove("supplyPresent")

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
// Removed dont need in the extra warn in the logs
//	logging("${device} : UNKNOWN DATA! Please report these messages to the developer.", "warn")
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

	sendEvent(name: "energy", value: 0, unit: "kWh", isStateChange: false)
	sendEvent(name: "energyWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")
	sendEvent(name: "power", value: 0, unit: "W", isStateChange: false)
	sendEvent(name: "powerWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)
//	sendEvent(name: "temperature", value: 0, unit: "F", isStateChange: false)
//	sendEvent(name: "temperatureWithUnit", value: "unknown", isStateChange: false)

	logging("${device} : Mode : Quiet", "info")

	refresh()

}


def off() {

	// The off command is custom to AlertMe equipment, so has to be constructed.
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 00 01} {0xC216}"])

}


def on() {

	// The on command is custom to AlertMe equipment, so has to be constructed.
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 01 01} {0xC216}"])

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

	logging("${device} : Parse : $description", "trace")

	sendEvent(name: "presence", value: "present")
	updatePresence()

	Map descriptionMap = zigbee.parseDescriptionAsMap(description)

	if (descriptionMap) {

		processMap(descriptionMap)

	} else {
		
		logging("${device} : Parse : Failed to parse received data.", "warn")
		logging("${device} : Splurge! : ${description}", "warn")

	}

}


def processMap(Map map) {

	logging("${device} : processMap() : ${map}", "trace")

	// AlertMe values are always sent in a data element.
	String[] receivedData = map.data

	if (map.clusterId == "00EE") {

		// Relay actuation and power state messages.

		if (map.command == "80") {

			// Power States   USA version

			def powerStateHex = "undefined"
			powerStateHex = receivedData[0]


            // Supply : Power has been restored code: 0E     Relay is off
            
            
            
            // old chart
			//   00 00 - Cold mains power on with relay off (only occurs when battery dead or after reset)
			//   01 01 - Cold mains power on with relay on (only occurs when battery dead or after reset)
			//   04 00 - Mains power returns with relay off (only follows a 00 00)
			//   05 01 - Mains power returns with relay on (only follows a 01 01)
			//   06 00 - Mains power on and relay off (normal actuation)
			//   07 01 - Mains power on and relay on (normal actuation)

			 if (powerStateHex == "0E" ) {

				state.supplyPresent ?: logging("${device} : Supply : Power has been restored code: ${powerStateHex}", "info")
				sendEvent(name: "stateMismatch", value: false)
				sendEvent(name: "powerSource", value: "mains")
				state.supplyPresent = true

			} else {

				// Supply returned!

                 logging("${device} : Supply : Power has been restored code: ${powerStateHex}", "warn")

				sendEvent(name: "stateMismatch", value: false)
				sendEvent(name: "powerSource", value: "mains")
				state.supplyPresent = true

			}

			// Relay States

			def switchStateHex = "undefined"
			switchStateHex = receivedData[1]

			if (switchStateHex == "01") {

				state.relayClosed = true
				sendEvent(name: "switch", value: "on")
				logging("${device} : Switch : On", "info")

			} else {

				state.relayClosed = false
				sendEvent(name: "switch", value: "off")
				logging("${device} : Switch : Off", "info")

			}

		} else {

			reportToDev(map)

		}

	} else if (map.clusterId == "00EF") {

		// Power and energy messages.

		if (map.command == "81") {

			// Power Reading

			def powerValueHex = "undefined"
			int powerValue = 0

			// These power readings are so frequent that we only log them in debug or trace.
			powerValueHex = receivedData[0..1].reverse().join()
			logging("${device} : power byte flipped : ${powerValueHex}", "trace")
			powerValue = zigbee.convertHexToInt(powerValueHex)
			logging("${device} : power sensor reports : ${powerValue}", "debug")

			sendEvent(name: "power", value: powerValue, unit: "W")
			sendEvent(name: "powerWithUnit", value: "${powerValue} W")

		} else if (map.command == "82") {

			// Command 82 returns energy summary in watt-hours with an uptime counter.

			// Energy

			String energyValueHex = "undefined"
			energyValueHex = receivedData[0..3].reverse().join()
			logging("${device} : energy byte flipped : ${energyValueHex}", "trace")

			BigInteger energyValue = new BigInteger(energyValueHex, 16)
			logging("${device} : energy counter reports : ${energyValue}", "debug")

			BigDecimal energyValueDecimal = BigDecimal.valueOf(energyValue / 3600 / 1000)
			energyValueDecimal = energyValueDecimal.setScale(4, BigDecimal.ROUND_HALF_UP)

			logging("${device} : Energy : ${energyValueDecimal} kWh", "info")

			sendEvent(name: "energy", value: energyValueDecimal, unit: "kWh")
			sendEvent(name: "energyWithUnit", value: "${energyValueDecimal} kWh")

			// Uptime

			String uptimeValueHex = "undefined"
			uptimeValueHex = receivedData[4..8].reverse().join()
			logging("${device} : uptime byte flipped : ${uptimeValueHex}", "trace")

			BigInteger uptimeValue = new BigInteger(uptimeValueHex, 16)
			logging("${device} : uptime counter reports : ${uptimeValue}", "debug")

			def newDhmsUptime = []
			newDhmsUptime = millisToDhms(uptimeValue * 1000)
			String uptimeReadable = "${newDhmsUptime[3]}d ${newDhmsUptime[2]}h ${newDhmsUptime[1]}m"

			logging("${device} : Uptime : ${uptimeReadable}", "debug")

			sendEvent(name: "uptime", value: uptimeValue, unit: "s")
			sendEvent(name: "uptimeReadable", value: uptimeReadable)

		} else {

			// Unknown power or energy data.
			reportToDev(map)

		}

	} else if (map.clusterId == "00F0") {

		// Device status cluster.
               logging("${device} : Bat report received. Impossible no bat in unit.", "trace")
		
		}

		// Report the temperature in celsius.
//		def temperatureValue = "undefined"
///		temperatureValue = receivedData[7..8].reverse().join()
//		logging("${device} : temperatureValue byte flipped : ${temperatureValue}", "trace")
//		BigDecimal temperatureF = hexToBigDecimal(temperatureValue) 

//		logging("${device} : temperature sensor value : ${temperatureF}", "trace")

// temp is broken not C not F ?
//		sendEvent(name: "temperature", value: temperatureF, unit: "F")
//		sendEvent(name: "temperatureWithUnit", value: "${temperatureF} °F")

       if (map.clusterId == "00F6") {

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

			String deviceManufacturer = "AlertMe"
			String deviceModel = ""
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {
				deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
			}

			logging("${device} : Device : ${deviceModel}", "info")
			logging("${device} : Firmware : ${deviceFirmware}", "info")

			updateDataValue("manufacturer", deviceManufacturer)
			updateDataValue("model", deviceModel)
			updateDataValue("firmware", deviceFirmware)//  The last current version should be 2013-09-26

		} else {

			// Not a clue what we've received.
			reportToDev(map)

		}
// These clusters are received. This ignores them until they are identified
//Received Unknown: cluster: null, clusterId: 00EF, attrId: null, command: 82 with value: null and 9 bits of data: [00, 00, 00, 00, 43, 40, 00, 00, 00]
//Received Unknown: cluster: null, clusterId: 00F0, attrId: null, command: FB with value: null and 13 bits of data: [1C, 21, B5, 00, 01, 0B, 32, 80, 77, C3, FF, 01, 00]
//Received Unknown: cluster: null, clusterId: 00EF, attrId: null, command: 81 with value: null and 2 bits of data: [00, 00]
//Received Unknown: cluster: null, clusterId: 00EE, attrId: null, command: 80 with value: null and 2 bits of data: [07, 01]          
           
	} else if (map.clusterId == "00EE") {

		// Unknown what this cluster is for
		//   00EE arrives with 2  bytes of data
		//  
		logging("${device} : Skipping  cluserId ${map.clusterId}.", "debug")

	}
    
    
    else if (map.clusterId == "00F0") {

		// Unknown what this cluster is for
		//   00EF arrives with 13  bytes of data
		//  
		logging("${device} : Skipping cluserId ${map.clusterId}.", "debug")

	}
    
    
    else if (map.clusterId == "00EF") {

		// Unknown what this cluster is for
		//   00EF arrives with 9 or 2  bytes of data
		//  
		logging("${device} : Skipping  cluserId ${map.clusterId}.", "debug")

	}
    
    
    else if (map.clusterId == "8001" || map.clusterId == "8038") {

		// These clusters are sometimes received from the SPG100 and I have no idea why.
		//   8001 arrives with 12 bytes of data
		//   8038 arrives with 27 bytes of data
		logging("${device} : Skipping  cluserId ${map.clusterId}.", "debug")

	} else if (map.clusterId == "8032" ) {

		// These clusters are sometimes received when joining new devices to the mesh.
		//   8032 arrives with 80 bytes of data, probably routing and neighbour information.
		// We don't do anything with this, the mesh re-jigs itself and is a known thing with AlertMe devices.
		logging("${device} : New join has triggered a routing table reshuffle.", "debug")

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
