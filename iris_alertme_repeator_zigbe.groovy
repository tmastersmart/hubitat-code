/*Iris v1 Repeator Zigbe
https://fcc.report/FCC-ID/WJHRP11/

// Item #388560 Model #REP901 REP800 Iris Range Extender FCC ID WJHRP11 Zigbee/Zwave


===========================================================================================
v1.8    09/21/2022 Adjustments to ranging
v1.7    09/19/2022 Rewrote logging routines. Block code changes copied from keypad code
                   Rewrote presence and ranging routines.
v1.6    09/04/2022  Copying improvements from power outlet
v1.5    08/06/2021  Remove power and uptime
v1.4    05/16/2021   
v1.3    05/11/2021   Power stats testing
v1.2    05/08/2021 
v1.1    04/04/2021   
v1.0    04/11/2021 
 https://github.com/tmastersmart/hubitat-code/blob/main/iris_alertme_repeator_zigbe.groovy
 https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy



You have to pair this as zigbee and then zwave.
Hold down button while plugging in then press about 10 times to reset. Once flashing pair zigbee
Then pair zwave exact zwave process is not clear try pairing before zigbee or after.
Zwave driver is required for zwave part.


fingerprint model:"RepeaterPlug", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00F3", outClusters:""

Tested on  REP800 uses Firmware : 2013-09-26 

REP901 is the new version Need firmware versions.



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

Iris driver source code 
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_AlertMe_RangeExtender.driver


 * using modified UK plug code
 * Forked from https://raw.githubusercontent.com/birdslikewires/hubitat/master/alertme/drivers/alertme_smartplug.groovy
 * name: "AlertMe Smart Plug" 
 * namespace: "BirdsLikeWires", 
 * author: "Andrew Davison", 
 */
def clientVersion() {
    TheVersion="1.7.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

metadata {

	definition (name: "Iris v1 Repeator Zigbe", namespace: "tmastersmart", author: "tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/iris_alertme_repeator_zigbe.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
		capability "PowerSource"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"

		command "normalMode"
		command "rangeAndRefresh"
        command "unschedule" 
        command "uninstall"

		attribute "batteryState", "string"
		attribute "batteryVoltage", "string"
		attribute "mode", "string"


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
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
	
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
    state.remove("LQI")


    
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
	// Runs on reboot, or can be triggered manually.
	state.batteryOkay = true
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0

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
clientVersion()
	// Initialisation complete.
	logging("${device} : Initialised", "info")

}


def configure() {

	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.

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
    random = Math.abs(new Random().nextInt() % 33500)
    logging("${device} : configure pause:${random}", "info")
    pauseExecution(random)
	rangeAndRefresh()
	runIn(10,normalMode)
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
}





void reportToDev(map) {
	String[] receivedData = map.data
	logging("${device} : New unknown Cluster Detected: clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command}, value:${map.value} data: ${receivedData}", "warn")
}


// To be used later on a schedule. 
def quietMode() {
	// Turns off all reporting except for a ranging message every 2 minutes.
    delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"]),
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"]),
    ], 3000)    
	logging ("${device} : Mode: Quiet  [FA:03.01]","info")
    randomSixty = Math.abs(new Random().nextInt() % 60)
    runIn(randomSixty,refresh) // Refresh in random time
}

