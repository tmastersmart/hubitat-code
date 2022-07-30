/*Iris v1 Senior Care Pendant
Hubitat Driver
CARE PRESENCE BEEP TONE FLASH
=============================

Corrects mising options in built in driver. 
This is the only driver that supports the care FOB the way it was orginaly designed to work.
A lot of work was needed to debug all of this and reverse the formats which are similar to the KeyPad.

This driver simulates the IRIS pay Care Tier which the Senior Care Pendant was for.

Add 2 devices to your dashboard for the pendant a keyfob and a alarm Switch

Pressing the HELP button turns on the alarm, The pendant is then notified help is coming and flashes red.
You have to create rules to monitor the alarm state and notify you of the alarm.
You then turn off the alarm from the dashboard switch and the pendant is notified help is coming and flashes green.
After a set delay it then clears for next use.



=============================================================================================================
v1.3 10/23/2021 Switch addded to be compatable with Dashboard. Added Keyfob mode with Long flash reply
v1.2 10/22/2021 typos
v1.1 10/22/2021 First release

reset:
HOLD down both buttons when inserting battery then press 3 or 4 times and it will start flashing to pair

Tested on 2012-09-20


"Iris Care monitored "aging adults," and let owners receive notifications when a loved one fell or 
when they system detected abnormal use. Such as you didnt open the door and get the mail or no motion. 
This was a 2nd level pay service above free you paid $9.99 + $4.99 for care. It was later all moved
to the free service on iris v2 after a few months.
The Care Pendant would call for help notify you it had called and notify you help was coming.

This drver duplicates the care service on Hubitat.

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
    TheVersion="1.3"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
metadata {

	definition (name: "Iris v1 Senior Care Pendant", namespace: "tmastersmart", author: "tmaster", importUrl: "https://github.com/tmastersmart/hubitat-code/raw/main/Iris_v1_Senior_Care_Pendant.groovy") {

        
        
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
        
		command "normalMode"
		command "rangingMode"
        command "WalkTest"



		attribute "batteryState", "string"
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
	
	input name: "infoLogging",  type: "bool", title: "Enable logging", defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", defaultValue: false
	input("delayTime",  "number", title: "Notify Timeout", description: "How many seconds to flash HELP Coming",defaultValue: 35,required: true)

    input name: "mode", type: "enum", title: "Mode", options: ["CARE", "KeyFOB"],description: "Use as CARE or 1 Button Keyfob", defaultValue: "CARE",required: true  
    
    
}


def installed() {
	// Runs after first pairing.
 
	logging("${device} : Paired!", "info")
}


//private getENCODING_BUTTON() { 0x0006 }


def initialize() {

// Testing is this needed? Because its not set right by default   
updateDataValue("inClusters", "00F0,00C0,00F3,00F5")
updateDataValue("outClusters", "00C0")

    
    state.icon = "<img src='data:image/jpeg;base64,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'>"
	state.batteryOkay = true
	state.operatingMode = "normal"
	state.presenceUpdated = 0
	state.rangingPulses = 0
//    state.deviceModel = "Unknown"

	// ...but don't arbitrarily reset the state of the device's main functions or tamper status.

	sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryState", value: "unknown", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
//	sendEvent(name: "lqi", value: 0)
//	sendEvent(name: "operation", value: "unknown", isStateChange: false)
	sendEvent(name: "presence", value: "not present")

	// Remove old settings. debugging
    state.remove("message")
	state.remove("care")	
	// Remove unnecessary device details.


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

	// Schedule our ranging report. ( using 12 hrs for keyfobs)
	int checkEveryHours = 12									           
    //  6 hours or every 1 hour for outlets.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)	
    // At X seconds past X minute, every checkEveryHours hours, starting at Y hour.

	// Schedule the presence check.
	int checkEveryMinutes = 1																					
    // Check presence timestamp every 6 minutes or every 1 minute for key fobs.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)									
    // At X seconds past the minute, every checkEveryMinutes minutes.

	// Configuration complete.
	logging("${device} : Configured", "info")

	// Run a ranging report and then switch to normal operating mode.
	rangingMode()
	runIn(12,normalMode)
	
}

def updated() {

	// Runs whenever preferences are saved.

	loggingStatus()
	runIn(5000,debugLogOff)
	runIn(5000,traceLogOff)
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
	logging("${device} : Received Unknown: clusterId: ${map.clusterId}, attrId: ${map.attrId}, command: ${map.command} with value: ${map.value} and ${receivedDataCount}data: ${receivedData}", "warn")
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
	sendEvent(name: "battery", value:0, unit: "%", isStateChange: false)
	sendEvent(name: "batteryVoltage", value: 0, unit: "V", isStateChange: false)
	sendEvent(name: "operation", value: "quiet")


	logging("${device} : Mode : Quiet", "info")

	refresh()

}



void refresh() {

	logging("${device} : Refreshing", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])	   // version information request

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
	// AlertMe devices check in with some sort of report at least every 2 minutes (every minute for fobs).



	presenceTimeoutMinutes = 2
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

				logging("${device} : Presence : Ignoring overdue presence reports for ${uptimeAllowanceMinutes} minutes. The hub was rebooted ${hubUptime} seconds ago.", "debug")

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
	// Primary parse routine.
    clientVersion()
	logging("${device} : Parse : $description", "debug")
	sendEvent(name: "presence", value: "present")
	updatePresence()
	Map descriptionMap = zigbee.parseDescriptionAsMap(description)
	if (descriptionMap) {
		processMap(descriptionMap)
	} else {
		logging("${device} : Parse : Failed to parse received data. Please report these messages to the developer.", "warn")
	}
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
sendSound(0) 
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
	logging ("${device} : ${map}","trace")
	String[] receivedData = map.data	
    logging("${device} : Cluster:${map.clusterId} CMD:${map.command} MAP:${map.data}","trace")


     if (map.clusterId == "0013"){
	logging("${device} : Device announce message (new device?)","warn")   
	logging("${device} : 0013 CMD:${map.command} MAP:${map.data} Anouncing New Device?","debug")


    } else if (map.clusterId == "0006") {
		logging("${device} : Sending Match Descriptor Response","debug")
		sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x8006 {00 00 00 01 02} {0xC216}"])

    
     } else if (map.clusterId == "00C0") {    
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
           
  
           }
        else { reportToDev(map) }// If we get anything else report it.
        
    } else if (map.clusterId == "00F0") {
       // AlertMe General Cluster  
       if (map.command == "FB") {
     
       batRec = receivedData[0]//1C=nobat 1D=batdata
//       test = receivedData[9]
//       testN = hexToBigDecimal(test)
       timmer = receivedData[3]
       timmerN = hexToBigDecimal(timmer) 
       if (state.Command =="Walk Test"){ logging("${device} : Running Walk test (expermential) ${timmerN}", "info") }    
       switchRec = receivedData[4]
       lqiRec = receivedData[8]// this is not 
           
 
        // if 1D we have batt   
		// Report the battery voltage and calculated percentage.
		def batteryVoltageHex = "undefined"
		BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
           
        if (batteryVoltageHex != "FFFF") {// if not FFF check bat
        batteryVoltage = zigbee.convertHexToInt(batteryVoltageHex) / 1000
		batteryVoltage = batteryVoltage.setScale(2, BigDecimal.ROUND_HALF_UP)
//        logging("${device} : Raw Battery  Bat:${batRec} ${batteryVoltage}", "trace")  


           

		BigDecimal batteryPercentage = 0
		BigDecimal batteryVoltageScaleMin = 2.1
		BigDecimal batteryVoltageScaleMax = 3.0


			batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
			batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
			batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
           
        if (state.lastBattery != batteryVoltage){
	     logging( "${device} : Battery : ${batteryPercentage}% (${batteryVoltage} V)","info")
	     sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
     	 sendEvent(name: "battery", value:batteryPercentage, unit: "%")
         
         
         if (batteryPercentage > 20) {  
             sendEvent(name: "batteryState", value: "ok")
             state.batteryOkay = true
             }
            
         if (batteryPercentage < 20) {
             logging("${device} : Battery LOW : $batteryPercentage%", "debug")
             sendEvent(name: "batteryState", value: "low")
             state.batteryOkay = false
         }
  
	 if (batteryPercentage < 10) {
            logging("${device} : Battery BAD: $batteryPercentage%", "debug") 
	    state.batteryOkay = false
	    sendEvent(name: "batteryState", value: "exhausted")
	}
        state.lastBattery = batteryVoltage     
    }
        }// end valid bat report Was not FFFF       
           

 // Temp data always 0000          
           
           
  }// end FB
        else { reportToDev(map) }       
  

     
     
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
				if (state.rangingPulses > 2) {  // this looks to large reducing
					"${state.operatingMode}Mode"()
				}

			} else if (receivedData[1] == "FF") {

				// This is the ranging report received every 30 seconds while in quiet mode.
				logging("${device} : quiet ranging report received", "debug")

			} else if (receivedData[1] == "00") {

				// This is the ranging report received when the device reboots.
				// After rebooting a refresh is required to bring back remote control.
				logging("${device} : Rebooted -- ranging report received", "warn")
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


			String deviceManufacturer = "Iris/AlertMe"
//			String deviceModel = "" 
			String deviceFirmware = versionInfoBlocks[versionInfoBlockCount - 1]

			// Sometimes the model name contains spaces.
			if (versionInfoBlockCount == 2) {
				deviceModel = versionInfoBlocks[0]
			} else {
				deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
			}
            logging("${device} : version received in ${versionInfoBlockCount} blocks : ${versionInfoDump}", "trace")
            logging("${device}: Firmware: ${deviceFirmware} Model: ${deviceModel} ", "info")
            updateDataValue("model", deviceModel)

            //version received in 4 blocks : [Care, Pendant, Device, 2012-09-20]          
if (deviceModel == "Care Pendant Device"){state.deviceModel = "KEY800"}//2012-09-20 <-known firmware

            
			updateDataValue("manufacturer", deviceManufacturer)
         	updateDataValue("firmware", deviceFirmware)

        }
        
	} else {
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
