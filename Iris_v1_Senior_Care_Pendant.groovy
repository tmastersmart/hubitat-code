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
v2.2 11/12/2022 Updated pressence code and rewrits to match other iris code.
v2.0 10/30/2022 New presence routine
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

Pair all keypads before installing this driver or they will mispar as a fob

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
    TheVersion="2.2.0"
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
// Pair all keypads before installing this driver 
//		fingerprint profileId: "C216", endpointId:"02", inClusters:"00F0,00C0", outClusters:"00C0", manufacturer: "Iris/AlertMe", model:"Care Pendant Device", deviceJoinName: "Iris v1 Senior Care Pendant"
		
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
	logging("Paired!", "info")
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
    logging("Initialize", "info")
	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.
	// Reset states...
    // Testing is this needed? Because its not set right by default   
updateDataValue("inClusters", "00F0,00C0,00F3,00F5")
updateDataValue("outClusters", "00C0")

    


	state.rangingPulses = 0
    state.lastCheckin = now()
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
    logging("Initialised Refreash in ${randomSixty}sec", "info")

}

def configure() {
    logging("${device} : configure", "info")
    
    // upgrade to new min values
	if (state.minVoltTest < 2.1 | state.minVoltTest > 2.25 ){ 
		state.minVoltTest= 2.25 
		logging("Min voltage set to ${state.minVoltTest}v Let bat run down to 0 for auto adj to work.", "info")
	}
	state.model = "-model?-"

    
    
	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.
    
	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("batteryOkay")
    state.remove("Config")
    state.remove("presenceUpdated")
    
    getIcons()
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


void refresh() {
    logging("Refreshing ${state.model} v${state.version}", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}

def ping() {
    logging("ping", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}
// 3 seconds mains 6 battery  2 flash good 3 bad
def rangeAndRefresh() {
    logging("StartMode : [Ranging]", "info")
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
   logging("Button ${buttonNumber}","info")
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
  logging("Alarm set OFF delay:${delayTime}", "info")    
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
  logging("Clear  mode:${mode}", "info") 
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
  logging("Care Help button pressed. Notified:${state.Command} ", "warn")    
}

def notifyHelpComing(){

  state.Command = "helpCalled" 
  sendIrisCmd (3)//called
  sendEvent(name: "care", value: "Help Ack", isStateChange: true) 
  logging("Care Help button Notified:${state.Command} ", "warn")
}

def strobe(cmd){return}
def both(cmd){return}




def checkPresence() {
    // presence routine. v5.1 11-12-22
    // simulated 0% battery detection
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 10
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
         sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)    
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





def WalkTest(){
logging("Starting walk test.", "warn")
logging("if you dont see a flash its out of range.", "info")    
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
logging("Ping Walk test Running.", "info")     
cluster = 0x00C0
attributeId = 0x022
dataType = DataType.ENUM8
sendZigbeeCommands(zigbee.writeAttribute(cluster, attributeId, dataType, 0x01, [destEndpoint :0x02]))
logging("Ping Walk test Running.", "info")      
}

def beep(cmd){
logging ("Sending Beep","info")    
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
logging ("Sending Care state [${state.Command}] to Pendient","info")
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

	if (map.clusterId == "00F0") {
     if (map.command == "FB") {
		// Device status cluster.
		def temperatureValue  = "NA"
        def batteryVoltageHex = "NA"
        BigDecimal batteryVoltage = 0
		batteryVoltageHex = receivedData[5..6].reverse().join()
        temperatureValue = receivedData[7..8].reverse().join()

     // some sensors report bat and temp at diffrent times some both at once?
     if (batteryVoltageHex != "FFFF") {
     	batteryVoltageRaw = zigbee.convertHexToInt(batteryVoltageHex) / 1000
    	batteryVoltage = batteryVoltageRaw.setScale(2, BigDecimal.ROUND_HALF_UP) // changed to x.xx from x.xxx
        if (batteryVoltage < state.minVoltTest){
            if (state.minVoltTest > 2.17){ 
                state.minVoltTest = batteryVoltage
                logging("Min Voltage Lowered to ${state.minVoltTest}v", "info")  
            }                             
        } 
		BigDecimal batteryPercentage = 0
        BigDecimal batteryVoltageScaleMin = state.minVoltTest 
		BigDecimal batteryVoltageScaleMax = 3.00  // 3.2 new battery
		batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
        powerLast = device.currentValue("battery")
        logging("battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V", "debug")
        if (powerLast != batteryPercentage){
           sendEvent(name: "battery", value:batteryPercentage, unit: "%")
           sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V", descriptionText: "Volts:${batteryVoltage}V MinVolts:${batteryVoltageScaleMin} v${state.version}")    
           logging("Battery:${batteryPercentage}% ${batteryVoltage}V", "info")

          if ( batteryVoltage < state.minVoltTest){state.minVoltTest = batteryVoltage}  // Record the min volts seen working      
         } // end dupe events detection
          
        }// end battery report        

    }// end FB
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
 			 if (state.rangingPulses > 7) {
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
    state.model = deviceModel        
    // Moved to debug because of to many events
            logging("${deviceModel} Ident: Firm:[${deviceFirmware}] ${reportFirm} Driver v${state.version}", "debug")
            if(!state.Config){
            state.Config = true    
			updateDataValue("manufacturer", deviceManufacturer)
            if (deviceModel == "Care Pendant Device"){state.deviceModel = "KEY800"}//2012-09-20 <-known firmware    
            updateDataValue("device", deviceModel)

            updateDataValue("firmware", deviceFirmware)
            updateDataValue("fcc", "WJHWD11")
                }   
		} 

// Standard IRIS USA Cluster detection block v4

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




private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}

void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
//    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/iris-v1-contact.jpg' >"
    state.icon = "<img src='data:image/jpeg;base64,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'>"
   
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
