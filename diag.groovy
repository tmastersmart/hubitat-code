/* Iris v1 Diag Pad Driver
for Hubitat
 */
def clientVersion() {
    TheVersion="1.0"
 if (state.version != TheVersion){ 
     state.version = TheVersion
     configure() 
 }
}


metadata {

	definition (name: "Iris v1 Diag", namespace: "tmastersmart", author: "Tmaster", importUrl: "") {


capability "Switch"


		

fingerprint profileId: "C216", inClusters: "00F0,00C0", outClusters: "", manufacturer: "AlertMe", model: "Device", deviceJoinName: "Iris V1"
	}

}


preferences {
	
	input name: "infoLogging",  type: "bool", title: "Enable info logging", description: "Recomended low level" ,defaultValue: true
	input name: "debugLogging", type: "bool", title: "Enable debug logging", description: "MED level Debug" ,defaultValue: false
	input name: "traceLogging", type: "bool", title: "Enable trace logging", description: "Insane HIGH level", defaultValue: false


}

def installed(){logging("${device} : Paired!", "info")}

 
def initialize() {
//state.batteryOkay = true

   
//sendEvent(name: "battery",value:100, unit: "%", isStateChange: false)



// Stagger our device init refreshes or we run the risk of DDoS attacking our hub on reboot!
randomSixty = Math.abs(new Random().nextInt() % 60)
runIn(randomSixty,refresh)
// Initialisation complete.
logging("${device} : Initialised", "info")
}


def configure() {

	initialize()
	unschedule()

	// Default logging preferences.
	device.updateSetting("infoLogging",[value:"true",type:"bool"])
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	device.updateSetting("traceLogging",[value:"false",type:"bool"])

	// Schedule our ranging report.
	int checkEveryHours = 10 // Request a ranging report and refresh every x hours.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	randomTwentyFour = Math.abs(new Random().nextInt() % 24)
	schedule("${randomSixty} ${randomSixty} ${randomTwentyFour}/${checkEveryHours} * * ? *", rangeAndRefresh)
    // At X seconds past X minute, every checkEveryHours hours, starting at Y hour.

	// Schedule the presence check.
	int checkEveryMinutes = 50 // Check presence timestamp every 6 minutes.						
	randomSixty = Math.abs(new Random().nextInt() % 60)
	schedule("${randomSixty} 0/${checkEveryMinutes} * * * ? *", checkPresence)// At X seconds past the minute, every checkEveryMinutes minutes.

	// Configuration complete.
	logging("${device} : Configured", "info")

	// Run a ranging report and then switch to normal operating mode.
	rangingMode()
	runIn(12,normalMode)
	
}


def updated() {
	loggingStatus()

// Dont auto turn off this is diag    
    
//	runIn(3600,debugLogOff)
//	runIn(1800,traceLogOff)
	refresh()

}




 
  

def setEntryDelay(code){logging("${device} : setEntryDelay ${code}  unsupported", "info")}
def setExitDelay(code){	logging("${device} : setExitDelay  ${code}  unsupported", "info")}
def setCodeLength(code){logging("${device} : setCodeLength 4", "info")                   }







def on() {

cluster = 0x00C0
attributeId = 0x020
dataType = DataType.ENUM8
value = 0x03    

 writeAttribute(cluster, attributeId, dataType, value)
return
}




void loggingStatus() {
	log.info "${device} : Logging : ${infoLogging == true}"
	log.debug "${device} : Debug Logging : ${debugLogging == true}"
	log.trace "${device} : Trace Logging : ${traceLogging == true}"
}


void traceLogOff(){
	device.updateSetting("traceLogging",[value:"false",type:"bool"])
	log.trace "${device} : Trace Logging : Automatically Disabled"
}


void debugLogOff(){
	device.updateSetting("debugLogging",[value:"false",type:"bool"])
	log.debug "${device} : Debug Logging : Automatically Disabled"
}



private boolean logging(String message, String level) {

	boolean didLog = false

	if (level == "error") {
		log.error "$message"
		didLog = true
	}

	if (level == "warn") {
		log.warn "$message"
		didLog = true
	}

	if (traceLogging && level == "trace") {
		log.trace "$message"
		didLog = true
	}

	if (debugLogging && level == "debug") {
		log.debug "$message"
		didLog = true
	}

	if (infoLogging && level == "info") {
		log.info "$message"
		didLog = true
	}

	return didLog

}
