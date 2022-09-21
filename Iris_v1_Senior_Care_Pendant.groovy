/*Iris v1 Senior Care Pendant
Hubitat Driver
CARE PRESENCE BEEP TONE FLASH
=============================

https://github.com/tmastersmart/hubitat-code
Corrects mising options in built in driver. 
This is the only driver that supports the care FOB the way it was orginaly designed to work.
A lot of work was needed to debug all of this and reverse the formats which are similar to the KeyPad.
This driver simulates the IRIS pay Care Tier which the Senior Care Pendant was for.
Add 2 devices to your dashboard for the pendant a keyfob and a alarm Switch
Pressing the HELP button turns on the alarm, The pendant is then notified help is coming and flashes red.
You have to create rules to monitor the alarm state and notify you of the alarm.
You then turn off the alarm from the dashboard switch and the pendant is notified help is coming and flashes green.
After a set delay it then clears for next use.
================================================================================================================
"Iris Care monitored "aging adults," and let owners receive notifications when a loved one fell or 
when they system detected abnormal use. Such as you didnt open the door and get the mail or no motion. 
This was a 2nd level pay service above free you paid $9.99 + $4.99 for care. It was later all moved
to the free service on iris v2 after a few months.
The Care Pendant would call for help notify you it had called and notify you help was coming.
This drver duplicates the care service on Hubitat.
=============================================================================================================
v1.9 09/21/2022 Adjust ranging code
v1.8 09/19/2022 Rewrote logging routines.
v1.7 09/16/2022 Routines copied from contact sensors. Updating iris blockcode
v1.6 07/29/2022 Minor updates
v1.5 06/10/2022 Default settings for OFF switch.
v1.4 11/25/2021 beep sends 2 beeps
v1.3 10/23/2021 Switch addded to be compatable with Dashboard. Added Keyfob mode with Long flash reply
v1.2 10/22/2021 typos
v1.1 10/22/2021 First release

reset:
HOLD down both buttons when inserting battery then press 3 or 4 times and it will start flashing to pair



https://github.com/tmastersmart/hubitat-code


 * based on alertme UK code from  
   https://github.com/birdslikewires/hubitat

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
 */

