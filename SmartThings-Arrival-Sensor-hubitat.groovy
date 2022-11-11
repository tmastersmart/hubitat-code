/** SmartThings Arrival Sensor V2 driver for hubitat

or  SmartThings Presence Sensor V2 driver for hubitat
 *     _____                      _ _____ _     _                       
 *    /  ___|                    | |_   _| |   (_)                      
 *    \ `--. _ __ ___   __ _ _ __| |_| | | |__  _ _ __   __ _ ___       
 *     `--. \ '_ ` _ \ / _` | '__| __| | | '_ \| | '_ \ / _` / __|      
 *    /\__/ / | | | | | (_| | |  | |_| | | | | | | | | | (_| \__ \      
 *    \____/|_| |_| |_|\__,_|_|   \__\_/ |_| |_|_|_| |_|\__, |___/      
 *                                                       __/ |          
 *                                                      |___/           
 *      ___            _            _   _____                           
 *     / _ \          (_)          | | /  ___|                          
 *    / /_\ \_ __ _ __ ___   ____ _| | \ `--.  ___ _ __  ___  ___  _ __ 
 *    |  _  | '__| '__| \ \ / / _` | |  `--. \/ _ \ '_ \/ __|/ _ \| '__|
 *    | | | | |  | |  | |\ V / (_| | | /\__/ /  __/ | | \__ \ (_) | |   
 *    \_| |_/_|  |_|  |_| \_/ \__,_|_| \____/ \___|_| |_|___/\___/|_|   
 * 

SmartThings Presence Sensor V2
Part Number	F-ARR-US-2 
MFN # STS-PRS-250
FCCID 2AF4S-STS-PRS-250
https://fccid.io/2AF4S-STS-PRS-250

Not yet tested on V1


Beep Beep for 5 seconds.

Chime for x seconds Play 6 chimes for 6 seconds.

This is my improved smartthings arrival sensor driver for hubitat.
One of the improvements is you can play longer beeps. Another is less cluter in the log
As only changes are recorded. Im also hoping for better reliabilaty and less dropouts.

To reset hold button for 5 seconds and led will flash.




====================================================================================================
v1.7  11/11/2022 Schedule added to recover from sleep
v1.6  09/19/2022 Logging code changed
v1.5  09/09/2022 auto adjust min battery level
v1.4  09/01/2022 Log cleanups. Renamed V2
v1.3  08/31/2022 Minor changes
v1.2  08/30/2022 Refresh added
v1.1  08/17/2022 Reworked all the code
v1.0  08/16/2022 Forked and modified or hubitat


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



forked from here ( with many rewrites)

Which a newer version is here 2016 +
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/arrival-sensor-ha.src/arrival-sensor-ha.groovy
https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/arrival-sensor-ha.src/README.md

 *
 */
def clientVersion() {
    TheVersion="1.7"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() // Forces config on updates
 }
}



metadata {
    
    definition (name: "SmartThings Arrival Sensor Chime V2", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/SmartThings-Arrival-Sensor-hubitat.groovy") {


		capability "Tone"
		capability "Actuator"
		capability "Refresh"
		capability "Presence Sensor"
		capability "Sensor"
		capability "Battery"
        capability "Configuration"
        capability "Chime"
        capability "Health Check"

        attribute "batteryVoltage", "string"

//        command "stopTimer"
        
        

// device Join name ignored in hubitat used for ref
        
        fingerprint profileId: "FC01", deviceId: "019A", manufacturer: "SmartThings", deviceJoinName: "SmartThings Arrival Sensor Chime"
		fingerprint profileId: "FC01", deviceId: "0131", manufacturer: "SmartThings", inClusters: "0000,0003", outClusters: "0003", deviceJoinName: "SmartThings Presence Sensor"
		fingerprint profileId: "FC01", deviceId: "0131", manufacturer: "SmartThings", inClusters: "0000",      outClusters: "0006", deviceJoinName: "SmartThings Presence Sensor"
        fingerprint inClusters: "0000,0001,0003,000F,0020", outClusters: "0003,0019", manufacturer: "SmartThings", model: "tagv4", deviceJoinName: "SmartThings Presence Sensor"

	}
	
}
preferences {
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    
	input("Interval",  "number", title: "Presence Timeout", description: "Time in mins driver sets not present",defaultValue: 15,required: true)

}

def installed(){
logging("Paired!", "info")
configure()
  
}

def configure() {
    logging("Configure Driver v${state.version}", "info")

    getIcons()

    updated() 
}

def updated() {
    clientVersion()
    loggingUpdate()
}

def stop(){
logging("Stop Ignored ", "info")
}

def beep() {
    logging("Beep for 5 seconds ", "info")
    return zigbee.command(0x0003, 0x00, "0500")
}

