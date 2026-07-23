/**
 *  Pool / Spa / Drainage Pump Scheduler 
 * 
 *  For swiming pool pumps that dont have a timer,Check your pools specs
 *  On how long it should run per day and its max run time.
 *
 *  For drainage pumps. If the area cant be drained in several hrs the pump 
 *  will over heat and trip the reset so a cool down time is needed. 
 *  Set max run time and max time per day.
 *
 *  Air pumps for septic systems. Once that expensive controler fails
 *  you can just add a relay and use your hub to control it.
 *  Check max air time per day and max ontime and max off time. 
 *
 *
 * v1.2 7/19/2026   Int version
 * v1.5 7/22/2026   Much debuging changes in monitor and more debug code
 * v1.6 7/23/2026   Insert into Hubitat Package Manager
 */



definition(
    name: "Pump Scheduler",
    namespace: "tmastersmart",
    author: "Tmaster",
    importUrl: "https://raw.githubusercontent.com/tmastersmart/hubitat-code/main/pump_scheduler.groovy",
    description: "Smart scheduler for pool pumps, spa pumps, septic aerators, drainage pumps, and other motorized equipment. Supports daily runtime targets, maximum cycle length, cooldown periods, sunrise/sunset scheduling, monitoring, and automatic recovery after hub reboots or app updates.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
    page(name: "mainPage")
    page(name: "setupPage")
    page(name: "defaultsPage")
    page(name: "advancedPage")
}



// ====================== VERSION CONTROL ======================
def version() { "1.6" }  
def clientVersion() {
    if (state.version != version()) {
        logging("Pump Scheduler Updated to v${version()}","warn")
        state.version = version()
    }
}




def mainPage() {

    dynamicPage(name: "mainPage", title: "Pump Scheduler", install: true, uninstall: true) {

        section("Current Status") {

            paragraph """
Pump: ${pump ? pump.currentSwitch.toUpperCase() : "Not Configured"}

Today's Runtime:
${String.format("%.2f", (state.totalRuntimeToday ?: 0)/3600)} hrs / ${dailyHours ?: 0} hrs

Cycles Today:
${state.cyclesToday ?: 0}

Running:
${state.isRunning ? "YES" : "NO"}
"""
        }

        if(state.stopTime) {
            section("Current Cycle") {
                paragraph "Stops: ${new Date(state.stopTime)}"
            }
        }

        if(state.nextStartTime) {
            section("Cooldown") {
                paragraph "Next Start: ${new Date(state.nextStartTime)}"
            }
        }

        section("Menu") {

            href "setupPage",
                title: "Configuration",
                description: "Pump settings"

            href "advancedPage",
                title: "Advanced",
                description: "Logging and diagnostics"

            href "defaultsPage",
                title: "Equipment Defaults",
                description: "Load preset values"
        }
    }
}

def defaultsPage() {

    dynamicPage(name: "defaultsPage", title: "Equipment Defaults") {

        section("Choose Equipment") {

            input "equipmentPreset",
                  "enum",
                  title: "Preset",
                  submitOnChange: true,
                  options:[
                      "Pool Pump",
                      "Spa Pump",
                      "Septic Aerator",
                      "Drainage Pump",
                      "Other"
                  ]
        }

        if(equipmentPreset){

            section("Load Defaults"){

                paragraph "Press Done to save these defaults."

                if(equipmentPreset=="Pool Pump"){
                    app.updateSetting("dailyHours",[value:"8",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"4",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"30",type:"number"])
                }

                if(equipmentPreset=="Spa Pump"){
                    app.updateSetting("dailyHours",[value:"2",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"2",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"15",type:"number"])
                }

                if(equipmentPreset=="Septic Aerator"){
                    app.updateSetting("dailyHours",[value:"12",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"4",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"60",type:"number"])
                }

                if(equipmentPreset=="Drainage Pump"){
                    app.updateSetting("dailyHours",[value:"18",type:"decimal"])
                    app.updateSetting("maxRunPerCycle",[value:"4",type:"decimal"])
                    app.updateSetting("cooldownMinutes",[value:"30",type:"number"])
                }

            }
        }
    }
}