def normalMode() {
    // This is the standard running mode.
   delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	], 3000)
    logging("${device} : SendMode: [Normal]  Pulses:${state.rangingPulses}", "info")
}
void refresh() {
	logging("${device} : Refreshing", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}
// 3 seconds mains 6 battery  2 flash good 3 bad
def rangeAndRefresh() {
    logging("${device} : StartMode : [Ranging]", "info")
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"]) // ranging
	state.rangingPulses = 0
	runIn(6, normalMode)
}





def checkPresence() {
    // New shorter presence routine.
    // Runs on every parse and a schedule.
    def checkMin  = 5  // 5 min warning
    def checkMin2 = 10 // 10 min [not present] and 0 batt
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("${device} : Check Presence its been ${state.lastCheckInMin} mins","debug")
    if (state.lastCheckInMin <= checkMin){ 
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
        logging("${device} : Creating presence event: ${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin){ 
        logging("${device} : Sensor timing out ${state.lastCheckInMin} min ago","warn")
        runIn(60,refresh)// Ping Perhaps we can wake it up...
    }
    if (state.lastCheckInMin >= checkMin2) { 
        test = device.currentValue("presence")
        if (test != "not present"){
        value = "not present"
        logging("${device} : Creating presence event: ${value} ${state.lastCheckInMin} min ago","warn")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"${value}% ${state.version}", isStateChange: true) 
        runIn(60,refresh) 
        }
    }
}


def parse(String description) {
	logging("${device} : Parse : ${description}", "trace")
    state.lastCheckin = now()
	checkPresence()
    clientVersion()
    // Device contacts are zigbee cluster compatable
    if (description?.startsWith('enroll request')) {
			logging("${device} : Responding to Enroll Request. ", "info")
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
        if (batteryVoltageHex == "FFFF") {return}
     		batteryVoltageRaw = zigbee.convertHexToInt(batteryVoltageHex) / 1000
    		batteryVoltage = batteryVoltageRaw.setScale(3, BigDecimal.ROUND_HALF_UP)

        if (state.minVoltTest < 2.00){ 
            state.minVoltTest= 2.90
            logging("${device} : Min Voltage Reset to ${state.minVoltTest}v", "info") 
        }
        if (batteryVoltage < state.minVoltTest){
            state.minVoltTest = batteryVoltage
            logging("${device} : Min Voltage Lowered to ${state.minVoltTest}v", "info")  
        } 
 
		BigDecimal batteryPercentage = 0
        BigDecimal batteryVoltageScaleMin = state.minVoltTest // Auto adjustment
		BigDecimal batteryVoltageScaleMax = 3.50 // 3.6v battery   3.294V is what my bat runs when charged

      
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
         
} else if (map.clusterId == "00F3" || map.clusterId == "00C0") {// This is only on the repeator
        logging("${device} : Button Pressed Cluster:${map.clusterId} MAP:${map.data}", "warn")
        
 }else if (map.clusterId == "00F6") {// Join Cluster 0xF6

		
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
            logging("${device} : Ranging ${state.rangingPulses}", "debug")    
 			 if (state.rangingPulses > 14) {
              normalMode()
              return   
             }  
        } else if (receivedData[1] == "00") { // Ranging during a reboot
				// when the device reboots.(keypad) Must answer
				logging("${device} : reboot ranging report received", "info")
				refresh()
                return
			} 
// End ranging block 
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

			logging("${device} : ${versionInfoDump}", "debug")

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
            
            
            logging("${device}: Firmware: ${deviceFirmware} Model: ${deviceModel} manufacturer: ${deviceManufacturer}", "debug")
            if(!state.DataUpdate){
             state.DataUpdate = true 
			 updateDataValue("manufacturer", deviceManufacturer)
             updateDataValue("model", deviceModel)
			 updateDataValue("firmware", deviceFirmware)
            }    

		} else {reportToDev(map)}

// Standard IRIS USA Cluster detection block
// Delay to prevent spamming the log on a routing messages    
   } else if (map.clusterId == "8038") {
    logging("${device} : ${map.clusterId} Seen before but unknown", "debug")
	} else if (map.clusterId == "8001" ) {
        pauseExecution(new Random().nextInt(10) * 3000)
		logging("${device} : ${map.clusterId} Routing and Neighbour Information", "info")    
	} else if (map.clusterId == "8032" ) {
        pauseExecution(new Random().nextInt(10) * 3000)
		logging("${device} : ${map.clusterId} New join has triggered a routing table reshuffle.", "info")
    } else if (map.clusterId == "0006") {
		logging("${device} : Match Descriptor Request. Sending Response","info")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])
	} else if (map.clusterId == "0013" ) {
        pauseExecution(new Random().nextInt(10) * 3000)
		logging("${device} : ${map.clusterId} Re-routeing around dead devices. (power Falure?)", "warn")
	} else {
		reportToDev(map)// unknown cluster
	}
	return null
}


void sendZigbeeCommands(List<String> cmds) {
    logging("${device} : sending:${cmds}", "trace")
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
