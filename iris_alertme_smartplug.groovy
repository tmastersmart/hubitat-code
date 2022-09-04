/* Iris v1 Smart Plug custom

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
================
v3.4  09/04/2022  Updating standard routines for all my drivers. Schedules now optional
                  Better error detection and less logs
v3.3  08/29/0222  Power Report bug fix
v3.1  05/02/2022  Init mode cleanup 
v3.0  04/28/2022  Added Un Schedule
v2.9  04/23/2022  Added mains detection 
                  Wattage events only created if diffrent than last reading
                  stoped recording voltage to many events
v2.8  10/19/2021  Ranging bug fixes
v2.7  10/15/2021  Operating state logging changed.
v2.6  10/05/2021  Icon added
v2.5  09/20/2021  Merging in code from my KepPad driver. Better error handeling and logging
v2.4  09-14-2021  Some log fixes and cleanup
v2.3  09-14-2021  Added alarm support
v2.1  09/03/2011  unknown commands logging, Battery reporting removed.
v2.0  08/05/2021  Cleanup on logging power states  removal of unneeded info
v1.8.2 08/04/2021 Better logging on energy total cleanup
v1.7  05/16/2021  
v1.0  04/11/2021  Release

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
    TheVersion="3.4"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

metadata {
	definition (name: "Iris v1 Smart Plug custom", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_alertme_smartplug.groovy") {

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
//        capability "Power Source"


		//command "lockedMode"
		command "normalMode"
		command "rangeAndRefresh"
//		command "quietMode"
        command "unschedule" 
        command "uninstall"


		attribute "strobe", "string"
		attribute "siren", "string"
//		attribute "operatingMode", "string"
		attribute "power", "string"
//        attribute "PowerRestore", "string"

		fingerprint profileId: "C216", inClusters: "00F0,00EF,00EE", outClusters: "", manufacturer: "AlertMe", model: "SmartPlug2.5", deviceJoinName: "Iris SmartPlug v2.5"
		
	}

}
//*fingerprint model:"SmartPlug2.5", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00EF,00EE", outClusters:""



preferences {
	
	input name: "infoLogging", type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
    input name: "optionPresent",type: "bool", title: "Presence Detection", description: "Run a presence schedule (save then config)",defaultValue: true,required: true
    input name: "optionRange",  type: "bool", title: "Ranging Report", description: "Run a ranging schedule (save then config)",defaultValue: true,required: true
	
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
    // This runs on reboot 
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0
    state.logo ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-switch.jpg' >"

	// Remove disused state variables from earlier versions.
state.remove("powerWithUnit")
state.remove("energyWithUnit")
state.remove("icon")
state.remove("relayClosed")	
state.remove("battery")
state.remove("batteryVoltage")
state.remove("powerSource")
state.remove("batteryWithUnit")
state.remove("operatingMode")
state.remove("power")
state.remove("operation")
	
	// Remove unnecessary device details.
    device.deleteCurrentState("alarm")
    device.deleteCurrentState("alarmcmd")
    device.deleteCurrentState("VoltageWithUnit")
    device.deleteCurrentState("lqi")


	// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!
	randomSixty = Math.abs(new Random().nextInt() % 60)
	runIn(randomSixty,refresh)
clientVersion()
	// Initialisation complete.
	logging("${device} : Initialised", "info")
}


def configure() {
	// Runs on reboot paired or rejoined

//    state.alarmcmd = 0
//    state.uptime = 1
//    state.power = 0
//    state.energy = 0
    state.DataUpdate = false  
	initialize()
	unschedule()

	// Default logging preferences.
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])

	// Schedule our ranging report.6 hours or every 1 hour for outlets.
    if(optionRange){
	int checkEveryHours = 1				
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)	// At X seconds past X minute, every checkEveryHours hours, starting at Y hour.
    }
	// Schedule the presence check. 6 minutes or every 1 minute for key fobs.
    // no effect on bat just checks time 
    if (optionPresent){
	int checkEveryMinutes = 6							
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)									// At X seconds past the minute, every checkEveryMinutes minutes.
    }
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
void reportToDev(map) {
	String[] receivedData = map.data
	def receivedDataCount = ""
	if (receivedData != null) {
		receivedDataCount = "${receivedData.length} bits of "
	}
	logging("${device} : New unknown Cluster Detected: Report to DEV clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command} with value:${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
}


def normalMode() {
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"])
	state.operatingMode = "normal"
	refresh()
// 	sendEvent(name: "operatingMode", value: "normal")
	logging("${device} : operatingMode : Normal  Range pulses:${state.rangingPulses}", "info")
}


def rangingMode() {
	// Ranging mode double-flashes (good signal) or triple-flashes (poor signal) the indicator
	// while reporting LQI values. It's also a handy means of identifying or pinging a device.
   state.operatingMode = "ranging"
   sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"])
//	sendEvent(name: "operatingMode", value: "ranging")
    logging("${device} : operatingMode : Ranging ", "info")
	// Ranging will be disabled after a maximum of 30 pulses.
    state.rangingPulses = 0
}

                   
def siren(cmd){
    state.alarmcmd = 1
    sendEvent(name: "siren", value: "on")
  on()
}
def strobe(cmd){
    state.alarmcmd = 2
    sendEvent(name: "strobe", value: "on")
  on()
}
def both(cmd){
    state.alarmcmd = 3
    sendEvent(name: "siren", value: "on")
    sendEvent(name: "strobe", value: "on")
  on()
}

def off() {
	state.alarmcmd = 0
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 00 01} {0xC216}"])
}

def on() {
    state.alarmcmd = 0
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 02 01 01} {0xC216}"])
}


void refresh() {

	// The Smart Plug becomes remote controllable after joining once it has received confirmation of the power control operating mode.
	// It also expects the Hub to check in with this occasionally, otherwise remote control is eventually dropped.
	// Whenever a refresh happens (which is at least hourly using rangeAndRefresh() for outlets) we send the nudge.
	logging("${device} : Refreshing  ${state.uptime} V${state.version}", "info")
	def cmds = new ArrayList<String>()
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}")    // version information request
	cmds.add("he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 01 01} {0xC216}")    // power control operating mode nudge
	sendZigbeeCommands(cmds)
    checkPresence()
    

}


def rangeAndRefresh() {
// This toggles ranging mode to update the device's LQI value.
//	int returnToModeSeconds = 3			// We use 3 seconds for outlets, 6 seconds for battery devices, which respond a little more slowly.
	rangingMode()
//	runIn(returnToModeSeconds, "${state.operatingMode}Mode")
    runIn(3, "normalMode")
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
				logging("${device} : Presence : Not Present! Last ${secondsElapsed} seconds ago.", "warn")
		} else {logging("${device} : Presence : Ignoring overdue presence reports for ${uptimeAllowanceMinutes} minutes. The hub was rebooted ${hubUptime} seconds ago.", "debug")}
            
		} else {
			sendEvent(name: "presence", value: "present")
			logging("${device} : Presence : Last presence report ${secondsElapsed} seconds ago.", "debug")
		}
		logging("${device} : checkPresence() : ${millisNow} - ${state.presenceUpdated} = ${millisElapsed} (Threshold: ${presenceTimeoutMillis} ms)", "trace")
  	} else {logging("${device} : Presence : Waiting", "warn")}
}



def parse(String description) {
	logging("${device} : Parse : ${description}", "trace")
//	state.batteryOkay == true ?	sendEvent(name: "presence", value: "present") : sendEvent(name: "presence", value: "not present")
	updatePresence()
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
	String[] receivedData = map.data
    logging("${device} : processMap clusterId:${map.clusterId} command:${map.command} ${receivedData} ", "debug")
    
// Relay actuation and power state messages.
    if (map.clusterId == "00EE") {
       //OPERATING_MODE     = 0
       //RELAY_STATE        = 1
       //RELAY_STATUS_RQST  = 2
       
       if (map.command == "80") { //RELAY_STATUS_REPORT= 80
       state.operatingModeCode  = receivedData[0]
               RELAY_STATE      = receivedData[1]
//               RELAY_STATUS_RQST= receivedData[2]
           
       if (RELAY_STATE == "01") {
	       if (device.currentValue("switch") != "on"){// Reduce events only change if dif 
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
                sendEvent(name: "switch", value: "on",descriptionText: "${state.uptime} V${state.version}")
                logging("${device} : Switch : ON", "info")
	         }
		} 
       if (RELAY_STATE == "00") {
	          if (device.currentValue("switch") != "off"){// Only send state if its diff
                  sendEvent(name: "switch", value: "off",descriptionText: "${state.uptime} V${state.version}")
                  sendEvent(name: "siren", value: "off")
                  sendEvent(name: "strobe", value: "off")
		          logging("${device} : Switch : OFF", "info")	   
		   }    
       }
    // 0E power up in off 
    // 0F Power up in ON
    // 0D power up in ON 
    // 06 and 07 are on off   
    logging("${device} : Mode :${state.operatingModeCode}", "trace")
           
    if (state.operatingModeCode == "0D" || state.operatingModeCode == "0F"){
//           sendEvent(name: "PowerRestore", value: "in on") 
           logging("${device} : Mode :${state.operatingModeCode} Powered up in ON mode", "debug")
       }     
    if (state.operatingModeCode == "0E"){
//           sendEvent(name: "PowerRestore", value: "in off") 
           logging("${device} : Mode :${state.operatingModeCode} Powered up in OFF mode", "debug")
       }
    if (state.operatingModeCode == "06" || state.operatingModeCode == "07"){
           logging("${device} : Mode :${state.operatingModeCode} switch status ignored", "trace")
       }
       
       
      }
	else {reportToDev(map)}    
    }
	 
//   0x00EF     (239) Power Monitor Cluster 
// These power readings are so frequent that we need to slow them down
// We need to only save on change.   
else if (map.clusterId == "00EF") {
// RQST_PWR_REPORT      = 03
// INST_PWR_REPORT      = 81
// TOTAL_ENERGY_REPORT  = 82
		if (map.command == "81") {
			def powerValueHex = "undefined"
			int powerValue = 0
			powerValueHex = receivedData[0..1].reverse().join()
			powerValue = zigbee.convertHexToInt(powerValueHex)
            if(powerValue < 0){powerValue =0 }
         
            logging("${device} : Power: now:${powerValue}W Last:${state.power}W", "debug")
            if (powerValue != state.power){
             logging("${device} : Current Power: ${powerValue} Watts", "info")           
			 sendEvent(name: "power", value: powerValue, unit: "W",descriptionText: "${state.uptime} V${state.version}")
             state.power = powerValue   
            }
            

		} else if (map.command == "82") {
			// Command 82 returns energy summary in watt-hours with an uptime counter.
			// Energy
			String energyValueHex = "undefined"
			energyValueHex = receivedData[0..3].reverse().join()
			BigInteger energyValue = new BigInteger(energyValueHex, 16)
			BigDecimal energyValueDecimal = BigDecimal.valueOf(energyValue / 3600 / 1000)
   		    energyValueDecimal = energyValueDecimal.setScale(4, BigDecimal.ROUND_HALF_UP)
            
            logging("${device} : Total Energy Usage: ${energyValueDecimal}kWh", "debug")
             if (energyValueDecimal != state.energy){
			 logging("${device} : Total Energy Usage: ${energyValueDecimal}kWh", "info")
             sendEvent(name: "energy", value: energyValueDecimal, unit: "kWh",descriptionText: "${state.uptime} V${state.version}" )
             state.energy = energyValueDecimal
            }
            // Uptime
            // This reports every min. 
			String uptimeValueHex = "undefined"
			uptimeValueHex = receivedData[4..8].reverse().join()
			BigInteger uptimeValue = new BigInteger(uptimeValueHex, 16)
			def newDhmsUptime = []
			newDhmsUptime = millisToDhms(uptimeValue * 1000)
			String uptimeReadable = "${newDhmsUptime[3]}d ${newDhmsUptime[2]}h ${newDhmsUptime[1]}m"
            logging("${device} : Uptime : ${uptimeReadable}  ${uptimeValue}", "debug")
            state.uptime = uptimeReadable
		} else {
			reportToDev(map)
		}


// These units have no battery and no temp sensor     
} else if (map.clusterId == "00F0") {
    // if bit 0 battery voltage
    // if bit 1 temp 
    // if bit 3 lqi
    // bit 5 and 6 reversed
    // bit 7 and 8 reversed
    // LQI = 10 (lqi * 100.0) / 255.0
 	def batteryVoltageHex = "undefined"
   	BigDecimal batteryVoltage = 0
	batteryVoltageHex = receivedData[5..6].reverse().join()
    if (batteryVoltageHex == "FFFF") {return}
        batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
		batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)
        logging("${device} : Volts:${batteryVoltage} ", "debug") // ignore internal voltage
    
    } else if (map.clusterId == "00F6") {
		// Discovery cluster. 
		if (map.command == "FD") {
			// Ranging is our jam, Hubitat deals with joining on our behalf.
			def lqiRangingHex = "undefined"
			int lqiRanging = 0
			lqiRangingHex = receivedData[0]
			lqiRanging = zigbee.convertHexToInt(lqiRangingHex)
            state.LQI = lqiRanging
//			sendEvent(name: "lqi", value: lqiRanging)
            logging("${device} : Ranging lqi:${lqiRanging} - ${state.rangingPulses}", "debug")
			if (receivedData[1] == "77") {
			state.rangingPulses++
		     // This is ranging mode, which must be temporary. Make sure we come out of it.
 			if (state.rangingPulses > 12) {
 	          logging("${device} : Ranging ${state.rangingPulses} times is to Long", "warn")
              normalMode()
              return
			}

			} else if (receivedData[1] == "FF") {
				// This is the ranging report received every 30 seconds while in quiet mode.
				logging("${device} : quiet ranging report received", "debug")

			} else if (receivedData[1] == "00") {
  			// This is the ranging report received when the device reboots.
				// After rebooting a refresh is required to bring back remote control.
				logging("${device} : --reboot-- ranging report received", "warn")
				refresh()
			} else {reportToDev(map)} 

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

			logging("${device} : Ident ${versionInfoDump}", "trace")

			String deviceManufacturer = "Iris/AlertMe/Centrica" 
			String deviceModel = "" 
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
            reportFirm = "Report to DEV"
            if(deviceFirmware == "2012-09-20" ){reportFirm = "Known v2012"}
            if(deviceFirmware == "2013-09-26" ){reportFirm = "Known v2013"}

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()}
            
            logging("${device} : ${deviceModel} Firmware :[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "debug")
            if(!state.DataUpdate){
            state.DataUpdate = true    
            updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
            updateDataValue("model", "SPG800")
	        updateDataValue("firmware", deviceFirmware)
            updateDataValue("fcc", "WJHSP11")
            updateDataValue("partno", "SP11")
            }    
        } else {
         reportToDev(map)
        }
        
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
