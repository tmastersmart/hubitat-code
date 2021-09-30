/* Iris v1 Smart Plug 

Hubitat iris smart plug driver with siren and strobe comands
AlertMe USA version smart plug
Centrica Connected home Limited Wireless Smartplug SP11

  ___      _             _   ____                       _     ____  _             
 |_ _|_ __(_)___  __   _/ | / ___| _ __ ___   __ _ _ __| |_  |  _ \| |_   _  __ _ 
  | || '__| / __| \ \ / / | \___ \| '_ ` _ \ / _` | '__| __| | |_) | | | | |/ _` |
  | || |  | \__ \  \ V /| |  ___) | | | | | | (_| | |  | |_  |  __/| | |_| | (_| |
 |___|_|  |_|___/   \_/ |_| |____/|_| |_| |_|\__,_|_|   \__| |_|   |_|\__,_|\__, |
                                                                            |___/

USA version  model# SPG800 FCC ID WJHSP11


 * 09/20/2021 v2.5  Merging in code from my KepPad driver. Better error handeling and logging
 * 09-14-2021 v2.4  Some log fixes and cleanup
 * 09-14-2021 v2.3  Added alarm support
 * 09/03/2011 v2.1  unknown commands logging, Battery reporting removed.
 * 08/05/2021 v2.0  Cleanup on logging power states  removal of unneeded info
 * 08/04/2021 v1.8.2 Better logging on energy total cleanup
 * 05/16/2021 v1.7 
 * 04/11/2021 v1   Release

https://github.com/tmastersmart/hubitat-code/blob/main/iris_alertme_smartplug.groovy
https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_smartplug.groovy

tested on:
 firmware: 2012-09-20 OK
 firmware: 2013-09-26 OK 


Hold down button while plugging in to factory reset. Should start double flashing

Energy is total since start
Power  is current watts

Battery USA model has no battery but still reports internal power at 12 volts. 





 * based on alertme UK code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
 *	
 */
