/* Iris v1 KeyFob Advanced

Iris v1 Button for hubitat

The iris v1 keyfob supports holdable buttons




https://fccid.io/WJHKF11
Centrica Connected home Limited Alertme Wireless keyfob 


support for scripts
---------------------------------------------
supports press held 

event: pushed
1 = pressed home
2 = pressed away

event: held
3 = home
4 = away    
(in testing away often had false hold states due to the processor slowing down)

Or you may test custom att with your scripts. Mode will change when button is pressed and stay until pressed again.

mode = Away
mode = Home

---------------------------------------------



v1.1.1 11/23/2022  First release Using current contact sensor code
=================================================================================================



Before going back to internal drivers you must use uninstall to stop chron
=================================================================================================== 
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



May contain code from the following


Iris v2 source code driver here 2.3 driver
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/platform/arcus-containers/driver-services/src/main/resources/ZB_AlertMe_KeyFob.driver
https://github.com/arcus-smart-home/arcusplatform/blob/a02ad0e9274896806b7d0108ee3644396f3780ad/common/arcus-protocol/src/main/irp/ame-general.irp





code includes some routines based on alertme UK code from  

GNU General Public License v3.0
Permissions of this strong copyleft license are conditioned on making available
complete source code of licensed works and modifications, which include larger
works using a licensed work, under the same license. Copyright and license
notices must be preserved. Contributors provide an express grant of patent rights.
Uk Iris code 
   https://github.com/birdslikewires/hubitat/blob/master/alertme/drivers/alertme_contact.groovy
 *	
 */

def clientVersion() {
    TheVersion="1.2.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.helper.HexUtils


metadata {

	definition (name: "Iris v1 KeyFob Advanced", namespace: "tmastersmart", author: "Tmaster", importUrl: "") {

	capability "Battery"
	capability "Configuration"
	capability "Initialize"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
	capability "SignalStrength"

    capability "Pushable Button"
    capability "ReleasableButton"
    capability "Holdable Button"    
 


	command "checkPresence"
	command "normalMode"
    command "rangeAndRefresh"

    command "ClearTamper"
    command "unschedule"
    command "uninstall"


	attribute "batteryVoltage", "string"
	attribute "mode", "string"


//        fingerprint model:"Button Device", manufacturer:"AlertMe.com", profileId:"C216", endpointId:"02", inClusters:"00F0,00F3,00F2,00F1", outClusters:""
       fingerprint model:"Keyfob Device", manufacturer:"AlertMe.com", profileId:"C216", endpointId:"02", inClusters:"00F0,00F3,00F4,00F1", outClusters:"" 

	}

}


preferences {
	
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    
//   input name: "minTime" ,type: "enum", title: "Presence timeout",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 15 ,required: true 
	input("minTime",  "number", title: "Presence Timeout", description: "Time in mins driver sets not present",defaultValue: 15,required: true)
   

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
    state.remove("mode"),  
    state.remove("tempAdj"),
	state.remove("rangingPulses"),
	state.remove("operatingMode"),
	state.remove("batteryOkay"),
	state.remove("battery"),
    state.remove("LQI"),
    state.remove("batteryOkay"),
    state.remove("Config"),
    state.remove("batteryState"), 
    state.remove("reportToDev"),
    state.remove("tries"),
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
      
// Works better with a delay. Some were not getting removed      
      
}



def initialize() {

	// Set states to starting values and schedule a single refresh.
	// Runs on reboot, or can be triggered manually.
	// Reset states...

	state.presenceUpdated = 0
	state.rangingPulses = 0
    state.lastPress =0
	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    
	// Remove unnecessary device details.
	removeDataValue("application")



    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 3500)
	runIn(randomSixty,refresh)
    logging("Initialised", "info")

}


