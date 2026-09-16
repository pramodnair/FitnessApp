package com.example.fitnessapp.ui.bodyprogress

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.FitnessApplication
import com.example.fitnessapp.data.model.BodyPhoto
import com.example.fitnessapp.data.model.BodyPose
import com.example.fitnessapp.data.model.UserProfile
import com.example.fitnessapp.data.repository.FitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class BodyProgressViewModel(
    private val repository: FitnessRepository = FitnessApplication.instance.repository
) : ViewModel() {

    val activeProfile: StateFlow<UserProfile> = repository.activeProfile
    val bodyPhotos: StateFlow<List<BodyPhoto>> = repository.bodyPhotos

    private val _selectedBeforePhoto = MutableStateFlow<BodyPhoto?>(null)
    val selectedBeforePhoto: StateFlow<BodyPhoto?> = _selectedBeforePhoto.asStateFlow()

    private val _selectedAfterPhoto = MutableStateFlow<BodyPhoto?>(null)
    val selectedAfterPhoto: StateFlow<BodyPhoto?> = _selectedAfterPhoto.asStateFlow()

    fun selectBefore(photo: BodyPhoto) {
        _selectedBeforePhoto.value = photo
    }

    fun selectAfter(photo: BodyPhoto) {
        _selectedAfterPhoto.value = photo
    }

    fun saveBodyPhoto(bitmap: Bitmap, pose: BodyPose, weightKg: Float, notes: String) {
        viewModelScope.launch {
            // Save to private internal storage directory
            val photosDir = File(FitnessApplication.instance.filesDir, "body_photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            val photoFile = File(photosDir, "body_${System.currentTimeMillis()}_${pose.name}.jpg")
            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            repository.addBodyPhoto(photoFile.absolutePath, pose, weightKg, notes)
        }
    }
}