def clientVersion() {
    TheVersion="2.5"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

metadata {
	definition (name: "Iris v1 Smart Plug w siren", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_smartplug.groovy") {

		capability "Actuator"
		capability "Configuration"
		capability "EnergyMeter"
		capability "Initialize"
		capability "Outlet"
		capability "PowerMeter"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"
		capability "Switch" 
        capability "Alarm"


		//command "lockedMode"
		command "normalMode"
		command "rangingMode"
		command "quietMode"

		attribute "alarmcmd", "string"
		attribute "strobe", "string"
		attribute "VoltageWithUnit", "string"
		attribute "siren", "string"
//		attribute "energyWithUnit", "string"
		attribute "mode", "string"
		attribute "power", "string"
//		attribute "stateMismatch", "boolean"
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
    sendEvent(name: "alarmcmd", value: "0")
	sendEvent(name: "lqi", value: 0)
	sendEvent(name: "operation", value: "unknown", isStateChange: false)
	sendEvent(name: "power", value: 0, unit: "W", isStateChange: false)
//	sendEvent(name: "powerSource", value: "unknown", isStateChange: false)
//	sendEvent(name: "powerWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "presence", value: "not present")
//	sendEvent(name: "stateMismatch", value: true, isStateChange: false)
	sendEvent(name: "switch", value: "unknown")
//	sendEvent(name: "temperature", value: 0, unit: "F", isStateChange: false)
//	sendEvent(name: "temperatureWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)

	// Remove disused state variables from earlier versions.
	state.remove("powerWithUnit")
    state.remove("energyWithUnit")
    state.remove("temperature")
	state.remove("temperatureWithUnit")	
	state.remove("battery")
    state.remove("batteryVoltage")
	state.remove("powerSource")
	state.remove("batteryWithUnit")
	state.remove("supplyPresent")
	state.remove("stateMismatch")

    state.remove("batteryOkay")
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
    state.alarmcmd = 0
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
    state.alarmcmd = 0
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
    logging("${device} : Mode : Ranging ${state.rangingPulses}", "info")

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
	sendEvent(name: "operation", value: "quiet")
	sendEvent(name: "power", value: 0, unit: "W", isStateChange: false)
//	sendEvent(name: "powerWithUnit", value: "unknown", isStateChange: false)
	sendEvent(name: "uptime", value: 0, unit: "s", isStateChange: false)
	sendEvent(name: "uptimeReadable", value: "unknown", isStateChange: false)


	logging("${device} : Mode : Quiet", "info")

	refresh()

}


def siren(cmd){
    state.alarmcmd = 1
    sendEvent(name: "alarmcmd", value: "${state.alarmcmd}")
  on()

}
def strobe(cmd){
    state.alarmcmd = 2
    sendEvent(name: "alarmcmd", value: "${state.alarmcmd}")
  on()
   
    
}
def both(cmd){
    state.alarmcmd = 3
    sendEvent(name: "alarmcmd", value: "${state.alarmcmd}")
  on()

}






def off() {
	state.alarmcmd = 0
    sendEvent(name: "alarmcmd", value: "${state.alarmcmd}")
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
//	logging("${device} :Parse Data: ${description}", "trace")

    clientVersion()
	updatePresence()
    
	Map descriptionMap = zigbee.parseDescriptionAsMap(description)
	if (descriptionMap) {
		processMap(descriptionMap)
	} else {
		logging("${device} : Parse Failed ..${description}", "warn")
	}
}


def processMap(Map map) {
    logging ("${device} : ${map}","trace")
	String[] receivedData = map.data	// AlertMe values are always sent in a data element.
    logging ("${device} : Cluster:${map.clusterId} State:${map.command} MAP:${map.data}","trace")

/*
Internal notes: Building Cluster map 
* = likely done by HUB in Join.
0000 Network (16-bit) Address Request *
0004 Simple Descriptor Request *
0005 Active Endpoint Request *
0006 Match Descriptor Request (Light Flashes)
0013 Device announce message (Light Flashes)
00C0 Button report
     00 = Unknown (lots of reports)
     0A = Button
00EE Relay actuation (smartPlugs)
     80 = PowerState
00EF Power Energy messages
     81 = Power Reading
     82 = Energy
00F0 Battery & Temp
     FB 
00F3 Key Fob
00F2 Tamper
00F6 Discovery Cluster
     FD = Ranging
     FE = Device version response.
0500 Security Cluster (Tamper & Reed)
8001 Routing Neighobor information
8004 simple descriptor response
8005 Active Endpoint Response (tells you what the device can do)
8032 Received when new devices join
8038 Management Network Update Request

*/    
   
   
    // Relay actuation and power state messages.
	if (map.clusterId == "00EE") {
       if (map.command == "80") {
       def powerStateHex = "undefined"
       def powerStateDisplay = "PowerState Needs mapping ?-->"    
	   powerStateHex  = receivedData[0]
       powerStateHex2 = receivedData[1] // This field is always 0 on US version.   
       if (powerStateHex == "0B" ) {powerStateDisplay="Joining"}
       if (powerStateHex == "0C" ) {powerStateDisplay="Power Restored"}
       if (powerStateHex == "08" ) {powerStateDisplay="Power Restored"}
       if (powerStateHex == "0A" ) {powerStateDisplay="Button Pressed"}
       if (powerStateHex == "0F" ) {powerStateDisplay="Button Pressed"}
       if (powerStateHex == "0D" ) {powerStateDisplay="Button Pressed"}
       if (powerStateHex == "0E" ) {powerStateDisplay="Last Power Restore memory was :OFF"}
       if (powerStateHex == "06" ) {powerStateDisplay="Power Restored"}// Reports this if on or off after restore?
       if (powerStateHex == "07" ) {powerStateDisplay="Last Power restore memory was :ON"}
       // need more info on these states They are diffrent than the UK models 
           size = receivedData.size() 
           if (size == 2 ){cmdREC = receivedData[0..1].collect{ (char)Integer.parseInt(it, 16) }.join()}
          
           logging("${device} :${powerStateDisplay} :${map.data} size ${size} :${cmdREC}", "info")
}     

			// Switch State

			def switchStateHex = "undefined"
			switchStateHex = receivedData[1]

			if (switchStateHex == "01") {

				state.relayClosed = true
                if(state.alarmcmd == 1){
                    sendEvent(name: "siren", value: "on")
                    logging("${device} : Sirene Alarm : ON", "info")
                }
                if(state.alarmcmd == 2){
                    sendEvent(name: "strobe", value: "on")
                    logging("${device} : Strobe Alarm : ON", "info")
                }
                if(state.alarmcmd == 3 ){
                    sendEvent(name: "strobe", value: "on")
                    sendEvent(name: "siren", value: "on")
                    logging("${device} : Siren-Strobe Alarm : ON", "info")
                }
                
                
				sendEvent(name: "switch", value: "on")
                logging("${device} : Switch : ON ${state.power} Watts", "info")

			} else {

				state.relayClosed = false
				sendEvent(name: "switch", value: "off") 
                sendEvent(name: "siren", value: "off")
                sendEvent(name: "strobe", value: "off")

				logging("${device} : Switch-siren-strobe : Off", "info")
			}
}



	 

else if (map.clusterId == "00EF") {

		// Power and energy messages.

		if (map.command == "81") {

			// Power Reading

			def powerValueHex = "undefined"
			int powerValue = 0

			// These power readings are so frequent that we only log them in debug or trace.
			powerValueHex = receivedData[0..1].reverse().join()
			logging("${device} : Current Power: ${powerValue} Watts :HEX ${powerValueHex}", "trace")
			powerValue = zigbee.convertHexToInt(powerValueHex)
			logging("${device} : Current Power: ${powerValue} Watts", "debug") // This has to be debug way to many reports
            state.power = powerValue
			sendEvent(name: "power", value: powerValue, unit: "W")
//			sendEvent(name: "powerWithUnit", value: "${powerValue} W")

		} else if (map.command == "82") {

			// Command 82 returns energy summary in watt-hours with an uptime counter.

			// Energy

			String energyValueHex = "undefined"
			energyValueHex = receivedData[0..3].reverse().join()
//			logging("${device} : energy byte flipped : ${energyValueHex}", "trace")

			BigInteger energyValue = new BigInteger(energyValueHex, 16)
			logging("${device} : energy counter reports : ${energyValue} hex:${energyValueHex}", "trace")

			BigDecimal energyValueDecimal = BigDecimal.valueOf(energyValue / 3600 / 1000)
			energyValueDecimal = energyValueDecimal.setScale(4, BigDecimal.ROUND_HALF_UP)

			logging("${device} : Total Energy Usage: ${energyValueDecimal} kWh", "debug")

			sendEvent(name: "energy", value: energyValueDecimal, unit: "kWh")
//			sendEvent(name: "energyWithUnit", value: "${energyValueDecimal} kWh")

			// Uptime

			String uptimeValueHex = "undefined"
			uptimeValueHex = receivedData[4..8].reverse().join()
//			logging("${device} : uptime byte flipped : ${uptimeValueHex}", "trace")

			BigInteger uptimeValue = new BigInteger(uptimeValueHex, 16)
			logging("${device} : uptime counter reports : ${uptimeValue} Hex:${uptimeValueHex} ", "trace")

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
        // These units have no battery but report internal voltage 12v but some report 30v ?
        // record voltage for testing as batteryVoltageWithUnit
		def batteryVoltageHex = "undefined"
		BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
		logging("${device} : batteryVoltageHex byte flipped : ${batteryVoltageHex}", "trace")
        batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
		batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)
		logging("${device} : Internal Voltage: ${batteryVoltage}", "debug")
//		sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V") 
//       sendEvent(name: "battery", value: 100, unit: "%")
        sendEvent(name: "VoltageWithUnit", value: batteryVoltage, unit: "V") 

        
        // Temp sensor data does not make sence. Just for testing
        // what i get is 7780 /16 = -8 deg 
        // I dont think this has a temp sensor 
    logging("${device} : Bat Volt Temp [00F0]  MAP:${map.data}", "trace")	     
        // Report the temperature in celsius.
//		def temperatureValue = "undefined"
//		temperatureValue = receivedData[7..8].reverse().join()
//		logging("${device} : temperatureValue byte flipped : ${temperatureValue}", "trace")
//		BigDecimal temperatureCelsius = hexToBigDecimal(temperatureValue) /16 
//		logging("${device} : temperatureCelsius sensor value : ${temperatureCelsius}", "info")
//		sendEvent(name: "temperature", value: temperatureCelsius, unit: "C")
 
    
    
    
    } else if (map.clusterId == "00F6") {
		// Discovery cluster. 
		if (map.command == "FD") {
			// Ranging is our jam, Hubitat deals with joining on our behalf.
			def lqiRangingHex = "undefined"
			int lqiRanging = 0
			lqiRangingHex = receivedData[0]
			lqiRanging = zigbee.convertHexToInt(lqiRangingHex)
			sendEvent(name: "lqi", value: lqiRanging)
		logging("${device} : Ranging lqi:${lqiRanging} ", "debug")
//            logging("${device} : lqi:${lqiRanging} ", "info")
			if (receivedData[1] == "77") {
				// This is ranging mode, which must be temporary. Make sure we come out of it.
				state.rangingPulses++
				if (state.rangingPulses > 30) {
					"${state.operatingMode}Mode"()
                    logging("${device} : Ranging Pulse: ${state.rangingPulse}. To Long Stopping", "warn")
				}

			} else if (receivedData[1] == "FF") {

				// This is the ranging report received every 30 seconds while in quiet mode.
				logging("${device} : quiet ranging report received", "debug")

			} else if (receivedData[1] == "00") {

				// This is the ranging report received when the device reboots.
				// After rebooting a refresh is required to bring back remote control.
				logging("${device} : reboot ranging report received", "warn")
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

			logging("${device} : VersionInfoBlock ${versionInfoBlockCount}", "debug")

			String deviceManufacturer = "Iris/AlertMe/Centrica" // Is this not stored in the device?
			String deviceModel = "" 
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
            reportFirm = "Report to DEV"
            if(deviceFirmware == "2012-09-20" ){reportFirm = "Known v2012"}
            if(deviceFirmware == "2013-09-26" ){reportFirm = "Known v2013"}

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {
				deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
			}
            logging("${device} : ${deviceModel} Firmware :[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "info")

            updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
            updateDataValue("model", "SPG800")
	        updateDataValue("firmware", deviceFirmware)
            updateDataValue("fcc", "WJHSP11")
            updateDataValue("partno", "SP11")

       
        } else {
         reportToDev(map)
        }
        
	}  else if (map.clusterId == "8038" ) {
       logging("${device} : cluster:8038 Seen before but unknown", "debug")
       logging("${device} : Dump: cluster: ${map.cluster}, clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "trace")
       // 8038, attrId: null, command: 00 with value: null and 27 bits of data: [00, 00, 00, F8, FF, 07, 2A, 00, 10, 00, 10, BC, CE, B6, B2, AC, A2, A3, A5, A4, AA, A5, A7, A3, 9D, 9D, 9D]
      

		// These clusters are sometimes received when joining new devices to the mesh.
		//   8032 arrives with 80 bytes of data, probably routing and neighbour information.
		// We don't do anything with this, the mesh re-jigs itself and is a known thing with AlertMe devices.
 
        // We have never seen 8032 on the USA model
    
        // on the USA we get 8001 or 0013 with a flashing light and 0006 as a join or droppoff error
        // in latest test moving the plug gave a 0013 flashing light Ranging was triggered then a 8001 
        // while pairing 6 and 13 will be sent 
 

        
	}  else if (map.clusterId == "8032" ) {
		logging("${device} : New join has triggered a routing table reshuffle.", "info")

   	} else if (map.clusterId == "8001") {
         logging("${device} : Routing and Neighbour Information", "info")	     


    } else if (map.clusterID == "0013"){
	logging("${device} : New Device announce message","info")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Anouncing New Device","debug")

    } else if (map.clusterId == "0006") {
		logging("${device} : Match Descriptor Request. Sending Response","debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])

    }  else {
//    logging("${device} : Received Unknown: cluster: ${map.cluster}, clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "trace")
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


