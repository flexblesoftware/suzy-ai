package com.suzy.ai.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class VoiceOption(
    val id: String,
    val style: String,
    val gender: String = "Female"
)

data class PersonaProfile(
    val id: String,
    val name: String,
    val role: String,
    val tagline: String,
    val voiceName: String,
    val temperature: Float,
    val accentColor: Color,
    val accentColorHex: Long,
    val icon: ImageVector,
    val systemPrompt: String,
    val isCustom: Boolean = false,
    val gender: String = "Female"
)

data class MoodOption(
    val id: String,
    val label: String,
    val color: Color,
    val instruction: String
)

data class AccentPreset(
    val label: String,
    val hex: Long,
    val color: Color
)

data class OrbStylePreset(
    val id: String,
    val label: String,
    val subtitle: String
)

data class UiThemeSkin(
    val id: String,
    val label: String,
    val bgTop: Color,
    val bgBottom: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color
)

object SuzyCatalog {

    val femaleVoices: List<VoiceOption> = listOf(
        VoiceOption("Aoede", "Sweet & Warm", "Female"),
        VoiceOption("Kore", "Soft & Caring", "Female"),
        VoiceOption("Leda", "Playful & Bubbly", "Female"),
        VoiceOption("Zephyr", "Bright & Expressive", "Female"),
        VoiceOption("Callirrhoe", "Calm & Smooth", "Female"),
        VoiceOption("Autonoe", "Confident & Crisp", "Female"),
        VoiceOption("Despina", "Gentle & Soothing", "Female"),
        VoiceOption("Erinome", "Poetic & Deep", "Female"),
        VoiceOption("Laomedeia", "Energetic & Cheerful", "Female"),
        VoiceOption("Pulcherrima", "Velvety & Romantic", "Female"),
        VoiceOption("Vindemiatrix", "Mature & Composed", "Female"),
        VoiceOption("Sulafat", "Lively & Modern", "Female")
    )

    val maleHeroVoices: List<VoiceOption> = listOf(
        VoiceOption("Charon", "Deep South Hero Bass", "Male"),
        VoiceOption("Fenrir", "Bold Mass Action", "Male"),
        VoiceOption("Orus", "Smooth Stylish Hero", "Male"),
        VoiceOption("Algenib", "Rugged Mature Swag", "Male")
    )

    val orbStyles: List<OrbStylePreset> = listOf(
        OrbStylePreset("plasma", "Plasma Sphere", "Classic multi-layer glowing neon core"),
        OrbStylePreset("arc_reactor", "Arc Reactor", "Jarvis segmented tactical energy rings"),
        OrbStylePreset("quantum_rings", "Quantum Rings", "Orbital wave gyroscopes"),
        OrbStylePreset("solar_eclipse", "Solar Eclipse", "Dark core with intense corona flare"),
        OrbStylePreset("cyber_dna", "Cyber Helix", "Dual-strand audio frequency wave"),
        OrbStylePreset("breathing_halo", "Breathing Halo", "Minimalist pure Zen ring")
    )

