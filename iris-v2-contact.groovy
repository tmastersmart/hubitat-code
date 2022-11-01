/* Iris v2 contact sensor
Iris v2 contact sensor for hubitat
iMagic by GreatStar  model: 1116-S

FCC ID:2AM121L06 model iL06_1






1.2.0    10/31/2021 First release
======================================================================================

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


forked from non working driver
https://github.com/djdizzyd/hubitat/blob/320e1f5ae0ff65d7a9131ba15165b92c5afaf0be/Drivers/Iris%20V3%20Contact%20Sensor%20IL06_1/iris-V3-il06-contact-sensor.groovy
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

metadata {

definition (name: "Iris v2 Contact Sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris-v2-contact.groovy") {

    capability "Health Check"
   
	capability "Battery"
	capability "Configuration"
	capability "Contact Sensor"
	capability "Initialize"
	capability "PresenceSensor"
	capability "Refresh"
	capability "Sensor"
	capability "TemperatureMeasurement"
    
    
	
command "checkPresence"
    
attribute "batteryVoltage", "string"
    
fingerprint model: "1116-S",manufacturer: "iMagic by GreatStar", deviceJoinName: "Iris V2 Contact Sensor", profileId:"0104", endpointId:"01", inClusters:"0000,0001,0003,0020,0402,0500,0B05,FC01,FC02", outClusters:"0003,0019", application:"00"
    
}
preferences {
		
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    	
    input name: "tempAdj",type:"enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8",
    "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8",
    "-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",
    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",
   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: "0",required: true  

    }
}
def installed() {

}


def updated(){
    logging("Updated ", "info")
    loggingUpdate()
}

def refresh() {
    logging("refresh ", "info")
    def cmds = zigbee.readAttribute(0x0001, 0x0020) +
        zigbee.readAttribute(0x0402, 0x0000) +
        zigbee.readAttribute(0x0500, 0x0002) +
        zigbee.enrollResponse()
    return cmds
}

def ping() {
    logging("Ping ", "info")
    zigbee.readAttribute(0x500, 0x0002) 
}

def configure() {
    logging("Config", "info")
    
/// w is this     
  sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
 
    // Check presence every hr
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${1} * * ? *", checkPresence)	    
    
    
   def cmds = refresh() +
        zigbee.batteryConfig() +
        zigbee.configureReporting(0x0500, 0x0002, DataType.BITMAP16, 30, 60 * 5, null) +
        zigbee.temperatureConfig(30, (60 * 30)) +
       zigbee.enrollResponse()
  return cmds

}




def parse(String description) {
    logging("Parse: [${description}]", "trace")
    state.lastCheckin = now()
    checkPresence()
    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("MAP: ${descMap}", "trace")
    
        if (description?.startsWith('enroll request')) { // The instant status
        zigbee.enrollResponse()
        return  
    }  
    
   	if (description.startsWith("zone status")) {
		ZoneStatus zoneStatus = zigbee.parseZoneStatus(description)
		processStatus(zoneStatus)
        return
    }
    

 
	
   
// cluster:0500, size:0A, attrId:0002, encoding:19, command:0A, value:0001, clusterInt:1280, attrInt:2]   
// clusterId:0500, clusterInt:1280, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:04, direction:01, data:[00]]

 
    if (descMap.cluster == "0001" & descMap.attrId == "0020"){
           powerLast = device.currentValue("battery")
           def rawValue = Integer.parseInt(descMap.value,16) 
           def batteryVoltage = rawValue / 10
           if (!(rawValue == 0 || rawValue == 255)) {
           def minVolts = 2.1
           def maxVolts = 3.0
           def pct = (batteryVoltage - minVolts) / (maxVolts - minVolts)
           def roundedPct = Math.round(pct * 100)
         if (roundedPct <= 0) roundedPct = 1
            batteryPercentage = Math.min(100, roundedPct)
            logging("Battery: now:${batteryPercentage}% Last:${powerLast}% ${batteryVoltage}V", "debug")   
            if (powerLast != batteryPercentage){
            logging("Battery:${batteryPercentage}%  ${batteryVoltage}V", "info")  
            sendEvent(name: "battery", value: batteryPercentage, unit: "%")      
            sendEvent(name: "batteryVoltage", value: batteryVoltage, unit: "V")
            }

        }
        
 
   
// cluster: 0402, size: 0C, attrId: 0000, encoding: 29, command: 01, value: 3A09]   
      
    
  

// enrole request
//clusterId:0013, clusterInt:19, sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[82, 5F, 01, F0, 0F, C9, FE, FF, 5E, CF, D0, 80]]
    

    
//  clusterId:0013, clusterInt:19,sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[81, 5F, 01, F0, 0F, C9, FE, FF, 5E, CF, D0, 80]]
//  clusterId:0006, clusterInt:6, sourceEndpoint:00, destinationEndpoint:00, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:00, direction:00, data:[02, FD, FF, 04, 01, 01, 19, 00, 00]]
    
    
    
    
//  clusterId:0500, clusterInt:1280, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:04, direction:01, data:[00]]
//  cluster:0500, size:0C, attrId:0002, encoding:19, command:01, value:0001, clusterInt:1280, attrInt:2]   
//  cluster:0500, size:0C, attrId:0002, encoding:19, command:01, value:0001, clusterInt:1280, attrInt:2]
//  cluster:0500, size:0C, attrId:0002, encoding:19, command:01, value:0001, clusterInt:1280, attrInt:2]
    
}else if (descMap.cluster == "0500"){// This is a repeating status

        if (descMap.attrId == "0002" ) {
         value = Integer.parseInt(descMap.value, 16)
            if(value== 0){contactClosed()}
            else {contactOpen()}
        
      } else if (descMap.commandInt == "07") {
                    if (descMap.data[0] == "00") {
                        logging("IAS ZONE REPORTING CONFIG RESPONSE: ", "info")
                        
                        sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
                    } else {logging("IAS ZONE REPORING CONFIG FAILED - Error Code: ${descMap.data[0]} ", "warn")}

                }   
  
	} 
  
   
    
    
  // cluster:0402, size:0A, attrId:0000, encoding:29, command:0A, value:09AA, clusterInt:1026, attrInt:0]
  //cluster: 0402, size:0C, attrId: 0000, encoding: 29, command: 01, value: 4C09]  
    else if (descMap.cluster == "0402" ) {//?Temp null 2409 0
         if (descMap.attrInt == 0) {
        def rawValue = Integer.parseInt(descMap.value,16)
        temperatureF = convertTemperatureIfNeeded((rawValue / 100), "C", 2)    
        temperatureU = temperatureF     
        def correctNum = (tempAdj ?: 0.0).toBigDecimal() 
        if (correctNum != 0){ temperatureF = temperatureF + correctNum }

        logging("Temp:${temperatureF}F adjust:${correctNum} [Sensor:${temperatureU}]", "info")
        sendEvent(name: "temperature", value: temperatureF, unit: "F")
		    	}

// clusterId:0402, clusterInt:1026, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:07, direction:01, data:[00]]
// clusterId:0001, clusterInt:1, sourceEndpoint:01, destinationEndpoint:01, options:0040, messageType:00, dni:015F, isClusterSpecific:false, isManufacturerSpecific:false, manufacturerId:0000, command:07, direction:01, data:[00]]
    }  else if (descMap.clusterId == "0006") {
        logging("unknown clusterId:0006 ${descMap.data}", "debug")

    
    
    }else if (descMap.clusterId == "0500" ) {
        logging("unknown clusterId:0500 command:${descMap.command} data:${descMap.data}", "debug")        

    }else if (descMap.clusterId == "0001" ) {
        logging("unknown clusterId:0001 ${descMap}", "debug")        
        
    
    }else if (descMap.clusterId == "0402" ) {
        logging("unknown clusterId:0402 ${descMap}", "debug")        
        

   }else if (descMap.cluster == "8021" | descMap.clusterId == "8021" ) {
        logging("Blind Cluster event :8021 ${descMap.data}", "debug")
   }else if (descMap.cluster == "8038" | descMap.clusterId == "8038") {
        logging("General Catchall :8038 ${descMap.data}", "debug")      
   }else if (descMap.cluster == "0013" | descMap.clusterId == "0013") {
        logging("Device Announcement Cluster ${descMap.data}", "warn")
        zigbee.enrollResponse()
    }  
    
    else{       logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}
    }


def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}

private BigDecimal hexToBigDecimal(String hex) {
    int d = Integer.parseInt(hex, 16) << 21 >> 21
    return BigDecimal.valueOf(d)
}


def ForceOpen (){
 contactOpen()
} 
void contactOpen(){
    test = device.currentValue("contact")
    if (test != "open"){sendEvent(name: "contact", value: "open")}
    logging("Contact: Open our state was:${test}", "info")
}


def ForceClosed (){
contactClosed()
}
void contactClosed(){
    test = device.currentValue("contact")
    if (test != "closed"){sendEvent(name: "contact", value: "closed")}
    logging("Contact: closed our state was:${test}", "info")
}

// sends standard zigbee command
def processStatus(ZoneStatus status) {
    logging("${device} : ZoneStatus Alarm1:${status.isAlarm1Set()} Alarm2:${status.isAlarm2Set()}", "debug")
	if (status.isAlarm1Set() || status.isAlarm2Set()) {
        contactOpen()
    }else {contactClosed()}
}



def checkPresence() {
    // New shorter presence routine. v2 10/22
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
    if (state.lastCheckInMin >= checkMin2) { 
        test = device.currentValue("presence")
        if (test != "not present"){
        value = "not present"
        logging("Creating presence event: ${value} ${state.lastCheckInMin} min ago","warn")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        sendEvent(name: "battery", value: 0, unit: "%",descriptionText:"${value}% ${state.version}", isStateChange: true) 
        runIn(60,refresh) 
        }
    } 
    if (state.lastCheckInMin >= checkMin){ 
      logging("Sensor timing out ${state.lastCheckInMin} min ago","warn")
      runIn(60,refresh)// Ping Perhaps we can wake it up...
    }
}

void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}




// Logging block ${device} added to routine v2

void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){runIn(3600,debugLogOff)}
    if (traceLogging){runIn(1800,traceLogOff)}
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