def configure() {

	// Set preferences and ongoing scheduled tasks.
	// Runs after installed() when a device is paired or rejoined, or can be triggered manually.
    if(!state.minVoltTest){state.minVoltTest= 2.25}
	// upgrade to new min values
	if (state.minVoltTest < 2.1 | state.minVoltTest > 2.25 ){ 
		state.minVoltTest= 2.25 
		logging("Min voltage set to ${state.minVoltTest}v Let bat run down to 0 for auto adj to work.", "info")
	}
	state.model = "-model?-"
    state.lastPress = 0
	// Remove state variables from old versions.
    state.remove("operatingMode")
    state.remove("LQI")
    state.remove("batteryOkay")
    state.remove("batteryState") 
    state.remove("Config")
    state.remove("presenceUpdated")
    state.remove("hwVer")
    state.remove("reportToDev")    
    sendEvent(name:"numberOfButtons",value: 4 , descriptionText:"2 pushable 2 holdable ${state.version}")
    
    getIcons()
	unschedule()


	// Schedule randon ranging in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${6} * * ? *", rangeAndRefresh)	

    // Check presence every hr
//	randomSixty = Math.abs(new Random().nextInt() % 60)
//	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
//	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *", checkPresence)	

    startTimer()
    
    
    runIn(randomSixty,rangeAndRefresh)
    logging("configure", "info")
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
}


// useless mode shuts down the device? Unknown 
// ranging message every 2 minutes.
//void offMode() {
//	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 03 01} {0xC216}"])// shut off device
//	logging ("Mode: Quiet  [FA:03.01]","info")
//}

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