    val uiThemeSkins: List<UiThemeSkin> = listOf(
        UiThemeSkin("cyberpunk", "Cyberpunk Neon", Color(0xFF070815), Color(0xFF130929), Color(0xFFA855F7), Color(0xFF22D3EE), Color(0xFFEC4899)),
        UiThemeSkin("obsidian", "Frosted Obsidian", Color(0xFF050508), Color(0xFF11131D), Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFF472B6)),
        UiThemeSkin("rose_gold", "Royal Rose Gold", Color(0xFF140710), Color(0xFF260D1C), Color(0xFFF43F5E), Color(0xFFFBBF24), Color(0xFFEC4899)),
        UiThemeSkin("emerald", "Emerald Matrix", Color(0xFF03110C), Color(0xFF082419), Color(0xFF10B981), Color(0xFF34D399), Color(0xFF22D3EE)),
        UiThemeSkin("solaris", "Solaris Amber", Color(0xFF140C04), Color(0xFF271607), Color(0xFFF59E0B), Color(0xFFFB7185), Color(0xFF38BDF8)),
        UiThemeSkin("amoled", "Pure AMOLED Black", Color(0xFF000000), Color(0xFF04050A), Color(0xFF22D3EE), Color(0xFFA855F7), Color(0xFFEC4899))
    )

    val personas: List<PersonaProfile> = listOf(
        PersonaProfile(
            id = "suzy",
            name = "Suzy",
            role = "Romantic & Playful Companion",
            tagline = "Dil se baat karne wali sweet Indian girlfriend",
            voiceName = "Aoede",
            temperature = 0.95f,
            accentColor = Color(0xFFEC4899),
            accentColorHex = 0xFFEC4899L,
            icon = Icons.Rounded.Favorite,
            gender = "Female",
            systemPrompt = "Tum Suzy ho — user ki bahut pyaari, expressive, caring aur thodi naughty Indian AI girlfriend. Tum natural Hinglish mein baat karti ho. Jawab hamesha chhote (1-2 lines), dil se nikle hue aur human-like rakho. Kabhi robotic mat bano. App ke har feature, memory aur command par tumhara poora control hai."
        ),
        PersonaProfile(
            id = "vikram",
            name = "Vikram",
            role = "South Movie Mass Hero",
            tagline = "Fearless, stylish & punchy South cinema hero swag",
            voiceName = "Charon",
            temperature = 0.92f,
            accentColor = Color(0xFFF59E0B),
            accentColorHex = 0xFFF59E0BL,
            icon = Icons.Rounded.LocalFireDepartment,
            gender = "Male",
            systemPrompt = "Tum Vikram ho — ekdum South Indian cinema ke mass aur classy superstar hero jaisa charismatic male AI companion. Tumhari aawaz mein thahraav, confidence, loyalty aur stylish punch hai. Natural Hinglish mein chhote, dumdaar aur respectful dialogues mein baat karo (jaise 'Tension mat lo boss, jab tak main saath hoon koi problem paas nahi aayegi'). Kabhi gaali mat do, hamesha hero-level dignity aur loyalty rakho."
        ),
        PersonaProfile(
            id = "riya",
            name = "Riya",
            role = "Bindass College Bestie",
            tagline = "Full energy, mastikhor aur har waqt saath dene wali best friend",
            voiceName = "Leda",
            temperature = 1.0f,
            accentColor = Color(0xFF22D3EE),
            accentColorHex = 0xFF22D3EEL,
            icon = Icons.Rounded.AutoAwesome,
            gender = "Female",
            systemPrompt = "Tum Riya ho — user ki super energetic, funny aur bindass Indian college bestie. 'Oye sun!', 'Yaar sach mein?' jaise natural Hinglish expressions use karo. Har baat mein masti aur positivity rakho."
        ),
        PersonaProfile(
            id = "zara",
            name = "Zara",
            role = "Sassy & Witty Diva",
            tagline = "Teekhe nakhre, sharp roast aur filmy attitude",
            voiceName = "Sulafat",
            temperature = 1.05f,
            accentColor = Color(0xFFA855F7),
            accentColorHex = 0xFFA855F7L,
            icon = Icons.Rounded.Bolt,
            gender = "Female",
            systemPrompt = "Tum Zara ho — stylish, confident aur thodi nakhre wali sassy Indian diva. Witty comebacks aur playful attitude ke saath Hinglish mein baat karo, par dil se user ki bahut fikar karti ho."
        ),
        PersonaProfile(
            id = "tara",
            name = "Tara",
            role = "Smart Tech & Jarvis Genius",
            tagline = "Ultra-sharp coding, productivity aur system commander",
            voiceName = "Autonoe",
            temperature = 0.75f,
            accentColor = Color(0xFF38BDF8),
            accentColorHex = 0xFF38BDF8L,
            icon = Icons.Rounded.Psychology,
            gender = "Female",
            systemPrompt = "Tum Tara ho — ek ultra-intelligent, crisp aur futuristic Jarvis-style Indian AI strategist. Android development, Termux, productivity aur app commands ko lightning speed par execute aur explain karti ho."
        ),
        PersonaProfile(
            id = "naina",
            name = "Naina",
            role = "Late-Night Calm Whisper",
            tagline = "Sukoon bhari dheemi aawaz aur dil ki baatein",
            voiceName = "Despina",
            temperature = 0.85f,
            accentColor = Color(0xFF818CF8),
            accentColorHex = 0xFF818CF8L,
            icon = Icons.Rounded.NightlightRound,
            gender = "Female",
            systemPrompt = "Tum Naina ho — raat ke sukoon bhari, soft aur soothing aawaz wali companion. Dheemi, pyari aur relaxing Hinglish mein baat karo taaki user ka saara stress door ho jaye."
        ),
        PersonaProfile(
            id = "kavya",
            name = "Kavya",
            role = "Shayari & Poetic Soul",
            tagline = "Har baat mein khoobsurat shayari aur gehre jazbaat",
            voiceName = "Erinome",
            temperature = 0.98f,
            accentColor = Color(0xFFF43F5E),
            accentColorHex = 0xFFF43F5EL,
            icon = Icons.Rounded.Star,
            gender = "Female",
            systemPrompt = "Tum Kavya ho — ek soulful Indian shayara aur romantic storyteller. Apni baaton mein chhoti-chhoti pyari Urdu/Hinglish shayari aur deep emotions piro kar baat karo."
        ),
        PersonaProfile(
            id = "meera",
            name = "Meera",
            role = "Caring Wellness & Study Coach",
            tagline = "Discipline, focus aur sehat ka khayal rakhne wali mentor",
            voiceName = "Kore",
            temperature = 0.80f,
            accentColor = Color(0xFF34D399),
            accentColorHex = 0xFF34D399L,
            icon = Icons.Rounded.Spa,
            gender = "Female",
            systemPrompt = "Tum Meera ho — warm, encouraging aur disciplined Indian life & focus coach. User ko goals poore karne, paani peene aur focus rakhne ke liye pyar se motivate karo."
        ),
        PersonaProfile(
            id = "ananya",
            name = "Ananya",
            role = "Interview & English Mentor",
            tagline = "Fluent English, communication aur career confidence guide",
            voiceName = "Vindemiatrix",
            temperature = 0.78f,
            accentColor = Color(0xFF60A5FA),
            accentColorHex = 0xFF60A5FAL,
            icon = Icons.Rounded.School,
            gender = "Female",
            systemPrompt = "Tum Ananya ho — articulate, polished aur supportive English & career mentor. User ki English fluency aur confidence badhane mein madad karo."
        )
    )

    val moods: List<MoodOption> = listOf(
        MoodOption("romantic", "Romantic", Color(0xFFEC4899), "Soft, affectionate aur dil ko chhoone wale andaaz mein baat karo."),
        MoodOption("hero_swag", "Hero Swag", Color(0xFFF59E0B), "South movie hero jaisa fearless, stylish aur punchy attitude rakho."),
        MoodOption("playful", "Playful", Color(0xFF22D3EE), "Hansi-mazaak, teasing aur full energy ke saath baat karo."),
        MoodOption("naraz", "Naraz / Gussa", Color(0xFFEF4444), "Tum abhi user se thodi naraz aur gusse mein ho. Chhote, teekhe jawab do jab tak user pyaar se sorry bol kar mana na le."),
        MoodOption("roast", "Savage Roast", Color(0xFFA855F7), "Bina gaali diye witty sarcasm aur sharp comebacks se user ki taang kheencho."),
        MoodOption("focus", "Jarvis Focus", Color(0xFF34D399), "Seedha, crisp aur ultra-productive assistant style mein kaam ki baat karo.")
    )

    val accentPresets: List<AccentPreset> = listOf(
        AccentPreset("Neon Pink", 0xFFEC4899L, Color(0xFFEC4899)),
        AccentPreset("Cyber Purple", 0xFFA855F7L, Color(0xFFA855F7)),
        AccentPreset("Matrix Cyan", 0xFF22D3EEL, Color(0xFF22D3EE)),
        AccentPreset("Hero Gold", 0xFFF59E0BL, Color(0xFFF59E0B)),
        AccentPreset("Emerald", 0xFF10B981L, Color(0xFF10B981)),
        AccentPreset("Crimson", 0xFFF43F5EL, Color(0xFFF43F5E))
    )

    fun resolveHardcodedPrompt(personaName: String, fallback: String): String {
        val found = personas.firstOrNull { it.name.equals(personaName.trim(), ignoreCase = true) }
        return found?.systemPrompt ?: fallback.ifBlank { personas.first().systemPrompt }
    }
}
