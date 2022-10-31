/* Iris v3 contact sensor
Iris v3 contact sensor for hubitat
iMagic by GreatStar  model: 1116-S








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


forked from 
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

definition (name: "Iris v3 Contact Sensor", namespace: "tmastersmart", author: "Tmaster", importUrl: "") {

capability "Battery"
capability "Configuration"
capability "Contact Sensor"
capability "Refresh"
capability "Temperature Measurement"
capability "Health Check"
capability "Sensor"

fingerprint manufacturer: "iMagic by GreatStar", model: "1116-S",  deviceJoinName: "Iris V3 Contact Sensor", inClusters: "0000,0001,0003,0020,0402,0500,0B05,FC01,FC02", outClusters: "0003,0019"

}
preferences {
		
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    	
    input name: "tempOffset",type:"enum", title: "Temperature Offset",description: "", options: ["-10","-9.8","-9.6","-9.4","-9.2","-9.0","-8.8","-8.6","-8.4","-8.2","-8.0","-7.8",
    "-7.6","-7.4","-7.2","-7.0","-6.8","-6.6","-6.4","-6.2","-6.0","-5.8","-5.6","-5.4","-5.2","-5.0","-4.8",
    "-4.6","-4.4","-4.2","-4.0","-3.8","-3.6","-3.4","-3.2","-3.0","-2.8","-2.6","-2.4","-2.2","-2.0","-1.8","-1.6","-1.4","-1.2","-1.0","-0.8","-0.6","-0.4","-0.2","0",
    "0.2","0.4","0.6","0.8","1.0","1.2","1.4","1.6","1.8","2.0","2.2","2.4","2.6","2.8","3.0","3.2","3.4","3.6","3.8","4.0","4.2","4.4","4.6","4.8","5.0","5.2","5.4","5.6","5.8",
   "6.0","6.2","6.4","6.6","6.8","7.0","7.2","7.4","7.6","7.8","8.0","8.2","8.4","8.6","8.8","9.0","9.2","9.4","9.6","9.8","10"], defaultValue: "0",required: true  

    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
}

def refresh() {
    if (logEnable) log.debug "refresh"
    def cmds = zigbee.readAttribute(0x0001, 0x0020) +
        // zigbee.readAttribute(0x0001, 0x0021) + 
        zigbee.readAttribute(0x0402, 0x0000) +
        zigbee.readAttribute(0x0500, 0x0002) +
        zigbee.enrollResponse()
    return cmds
}

def ping() {
    zigbee.readAttribute(0x500, 0x0002) 
}

def configure() {
    log.warn "configure..."
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
    runIn(1800,logsOff)
    def cmds = refresh() +
        zigbee.batteryConfig() +
        zigbee.configureReporting(0x0500, 0x0002, DataType.BITMAP16, 30, 60 * 5, null) +
        zigbee.temperatureConfig(30, (60 * 30)) +
        zigbee.enrollResponse()
   
    return cmds
}





def parse(String description) {
    if (logEnable) log.debug "parse description: ${description}"
    if (description.startsWith("catchall")) return
    //def eventMap = zigbee.getEvent(description)
    if (description.startsWith("zone status")) {
        ZoneStatus zs=zigbee.parseZoneStatus(description)
        sendEvent(name: "contact", value: zs.isAlarm1Set() ? "open" : "closed")
	} else {
        def descMap = zigbee.parseDescriptionAsMap(description)
        def descriptionText
    
        def value
        def name
        def unit
        def rawValue = Integer.parseInt(descMap.value,16)

        switch (descMap.clusterInt){
            case 0x0500: //IAS 
                if (descMap.attrInt == 0x0002){ //zone status
                    def zs = new ZoneStatus(rawValue)
                    log.debug "zone status got"
                    name="contact"
                    value=zs.isAlarm1Set() ? "open" : "closed"
                    sendEvent(name: name, value: value)
                } else if (descMap.commandInt == 0x07) {
                    if (descMap.data[0] == "00") {
                        if (logEnable) log.debug "IAS ZONE REPORTING CONFIG RESPONSE: ${descMap}"
                        sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
                    } else {
                        log.warn "IAS ZONE REPORING CONFIG FAILED - Error Code: ${descMap.data[0]}"        
				    }
                } else {
                    log.debug "0x0500:${descMap.attrId}:${rawValue}"
                }
                break
            case 0x0001: //power configuration
                if (descMap.attrInt == 0x0020){
                    unit = "%"
                    def volts = rawValue / 10
                    if (!(rawValue == 0 || rawValue == 255)) {
                        def minVolts = 2.1
                        def maxVolts = 3.0
                        def pct = (volts - minVolts) / (maxVolts - minVolts)
                        def roundedPct = Math.round(pct * 100)
                        if (roundedPct <= 0) roundedPct = 1
                        value = Math.min(100, roundedPct)
					}
                    name = "battery"
                    sendEvent(name: name, value: value, unit: unit)
                } else {
                    log.debug "0x0001:${descMap.attrId}:${rawValue}"
                }
                break
            case 0x0402: //temperature
                if (descMap.attrInt == 0) {
                    name = "temperature"
                    unit = location.TemperatureScale
                    //if (logEnable) log.debug "0x0402:${descMap.attrId}:${rawValue}"
                    if (tempOffset) {
                        value = (int) convertTemperatureIfNeeded((rawValue / 100), "C", 2) + (int) tempOffset
				    } else {
                        value = convertTemperatureIfNeeded((rawValue / 100), "C", 2)
                    }
                    sendEvent(name: name, value: value, unit: unit)
		    	} else {
                    log.debug "0x0402:${descMap.attrId}:${rawValue}"     
    			}
                break
        }
        if (logEnable) log.debug "evt- rawValue:${rawValue}, value: ${value}, descT: ${descriptionText}"
    }
}

def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}

def installed() {

}

// Logging block 

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

