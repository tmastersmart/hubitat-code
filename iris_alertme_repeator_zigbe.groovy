/*Iris v1 Repeator Zigbee
https://fcc.report/FCC-ID/WJHRP11/

Iris v1 repeader zigbee driver for hubitat

// Item #388560 Model #REP901 REP800 Iris Range Extender FCC ID WJHRP11 Zigbee/Zwave
notice acording to old reports the REP800 had a defect in the ZWAVE side so dont pair ZWAVE
    10/06/2020 v2.0 Added logo. Reduced BAT reports now only when changed
    09/30/2021 v1.9 Merge in new code from KeyPad driver better error detection logging
    09/06/2021 v1.8 Battery fix / Powerfalure detection
    09/04/2021 v1.7 Button support added 
               v1.6 Model detection fix. Change flag
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

Post comments here
http://www.winnfreenet.com/wp/2021/09/iris-v1-alertme-repeater-zigbe-hubitat-driver/





 * based on alertme UK code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
 */
def clientVersion() {
    TheVersion="2.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

metadata {

	definition (name: "Iris v1 Repeator Zigbee", namespace: "tmastersmart", author: "tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
        capability "PowerSource"
        capability "ReleasableButton"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"

		command "normalMode"
		command "rangingMode"
		command "quietMode"

		attribute "batteryState", "string"
		attribute "batteryVoltage", "string"
		attribute "mode", "string"


		fingerprint profileId: "C216", inClusters: "00F0,00F3", outClusters: "", manufacturer: "Iris/AlertMe", model: "RepeaterPlug", deviceJoinName: "Iris AlertMe Repeater Plug"
		
	}

}

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
    state.icon = "<img src='data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/4gIoSUNDX1BST0ZJTEUAAQEAAAIYAAAAAAIQAABtbnRyUkdCIFhZWiAAAAAAAAAAAAAAAABhY3NwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQAA9tYAAQAAAADTLQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAlkZXNjAAAA8AAAAHRyWFlaAAABZAAAABRnWFlaAAABeAAAABRiWFlaAAABjAAAABRyVFJDAAABoAAAAChnVFJDAAABoAAAAChiVFJDAAABoAAAACh3dHB0AAAByAAAABRjcHJ0AAAB3AAAADxtbHVjAAAAAAAAAAEAAAAMZW5VUwAAAFgAAAAcAHMAUgBHAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAFhZWiAAAAAAAABvogAAOPUAAAOQWFlaIAAAAAAAAGKZAAC3hQAAGNpYWVogAAAAAAAAJKAAAA+EAAC2z3BhcmEAAAAAAAQAAAACZmYAAPKnAAANWQAAE9AAAApbAAAAAAAAAABYWVogAAAAAAAA9tYAAQAAAADTLW1sdWMAAAAAAAAAAQAAAAxlblVTAAAAIAAAABwARwBvAG8AZwBsAGUAIABJAG4AYwAuACAAMgAwADEANv/bAEMAAwICAgICAwICAgMDAwMEBgQEBAQECAYGBQYJCAoKCQgJCQoMDwwKCw4LCQkNEQ0ODxAQERAKDBITEhATDxAQEP/bAEMBAwMDBAMECAQECBALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEP/AABEIADcAZAMBIgACEQEDEQH/xAAcAAEAAgIDAQAAAAAAAAAAAAAABAUCBgEHCAP/xAA6EAABAgMCCAsHBQAAAAAAAAAAAgMBBAUGEhYXIlRVdJGSBxETMjU2coKisrMUJDRCRVFzZHGjscH/xAAXAQEBAQEAAAAAAAAAAAAAAAAAAgED/8QAFhEBAQEAAAAAAAAAAAAAAAAAAAEC/9oADAMBAAIRAxEAPwD3DXOFC1D1Vcbps4mXlUuqbS2ltKuaq7ziyYtZaJxq8qrO3u6devp98VrL3qKNulvhiMtq4wotBpV7aIWktAr6o/vFZBN75j6pTdSGLOFoq9pR/eOcJK9pR7eK0AWWEle0o9vDCSuaSe3itAFjhHXNJP7wwhrWkn9pXAsWSLRVhMcqedV3iZN1mfXTlTMtPvoeQm8nLVdgooSYrop/sKA7Lo8/Cp0mSqLkeTVNS7b0U/a8mEf9BHsd1VpGpMemkAeeZlPvitac9RRtste5E1SY+PXrb3qKNtlE3mTlltfVCbiTMhSErUpebnXp6pJmGHnUqlmuSS3yCbvNvfNlZRNKYAAAAAAAAEv6S/2FEQmp6Kf7Cix2HY/qpR9SZ8kAY2M6p0jU2fJAAef5xN2dXrj3qKNqlcps1+sMql6ktKs7cV/Io2CVyWjlltSAAUwAAAAAAAAJsOjH/wAaiET2+jX/AMagN7sj1Vo+pNeWAFk0cVl6RD9E15YAsa3aDgtbq88qfYqns8FKvXIovGTfBvNtIuxqbMe6oAgc4vJ3SjW6oYvZ7SLGxQAHOL6oZ8x4hi+qGeseIABi/qGeMeIxwAqOeseIABgBUc9Y8Qxf1POWdgACFgKjCOXMtQ/Yl4ETLkupj2pDSVp4lOcV6OwAsbZIyrUhKMyMvC61LtpaRD7JTCEIf0AAP//Z'>"
    
    state.message = "REP800 should not be paired in ZWAVE as it has a defect. Only REP901 works on Zwave."
  
    sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
	sendEvent(name: "lqi", value: 0)
	sendEvent(name: "operation", value: "unknown", isStateChange: false)
	sendEvent(name: "presence", value: "not present")



	// Remove old settings. Upgrade for debugging
	state.remove("switch")
    state.remove("tamper")
	state.remove("temperature")	
	state.remove("batteryState")
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

	logging("${device} : Received Unknown: cluster: ${map.cluster}, clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")

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
    sendEvent(name: "operation", value: "quiet")

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
    logging("${device} : Cluster:${map.clusterId} Cmd:${map.command} MAP:${map.data}","debug")

 
    if (map.clusterID == "0013"){
	logging("${device} : Device announce message","warn")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Anouncing New Device","debug")
	

    } else if (map.clusterId == "0006") {
		logging("${device} : Sending Match Descriptor Response","debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])
       
    } else if (map.clusterId == "00EF") {
     // Relay actuation and power state messages. 
     // This should give us Mains decection but it never reports   
    logging("${device} : Mains cluster Notify DEF -->: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} data: ${receivedData}", "trace")
   

    } else if (map.clusterId == "00F0") {

  batRec = receivedData[0]// [19] This sould be set with bat data but doesnt follow standard
      tempRec = receivedData[1]// does not folow alertme standard 1 2 3 are actualy running a timmer up and down
    switchRec = receivedData[4]
       lqiRec = receivedData[8]// 
      //if (lqiRec){ lqi = receivedData[10]}
      def batteryVoltageHex = "undefined"
      BigDecimal batteryVoltage = 0
      inspect = receivedData[1..3].reverse().join()
      inspect2 = zigbee.convertHexToInt(inspect) // Unknown Counter Counts up or down
      batteryVoltageHex = receivedData[5..6].reverse().join()
//      if (tempRec){
//          temp  = receivedData[7..8].reverse().join()
//          temp = zigbee.convertHexToInt(temp)
//      }
      if (batteryVoltageHex == "FFFF") {return}
//      if (batRec){ 
     batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
     batteryVoltage = batteryVoltage.setScale(2, BigDecimal.ROUND_HALF_UP)
     batteryVoltage = batteryVoltage + 1 // Kludge for low value being reported
     logging("${device} Raw Battery  Bat:${batRec} ${batteryVoltage}", "debug")    


		BigDecimal batteryVoltageScaleMin = 2.72// 3v would be 1 volt per cell
		BigDecimal batteryVoltageScaleMax = 4.19
        
        BigDecimal batteryPercentage = 0
        batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage

        
        
    if (batteryVoltage > state.lastBatteryVoltage  ){
	 logging( "${device} : Battery : ${batteryPercentage}% (${batteryVoltage} V)","info")
	 sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
     sendEvent(name: "battery", value:batteryPercentage, unit: "%")
     logging("${device} : Last:${state.lastBatteryVoltage} Now:${batteryVoltage}", "debug")    
     state.supplyPresent = true
        if (batteryVoltage > batteryVoltageScaleMax){sendEvent(name: "batteryState", value: "Full")}
        else{sendEvent(name: "batteryState", value: "charging")}
     sendEvent(name: "PowerSource", value: "mains")
     sendEvent(name: "supplyPresent", value: "${state.supplyPresent}")
    }
    if (batteryVoltage < state.lastBatteryVoltage ){    
      logging( "${device} : Battery : ${batteryPercentage}% (${batteryVoltage} V)","info")
	 sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
     sendEvent(name: "battery", value:batteryPercentage, unit: "%")
     logging("${device} : Last:${state.lastBatteryVoltage} Now:${batteryVoltage}", "debug")    
     logging("${device} : Discharging", "debug") 
     state.supplyPresent = false
     sendEvent(name: "batteryState", value: "discharging")
     sendEvent(name: "PowerSource", value: "battery")
     sendEvent(name: "supplyPresent", value: "${state.supplyPresent}")
     }
    state.lastBatteryVoltage = batteryVoltage
     
         

//00F3 Key Fob Cluster:00F3 Cmd:00 MAP:[00, 02, A2, A6, 00, 00]
    } else if (map.clusterId == "00F3") {
    PinEnclosed = receivedData[0]// The command being passed to us
 
//State:00 [F] MAP:[00, 02, B9, 80, 00, 00]
//State:20 [F] MAP:[20, 00, 0B, CD, 6A, 3E, C3]
//State:21 [F] MAP:[21, 00, 30, 00]        
            
        logging("${device} : State:${PinEnclosed} MAP:${map.data}", "trace")
        logging("${device} : Button 1 pushed", "info")
           
		sendEvent(name: "pushed", value: 1, isStateChange: true)
        
//00C0 KeyPad cluster
    } else if (map.clusterId == "00C0") {
       
//Pressing button on ac Sends PIN report with 0 bytes then state F then Released NULL
//00C0, 0A [21, 00, 30, 00]
//          21= PIN 0 bytes
//00C0, 0A [20, 00, 0B, 43, 46, 1A, EA]
//          20=state message 64=F "F"
//00C0, 0A [23, 00, 30, 02]
//          23= Released    
       PinEnclosed = receivedData[0]// The command being passed to us

            
        logging("${device} : State:${PinEnclosed}  MAP:${map.data}", "trace")
        logging("${device} : Button 1 released", "info")
		sendEvent(name: "released", value: 1, isStateChange: true)
  
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
            reportFirm = "Report to DEV Firm and MDL"
            reportModel = false
            if(deviceFirmware == "2013-09-26" ){
                reportFirm = "Known v2013"
                reportModel = true
            }
            // device: RepeaterPlug 
			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {
				deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
			}
           logging("${device} : ${deviceModel} Firmware :[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "info")
	       updateDataValue("manufacturer", deviceManufacturer)
           updateDataValue("device", deviceModel)
            if (reportModel){updateDataValue("model", "REP800")}// because we dont know how to detect REP901 yet
	       updateDataValue("firmware", deviceFirmware)
           updateDataValue("fcc", "WJHRP11")
           updateDataValue("partno", "RP11")
          
        //  Centrica Connected home Limited Wireless Repeater RP11          
        }
} else if (map.clusterId == "8001") {

  logging("${device} : Routing and Neighbour Information", "info")	     
// no ideal here I thought 8001 was a recovery mesg but it is not.
//  state.supplyPresent = true
//  sendEvent(name: "PowerSource", value: "mains", isStateChange: true)   
        
} else if (map.clusterId == "8032" ) {
	// These clusters are sometimes received when joining new devices to the mesh.
        //   8032 arrives with 80 bytes of data, probably routing and neighbour information.
        // We don't do anything with this, the mesh re-jigs itself and is a known thing with AlertMe devices.
	logging( "${device} : New join has triggered a routing table reshuffle.","debug")
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
