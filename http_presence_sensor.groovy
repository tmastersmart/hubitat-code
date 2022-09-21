/**HTTP Presence Sensor adjustable schedule
 *  
    Hubitat HTTP presence sensor

Allows you to set chron to stop overloading servers

This fixes the orginal version 


=================================================================
  v2.2  09/21/2022 Better logging 
  v2.1.1 09/14/2021
  v2.0 09/12/2021




https://github.com/tmastersmart/hubitat-code/blob/main/http_presence_sensor.groovy
https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/http_presence_sensor.groovy








forked from
https://github.com/joelwetzel/Hubitat-HTTP-Presence-Sensor/blob/master/httpPresenceSensor.groovy
   
    v1.0
 *
 *  Copyright 2019 Joel Wetzel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

	
metadata {
    definition (name: "HTTP Presence Sensor with schedule", namespace: "tmastersmart", author: "WinnFreeNet.com", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/http_presence_sensor.groovy") {
		capability "Refresh"
		capability "Sensor"
        capability "Presence Sensor"
        capability "Configuration"
	}

preferences {
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true
    input name: "endpointUrl" , type: "string",title: "Endpoint URL",required: true
    input name: "pollMinutes" ,	type: "number",title: "Polling Minutes",description: "Schedule to check",required: true,defaultValue: 5

}

}



def configure() {
    logging("${device} : Configure", "info")
    updated()
}
    
    
def installed () {
	logging("${device} : Install", "info")
    updated()
}


def updated () {
    state.tryCount = 0
    schedule("0 */${pollMinutes} * ? * *", refresh)
    logging("${device} : Updated Schedule ${pollMinutes} mins", "info")
    runIn(2, refresh)
}


def refresh() {
    logging("${device} : Checking Presence  ${state.tryCount}", "debug")
	state.tryCount = state.tryCount + 1
    
    if (state.tryCount > 3 && device.currentValue('presence') != "not present") {
        logging("${device} : Presence: [Not Present] Tries:${state.tryCount} ", "warn")
        sendEvent(name: "presence", value: "not present", descriptionText: "[Not Present] Tries:${state.tryCount} ")
    }
    
	asynchttpGet("httpGetCallback", [
		uri: endpointUrl,
        timeout: 10
	]);
}


def httpGetCallback(response, data) {
	if (response == null || response.class != hubitat.scheduling.AsyncResponse) {
		return
	}
  
    def st = response.getStatus()
    logging("${device} : Presence check status =${st}  Tries:${state.tryCount}", "debug")
	if (st == 200) {
        logging("${device} : Presence:[Present] Tries:${state.tryCount} ", "info")
		state.tryCount = 0
		if (device.currentValue('presence') != "present") {
        logging("${device} : Presence: [Present] Tries:${state.tryCount} ", "debug")
        sendEvent(name: "presence", value: "present", descriptionText: "[Present] Tries:${state.tryCount} ")
            
		}
	}
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
