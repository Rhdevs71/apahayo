package com.rhdevs.rhpatch.discord

import com.rhdevs.rhpatch.Patch
import com.rhdevs.rhpatch.discord.messages.AntiDelete
import com.rhdevs.rhpatch.discord.privacy.GhostMode

val DiscordPatches = arrayOf<Patch>(
    GhostMode,
    AntiDelete
)