def playSound(cmd){
    if(cmd <2){return}
    logging("Chime for ${cmd} seconds ", "info")
    cmd = cmd * 100
    return zigbee.command(0x0003, 0x00, "${cmd}")
    
}    
def refresh() {
    
 delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
    sendZigbeeCommands(zigbee.readAttribute(0x001, 0x0020)),//Battery percentage
   ], 1000)      
    logging("refresh ", "info")
}

def ping() {
    logging("Ping", "info")
    sendZigbeeCommands(zigbee.readAttribute(0x001, 0x0020))//Battery percentage   
}


void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

// [raw:E131010001082000201A, dni:E131, endpoint:01, cluster:0001, size:08, attrId:0020, encoding:20, command:0A, value:1A, clusterInt:1, attrInt:32]
// [raw:catchall: 0104 0003 01 01 0040 00 E131 00 00 0000 0B 01 0000, profileId:0104, clusterId:0003, clusterInt:3, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:E131, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:0B, direction:01, data:[00, 00]] 
// [raw:E1310100010A2000201A, dni:E131, endpoint:01, cluster:0001, size:0A, attrId:0020, encoding:20, command:01, value:1A, clusterInt:1, attrInt:32] 

def parse(String description) {
    state.lastCheckin = now()
    def descMap = zigbee.parseDescriptionAsMap(description)
    logging("Parse :${descMap} ", "trace")
    handlePresenceEvent(true)
    if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0020) { 
        handleBatteryEvent(Integer.parseInt(descMap.value, 16)) 
    }
    if (descMap.command == "00") { // options 0040 (when rejoining we get this)
    logging("Rejoining Mesh", "info")
    return
    }
    if (descMap.command == "01") {
    logging("Reply to refresh ", "info")
    return
    }
    if (descMap.command == "0B") {
        logging("Beep Processed ", "info")
        return
    }
    if (descMap.command == "0A") {
        logging("Ping", "debug")
        return
     }
    else{
    logging("Unknown command:${descMap.command} Options:${descMap.options} Value:${descMap.value}", "warn")
    }    
    
    
    

}

// Only log battery if its changed
private handleBatteryEvent(batteryVoltage) {
  
    if (batteryVoltage == 0 || batteryVoltage == 255) {
        return
    }
    else {
         // Auto adjustment  
        if (state.minVoltTest < 0.90){ 
            state.minVoltTest= 1.50 
            logging("Min Voltage Reset to ${state.minVoltTest}v", "info") 
        }
        if (batteryVoltage < state.minVoltTest){
            state.minVoltTest = batteryVoltage
            logging("Min Voltage Lowered to ${state.minVoltTest}v", "info")  
        } 
        BigDecimal batteryVoltageScaleMin = state.minVoltTest
		BigDecimal batteryVoltageScaleMax = 3.0
        batteryVoltage = batteryVoltage/10
        BigDecimal batteryPercentage = 0
  
        batteryPercentage = ((batteryVoltage - batteryVoltageScaleMin) / (batteryVoltageScaleMax - batteryVoltageScaleMin)) * 100.0
		batteryPercentage = batteryPercentage.setScale(0, BigDecimal.ROUND_HALF_UP)
		batteryPercentage = batteryPercentage > 100 ? 100 : batteryPercentage
        logging("Battery ${batteryVoltage}V: ${batteryPercentage}%  Last:${state.lastBattery}v","debug")
        
        if (state.lastBattery != batteryVoltage){
         logging("Battery ${batteryVoltage}V ${batteryPercentage}%","info")   
    	 sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V",descriptionText:"${value}V ${state.version}", isStateChange: true)
     	 sendEvent(name: "battery", value: batteryPercentage, unit: "%",descriptionText:"${value}% ${state.version}", isStateChange: true)
         state.lastBattery = batteryVoltage
        }  
        
        
        }

}
// To limit logs only report in log on change
private handlePresenceEvent(present) {
    logging("Received presence event: ${present}","trace")
    if(present){value="present"}
    else{value="not present"}

    def wasPresent = device.currentState("presence")?.value == "present"
    if (!wasPresent && present) {
        logging("${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        startTimer()
    } else if (!present) {
        logging("${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        stopTimer()
    }
}

private startTimer() {
    unschedule()
    logging("Checking ever min","info")
    runEvery1Minute("checkPresenceCallback")
}

private stopTimer() {// Check presence every hr
    unschedule()
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *",checkPresenceCallback)	
    logging("Checking ever hr","info")
}


def checkPresenceCallback() {
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
//  Interval set by driver
    min = timeSinceLastCheckin/60
//  sensor checks in every  0.13 seconds. We have to mimimize log entry.
    logging("Check ${min} mins","debug")
    if (min >= Interval-5){ping()}
    if (min >= Interval) { handlePresenceEvent(false)}
}

void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/tagv4.jpg' >"
 }

// Logging block v5 11/2022
//	
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    if (disableLogsOff){return}
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