def setupPage() {

    dynamicPage(name: "setupPage", title: "Pump Scheduler Configuration", install: true, uninstall: true) {

        section("Pump") {

            input "pump",
                  "capability.switch",
                  title: "Select Pump Device",
                  description: "Choose the switch that controls your pump.",
                  required: true

            input "deviceType",
                  "enum",
                  title: "Pump Type",
                  options: [
                      "Pool Pump",
                      "Spa Pump",
                      "Drainage Pump",
                      "Septic Aerator",
                      "Other"
                  ],
                  description: "Used for presets and status messages.",
                  required: true
        }

        section("Daily Schedule") {

            input "dailyHours",
                  "decimal",
                  title: "Total Daily Runtime (hours)",
                  description: "Total amount of time the pump should run each day.",
                  defaultValue: 8.0,
                  required: true

            input "maxRunPerCycle",
                  "decimal",
                  title: "Maximum Continuous Runtime (hours)",
                  description: "Longest the pump may run before taking a cooldown break.",
                  defaultValue: 4.0,
                  required: true

            input "startOption",
                  "enum",
                  title: "Daily Start Time",
                  options: [
                      "Specific Time",
                      "Sunrise",
                      "Sunset"
                  ],
                  description: "Choose when the first run of the day begins.",
                  defaultValue: "Specific Time",
                  required: true

            input "startTime",
                  "time",
                  title: "Start Time",
                  description: "Only used when 'Specific Time' is selected.",
                  required: false
        }

        section("Cooldown") {

            input "cooldownMinutes",
                  "number",
                  title: "Cooldown Between Cycles (minutes)",
                  description: "Time the pump remains OFF before the next run begins.",
                  defaultValue: 30,
                  required: true
        }
    }
}



def advancedPage(){

    dynamicPage(name:"advancedPage",title:"Advanced"){

        
section("Limits") {

    input "maxCyclesPerDay",
          "number",
          title: "Maximum Cycles Per Day",
          description: "Number of run cycles allowed each day. Set to 0 for unlimited cycles.",
          defaultValue: 0,
          required: true
}
//   log.info "Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]"
    
section("Logging") {

    input "infoLogging",
          "bool",
          title: "Enable Info Logging",
          defaultValue: (settings.infoLogging != null ? settings.infoLogging : true)

    input "debugLogging",
          "bool",
          title: "Enable Debug Logging",
          defaultValue: (settings.debugLogging != null ? settings.debugLogging : false)

    input "traceLogging",
          "bool",
          title: "Enable Trace Logging",
          defaultValue: (settings.traceLogging != null ? settings.traceLogging : false)
}

        section("Diagnostics"){

            paragraph """
Current Runtime:${state.totalRuntimeToday}
Current Cycle:${state.cyclesToday}
Running:${state.isRunning}
Stop Timer:${state.stopTime}
Next Start:${state.nextStartTime}
"""
        }

    }
}



def installed() {
    clientVersion()
    logging("Pump - ${pump.displayName} Scheduler Installed","info")
    initialize()
}

def updated() {
    logging("Pump - ${pump.displayName} Scheduler Updated - ${dailyHours} hrs/day | Max ${maxRunPerCycle} hrs per cycle","info")
    unsubscribe()
    unschedule()
    initialize()
    restoreMonitor()
//    loggingUpdate()
    
}


def restoreMonitor() {
    def runtime = state.totalRuntimeToday ?: 0
    def target = dailyHours * 3600
    def remaining = target - runtime
    if (state.isRunning) {
        unschedule("monitorPump")
        schedule("0 0/10 * * * ?", "monitorPump")
        logging("Pump - ${pump.displayName} Monitor restored - Pump Running Runtime=${String.format('%.2f', runtime/3600)}h","warn")
    }
    else if (remaining > 60 && state.nextStartTime) {
        unschedule("monitorPump")
        schedule("0 0/10 * * * ?", "monitorPump")
        logging("Pump - ${pump.displayName} Monitor restored - Cooldown active NextStart=${new Date(state.nextStartTime).format('HH:mm:ss', location.timeZone)} Remaining=${String.format('%.2f', remaining/3600)}h","warn")
    }
    else {
        state.nextStartTime = null
        unschedule("monitorPump")
        logging("Pump - ${pump.displayName} Daily runtime complete Runtime=${String.format('%.2f', runtime/3600)}h","info")
        logging("Pump - ${pump.displayName} Monitor not needed - Daily runtime complete Runtime=${String.format('%.2f', runtime/3600)}h","debug")
    }
}


