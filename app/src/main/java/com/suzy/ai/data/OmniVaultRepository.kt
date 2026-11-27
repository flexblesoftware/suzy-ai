package com.suzy.ai.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.omniVaultStore by preferencesDataStore(name = "suzy_omni_vault_v4")

data class ChatMessageItem(
    val id: String,
    val role: String, // "user" or "model"
    val personaName: String,
    val text: String,
    val attachmentLabel: String = "",
    val commandExecuted: String = "",
    val timestamp: String
)

data class PlannerTaskItem(
    val id: String,
    val title: String,
    val category: String,
    val timeLabel: String,
    val completed: Boolean
)

data class FinanceEntryItem(
    val id: String,
    val kind: String, // "EXPENSE", "UDHAAR_LENA", "UDHAAR_DENA", "SAVINGS", "BILL"
    val title: String,
    val personOrCategory: String,
    val amountInr: Double,
    val dueDateOrNote: String,
    val settled: Boolean = false
)

data class HabitEntryItem(
    val id: String,
    val name: String,
    val streak: Int,
    val doneToday: Boolean
)

data class NoteVaultItem(
    val id: String,
    val kind: String, // "MEMO", "MEETING", "BOOKMARK", "DREAM", "SHOPPING", "MEDICINE"
    val title: String,
    val body: String,
    val timestamp: String,
    val checked: Boolean = false
)

data class AutomationRuleItem(
    val id: String,
    val kind: String, // "MACRO", "WEBHOOK", "SHORTCUT", "IFTTT"
    val triggerPhrase: String,
    val actionSummary: String,
    val endpointOrTarget: String,
    val enabled: Boolean = true
)

data class SecurityAuditItem(
    val id: String,
    val timestamp: String,
    val command: String,
    val status: String,
    val reason: String
)

class OmniVaultRepository(private val context: Context) {

    private object Keys {
        val CHAT_JSON = stringPreferencesKey("chat_messages_v4")
        val TASKS_JSON = stringPreferencesKey("planner_tasks_v4")
        val FINANCE_JSON = stringPreferencesKey("finance_entries_v4")
        val HABITS_JSON = stringPreferencesKey("habits_v4")
        val NOTES_JSON = stringPreferencesKey("notes_vault_v4")
        val RULES_JSON = stringPreferencesKey("automation_rules_v4")
        val SECURITY_JSON = stringPreferencesKey("security_audit_v4")
        val PARKING_JSON = stringPreferencesKey("parking_spot_v4")
        val MEDICAL_JSON = stringPreferencesKey("medical_id_v4")
        val CREDENTIALS_JSON = stringPreferencesKey("encrypted_creds_v4")
    }

