/**Iris Utilitech Siren driver hubitat

This is for the Iris Utilitech Siren #0422360 No test done on other models
Designed to detect false alarms and give proper bat suppot and drop out detection.
Device does not support tamper. The tamper button never sends any status.
There are no settings on this device however some EverSpring models have a timeout
setting but im unable to test that.

Presence is used to detect dead batteries. 

New timeout timmer.

State verification and repair. No more ignoring the OFF commands



https://www.lowes.com/pdf/UT_IndoorSiren_IM%208-2%20ENG-101512.pdf
------------------------------------------------------------------------------------------------------
Its sometimes hard to get it to pair or repair after removing or resetting.
Remove batteries let it set and try again later.

UNDOCUMENTED HUBITAT Z-WAVE BUG
Go to settings/Z-Wave Details/Z-Wave Radio Devices

Unlike other zwave devices this device gets stuck in this list and the hub gets confused.
GENERIC_TYPE_SWITCH_BINARY Everspring
If its already in zwave list it will never be able to re-pair. Press REPAIR or REFREASH and you will 
get a option to REMOVE, Remove it then the hub will now see it as a new device.
I beleive the cause is that normal devices creating a new ID on each pair. Whatever the case the hub does
not know what to do. I think it was said after several reboots it will go away but we need it out now.
---------------------------------------------------------------------------------------------------------


 v1.0.5    12/03/2022 First working release
 v1.0.1    12/01/2022  beta
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
		capability "Health Check"
        capability "Battery"
		capability "Configuration"
        capability "Refresh"
        capability "Polling"
        capability "PresenceSensor"
        
        command "checkPresence"
        command "initialize"
        command "uninstall"

        fingerprint mfr: "0060", prod: "000C", deviceId:"0001", deviceJoinName: "Utilitech Siren" , inClusters:"0x20,0x25,0x86,0x80,0x85,0x72,0x71"
		fingerprint mfr: "0060", prod: "000C", model: "0001", deviceJoinName: "Utilitech Siren" 
		fingerprint mfr: "0060", prod: "000C", model: "0002", deviceJoinName: "Everspring Siren"//Everspring Outdoor Solar Siren
	}


}
preferences {
    
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

    input name: "timeout" ,type: "enum", title: "Timeout",description: "Auto send off command and reset after x seconds 0=disabled 300=5min 1800=30min",options: ["0","300","600","1200","1500","1800"], defaultValue: 1800 ,required: true 

    input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
    input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 8 ,required: true 


}
def clientVersion() {
    TheVersion="1.0.5"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}

def uninstall() {
 

    unschedule()
    state.icon = ""
    state.donate = ""
    state.remove("presenceUpdated")    
	state.remove("version")
    state.remove("configured")
    state.remove("lastCheckInMin")
    state.remove("icon")
    state.remove("logo")  
    state.remove("FalseAlarm")
    state.remove("tries")
    state.remove("lastPoll")
    state.remove("Alarm")        

      
      }

def installed() {
	logging("${device} : Paired!", "info")
    initialize()

}



def updated() {
    logging("updated","info")
    clientVersion()
	loggingUpdate()
    if (pollYes == false){unschedule(checkPresence)}
}



def initialize() {
    logging("Initialize","info")
    state.FalseAlarm = 0
    
    
configure()

}




def configure() {
	logging("Config","info")
    state.Alarm = false
    if(!state.FalseAlarm){state.FalseAlarm = 0}
    
    state.remove("configured")
    state.remove("LastCheckin")
    state.remove("initializeAttempts")
    state.remove("lastAlarmType")
    removeDataValue("manufacturerName")
    getIcons()
    
    unschedule(checkPresence)
    if (pollYes){ 
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)    
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
    logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", "debug") 
    schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", checkPresence)	
    logging("Presence Check Every ${pollHR}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
    } 
    

    
    
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

runIn(20,refresh)    
}

// Resend the proper state to get back in sync
def sync (){
    state.FalseAlarm = state.FalseAlarm +1 // Keeping track of how many we get
    logging("Resyncing State. Errors:${state.FalseAlarm}", "warn") 
    if (state.Alarm== true){ on()}
    else {off()}
}

def on() {
    logging("Sending On", "info")
    state.Alarm = true

    runIn(20,ping) 
    if (timeout == 300){ runIn(300,off)}
    if (timeout == 600){ runIn(600,off)}
    if (timeout == 800){ runIn(800,off)}
    if (timeout == 1500){ runIn(1500,off)}
    if (timeout == 1800){ runIn(1800,off)}
    if (timeout == 2200){ runIn(2200,off)}

    delayBetween([
        zwave.basicV1.basicSet(value: 0xFF).format(),
        zwave.switchBinaryV1.switchBinaryGet().format(),
 
       secure(zwave.basicV1.basicSet(value: 0xFF)),
       secure(zwave.switchBinaryV1.switchBinaryGet()),
       secure(zwave.basicV1.basicGet())
    ], 3000) 
    

}

def offOFF(){
off()
}


def off() {
    logging("Sending Off", "info")
    state.Alarm = false
    runIn(20,poll)
    
   delayBetween([
        zwave.basicV1.basicSet(value: 0x00).format(),
        zwave.switchBinaryV1.switchBinaryGet().format(),
       
        secure(zwave.basicV1.basicSet(value: 0x00)),
        secure(zwave.switchBinaryV1.switchBinaryGet()),
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
//    zwave.basicV1.basicGet().format()
    zwave.switchBinaryV1.switchBinaryGet().format()
}

def ping() {
    logging("Ping", "info") 
//    zwave.basicV1.basicGet().format()
    zwave.switchBinaryV1.switchBinaryGet().format()
}

def refresh() {
    clientVersion()
    def nowCal = Calendar.getInstance(location.timeZone)
    Timecheck = "${nowCal.getTime().format("EEE MM/dd/YYYY", location.timeZone)}"
    logging("Refresh  ${Timecheck} v${state.version}", "info") 


    
    delayBetween([
//      zwave.configurationV2.configurationGet().format(),
        zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),// fingerprint
//        zwave.basicV1.basicGet().format(),// this just dupes switchbinary
        zwave.batteryV1.batteryGet().format(),
        zwave.switchBinaryV1.switchBinaryGet().format(),

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
    state.lastCheckin = now()
    logging("Raw [${description}]", "trace")
    checkPresence()

    hubitat.zwave.Command map = zwave.parse(description, getCommandClassVersions())
    if (map == null) {return null}
    def result = [map]
    if (!result) {return null}

    logging("${device} : Parse ${result}", "debug")
    if (map) { 
        zwaveEvent(map)
        return
    }

}

/**
 *  getCommandClassVersions()
 *
 *  Returns a map of the command class versions supported by the device. Used by parse() and zwaveEvent() to
 *  extract encapsulated commands from MultiChannelCmdEncap, MultiInstanceCmdEncap, SecurityMessageEncapsulation,
 *  and Crc16Encap messages.
 *
 *  Reference: http://products.z-wavealliance.org/products/629/classes
 **/
