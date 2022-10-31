/** Zigbee Sonoff - generic relays
driver for hubitat

Sonoff MINI ZB ,eWeLink ,3A Smart Home ,Generic
Generic zigbee relays
Lamp_01, SA-003-Zigbee, 01MINIZB, BASICZBR3, LXN59-1S7LX1.0


This driver was created to handel all my Sonoff MINI ZB / eWeLink /3A Smart Home /Generic relays.
These relays all use the same formats but have diffrent problems with internal drivers.

Suports alarm,strobe,siren,refreash and presence.

Send me your fingerprints so they can be added.

NOTES:
If you are switching from another driver you must FIRST switch to internal driver (zigbee generic outlet)
and press config. This repairs improper binding from other drivers. Otherwise you will get a lot of unneeded traffic.
---------------------------------------------------------------------------------------------------------
 1.5.4 10/30/2022   Store last status human form. Polling Options added
 1.5.3 10/30/2022   More minor rewrites.
 1.5.1 10/29/2022   Rewrote on off detection / Model detection/ Poll routine
 1.4.1 10/29/2022   Timeout changed
 1.4.0 10/27/2022   Parsing changes
 1.3.3 10/26/2022   Bug fix line 330
 1.3.2 10/26/2022   Option to disable button report on some relays
 1.3.1 10/23/2022   Bug fixes more untrapted cluster fixes
 1.3.0 10/24/2022   Minor logging and on off code rewriten
 1.2.3 10/23/2022   Bug fixes more untrapted cluster fixes
 1.1.0 10/23/2022   more fingerprintrs added eWeLink - no name - 3A Smart Home
 1.0.0 10/23/2022   Creation
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
may contain bits of code from some of the following.
https://github.com/tmastersmart/hubitat-code/blob/main/opensource_links.txt
 *	
 */
def clientVersion() {
    TheVersion="1.5.4"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils
metadata {
    
	definition (name: "Zigbee - Sonoff - generic relays", namespace: "tmastersmart", author: "tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/generic-zigbee-relays.groovy") {

		capability "Actuator"
		capability "Configuration"
		capability "EnergyMeter"
		capability "Initialize"
		capability "Outlet"
		capability "PresenceSensor"
		capability "Refresh"
		capability "Switch" 
        capability "Alarm"



        command "unschedule" 
        command "uninstall"
        command "checkPresence"


		attribute "strobe", "string"
		attribute "siren", "string"

        fingerprint model:"BASICZBR3",     manufacturer:"SONOFF",          deviceJoinName:"SONOFF Relay BASICBR3", profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006",outClusters:"0000"
	    fingerprint model:"01MINIZB",      manufacturer:"SONOFF",          deviceJoinName:"SONOFF Relay MINI",     profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,FC57",outClusters:"0019"	
        fingerprint model:"SA-003-Zigbee", manufacturer:"eWeLink",         deviceJoinName:"eWeLink Relay",         profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000"
        fingerprint model:"Lamp_01",       manufacturer:"SZ",              deviceJoinName:"Generic Relay",         profileId:"0104", endpointId:"0B", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000", application:"01"
        fingerprint model:"LXN59-1S7LX1.0",manufacturer:"3A Smart Home DE",deviceJoinName:"Inline Switch",         profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006", outClusters:"", application:"01"
    }

}


//https://zigbee.blakadder.com/Zemismart_ZW-EU-01.html

preferences {
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

	input name: "resendState",  type: "bool", title: "Resend Last State on Refresh", description: "If Refresh does not wake up your device use this", defaultValue: false
    input name: "pollHR" ,	    type: "enum", title: "Check Presence Hours",description: "Chron Schedule. Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: "10",required: true 
//    input name: "timeOut" ,	    type: "enum", title: "Timeout in mins",description: "",options: ["40","80","100","200","250"], defaultValue: "100",required: true 
    
}


def installed() {
	// Runs after first pairing. this may never run internal drivers overide pairing.
	logging("${device} : Paired!", "info")
    state.DataUpdate = false 
    initialize()
}

def uninstall() {
	unschedule()
	state.remove("presenceUpdated")    
	state.remove("version")
    state.remove("checkPhase")
    state.remove("lastCheckInMin")
    state.remove("logo")
    state.remove("bin")
    state.remove("DataUpdate")
    state.remove("lastCheckin")
    state.remove("lastPoll")
    state.remove("donate")
    state.remove("model")

    logging("Uninstalled", "info")  
}

def initialize() {
   logging("initialize", "info") 
    // This runs on reboot 
	state.presenceUpdated = 0

	// Remove disused state variables from earlier versions.
state.remove("status")
state.remove("comment")    
state.remove("icon")
state.remove("logo")
state.remove("flashing")    
state.remove("timeOut")
	// Remove unnecessary device details.
    device.deleteCurrentState("alarm")
    configure()
    clientVersion()
    
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)

}


def configure() {
	// Runs on reboot paired or rejoined
	unschedule()
	state.poll = pollHR
    if (!state.poll){ state.poll= 10}
    // Schedule presence in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${state.poll} * * ? *", checkPresence)	
    logging("Configure - Presence Check Every ${state.poll}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
     
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
    getIcons()
}





void refresh(cmd) {
    if(state.DataUpdate){ logging("Refreshing MFR:${state.MFR} Model:${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}
delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000)),
   ], 1000)    

    
    if(resendState){
      Test = device.currentValue("switch")  
      if (Test =="on"){runIn(4,on)}
      else {runIn(4,off)} 
    }
}






def alarm(cmd){
    logging("Alarm ON", "info")
    sendEvent(name: "alarm", value: "on")
  on()
}
                   