def ping(){
    logging("Ping", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}

void refresh() {
    logging("Refreshing ${state.model} v${state.version}", "info")
	sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F6 {11 00 FC 01} {0xC216}"])// version information request
}

// 3 seconds mains 6 battery  2 flash good 3 bad
def rangeAndRefresh() {
    logging("StartMode : [Ranging]", "info")
    sendZigbeeCommands(["he raw ${device.deviceNetworkId} 0 ${device.endpointId} 0x00F0 {11 00 FA 01 01} {0xC216}"]) // ranging
	state.rangingPulses = 0
	runIn(6, normalMode)
 
}

private startTimer() {
    unschedule(checkPresence)
    logging("Checking ever min","info")
    runEvery1Minute("checkPresence")
}

private stopTimer() {// Check presence every hr
    unschedule(checkPresence)
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *",checkPresence)	
    logging("Checking ever hr","info")
}

def checkPresence() {
    // presence routine. v5.1 11-12-22
    // simulated 0% battery detection
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    if(!minTime){ minTime = 10}
    def checkMin = minTime
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
        startTimer()    
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
         stopTimer()   
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




  
    

def ClearTamper (){
        logging("Tamper : Cleared FORCED", "info")
		sendEvent(name: "tamper", value: "clear", isStateChange: true, descriptionText: "force cleared v${state.version}")
}







// process zonealarm as button
def processStatus(ZoneStatus status) {
    logging("ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()} ", "debug")
}


void sendMode(){
    
sendEvent(name: "mode", value: name, descriptionText: "IRIS state ${name} button held! ${state.version}",translatable: true,isStateChange: true)

}


// this is for compatability
def hold(cmd){
  logging("held: ${name} ${cmd}", "info")  
  sendEvent(name: "held", value: cmd, descriptionText: "${cmd} button held! ${state.version}",translatable: true,isStateChange: true)
}


def released(cmd)
{
    logging("Button: released ", "info")
   sendEvent(name: "button", value: "released", descriptionText: "${cmd} button released! ${state.version}",translatable: true,isStateChange: true)
    
    release(cmd)
    
}


def release(cmd)
{

   logging("released: ${name} ${cmd}", "info")
   sendEvent(name: "released", value: cmd, isStateChange: true,descriptionText:"${cmd} button Pressed! ${state.version}",data: [buttonNumber: cmd]) 
}

def push(cmd){
    logging("pushed: ${name} ${cmd}", "info")
    sendEvent(name: "pushed", value: cmd, isStateChange: true,descriptionText:"${cmd} button Pressed! ${state.version}",data: [buttonNumber: cmd])
}


def parse(String description) {
	logging("Parse : ${description}", "trace")
    clientVersion()
    state.lastCheckin = now()
    checkPresence()
    // Device contacts are zigbee cluster compatable
	if (description.startsWith("zone status")) {
		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
		processStatus(zoneStatus)
        return
   }else if (description?.startsWith('enroll request')) {
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

//profileId:C216, clusterId:00F3, command:01,  data:[00, 01, 51, 14, 00, 00]]/    
//profileId:C216, clusterId:00F3, command:00,  data:[00, 02, 05, 15, 00, 00]]
    
    
 if (map.clusterId == "00F3"){ // button cluster
     timeSinceLastCheckin = (now() - state.lastPress ?: 0) / 100
     cmd1 = receivedData[0]
     button=2 // should be 1 but is 2 to be back comp with internal drivers/scripts
     name = "Home"
     if (cmd1 == "01" ){
         button=1
         name = "Away"
     }
     logging("PRESS command:${map.command} button:${button}  ${timeSinceLastCheckin}", "debug")
    if (map.command == "01") {
        state.lastPress = now()
        push(button)
        sendMode()
    } // wait for released to check for holding
    if (map.command == "00") {
       released(button) 
     if(timeSinceLastCheckin >=10){// how many seconds to hold
       button=4
       name = "Home"
       if (cmd1 == "01" ){
       button=3
       name = "Away"
       }   
      hold(button)    
      released(button)    
     }
   }    
    
}else if (map.clusterId == "00F0") {
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

 

 else if (map.clusterId == "00F6") {// Join Cluster 0xF6

		
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
            logging("Ranging ${state.rangingPulses} ", "debug")    
 			 if (state.rangingPulses > 6) {
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

            // taken from iris source code
            mfgIdhex = receivedData[11] + receivedData[10]  
            def mfgIddec = Integer.parseInt(mfgIdhex,16)
            dvcType = receivedData[13] + receivedData[12]
            def appRel = Integer.parseInt(receivedData[14],16)
            def appVer = Integer.parseInt(receivedData[15],16)
            hwVerHex = receivedData[17] + receivedData[16]
            def hwVer = Integer.parseInt(hwVerHex,16)
            
//            appVerHex = receivedData[15]
//            def appVerDec = Integer.parseInt(appVerHex,16)
//            appVer = new Double(appVerDec) /10
//            hwVer = new Double(receivedData[17]) + (new Double(receivedData[16]) / 10)
            String deviceFirmwareDate = versionInfoBlocks[versionInfoBlockCount - 1]
            firmwareVersion = "appV.appRel.hwV-" +appVer + "." + appRel + "." + hwVer+"-date-" + deviceFirmwareDate
            logging("Ident Block: ${versionInfoDump} ${firmwareVersion}", "trace")
    
    state.firmware =  appVer + "." + appRel + "." + hwVer 
			String deviceManufacturer = "IRIS"
			String deviceModel = ""
//Unknown firmware [appV.appRel.hwV-40.6.257-date-2012-02-03] 2012-02-03            
           reportFirm = "unknown"
          if(deviceFirmwareDate == "2012-02-03" ){reportFirm = "Ok"}
         if(reportFirm == "unknown"){state.reportToDev="Unknown firmware [${firmwareVersion}] ${deviceFirmwareDate}" }
          else{state.remove("reportToDev")}
			// Sometimes the model name contains spaces.
	if (versionInfoBlockCount == 2) {
	deviceModel = versionInfoBlocks[0]
	} else {
	deviceModel = versionInfoBlocks[0..versionInfoBlockCount - 2].join(' ').toString()
	}
   state.model = deviceModel
    // Moved to debug because of to many events
            logging("Ident:${deviceModel} Firm:[${firmwareVersion}] ${reportFirm} Driver v${state.version}", "debug")
            if(!state.Config){
            state.Config = true    
			updateDataValue("manufacturer", deviceManufacturer)
            updateDataValue("device", deviceModel)
            updateDataValue("model", "KEY800")
            updateDataValue("firmware", firmwareVersion)
            updateDataValue("fcc", "WJHKF11")
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
logging("send Zigbee :${cmds}", "trace")
sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}





private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}


void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/irisv1keyfob.jpg' >"

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