private getCommandClassVersions() {
    return [
        0x20: 1, // Basic V1
        0x22: 1, // Application Status V1 (Not advertised but still sent)
        0x25: 1, // Switch Binary V1
        0x27: 1, // Switch All V1
        0x30: 1, // ?
        0x32: 3, // Meter V3
        0x56: 1, // CRC16 Encapsulation V1
        0x70: 1, // Configuration V1
        0x71: 1, // Alarm (Notification) V1
        0x72: 2, // Manufacturer Specific V2
        0x75: 2, // Protection V2
        0x80: 1, // Battery v1
        0x84: 2, // ?
        0x85: 2, // Association V2
        0x86: 1, // Version V1
        0x87: 1, // Indicator V1
        0x9C: 1 // Alarm Sensor
    ]
}

//The siren supports Z-Wave™ Command Classes including:
// * COMMAND_CLASS_BASIC
// * COMMAND_CLASS_VERSION
// * COMMAND_CLASS_BATTERY
// * COMMAND_CLASS_MANUFACTURER_SPECIFIC
// * COMMAND_CLASS_SWITCH_BINARY
// * COMMAND_CLASS_ASSOCIATION_V2


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



void readSwitch (cmd){
    if (cmd == 0){  value="off"} 
    if (cmd == 255){value="on"} 
    
    if (value == "on" || value=="off"){
        logging("State:${value} [Ours:${state.Alarm}]", "info")
        sendEvent(name: "switch", value: value, descriptionText: " v${state.version}", isStateChange:true)
        sendEvent(name: "alarm",  value: value, descriptionText: " v${state.version}", isStateChange:true)
        sendEvent(name: "strobe", value: value, descriptionText: " v${state.version}", isStateChange:true) 
        // Error recovery     
        if (value == "on"  && state.Alarm == false){sync()}  
        if (value == "off" && state.Alarm == true ){sync()} 
        if (value == "off" && state.Alarm == false){unschedule(off)}// stop the auto off timmer
    }



}
// SwitchBinaryReport(value:0) (only when we request status )
def zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    logging("${cmd}", "debug")
    readSwitch(cmd.value)
}