def clientVersion() {
    TheVersion="1.9.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
metadata {

	definition (name: "Iris v1 Senior Care Pendant", namespace: "tmastersmart", author: "tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/Iris_v1_Senior_Care_Pendant.groovy") {

		capability "Battery"
		capability "Configuration"
		capability "Initialize"
		capability "PresenceSensor"
		capability "Refresh"
		capability "SignalStrength"
//		capability "HoldableButton"
		capability "PushableButton"
        capability "Tone" 
		capability "Switch" 
        capability "Alarm"
        
        command "checkPresence"
        command "normalMode"
		command "rangeAndRefresh"
        command "WalkTest"
        command "unschedule"
        command "uninstall"



		
		attribute "batteryVoltage", "string"
        attribute "deviceModel", "string"
		attribute "mode", "string"
        attribute "care","string"

		fingerprint profileId: "C216", endpointId:"02", inClusters:"00F0,00C0", outClusters:"00C0", manufacturer: "Iris/AlertMe", model:"Care Pendant Device", deviceJoinName: "Iris v1 Senior Care Pendant"
		
	}

}
//firmware: 2012-09-20
//manufacturer: AlertMe



preferences {
	
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    
	input("delayTime",  "number", title: "Notify Timeout", description: "How many seconds to flash HELP Coming",defaultValue: 35,required: true)

    input name: "mode", type: "enum", title: "Mode", options: ["CARE", "KeyFOB"],description: "Use as CARE or 1 Button Keyfob", defaultValue: "CARE",required: true  
    
    
}


def installed() {
	// Runs after first pairing. this may never run internal drivers overide pairing.
	logging("${device} : Paired!", "info")
    initialize()
    configure()
    
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
    state.remove("batteryOkay")
    state.remove("Config")
    
removeDataValue("battery")
removeDataValue("battertState")
removeDataValue("batteryVoltage")
removeDataValue("contact")
removeDataValue("lqi")
removeDataValue("operation")
removeDataValue("presence")
removeDataValue("tamper")    
removeDataValue("temperature")
logging("${device} : Uninstalled", "info")   
}


//private getENCODING_BUTTON() { 0x0006 }

def initialize() {
    logging("${device} : Initialize", "info")
	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.
	// Reset states...
    // Testing is this needed? Because its not set right by default   
updateDataValue("inClusters", "00F0,00C0,00F3,00F5")
updateDataValue("outClusters", "00C0")

    
    state.icon = "<img src='data:image/jpeg;base64,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'>"
    state.donate="<img src='data:image/png;base64,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'>"


	state.presenceUpdated = 0
	state.rangingPulses = 0

	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("message")
	state.remove("care")	
    
	// Remove unnecessary device details.
	removeDataValue("application")
    
  sendEvent(name: "alarm", value: "off")
  sendEvent(name: "siren", value: "off")
  sendEvent(name: "strobe", value: "off")
  sendEvent(name: "switch", value: "off") 

    sendEvent(name: "powerSource", value: "battery")
    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
    logging("${device} : Initialised Refreash in ${randomSixty}sec", "info")

}

def configure() {
    logging("${device} : configure", "info")
	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.
    
	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("batteryOkay")
    state.remove("Config")
    
	unschedule()


	// Schedule randon ranging in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${6} * * ? *", rangeAndRefresh)	

// Check presence in hrs
//	randomSixty = Math.abs(new Random().nextInt() % 60)
//	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
//	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *", checkPresence)	
    
	// Schedule presence check in mins
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${1} * * * ? *", checkPresence)	

	// Configuration complete.
	logging("${device} : Configured", "info")

	// Run a ranging report and then switch to normal operating mode.
	rangeAndRefresh()
	runIn(12,normalMode)
	
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
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

// HELPSTATE_IDLE = 0
// HELPSTATE_HELP_NEEDED = 1
// HELPSTATE_HELP_NEEDED_ACK = 2
// HELPSTATE_HELP_CALLED = 3
// HELPSTATE_HELP_COMING = 4



def on(){
}

def press(buttonNumber){
   logging("${device} : Button ${buttonNumber}","info")
   sendEvent(name: "pushed", value: buttonNumber, isStateChange: true) 
}

// Advanced keyfob responce Long flash and beep
def keyfob(cmd){
    sendIrisCmd (2)
    press(1)
    pauseExecution(1500)
    sendIrisCmd (4)
    runIn(10,offFOB)
}

def off(){
  logging("${device} : Alarm set OFF delay:${delayTime}", "info")    
  state.Command = "HelpComing"  
  sendIrisCmd (4)
  sendEvent(name: "care", value: "Help Coming", isStateChange: true) 
  sendEvent(name: "alarm", value: "off")
  sendEvent(name: "siren", value: "off")
  sendEvent(name: "strobe", value: "off")
  sendEvent(name: "switch", value: "off")  
  if (delayTime <5){delayTime = 5}
    
  runIn(delayTime,offFOB)   
}

def offFOB(){
  logging("${device} : Clear  mode:${mode}", "info") 
  state.Command = "Clear"  
  sendIrisCmd (0)
  sendEvent(name: "care", value: "Clear", isStateChange: true)    
}    
    

def siren(cmd){
  press(1)  
  log.info "${device} :Alarm :ON"
  sendEvent(name: "alarm", value: "on")
  sendEvent(name: "siren", value: "on")
  sendEvent(name: "strobe", value: "on") 
  sendEvent(name: "switch", value: "on")   
  sendEvent(name: "pushed", value: 1, isStateChange: true)
  state.Command = "helpAck"
  sendIrisCmd (2)//ack
  sendEvent(name: "care", value: "Help Needed", isStateChange: true) 
  logging("${device} : Care Help button pressed. Notified:${state.Command} ", "warn")    
}

def notifyHelpComing(){

  state.Command = "helpCalled" 
  sendIrisCmd (3)//called
  sendEvent(name: "care", value: "Help Ack", isStateChange: true) 
  logging("${device} : Care Help button Notified:${state.Command} ", "warn")
}

def strobe(cmd){return}
def both(cmd){return}



def updatePresence() {
	long millisNow = new Date().time
	state.presenceUpdated = millisNow
}




def checkPresence() {
	// Check how long ago the presence state was updated.
	// AlertMe devices check in with some sort of report at least every 2 minutes (every minute for outlets).
	// It would be suspicious if nothing was received after 4 minutes, but this check runs every 6 minutes
	// by default (every minute for key fobs) so we don't exaggerate a wayward transmission or two.
	presenceTimeoutMinutes = 2 
    uptimeAllowanceMinutes = 5
    LastPresence = device.currentValue("presence")// only change state one time..
	if (state.presenceUpdated > 0 ) {
		long millisNow = new Date().time
		long millisElapsed = millisNow - state.presenceUpdated
		long presenceTimeoutMillis = presenceTimeoutMinutes * 60000
		BigInteger secondsElapsed = BigDecimal.valueOf(millisElapsed / 1000)
		BigInteger hubUptime = location.hub.uptime
		if (millisElapsed > presenceTimeoutMillis) {
			if (hubUptime > uptimeAllowanceMinutes * 60) {
                if (LastPresence != "not present"){
				sendEvent(name: "presence", value: "not present")
                logging("${device} : Presence: not present", "warn")    
                }    
				logging("${device} : Presence: Not Present! Last report received ${secondsElapsed} seconds ago.", "debug")
                powerLast = device.currentValue("battery")
                if (powerLast != 0){ // this does not work well yet
                    sendEvent(name: "battery", value:0, unit: "%")
                    logging("${device} : Battery set to 0%", "warn")
                }
		} else {logging("${device} : Presence: Ignoring overdue presence reports for ${uptimeAllowanceMinutes} minutes. The hub was rebooted ${hubUptime} seconds ago.", "info")}
        }else {
            if (LastPresence != "present"){
			 sendEvent(name: "presence", value: "present")
			 logging("${device} : Presence: present", "info")
            }
            logging("${device} : Presence: Last presence report ${secondsElapsed} seconds ago.", "debug")   
		}
	} else {logging("${device} : Presence: Not yet received. ", "warn")}
}

def parse(String description) {
	logging("${device} : Parse : ${description}", "trace")

	updatePresence()// this updates the timmer not the state
    // Device contacts are zigbee cluster compatable
//	if (description.startsWith("zone status")) {
//		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
//		processStatus(zoneStatus)
//	}  else 
    if (description?.startsWith('enroll request')) {
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

void reportToDev(map) {
	String[] receivedData = map.data
	logging("${device} : New unknown Cluster Detected: clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command}, value:${map.value} data: ${receivedData}", "warn")
}

def WalkTest(){
logging("${device} : Starting walk test.", "warn")
logging("${device} : if you dont see a flash its out of range.", "info")    
sendEvent(name: "care", value: "Walk Test", isStateChange: true) 
runIn(1,keepWalking)
runIn(10,keepWalking) 
runIn(20,keepWalking) 
runIn(30,keepWalking) 
runIn(40,keepWalking) 
runIn(50,keepWalking) 
runIn(60,keepWalking) 
runIn(70,keepWalking)     
runIn(80,offFOB)    
}    


def keepWalking(){
state.Command = "Walk Test" 
logging("${device} : Ping Walk test Running.", "info")     
cluster = 0x00C0
attributeId = 0x022
dataType = DataType.ENUM8
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, 0x01, [destEndpoint :0x02]))
logging("${device} : Ping Walk test Running.", "info")      
}

def beep(cmd){
logging ("${device} : Sending Beep","info")    
sendSound(4)
pauseExecution(500)  
sendSound(4)
sendSound(4)     
runIn(20,offFOB) 
}


def sendSound (cmdI){
cluster = 0x00C0
attributeId = 0x020
dataType = DataType.ENUM8
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, cmdI, [destEndpoint :0x02]))    
}


