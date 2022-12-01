/**Iris Utilitech Siren driver hubitat





This is for the Iris Utilitech Siren #0422360 No test done on other models
Designed to detect false alarms and give proper bat suppot and drop out detection.





 v1.1.0    12/01/2022  Phase 2 creation rewriting code
 v1.0.0    10/01/2022  created 
       


 *  Copyright 2022 by winnfreenet.com
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
 *  using zwave commands from this driver. Code rewritten......
 *  https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/zwave-siren.src/zwave-siren.groovy
 */
metadata {
	definition(name: "Iris Utilitech Siren", namespace: "tmastersmart", author: "tmaster",importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/iris_utilitech_siren_zwave.groovy") {
		capability "Actuator"
		capability "Alarm"
		capability "Sensor"
		capability "Switch"
		capability "Tamper Alert"
		capability "Health Check"
        
        capability "Battery"
		capability "Configuration"
        capability "Refresh"
        capability "Polling"
        capability "PresenceSensor"
        
        command "checkPresence"
        command "initialize"
        

        fingerprint mfr: "0060", prod: "000C", deviceId:"0001", deviceJoinName: "Utilitech Siren" , inClusters:"0x20,0x25,0x86,0x80,0x85,0x72,0x71"
		fingerprint mfr: "0060", prod: "000C", model: "0001", deviceJoinName: "Utilitech Siren" 
		fingerprint mfr: "0060", prod: "000C", model: "0002", deviceJoinName: "Everspring Siren"//Everspring Outdoor Solar Siren
	}


}
preferences {
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true


}
def clientVersion() {
    TheVersion="1.2.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}



def installed() {
	logging("${device} : Paired!", "info")
    initialize()
    runIn(20,refresh)
}



def updated() {
    logging("updated","info")
    clientVersion()
	loggingUpdate()
	state.configured = false
}



def initialize() {
    logging("Initialize","info")
    state.FalseAlarm = 0
    
    
 delayBetween([
        zwave.basicV1.basicGet().format(),
        zwave.associationV2.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format(),
        zwave.wakeUpV2.wakeUpIntervalGet().format(),
        zwave.batteryV1.batteryGet().format(),
    ], 2300)


configure()

}




def configure() {
	logging("Config","info")
	state.configured = true
    unschedule()
    
    // Schedule presence in hrs 8
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${8} * * ? *", checkPresence)	
    logging("Presence set for 8 hrs ${randomSixty} ${randomSixty} ${randomTwentyFour}/${8} * * ? *", "info")	

//Everspring 1,2,3 minutes	1:60, 2:120, 3:180 (untested this is a IRIS driver)
 
	if (zwaveInfo?.mfr == "0060" && zwaveInfo?.prod == "000C" && zwaveInfo?.model == "0002") {// 0001= Utilitech  0002=Everspring
       logging("Everspring alarm Length 3 min", "debug")
        
       delayBetween([
        secure(zwave.configurationV2.configurationSet(parameterNumber: 1, size: 2, configurationValue: [0,180])),
        secure(zwave.configurationV2.configurationGet(parameterNumber: 1)),
        zwave.configurationV2.configurationSet(parameterNumber: 1, size: 2, configurationValue: [0,180]).format(), 
        zwave.configurationV2.configurationGet(parameterNumber: 1).format(),   
    ], 2300)
}

    
}


def on() {
    logging("On", "info")
    state.Alarm = true
    delayBetween([
        secure(zwave.basicV1.basicSet(value: 0xFF)),
        secure(zwave.basicV1.basicGet())
    ], 3000)
}


def off() {
    logging("Off", "info")
    state.Alarm = false
   delayBetween([
        secure(zwave.basicV1.basicSet(value: 0x00)),
        secure(zwave.basicV1.basicGet())
    ], 3000)
}

// included for compatability 
def siren() {
    logging("Sirene", "info")
	on()
}

def strobe() {
    logging("Strobe", "info")
	on()
}

def both() {
    logging("Both", "info")
	on()
}


// -------------------------------------------------
def poll() {
    logging("Poll", "info") 
    zwave.basicV1.basicGet().format()
}

def ping() {
    logging("Ping", "info") 
    zwave.basicV1.basicGet().format()
}

def refresh() {
    clientVersion()
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MM/dd/YYYY", location.timeZone)}"
    logging("Refresh  ${Timecheck} v${state.version}", "info") 


    
    delayBetween([
//        zwave.configurationV2.configurationGet().format(),
        zwave.basicV1.basicGet().format(),
        zwave.batteryV1.batteryGet().format(),
		secure(zwave.batteryV1.batteryGet())
    ], 3000)
}


def checkPresence() {
    // presence routine. v5.1 11-12-22
    // simulated 0% battery detection
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 2800
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

def parse(String description) {
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MM/dd/YYYY", location.timeZone)}"
    state.lastCheckin = now()
    state.LastCheckin = Timecheck
    logging("${device} : Raw [${description}]", "trace")
    checkPresence()

    CommandClassCapabilities = [0x20: 1,0x9C: 1, 0x71: 1, 0x84: 2, 0x30: 1, 0x70: 1]  
//    CommandClassCapabilities = [0x20: 1, 0x71: 1]
    hubitat.zwave.Command map = zwave.parse(description, CommandClassCapabilities)
    if (map == null) {return null}
    def result = [map]
    if (!result) {return null}

    logging("${device} : Parse ${result}", "debug")
    if (map) { 
        zwaveEvent(map)
        return
    }

}

private secure(hubitat.zwave.Command cmd) {
	if (zwaveInfo?.zw?.contains("s")) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand
    logging("${device} : encapsulated:: ${encapsulatedCommand}", "debug")

	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(hubitat.zwave.commands.configurationv2.ConfigurationReport cmd) {
    logging("configuration report: ${cmd}", "debug")
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd) {
    logging("NotificationReport: ${cmd}", "debug")
}

// This is the report we receive <----------------------------------------
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("${cmd}", "debug")
    
    if (cmd.value == 0){  value="off"} 
    if (cmd.value == 255){value="on"} 
    
    if (value == "on" || value=="off"){
    logging("Event ${value} ", "info")
        sendEvent(name: "switch", value: value, descriptionText: " v${state.version}", isStateChange:true)
        sendEvent(name: "alarm",  value: value, descriptionText: " v${state.version}", isStateChange:true)
        sendEvent(name: "strobe", value: value, descriptionText: " v${state.version}", isStateChange:true) 
    }
	
//  An atempt to stop the random false alarms the device gets.
    if (value == "on" && state.Alarm == false){
        logging("False Alarm, ON not sent Turning OFF", "warn")  
        off()
        state.FalseAlarm = state.FalseAlarm +1 // Keeping track of how many we get
    }  
        
        
}

def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    logging("${cmd}", "debug")
	
}


def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
      logging("received batteryv1 ${cmd}", "debug")
    if (cmd.batteryLevel == 0xFF) { 
        logging("Low battery FLAG FF", "warn")// not getting these reports
        return
     } 
    if (cmd.batteryLevel == 0) { 
        logging("Ignoring bat 0 ", "warn")
        return
     } 
    
//    state.LastBat = device.currentValue("battery")
//    if (state.LastBat != cmd.batteryLevel ){ 
    logging("battery: ${cmd.batteryLevel} ", "info")
    sendEvent(name: "battery", value: cmd.batteryLevel,unit: "%", descriptionText: "${cmd.batteryLevel} ${state.LastCheckin} v${state.version}", isStateChange:true)
//    }
}








// these are untrapped log them...

def zwaveEvent(hubitat.zwave.Command cmd) {
	logging("${cmd} ", "debug")
    if (cmd.alarmLevel == 0 && cmd.alarmType==0){return}// why send us 0 0 
// AlarmReport(alarmLevel:1, alarmType:2) Unknown report received on bat change.
    if (cmd.alarmLevel == 1 && cmd.alarmType==2){
        logging("Powering Up OK", "info")
        return
    }
    if (cmd.alarmLevel == 1 && cmd.alarmType==1){
        logging("Low Battery Alarm", "info")// some of these are false
        return
    }
       
    logging("Unknown Level:${cmd.alarmLevel} Type:${cmd.alarmType}", "info")
}











// Logging block 
//	device.updateSetting("infoLogging",[value:"true",type:"bool"])
void loggingUpdate() {
    logging("${device} : Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
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