// zwaveEvent( COMMAND_CLASS_BASIC (0x20)  v1 ) (normaly reports here)
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    logging("${cmd}", "debug")
    readSwitch(cmd.value)
}

/**
 *  zwaveEvent( COMMAND_CLASS_BATTERY V1 (0x80) : BATTERY_REPORT (0x03) )
 *
 *
 *  Example: BatteryReport(batteryLevel: 52)
 **/


def zwaveEvent(hubitat.zwave.commands.batteryv1.BatteryReport cmd) {
      logging("${cmd}", "debug")
    if (cmd.batteryLevel == 0xFF) { 
        logging("Low battery FLAG FF", "warn")
        return
     } 
    if (cmd.batteryLevel == 0) { 
        logging("Ignoring false bat 0 ", "debug")
        return
     } 
    
    state.LastBat = device.currentValue("battery")
    if (state.LastBat != cmd.batteryLevel ){ 
     logging("battery: ${cmd.batteryLevel} ", "info")
     sendEvent(name: "battery", value: cmd.batteryLevel,unit: "%", descriptionText: "${cmd.batteryLevel} ${state.LastCheckin} v${state.version}", isStateChange:true)
    }
}


/**
 *  zwaveEvent( COMMAND_CLASS_MANUFACTURER_SPECIFIC V2 (0x72) : MANUFACTURER_SPECIFIC_REPORT (0x05) )
 *
 *  Manufacturer-Specific Reports are used to advertise manufacturer-specific information, such as product number
 *  and serial number.
 **/
def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  logging("${cmd} ", "debug")
    
  // does not send name in text  
   def map = [:]
    map.mfr   = hubitat.helper.HexUtils.integerToHexString(cmd.manufacturerId, 2)
    map.model = hubitat.helper.HexUtils.integerToHexString(cmd.productId, 2)
    map.type  = hubitat.helper.HexUtils.integerToHexString(cmd.productTypeId, 2)
    state.fingerprint = "${map.mfr}-${map.type}-${map.model}"
    logging("fingerprint ${state.fingerprint}", "debug")
    
    
    updateDataValue("manufacturer","Iris Utilitech")
    updateDataValue("manufacturerId","${cmd.manufacturerId}")
    updateDataValue("deviceType","${cmd.productTypeId}")
    updateDataValue("model","TSE07-1")  
    updateDataValue("deviceId","${cmd.productId}") 
    
}
    

/**
 *  zwaveEvent( COMMAND_CLASS_ALARM (0x71) : ALARM_REPORT (0x05) )
 *
 *  The Alarm Report command used to report the type and level of an alarm.
 *
 *  cmd attributes:
 *    Short  alarmLevel  Application specific
 *    Short  alarmType   Application specific
 **/
def zwaveEvent(hubitat.zwave.commands.alarmv1.AlarmReport cmd) {
	logging("${cmd} ", "debug")
    
    if (cmd.alarmLevel == 0 && cmd.alarmType==0){return}// why send us 0 0 AllClear?

    if (cmd.alarmLevel == 1 && cmd.alarmType==2){
        logging("Alarm Type 2 - Powering Up OK", "info")
        return
    }
    // Way to many false alarms here (unusable) Just log for now
    if (cmd.alarmLevel == 1 && cmd.alarmType==1){
        logging("Alarm Type 1 - Battery Low", "debug")
        return
    }
       
    logging("Unknown Alarm Level:${cmd.alarmLevel} Type:${cmd.alarmType}", "warn")
}





def zwaveEvent(hubitat.zwave.Command cmd) {
	logging("${cmd} ", "debug")

}




void getIcons(){
    state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/utilitech-siren.jpg' >"    
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"


 }




// Logging block  v5
void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
        if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
}

void loggingCheck(){ 
// not working fix later
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
