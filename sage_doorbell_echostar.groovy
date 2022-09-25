/**
 *  SAGE Doorbell Sensor fix
    Hubitat driver. 

Conversion to hubitat so I could find out why im getting false
button 2 presses on internal drivers which have no debugging logs.

Adds battery support (siulated) if stops reporting bat goes to 0



================================================================================    
v2.2  09/25/2022  Cleanup code
v2.1  09/24/2022  Presence schedule added
v2.0  09/24/2022  This fixes false button press after last hub update.
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




Orginal here
https://github.com/trunzoc/Hubitat/blob/master/Drivers/Sage_Doorbell/Sage_Doorbell.groovy
Ported to hubitat here
https://github.com/rbaldwi3/Sage-HVAC-Sensor/edit/master/groovy
 *
 *  darwinsden.com/sage-doorbell
 *
 *  White wire: common
 *  Green wire: doorbell 1 / front
 *  Yellow wire: doorbell 2 / rear
 *
 * Factory reset:
 * Remove the plastic cover and the battery.
 * Press and hold the tiny RESET button (next to where the wires attach to the circuit board) while you reinstall the battery.
 * Continue holding RESET until the red LED blinks.
 *
 *	Author: Darwin@DarwinsDen.com
 *  HE Conversion: CraigTrunzo
 *	Date: 2019-11-23
 *
 *	Changelog:
 *
 *  1.0.0 (11/27/2019) - Converted to a standardized HE Driver and added parameter names for buttons
 *  0.40 (03/23/2017) - set numberOfButtons attribute for those smart apps that rely on this
 *  0.30 (11/20/2016) - Removed non-operational battery capability; was preventing device display on 2.2.2 mobile app
 *  0.20 (08/02/2016) - Added preference option for allowed time between presses to eliminate duplicate notifications on some systems
 *  0.10 (06/13/2016) - Initial 0.1 pre-beta Test Code
 */
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
def clientVersion() {
    TheVersion="2.2"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() // Forces config on updates
 }
}


metadata {
    definition (name: "Sage Doorbell echostar sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/sage_doorbell_echostar.groovy") {

		capability "Configuration"
        capability "Pushable Button"
		capability "Refresh"
        capability "Battery"
        capability "PresenceSensor"
        
        command "checkPresence"
        command "enrollResponse"
        command "uninstall"
 	   
        fingerprint endpointId: "12", inClusters: "0000,0003,0009,0001", outClusters: "0003,0006,0008,0019", model: "Bell", manufacturer: "Echostar"
    }
      
    preferences {
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

	input name: "timeBetweenPresses",type: "number",title: "Seconds allowed between presses (increase this value to eliminate duplicate notifications)",required: false,displayDuringSetup: true,defaultValue: 10
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
   setPrefs()
   loggingUpdate()
   clientVersion() 
}

def configure() {
    unschedule()
    // Schedule presence in hrs
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${12} * * ? *", checkPresence)	

    buttons = device.currentValue("numberOfButtons")
    if (buttons != 2){sendEvent(name: "numberOfButtons", value: 2, displayed: true)}

    setPrefs()
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
    logging("${device} : clusterId:${descMap.clusterId} command:${descMap.command} options:${descMap.options} data:${descMap.data}", "debug")
	Map map = [:]
	if (description?.startsWith('catchall:')) {	
        map = parseCatchAllMessage(description)	
        logging("${device} : catchall map ${map}", "trace")
       
    }
    
    else if (description?.startsWith('read attr -')) {
        map = parseReportAttributeMessage(description)
        logging("${device} : read attr map ${map}", "trace")
    }
    
    
	def result = map ? createEvent(map) : null
    
    if (description?.startsWith('enroll request')) {
    	List cmds = enrollResponse()
        logging("${device} : enroll request ${cmds}", "trace")
        result = cmds?.collect { new hubitat.device.HubAction(it) }
    }
    return result
}
 
private Map parseCatchAllMessage(String description) {
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    if (shouldProcessMessage(cluster)) { 
        logging("${device} : cluster:${cluster}", "debug")
        switch(cluster.clusterId) {
            case 0x0006:
            	resultMap = getDoorbellPressResult(cluster)
                break
        }
    }
    return resultMap
}

private boolean shouldProcessMessage(cluster) {
    // 0x0B is default response indicating message got through
    // 0x07 is bind message
    boolean ignoredMessage = cluster.profileId != 0x0104 || 
        cluster.command == 0x0B ||
        cluster.command == 0x07 ||
        (cluster.data.size() > 0 && cluster.data.first() == 0x3e)
    return !ignoredMessage
}

private Map parseReportAttributeMessage(String description) {
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
    logging("${device} : Desc Map ${descMap}", "debug")
 
	Map resultMap = [:]
	if (descMap.cluster == "0001" && descMap.attrId == "0020") {
        log.debug descMap.value
		resultMap = getBatteryResult(Integer.parseInt(descMap.value, 16))
	}
 
	return resultMap
}

private Map getBatteryResult(rawValue) {
    logging("${device} : Bat value ${rawValue} ignored", "trace")// invalid data
}

def push(cmd){
    
    if (cmd ==1){
    logging("${device} : ${button1Name} Doorbell Pressed! [button 1]", "info")
    sendEvent(name: "pushed", value: "1", isStateChange: true)
    }
    if (cmd ==2){
    logging("${device} : ${button2Name} Doorbell Pressed! [Button 2]", "info")
    sendEvent(name: "pushed", value: "2", isStateChange: true)
    }
  
}

private Map getDoorbellPressResult(cluster) {
    def linkText = getLinkText(device)
    def buttonNumber = (cluster.command as int)
    def result = [:]
    switch(buttonNumber) {
        case 0: 
            if (!isDuplicateCall(state.lastButton2Updated, state.timeBetweenPresses) ){
                push(2)
            }
            state.lastButton2Updated = new Date().time	
            break

        case 1: 
            if (!isDuplicateCall(state.lastButton1Updated, state.timeBetweenPresses) ){		
                push(1)
            }
            state.lastButton1Updated = new Date().time
    }
    return result
}

def refresh() {
    logging("${device} : Refresh", "debug")
   
    def refreshCmds = [
        "he rattr 0x${device.deviceNetworkId} 18 0x0001 0x20", "delay 500", 
	]

    setPrefs()
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

def setPrefs() 
{
if (!timeBetweenPresses)  {timeBetweenPresses = 10}
if (timeBetweenPresses <0){timeBetweenPresses = 10}
state.timeBetweenPresses = timeBetweenPresses
    logging("${device} :Time Between Presses ${state.timeBetweenPresses}", "info")
 
    
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