def siren(cmd){
    logging("siren ON", "info")
    sendEvent(name: "siren", value: "on")
  on()
}
def strobe(cmd){
    logging("strobe ON", "info")
    sendEvent(name: "strobe", value: "on")
  on()
}
def both(cmd){
    logging("both ON", "info")
    sendEvent(name: "siren", value: "on")
    sendEvent(name: "strobe", value: "on")
  on()
}

def off() {
    logging("Sending OFF", "info")
	zigbee.command(0x006, 0x00)
}

def on() {
    logging("Sending ON", "info")
	zigbee.command(0x006, 0x01)
}


private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}
private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}


def checkPresence() {
    // New shorter presence routine. v2 10-30-22
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 200// [not present] and 0 batt
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins Timeout:${timeOut}","debug")
    if (state.lastCheckInMin <= checkMin){ 
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
            logging("Creating presence event: ${value}  ","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin) { 
        test = device.currentValue("presence")
        if (test != "not present"){
        value = "not present"
        logging("Creating presence event: ${value} ${state.lastCheckInMin} min ago","warn")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        runIn(6,refresh)
        }
    }

}

def enrollResponse() {
    logging("Sending enroll response", "info")
	String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
   
    delayBetween([
            sendZigbeeCommands(["zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}"]),// send CIE
            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 ${endpointId}"]),
            sendZigbeeCommands(["raw 0x500 {01 23 00 00 00}"]),// enrole res
            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 1"]),	
   ], 1000)
    
}

def parse(String description) {
    state.lastCheckin = now()
    checkPresence()
    logging("Raw: [${description}]", "trace")
    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("${descMap}", "debug")

    if (description?.startsWith('enroll request')) { 
        enrollResponse()
        return  
    }  
    


    if (descMap) {processMap(descMap)}
        else{
            logging("Error ${description} ${descMap}", "error") 
        }
	
}





//profileId:0104, clusterId:0006, clusterInt:6, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:697F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]]
def processMap(Map map) {
	String[] receivedData = map.data

    // fix parse Geting 2 formats so merge them
    if (map.clusterId) {map.cluster = map.clusterId} 
   
    if (map.cluster == "0006" | map.clusterId == "0006" ) {
      if (map.value){status = map.value}
      if (map.data) {status = map.data[0]} 
      logging("ON/OFF report", "debug")
      if (status == "01"){onEvents()}
      if (status == "00"){offEvents()}
     

}else if (map.cluster == "0000" ) {
        if (map.attrId== "0004" && map.attrInt ==4){
        logging("Manufacturer :${map.value}", "debug") 
        state.MFR = map.value 
        updateDataValue("manufacturer", state.MFR)
        state.DataUpdate = true                     
        } 
        if (map.attrId== "0005" && map.attrInt ==5){
        logging("Model :${map.value}", "debug")
        state.model = map.value    
        updateDataValue("model", state.model)
        state.DataUpdate = true    
        }
       
        
//New unknown Cluster Detected: clusterId:8001, attrId:null, command:00, value:null data: [B6, 00, 37, EE, C8, 24, 00, 4B, 12, 00, 97, 36]        
   }else if (map.cluster == "8001" ) { 
        logging("General event :8001 ${map.data}", "debug") 
        logging("Device may need CONFIG on internal drivers to stop this cluster", "debug") 
   }else if (map.cluster == "8021") {
        logging("Blind Cluster event :8021 ${map.data}", "debug")
   }else if (map.cluster == "8038") {
        logging("General Catchall :8038 ${map.data}", "debug")      
   }else if (map.cluster == "0013") {
        logging("Device Announcement Cluster ${map.data}", "warn")      
    
   }else {
        logging("New unknown Cluster Detected: ${map}", "warn")
    
    }
    

}



// prevents dupe events
def onEvents(){
    alarmTest = device.currentValue("switch")
    if (alarmTest != "on"){sendEvent(name: "switch", value: "on")}    
    logging("is ON our state was:${alarmTest}", "info")

}

def offEvents(){
    alarmTest = device.currentValue("alarm")   
    if (alarmTest != "off"){sendEvent(name: "alarm",  value: "off")}
    alarmTest = device.currentValue("siren") 
    if (alarmTest != "off"){sendEvent(name: "siren",  value: "off")} 
    alarmTest = device.currentValue("strobe") 
    if (alarmTest != "off"){sendEvent(name: "strobe", value: "off")}
    alarmTest = device.currentValue("switch")
    if (alarmTest != "off"){sendEvent(name: "switch", value: "off")}
    logging("is OFF our state was:${alarmTest}", "info")
   
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

String integerToHexString(BigDecimal value, Integer minBytes, boolean reverse=false) {
    return integerToHexString(value.intValue(), minBytes, reverse=reverse)
}

String integerToHexString(Integer value, Integer minBytes, boolean reverse=false) {
    if(reverse == true) {
        return HexUtils.integerToHexString(value, minBytes).split("(?<=\\G..)").reverse().join()
    } else {
        return HexUtils.integerToHexString(value, minBytes)
    }
    
}



private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}

void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    if (state.model == "BASICZBR3"){     state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/BASICZBR3.jpg' >"}
    if (state.model == "01MINIZB"){      state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/01MINIZB.jpg' >"  }                                  
    if (state.model == "SA-003-Zigbee"){ state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/Lamp_01.jpg' >"}
    if (state.model == "Lamp_01"){       state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/Lamp_01.jpg' >"}                             
    if (state.model == "LXN59-1S7LX1.0"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/LXN59-1S7LX1.0.jpg' >"}
 }

// Logging block v4 10/24/2022
//	
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    if (debugLogging){runIn(4000,debugLogOff)}
    if (traceLogging){runIn(1000,traceLogOff)}
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
