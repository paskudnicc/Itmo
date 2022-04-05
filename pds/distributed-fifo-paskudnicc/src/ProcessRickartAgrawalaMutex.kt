package mutex

import kotlin.math.*

/**
 * Rickart & Agrawala's Distributed Mutual Exclusion Algorithm implementation.
 * All functions are called from the single main thread.
 */
class ProcessRickartAgrawalaMutex(private val env: Environment) : Process {
    private val inf = Int.MAX_VALUE
    private var clock = 0 // logical time
    private var inCS = false // are we in critical section?
    private val req = IntArray(env.nProcesses + 1) { inf } // time of last REQ message
    private val ok = IntArray(env.nProcesses + 1) // time of last OK message
    private val pendingOk = BooleanArray(env.nProcesses + 1) // pending OK message (to send on unlock)

    override fun onMessage(srcId: Int, message: Message) {
        message.parse {
            val msgTime = readInt()
            val type = readEnum<MsgType>()
            clock = max(clock, msgTime) + 1 // update logical clock
            when (type) {
                MsgType.REQ -> {
                    val reqTime = readInt()
                    req[srcId] = reqTime
                    val myReqTime = req[env.processId]
                    if (reqTime < myReqTime || reqTime == myReqTime && srcId < env.processId) {
                        send(srcId, MsgType.OK)
                    } else {
                        pendingOk[srcId] = true
                    }
                }
                MsgType.OK -> {
                    ok[srcId] = msgTime
                    req[srcId] = inf
                }
            }
            checkCSEnter()
        }
    }

    private fun checkCSEnter() {
        val myReqTime = req[env.processId]
        if (myReqTime == inf) return // did not request CS, do nothing
        if (inCS) return // already in CS, do nothing
        for (i in 1..env.nProcesses) {
            if (i != env.processId) {
                if (req[i] < myReqTime || req[i] == myReqTime && i < env.processId) return // better ticket
                if (ok[i] <= myReqTime) return // did not Ok our request
            }
        }
        inCS = true
        env.locked()
    }

    override fun onLockRequest() {
        check(req[env.processId] == inf) { "Lock was already requested" }
        val myReqTime = ++clock // my request id
        req[env.processId] = myReqTime
        broadcast(MsgType.REQ) {
            writeInt(myReqTime)
        }
    }

    override fun onUnlockRequest() {
        check(inCS) { "We are not in critical section" }
        env.unlocked()
        inCS = false
        req[env.processId] = inf
        for (i in 1..env.nProcesses) {
            if (pendingOk[i]) {
                pendingOk[i] = false
                send(i, MsgType.OK)
            }
        }
    }

    private fun broadcast(type: MsgType, builder: MessageBuilder.() -> Unit = {}) {
        for (i in 1..env.nProcesses) {
            if (i != env.processId) {
                send(i, type, builder)
            }
        }
    }

    private fun send(destId: Int, type: MsgType, builder: MessageBuilder.() -> Unit = {}) {
        val msgTime = ++clock // increase logical time
        env.send(destId) {
            writeInt(msgTime)
            writeEnum(type)
            builder()
        }
    }

    enum class MsgType { REQ, OK }
}