    private fun nowStr(): String =
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())

    val chatMessagesFlow: Flow<List<ChatMessageItem>> = context.omniVaultStore.data.map { prefs ->
        parseChat(prefs[Keys.CHAT_JSON] ?: "[]")
    }

    val tasksFlow: Flow<List<PlannerTaskItem>> = context.omniVaultStore.data.map { prefs ->
        parseTasks(prefs[Keys.TASKS_JSON] ?: defaultTasksJson())
    }

    val financeFlow: Flow<List<FinanceEntryItem>> = context.omniVaultStore.data.map { prefs ->
        parseFinance(prefs[Keys.FINANCE_JSON] ?: defaultFinanceJson())
    }

    val habitsFlow: Flow<List<HabitEntryItem>> = context.omniVaultStore.data.map { prefs ->
        parseHabits(prefs[Keys.HABITS_JSON] ?: defaultHabitsJson())
    }

    val notesFlow: Flow<List<NoteVaultItem>> = context.omniVaultStore.data.map { prefs ->
        parseNotes(prefs[Keys.NOTES_JSON] ?: defaultNotesJson())
    }

    val rulesFlow: Flow<List<AutomationRuleItem>> = context.omniVaultStore.data.map { prefs ->
        parseRules(prefs[Keys.RULES_JSON] ?: defaultRulesJson())
    }

    val securityAuditFlow: Flow<List<SecurityAuditItem>> = context.omniVaultStore.data.map { prefs ->
        parseSecurity(prefs[Keys.SECURITY_JSON] ?: "[]")
    }

    val parkingNoteFlow: Flow<String> = context.omniVaultStore.data.map { prefs ->
        prefs[Keys.PARKING_JSON] ?: "Not saved yet — say 'Save parking Basement 1 Pillar A4'"
    }

    val medicalIdFlow: Flow<String> = context.omniVaultStore.data.map { prefs ->
        prefs[Keys.MEDICAL_JSON] ?: "Blood Group: O+ | Allergies: None | Emergency Contact: Mom"
    }

    suspend fun appendChatMessage(
        role: String,
        personaName: String,
        text: String,
        attachmentLabel: String = "",
        commandExecuted: String = ""
    ) {
        val item = ChatMessageItem(
            id = UUID.randomUUID().toString(),
            role = role,
            personaName = personaName,
            text = text,
            attachmentLabel = attachmentLabel,
            commandExecuted = commandExecuted,
            timestamp = nowStr()
        )
        context.omniVaultStore.edit { prefs ->
            val current = parseChat(prefs[Keys.CHAT_JSON] ?: "[]")
            val updated = (current + item).takeLast(120)
            prefs[Keys.CHAT_JSON] = serializeChat(updated)
        }
    }

    suspend fun clearChatHistory() {
        context.omniVaultStore.edit { prefs ->
            prefs[Keys.CHAT_JSON] = "[]"
        }
    }

    suspend fun addTask(title: String, category: String = "Daily", timeLabel: String = "Today") {
        if (title.isBlank()) return
        val item = PlannerTaskItem(UUID.randomUUID().toString(), title.trim(), category, timeLabel, false)
        context.omniVaultStore.edit { prefs ->
            val current = parseTasks(prefs[Keys.TASKS_JSON] ?: defaultTasksJson())
            prefs[Keys.TASKS_JSON] = serializeTasks(listOf(item) + current)
        }
    }

    suspend fun toggleTask(id: String) {
        context.omniVaultStore.edit { prefs ->
            val current = parseTasks(prefs[Keys.TASKS_JSON] ?: defaultTasksJson())
            prefs[Keys.TASKS_JSON] = serializeTasks(current.map {
                if (it.id == id) it.copy(completed = !it.completed) else it
            })
        }
    }

    suspend fun deleteTask(id: String) {
        context.omniVaultStore.edit { prefs ->
            val current = parseTasks(prefs[Keys.TASKS_JSON] ?: defaultTasksJson())
            prefs[Keys.TASKS_JSON] = serializeTasks(current.filterNot { it.id == id })
        }
    }

    suspend fun addFinanceEntry(
        kind: String,
        title: String,
        personOrCategory: String,
        amountInr: Double,
        dueDateOrNote: String = ""
    ) {
        val item = FinanceEntryItem(
            id = UUID.randomUUID().toString(),
            kind = kind,
            title = title.trim(),
            personOrCategory = personOrCategory.trim(),
            amountInr = amountInr,
            dueDateOrNote = dueDateOrNote.ifBlank { nowStr() },
            settled = false
        )
        context.omniVaultStore.edit { prefs ->
            val current = parseFinance(prefs[Keys.FINANCE_JSON] ?: defaultFinanceJson())
            prefs[Keys.FINANCE_JSON] = serializeFinance(listOf(item) + current)
        }
    }

    suspend fun toggleFinanceSettled(id: String) {
        context.omniVaultStore.edit { prefs ->
            val current = parseFinance(prefs[Keys.FINANCE_JSON] ?: defaultFinanceJson())
            prefs[Keys.FINANCE_JSON] = serializeFinance(current.map {
                if (it.id == id) it.copy(settled = !it.settled) else it
            })
        }
    }

    suspend fun toggleHabit(id: String) {
        context.omniVaultStore.edit { prefs ->
            val current = parseHabits(prefs[Keys.HABITS_JSON] ?: defaultHabitsJson())
            prefs[Keys.HABITS_JSON] = serializeHabits(current.map {
                if (it.id == id) {
                    val nextDone = !it.doneToday
                    it.copy(doneToday = nextDone, streak = if (nextDone) it.streak + 1 else (it.streak - 1).coerceAtLeast(0))
                } else it
            })
        }
    }

    suspend fun addHabit(name: String) {
        if (name.isBlank()) return
        val item = HabitEntryItem(UUID.randomUUID().toString(), name.trim(), 1, false)
        context.omniVaultStore.edit { prefs ->
            val current = parseHabits(prefs[Keys.HABITS_JSON] ?: defaultHabitsJson())
            prefs[Keys.HABITS_JSON] = serializeHabits(listOf(item) + current)
        }
    }

    suspend fun addNoteItem(kind: String, title: String, body: String) {
        val item = NoteVaultItem(UUID.randomUUID().toString(), kind, title.trim(), body.trim(), nowStr(), false)
        context.omniVaultStore.edit { prefs ->
            val current = parseNotes(prefs[Keys.NOTES_JSON] ?: defaultNotesJson())
            prefs[Keys.NOTES_JSON] = serializeNotes(listOf(item) + current)
        }
    }

    suspend fun toggleNoteChecked(id: String) {
        context.omniVaultStore.edit { prefs ->
            val current = parseNotes(prefs[Keys.NOTES_JSON] ?: defaultNotesJson())
            prefs[Keys.NOTES_JSON] = serializeNotes(current.map {
                if (it.id == id) it.copy(checked = !it.checked) else it
            })
        }
    }

    suspend fun deleteNoteItem(id: String) {
        context.omniVaultStore.edit { prefs ->
            val current = parseNotes(prefs[Keys.NOTES_JSON] ?: defaultNotesJson())
            prefs[Keys.NOTES_JSON] = serializeNotes(current.filterNot { it.id == id })
        }
    }

    suspend fun addAutomationRule(kind: String, triggerPhrase: String, actionSummary: String, endpointOrTarget: String) {
        val item = AutomationRuleItem(UUID.randomUUID().toString(), kind, triggerPhrase.trim(), actionSummary.trim(), endpointOrTarget.trim(), true)
        context.omniVaultStore.edit { prefs ->
            val current = parseRules(prefs[Keys.RULES_JSON] ?: defaultRulesJson())
            prefs[Keys.RULES_JSON] = serializeRules(listOf(item) + current)
        }
    }

    suspend fun logSecurityEvent(command: String, status: String, reason: String) {
        val item = SecurityAuditItem(UUID.randomUUID().toString(), nowStr(), command, status, reason)
        context.omniVaultStore.edit { prefs ->
            val current = parseSecurity(prefs[Keys.SECURITY_JSON] ?: "[]")
            prefs[Keys.SECURITY_JSON] = serializeSecurity((listOf(item) + current).take(50))
        }
    }

    suspend fun saveParkingSpot(spot: String) {
        context.omniVaultStore.edit { prefs ->
            prefs[Keys.PARKING_JSON] = "$spot (${nowStr()})"
        }
    }

    suspend fun saveMedicalId(info: String) {
        context.omniVaultStore.edit { prefs ->
            prefs[Keys.MEDICAL_JSON] = info.trim()
        }
    }

    private fun defaultTasksJson(): String = serializeTasks(
        listOf(
            PlannerTaskItem("t1", "Morning GitHub APK Release Check", "Coding", "09:00 AM", false),
            PlannerTaskItem("t2", "Drink 2L Water & Workout", "Health", "06:00 PM", false)
        )
    )

    private fun defaultFinanceJson(): String = serializeFinance(
        listOf(
            FinanceEntryItem("f1", "EXPENSE", "Chai & Snacks", "Food", 120.0, "Today"),
            FinanceEntryItem("f2", "UDHAAR_LENA", "App Design Split", "Rahul", 500.0, "Pending"),
            FinanceEntryItem("f3", "SAVINGS", "New Android Device Jar", "Goal ₹25,000", 4500.0, "18% Saved"),
            FinanceEntryItem("f4", "BILL", "5G Mobile Recharge", "Monthly", 349.0, "Due in 4 days")
        )
    )

    private fun defaultHabitsJson(): String = serializeHabits(
        listOf(
            HabitEntryItem("h1", "Android Coding Sprint", 6, true),
            HabitEntryItem("h2", "Hydration 8 Glasses", 4, false),
            HabitEntryItem("h3", "Evening Walk 5,000 Steps", 5, false)
        )
    )

    private fun defaultNotesJson(): String = serializeNotes(
        listOf(
            NoteVaultItem("n1", "MEDICINE", "Vitamin D3 Capsule", "After Lunch 02:00 PM", "Daily", false),
            NoteVaultItem("n2", "SHOPPING", "Cold Coffee & Almonds", "Groceries", "Today", false),
            NoteVaultItem("n3", "MEMO", "Suzy AI v4.0 Omni-OS", "150 modules + South Hero Vikram + Chat", "Today", false)
        )
    )

    private fun defaultRulesJson(): String = serializeRules(
        listOf(
            AutomationRuleItem("r1", "MACRO", "Good Morning Suzy", "Boss Briefing + Arc Reactor ORB + 70% Brightness", "morning_routine"),
            AutomationRuleItem("r2", "MACRO", "Hero Entry", "Switch Persona to Vikram + Hero Gold Theme", "vikram_mode"),
            AutomationRuleItem("r3", "WEBHOOK", "Ping Local Termux", "GET http://127.0.0.1:8080/status", "http://127.0.0.1:8080/status")
        )
    )

    private fun parseChat(raw: String): List<ChatMessageItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ChatMessageItem(o.optString("id"), o.optString("role"), o.optString("personaName"), o.optString("text"), o.optString("attachmentLabel"), o.optString("commandExecuted"), o.optString("timestamp")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeChat(list: List<ChatMessageItem>): String = JSONArray().apply {
        list.forEach { m ->
            put(JSONObject().apply {
                put("id", m.id); put("role", m.role); put("personaName", m.personaName)
                put("text", m.text); put("attachmentLabel", m.attachmentLabel)
                put("commandExecuted", m.commandExecuted); put("timestamp", m.timestamp)
            })
        }
    }.toString()

    private fun parseTasks(raw: String): List<PlannerTaskItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(PlannerTaskItem(o.optString("id"), o.optString("title"), o.optString("category"), o.optString("timeLabel"), o.optBoolean("completed")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeTasks(list: List<PlannerTaskItem>): String = JSONArray().apply {
        list.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id); put("title", t.title); put("category", t.category)
                put("timeLabel", t.timeLabel); put("completed", t.completed)
            })
        }
    }.toString()

    private fun parseFinance(raw: String): List<FinanceEntryItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(FinanceEntryItem(o.optString("id"), o.optString("kind"), o.optString("title"), o.optString("personOrCategory"), o.optDouble("amountInr"), o.optString("dueDateOrNote"), o.optBoolean("settled")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeFinance(list: List<FinanceEntryItem>): String = JSONArray().apply {
        list.forEach { f ->
            put(JSONObject().apply {
                put("id", f.id); put("kind", f.kind); put("title", f.title)
                put("personOrCategory", f.personOrCategory); put("amountInr", f.amountInr)
                put("dueDateOrNote", f.dueDateOrNote); put("settled", f.settled)
            })
        }
    }.toString()

    private fun parseHabits(raw: String): List<HabitEntryItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(HabitEntryItem(o.optString("id"), o.optString("name"), o.optInt("streak"), o.optBoolean("doneToday")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeHabits(list: List<HabitEntryItem>): String = JSONArray().apply {
        list.forEach { h ->
            put(JSONObject().apply {
                put("id", h.id); put("name", h.name); put("streak", h.streak); put("doneToday", h.doneToday)
            })
        }
    }.toString()

    private fun parseNotes(raw: String): List<NoteVaultItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(NoteVaultItem(o.optString("id"), o.optString("kind"), o.optString("title"), o.optString("body"), o.optString("timestamp"), o.optBoolean("checked")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeNotes(list: List<NoteVaultItem>): String = JSONArray().apply {
        list.forEach { n ->
            put(JSONObject().apply {
                put("id", n.id); put("kind", n.kind); put("title", n.title)
                put("body", n.body); put("timestamp", n.timestamp); put("checked", n.checked)
            })
        }
    }.toString()

    private fun parseRules(raw: String): List<AutomationRuleItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(AutomationRuleItem(o.optString("id"), o.optString("kind"), o.optString("triggerPhrase"), o.optString("actionSummary"), o.optString("endpointOrTarget"), o.optBoolean("enabled", true)))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeRules(list: List<AutomationRuleItem>): String = JSONArray().apply {
        list.forEach { r ->
            put(JSONObject().apply {
                put("id", r.id); put("kind", r.kind); put("triggerPhrase", r.triggerPhrase)
                put("actionSummary", r.actionSummary); put("endpointOrTarget", r.endpointOrTarget); put("enabled", r.enabled)
            })
        }
    }.toString()

    private fun parseSecurity(raw: String): List<SecurityAuditItem> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(SecurityAuditItem(o.optString("id"), o.optString("timestamp"), o.optString("command"), o.optString("status"), o.optString("reason")))
            }
        }
    } catch (_: Exception) { emptyList() }

    private fun serializeSecurity(list: List<SecurityAuditItem>): String = JSONArray().apply {
        list.forEach { s ->
            put(JSONObject().apply {
                put("id", s.id); put("timestamp", s.timestamp); put("command", s.command)
                put("status", s.status); put("reason", s.reason)
            })
        }
    }.toString()
}
