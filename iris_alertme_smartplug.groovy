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
v4.1.0 11/15/2022 Bug fix in refreash and on off control
v4.0.2  11/12/2022 Bug fix presence
v4.0  11/11/2022 Bug fix on presence. Improvements from other iris drivers added
v3.9.1 11/06/2022 Logos added. Bug fix on config delay. operating Mode human form
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
    TheVersion="4.1.0"
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
    state.remove("operatingModeCode"),  
	state.remove("batteryOkay"),
	state.remove("battery"),
    state.remove("LQI"),
    state.remove("batteryOkay"),
    state.remove("Config"),
    state.remove("batteryState"), 
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

    // This runs on reboot 
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0

	// Remove disused state variables from earlier versions.
state.remove("powerWithUnit")
state.remove("energyWithUnit")
state.remove("logo")    
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
    state.remove("operatingModeCode")
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

   

	rangeAndRefresh()

    
    runIn(randomSixty,rangeAndRefresh)
    logging("configure", "info")
    
}
def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
}




// To be used later on a schedule. 
def quietMode() {
	// Turns off all reporting except for a ranging message every 2 minutes.
    delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"]),
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"]),
    ], 3000)    
	logging ("Mode: Quiet  [FA:03.01]","info")
    randomSixty = Math.abs(new Random().nextInt() % 60)
    runIn(randomSixty,refresh) // Refresh in random time
}

def normalMode() {
    // This is the standard running mode.
   delayBetween([ // Once is not enough
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 00 01} {0xC216}"]),// normal
	], 3000)
    logging("SendMode: [Normal]  Pulses:${state.rangingPulses}", "info")
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
void ping() {
    logging("ping", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
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
    logging("StartMode : [Ranging]", "info")
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
    // presence routine. v5 11-12-22
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 60
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


def parse(String description) {
	logging("Parse : ${description}", "trace")
    clientVersion()
    state.lastCheckin = now()
    checkPresence()
    
    if (description?.startsWith('enroll request')) {
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

    if (map.clusterId == "00EE") {
        if (map.command == "80") { //State:00EE command:80 data:[07, 01] 
           
       state.operatingMode = Integer.parseInt(receivedData[0],16)     
       operatingModeCode   = receivedData[0]
                        onOff   = receivedData[1]
                        current = device.currentValue("switch")
                   currentSiren = device.currentValue("siren")
                  currentStrobe = device.currentValue("strobe")
            logging("State: mode:${state.operatingMode} On/Off:${onOff} [${map.data}]", "debug")
        if (onOff == "00" || operatingModeCode =="06" ) {
  	     if (current == "on"){
           sendEvent(name: "switch", value: "off",descriptionText: "${state.uptime} V${state.version}")
           if (currentSiren  != "off"){sendEvent(name: "siren",  value: "off")}
           if (currentStrobe != "off"){sendEvent(name: "strobe", value: "off")}
           logging("Switch : OFF ", "info")
		   }  else {logging("Switch :OFF Our state:${current}", "info")}    
        }
       if (onOff == "01" || operatingModeCode =="07") {
	       if (current != "on"){
               if(state.alarmcmd == 1){
                    sendEvent(name: "siren", value: "on")
                    logging("Sirene Alarm : ON", "info")
                }
                if(state.alarmcmd == 2){
                    sendEvent(name: "strobe", value: "on")
                    logging("Strobe Alarm : ON", "info")
                }
                if(state.alarmcmd == 3 ){
                    sendEvent(name: "strobe", value: "on")
                    sendEvent(name: "siren", value: "on")
                    logging("Siren-Strobe Alarm : ON", "info")
                }
                sendEvent(name: "switch", value: "on",descriptionText: "${state.uptime} V${state.version}")
                logging("Switch : ON", "info")
	         }
           else {logging("Switch :ON Our state:${current}", "info")} 
		} 
  
       
    // 0E power up in off 
    // 0F Power up in ON
    // 0D on 
    // 0C off       
    // 06 and 07 are on off
    // give up on mode it doesnt follow the chart all the time      

      }
	   
    }
	 
//   0x00EF     (239) Power Monitor Cluster 
// These power readings are so frequent that we need to slow them down
// We need to only save on change.   
else if (map.clusterId == "00EF") {
 logging("Power:${map.clusterId} :${map.command} :${map.data}", "debug")
// RQST_PWR_REPORT      = 03
// INST_PWR_REPORT      = 81
// TOTAL_ENERGY_REPORT  = 82
		if (map.command == "81") {
			def powerValueHex = "undefined"
			int powerValue = 0
			powerValueHex = receivedData[0..1].reverse().join()
			powerValue = zigbee.convertHexToInt(powerValueHex)
            if(powerValue < 0){powerValue =0 }
         
            logging("Power: now:${powerValue}W Last:${state.power}W", "debug")
            if (powerValue != state.power){
             logging("Current Power: ${powerValue} Watts", "info")           
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
			 logging("Total Energy Usage: ${energyValueDecimal}kWh", "debug")
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
            logging("Uptime : ${uptimeReadable}  ${uptimeValue}", "debug")
            state.uptime = uptimeReadable
		} 


  
} else if (map.clusterId == "00F0") {
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
        logging("Battery: ${map.clusterId} :${map.command} Volts:${batteryVoltage}", "debug")
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
			 logging("LQI: ${lqiRanging}", "info")
            }   
		if (receivedData[1] == "77" || receivedData[1] == "FF") { // Ranging running in a loop
			state.rangingPulses++
            logging("Ranging ${state.rangingPulses}", "debug")    
 			 if (state.rangingPulses > 14) {
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
            


			String deviceManufacturer = "Iris/AlertMe/Centrica" 
			String deviceModel = "" 
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]
            reportFirm = "Report to DEV"
            if(deviceFirmware == "2012-09-20" ){reportFirm = "Known v2012"}
            if(deviceFirmware == "2013-09-26" ){reportFirm = "Known v2013"}
            
            if(reportFirm == "unknown"){state.reportToDev="Unknown firmware [${firmwareVersion}] ${deviceFirmwareDate}" }
            else{state.remove("reportToDev")}

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()}
            state.model = deviceModel
            logging("Ident:${deviceModel} Firm:[${firmwareVersion}] ${reportFirm} Driver v${state.version}", "debug")
            if(!state.DataUpdate){
            state.DataUpdate = true    
            updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
            updateDataValue("model", "SPG800")
	        updateDataValue("firmware", firmwareVersion)
            updateDataValue("fcc", "WJHSP11")
            updateDataValue("partno", "SP11")
            }    
        } 
        
/// Standard IRIS USA Cluster detection block v4

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
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}


// used only by uptime readable. 
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


void getIcons(){
    state.remove("logo")
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-v1-smartplug.jpg' >"
//  state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-switch.jpg' >"

 }

// Logging block  v4

void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
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

