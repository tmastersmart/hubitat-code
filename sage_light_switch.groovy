/**
 *  SAGE Light Switch 
    Presence and bat support
    Hubitat driver. 206611 
    FCC ID :DKN-301LM


Adds battery support (simlated) if stops reporting bat goes to 0



If device keeps sending 0000 0006 pings switch to internal driver 
and use config option then switch back.
Help is needed do you know the command to stop the above?


 Factory reset:
 Hold down ON while inserting battery it will start flashing red


================================================================================
v2.7.1  11/05/2022 added schedule options
v2.7.0  11/04/2022 Imported Sage doorbell code and modified into a switch
v2.6.0  10/30/2022  Presence Bug Fix Was not warning first







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



uses some source code from. 
https://github.com/trunzoc/Hubitat/blob/master/Drivers/Sage_Doorbell/Sage_Doorbell.groovy (11/27/2019)
https://github.com/rbaldwi3/Sage-HVAC-Sensor/edit/master/groovy

 */
import hubitat.zigbee.clusters.iaszone.ZoneStatus
import hubitat.zigbee.zcl.DataType
import hubitat.helper.HexUtils

def clientVersion() {
    TheVersion="2.7.1"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() // Forces config on updates
 }
}


metadata {
    definition (name: "Sage light switch echostar", namespace: "tmastersmart", author: "Tmaster", importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/sage_light_switch.groovy") {

		capability "Configuration"
        capability "Pushable Button"
        capability "ReleasableButton"
		capability "Refresh"
        capability "Battery"
        capability "PresenceSensor"
        capability "Actuator"
		capability "Switch"
        
        command "checkPresence"

        command "uninstall"

 
fingerprint model:" Switch", manufacturer:" Echostar", profileId:"0104", endpointId:"12", inClusters:"0000,0003,0009,0001", outClusters:"0003,0006,0008,0019", application:"02"        
        
        
    }

    preferences {
    input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true,required: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false,required: true
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false,required: true

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
    state.remove("lastButton1Updated")
    state.remove("lastButton2Updated")
 //   removeDataValue("softwareBuild")//00000009
 //   removeDataValue("firmwareMT")//1014-0002-00000009

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
    def checkMin  = 1400 // 24 hrs warning
    def checkMin2 = 2800 // 48 hrs [not present] and 0 batt
    // New shorter presence routine. v2 10/22
//    def checkMin  = 5  // 5 min warning
//    def checkMin2 = 10 // 10 min [not present] and 0 batt
    def timeSinceLastCheckin = (now() - state.lastCheckin ?: 0) / 1000
    def theCheckInterval = (checkInterval ? checkInterval as int : 2) * 60
    state.lastCheckInMin = timeSinceLastCheckin/60
    logging("Check Presence its been ${state.lastCheckInMin} mins","trace")
    if (state.lastCheckInMin <= checkMin){ 
        test = device.currentValue("presence")
        if (test != "present"){
        value = "present"
        logging("Creating presence event: ${value}","info")
        sendEvent(name:"presence",value: value , descriptionText:"${value} ${state.version}", isStateChange: true)
        sendEvent(name: "battery", value: 90, unit: "%",descriptionText:"Simulated ${state.version}", isStateChange: true)    
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
// parse =====================================================
def parse(String description) {
    state.lastCheckin = now()
    checkPresence()
    logging("Raw [${description}]", "trace")
    Map descMap = zigbee.parseDescriptionAsMap(description)
    logging("Map :${descMap}", "trace")
    logging("profileId:${descMap.profileId} clusterId:${descMap.clusterId} clusterInt:${descMap.clusterInt} sourceEndpoint${descMap.sourceEndpoint} destinationEndpoint${descMap.destinationEndpoint} options:${descMap.options} command:${descMap.command} data:${descMap.data}", "trace")

  
    if (description?.startsWith('enroll request')) { 
//        enrollResponse()
        zigbee.enrollResponse()
        return  
    }  
    if (descMap.clusterId){descMap.cluster = descMap.clusterId} // fix for 2 formats
    

//profileId:0104 clusterId:0006 clusterInt:6 sourceEndpoint12 destinationEndpoint01 options:0040 command:00 data:[]    
  if (descMap.clusterId == "0006" && descMap.profileId == "0104" ){
            logging("Action ${descMap.command}", "debug")
            if (descMap.command == "01" ){	
                push(1)
                on()
            }
            if (descMap.command == "00" ){ 
                push(2)
                off()
            }
           


//}else if (descMap.cluster == "0001" && descMap.attrId == "0020") {//Power configuration
//        battery = Integer.parseInt(descMap.value, 16)
//        logging("Battery: is TRUE", "info")
//        logging("${descMap}", "warn")
// device does not support battery cluster   
    
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
    updateDataValue("partNo", "206611")
    updateDataValue("model", "SWITCH")
    updateDataValue("fcc", "DKN-301LM")
    }   
   if (descMap.clusterId == "0006" ){
    logging("Cluster 0000 id:0006 ${descMap}", "warn")
    zigbee.enrollResponse()
   }      
//}else if (descMap.profileId == "0000" && descMap.clusterId == "8021") {logging("Replying to cfg? clusterInt:${descMap.clusterInt} data:${descMap.data}", "debug")
//}else if (descMap.profileId == "0000" && descMap.clusterId == "8031") {logging("Link Quality Cluster Event  data:${descMap.data}", "debug")
//}else if (descMap.profileId == "0000" && descMap.clusterId == "8032") {logging("Routing Table Cluster Event  data:${descMap.data}", "debug")

}else if (descMap.cluster == "8032" ||descMap.cluster == "8031" || descMap.cluster == "8021" ||descMap.cluster == "0500" || descMap.cluster == "0000" ||descMap.cluster == "0001" || descMap.cluster == "0402" || descMap.cluster == "8038" || descMap.cluster == "8005") {
      
   text= ""
   if (descMap.data){text ="clusterInt:${descMap.clusterInt} command:${descMap.command} options:${descMap.options} data:${descMap.data}" }
   logging("Ignoring ${descMap.cluster} ${text}", "debug") 
       
}else if (descMap.cluster == "0013") {
        logging("cluster:${descMap.cluster} 0013 Responding to Enroll Request. Likely Battery Change", "info")
        zigbee.enrollResponse()

}else if (descMap.cluster == "0006") {
        logging("cluster:${descMap.cluster} 0006 Seen after a Enroll Request. Unknown", "debug")
        zigbee.enrollResponse()
        
 }  else{logging("New unknown Cluster${descMap.cluster} Detected: ${descMap}", "warn")}// report to dev

}    



def push(cmd){

    if (cmd ==1){ Name = "on"}
    if (cmd ==2){ Name = "off"}
    logging("${Name} button ${cmd} Pressed!", "info")
    sendEvent(name: "pushed", value: cmd, isStateChange: true,descriptionText:"${Name} button Pressed! ${state.version}")

}

def on(){
    Test = device.currentValue("switch")
    logging("Switch On our State:${Test}", "debug")
    if(Test != "on"){
     logging("Switch On", "info")
     sendEvent(name: "switch", value: "on" , isStateChange: true,descriptionText:"Virtual Switch ON ${state.version}")
    }    

}
def off(){
    Test = device.currentValue("switch")
    logging("Switch Off our State:${Test}", "debug")
    if(Test != "off"){
     logging("Switch Off", "info")
     sendEvent(name: "switch", value: "off" , isStateChange: true,descriptionText:"Virtual Switch OFF ${state.version}")
    }    

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
    logging("Sending:${cmds}", "trace")
    sendHubCommand(new hubitat.device.HubMultiAction(cmds, hubitat.device.Protocol.ZIGBEE))
}

// Logging block v3
//	device.updateSetting("infoLogging",[value:"true",type:"bool"])
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

void getIcons(){
    state.donate="<a href='https://www.paypal.com/paypalme/tmastersat?locale.x=en_US'><img src='https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/images/paypal2.gif'></a>"
 }