def initialize() {
    if (state.totalRuntimeToday == null){ state.totalRuntimeToday = 0.0 }
          if (state.cyclesToday == null){ state.cyclesToday = 0 }
           if (state.isRunning == null){  state.isRunning = false }
    logging("Pump - ${pump.displayName} Runtime ${state.totalRuntimeToday} Cycles ${state.cyclesToday}", "debug")
    clientVersion()
    scheduleStartTime()
    logging("Pump - ${pump.displayName} Scheduler initialized - ${dailyHours} hrs/day | Max ${maxRunPerCycle} hrs per cycle", "info")
        // Restore monitor if pump was running


}


def scheduleStartTime() {

    // Remove any previous start schedule
    unschedule("startDailySchedule")

    switch(startOption) {

        case "Sunrise":
            subscribe(location, "sunriseTime", startDailySchedule)
            logging("Pump - ${pump.displayName} Scheduled to start at Sunrise", "info")
            break

        case "Sunset":
            subscribe(location, "sunsetTime", startDailySchedule)
            logging("Pump - ${pump.displayName} Scheduled to start at Sunset", "info")
            break

        case "Specific Time":
            if (startTime) {
                schedule(startTime, startDailySchedule)
                logging("Pump - ${pump.displayName} Scheduled to start at ${startTime}", "info")
            }
            break

        default:
            logging("Pump - ${pump.displayName} No valid start schedule configured", "warn")
            break
    }
}





def startDailySchedule(evt = null) {
    
    // Always make sure the monitor is running
    unschedule("monitorPump")
    schedule("0 0/10 * * * ?", "monitorPump")
    logging("Pump - ${pump.displayName} Monitor reset - 10m", "debug")
    
 //   runIn(60, "monitorPump") // lets not do this now
    
    def today = new Date().format("yyyy-MM-dd", location.timeZone)

    if (state.lastRunDate == today) {
        logging("Pump - ${pump.displayName} Today's schedule already started. Runtime ${state.totalRuntimeToday} Cycles ${state.cyclesToday}","warn")
        return
    }

    state.lastRunDate = today

    state.totalRuntimeToday = 0.0
    state.cyclesToday = 0

    state.isRunning = false
    state.stopTime = null
    state.nextStartTime = null
    state.currentCycleStart = null


    logging("Pump - ${pump.displayName} Daily cycle started","info")
    startPumpCycle()

    
}



def startPumpCycle() {
    if (state.isRunning) return

    if (maxCyclesPerDay > 0 && state.cyclesToday >= maxCyclesPerDay) {
        logging("Pump - ${pump.displayName} Max cycles per day reached ${state.cyclesToday}","info")
        return
    }
    
    def remaining = (dailyHours * 3600) - state.totalRuntimeToday
    if (remaining <= 0) return
    
    def runSeconds = Math.min(remaining, maxRunPerCycle * 3600)
    
    pump.on()
    state.isRunning = true
    state.currentCycleStart = now()
    state.stopTime = now() + (runSeconds.toInteger() * 1000)
    state.nextStartTime = null
    state.cyclesToday++
    state.lastStopTime = null    

    
    runIn(runSeconds.toInteger(), stopPumpCycle)

    logging("Pump - ${pump.displayName} turned on. Cycle:${state.cyclesToday} Run time:${runSeconds/3600} hrs Stop:${new Date(state.stopTime)}","info")
}

def stopPumpCycle() {
    if (!state.isRunning){
    logging("Pump - ${pump.displayName} Off time but was not running.","warn")
    return
    }
    state.isRunning = false 
    def actualRun = (now() - state.currentCycleStart) / 1000
    state.totalRuntimeToday += actualRun
    state.stopTime = null
    state.currentCycleStart = null
    state.lastStopTime = now()
    
    pump.off()  // do after to make sure theres no bounce
    
    logging("Pump - ${pump.displayName} turned OFF after ${actualRun/3600} hours","info")
    
 
    if (state.totalRuntimeToday < dailyHours * 3600) {
    logging("Pump - ${pump.displayName} Scheduling cooldown. Runtime=${state.totalRuntimeToday} Target=${dailyHours * 3600}", "debug")
    state.nextStartTime = now() + (cooldownMinutes * 60 * 1000)
    logging("Pump - ${pump.displayName} Next start will be ${new Date(state.nextStartTime)}", "debug")
    runIn(cooldownMinutes * 60, startPumpCycle)
}
else {
    logging("Pump - ${pump.displayName} Daily runtime complete", "info")
}
    logging("Pump - ${pump.displayName} After Stop: Runtime=${state.totalRuntimeToday} Stop=${state.stopTime} Next=${state.nextStartTime}", "debug")
    
    
}

