/**
 *  SAGE Doorbell Sensor 
    Presence and bat support
    Hubitat driver. 



Adds battery support (simlated) if stops reporting bat goes to 0

White wire:  common
Green wire:  doorbell 1  front
Yellow wire: doorbell 2  rear

 Factory reset:
 Press and hold the RESET button until the red LED blinks.


================================================================================ 
v2.5.0  09/28/2022  Debug logs not auto disabling/Bug on line 285
v2.4.0  09/28/2022  Code cleanup.
v2.3    09/27/2022  Total rewrite of parse code.
v2.2    09/25/2022  Cleanup code
v2.1    09/24/2022  Presence schedule added
v2.0    09/24/2022  This fixes false button press after last hub update.
                  Simulated battery 







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



Main routines totaly rewritten but uses some source code from. 
https://github.com/trunzoc/Hubitat/blob/master/Drivers/Sage_Doorbell/Sage_Doorbell.groovy (11/27/2019)
https://github.com/rbaldwi3/Sage-HVAC-Sensor/edit/master/groovy

 */
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

def clientVersion() {
    TheVersion="2.5.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() // Forces config on updates
 }
}


metadata {
    definition (name: "Sage Doorbell echostar sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/sage_doorbell_echostar.groovy") {

		capability "Configuration"
        capability "Pushable Button"
        capability "ReleasableButton"
		capability "Refresh"
        capability "Battery"
        capability "PresenceSensor"
        
        command "checkPresence"
        command "enrollResponse"
        command "uninstall"

 	   
        fingerprint endpointId: "12", inClusters: "0000,0003,0009,0001", outClusters: "0003,0006,0008,0019", model: "Bell", manufacturer: "Echostar"
    }
//  fingerprint model:" Bell", manufacturer:" Echostar", profileId:"0104", endpointId:"12", inClusters:"0000,0003,0009,0001", outClusters:"0003,0006,0008,0019", application:"02"    
    preferences {
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

	input name: "timeBetweenPresses",type: "number",title: "Seconds allowed between presses (increase this value to eliminate duplicate notifications)",required: true,displayDuringSetup: true,defaultValue: 10
	input name: "button1Name",type: "text",title: "Doorbell name to be associated with the 'Front Door' contact...",required: false,defaultValue: "Front Door",displayDuringSetup: true
    input name: "button2Name",type: "text",title: "Doorbell name to be associated with the 'Rear Door' contact...",required: false, defaultValue: "Rear Door",displayDuringSetup: true
   }
 } 
def installed() {
	logging("${device} : Paired!", "info")
    configure()
    updated()
    runIn(20,refresh)
}


def uninstall() {
	unschedule()
    state.remove("lastCheckInMin") 
    state.remove("lastCheckin")
    state.remove("timeBetweenPresses")
    state.remove("version")
removeDataValue("presence")    
removeDataValue("battery")
}


def updated(){
   loggingUpdate()
   clientVersion() 
}

def configure() {
    state.remove("ignore01")
    state.remove("ignore00")
    state.remove("waitForGetInfo")
    state.remove("timeBetweenPresses")
    removeDataValue("softwareBuild")//00000009
    removeDataValue("firmwareMT")//1014-0002-00000009

    
    unschedule()
    loggingUpdate()
    // Schedule presence in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${12} * * ? *", checkPresence)	

    buttons = device.currentValue("numberOfButtons")
    if (buttons != 2){sendEvent(name: "numberOfButtons", value: 2, displayed: true)}

    if(!timeBetweenPresses){timeBetweenPresses = 10}
	String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
    logging("${device} : Configure", "info")

	def configCmds = [
		"zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}", "delay 200",
		"send 0x${device.deviceNetworkId} 1 1", "delay 500",

		"zdo bind 0x${device.deviceNetworkId} ${endpointId} 1 1 {${device.zigbeeId}} {}", "delay 500",
		"zcl global send-me-a-report 1 0x20 0x20 30 21600 {01}",		//checkin time 6 hrs
		"send 0x${device.deviceNetworkId} 1 1", "delay 500",

		"zdo bind 0x${device.deviceNetworkId} ${endpointId} 1 0x402 {${device.zigbeeId}} {}", "delay 500",
		"zcl global send-me-a-report 0x402 0 0x29 30 3600 {6400}",
		"send 0x${device.deviceNetworkId} 1 1", "delay 500"
	]
    return configCmds + refresh() // send refresh cmds as part of config
}
def checkPresence() {
    // New shorter presence routine.
    // Runs on every parse and a schedule.
    def checkMin  = 1400 // 24 hrs warning
    def checkMin2 = 2800 // 48 hrs [not present] and 0 batt
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
        sendEvent(name: "battery", value: 90, unit: "%",descriptionText:"Estimated ${state.version}", isStateChange: true)    
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
        sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"Estimated 0 ${state.version}", isStateChange: true) 
        runIn(60,refresh) 
        }
    }
}

