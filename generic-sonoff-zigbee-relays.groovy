/** Zigbee Sonoff - generic relays
driver for hubitat

Sonoff MINI ZB / eWeLink /3A Smart Home /Generic
Generic zigbee relays/outlets...


This driver was created to handel all my Sonoff MINI ZB / eWeLink /3A Smart Home /Generic relays.
These relays all use the same formats but have diffrent problems with internal drivers.

Suports alarm,strobe,siren,refreash and presence.

Send me your fngerprints so they can be added.

NOTES:
If you are switching from another driver you must FIRST switch to internal driver (zigbee generic outlet)
and press config. This repairs improper binding from other drivers. Otherwise you will get a lot of unneeded traffic.
---------------------------------------------------------------------------------------------------------


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
    TheVersion="1.3.2"
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
        command "enrollResponse"


		attribute "strobe", "string"
		attribute "siren", "string"


        fingerprint model:"BASICZBR3",     manufacturer:"SONOFF",          deviceJoinName:"SONOFF Relay BASICBR3", profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006",outClusters:"0000"
	    fingerprint model:"01MINIZB",      manufacturer:"SONOFF",          deviceJoinName:"SONOFF Relay MINI",     profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,FC57",outClusters:"0019"	
        fingerprint model:"SA-003-Zigbee", manufacturer:"eWeLink",         deviceJoinName:"eWeLink Relay",         profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000"
        fingerprint model:"Lamp_01",       manufacturer:"SZ",              deviceJoinName:"Generic Relay",         profileId:"0104", endpointId:"0B", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000", application:"01"
        fingerprint model:"LXN59-1S7LX1.0",manufacturer:"3A Smart Home DE",deviceJoinName:"Inline Switch",         profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006", outClusters:"", application:"01"
    }

}

preferences {
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

    input name: "buttonDetect", type: "bool", title: "Ignore Button Press", description: "Some generic relays use button cluster as a status report. Just report as status not button.", defaultValue: false
	
}


def installed() {
	// Runs after first pairing. this may never run internal drivers overide pairing.
	logging("${device} : Paired!", "info")
}

def uninstall() {
	unschedule()
	state.remove("presenceUpdated")    
	state.remove("version")
    logging("Uninstalled", "info")   
    state.remove("checkPhase")
    state.remove("lastCheckInMin")
    state.remove("logo")
    state.remove("bin")
    state.remove("DataUpdate")
    state.remove("lastCheckin")

}

def initialize() {

    // This runs on reboot 
	state.presenceUpdated = 0
//    state.logo =""

	// Remove disused state variables from earlier versions.
state.remove("status")
state.remove("comment")    
state.remove("icon")
state.remove("logo")
state.remove("flashing")    

	// Remove unnecessary device details.
    device.deleteCurrentState("alarm")

    clientVersion()
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
}


def configure() {
	// Runs on reboot paired or rejoined
    logging("configure", "info")  
    state.DataUpdate = false  
	unschedule()
	
    // Schedule presence in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${12} * * ? *", checkPresence)	
   

}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    refresh() 
}

void reportToDev(map) {
	String[] receivedData = map.data
	logging("New unknown Cluster Detected: clusterId:${map.clusterId}, attrId:${map.attrId}, command:${map.command}, value:${map.value} data: ${receivedData}", "warn")
}




void refresh(cmd) {
	logging("Refreshing", "info")
    Test = device.currentValue("switch")
    if (Test =="on"){runIn(4,on)}
    else {runIn(4,off)}    
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
    // New shorter presence routine.
    // Runs on every parse and a schedule.
    def checkMin  = 5  // 5 min warning
    def checkMin2 = 10 // 10 min [not present] and 0 batt
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins","debug")
    if (state.lastCheckInMin <= checkMin){ 
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
        logging("Creating presence event: ${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        return    
        }
    }
    if (state.lastCheckInMin >= checkMin){ 
        logging("Sensor timing out ${state.lastCheckInMin} min ago","warn")
        runIn(60,refresh)// Ping Perhaps we can wake it up...
    }
    if (state.lastCheckInMin >= checkMin2) { 
        test = device.currentValue("presence")
        if (test != "not present"){
        value = "not present"
        logging("Creating presence event: ${value} ${state.lastCheckInMin} min ago","warn")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        runIn(60,refresh) 
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
        // we should never get here reportToDev is in processMap above
            logging("Error ${description} ${descMap}", "debug") 

        }
	
}






def processMap(Map map) {
	String[] receivedData = map.data
 
//    logging("PARSE [${map}]", "trace")
// relay report
    if (map.clusterId == "0006" && map.profileId == "0104"  ){
        logging("ON/OFF report command:${map.command} data:${map.data}", "debug")
      status  = map.data[0]
        if (status == "01"){onEvents()}
        if (status == "00"){offEvents()}
      status = map.data[1] 
        if (status == "01"){logging("2nd data field report ${status}", "warn")} 
    } 
// button report 
    else if (map.cluster == "0006") {
 //       logging("cluster:${map.cluster} command:${map.command} value:${map.value}", "debug")
        
      if(!buttonDetect){
        press(1)
        runIn(2,Release(1))
      }
        
        status = map.value
        if (status == "01"){onEvents()}
        if (status == "00"){offEvents()}
        
        
        
//New unknown Cluster Detected: clusterId:8001, attrId:null, command:00, value:null data: [B6, 00, 37, EE, C8, 24, 00, 4B, 12, 00, 97, 36]        
   }else if (map.cluster == "8001" | map.clusterId == "8001") { 
        logging("General event :8001 ${map.data}", "debug") 
        logging("Device may need CONFIG on internal drivers to stop this cluster", "debug") 
   }else if (map.cluster == "8021"| map.clusterId == "8021" ) {
        logging("Blind Cluster event :8021 ${map.data}", "debug")
   }else if (map.cluster == "8038"| map.clusterId == "8038") {
        logging("General Catchall :8038 ${map.data}", "debug")      
   }else if (map.cluster == "0013"| map.clusterId == "0013") {
        logging("Multistate event: UNKNOWN ${map.data}", "warn")      
    
	} else {
		reportToDev(map)// unknown cluster
	}
	return null
}

// prevents dupe events
def onEvents(){
    alarmTest = device.currentValue("switch")
    if(alarmTest != "on"){sendEvent(name: "switch", value: "on")}    
    logging("is ON [digital] our state was:${alarmTest}", "info")
}

def offEvents(){
    alarmTest = device.currentValue("alarm")   
    if(alarmTest != "off"){sendEvent(name: "alarm",  value: "off")}
    alarmTest = device.currentValue("siren") 
    if(alarmTest != "off"){sendEvent(name: "siren",  value: "off")} 
    alarmTest = device.currentValue("strobe") 
    if(alarmTest != "off"){sendEvent(name: "strobe", value: "off")}
    alarmTest = device.currentValue("switch")
    if(alarmTest != "off"){sendEvent(name: "switch", value: "off")}
    logging("is OFF [digital] our state was:${alarmTest}", "info")
}
    
def press(cmd){
logging("Button pressed", "info")    
sendEvent(name: "pushed", value: cmd, isStateChange: true)    
}

def Release(cmd){
logging("Button released", "info")    
sendEvent(name: "released", value: cmd, isStateChange: true)    
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
