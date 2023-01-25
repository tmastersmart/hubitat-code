// Runs after first pairing.
def installed() {
logging("Installed ", "warn")    
state.DataUpdate = false
pollHR = 10
pingIt = 30 
state.minVoltTest = 2.2   
configure()   
updated()
    
}
// Runs on reboot
def initialize(){
    logging("initialize ", "debug")
    clientVersion()    
  	randomSixty = Math.abs(new Random().nextInt() % 180)
	runIn(randomSixty,refresh)
}