def monitorPump() {
def debugRuntime = state.totalRuntimeToday ?: 0

    if (state.lastMonitorRun && (now() - state.lastMonitorRun) < 60) {
    logging("Pump - ${pump.displayName} Monitor skipped - ran recently", "debug")
    return
}

state.lastMonitorRun = now()
    
if (state.isRunning && state.currentCycleStart) {
    debugRuntime += (now() - state.currentCycleStart) / 1000
}

    def debugHours = debugRuntime / 3600
    logging("Pump - ${pump.displayName} Monitor: Switch=${pump.currentSwitch} Running=${state.isRunning} Runtime=${String.format('%.3f', debugHours)} hrs Cycles=${state.cyclesToday}","debug")     
    logging("Pump - ${pump.displayName} Monitor: StopTime=${state.stopTime ? new Date(state.stopTime) : 'NONE'} NextStart=${state.nextStartTime ? new Date(state.nextStartTime) : 'NONE'}", "trace")

    
if (state.isRunning) {

    if (pump.currentSwitch != "on") {
        logging("Pump - ${pump.displayName} was OFF but schedule says ON - turning ON", "warn")
        pump.on()
    }

    if (!state.stopTime) {
        logging("Pump - ${pump.displayName} StopTime missing - rebuilding from current cycle", "warn")
        state.stopTime = state.currentCycleStart + (maxRunPerCycle * 3600 * 1000)
    }

    if (now() >= state.stopTime) {
        logging("Pump - ${pump.displayName} Stop timer expired", "warn")
        stopPumpCycle()
    }

    // Clear bad cooldown state
    if (state.nextStartTime) {
        logging("Pump - ${pump.displayName} Clearing stale NextStart while running", "warn")
        state.nextStartTime = null
    }



    } else {

        // Pump should be OFF
        if (pump.currentSwitch != "off") {
            logging("Pump - ${pump.displayName} was ON but schedule says OFF - turning OFF", "warn")
            pump.off()
        }

        // More runtime needed today?
        if (state.totalRuntimeToday < (dailyHours * 3600)) {

            // Lost cooldown timer?
            if (!state.nextStartTime) {
                logging("Pump - ${pump.displayName} NextStart missing - rebuilding cooldown", "warn")
                state.nextStartTime = now() + (cooldownMinutes * 60 * 1000)
            }

           
           // Is the cooldown finished?
 if (state.nextStartTime && now() >= state.nextStartTime) {
    logging("Pump - ${pump.displayName} cooldown expired - starting next cycle", "warn")
    startPumpCycle()
}                 
            

        } else {

            logging("Pump - ${pump.displayName} Daily runtime complete Runtime=${state.totalRuntimeToday} Cycles=${state.cyclesToday}", "info")
            unschedule("monitorPump")
            logging("Pump - ${pump.displayName} Monitor stopped", "info")


        }
    }
}

// ====================== LOGGING ROUTINE (App Version) ======================

def loggingUpdate() {
    log.info "Logging Info:[${infoLogging}] Debug:[${debugLogging}] Trace:[${traceLogging}]"
    
    if (debugLogging) {
        log.warn "Debug logging will auto-disable in 50 minutes"
        runIn(3000, debugLogOff)   // 3000 seconds = 50 minutes
    }
    
    if (traceLogging) {
        log.warn "Trace logging will auto-disable in 30 minutes"
        runIn(1800, traceLogOff)   // 1800 seconds = 30 minutes
    }
}

void debugLogOff() {
    app.updateSetting("debugLogging", [value: "false", type: "bool"])
    log.debug "Debug Logging Automatically Disabled"
}

void traceLogOff() {
    app.updateSetting("traceLogging", [value: "false", type: "bool"])
    log.trace "Trace Logging Automatically Disabled"
}

private logging(String message, String level = "info") {
    switch(level) {
        case "error":
            log.error message
            break
        case "warn":
            log.warn message
            break
        case "trace":
            if (traceLogging) log.trace message
            break
        case "debug":
            if (debugLogging) log.debug message
            break
        case "info":
        default:
            if (infoLogging) log.info message
            break
    }
}
