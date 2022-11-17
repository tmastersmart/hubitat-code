/** Zigbee Sonoff - generic Relays/Outlets
driver for hubitat

Sonoff MINI ZB ,eWeLink ,3A Smart Home ,Generic
Generic zigbee relays
Lamp_01,Plug_01, SA-003-Zigbee, 01MINIZB, BASICZBR3, LXN59-1S7LX1.0
Sylvania Smart + LEDVANCE

This driver was created to handel all my Sonoff MINI ZB / eWeLink /3A Smart Home /Generic relays.
These relays all use the same formats but have diffrent problems with internal drivers.

It also works with Sylvania Smart + LEDVANCE outlets as likely many more generic outlets.

Suports alarm,strobe,siren,refreash and presence.

If it works with your relay and outlet and its not listed Send me your fingerprints so they can be added.
To create a fingerprint switch driver to DEVICE and press get info....The fingerprint will be in the log

-----Warning --------
If you are switching from another driver you must FIRST switch to internal driver (zigbee generic outlet)
and press config. This repairs improper binding from other drivers. Otherwise you will get a lot of unneeded traffic.

---------------------------------------------------------------------------------------------------------
 1.6.4 11/17/2022   Plug_01 fingerprint added
 1.6.3 11/15/2022   Cluster code rewrite
 1.6.1 11/12/2022   More bug fixes in presence
 1.6.0 11/10/2022   Added retry to recovery mode was creating false non present alarms
 1.5.7 11/05/2022   SA-003-Zigbee images added. This Fingerprint can be a relay or a round outlet same ID 
 1.5.6 11/03/2022   Added ping. Added disable presence schedule
 1.5.5 11/01/2022   Removed unschedule. Rewrites
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
    TheVersion="1.6.4"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils
metadata {
    
	definition (name: "Zigbee - Sonoff - generic Relays/Outlets", namespace: "tmastersmart", author: "tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/generic-zigbee-relays.groovy") {

        capability "Health Check"
		capability "Actuator"
		capability "Configuration"
		capability "EnergyMeter"
		capability "Initialize"
		capability "Outlet"
		capability "PresenceSensor"
		capability "Refresh"
		capability "Switch" 
        capability "Alarm"

        command "uninstall"
        command "checkPresence"

		attribute "strobe", "string"
		attribute "siren", "string"

        fingerprint model:"BASICZBR3",     manufacturer:"SONOFF",          deviceJoinName:"SONOFF Relay BASICBR3", profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006",outClusters:"0000"
	    fingerprint model:"01MINIZB",      manufacturer:"SONOFF",          deviceJoinName:"SONOFF Relay MINI",     profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,FC57",outClusters:"0019"	
        fingerprint model:"SA-003-Zigbee", manufacturer:"eWeLink",         deviceJoinName:"eWeLink Relay",         profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000"
        fingerprint model:"Lamp_01",       manufacturer:"SZ",              deviceJoinName:"Generic Relay",         profileId:"0104", endpointId:"0B", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000", application:"01"
        fingerprint model:"LXN59-1S7LX1.0",manufacturer:"3A Smart Home DE",deviceJoinName:"Inline Switch",         profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006", outClusters:"", application:"01"
        fingerprint model:"PLUG",          manufacturer:"LEDVANCE"        ,deviceJoinName:"Sylvania Smart +",      profileId:"0104", endpointId:"01", inClusters:"0000,0003,0004,0005,0006,0B05,FC01,FC08", outClusters:"0003,0019" 
        fingerprint model:"Plug_01",       manufacturer:"SZ",                                                      profileId:"0104", endpointId:"0B", inClusters:"0000,0003,0004,0005,0006", outClusters:"0000", application:"01"
// 2 devices share the same fingerprint "SA-003-Zigbee""eWeLink" one a relay one a round outlet
//  Plug_01   
    }

}
// If the above fingerprint doesnt work please send me yours. You can get it using internal GENERIC DEVICE driver Just press info


//https://zigbee.blakadder.com/Zemismart_ZW-EU-01.html

preferences {
	
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

	input name: "disableLogsOff",type: "bool", title: "Disable Auto Logs off", description: "For debugging doesnt auto disable", defaultValue: false

    input name: "resendState",  type: "bool", title: "Resend Last State on Refresh", description: "If Refresh does not wake up your device use this", defaultValue: false
    input name: "pollHR" ,	    type: "enum", title: "Check Presence Hours",description: "Chron Schedule. 0=disable Press config after saving",options: ["0","1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: "10",required: true 
   
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
    state.remove("MFD")
    state.remove("tries")

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
state.remove("bin")
state.remove("checkPhase")    
	// Remove unnecessary device details.
    device.deleteCurrentState("alarm")
    configure()
    clientVersion()
    
	// multi devices will run this on reboot make sure they all use a diffrent time
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)

}


def configure() {
    logging("Config", "info") 
	// Runs on reboot paired or rejoined
	unschedule()
    
    
 

    if (!pollHR){ pollHR= 8}
    state.poll = pollHR
    if (state.poll != 0){ 
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)    
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
    logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${state.poll} * * ? *", "debug") 
    schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${state.poll} * * ? *", checkPresence)	
    logging("Presence Check Every ${state.poll}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
    }     
    
    
    
}


def updated() {
	// Runs whenever preferences are saved.
    clientVersion()
	loggingUpdate()
    ping() 
    getIcons()
}





void refresh(cmd) {
    if(state.DataUpdate){ logging("Refreshing ${state.MFR} ${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}
delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000)),// switch
    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000)),// switch
   ], 1000)    

    
    if(resendState){
      Test = device.currentValue("switch")  
      if (Test =="on"){runIn(4,on)}
      else {runIn(4,off)} 
    }
}

def ping() {
    logging("Ping ", "info")
        delayBetween([
    sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000)),// switch
    sendZigbeeCommands(zigbee.readAttribute(0x0006, 0x0000)),// switch      
   ], 900) 
    
    if(resendState){
      Test = device.currentValue("switch")  
      if (Test =="on"){runIn(4,on)}
      else {runIn(4,off)} 
    }
    
}




def alarm(cmd){
    logging("Alarm ON", "info")
    sendEvent(name: "alarm", value: "on",isStateChange: true)
  on()
}
                   
def siren(cmd){
    logging("siren ON", "info")
    sendEvent(name: "siren", value: "on",isStateChange: true)
  on()
}
def strobe(cmd){
    logging("strobe ON", "info")
    sendEvent(name: "strobe", value: "on",isStateChange: true)
  on()
}
def both(cmd){
    logging("both ON", "info")
    sendEvent(name: "siren", value: "on",isStateChange: true)
    sendEvent(name: "strobe", value: "on",isStateChange: true)
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
    // presence routine. v5 11-12-22
    if(!state.tries){state.tries = 0} 
    state.lastPoll = new Date().format('MM/dd/yyyy h:mm a',location.timeZone) 
    def checkMin = 200
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
    state.lastCheckin = now()
    checkPresence()
    logging("Raw: [${description}]", "trace")


    if (description?.startsWith('enroll request')) { 
        zigbee.enrollResponse() 
        return  
    }  
    

    Map descMap = zigbee.parseDescriptionAsMap(description) 
    logging("map: ${descMap}", "trace")


    // fix parse Geting 2 formats so merge them
    if (descMap.clusterId) {descMap.cluster = descMap.clusterId} 
   
    if (descMap.cluster == "0006" || descMap.clusterId == "0006" ) {
      if (descMap.value){status = descMap.value}
      if (descMap.data) {status = descMap.data[0]}
        if(!status){
            logging("Ignoring 0006 Wrong format for on/off ${descMap}", "warn")  
            return
        }
        
        logging("ON/OFF report [${status}]", "debug")
      if (status == "01"){onEvents()}
      if (status == "00"){offEvents()}
     

}else if (descMap.cluster == "0000" ) {
        if (descMap.attrId== "0001" ){
        logging("Application ID :${descMap.value}", "debug")
        // should be 0x0104=Home Automation 
        } 
        if (descMap.attrId== "0004" && descMap.attrInt ==4){
        logging("Manufacturer :${mdescMap.value}", "debug") 
        state.MFR = descMap.value 
        updateDataValue("manufacturer", state.MFR)
        state.DataUpdate = true                     
        } 
        if (descMap.attrId== "0005" && descMap.attrInt ==5){
        logging("Model :${descMap.value}", "debug")
        state.model = descMap.value    
        updateDataValue("model", state.model)
        state.DataUpdate = true    
        }
       
 
// just ignore these unknown clusters for now
}else if (descMap.cluster == "0500" ||descMap.cluster == "0006" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8021" || descMap.cluster == "8038" || descMap.cluster == "8005" ) {
   text= ""
      if (descMap.cluster =="8001"){text="GENERAL"}
 else if (descMap.cluster =="8021"){text="BIND RESPONSE"}
 else if (descMap.cluster =="8031"){text="Link Quality"}
 else if (descMap.cluster =="8032"){text="Routing Table"}

   
   if (descMap.data){text ="${text} clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Ignoring ${map.cluster} ${text}", "debug") 

}else if (descMap.cluster =="0013"){logging("${descMap.cluster} Multistate event (Rejoining) data:${descMap.data}", "debug") 
   
        
 }  else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

    }

// prevent dupe events
def onEvents(){
    Test = device.currentValue("switch")
    if (Test != "on"){
        logging("is ON our state was:${Test}", "info")
        sendEvent(name: "switch", value: "on",isStateChange: true)
    }
    else {logging("is ON our state was:${Test}", "debug")}
}
def offEvents(){
    alarmTest = device.currentValue("alarm")   
    if (alarmTest != "off"){sendEvent(name: "alarm",  value: "off",isStateChange: true)}
    alarmTest = device.currentValue("siren") 
    if (alarmTest != "off"){sendEvent(name: "siren",  value: "off",isStateChange: true)} 
    alarmTest = device.currentValue("strobe") 
    if (alarmTest != "off"){sendEvent(name: "strobe", value: "off",isStateChange: true)}

    Test = device.currentValue("switch")
    if (Test != "off"){
        logging("is OFF our state was:${Test}", "info")
        sendEvent(name: "switch", value: "off",isStateChange: true)
    }
    else {logging("is OFF our state was:${Test}", "debug")}
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
    if (state.model == "SA-003-Zigbee"){ state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/SA-003-Zigbee.jpg' >"}
    if (state.model == "Lamp_01"){       state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/Lamp_01.jpg' >"} 
    if (state.model == "Plug_01"){       state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/Plug_01.jpg' >"} 
    if (state.model == "LXN59-1S7LX1.0"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/LXN59-1S7LX1.0.jpg' >"}
    if (state.model == "PLUG" && state.MFR =="LEDVANCE"){state.icon ="<img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/sylvania-smart-plus.jpg' >"}
    

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