def parse(String description) {
    state.lastCheckin = now()
    checkPresence()
    logging("${device} : Raw [${description}]", "trace")
    Map descMap = zigbee.parseDescriptionAsMap(description)
    logging("${device} : ${descMap}", "trace")
    logging("${device} : profileId:${descMap.profileId} clusterId:${descMap.clusterId} clusterInt:${descMap.clusterInt} sourceEndpoint${descMap.sourceEndpoint} destinationEndpoint${descMap.destinationEndpoint} options:${descMap.options} command:${descMap.command} data:${descMap.data}", "trace")
//profileId:0000 clusterId:0006 clusterInt:6 sourceEndpoint00 destinationEndpoint00 options:0040 command:00 data:[25, 00, 00, 04, 01, 01, 19, 00, 00]    
if (descMap.profileId == "0000" && descMap.clusterId == "0006" ){logging("${device} : Ping", "debug")}
//profileId:0104 clusterId:0006 clusterInt:6 sourceEndpoint12 destinationEndpoint01 options:0040 command:00 data:[]    
else  if (descMap.profileId == "0104" && descMap.clusterId == "0006" ){
            def buttonNumber = (descMap.command as int)
            if (buttonNumber == 1 && !isDuplicateCall(state.lastButton1Updated, timeBetweenPresses)){	
                push(1)
                state.lastButton1Updated = new Date().time 
            }
            if (buttonNumber == 0 && !isDuplicateCall(state.lastButton2Updated, timeBetweenPresses)){ 
                push(2)
                state.lastButton2Updated = new Date().time	
            }
           
        }
// profileId:0104 clusterId:0500 clusterInt:1280 sourceEndpoint12 destinationEndpoint01 options:0040 command:04 data:[86, 10, 00] 
else  if (descMap.profileId == "0104" && descMap.clusterId == "0500" ){logging("${device} : IAS Zone", "debug")}   
//[raw:F7F61200010A20002000, dni:F7F6, endpoint:12, cluster:0001, size:0A, attrId:0020, encoding:20, command:01, value:00, clusterInt:1, attrInt:32]    
else if (descMap.cluster == "0001" && descMap.attrId == "0020") {//Power configuration
        battery = Integer.parseInt(descMap.value, 16)
        logging("${device} : Battery:${battery} FALSE data ignored", "debug")
	}
    
    
else if (descMap.cluster == "0000" && descMap.attrId == "0004") {
    logging("${device} : Manufacturer:${descMap.value} ", "info")
    updateDataValue("manufacturer", descMap.value)
}
else if (descMap.cluster == "0000" && descMap.attrId == "0005") {
    logging("${device} : device:${descMap.value} ", "info")
    updateDataValue("device", descMap.value)
    updateDataValue("partNo", "206612")
    updateDataValue("model", "DRBELL")
    updateDataValue("fcc", "DKN-401DM")
}    
    
// Raw [catchall: 0000 8021 00 00 0040 00 F7F6 00 00 0000 00 00 B500]
//else if (descMap.cluster == "0000" && descMap.attrId == "8021") {logging("${device} : Unknown Event ", "debug")}
else if (descMap.cluster == "0000" && descMap.attrId == "8031") {logging("${device} : Link Quality Cluster Event ", "debug")}
else if (descMap.cluster == "0000" && descMap.attrId == "8032") {logging("${device} : Routing Table Cluster Event ", "debug")}
else {logging("${device} : Unknown profileId:${descMap.profileId} clusterId:${descMap.clusterId} clusterInt:${descMap.clusterInt} sourceEndpoint${descMap.sourceEndpoint} destinationEndpoint${descMap.destinationEndpoint} options:${descMap.options} command:${descMap.command} data:${descMap.data}", "debug")    }

    if (description?.startsWith('enroll request')) {
    	List cmds = enrollResponse()
        logging("${device} : enroll request ${cmds}", "trace")
        result = cmds?.collect { new hubitat.device.HubAction(it) }
        return result
    }
    
}

def release(cmd){
    if (cmd ==1){
    logging("${device} : ${button1Name} Doorbell [button 1] released", "debug")
    sendEvent(name: "released", value: "1", isStateChange: true)
    }
    if (cmd ==2){
    logging("${device} : ${button2Name} Doorbell [Button 2] released", "debug")
    sendEvent(name: "released", value: "2", isStateChange: true)
    }
} 

def push(cmd){
    
    if (cmd ==1){
    logging("${device} : ${button1Name} Doorbell [button 1] Pressed!", "info")
    sendEvent(name: "pushed", value: "1", isStateChange: true)
    pauseExecution(6000)
    release(cmd)
    }
    if (cmd ==2){
    logging("${device} : ${button2Name} Doorbell [Button 2] Pressed!", "info")
    sendEvent(name: "pushed", value: "2", isStateChange: true)
    pauseExecution(6000)
    release(cmd)   
    }
    
 
    
}


def refresh() {
    logging("${device} : Refresh", "info")
    def refreshCmds = [
        "he rattr 0x${device.deviceNetworkId} 18 0x0001 0x20", "delay 500",
        "he raw 0x${device.deviceNetworkId} 1 0x12 0x0000 {10 00 00 04 00}", "delay 2000",
        "he raw 0x${device.deviceNetworkId} 1 0x12 0x0000 {10 00 00 05 00}", "delay 2000", 
	]
	return refreshCmds + enrollResponse()
}



def enrollResponse() {
    logging("${device} : Sending enroll response", "info")
	String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
	[
		//Resending the CIE in case the enroll request is sent before CIE is written
		"zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}", "delay 200",
		"send 0x${device.deviceNetworkId} 1 ${endpointId}", "delay 500",
		//Enroll Response
		"raw 0x500 {01 23 00 00 00}",
		"send 0x${device.deviceNetworkId} 1 1", "delay 200"
	]
}

private isDuplicateCall(lastRun, allowedEverySeconds) {
	def result = false
	if (lastRun) {
		result =((new Date().time) - lastRun) < (allowedEverySeconds * 1000)
	}
	result
}




private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}

private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
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

