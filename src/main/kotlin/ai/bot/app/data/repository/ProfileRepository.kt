package ai.bot.app.data.repository

import ai.bot.app.data.model.Profile
import ai.bot.app.data.model.ProfileType
import ai.bot.app.usecase.LoadProfileFromFileUseCase

class ProfileRepository(
    private val loadProfileFromFileUseCase: LoadProfileFromFileUseCase
) {
    private val cache = mutableMapOf<ProfileType, Profile>()

    fun getProfile(profileType: ProfileType): Profile {
        return cache[profileType] ?: run {
            val profile = loadProfileFromFileUseCase(profileType)
            cache[profileType] = profile
            profile
        }
    }
}