// Send the care command
def sendIrisCmd (cmdI){
cluster = 0x00C0
attributeId = 0x020
dataType = DataType.ENUM8
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, cmdI, [destEndpoint :0x02]))    
logging ("${device} : Sending Care state [${state.Command}] to Pendient","info")
} 


def processMap(Map map) {
	
	// AlertMe values are always sent in a data element.
	String[] receivedData = map.data
    size = receivedData.size()// size of data field

    logging("${device} : processMap clusterId:${map.clusterId} command:${map.command} ${receivedData} ${size}", "debug")

	if (map.clusterId == "00F0") {
		// Device status cluster.
		def temperatureValue  = "NA"
        def batteryVoltageHex = "NA"
        BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
        temperatureValue = receivedData[7..8].reverse().join()

     // some sensors report bat and temp at diffrent times some both at once?
     if (batteryVoltageHex != "FFFF") {
     	batteryVoltageRaw = zigbee.convertHexToInt(batteryVoltageHex) / 1000
    	batteryVoltage = batteryVoltageRaw.setScale(3, BigDecimal.ROUND_HALF_UP)
        // Auto adjustment like iris hub did it  minimumVolts:2.2, nominalVolts:3.0  
        if (state.minVoltTest < 2.19){ 
            state.minVoltTest= 2.40 
            logging("${device} : Min Voltage Reset to ${state.minVoltTest}v", "info") 
        }
        if (batteryVoltage < state.minVoltTest){
            state.minVoltTest = batteryVoltage
            logging("${device} : Min Voltage Lowered to ${state.minVoltTest}v", "info")  
        } 
		BigDecimal batteryPercentage = 0
        BigDecimal batteryVoltageScaleMin = state.minVoltTest 
		BigDecimal batteryVoltageScaleMax = 3.00  // 3.2 new battery
		batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
        powerLast = device.currentValue("battery")
        logging("${device} : battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V", "debug")
        if (powerLast != batteryPercentage){
           sendEvent(name: "battery", value:batteryPercentage, unit: "%")
           sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V", descriptionText: "Volts:${batteryVoltage}V MinVolts:${batteryVoltageScaleMin} v${state.version}")    
          if (batteryPercentage > 19) {logging("${device} : Battery:${batteryPercentage}% ${batteryVoltage}V", "info")}
          else { logging("${device} : Battery :LOW ${batteryPercentage}% ${batteryVoltage}V", "info")}
          if ( batteryVoltage < state.minVoltTest){state.minVoltTest = batteryVoltage}  // Record the min volts seen working      
         } // end dupe events detection
         logging("${device} : Temp Report ${temperatureValue}", "debug") 
        }// end battery report        

        // We get temp readings of 0000 Nosensor.


 } // end cluster 00F0

 else if (map.clusterId == "00C0") {    
/*
HELP_STATE = 0x20;
CUSTOM_SOUND = 0x21;
WALKTEST_TIMEOUT_S = 0x22;
LAST_HOME_LQI = 0x23;
LQI_THRESHOLD = 0x24;

IDLE = 0x00;
HELP_NEEDED = 0x01;
HELP_NEEDED_ACK = 0x02;
HELP_CALLED = 0x03;
HELP_COMING = 0x04;
 10 green light
*/
    if (map.command == "0A") { 
      cmd2 = receivedData[0]// HELP_STATE = 0x20
      cmd1 = receivedData[1]// 0     
      cmd3 = receivedData[2]// 30 
      cmd4 = receivedData[3]// HELP_NEEDED = 0x01
        // [20, 00, 30, 01]         
        if (cmd4 == "02") {// Missed the 1st ACK send again
            if (mode == "CARE"){notifyHelpComing()} 
            else{keyfob(1)}
           return 
        }
           
		if (cmd4 == "01") {// Button pressed answer it
         if (mode =="CARE"){ 
           siren()
           runIn(3,notifyHelpComing)
         }
        else {keyfob(1)}
        return
       } 
           
      }else { reportToDev(map) }// If we get anything else report it.
        
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

			logging("${device} : Ident Block: ${versionInfoDump}", "trace")

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
    // Moved to debug because of to many events
            logging("${device} : ${deviceModel} Ident: Firm:[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "debug")
            if(!state.Config){
            state.Config = true    
			updateDataValue("manufacturer", deviceManufacturer)
            if (deviceModel == "Care Pendant Device"){state.deviceModel = "KEY800"}//2012-09-20 <-known firmware    
            updateDataValue("device", deviceModel)

            updateDataValue("firmware", deviceFirmware)
            updateDataValue("fcc", "WJHWD11")
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
		logging("${device} : ${map.clusterId} Re-routeing", "warn")
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
