/*
Lock Button Monitor
Checks for locked by external button and links to button press
Hubitat HSM armed by external lock button.
Schlage and Vale locks.

Uses to detect external door lock and switch to away mode or arm HSM
For hubitat


Installation Notes:
Create a Virtual button. set buttons to 5

set lock and button in app

This works for one door only at this time.
If you need more than one door you will have
to save more than one copy of this code and change its name.

============================================================
Here is the key to the button push. Monitor with your rules

Button 1 = Locked by keypad button
Button 2 = Locked by Thumb Turn
Button 3 = Unlocked by Thumb Turn
Button 4 = Unlocked by Code
Button 5 = Unlocked by HUB 


=============================================================
 10/21/2022  v1.3  Unlocked by keypad added
 09/16/2022  v1.2  Support locks that use VIA instead of BY in description.
 06/28/2022  v1.1  working version. One lock only
 06/27/2022  v1.0  Testing version.....


*/

def setVersion(){
    state.name = "Lock Button Monitor ${app.lock}"
    state.version ="1.3"
//    updateLabel("Lock Button Monitor ${app.lock}")

//    updateLabel(state.name)
    def appLabel = app.getLabel()
//    removeSetting(switchMode)
//    log.debug "app label is ${appLabel}"

}

definition(
    name: "Lock Button Monitor V1",
    namespace: "tmaster",
    author: "tmaster",
    description: "Triggers when outside button locks the door",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/lock%20button%20monitor.groovy"
)


def lock = [
    name: "lock",
    type: "capability.lock",
    title: "Lock to monitor",
    description: "Select the lock to monitor.",
    required: true,
    multiple: false
]

def virtuaButton = [
    name: "virtuaButton",
    type: "capability.pushableButton",
    title: "Virtual Button",
    description: "Virtual button to receive results.",
    required: true,
    multiple: false
]

def enableLogging = [
    name:				"enableLogging",
    type:				"bool",
    title:				"Enable debug Logging?",
    defaultValue:		true,
    required:			true
]
def switchMode = [
    name:				"switchMode",
    type:				"bool",
    title:				"Switch to AWAY mode on lock?",
    defaultValue:		false,
    required:			true
]
preferences {
    page(name: "mainPage", title: "<b>${app.label} [${state.name}]:</b>", install: true, uninstall: true) {
		section("") {
            input name
			input lock

		}
       		section("Button 1 = Locked by keypad button<br>Button 2 = Locked by Thumb Turn<br>Button 3 = Unlocked by Thumb Turn<br>Button 4 = Unlocked by Code<br>Button 5 = Unlocked by HUB<br>Button 6 = Lock brand not setup") {

			input virtuaButton
		}
       section ("version ${state.version} by Tmaster ") {
//            input switchMode
//			input enableLogging
		}
	}
}

def installed() {
	log.debug "Lock monitor${state.version} Installed:${settings}"
	initialize()
}

def updated() {
    log.debug "Lock monitor${state.version} Updated:${settings}"
	unsubscribe()
	initialize()
    state.lock = lock
}

def initialize() {
//    log.info  "Lock monitor: Lock status: ${lock.displayName} " + lockStatus()
	subscribe(lock, "lock.lastCodeName", codeName)
    subscribe(lock, "lock.unlocked", unlockHandler)
    subscribe(lock, "lock.locked", lockHandler)
//  subscribe(lock, "locked", lockHandler)
//  subscribe(lock, "lock", lockStatus)
    setVersion()
}

def codeName(evt){
name = evt.value

    
}

def lockStatus(evt) {
    log.debug ("Lock monitor: Lock Status: ${evt.descriptionText}")
return
}

def unlockHandler(evt) {
setVersion()
    
   if(!name){name="NA"}
    
   if (evt.descriptionText.contains('unlocked by thumb turn')){
   log.info ("Lock monitor: unlocked by thumb turn")
   virtuaButton.push(3)
   return
   }
   if (evt.descriptionText.contains('ulocked by key or thumbturn')){
   log.info ("Lock monitor: UnLocked by thumb turn")
   virtuaButton.push(3)    
   return    
    } 
   if (evt.descriptionText.contains('was unlocked by')){//was unlocked by 
       log.info ("Lock monitor: UnLocked by a valid code ${name}")
   virtuaButton.push(4)
   return    
    } 
   if (evt.descriptionText.contains('was unlocked via')){//was unlocked by 
       log.info ("Lock monitor: UnLocked by a valid code ${name}")
   virtuaButton.push(4)
   return    
    }   
if (evt.descriptionText.contains('was unlocked with keypad')){//was unlocked by 
       log.info ("Lock monitor: UnLocked by a valid code ${name}")
   virtuaButton.push(4)
   return    
    }    
    
	
    
   if (evt.descriptionText.contains('was unlocked [digital]')){
   log.info ("Lock monitor: was unlocked by HUB")
   virtuaButton.push(5)    
   return    
    }  

   
log.warn ("Lock monitor: Error: [${evt.descriptionText}]")
 
}

// Schlage log results to scan
// was unlocked by XXXX 
// was locked by keypad [physical]
// was unlocked by thumb turn [physical]
// was locked by thumb turn [physical]
// was locked [digital]
// was unlocked [digital]

// Yale log results
// was unlocked by XXXX 
// was locked by button [physical]
// was unlocked by key or thumbturn [physical]
// was locked by key or thumbturn [physical]
// was locked [digital]
// was unlocked [digital]



def lockHandler(evt) {
	setVersion()

    if (evt.descriptionText.contains('locked by keypad')){
    log.info ("Lock monitor: Locked by Keypad")
    virtuaButton.push(1)
    return    
    }
    if (evt.descriptionText.contains('locked by button')){
    log.info ("Lock monitor: Locked by button")
    virtuaButton.push(1)
    return    
    }
    if (evt.descriptionText.contains('locked via button')){
    log.info ("Lock monitor: Locked by button")
    virtuaButton.push(1)
    return    
    }    
    
    if (evt.descriptionText.contains('was locked by thumb turn')){
    log.info ("Lock monitor: Locked by thumb turn")
    virtuaButton.push(2)
    
    return    
    } 
     if (evt.descriptionText.contains('locked by key or thumbturn')){
    log.info ("Lock monitor: Locked by thumb turn")
    virtuaButton.push(2)
   
    return    
    }    

    
    if (evt.descriptionText.contains('was locked [digital]')){
    log.info ("Lock monitor: Locked by HUB")
    virtuaButton.push(5)    
    return    
    } 
    
       
     
log.warn ("Lock monitor: Error: [${evt.descriptionText}]")

}



def log(msg) {
    if (enableLogging) {
        log.debug msg
    }
}
