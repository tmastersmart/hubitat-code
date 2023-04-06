/**
 *  SAGE Doorbell Sensor 
    Presence and bat support
    Hubitat driver. 



Adds battery support (simlated) if stops reporting bat goes to 0

White wire:  common
Green wire:  doorbell 1  front
Yellow wire: doorbell 2  rear


If device keeps sending 0000 0006 pings switch to internal driver 
and use config option then switch back.
Help is needed do you know the command to send to stop the reporting above?


 Factory reset:
 Press and hold the RESET button until the red LED blinks.


================================================================================
v2.8.6  04/06/2023  detection of hub bug
v2.8.4  12/21/2022  Bat fix
v2.8.3  11/22/2022  cluster 0013
v2.8.2  11/12/2022  nother bug fix for presence
v2.8.0  11/11/2022  Presence updated with retries
v2.7.0  11/05/2022  Merged in changes made in Light switch driver,added schedule options
v2.6.0  10/30/2022  Presence Bug Fix Was not warning first
v2.5.6  10/17/2022  bug fixes and cleanup
v2.5.4  10/15/2022  Config changes
v2.5.1  10/14/2022  Adjusted uninstall to remove some left overs
                    Changed sending zigbee routines and delays
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
    TheVersion="2.8.6"
if (state.version != TheVersion){
    logging("Upgrading ! ${state.version} to ${TheVersion}", "warn")
     state.version = TheVersion
     configure() 
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
        command "uninstall"

 
       
        fingerprint endpointId: "12", inClusters: "0000,0003,0009,0001", outClusters: "0003,0006,0008,0019", model: "Bell", manufacturer: "Echostar"
    }

    preferences {
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

	input name: "timeBetweenPresses",type: "number",title: "Seconds allowed between presses (increase this value to eliminate duplicate notifications)",required: true,displayDuringSetup: true,defaultValue: 10
	input name: "button1Name",type: "text",title: "Doorbell name to be associated with the 'Front Door' contact...",required: false,defaultValue: "Front Door",displayDuringSetup: true
    input name: "button2Name",type: "text",title: "Doorbell name to be associated with the 'Rear Door' contact...",required: false, defaultValue: "Rear Door",displayDuringSetup: true

    input name: "pollYes",type: "bool", title: "Enable Presence", description: "", defaultValue: true,required: true
    input name: "pollHR" ,type: "enum", title: "Check Presence Hours",description: "Press config after saving",options: ["1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20"], defaultValue: 15 ,required: true 
     
    }
 } 
def installed() {
	logging("Paired!", "info")
    configure()
    updated()
    runIn(20,refresh)
}


def uninstall() {
	unschedule()
    state.remove("lastCheckInMin") 
    state.remove("lastCheckin")
    state.remove("timeBetweenPresses")
    state.remove("lastButton2Updated")
    state.remove("lastButton1Updated")
    state.remove("version")
    
device.deleteCurrentState("presence")
device.deleteCurrentState("battery")     
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
    getIcons()
    
    unschedule()
    
    
    if (pollYes){ 
	randomSixty = Math.abs(new Random().nextInt() % 60)
    randomSixty2 = Math.abs(new Random().nextInt() % 60)    
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
    logging("CHRON: ${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", "debug") 
    schedule("${randomSixty2} ${randomSixty} ${randomTwentyFour}/${pollHR} * * ? *", checkPresence)	
    logging("Presence Check Every ${pollHR}hrs starting at ${randomTwentyFour}:${randomSixty}:${randomSixty2} ", "info") 
    }	

    buttons = device.currentValue("numberOfButtons")
    if (buttons != 2){sendEvent(name: "numberOfButtons", value: 2, displayed: true)}
    sendEvent(name: "battery", value: 90, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)

    if(!timeBetweenPresses){timeBetweenPresses = 10}
	String zigbeeEui = swapEndianHex(device.hub.zigbeeEui)
    logging("Configure", "info")
    
    
   delayBetween([
            sendZigbeeCommands(["zcl global write 0x500 0x10 0xf0 {${zigbeeEui}}"]),
            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 1"]),
       
            sendZigbeeCommands(["zdo bind 0x${device.deviceNetworkId} ${endpointId} 1 1 {${device.zigbeeId}} {}"]),
            sendZigbeeCommands(["zcl global send-me-a-report 1 0x20 0x20 30 21600 {01}"]),		//checkin time 6 hrs

            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 1"]),
                                
            sendZigbeeCommands(["zdo bind 0x${device.deviceNetworkId} ${endpointId} 1 0x402 {${device.zigbeeId}} {}"]),
            sendZigbeeCommands(["zcl global send-me-a-report 0x402 0 0x29 30 21600 {6400}"]),   //checkin time 6 hrs

//            sendZigbeeCommands(["zcl global send-me-a-report 0x402 0 0x29 30 3600 {6400}"]),
            sendZigbeeCommands(["send 0x${device.deviceNetworkId} 1 1"]),
            sendZigbeeCommands(zigbee.enrollResponse())                
   ], 1000)
//  “send-me-a-report” cluster, attribute, data type, min report, max report,       
runIn(8,refresh)     
   


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
        sendEvent(name: "battery", value: 90, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)  
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
    state.lastCheckin = now()
    checkPresence()
    logging("Raw [${description}]", "trace")
    Map descMap = zigbee.parseDescriptionAsMap(description)
    logging("Map ${descMap}", "trace")
    logging("profileId:${descMap.profileId} clusterId:${descMap.clusterId} clusterInt:${descMap.clusterInt} sourceEndpoint${descMap.sourceEndpoint} destinationEndpoint${descMap.destinationEndpoint} options:${descMap.options} command:${descMap.command} data:${descMap.data}", "trace")
//profileId:0000 clusterId:0006 clusterInt:6 sourceEndpoint00 destinationEndpoint00 options:0040 command:00 data:[25, 00, 00, 04, 01, 01, 19, 00, 00]    

  
    if (description?.startsWith('enroll request')) { 
 //        enrollResponse()
        zigbee.enrollResponse()
        return  
    }  
       if (descMap.clusterId){descMap.cluster = descMap.clusterId} // fix for 2 formats
   
// def evt = zigbee.getEvent(description)
// if (evt){logging("Event: ${evt}", "debug")} // testing     
 
 if (descMap.clusterId == "0006"  && descMap.profileId == "0104" ){
            def buttonNumber = (descMap.command as int)
            if (buttonNumber == 1 && !isDuplicateCall(state.lastButton1Updated, timeBetweenPresses)){	
                push(1)
                state.lastButton1Updated = new Date().time 
            }
            if (buttonNumber == 0 && !isDuplicateCall(state.lastButton2Updated, timeBetweenPresses)){ 
                push(2)
                state.lastButton2Updated = new Date().time	
            }
           
 
//}else if (descMap.cluster == "0001" && descMap.attrId == "0020") {//Power configuration
//        battery = Integer.parseInt(descMap.value, 16)
//        logging("Battery: is TRUE", "info")
//        logging("${descMap}", "warn")
// device does not support battery cluster  
     
/// profileId:0000 clusterId:0006 clusterInt:6 sourceEndpoint00 destinationEndpoint00 options:0040 command:00 data:[C3, FD, FF, 04, 01, 01, 19, 00, 00]  <-hub error causes this
}else if (descMap.cluster == "0006" && descMap.profileId == "0000"){
    logging("HUB bug is back ->Cluster 0000 id:0006 I cant fix this and it will run down the BAT --- Trying enrollResponse", "error")
    zigbee.enrollResponse()  
    return
   
   
}else if (descMap.cluster == "0000"){
    if( descMap.attrId == "0004") {
    logging("Manufacturer:${descMap.value} ", "debug")
    state.MFR = descMap.value     
    updateDataValue("manufacturer", descMap.value)
    }
   if (descMap.attrId == "0005") {
    logging("Device:${descMap.value} ", "debug")
    state.model = descMap.value    
    state.DataUpdate = true   
    updateDataValue("device", descMap.value)
    updateDataValue("partNo", "206612")
    updateDataValue("model", "DRBELL")
    updateDataValue("fcc", "DKN-401DM")
    }   
   if (descMap.clusterId == "0006" ){
    logging("HUB bug is back ->Cluster 0000 id:0006 I cant fix this and it will run down the BAT --- Trying enrollResponse", "error")
    zigbee.enrollResponse()
    return
   }  

}else if (descMap.clusterId == "0013") {
        logging("0013 Enroll Request. ", "info")
        zigbee.enrollResponse()   
        return


}else if (descMap.cluster == "8032" ||descMap.cluster == "8031" || descMap.cluster == "8021" ||descMap.cluster == "0500" || descMap.cluster == "0000" ||descMap.cluster == "0001" ||descMap.cluster == "0006" || descMap.cluster == "0402" || descMap.cluster == "8038" || descMap.cluster == "8005") {
      
 text= "unknown"
      if (descMap.cluster =="8001"){text="GENERAL"}
 else if (descMap.cluster =="8021"){text="BIND RESPONSE"}
 else if (descMap.cluster =="8031"){text="Link Quality"}
 else if (descMap.cluster =="8032"){text="Routing Table"}
      
      if (descMap.data){text ="${text} clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Ignoring ${descMap.cluster} ${text}", "debug") 
       


//}else if (descMap.cluster == "0006") {
//        logging("cluster:${descMap.cluster} 0006 Seen after a Enroll Request. Unknown", "debug")
//       zigbee.enrollResponse()
        
 }  else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

} 


def push(cmd){

    if (cmd ==1){ Name = button1Name}
    if (cmd ==2){ Name = button2Name}

    logging("${Name} button ${cmd} Pressed!", "info")
    sendEvent(name: "pushed", value: cmd, isStateChange: true,descriptionText:"${Name} button Pressed! ${state.version}")

}


def refresh() {
    if(state.DataUpdate){ logging("Refreshing ${state.MFR} ${state.model} Ver:${state.version}", "info")}
    else {logging("Refreshing -unknown device-  Ver:${state.version}", "info")}
    delayBetween([
      sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0004)),// mf
      sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005)),// model
//      sendZigbeeCommands(zigbee.readAttribute(0x0402, 0x0000)),// temp  
//      sendZigbeeCommands(zigbee.readAttribute(0x0001, 0x0020)),// battery not supported
        
//            sendZigbeeCommands(["he rattr 0x${device.deviceNetworkId} 18 0x0001 0x20"]), // bat
//            sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 1 0x12 0x0000 {10 00 00 04 00}"]),
//            sendZigbeeCommands(["he raw 0x${device.deviceNetworkId} 1 0x12 0x0000 {10 00 00 05 00}"]),
   ], 1000)
        
}

def ping() {
    logging("ping", "info")
      sendZigbeeCommands(zigbee.readAttribute(0x0000, 0x0005))// model
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

void sendZigbeeCommands(List<String> cmds) {
    logging("sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}


// Logging block  v4

void loggingUpdate() {
    logging("Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]", "infoBypass")
    // Only do this when its needed
    if (debugLogging){
        logging("Debug log:off in 3000s", "warn")
        runIn(3000,debugLogOff)
    }
    if (traceLogging){
        logging("Trace log: off in 1800s", "warn")
        runIn(1800,traceLogOff)
    }
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

void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
 }
