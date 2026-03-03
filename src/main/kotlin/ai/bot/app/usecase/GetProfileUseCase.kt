package ai.bot.app.usecase

import ai.bot.app.data.model.Profile
import ai.bot.app.data.model.ProfileType
import ai.bot.app.data.repository.ProfileRepository

class GetProfileUseCase(private val profileRepository: ProfileRepository) {
    
    operator fun invoke(profileType: ProfileType): Profile {
        return profileRepository.getProfile(profileType)
    }
}