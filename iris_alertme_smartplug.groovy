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
v3.9  10/30/2022 Bug fix in presence routine
v3.8  10/11/2022 Energy usage moved to debug from info
v3.7  09/21/2022 Adjustments to ranging
v3.6  09/19/2022 Rewrote logging routines. Block code changes copied from keypad code
                 Rewrote presence and ranging routines.
v3.5  09/06/2022 Init routine delayed. minor fixes
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
    TheVersion="3.9.0"
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



		command "normalMode"
		command "rangeAndRefresh"

        command "unschedule" 
        command "uninstall"


		attribute "strobe", "string"
		attribute "siren", "string"

		attribute "power", "string"


		fingerprint profileId: "C216", inClusters: "00F0,00EF,00EE", outClusters: "", manufacturer: "AlertMe", model: "SmartPlug2.5", deviceJoinName: "Iris SmartPlug v2.5"
		
	}

}
//*fingerprint model:"SmartPlug2.5", manufacturer:"AlertMe", profileId:"C216", endpointId:"02", inClusters:"00F0,00EF,00EE", outClusters:""



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


    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
}


def configure() {
	// Runs on reboot paired or rejoined
    state.DataUpdate = false  
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
    delayBetween([ 
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"]),// get version info
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00EE {11 00 01 01} {0xC216}"]),// get power info
    ], 3000) 
}

// 3 seconds mains 6 battery  2 flash good 3 bad
def rangeAndRefresh() {
    logging("${device} : StartMode : [Ranging]", "info")
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"]) // ranging
	state.rangingPulses = 0
	runIn(6, normalMode)
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








def checkPresence() {
    // New shorter presence routine. v2 10/22
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
    if (state.lastCheckInMin >= checkMin){ 
      logging("${device} : Sensor timing out ${state.lastCheckInMin} min ago","warn")
      runIn(60,refresh)// Ping Perhaps we can wake it up...
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
	String[] receivedData = map.data
  
// Relay actuation and power state messages.
    if (map.clusterId == "00EE") {
    logging("${device} : State:${map.clusterId} :${map.command} :${map.data}", "debug")

       if (map.command == "80") { //State:00EE command:80 data:[07, 01] 
       state.operatingModeCode  = receivedData[0]
                        onOff   = receivedData[1]

                        current = device.currentValue("switch")
                   currentSiren = device.currentValue("siren")
                  currentStrobe = device.currentValue("strobe")

        if (onOff == "00" || state.operatingModeCode =="06" ) {
  	     if (current == "on"){
           sendEvent(name: "switch", value: "off",descriptionText: "${state.uptime} V${state.version}")
           if (currentSiren  != "off"){sendEvent(name: "siren",  value: "off")}
           if (currentStrobe != "off"){sendEvent(name: "strobe", value: "off")}
           logging("${device} : Switch : OFF ", "info")
		   }  else {logging("${device} : Switch :OFF Our state:${current}", "info")}    
        }
       if (onOff == "01" || state.operatingModeCode =="07") {
	       if (current != "on"){
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
           else {logging("${device} : Switch :ON Our state:${current}", "info")} 
		} 
  
       
    // 0E power up in off 
    // 0F Power up in ON
    // 0D power up in ON 
    // 06 and 07 are on off   
    logging("${device} : Mode :${state.operatingModeCode} ", "trace")
   
    if (state.operatingModeCode == "0D" || state.operatingModeCode == "0F"){
           logging("${device} : Mode :${state.operatingModeCode} Powered up in ON mode", "debug")
       }     
    if (state.operatingModeCode == "0E"){
        logging("${device} : Mode :${state.operatingModeCode} Powered up in OFF mode", "debug")
       }
     
       
      }
	else {reportToDev(map)}    
    }
	 
//   0x00EF     (239) Power Monitor Cluster 
// These power readings are so frequent that we need to slow them down
// We need to only save on change.   
else if (map.clusterId == "00EF") {
 logging("${device} : Power:${map.clusterId} :${map.command} :${map.data}", "debug")
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

            if (energyValueDecimal != state.energy){
			 logging("${device} : Total Energy Usage: ${energyValueDecimal}kWh", "debug")
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


// These units have no battery and no temp sensor Just log    
} else if (map.clusterId == "00F0") {
    logging("${device} : Sensor:${map.clusterId} :${map.command} :${map.data}", "debug")
    // if bit 0 battery voltage
    // if bit 1 temp 
    // if bit 3 lqi
    // bit 5 and 6 reversed
    // bit 7 and 8 reversed
    // LQI = 10 (lqi * 100.0) / 255.0
    def temperatureValue  = "NA"
    def batteryVoltageHex = "NA"
    BigDecimal batteryVoltage = 0
	batteryVoltageHex = receivedData[5..6].reverse().join()
    temperatureValue = receivedData[7..8].reverse().join()
    if (batteryVoltageHex != "FFFF") {
        batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
		batteryVoltage = batteryVoltage.setScale(3, BigDecimal.ROUND_HALF_UP)
        logging("${device} : Volts:${batteryVoltage} Ignoring", "debug") 
      }
    if (temperatureValue != "0000") {
		BigDecimal temperatureCelsius = hexToBigDecimal(temperatureValue) / 16
        temperatureF = (temperatureCelsius * 9/5) + 32//      fixed from UK code use F
        temperatureU = temperatureF
        logging("${device} : Temp:${temperatureF}F ${temperatureCelsius}C Ignoring", "debug")
    }  
    
    
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

			logging("${device} : Ident ${versionInfoDump}", "trace")

			String deviceManufacturer = "Iris/AlertMe/Centrica" 
			String deviceModel = "" 
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
            reportFirm = "Report to DEV"
            if(deviceFirmware == "2012-09-20" ){reportFirm = "Known v2012"}
            if(deviceFirmware == "2013-09-26" ){reportFirm = "Known v2013"}
            
            if(reportFirm == "Report to DEV"){state.reportToDev="Report Unknown firmware [${deviceFirmware}] " }
            else{state.remove("reportToDev")}

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
        } else {  reportToDev(map) }
        
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
